package com.meetfriend.app.ui.music

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.meetfriend.app.R
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.databinding.ActivityTrimMusicBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.camerakit.utils.MIME_TYPE_IMAGE_JPEG
import com.meetfriend.app.ui.camerakit.utils.MIME_TYPE_VIDEO_MP4
import com.meetfriend.app.ui.music.view.AudioWaveAdapter
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.math.ceil
import kotlin.math.pow

class TrimMusicActivity : BasicActivity() {

    private var mediaType: String = MIME_TYPE_IMAGE_JPEG
    private var downloadId: Long? = null

    private lateinit var binding: ActivityTrimMusicBinding
    private var videoPath: String = ""
    private lateinit var videoPlayer: SimpleExoPlayer
    private lateinit var audioPlayer: SimpleExoPlayer
    private var handler: Handler? = null
    var startPosition: Int = 0
    var endPosition: Int = 0
    private lateinit var musicResponse: MusicInfo
    private var fileName: String = ""
    private var audioPath: String = ""
    private var videoDuration: Int = 0

    private var mp3Uri: File? = null
    private var durationSet: Boolean = false

    companion object {
        private const val INTENT_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"
        private const val INTENT_EXTRA_AUDIO_PATH = "INTENT_EXTRA_AUDIO_PATH"
        private const val INTENT_EXTRA_MEDIA_TYPE = "INTENT_EXTRA_MEDIA_TYPE"

        private const val TAG = "TrimMusicActivity"

        fun getIntent(
            context: Context, mediaType: String, videoPath: String, songFile: MusicInfo
        ): Intent {
            val intent = Intent(context, TrimMusicActivity::class.java)
            intent.putExtra(INTENT_EXTRA_MEDIA_TYPE, mediaType)
            intent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
            intent.putExtra(INTENT_EXTRA_AUDIO_PATH, songFile)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trim_music)

        binding = ActivityTrimMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handler = Handler()
        videoPath = intent?.getStringExtra(INTENT_EXTRA_VIDEO_PATH) ?: ""
        musicResponse = intent?.getParcelableExtra(INTENT_EXTRA_AUDIO_PATH) ?: return
        mediaType = intent?.getStringExtra(INTENT_EXTRA_MEDIA_TYPE) ?: MIME_TYPE_IMAGE_JPEG
        val downloadUrl = musicResponse.downloadUrl?.lastOrNull()?.url ?: ""
        fileName = getFileNameFromUri(downloadUrl)

        initUI()

        if (mediaType == MIME_TYPE_VIDEO_MP4) {
            initPlayer()
        } else {
            initImage()
        }
    }

    private fun initImage() {
        audioPlayer = SimpleExoPlayer.Builder(this).build()
        binding.playerView.isVisible = false
        binding.imagePreview.isVisible = true
        videoDuration = 15000
        binding.imagePreview.post {
            Glide.with(this).load(videoPath).listener(object : RequestListener<Drawable> {

                override fun onLoadFailed(
                    e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean
                ): Boolean {
                    finish()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable, model: Any, target: Target<Drawable>, dataSource: com.bumptech.glide.load.DataSource, isFirstResource: Boolean
                ): Boolean {
                    startPostponedEnterTransition()
                    binding.imagePreview.setBackgroundColor(Color.BLACK)
                    return false
                }
            })
                .into(binding.imagePreview)
        }
    }


    private fun initPlayer() {
        try {
            binding.playerView.isVisible = true
            binding.imagePreview.isVisible = false
            videoPlayer = SimpleExoPlayer.Builder(this).build()
            audioPlayer = SimpleExoPlayer.Builder(this).build()
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.let { Integer.valueOf(it) } ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.let { Integer.valueOf(it) } ?: 0
            retriever.release()

            binding.playerView.resizeMode = if (width > height) {
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            binding.playerView.player = videoPlayer
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE) // Use CONTENT_TYPE_DEFAULT instead of CONTENT_TYPE_MOVIE
                .build()

            videoPlayer.setAudioAttributes(audioAttributes, true)

            if (videoPath.isNotEmpty()) {
                val duration = getFormattedVideoDuration(videoPath)
                videoDuration = getVideoDuration(videoPath).toInt()
                binding.durationTxt.text = duration
                buildMediaSource(Uri.parse(videoPath))
            } else {
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildMediaSource(mUri: Uri) {
        try {
            val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(this, resources.getString(R.string.app_name))
            val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mUri))
            val loopingMediaSource = LoopingMediaSource(mediaSource)
            videoPlayer.setMediaSource(loopingMediaSource)
            videoPlayer.prepare()
            videoPlayer.volume = 0.0f
        } catch (e: java.lang.Exception) {
            Timber.tag(TAG).i("Error : $e")
        }
    }

    private fun getFormattedVideoDuration(videoPath: String?): String {
        val videoDuration = getVideoDuration(videoPath)

        // Convert milliseconds to HH:MM:ss format
        var seconds = videoDuration / 1000
        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        seconds %= 60

        return when {
            hours > 0 -> String.format(
                Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds
            )

            minutes > 0 -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            else -> String.format(Locale.getDefault(), "%02d", seconds)
        }
    }

    private fun getVideoDuration(videoPath: String?): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        val durationString: String = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toString()
        retriever.release()
        return durationString.toLong()
    }

    private fun getArtistsNames(): String? {
        return musicResponse.artists?.all?.joinToString(separator = ", ") { it?.name ?: "" }
    }

    private fun initUI() {

        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadAudio()
        binding.progressBar.visibility = View.VISIBLE
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.audioWaveView.layoutManager = layoutManager

        Glide.with(this).load(musicResponse.image?.lastOrNull()?.url).transition(DrawableTransitionOptions.withCrossFade()).into(binding.ivProfile)

        binding.musicTitleAppCompatTextView.text = musicResponse.name
        binding.singerNameAppCompatTextView.text = getArtistsNames()

        binding.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?, progress: Int, fromUser: Boolean
            ) {
                if (fromUser) {
                    if (mediaType == MIME_TYPE_VIDEO_MP4) {
                        videoPlayer.seekTo(0)
                    }
                    val itemCountLayout = layoutManager.itemCount
                    val scrollToPosition = ((progress.toFloat() / 100) * (itemCountLayout - 1)).toInt()
                    layoutManager.scrollToPosition(scrollToPosition)

                    if (itemCountLayout > 0) {
                        val totalDuration = audioPlayer.duration
                        startPosition = ((progress.toFloat() / 100) * totalDuration).toInt()

                        val audioPlayerLength = audioPlayer.currentPosition.toString().length
                        // Update audio playback position based on the scroll
                        val newIntVideoDuration = videoDuration / 1000
                        if (audioPlayerLength >= 3) {
                            startPosition -= calculateAdjustment(
                                audioPlayerLength, newIntVideoDuration
                            )
                        } else {
                            if (audioPlayerLength == 2) {
                                if (startPosition >= 20) {
                                    startPosition -= newIntVideoDuration
                                } else {
                                    startPosition
                                }
                            }
                        }
                        endPosition = (startPosition + videoDuration).coerceAtMost(totalDuration.toInt())
                        audioPlayer.seekTo(startPosition.toLong())
                        audioPlayer.play()
                        binding.pauseButton.visibility = View.VISIBLE
                        binding.playButton.visibility = View.GONE
                        handler?.postDelayed(object : Runnable {
                            override fun run() {
                                try {
                                    // Ensure audioPlayer is not null and is prepared before accessing its currentPosition
                                    if (audioPlayer.isPlaying) {
                                        val currentPos = audioPlayer.currentPosition
                                        if (currentPos >= endPosition) {
                                            audioPlayer.pause()
                                            binding.playButton.visibility = View.VISIBLE
                                            binding.pauseButton.visibility = View.INVISIBLE
                                        } else {
                                            handler?.postDelayed(
                                                this, 100
                                            ) // Check every 100 milliseconds
                                        }
                                    } else {
                                        // Handle the case where audioPlayer is not playing or in an invalid state
                                        // You might want to release or reset the audioPlayer in this case
                                    }
                                } catch (e: IllegalStateException) {
                                    // Handle IllegalStateException, log it, or take appropriate action
                                    Timber.e(e, "Error in handler postDelayed")
                                }
                            }
                        }, 100)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Do nothing for now
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Do nothing for now
            }
        })

        binding.audioWaveView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Update the SeekBar progress based on the current scroll position
                if (mediaType == MIME_TYPE_VIDEO_MP4) {
                    videoPlayer.seekTo(0)
                }
                val itemCountLayout = layoutManager.itemCount
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (itemCountLayout > 0) {
                    val progress = ceil((firstVisibleItem.toFloat() / (itemCountLayout - 1)) * 100).toInt()

                    if (progress in 95..100) {
                        binding.audioSeekBar.progress = 100
                    } else {
                        binding.audioSeekBar.progress = progress
                    }

                    val totalDuration = audioPlayer.duration
                    startPosition = ((progress.toFloat() / 100) * totalDuration).toInt()

                    val audioPlayerLength = audioPlayer.currentPosition.toString().length
                    // Update audio playback position based on the scroll
                    val newIntVideoDuration = videoDuration / 1000
                    if (audioPlayerLength >= 3) {
                        startPosition -= calculateAdjustment(audioPlayerLength, newIntVideoDuration)
                    } else {
                        if (audioPlayerLength == 2) {
                            if (startPosition >= 20) {
                                startPosition -= newIntVideoDuration
                            } else {
                                startPosition
                            }
                        }
                    }
                    endPosition = (startPosition + videoDuration).coerceAtMost(totalDuration.toInt())

                    audioPlayer.seekTo(startPosition.toLong())
                    audioPlayer.play()
                    binding.pauseButton.visibility = View.VISIBLE
                    binding.playButton.visibility = View.GONE

                    handler?.postDelayed(object : Runnable {
                        override fun run() {
                            try {
                                // Ensure audioPlayer is not null and is prepared before accessing its currentPosition
                                if (audioPlayer.isPlaying) {
                                    val currentPos = audioPlayer.currentPosition
                                    if (currentPos >= endPosition) {
                                        audioPlayer.pause()
                                        audioPlayer.seekTo(startPosition.toLong())
                                        audioPlayer.play()
                                    } else {
                                        handler?.postDelayed(
                                            this, 100
                                        ) // Check every 100 milliseconds
                                    }
                                } else {
                                    // Handle the case where audioPlayer is not playing or in an invalid state
                                    // You might want to release or reset the audioPlayer in this case
                                }
                            } catch (e: IllegalStateException) {
                                // Handle IllegalStateException, log it, or take appropriate action
                            }
                        }
                    }, 100)

                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        audioPlayer.seekTo(startPosition.toLong())
                        audioPlayer.play()
                        binding.pauseButton.visibility = View.VISIBLE
                        binding.playButton.visibility = View.GONE
                        handler?.postDelayed(object : Runnable {
                            override fun run() {
                                try {
                                    // Ensure audioPlayer is not null and is prepared before accessing its currentPosition
                                    if (audioPlayer.isPlaying) {
                                        val currentPos = audioPlayer.currentPosition
                                        if (currentPos >= endPosition) {
                                            audioPlayer.pause()
                                            audioPlayer.seekTo(startPosition.toLong())
                                            audioPlayer.play()
                                        } else {
                                            handler?.postDelayed(
                                                this, 100
                                            ) // Check every 100 milliseconds
                                        }
                                    } else {
                                        // Handle the case where audioPlayer is not playing or in an invalid state
                                        // You might want to release or reset the audioPlayer in this case
                                    }
                                } catch (e: IllegalStateException) {
                                    // Handle IllegalStateException, log it, or take appropriate action
                                }
                            }
                        }, 100)
                    }
                }
            }
        })

        binding.playButton.setOnClickListener {
            pause()
            if (mediaType == MIME_TYPE_VIDEO_MP4) {
                videoPlayer.seekTo(0)
            }
            audioPlayer.seekTo(startPosition.toLong())
            audioPlayer.play()
            if (mediaType == MIME_TYPE_VIDEO_MP4) {
                videoPlayer.play()
            }
            handler?.postDelayed(object : Runnable {
                override fun run() {
                    val currentPos = audioPlayer.currentPosition
                    if (currentPos >= endPosition) {
                        audioPlayer.pause()
                        play()
                    } else {
                        handler?.postDelayed(this, 100) // Check every 100 milliseconds
                    }
                }
            }, 100)
        }

        binding.pauseButton.setOnClickListener {
            play()
            audioPlayer.pause()
            if (mediaType == MIME_TYPE_VIDEO_MP4) {
                videoPlayer.pause()
            }
        }

        binding.btnAdd.throttleClicks().subscribeAndObserveOnMainThread {
            binding.progress.isVisible = true
            audioPlayer.pause()
            if (mediaType == MIME_TYPE_VIDEO_MP4) {
                videoPlayer.pause()
            }
            downloadTrimAudio()
        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

    }

    private fun pause() {
        binding.pauseButton.visibility = View.VISIBLE
        binding.playButton.visibility = View.GONE
    }

    private fun play() {
        binding.pauseButton.visibility = View.GONE
        binding.playButton.visibility = View.VISIBLE
    }

    private fun getFileNameFromUri(uriString: String): String {
        val uri = Uri.parse(uriString)
        val path = uri.lastPathSegment
        return path ?: ""
    }

    private fun calculateAdjustment(length: Int, newIntVideoDuration: Int): Int {
        return newIntVideoDuration * 10.0.pow((length - 3).coerceAtLeast(0)).toInt()
    }

    object Utils {
        fun getConvertedFile(directoryPath: String, fileName: String): File {
            val directory = File(directoryPath)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            return File(directory, fileName)
        }
    }

    private fun downloadAudio() {
        val downloadUrl = musicResponse.downloadUrl?.lastOrNull()?.url ?: ""
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
        request.setTitle("Downloading $fileName")
        request.setDescription("Downloading $fileName")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val destinationUri = Uri.fromFile(File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/Meeturfriends")), fileName))
        request.setDestinationUri(destinationUri)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        Timber.tag("TrimMusicActivity").i("downloadId :$downloadId")
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //Fetching the download id received with the broadcast
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            Timber.tag("TrimMusicActivity").i("id :$id")
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadId == id) {
                val mp4UriAfterTrim = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/Meeturfriends"))?.let {
                    Utils.getConvertedFile(
                        it.absolutePath, fileName
                    )
                }
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(mp4UriAfterTrim?.path.toString()))
                    audioPlayer.setMediaItem(mediaItem)
                    audioPlayer.prepare()
                    audioPlayer.addListener(object : Player.Listener {
                        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                            if (playbackState == ExoPlayer.STATE_READY && !durationSet) {
                                binding.progressBar.visibility = View.GONE
                                if (!audioPlayer.isPlaying) {
                                    val duration = audioPlayer.duration
                                    val granularity = 1000
                                    val itemCount = (duration / granularity).toInt()
                                    val itemList = MutableList(itemCount) {}
                                    binding.audioWaveView.adapter = AudioWaveAdapter(itemList)
                                    audioPlayer.playWhenReady = true
                                    if (mediaType == MIME_TYPE_VIDEO_MP4) {
                                        videoPlayer.playWhenReady = true
                                    }
                                }
                                durationSet = true
                            }
                        }
                    })
                } catch (_: java.lang.Exception) {
                }
            }
        }
    }

    private fun downloadTrimAudio() {
        showLoading(true)
        val outputFileName = "${fileName}_trimmed_audio.mp4"

        val fileUri = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/Meeturfriends")), fileName)

        if (!fileUri.exists()) {
            return
        }

        val dirUri = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/Meeturfriends")), fileName)
        if (!dirUri.exists()) {
            dirUri.mkdir()
        }


        mp3Uri = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/Meeturfriends"))?.let {
            Utils.getConvertedFile(
                it.absolutePath, outputFileName
            )
        }

        val startTimeInSeconds = startPosition / 1000.0
        val endTimeInSeconds = endPosition / 1000.0

        val cmd = mp3Uri?.let {
            arrayOf(
                "-y",
                "-i", fileUri.absolutePath,
                "-ss", "${startTimeInSeconds.toInt()}",
                "-to", "${endTimeInSeconds.toInt()}",
                "-c", "copy",
                it.path
            )
        }

        Thread {
            val result: Int = FFmpeg.execute(cmd)
            when (result) {
                0 -> {
                    audioPath = mp3Uri?.path.toString()
                    showLoading(false)
                    val intent = Intent()
                    intent.putExtra("AUDIO_PATH", audioPath)
                    intent.putExtra("MUSIC_INFO", musicResponse)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
                255 -> {
                    showLoading(false)
                    binding.progress.isVisible = false
                }
                else -> {
                    showLoading(false)
                    binding.progress.isVisible = false
                }
            }
        }.start()
    }
    override fun onStop() {
        super.onStop()
        audioPlayer.pause()
    }

    override fun onPause() {
        super.onPause()
        audioPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
        if (mediaType == MIME_TYPE_VIDEO_MP4) {
            videoPlayer.release()
        }
    }
}