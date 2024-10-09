package net.vrgsoft.videcrop.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;

import static com.google.android.exoplayer2.C.TIME_UNSET;

import net.vrgsoft.videcrop.R;

public class VideoPlayer implements TimeBar.OnScrubListener, Player.Listener{
    private SimpleExoPlayer player;
    private OnProgressUpdateListener mUpdateListener;
    private Handler progressHandler;
    private Runnable progressUpdater;

    public VideoPlayer(Context context) {
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);

        // Create a RenderersFactory (using the default implementation here)
        RenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        player = new SimpleExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector)
                .build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.addListener(this);
        progressHandler = new Handler();
    }

    public void initMediaSource(Context context, String uri) {
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(context, context.getResources().getString(R.string.app_name)));
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                context, httpDataSourceFactory
        );
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(uri)));

        player.setMediaSource(mediaSource);
        player.prepare();
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void play(boolean play) {
        player.setPlayWhenReady(play);
        if (!play) {
            removeUpdater();
        }
    }

    public void release() {
        player.release();
        removeUpdater();
        player = null;
    }

    public boolean isPlaying() {
        return player.getPlayWhenReady();
    }



    @Override
    public void onScrubStart(TimeBar timeBar, long position) {

    }


    @Override
    public void onScrubMove(TimeBar timeBar, long position) {
        seekTo(position);
        updateProgress();
    }

    @Override
    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        seekTo(position);
        updateProgress();
    }

    private void updateProgress() {
        if (mUpdateListener != null) {
            mUpdateListener.onProgressUpdate(
                    player.getCurrentPosition(),
                    player.getDuration() == TIME_UNSET ? 0L : player.getDuration(),
                    player.getBufferedPosition());
        }
        initUpdateTimer();
    }

    private void initUpdateTimer() {
        long position = player.getCurrentPosition();
        int playbackState = player.getPlaybackState();
        long delayMs;
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            if (player.getPlayWhenReady() && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }

            removeUpdater();
            progressUpdater = new Runnable() {
                @Override
                public void run() {
                    updateProgress();
                }
            };

            progressHandler.postDelayed(progressUpdater, delayMs);
        }
    }

    private void removeUpdater() {
        if (progressUpdater != null)
            progressHandler.removeCallbacks(progressUpdater);
    }

    @Override
    public void onVideoSizeChanged(VideoSize videoSize) {
        if (mUpdateListener != null) {
            mUpdateListener.onFirstTimeUpdate(player.getDuration(), player.getCurrentPosition());
        }
    }

    public void seekTo(long position) {
        player.seekTo(position);
    }

    public void setUpdateListener(OnProgressUpdateListener updateListener) {
        mUpdateListener = updateListener;
    }


    public interface OnProgressUpdateListener {
        void onProgressUpdate(long currentPosition, long duration, long bufferedPosition);

        void onFirstTimeUpdate(long duration, long currentPosition);
    }
}
