package  com.meetfriend.app.videoplayer

import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import cn.jzvd.JZMediaInterface
import cn.jzvd.Jzvd
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.meetfriend.app.R

class JZMediaMp4ExoKotlin(jzvd: Jzvd) : JZMediaInterface(jzvd), Player.Listener {

    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var callback: Runnable? = null
    private var previousSeek: Long = 0

    override fun start() {
        simpleExoPlayer?.playWhenReady = true
    }

    override fun prepare() {
        val context = jzvd.context
        release()
        mMediaHandlerThread = HandlerThread("JZVD")
        mMediaHandlerThread.start()
        mMediaHandler = Handler(context.mainLooper) //主线程还是非主线程，就在这里
        handler = Handler()
        mMediaHandler.post {
            val trackSelector: TrackSelector =
                DefaultTrackSelector(context, AdaptiveTrackSelection.Factory())
            val loadControl: DefaultLoadControl.Builder = DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                .setBufferDurationsMs(1000,
                    1000,
                    1000,
                    1000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .setTargetBufferBytes(C.LENGTH_UNSET)
            val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
            val renderersFactory: RenderersFactory = DefaultRenderersFactory(context)
            if (simpleExoPlayer == null) {
                simpleExoPlayer = SimpleExoPlayer.Builder(context, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl.build())
                    .setBandwidthMeter(bandwidthMeter)
                    .build()
                val isLoop = jzvd.jzDataSource.looping
                simpleExoPlayer?.repeatMode = if (isLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                simpleExoPlayer?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                jzvd.textureView?.surfaceTexture?.let {
                    simpleExoPlayer?.setVideoSurface(Surface(it))
                }
                simpleExoPlayer?.addListener(this)
                simpleExoPlayer?.playWhenReady = true
            }
            val currUrl = jzvd.jzDataSource.currentUrl?.toString() ?: ""

            val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
                context,
                Util.getUserAgent(context, context.resources.getString(R.string.app_name))
            )

            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(currUrl))
            simpleExoPlayer?.setMediaSource(mediaSource)
            simpleExoPlayer?.prepare()
            callback = OnBufferingUpdate()
        }
    }

    override fun pause() {
        simpleExoPlayer?.playWhenReady = false
    }

    override fun isPlaying(): Boolean {
        return simpleExoPlayer?.playWhenReady ?: false
    }

    override fun seekTo(time: Long) {
        if (simpleExoPlayer == null) {
            return
        }
        if (time != previousSeek) {
            if (time >= (simpleExoPlayer?.bufferedPosition ?: 0)) {
                jzvd.onStatePreparingPlaying()
            }
            simpleExoPlayer?.seekTo(time)
            previousSeek = time
            jzvd.seekToInAdvance = time
        }
    }

    override fun release() {
        if (mMediaHandler != null && mMediaHandlerThread != null && simpleExoPlayer != null) { //不知道有没有妖孽
            val tmpHandlerThread = mMediaHandlerThread
            val tmpMediaPlayer: SimpleExoPlayer = simpleExoPlayer ?: return
            SAVED_SURFACE = null
            mMediaHandler.post {
                tmpMediaPlayer.release() //release就不能放到主线程里，界面会卡顿
                tmpHandlerThread.quit()
            }
            simpleExoPlayer = null
        }
    }

    override fun getCurrentPosition(): Long {
        return simpleExoPlayer?.currentPosition ?: 0
    }

    override fun getDuration(): Long {
        return simpleExoPlayer?.duration ?: 0
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        simpleExoPlayer?.volume = leftVolume
        simpleExoPlayer?.volume = rightVolume
    }

    override fun setSpeed(speed: Float) {
        val playbackParameters = PlaybackParameters(speed, 1.0f)
        simpleExoPlayer?.playbackParameters = playbackParameters
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        handler.post {
            when (playbackState) {
                Player.STATE_IDLE -> {
                }

                Player.STATE_BUFFERING -> {
                    jzvd.onStatePreparingPlaying()
                    handler.post(callback!!)
                }

                Player.STATE_READY -> {
                    if (simpleExoPlayer?.playWhenReady == true) {
                        jzvd.onStatePlaying()
                    }
                }

                Player.STATE_ENDED -> {
                    jzvd.onCompletion()
                }
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        when(reason) {
            Player.DISCONTINUITY_REASON_SKIP -> {
                handler.post { jzvd.onSeekComplete() }
            }
            else -> {

            }
        }
    }

    override fun setSurface(surface: Surface) {
        if (simpleExoPlayer != null) {
            simpleExoPlayer?.setVideoSurface(surface)
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (SAVED_SURFACE == null) {
            SAVED_SURFACE = surface
            prepare()
        } else {
            jzvd.textureView.setSurfaceTexture(SAVED_SURFACE)
        }
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private inner class OnBufferingUpdate : Runnable {
        override fun run() {
            if (simpleExoPlayer != null) {
                val percent = simpleExoPlayer?.bufferedPercentage ?: 0
                handler.post { jzvd.setBufferProgress(percent) }
                if (percent < 100) {
                    handler.postDelayed(callback!!, 300)
                } else {
                    handler.removeCallbacks(callback!!)
                }
            }
        }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        handler.post { jzvd.onVideoSizeChanged(videoSize.width, videoSize.height) }
    }
}