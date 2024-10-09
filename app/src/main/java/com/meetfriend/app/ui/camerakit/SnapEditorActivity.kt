package com.meetfriend.app.ui.camerakit

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.Editable
import android.text.Html
import android.util.Log
import android.util.Size
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.FrameLayout.LayoutParams
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.LogMessage
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.meetfriend.app.R
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.StoryUploadData
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.api.post.model.FontDetails
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.api.post.model.LinkAttachmentDetails
import com.meetfriend.app.api.post.model.LinkAttachmentState
import com.meetfriend.app.api.post.model.MultipleImageDetails
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.application.MeetFriendApplication.Companion.context
import com.meetfriend.app.databinding.ActivitySnapEditorBinding
import com.meetfriend.app.ffmpeg.FFmpegCallBack
import com.meetfriend.app.ffmpeg.FFmpegUtils
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.camerakit.SnapkitActivity.Companion.IS_DELETE_VIDEO
import com.meetfriend.app.ui.camerakit.attachment.LinkAttachmentBottomSheetFragment
import com.meetfriend.app.ui.camerakit.stickers.StickerBottomSheet
import com.meetfriend.app.ui.camerakit.utils.MIME_TYPE_IMAGE_JPEG
import com.meetfriend.app.ui.camerakit.utils.MIME_TYPE_VIDEO_MP4
import com.meetfriend.app.ui.camerakit.utils.TextAttributes
import com.meetfriend.app.ui.camerakit.videoplayer.event.OnProgressVideoEvent
import com.meetfriend.app.ui.camerakit.videoplayer.event.OnRangeSeekBarEvent
import com.meetfriend.app.ui.camerakit.videoplayer.event.OnVideoEditedEvent
import com.meetfriend.app.ui.camerakit.videoplayer.utils.TrimVideoUtils
import com.meetfriend.app.ui.camerakit.videoplayer.view.RangeSeekBarView
import com.meetfriend.app.ui.camerakit.videoplayer.view.Thumb
import com.meetfriend.app.ui.camerakit.view.FontPickerAdapter
import com.meetfriend.app.ui.camerakit.view.MultipleMediaAdapter
import com.meetfriend.app.ui.challenge.CreateChallengeActivity
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewModel
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewState
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.music.AddMusicActivity
import com.meetfriend.app.utilclasses.download.DownloadService
import com.meetfriend.app.utils.FileUtils
import com.meetfriend.app.utils.penview.DrawView
import com.rtugeek.android.colorseekbar.ColorSeekBar
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.vrgsoft.videcrop.VideoCropActivity
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.*
import java.lang.Math.sqrt
import java.lang.ref.WeakReference
import java.net.URI
import java.net.URL
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SnapEditorActivity : BasicActivity(), OnVideoEditedEvent {

    companion object {
        const val THREE = 3
        const val FOUR = 4
        const val FIVE = 5
        const val SIX = 6
        const val SEVEN = 7
        const val TEN_DURATION = 10L
        const val THOUSAND = 1000
        const val TEN_THOUSAND = 10000
        const val THOUSAND_DURATION = 1000L
        const val ONE_HUNDRED_DURATION = 100L
        const val ONE_HUNDRED = 100
        private const val clickThreshold = 10 // Minimum movement in pixels to distinguish a drag from a click
        const val CROP_REQUEST = 200
        var colorCodeTextView: Int = -65512
        const val DEFOULT_TEXT_COLOR_CODE = -65512
        const val INTENT_VIDEO_PATH = "path"
        private const val STICKER = "STICKER"
        private const val EMOJI = "EMOJI"
        private const val CROP = "CROP"
        private const val TEXT = "TEXT"
        private const val PEN = "PEN"
        private const val BLUR = "ERASE"
        private const val TEXT_ALIGN_CENTER = "TEXT_ALIGN_CENTER"
        private const val TEXT_ALIGN_RIGHT = "TEXT_ALIGN_RIGHT"
        private const val TEXT_ALIGN_LEFT = "TEXT_ALIGN_LEFT"
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val BUNDLE_ARG_PLAYBACK_SPEED = "playback_speed"
        private const val BUNDLE_MIME_TYPE = "mime_type"
        var IS_SHORTS = "IS_SHORTS"
        var IS_CHALLENGE = "IS_CHALLENGE"
        var IS_STORY = "IS_STORY"
        var IS_CLIP = "IS_CLIP"
        private const val TAG = "SnapEditorActivity"
        private const val LINE_DRAWING = 0
        private const val EMOJIS_DRAWING = 1
        const val LIST_OF_MULTIPLE_MEDIA = "LIST_OF_MULTIPLE_MEDIA"
        const val IS_FROM_PREVIEW = "IS_FROM_PREVIEW"
        private const val EXPORT_NONE = 0
        const val ADD_MUSIC_REQUEST_CODE = 1852
        private const val EXPORT_ONLY_TRIM = 1
        private const val EXPORT_ONLY_ADDED_VIEW = 2
        private const val EXPORT_TRIM_AND_ADDED_VIEW_BOTH = 3
        private const val SHOW_PROGRESS = 2
        const val INTENT_TAG_NAME = "INTENT_TAG_NAME"

        private var finalX: Float = 40.0f
        private var finalY: Float = 891.0f
        private var lastTextSize = 40f

        fun getIntent(
            context: Context,
            path: String,
            mimeType: String,
            playBackSpeed: Float,
            isShorts: Boolean = false,
            isChallenge: Boolean = false,
            isStory: Boolean = false,
            isClip: Boolean = false,
            isFromPreview: Boolean = false,
            listOfMultipleMedia: ArrayList<MultipleImageDetails>,
            tagName: String? = null
        ): Intent {
            val intent = Intent(context, SnapEditorActivity::class.java)
            intent.putExtra(INTENT_VIDEO_PATH, path)
            intent.putExtra(BUNDLE_MIME_TYPE, mimeType)
            intent.putExtra(BUNDLE_ARG_PLAYBACK_SPEED, playBackSpeed)
            intent.putExtra(IS_SHORTS, isShorts)
            intent.putExtra(IS_CHALLENGE, isChallenge)
            intent.putExtra(IS_STORY, isStory)
            intent.putExtra(IS_CLIP, isClip)
            intent.putExtra(IS_FROM_PREVIEW, isFromPreview)
            intent.putParcelableArrayListExtra(LIST_OF_MULTIPLE_MEDIA, listOfMultipleMedia)
            if (!tagName.isNullOrEmpty()) intent.putExtra(INTENT_TAG_NAME, tagName)
            return intent
        }
    }

    private var linkAttachmentDetails: LinkAttachmentDetails? = null
    lateinit var binding: ActivitySnapEditorBinding
    private var playBackSpeed = 1f

    private var mimeType = MIME_TYPE_IMAGE_JPEG
    private var isChallenge: Boolean = false
    private var isShorts: Boolean = false
    private var isStory: Boolean = false
    private var isClip: Boolean = false
    private var isFromPreview: Boolean = false
    private var isClickText: Boolean = false
    private var isClickPen: Boolean = false
    private var emojisIndex: Int? = 0
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var isClickEmojis: Boolean = false
    private var fontId: Int = R.font.american_typewriter
    private var fontList: ArrayList<FontDetails> = arrayListOf()
    private var listOfRemoveCount: ArrayList<Int> = arrayListOf()
    private var listOfDrawingView: ArrayList<Int> = arrayListOf()
    private var textAlignPosition: String = TEXT_ALIGN_CENTER
    private var currentTextView: TextView? = null
    private var isBackGroundAdd = false
    private var ivStrikethroughClicked = false

    private var shadowLayerRadius: Float = 0f
    private var shadowLayerDx: Float = 0f
    private var shadowLayerDy: Float = 0f
    private var shadowLayerColor: Int = 0
    private var backgroundAdded = false
    private var strikethroughApplied = false
    private var selectedItemAddText: TextAttributes? = null

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var isDragging: Boolean = false
    private val texts = mutableListOf<TextAttributes>()

    private var selectedItem: String? = null
    private val drawnEmojis = mutableListOf<Pair<Float, Float>>()
    private val emojiViews = mutableListOf<ImageView>()
    private var cloudFlareConfig: CloudFlareConfig? = null
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var duration: Long = 0L
    private var mediaUrl = ""
    private var outputMediaUrl = ""
    private var cropVideoPath = ""
    private var lastX = -1f
    private var lastY = -1f
    private var lastEvent: FloatArray? = null
    private var d = 0f
    private var isZoomAndRotate = false
    private var isOutSide = false
    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    private var xCoOrdinate = 0f
    private var yCoOrdinate = 0f

    private lateinit var stickerBottomSheet: StickerBottomSheet
    private var listOfMultipleMedia: ArrayList<MultipleImageDetails>? = arrayListOf()
    private lateinit var multipleMediaAdapter: MultipleMediaAdapter
    private var lastView: View? = null
    private var lastWidth: Float = 0.0f
    private var lastHeight: Float = 0.0f
    private var lastRotation: Float = 0f
    private var lastMode = NONE
    private var attchmentUrl = ""
    private var lastShadowColor = 0
    private var count = 0
    private var totalDuration: Int? = null
    private var startDuration: Int? = null
    private var endDuration: Int? = null
    private var savedDrawing: Bitmap? = null

    lateinit var mPlayer: ExoPlayer
    private lateinit var mSrc: Uri

    private var mMaxDuration: Int = -1
    private var mMinDuration: Int = -1
    private var mListeners: ArrayList<OnProgressVideoEvent> = ArrayList()

    private var mOnVideoEditedListener: OnVideoEditedEvent? = null

    private var mDuration: Long = 0L
    private var mTimeVideo = 0L
    private var mStartPosition = 0L

    private var mEndPosition = 0L
    private var mResetSeekBar = false
    private val mMessageHandler = MessageHandler(this)
    private var originalVideoWidth: Int = 0
    private var originalVideoHeight: Int = 0
    private var videoPlayerWidth: Int = 0
    private var videoPlayerHeight: Int = 0
    private var isVideoPrepared = false
    private var musicResponse: MusicInfo? = null
    private var audioPath: String = ""

    private var tagName: String? = null

    val videoFileName: String
        get() {
            val rnds = (0..TEN_THOUSAND).random()

            return "video_${System.currentTimeMillis()}_$rnds.mp4"
        }

    private var shapeDrawable: GradientDrawable? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddNewPostViewModel>
    private lateinit var deepArViewModel: AddNewPostViewModel

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySnapEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MeetFriendApplication.component.inject(this@SnapEditorActivity)
        deepArViewModel = getViewModelFromFactory(viewModelFactory)
        stickerBottomSheet = StickerBottomSheet(this@SnapEditorActivity)
        playBackSpeed = intent?.getFloatExtra(BUNDLE_ARG_PLAYBACK_SPEED, 1f) ?: 1f
        mimeType = intent?.getStringExtra(BUNDLE_MIME_TYPE) ?: MIME_TYPE_IMAGE_JPEG
        isShorts = intent?.getBooleanExtra(IS_SHORTS, false) ?: false
        isChallenge = intent?.getBooleanExtra(IS_CHALLENGE, false) ?: false
        isStory = intent?.getBooleanExtra(IS_STORY, false) ?: false
        isClip = intent?.getBooleanExtra(IS_CLIP, false) ?: false
        isFromPreview = intent?.getBooleanExtra(IS_FROM_PREVIEW, false) ?: false
        listOfMultipleMedia = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(
                LIST_OF_MULTIPLE_MEDIA, MultipleImageDetails::class.java
            ) ?: arrayListOf()
        } else {
            intent.getParcelableArrayListExtra(LIST_OF_MULTIPLE_MEDIA) ?: arrayListOf()
        }
        if (intent?.hasExtra(INTENT_TAG_NAME) == true) {
            tagName = intent?.getStringExtra(INTENT_TAG_NAME)
        }
        mediaUrl = intent.getStringExtra(INTENT_VIDEO_PATH) ?: return
        linkAttachmentDetails = LinkAttachmentDetails()
        multipleMediaAdapter = MultipleMediaAdapter(this, false)
        linkAttachmentDetails = LinkAttachmentDetails()
        if (mimeType == MIME_TYPE_VIDEO_MP4) {
            totalDuration = getVideoDuration(mediaUrl.toUri()).toInt()
        }
        if (isOnline(this) && isStory) {
            deepArViewModel.getCloudFlareConfig(false)
        } else if (isStory) {
            showToast("No internet connection")
        }
        if (mimeType == MIME_TYPE_IMAGE_JPEG) {
            binding.ivAttachLink.isVisible = true
        } else {
            binding.ivAttachLink.isVisible = !isClip
            binding.ivSounds.isVisible = !isClip
        }

        initializeFontList()
        listenToViewModel()
        setUpListeners()
        setUpMargins()
        setupMediaIfNeeded(mediaUrl)
        if (mimeType == MIME_TYPE_IMAGE_JPEG) {
            if (isStory || isChallenge) {
                binding.tvDone.text = resources.getString(R.string.label_upload)
            }
        }
        listenToViewEvents()
        if (!isClip && mimeType == MIME_TYPE_VIDEO_MP4) {
            binding.timeLineFrame.visibility = View.GONE
            marginBottom()
            binding.deleteAppCompatImageView.isVisible = false
            binding.rvMultipleMedia.isVisible = true
            binding.tvAdd.isVisible = true
            binding.tvDone.text = resources.getString(R.string.label_upload)
            binding.rvMultipleMedia.apply {
                adapter = multipleMediaAdapter
                layoutManager = LinearLayoutManager(this@SnapEditorActivity, RecyclerView.HORIZONTAL, false)
            }
            multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
        }
        if (isClip && mimeType == MIME_TYPE_VIDEO_MP4) {
            binding.tvDone.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = resources.getDimensionPixelSize(R.dimen._10sdp)
            }
        }
    }

    // Function to update the highlight view position and size
    private fun updateHighlightView(touchY: Float, emojiDrawableResId: Int, emojiView: View) {
        val highlightView = binding.highlightView
        val layoutParams = highlightView.layoutParams as RelativeLayout.LayoutParams
        layoutParams.topMargin = (touchY - 160).toInt()
        highlightView.layoutParams = layoutParams

        // Set the drawable of the highlight view
        binding.highlightViewEmoji.setImageResource(emojiDrawableResId)
        highlightView.visibility = View.VISIBLE

        // Reset backgrounds for all emoji views to null
        resetEmojiBackgrounds()

        // Set the background of the touched emoji
        emojiView.background = ContextCompat.getDrawable(emojiView.context, R.drawable.rounded_button_selected)

        // Update emojisIndex based on the selected emoji view
        when (emojiView.id) {
            R.id.smileEmoji -> emojisIndex = 0
            R.id.heartEmoji -> emojisIndex = 1
            R.id.happyEmoji -> emojisIndex = 2
            R.id.fireEmoji -> emojisIndex = THREE
            R.id.kissEmoji -> emojisIndex = FOUR
            R.id.ghostEmoji -> emojisIndex = FIVE
            R.id.cryEmoji -> emojisIndex = SIX
            R.id.hundredEmoji -> emojisIndex = SEVEN
        }
    }

    // Function to reset all emoji backgrounds to default
    private fun resetEmojiBackgrounds() {
        val emojiViews = listOf(
            binding.smileEmoji,
            binding.heartEmoji,
            binding.happyEmoji,
            binding.fireEmoji,
            binding.kissEmoji,
            binding.ghostEmoji,
            binding.cryEmoji,
            binding.hundredEmoji
        )

        emojiViews.forEach { emojiView ->
            emojiView.background = null
        }
    }

    // Function to check if a point is inside a view
    private fun isPointInsideView(x: Float, y: Float, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val viewX = location[0]
        val viewY = location[1]

        return (x >= viewX && x <= viewX + view.width && y >= viewY && y <= viewY + view.height)
    }

    @SuppressLint("ClickableViewAccessibility")
    val touchListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Get the touch coordinates relative to the screen
                val touchX = event.rawX
                val touchY = event.rawY

                // Determine which emoji is being touched
                val emojiViews = listOf(
                    binding.smileEmoji to R.drawable.ic_smile,
                    binding.heartEmoji to R.drawable.ic_heart_emoji,
                    binding.happyEmoji to R.drawable.ic_laugh,
                    binding.fireEmoji to R.drawable.ic_burn,
                    binding.kissEmoji to R.drawable.ic_kiss,
                    binding.ghostEmoji to R.drawable.ic_ghost,
                    binding.cryEmoji to R.drawable.ic_sad,
                    binding.hundredEmoji to R.drawable.ic_hundred
                )

                var touchedView: View? = null
                var emojiDrawableResId = 0

                emojiViews.forEach { (view, drawableResId) ->
                    if (isPointInsideView(touchX, touchY, view)) {
                        emojiDrawableResId = drawableResId
                        touchedView = view
                    }
                }

                // Update the highlight view position, size, and drawable if an emoji is touched
                touchedView?.let { updateHighlightView(touchY, emojiDrawableResId, it) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Hide the highlight view when touch is released
                binding.highlightView.visibility = View.GONE
            }
        }
        true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpListeners() {
        mListeners = ArrayList()
        mListeners.add(object : OnProgressVideoEvent {
            override fun updateProgress(time: Float, max: Long, scale: Long) {
                updateVideoProgress(time.toLong())
            }
        })

        binding.tvPlay.setOnClickListener {
            onClickVideoPlayPause()
        }

        binding.handlerTop.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onPlayerIndicatorSeekChanged(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStart()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                binding.tvPlay.setImageResource(R.drawable.ic_video_play)
                onPlayerIndicatorSeekStop(seekBar)
            }
        })

        binding.timeLineBar.addOnRangeSeekBarListener(object : OnRangeSeekBarEvent {
            override fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) = Unit

            override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                binding.handlerTop.visibility = View.GONE
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) =
                Unit

            override fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })
    }

    private fun onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mPlayer.pause()
        notifyProgressUpdate(false)
    }

    private fun setUpMargins() {
        val marge = binding.timeLineBar.thumbs[0].widthBitmap
        val lp = binding.timeLineView.layoutParams as LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        binding.timeLineView.layoutParams = lp
    }

    private fun updateVideoProgress(time: Long) {
        if (time <= mStartPosition && time <= mEndPosition) {
            binding.handlerTop.visibility = View.GONE
        } else {
            binding.handlerTop.visibility = View.VISIBLE
        }
        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            mPlayer.pause()
            mResetSeekBar = true
            return
        }
        setProgressBarPosition(time)
    }

    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {
        val duration = (mDuration * progress / THOUSAND_DURATION)
        if (fromUser) {
            if (duration < mStartPosition) {
                setProgressBarPosition(mStartPosition)
            } else if (duration > mEndPosition) setProgressBarPosition(mEndPosition)
        }
    }

    private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mPlayer.pause()

        val duration = (mDuration * seekBar.progress / THOUSAND_DURATION).toInt()
        mPlayer.seekTo(duration.toLong())
        notifyProgressUpdate(false)
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = ((mDuration * value / ONE_HUNDRED_DURATION).toLong())
                mPlayer.seekTo(mStartPosition)
            }

            Thumb.RIGHT -> {
                mEndPosition = ((mDuration * value / ONE_HUNDRED_DURATION).toLong())
            }
        }
        setTimeFrames()
        mTimeVideo = mEndPosition - mStartPosition
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mPlayer.pause()
    }

    fun getDurations(): Pair<Int, Int> {
        val startSeconds = (mStartPosition / THOUSAND).toInt()
        val endSeconds = (mEndPosition / THOUSAND).toInt()
        return Pair(startSeconds, endSeconds)
    }

    fun marginBottom() {
        binding.tvPlay.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = resources.getDimensionPixelSize(R.dimen._35sdp)
            marginStart = resources.getDimensionPixelSize(R.dimen._12sdp)
        }
    }

    fun setVideoBackgroundColor(@ColorInt color: Int) = with(binding) {
        binding.videoContainer.setBackgroundColor(color)
        layout.setBackgroundColor(color)
    }

    fun setOnTrimVideoListener(onVideoEditedListener: OnVideoEditedEvent): SnapEditorActivity {
        mOnVideoEditedListener = onVideoEditedListener
        return this
    }

    fun setVideoURI(videoURI: Uri): SnapEditorActivity {
        mSrc = videoURI

        /**
         * setup video player
         */
        mPlayer = ExoPlayer.Builder(context).build()

        val dataSourceFactory = DefaultDataSource.Factory(context)
        val videoSource: MediaSource = ProgressiveMediaSource.Factory(
            dataSourceFactory
        ).createMediaSource(MediaItem.fromUri(videoURI))

        mPlayer.setMediaSource(videoSource)
        mPlayer.prepare()
        mPlayer.playWhenReady = true // Start playback automatically

        binding.videoLoader.also {
            it.player = mPlayer
            it.useController = false
        }
        mPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                mOnVideoEditedListener?.onError("Something went wrong reason : ${error.localizedMessage}")
            }

            @SuppressLint("UnsafeOptInUsageError")
            override fun onVideoSizeChanged(videoSize: com.google.android.exoplayer2.video.VideoSize) {
                if (videoSize.width > videoSize.height) {
                    binding.videoLoader.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                } else {
                    binding.videoLoader.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    mPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }

                onVideoPrepared(mPlayer)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    // Video has ended, seek back to the start
                    mResetSeekBar = true
                    mPlayer.seekTo(mStartPosition)
                    binding.handlerTop.visibility = View.VISIBLE
                    setProgressBarPosition(0)
                    binding.tvPlay.setImageResource(R.drawable.baseline_pause_circle_outline_24)
                    // You might want to start playing the video again here if needed
                    mPlayer.play()
                }
            }
        })

        binding.videoLoader.requestFocus()
        binding.timeLineView.setVideo(mSrc)
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, mSrc)
        val metaDateWidth = mediaMetadataRetriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
        )?.toInt() ?: 0
        val metaDataHeight = mediaMetadataRetriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
        )?.toInt() ?: 0

        // If the rotation is 90 or 270 the width and height will be transposed.
        when (mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt()) {
            90, 270 -> {
                originalVideoWidth = metaDataHeight
                originalVideoHeight = metaDateWidth
            }
            else -> {
                originalVideoWidth = metaDateWidth
                originalVideoHeight = metaDataHeight
            }
        }

        return this
    }

    private fun setProgressBarPosition(position: Long) {
        if (mDuration > 0) binding.handlerTop.progress = (THOUSAND_DURATION * position / mDuration).toInt()
    }

    private fun onVideoPrepared(mp: ExoPlayer) {
        if (isVideoPrepared) return
        isVideoPrepared = true
        val videoWidth = mp.videoSize.width
        val videoHeight = mp.videoSize.height
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
        val screenWidth = binding.layoutSurfaceView.width
        val screenHeight = binding.layoutSurfaceView.height
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
        val lp = binding.videoLoader.layoutParams

        if (videoProportion > screenProportion) {
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.height = screenHeight
        }
        videoPlayerWidth = lp.width
        videoPlayerHeight = lp.height
        binding.videoLoader.layoutParams = lp

        mDuration = mPlayer.duration
        setSeekBarPosition()
        setTimeFrames()
    }

    private fun setSeekBarPosition() {
        when {
            mDuration >= mMaxDuration && mMaxDuration != -1 -> {
                mStartPosition = mDuration / 2 - mMaxDuration / 2
                mEndPosition = mDuration / 2 + mMaxDuration / 2
                binding.timeLineBar.setThumbValue(0, (mStartPosition * ONE_HUNDRED / mDuration))
                binding.timeLineBar.setThumbValue(1, (mEndPosition * ONE_HUNDRED / mDuration))
            }

            mDuration <= mMinDuration && mMinDuration != -1 -> {
                mStartPosition = mDuration / 2 - mMinDuration / 2
                mEndPosition = mDuration / 2 + mMinDuration / 2
                binding.timeLineBar.setThumbValue(0, (mStartPosition * ONE_HUNDRED / mDuration))
                binding.timeLineBar.setThumbValue(1, (mEndPosition * ONE_HUNDRED / mDuration))
            }

            else -> {
                mStartPosition = 0L
                mEndPosition = mDuration
            }
        }
        mPlayer.seekTo(mStartPosition)
        mTimeVideo = mDuration
        binding.timeLineBar.initMaxWidth()
    }

    private fun setTimeFrames() {
        val seconds = context.getString(R.string.short_seconds)
        binding.textTimeSelection.text = String.format(
            Locale.ENGLISH,
            "%s %s - %s %s",
            TrimVideoUtils.stringForTime(mStartPosition),
            seconds,
            TrimVideoUtils.stringForTime(mEndPosition),
            seconds
        )
    }

    private class MessageHandler(view: SnapEditorActivity) : Handler(Looper.getMainLooper()) {
        private val mView: WeakReference<SnapEditorActivity> = WeakReference(view)
        override fun handleMessage(msg: Message) {
            val view = mView.get() ?: return
            view.notifyProgressUpdate(true)
            if (view.binding.videoLoader.player?.isPlaying == true) sendEmptyMessageDelayed(0, TEN_DURATION)
        }
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0L) return
        val position = mPlayer.currentPosition // binding.videoLoader.currentPosition
        if (all) {
            for (item in mListeners) {
                item.updateProgress(position.toFloat(), mDuration, (position * ONE_HUNDRED / mDuration))
            }
        } else {
            mListeners[0].updateProgress(
                position.toFloat(),
                mDuration,
                (position * ONE_HUNDRED / mDuration)
            )
        }
    }

    private fun setupMediaIfNeeded(path: String) {
        binding.ivEraser.isVisible = mimeType == MIME_TYPE_IMAGE_JPEG
        binding.deleteAppCompatImageView.isVisible = isClip && mimeType == MIME_TYPE_VIDEO_MP4

        if (mimeType == MIME_TYPE_VIDEO_MP4) {
            binding.imagePreview.visibility = View.GONE
            binding.layout.visibility = View.VISIBLE
            binding.videoLoader.apply {
                setVideoBackgroundColor(resources.getColor(R.color.white))
                setOnTrimVideoListener(this@SnapEditorActivity)
                setVideoURI(Uri.parse(path))
                setMinDuration(1)
                setPlayBackSpeed(playBackSpeed)
                Handler(Looper.getMainLooper()).postDelayed({
                    onClickVideoPlayPause()
                }, 500)
            }
            val videoSize = getVideoDimensions(path)
            if (videoSize != null) {
                val deviceHeight = resources.displayMetrics.heightPixels
                val deviceWitdh = resources.displayMetrics.widthPixels
                videoHeight = if (videoSize.height > deviceHeight) deviceHeight else videoSize.height
                videoWidth = if (videoSize.width > deviceWitdh) deviceWitdh else videoSize.width
                binding.flContainer.layoutParams?.apply {
                    this.height = videoHeight
                    this.width = videoWidth
                }
            } else {
                Timber.tag(TAG).e("Failed to retrieve video dimisensions.")
            }
        } else if (mimeType == MIME_TYPE_IMAGE_JPEG) {
            binding.videoContainer.visibility = View.GONE
            binding.layout.visibility = View.GONE
            binding.imagePreview.post {
                Glide.with(this).load(path).listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        finish()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        startPostponedEnterTransition()
                        binding.imagePreview.setBackgroundColor(Color.BLACK)
                        imageWidth = resource.intrinsicWidth
                        imageHeight = resource.intrinsicHeight
                        binding.flContainer.layoutParams?.apply {
                            this.height = imageHeight
                            this.width = imageWidth
                        }
                        return false
                    }
                })
                    .into(binding.imagePreview)
            }
        }
    }

    fun getVideoDimensions(videoPath: String): Size? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()

            if (width != null && height != null) {
                Size(width, height)
            } else {
                null
            }
        } catch (e: IOException) {
            Timber.tag("SnapEditorActivity").d("Error:->$e")
            null
        } finally {
            retriever.release()
        }
    }

    fun onClickVideoPlayPause() {
        if (mPlayer.isPlaying) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            mPlayer.pause()
            binding.tvPlay.setImageResource(R.drawable.ic_video_play)
            binding.handlerTop.visibility = View.VISIBLE
        } else {
            if (mResetSeekBar) {
                mResetSeekBar = false
                mPlayer.seekTo(mStartPosition)
                binding.handlerTop.visibility = View.VISIBLE
                setProgressBarPosition(0)
            }
            mResetSeekBar = false
            binding.handlerTop.visibility = View.VISIBLE
            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
            mPlayer.play()
            binding.tvPlay.setImageResource(R.drawable.baseline_pause_circle_outline_24)
        }
    }

    fun setMinDuration(minDuration: Int): SnapEditorActivity {
        mMinDuration = minDuration * THOUSAND
        return this
    }

    fun setPlayBackSpeed(playBackSpeed: Float) {
        mPlayer.setPlaybackSpeed(playBackSpeed)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listenToViewEvents() {
        binding.drawView.setDrawListener(object : DrawView.DrawListener {
            override fun onDrawStart() {}

            override fun onDrawEnd() {
                listOfDrawingView.add(LINE_DRAWING)
            }
        })

        // Set the common touch listener to both ImageView and VideoView
        if (mimeType != MIME_TYPE_IMAGE_JPEG) {
            binding.emojiContainer.setOnTouchListener(commonTouchListener)
        }
        binding.imagePreview.setOnTouchListener(commonTouchListener)

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        binding.tvAdd.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        binding.ivAddImage.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        binding.tvDone.throttleClicks().subscribeAndObserveOnMainThread {
            if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                if (binding.emojiContainer.childCount > 0 || binding.drawView.hasDrawingOccurred()) {
                    val bitmap = getBitmapFromView(binding.flContainer)
                    saveBitmap(bitmap, EXPORT_NONE)
                    runOnUiThread { showLoading() }
                } else {
                    if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                        val taggedView = binding.emojiContainer.findViewById<CardView>(R.id.linkAttachmentCardView) ?: null
                        val screenHeight = resources.displayMetrics.heightPixels
                        val screenWidth = resources.displayMetrics.widthPixels
                        if (taggedView != null) {
                            finalX = taggedView.x
                            finalY = taggedView.y
                            lastHeight = taggedView.height.toFloat()
                            lastWidth = taggedView.width.toFloat()
                            lastRotation = taggedView.rotation
                            window.decorView.post {
                                val insets = window.decorView.rootWindowInsets
                                if (insets != null) {
                                    val statusBarHeight = insets.systemWindowInsetTop
                                    val navigationBarHeight = insets.systemWindowInsetBottom
                                    val heightExcludingBars = screenHeight - statusBarHeight - navigationBarHeight
                                    finalX = (finalX * ONE_HUNDRED) / screenWidth.toFloat()
                                    finalY = (finalY * ONE_HUNDRED) / heightExcludingBars.toFloat()
                                    lastWidth = (lastWidth * ONE_HUNDRED) / screenWidth.toFloat()
                                    lastHeight = (lastHeight * ONE_HUNDRED) / heightExcludingBars.toFloat()
                                    linkAttachmentDetails?.lastHeight = lastHeight
                                    linkAttachmentDetails?.lastWidth = lastWidth
                                    linkAttachmentDetails?.finalY = finalY
                                    linkAttachmentDetails?.finalX = finalX
                                    linkAttachmentDetails?.lastRotation = lastRotation
                                }
                            }
                        }
                    }

                    if (!isShorts && !isChallenge && !isStory) {
                        // only for post case

                        if (musicResponse != null && audioPath.isNotEmpty()) {
                            val savedFile = saveVideoToCacheDir(context, "Edited_video_${System.currentTimeMillis()}")
                            addMusicToImage(mediaUrl, audioPath, savedFile.path)
                        } else {
                            linkAttachmentDetails =
                                LinkAttachmentDetails(finalX, finalY, lastHeight, lastWidth, lastRotation, attachUrl = attchmentUrl)
                            val resultIntent = Intent()
                            resultIntent.putExtra("linkAttachmentDetails", linkAttachmentDetails)
                            resultIntent.putExtra("file", mediaUrl)
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                    } else {
                        if (isClip) {
                            val resultIntent = Intent()
                            linkAttachmentDetails = null
                            resultIntent.putExtra("linkAttachmentDetails", linkAttachmentDetails)
                            resultIntent.putExtra("file", mediaUrl)
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        } else {
                            if (isStory || isChallenge) {
                                linkAttachmentDetails = LinkAttachmentDetails(
                                    finalX, finalY, lastHeight, lastWidth, lastRotation, attachUrl = attchmentUrl
                                )
                                if (!isStory && !isChallenge && !isShorts) {
                                    if (listOfMultipleMedia.isNullOrEmpty()) {
                                        openEditor(Uri.fromFile(File(mediaUrl)))
                                    } else {
                                        listOfMultipleMedia?.let {
                                            openEditor(
                                                Uri.fromFile(
                                                    File(
                                                        it[0].editedImagePath ?: it[0].mainImagePath ?: ""
                                                    )
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    if (musicResponse != null && audioPath.isNotEmpty()) {
                                        if (listOfMultipleMedia.isNullOrEmpty()) {
                                            val savedFile = saveVideoToCacheDir(
                                                context,
                                                "Edited_video_${System.currentTimeMillis()}"
                                            )
                                            addMusicToImage(mediaUrl, audioPath, savedFile.path)
                                        } else {
                                            listOfMultipleMedia?.let {
                                                val imagePath = it[0].editedImagePath ?: it[0].mainImagePath ?: ""

                                                val savedFile = saveVideoToCacheDir(
                                                    context,
                                                    "Edited_video_${System.currentTimeMillis()}"
                                                )
                                                addMusicToImage(
                                                    imagePath,
                                                    audioPath,
                                                    savedFile.path
                                                )
                                            }
                                        }
                                    } else {
                                        if (listOfMultipleMedia.isNullOrEmpty()) {
                                            openEditor(Uri.fromFile(File(mediaUrl)))
                                        } else {
                                            listOfMultipleMedia?.let {
                                                openEditor(
                                                    Uri.fromFile(
                                                        File(
                                                            it[0].editedImagePath ?: it[0].mainImagePath ?: ""
                                                        )
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val (startPosition, endPosition) = getDurations()
                startDuration = startPosition
                endDuration = endPosition
                val hasEmojis = binding.emojiContainer.childCount > 0 || binding.drawView.hasDrawingOccurred()
                val hasStartDuration = startDuration?.let { it > 0 } ?: false
                val hasValidDuration = totalDuration?.let { it > endDuration!! } ?: false
                if (
                    hasEmojis ||
                    hasStartDuration ||
                    hasValidDuration
                ) {
                    showLoading()
                    if (startDuration!! > 0 || totalDuration!! > endDuration!!) {
                        if (binding.emojiContainer.childCount > 0 || binding.drawView.hasDrawingOccurred()) {
                            Timber.tag("export").d("trim and added view")
                            val bitmap = getBitmapFromView(binding.flContainer)
                            saveBitmap(bitmap, EXPORT_TRIM_AND_ADDED_VIEW_BOTH)
                        } else {
                            Timber.tag("export").d("only trim video")
                            val savedFile = saveVideoToCacheDir(context, "Edited_video_${System.currentTimeMillis()}")
                            addTextToVideo(mediaUrl, " ", savedFile.path, EXPORT_ONLY_TRIM)
                        }
                    } else if (binding.emojiContainer.childCount > 0 || binding.drawView.hasDrawingOccurred()) {
                        Timber.tag("export").d("only save with added-view")
                        val bitmap = getBitmapFromView(binding.flContainer)
                        saveBitmap(bitmap, EXPORT_ONLY_ADDED_VIEW)
                    } else {
                        Timber.tag("export").d("trim and added view")
                    }
                } else {
                    Timber.tag("export").d("no need to save video")

                    if (isClip) {
                        val resultIntent = Intent()
                        linkAttachmentDetails = null
                        resultIntent.putExtra("linkAttachmentDetails", linkAttachmentDetails)
                        resultIntent.putExtra("file", mediaUrl)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        if (!isStory && !isChallenge && !isShorts) {
                            if (mimeType == MIME_TYPE_VIDEO_MP4) {
                                if (musicResponse != null && audioPath.isNotEmpty()) {
                                    mergeVideoAndAudio(mediaUrl, audioPath)
                                } else {
                                    openEditor(Uri.fromFile(File(mediaUrl)))
                                }
                            }
                        } else {
                            if (musicResponse != null && audioPath.isNotEmpty()) {
                                if (listOfMultipleMedia.isNullOrEmpty()) {
                                    val dirPathFile = File(
                                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                            .toString() + "/Meeturfriends/Video"
                                    )
                                    // Create the storage directory if it does not exist
                                    if (!dirPathFile.exists()) {
                                        dirPathFile.mkdirs()
                                    }
                                    mergeVideoAndAudio(mediaUrl, audioPath)
                                } else {
                                    listOfMultipleMedia?.let {
                                        val dirPathFile = File(
                                            Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DOWNLOADS
                                            ).toString() + "/Meeturfriends/Video"
                                        )
                                        val imagePath = it[0].editedVideoPath ?: it[0].mainVideoPath ?: ""
                                        if (!dirPathFile.exists()) {
                                            dirPathFile.mkdirs()
                                        }
                                        mergeVideoAndAudio(imagePath, audioPath)
                                    }
                                }
                            } else {
                                if (listOfMultipleMedia.isNullOrEmpty()) {
                                    openEditor(Uri.fromFile(File(mediaUrl)))
                                } else {
                                    listOfMultipleMedia?.let {
                                        openEditor(
                                            Uri.fromFile(File(it[0].editedVideoPath ?: it[0].mainVideoPath ?: ""))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.autoDispose()

        binding.ivAddText.throttleClicks().subscribeAndObserveOnMainThread {
            selectedItem = TEXT
            binding.llText.isVisible = false
            shapeDrawable = null
            colorCodeTextView = DEFOULT_TEXT_COLOR_CODE
            textAlignPosition = TEXT_ALIGN_CENTER
            fontId = R.font.american_typewriter
            lastTextSize = 40f
            shadowLayerRadius = 0f
            shadowLayerDx = 0f
            shadowLayerDy = 0f
            lastShadowColor = 0
            val newTextAttributes = TextAttributes(
                text = " ",
                color = colorCodeTextView,
                size = lastTextSize,
                alignment = TEXT_ALIGN_CENTER,
                background = shapeDrawable,
                fontId = fontId,
                shadowLayerRadius = shadowLayerRadius,
                shadowLayerDx = shadowLayerDx,
                shadowLayerDy = shadowLayerDy,
                shadowLayerColor = lastShadowColor,

            )
            openAddTextPopupWindow(newTextAttributes)
            if (mimeType == MIME_TYPE_VIDEO_MP4) {
                moveDrawViewToTop()
            }
        }.autoDispose()

        binding.ivSelectedText.throttleClicks().subscribeAndObserveOnMainThread {
            if (isClickText) {
                selectedItem = null
                binding.llEdit.isVisible = true
                binding.llTextEdit.isVisible = false
                binding.addEmojis.isVisible = false
                binding.ivStrikethrough.isVisible = false
                binding.addAlignCenter.isVisible = false
                binding.addTextHighlighter.isVisible = false
                isClickText = false
                isClickPen = false
            }
        }.autoDispose()

        binding.ivEdit.throttleClicks().subscribeAndObserveOnMainThread {
            if (!isClickPen) {
                binding.llText.isVisible = false
                isClickPen = true
                isClickEmojis = false
                selectedItem = PEN
                binding.llEdit.isVisible = false
                binding.ivSelectedPen.isVisible = true
                binding.llTextEdit.isVisible = true
                binding.addEmojis.isVisible = true
                binding.colorSeekBar.isVisible = true
                binding.toolsLayout.isVisible = true
                binding.ivSelectedText.isVisible = false
                binding.drawView.enableDrawing()
                binding.emojisCardView.isVisible = false

                if (mimeType == MIME_TYPE_VIDEO_MP4) {
                    binding.relativeLayout.visibility = View.INVISIBLE
                    binding.deleteAppCompatImageView.visibility = View.INVISIBLE
                    binding.layout.alpha = 0.0f
                    binding.layout.setBackgroundColor(resources.getColor(R.color.color_transparent, null))
                    moveDrawViewToTop()
                } else {
                    binding.relativeLayout.visibility = View.INVISIBLE
                    binding.deleteAppCompatImageView.visibility = View.INVISIBLE
                }
            }
        }.autoDispose()

        binding.addEmojis.throttleClicks().subscribeAndObserveOnMainThread {
            if (!isClickEmojis) {
                setDrawable(8)
                isClickEmojis = true
                selectedItem = EMOJI
                binding.llEdit.isVisible = false
                binding.ivSelectedPen.isVisible = true
                binding.llTextEdit.isVisible = true
                binding.toolsLayout.isVisible = true
                binding.ivSelectedText.isVisible = false
                binding.emojisCardView.isVisible = true
                binding.colorSeekBar.isVisible = false
                binding.addEmojis.isVisible = false
                binding.addGridHex.isVisible = true
                binding.drawView.disableDrawing()
                binding.emojiContainer.isVisible = true
            }
        }.autoDispose()
// Set the OnTouchListener to all emoji views
        binding.smileEmoji.setOnTouchListener(touchListener)
        binding.heartEmoji.setOnTouchListener(touchListener)
        binding.happyEmoji.setOnTouchListener(touchListener)
        binding.fireEmoji.setOnTouchListener(touchListener)
        binding.kissEmoji.setOnTouchListener(touchListener)
        binding.ghostEmoji.setOnTouchListener(touchListener)
        binding.cryEmoji.setOnTouchListener(touchListener)
        binding.hundredEmoji.setOnTouchListener(touchListener)
        binding.smileEmoji.throttleClicks().subscribeAndObserveOnMainThread {
            binding.smileEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
            emojisIndex = 0
            drawnEmojis.clear()
            setDrawable(emojisIndex!!)
        }.autoDispose()

        binding.heartEmoji.throttleClicks().subscribeAndObserveOnMainThread {
            emojisIndex = 1
            drawnEmojis.clear()
            setDrawable(emojisIndex!!)
        }.autoDispose()

        binding.happyEmoji.throttleClicks().subscribeAndObserveOnMainThread {
            emojisIndex = 2
            drawnEmojis.clear()
            setDrawable(emojisIndex!!)
        }.autoDispose()

        binding.fireEmoji.throttleClicks().subscribeAndObserveOnMainThread {
            emojisIndex = 3
            drawnEmojis.clear()
            setDrawable(emojisIndex!!)
        }.autoDispose()

        binding.kissEmoji.throttleClicks().subscribeAndObserveOnMainThread {
            emojisIndex = 4
            drawnEmojis.clear()
            setDrawable(emojisIndex!!)
        }.autoDispose()

        binding.ghostEmoji.throttleClicks().subscribeAndObserveOnMainThread {
            emojisIndex = 5
            drawnEmojis.clear()
            setDrawable(emojisIndex!!)
        }.autoDispose()

        binding.cryEmoji.throttleClicks().subscribeAndObserveOnMainThread {
            emojisIndex = 6
            drawnEmojis.clear()
            setDrawable(emojisIndex!!)
        }.autoDispose()

        binding.hundredEmoji.throttleClicks().subscribeAndObserveOnMainThread {
            emojisIndex = 7
            drawnEmojis.clear()
            setDrawable(emojisIndex!!)
        }.autoDispose()

        binding.addGridHex.throttleClicks().subscribeAndObserveOnMainThread {
            binding.colorSeekBar.isVisible = true
            binding.addGridHex.isVisible = false
            binding.addEmojis.isVisible = true
            binding.emojisCardView.isVisible = false
            selectedItem = PEN
            emojisIndex = null
            isClickEmojis = false
            binding.drawView.enableDrawing()
        }.autoDispose()

        binding.ivSelectedPen.throttleClicks().subscribeAndObserveOnMainThread {
            if (isClickPen) {
                binding.llEdit.isVisible = true
                binding.llTextEdit.isVisible = false
                binding.toolsLayout.isVisible = false
                emojisIndex = null
                isClickText = false
                isClickPen = false
                binding.drawView.disableDrawing()

                if (mimeType != MIME_TYPE_VIDEO_MP4) {
                    binding.deleteAppCompatImageView.isVisible = false
                    binding.relativeLayout.isVisible = true
                } else {
                    binding.relativeLayout.isVisible = true
                    binding.deleteAppCompatImageView.isVisible = true
                    binding.layout.alpha = 1.0f
                    moveDrawViewToTop()
                }
            }
        }.autoDispose()

        binding.ivCrop.throttleClicks().subscribeAndObserveOnMainThread {
            binding.llText.isVisible = false
            if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                selectedItem = CROP
                emojisIndex = null
                drawnEmojis.clear()
                val file = File(mediaUrl)
                val uri: Uri = Uri.fromFile(file)
                startCrop(uri)
            } else {
                val savedFile = saveVideoToCacheDir(context, "Edited_video_${System.currentTimeMillis()}")
                cropVideoPath = savedFile.path
                startActivityForResult(VideoCropActivity.createIntent(this, mediaUrl, savedFile.path), CROP_REQUEST)
            }
        }.autoDispose()

        binding.ivAddSticker.throttleClicks().subscribeAndObserveOnMainThread {
            binding.llText.isVisible = false
            selectedItem = STICKER
            emojisIndex = null
            openBottomSheet()
            if (mimeType == MIME_TYPE_VIDEO_MP4) {
                moveDrawViewToTop()
            }
        }.autoDispose()

        binding.ivEraser.throttleClicks().subscribeAndObserveOnMainThread {
            binding.llText.isVisible = false
            selectedItem = BLUR
            binding.llEdit.isVisible = false
            binding.llEraser.isVisible = true
            binding.relativeLayout.isVisible = false
        }.autoDispose()

        binding.ivSounds.throttleClicks().subscribeAndObserveOnMainThread {
            if (mimeType == MIME_TYPE_VIDEO_MP4) {
                val videoUrl = mediaUrl
                val intent = AddMusicActivity.getIntent(this@SnapEditorActivity, videoUrl, MIME_TYPE_VIDEO_MP4)
                startActivityForResult(intent, ADD_MUSIC_REQUEST_CODE)
            } else {
                val photoUrl = mediaUrl
                val intent = AddMusicActivity.getIntent(this@SnapEditorActivity, photoUrl, MIME_TYPE_IMAGE_JPEG)
                startActivityForResult(intent, ADD_MUSIC_REQUEST_CODE)
            }
        }.autoDispose()
        binding.rlSound.throttleClicks().subscribeAndObserveOnMainThread {
            if (mimeType == MIME_TYPE_VIDEO_MP4) {
                val videoUrl = mediaUrl
                val intent = AddMusicActivity.getIntent(this@SnapEditorActivity, videoUrl, MIME_TYPE_VIDEO_MP4)
                startActivityForResult(intent, ADD_MUSIC_REQUEST_CODE)
            } else {
                val photoUrl = mediaUrl
                val intent = AddMusicActivity.getIntent(this@SnapEditorActivity, photoUrl, MIME_TYPE_IMAGE_JPEG)
                startActivityForResult(intent, ADD_MUSIC_REQUEST_CODE)
            }
        }.autoDispose()

        binding.ivDeleteSong.throttleClicks().subscribeAndObserveOnMainThread {
            if (musicResponse != null) {
                musicResponse = null
                audioPath = ""
                binding.rlSound.visibility = View.GONE
            }
        }.autoDispose()

        binding.deleteAppCompatImageView.setOnClickListener {
            deleteVideo()
        }

        binding.ivSelectedEraser.throttleClicks().subscribeAndObserveOnMainThread {
            selectedItem = null
            binding.llEdit.isVisible = true
            binding.llEraser.isVisible = false
            binding.relativeLayout.isVisible = true
        }.autoDispose()

        binding.ivAttachLink.throttleClicks().subscribeAndObserveOnMainThread {
            if (mimeType == MIME_TYPE_VIDEO_MP4) {
                moveDrawViewToTop()
            }
            binding.llText.isVisible = false
            val linkAttachmentBottomSheetFragment = LinkAttachmentBottomSheetFragment().apply {
                linkAttachmentState.subscribeAndObserveOnMainThread {
                    when (it) {
                        is LinkAttachmentState.GoogleAddClick -> {
                            val u = it.googleSearchData.url
                            val url = URL(u)
                            val host: String = url.getHost()
                            val query = url.query
                            if (lastView != null) {
                                lifecycleScope.launch {
                                    val metadata = fetchTitleDomainAndIcon(it.googleSearchData.url)
                                    if (metadata.first != null && metadata.second != null) {
                                        lastView!!.findViewById<TextView>(R.id.title).text = metadata.first
                                        lastView!!.findViewById<TextView>(R.id.url).text = metadata.second
                                        metadata.third?.let { faviconUrl ->
                                            Glide.with(
                                                this@SnapEditorActivity
                                            ).load(faviconUrl).into(lastView!!.findViewById<ImageView>(R.id.icon))
                                        }
                                    } else {
                                        Glide.with(
                                            this@SnapEditorActivity
                                        ).load(
                                            R.drawable.img_google_logo
                                        ).placeholder(
                                            R.drawable.img_google_logo
                                        ).centerCrop().into(lastView!!.findViewById<ImageView>(R.id.icon))
                                        lastView!!.findViewById<TextView>(R.id.title).text = query ?: "est - Google Search"
                                        lastView!!.findViewById<TextView>(R.id.url).text = host
                                    }
                                }
                                Glide.with(
                                    this@SnapEditorActivity
                                ).load("").centerCrop().placeholder(R.drawable.img_google_logo)
                                    .into(lastView!!.findViewById(R.id.icon))
                                lastView!!.findViewById<TextView>(R.id.title).text = it.googleSearchData.title
                                lastView!!.findViewById<TextView>(R.id.url).text = it.googleSearchData.url
                                attchmentUrl = it.googleSearchData.url
                            } else {
                                val view = LayoutInflater.from(
                                    this@SnapEditorActivity
                                ).inflate(R.layout.link_preview, binding.emojiContainer, false)
                                view.findViewById<TextView>(R.id.title).text = it.googleSearchData.title
                                view.findViewById<TextView>(R.id.url).text = it.googleSearchData.url
                                attchmentUrl = it.googleSearchData.url
                                val layoutParams = LayoutParams(
                                    LayoutParams.WRAP_CONTENT,
                                    LayoutParams.WRAP_CONTENT
                                ).apply {
                                    gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
                                    marginEnd = 40
                                    marginStart = 40
                                }
                                view.setOnTouchListener { v, event ->
                                    val views = v as View
                                    views.bringToFront()
                                    viewTransformation(views, event, true)
                                    true
                                }
                                lastView = view
                                binding.emojiContainer.addView(view, layoutParams)
                            }
                        }

                        is LinkAttachmentState.WebAddClick -> {
                            Timber.tag("testing").d("webSearchData = event  = %s", it.goggleWebData)
                            if (lastView != null) {
                                Glide.with(
                                    this@SnapEditorActivity
                                ).load(it.goggleWebData.logoUrl).centerCrop().placeholder(R.drawable.img_google_logo)
                                    .into(lastView!!.findViewById(R.id.icon))
                                lastView!!.findViewById<TextView>(R.id.title).text = it.goggleWebData.title
                                lastView!!.findViewById<TextView>(R.id.url).text = it.goggleWebData.url
                                attchmentUrl = it.goggleWebData.url
                            } else {
                                val view = LayoutInflater.from(
                                    this@SnapEditorActivity
                                ).inflate(R.layout.link_preview, binding.emojiContainer, false)
                                Glide.with(
                                    this@SnapEditorActivity
                                ).load(it.goggleWebData.logoUrl).centerCrop().placeholder(R.drawable.img_google_logo)
                                    .into(view.findViewById(R.id.icon))
                                view.findViewById<TextView>(R.id.title).text = it.goggleWebData.title
                                view.findViewById<TextView>(R.id.url).text = it.goggleWebData.url
                                attchmentUrl = it.goggleWebData.url
                                val layoutParams = LayoutParams(
                                    LayoutParams.WRAP_CONTENT,
                                    LayoutParams.WRAP_CONTENT
                                ).apply {
                                    gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
                                    marginEnd = 40
                                    marginStart = 40
                                }
                                scaleGestureDetector = ScaleGestureDetector(
                                    this@SnapEditorActivity, ScaleListener(view)
                                )
                                view.setOnTouchListener { v, event ->
                                    val views = v as View
                                    scaleGestureDetector.onTouchEvent(event)
                                    views.bringToFront()
                                    viewTransformation(views, event, true)
                                    true
                                }
                                lastView = view
                                binding.emojiContainer.addView(view, layoutParams)
                            }
                        }
                    }
                }.autoDispose()
            }
            linkAttachmentBottomSheetFragment.show(
                supportFragmentManager,
                LinkAttachmentBottomSheetFragment::class.java.name
            )
        }.autoDispose()

        binding.buttonUndo.throttleClicks().subscribeAndObserveOnMainThread {
            if (!listOfDrawingView.isNullOrEmpty()) {
                when (listOfDrawingView.last()) {
                    EMOJIS_DRAWING -> {
                        if (!listOfRemoveCount.isNullOrEmpty()) {
                            for (i in 0 until listOfRemoveCount[listOfRemoveCount.size - 1]) {
                                undoLastEmoji()
                            }
                            listOfRemoveCount.removeLast()
                        }
                    }

                    LINE_DRAWING -> {
                        binding.drawView.undo()
                    }

                    else -> {
                    }
                }
                listOfDrawingView.removeLast()
            }
        }.autoDispose()

        binding.colorSeekBar.setOnColorChangeListener { _, color ->
            binding.drawView.setColor(color)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            animateTextView(binding.textTextView)
            animateTextView(binding.drawTextView)
            animateTextView(binding.stickersTextView)
            animateTextView(binding.cropTextView)
            binding.eraserTextView.visibility = View.GONE
            binding.linksTextView.visibility = View.GONE
            binding.soundsTextView.visibility = View.GONE
            if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                animateTextView(binding.soundsTextView)
                animateTextView(binding.linksTextView)
                animateTextView(binding.eraserTextView)
            } else {
                if (!isClip) {
                    animateTextView(binding.soundsTextView)
                    animateTextView(binding.linksTextView)
                }
            }
        }, ONE_HUNDRED_DURATION)

        Handler(Looper.getMainLooper()).postDelayed({
            binding.llText.isVisible = false
        }, 5000)

        RxBus.listen(RxEvent.ItemClick::class.java).subscribeAndObserveOnMainThread { event ->
            val bitmap = BitmapFactory.decodeResource(resources, event.itemId)

            val imageView = ImageView(this).apply {
                setImageBitmap(bitmap)
            }

            val imageViewParams = LayoutParams(
                150,
                150
            ).apply {
                gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
            }
            binding.emojiContainer.addView(imageView, imageViewParams)

            imageView.setOnTouchListener { v, events ->
                val views = v as ImageView
                views.bringToFront()
                viewTransformation(views, events)
                true
            }
            stickerBottomSheet.dismiss()
        }.autoDispose()
    }

    fun saveVideoToCacheDir(context: Context, fileName: String): File {
        val cacheDir = context.cacheDir
        val specificCacheDir = File(cacheDir, "temp_Video")

        // Create the cache directory if it does not exist
        if (!specificCacheDir.exists()) {
            specificCacheDir.mkdirs()
        }

        // Create the video file
        return File(specificCacheDir, "$fileName.mp4")
    }

    // Touch listener for both ImageView and VideoView
    private val commonTouchListener = View.OnTouchListener { _, event ->
        val x = event.x
        val y = event.y
        val emojiDrawThreshold = 80 // Minimum distance between emojis
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (selectedItem == EMOJI) {
                    val distance = kotlin.math.sqrt((x - lastX).pow(2) + (y - lastY).pow(2))
                    if (lastX == -1f || lastY == -1f || distance > emojiDrawThreshold) {
                        drawEmojiOnView(x, y, binding.emojiContainer)
                        lastX = x
                        lastY = y
                    }
                } else if (selectedItem == BLUR) {
                    applyBlurEfect(x.toInt() - 50, y.toInt() - 50)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (selectedItem == EMOJI) {
                    listOfRemoveCount.add(count)
                    listOfDrawingView.add(EMOJIS_DRAWING)
                    // Reset the last position after lifting the finger
                    lastX = -1f
                    lastY = -1f
                }
            }
        }

        true
    }

    // Function to draw emoji on the given container at the specified coordinates
    private fun drawEmojiOnView(x: Float, y: Float, container: FrameLayout) {
        if (emojisIndex != null) {
            val emoji = when (emojisIndex) {
                0 -> R.drawable.smile
                1 -> R.drawable.heart
                2 -> R.drawable.laugh
                3 -> R.drawable.burn
                4 -> R.drawable.kiss
                5 -> R.drawable.ghost
                6 -> R.drawable.sad
                7 -> R.drawable.hundred
                else -> R.drawable.smile
            }

            val imageView = ImageView(this).apply {
                setImageResource(emoji)
            }
            val imageViewParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = x.toInt() - 50
                topMargin = y.toInt() - 50 // Adjust this value as needed
            }
            container.addView(imageView, imageViewParams)
            count++
            // Add the ImageView to the list
            emojiViews.add(imageView)
        }
    }

    private fun mergeVideoAndAudio(imagePath: String, audioPath: String) {
        val savedFile = saveVideoToCacheDir(context, "Edited_video_${System.currentTimeMillis()}")
        val cmd = arrayOf(
            "-y", "-i",
            imagePath, "-i",
            audioPath, "-c:v",
            "copy", "-c:a", "aac",
            "-map", "0:v:0",
            "-map", "1:a:0",
            "-shortest",
            savedFile.path
        )

        // Execute the FFmpeg command asynchronously
        FFmpeg.executeAsync(cmd) { _, returnCode ->
            if (returnCode == 0) {
                hideLoading()
                openEditor(Uri.fromFile(File(savedFile.path)))
            } else {
                // Failure
                hideLoading()
            }
        }
    }

    private fun moveDrawViewToTop() {
        savedDrawing = binding.drawView.saveDrawing()
        binding.videoLoader.post {
            val playerViewWidth = binding.videoLoader.width
            val playerViewHeight = binding.videoLoader.height
            val layoutParams = FrameLayout.LayoutParams(playerViewWidth, playerViewHeight).apply {
                // Center the view
                gravity = Gravity.CENTER
            }

            // Apply the new layout parameters with margin
            binding.drawView.layoutParams = layoutParams
            binding.emojiContainer.layoutParams = layoutParams
            videoHeight = playerViewHeight
            videoWidth = playerViewWidth
        }
    }

    class ScaleListener(private val view: View) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private var scaleFactor = 1.0f
        private val minScale = 1.0f // Minimum scale factor for zoom out
        private val maxScale = 2.0f // Maximum scale factor for zoom in

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(minScale, min(scaleFactor, maxScale))
            view.scaleX = scaleFactor
            view.scaleY = scaleFactor
            return true
        }
    }

    fun addMusicToImage(imagePath: String, audioPath: String, outputPath: String) {
        showLoading(true)
        val cmd = arrayOf(
            "-loop", "1",
            "-i", imagePath,
            "-i", audioPath,
            "-c:v", "mpeg4",
            "-c:a", "aac",
            "-b:a", "192k",
            "-shortest",
            "-preset", "superfast",
            "-pix_fmt", "yuv420p",
            "-y", outputPath
        )
        Thread {
            val result: Int = FFmpeg.execute(cmd)
            Timber.tag("CreateStoryActivity").i("mergeAudioVideo -> result: $result")
            when (result) {
                0 -> {
                    hideLoading()
                    Timber.tag("VideoTrim").i("result: Success")
                    openEditor(Uri.fromFile(File(outputPath)))
                }

                255 -> {
                    hideLoading()
                    Timber.tag("VideoTrim").d("result: Canceled")
                }

                else -> {
                    hideLoading()
                    Timber.tag("VideoTrim").e("result: Failed")
                }
            }
        }.start()
    }

    private fun deleteVideo() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage("Delete Clip?")
        builder.setPositiveButton(
            Html.fromHtml(
                "<font color='#B22827'>Delete</font>"
            )
        ) { dialog, _ ->
            dialog.dismiss()
            val intent = Intent()
            intent.putExtra(IS_DELETE_VIDEO, true)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun animateTextView(textView: TextView) {
        textView.visibility = View.VISIBLE
        val screenWidth = resources.displayMetrics.widthPixels
        textView.translationX = screenWidth.toFloat()
        val animator = ObjectAnimator.ofFloat(textView, "translationX", 0f)
        animator.duration = THOUSAND_DURATION // 1 second
        animator.start()
    }

    private fun openBottomSheet() {
        stickerBottomSheet.show(supportFragmentManager, StickerBottomSheet.TAG)
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "IMG_" + System.currentTimeMillis()))

        val options = UCrop.Options().apply {
            setCompressionQuality(70)
            setFreeStyleCropEnabled(true)
        }

        UCrop.of(uri, destinationUri).withOptions(options).withAspectRatio(1f, 1f).start(this)
    }

    private suspend fun fetchTitleDomainAndIcon(url: String): Triple<String?, String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val document = Jsoup.connect(url).get()
                val title = document.title()
                val uri = URI(url)
                val domain = uri.host?.replace("www.", "")
                val highQualityIconElements = document.select("link[rel~=(?i)^(apple-touch-icon|icon)]")
                val faviconUrl = if (highQualityIconElements.isNotEmpty()) {
                    highQualityIconElements.firstOrNull()?.absUrl("href")
                } else {
                    val elements = document.select("link[rel~=(?i)^(shortcut|icon|favicon)]")
                    if (elements.isNotEmpty()) {
                        elements.firstOrNull()?.absUrl("href")
                    } else {
                        URI(url).resolve("/favicon.ico").toString()
                    }
                }
                Triple(title, domain, faviconUrl)
            } catch (e: IOException) {
                Timber.tag("SnapEditorActivity").d("Error:-$e")
                Triple(null, null, null)
            }
        }
    }

    private fun addTextToVideo(
        videoPath: String,
        imagePath: String,
        outputPath: String,
        exportMode: Int
    ) {
        val taggedView = binding.emojiContainer.findViewById<CardView>(R.id.linkAttachmentCardView) ?: null
        val screenHeight = resources.displayMetrics.heightPixels
        val screenWidth = resources.displayMetrics.widthPixels
        if (taggedView != null) {
            finalX = taggedView.x
            finalY = taggedView.y
            lastHeight = taggedView.height.toFloat()
            lastWidth = taggedView.width.toFloat()
            lastRotation = taggedView.rotation
            window.decorView.post {
                val insets = window.decorView.rootWindowInsets
                if (insets != null) {
                    val statusBarHeight = insets.systemWindowInsetTop
                    val navigationBarHeight = insets.systemWindowInsetBottom
                    val heightExcludingBars = screenHeight - statusBarHeight - navigationBarHeight
                    finalX = (finalX * ONE_HUNDRED) / screenWidth.toFloat()
                    finalY = (finalY * ONE_HUNDRED) / heightExcludingBars.toFloat()
                    lastWidth = (lastWidth * ONE_HUNDRED) / screenWidth.toFloat()
                    lastHeight = (lastHeight * ONE_HUNDRED) / heightExcludingBars.toFloat()
                    linkAttachmentDetails?.lastHeight = lastHeight
                    linkAttachmentDetails?.lastWidth = lastWidth
                    linkAttachmentDetails?.finalY = finalY
                    linkAttachmentDetails?.finalX = finalX
                    linkAttachmentDetails?.lastRotation = lastRotation
                }
            }
        }

        val videoSize = getVideoDimensions(videoPath = videoPath)
        val commonWidth = videoSize?.width
        val commonHeight = videoSize?.height
        val filterComplex =
            "[0:v]scale=$commonWidth:$commonHeight" +
                "[main];[1:v]scale=$commonWidth:$commonHeight" +
                "[overlay];[main][overlay]overlay=(main_w-overlay_w)/2:(main_h-overlay_h)/2"

        val startDurationStr = String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            startDuration?.div(3600),
            (startDuration?.rem(3600))?.div(60),
            startDuration?.rem(60)
        )
        val endDurationStr = String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            endDuration?.div(3600),
            (endDuration!!.rem(3600)).div(60),
            endDuration!!.rem(60)
        )
        val ffmpegCommand = when (exportMode) {
            EXPORT_ONLY_TRIM -> {
                Timber.tag("export").d("EXPORT_ONLY_TRIM")
                arrayOf(
                    "-ss", startDurationStr,
                    "-i", videoPath,
                    "-c:v", "mpeg4",
                    "-q:v", "3",
                    "-preset", "ultrafast",
                    "-r", "30",
                    "-t", endDurationStr,
                    outputPath
                )
            }

            EXPORT_ONLY_ADDED_VIEW -> {
                Timber.tag("export").d("EXPORT_ONLY_ADDED_VIEW")
                arrayOf(
                    "-i", videoPath,
                    "-i", imagePath,
                    "-filter_complex", filterComplex,
                    "-c:v", "mpeg4",
                    "-q:v", "3",
                    "-preset", "ultrafast",
                    "-r", "30",
                    outputPath
                )
            }

            EXPORT_TRIM_AND_ADDED_VIEW_BOTH -> {
                Timber.tag("export").d("EXPORT_TRIM_AND_ADDED_VIEW_BOTH")
                arrayOf(
                    "-ss", startDurationStr,
                    "-i", videoPath,
                    "-i", imagePath,
                    "-filter_complex", filterComplex,
                    "-c:v", "mpeg4",
                    "-q:v", "3",
                    "-preset", "ultrafast",
                    "-r", "30",
                    "-t", endDurationStr,
                    outputPath
                )
            }

            else -> {
                arrayOf(
                    "-ss", startDurationStr,
                    "-i", videoPath,
                    "-i", imagePath,
                    "-filter_complex", filterComplex,
                    "-c:v", "mpeg4",
                    "-q:v", "3",
                    "-preset", "ultrafast",
                    "-r", "30",
                    "-t", endDurationStr,
                    outputPath
                )
            }
        }

        FFmpegUtils().callFFmpegQuery(
            ffmpegCommand,
            object : FFmpegCallBack {
                override fun process(logMessage: LogMessage) {
                    Timber.tag("FFmpeg").d("process = $logMessage")
                }

                override fun success() {
                    Timber.tag("FFmpeg").d("success")
                    runOnUiThread { hideLoading() }
                    runOnUiThread {
                        val file = File(imagePath)
                        file.delete()
                        if (isClip) {
                            val resultIntent = Intent()
                            resultIntent.putExtra(
                                "linkAttachmentDetails",
                                linkAttachmentDetails
                            ) // Replace with your data
                            resultIntent.putExtra("file", outputPath) // Replace with your data
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        } else {
                            // upload post/story/challenge/shorts merge after add Add sticker, draw line …etc
                            openEditor(Uri.fromFile(File(outputPath)))
                        }
                    }
                }

                override fun cancel() {
                    Timber.tag("FFmpeg").d("cancel, please try again.")
                    runOnUiThread { hideLoading() }
                    runOnUiThread {
                        showToast("Failed with return code = 1")
                    }
                }

                override fun failed() {
                    Timber.tag("FFmpeg").d("failed, please try again.")
                    runOnUiThread { hideLoading() }
                    runOnUiThread {
                        showToast("Failed with return code = 1")
                    }
                }
            }
        )
    }
    private fun createShapeDrawable() {
        shapeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(this@SnapEditorActivity, android.R.color.white))
            cornerRadius = 10 * resources.displayMetrics.density
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setPadding(
                    (10 * resources.displayMetrics.density).toInt(),
                    (4 * resources.displayMetrics.density).toInt(),
                    (10 * resources.displayMetrics.density).toInt(),
                    (5 * resources.displayMetrics.density).toInt()
                )
            }
        }
    }
    private fun openAddTextPopupWindow(attributes: TextAttributes) {
        binding.llEdit.visibility = View.GONE
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val addTextPopupWindowRootView: View = inflater.inflate(R.layout.add_text_popup_window, null)
        val addTextEditText = addTextPopupWindowRootView.findViewById<View>(R.id.add_text_edit_text) as EditText
        addTextEditText.requestFocus()
        colorCodeTextView = attributes.color
        textAlignPosition = attributes.alignment
        fontId = attributes.fontId
        shadowLayerRadius = attributes.shadowLayerRadius
        shadowLayerDx = attributes.shadowLayerDx
        shadowLayerDy = attributes.shadowLayerDy
        lastShadowColor = attributes.shadowLayerColor
        shapeDrawable = attributes.background as GradientDrawable?

        addTextEditText.setText(attributes.text)
        addTextEditText.setTextColor(colorCodeTextView)
        addTextEditText.background = attributes.background
        addTextEditText.typeface = ResourcesCompat.getFont(this@SnapEditorActivity, fontId)
        // Apply shadow layer for strikethrough effect
        addTextEditText.setShadowLayer(
            shadowLayerRadius,
            shadowLayerDx,
            shadowLayerDy,
            lastShadowColor
        )

        val addTextDoneTextView = addTextPopupWindowRootView.findViewById<View>(R.id.ivSelectedText) as ImageView
        val colorPicker = addTextPopupWindowRootView.findViewById<View>(R.id.colorSeekBarInPopPop) as ColorSeekBar
        val textSizePicker = addTextPopupWindowRootView.findViewById<View>(R.id.textSizePicker) as SeekBar
        val textCenter = addTextPopupWindowRootView.findViewById<View>(R.id.textCenter) as ImageView
        val ivAddBackGround = addTextPopupWindowRootView.findViewById<View>(R.id.ivAddBackGround) as ImageView
        val ivStrikethrough = addTextPopupWindowRootView.findViewById<View>(R.id.ivStrikethrough) as ImageView
        val etLinearLayout = addTextPopupWindowRootView.findViewById<View>(R.id.llEditTextView) as LinearLayout
        val fontPickerRecyclerView = addTextPopupWindowRootView.findViewById<View>(
            R.id.font_picker_recycler_view
        ) as RecyclerView
        val layoutManager = LinearLayoutManager(this@SnapEditorActivity, LinearLayoutManager.HORIZONTAL, false)
        fontPickerRecyclerView.layoutManager = layoutManager
        fontPickerRecyclerView.setHasFixedSize(true)
        val fontPickerAdapter = FontPickerAdapter(this@SnapEditorActivity, fontList)
        fontPickerAdapter.setOnFontPickerClickListener(object : FontPickerAdapter.OnFontPickerClickListener {
            override fun onFontPickerClickListener(fontCode: Int) {
                fontId = fontCode
                val typeface = ResourcesCompat.getFont(this@SnapEditorActivity, fontCode)
                addTextEditText.typeface = typeface
            }
        })
        fontPickerRecyclerView.adapter = fontPickerAdapter
        val minFontSize = 10
        val maxFontSize = 80
        val defaultFontSize = 30

        when (textAlignPosition) {
            TEXT_ALIGN_RIGHT -> {
                etLinearLayout.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                addTextEditText.gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }

            TEXT_ALIGN_LEFT -> {
                etLinearLayout.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                addTextEditText.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }

            else -> {
                etLinearLayout.gravity = Gravity.CENTER
                addTextEditText.gravity = Gravity.CENTER
            }
        }

        textSizePicker.max = maxFontSize - minFontSize
        textSizePicker.progress = defaultFontSize - minFontSize
        lastTextSize = (defaultFontSize - minFontSize).toFloat()
        addTextEditText.textSize = defaultFontSize.toFloat()
        textSizePicker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = minFontSize + progress
                // Set the new font size to the TextView
                addTextEditText.textSize = fontSize.toFloat()
                lastTextSize = fontSize.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        lastTextSize = attributes.size
        addTextEditText.textSize = lastTextSize

        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                        val viewAtPosition = layoutManager.findViewByPosition(firstVisiblePosition)
                        if (viewAtPosition != null) {
                            if (fontList[firstVisiblePosition].fontId != null) {
                                fontList[firstVisiblePosition].fontId?.let {
                                    fontId = it
                                }
                                val typeface = ResourcesCompat.getFont(this@SnapEditorActivity, fontId)
                                addTextEditText.typeface = typeface
                                fontPickerAdapter.setSelectedPosition(firstVisiblePosition)
                            }
                        }
                    }
                }
            }
        }

        fontPickerRecyclerView.addOnScrollListener(scrollListener)
        colorPicker.setOnColorChangeListener { _, color ->
            val newColorCode: String = decimalToHex(color)
            if (isBackGroundAdd) {
                if (shapeDrawable == null) {
                    createShapeDrawable() // Initialize shapeDrawable if it's null
                }
                shapeDrawable?.setColor(color) // Safe call using ?. to avoid null pointer exception
            } else if (ivStrikethroughClicked) {
                lastShadowColor = color
                addTextEditText.setShadowLayer(15f, 0f, 0f, color)
            } else {
                colorCodeTextView = color
                addTextEditText.setTextColor(Color.parseColor(newColorCode))
            }

            val textContent: String = if (addTextEditText.text.toString().startsWith(
                    " "
                )
            ) {
                addTextEditText.text.toString()
                    .drop(1)
            } else {
                addTextEditText.text.toString() // remove first letter space
            }
            addTextEditText.text = Editable.Factory.getInstance().newEditable(textContent) // remove first letter space
        }

        val pop = PopupWindow(this@SnapEditorActivity)
        pop.contentView = addTextPopupWindowRootView
        pop.width = LinearLayout.LayoutParams.MATCH_PARENT
        pop.height = LinearLayout.LayoutParams.MATCH_PARENT
        pop.isFocusable = true
        pop.setBackgroundDrawable(null)
        pop.showAtLocation(addTextPopupWindowRootView, Gravity.TOP, 0, 0)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        addTextDoneTextView.setOnClickListener { view ->
            hideKeyboard(view)

            val newText = addTextEditText.text.toString()
            if (newText.isNotBlank()) {
                val newTextProperties: TextAttributes?

                if (selectedItemAddText != null) {
                    selectedItemAddText = selectedItemAddText?.copy(
                        text = newText,
                        color = colorCodeTextView,
                        size = lastTextSize,
                        alignment = textAlignPosition,
                        background = shapeDrawable,
                        fontId = fontId
                    )
                    newTextProperties = selectedItemAddText
                } else {
                    // Add a new text entry
                    newTextProperties = TextAttributes(
                        text = newText,
                        color = colorCodeTextView,
                        size = lastTextSize,
                        alignment = textAlignPosition,
                        background = shapeDrawable,
                        fontId = fontId,
                        shadowLayerRadius = shadowLayerRadius,
                        shadowLayerDx = shadowLayerDx,
                        shadowLayerDy = shadowLayerDy,
                        shadowLayerColor = lastShadowColor,
                    )
                    texts.add(newTextProperties)
                }

                // Display the text on the image
                newTextProperties?.let {
                    addTextOnImage(newText, it)
                } ?: Log.d("SnapEditorActivity", "Text properties are null, unable to add text.")
            }
            // Reset the selected item and clear the input
            selectedItem = null
            addTextEditText.text.clear()
            pop.dismiss()
        }
        textCenter.setOnClickListener {
            val textContent: String = if (addTextEditText.text.toString().startsWith(
                    " "
                )
            ) {
                addTextEditText.text.toString()
                    .drop(1)
            } else {
                addTextEditText.text.toString() // remove first letter space
            }
            addTextEditText.text = Editable.Factory.getInstance().newEditable(textContent) // remove first letter space
            when (etLinearLayout.gravity) {
                Gravity.CENTER -> {
                    etLinearLayout.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    addTextEditText.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    textAlignPosition = TEXT_ALIGN_RIGHT
                }

                Gravity.END or Gravity.CENTER_VERTICAL -> {
                    etLinearLayout.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    addTextEditText.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    textAlignPosition = TEXT_ALIGN_LEFT
                }

                else -> {
                    etLinearLayout.gravity = Gravity.CENTER
                    addTextEditText.gravity = Gravity.CENTER
                    textAlignPosition = TEXT_ALIGN_CENTER
                }
            }
        }
        ivAddBackGround.setOnClickListener {
            val textContent: String = if (addTextEditText.text.toString().startsWith(
                    " "
                )
            ) {
                addTextEditText.text.toString()
                    .drop(1)
            } else {
                addTextEditText.text.toString() // remove first letter space
            }
            addTextEditText.text = Editable.Factory.getInstance().newEditable(textContent)
            isBackGroundAdd = !isBackGroundAdd
            ivStrikethroughClicked = false
            ivStrikethrough.setImageResource(R.drawable.ic_strikethrough)
            if (isBackGroundAdd) {
                ivAddBackGround.setImageResource(R.drawable.fil_icon)
                // Initialize shapeDrawable if it is null
                if (shapeDrawable == null) {
                    createShapeDrawable()
                }
                // Apply the drawable if it's initialized
                addTextEditText.background = shapeDrawable
            } else {
                shapeDrawable = null
                ivAddBackGround.setImageResource(R.drawable.bg_add_text)
                addTextEditText.background = null // Clear background
            }
        }

        ivStrikethrough.setOnClickListener {
            val textContent: String = if (addTextEditText.text.toString().startsWith(
                    " "
                )
            ) {
                addTextEditText.text.toString()
                    .drop(1)
            } else {
                addTextEditText.text.toString() // remove first letter space
            }
            addTextEditText.text = Editable.Factory.getInstance().newEditable(textContent) // remove first letter space
            ivStrikethroughClicked = !ivStrikethroughClicked
            isBackGroundAdd = false
            ivAddBackGround.setImageResource(R.drawable.bg_add_text)
            strikethroughApplied = true
            if (ivStrikethroughClicked) {
                ivStrikethrough.setImageResource(R.drawable.select_stroke)
                addTextEditText.setShadowLayer(15f, 0f, 0f, R.color.textColor_red)
                shadowLayerRadius = 15f
                shadowLayerDx = 0f
                shadowLayerDy = 0f
                lastShadowColor = R.color.textColor_red
            } else {
                ivStrikethrough.setImageResource(R.drawable.ic_strikethrough)
                addTextEditText.setShadowLayer(0f, 0f, 0f, 0)
                shadowLayerRadius = 0f
                shadowLayerDx = 0f
                shadowLayerDy = 0f
                shadowLayerColor = 0
            }
        }

        pop.setOnDismissListener {
            binding.llEdit.visibility = View.VISIBLE
        }
    }

    private fun rotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = Math.atan2(deltaY, deltaX)
        return Math.toDegrees(radians).toFloat()
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun decimalToHex(decimalColor: Int): String {
        // Convert decimal color to hexadecimal and strip the '0x' prefix
        val hexColor = Integer.toHexString(decimalColor and 0xFFFFFF).uppercase()
        // Ensure the hex color is 6 characters long
        val paddedHexColor = hexColor.padStart(6, '0')
        // Add the '#' prefix
        return "#$paddedHexColor"
    }

    private fun isViewOverlapping(view: View, otherView: View): Boolean {
        val viewRect = Rect()
        val otherRect = Rect()
        view.getHitRect(viewRect)
        otherView.getHitRect(otherRect)
        return Rect.intersects(viewRect, otherRect)
    }

    private fun stringIsNotEmpty(string: String?): Boolean {
        if (string != null && string != "null") {
            if (string.trim { it <= ' ' } != "") {
                return true
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility", "ResourceAsColor")
    private fun addTextOnImage(content: String, attributes: TextAttributes) {
        val textContent = if (content.startsWith(
                " "
            )
        ) {
            attributes.text.drop(1)
        } else {
            attributes.text // remove first letter space
        }
        val textView = TextView(this).apply {
            text = textContent
            textSize = attributes.size
            setTextColor(attributes.color)
            textAlignment = when (attributes.alignment) {
                TEXT_ALIGN_LEFT -> {
                    View.TEXT_ALIGNMENT_TEXT_START
                }

                TEXT_ALIGN_RIGHT -> {
                    View.TEXT_ALIGNMENT_TEXT_END
                }

                else -> View.TEXT_ALIGNMENT_CENTER
            }
            background = attributes.background
            typeface = ResourcesCompat.getFont(this@SnapEditorActivity, attributes.fontId)
            if (backgroundAdded) {
                background = shapeDrawable
            }
            if (strikethroughApplied) {
                setShadowLayer(15f, 0f, 0f, lastShadowColor)
            }

            val lastTextAttributes = TextAttributes(
                text = textContent,
                color = colorCodeTextView,
                size = lastTextSize,
                alignment = textAlignPosition,
                background = shapeDrawable,
                fontId = fontId,
                shadowLayerRadius = shadowLayerRadius,
                shadowLayerDx = shadowLayerDx,
                shadowLayerDy = shadowLayerDy,
                shadowLayerColor = lastShadowColor,
            )

            setOnClickListener {
                // Ensure the last background is set before opening the popup
                var selectedTextView: TextView?
                selectedTextView = this
                selectedTextView.let {
                    binding.emojiContainer.removeView(it)
                    selectedTextView = null
                }
                openAddTextPopupWindow(lastTextAttributes)
            }
        }

        val textViewParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = when (textAlignPosition) {
                TEXT_ALIGN_LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
                TEXT_ALIGN_RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
                else -> Gravity.CENTER or Gravity.CENTER_VERTICAL
            }
        }
        binding.emojiContainer.addView(textView, textViewParams)
        currentTextView = textView
        ivStrikethroughClicked = false
        isBackGroundAdd = false
        textView.setOnTouchListener { v, event ->
            val view = v as TextView
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Capture starting point
                    startX = event.x
                    startY = event.y
                    isDragging = false

                    view.bringToFront()
                    viewTransformation(view, event)

                    true // Return true to indicate we are handling the touch event
                }
                MotionEvent.ACTION_MOVE -> {
                    // Calculate the movement distance
                    val deltaX = Math.abs(event.x - startX)
                    val deltaY = Math.abs(event.y - startY)

                    if (deltaX > clickThreshold || deltaY > clickThreshold) {
                        isDragging = true
                        view.bringToFront()
                        v.x += event.x - startX
                        v.y += event.y - startY

                        viewTransformation(view, event)
                    }
                    true // Return true to indicate we are handling the touch event
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // If no dragging was detected, handle as a click
                        v.performClick() // Trigger click event manually
                    }

                    view.bringToFront()
                    viewTransformation(view, event)
                    true // Return true to indicate we are handling the touch event
                }
                else -> false // Allow other touch events to propagate
            }
        }
    }

    private fun viewTransformation(view: View, event: MotionEvent, isLinkPreview: Boolean = false) {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                binding.deleteTv.visibility = View.VISIBLE
                binding.tvDone.visibility = View.GONE
                binding.llOption.visibility = View.GONE
                xCoOrdinate = view.x - event.rawX
                yCoOrdinate = view.y - event.rawY

                start.set(event.x, event.y)
                isOutSide = false
                mode = DRAG
                lastMode = DRAG
                lastEvent = null
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    midPoint(mid, event)
                    mode = ZOOM
                    lastMode = ZOOM
                }

                lastEvent = FloatArray(4)
                lastEvent?.set(0, event.getX(0))
                lastEvent?.set(1, event.getX(1))
                lastEvent?.set(2, event.getY(0))
                lastEvent?.set(3, event.getY(1))
                d = rotation(event)
            }

            MotionEvent.ACTION_UP -> {
                if (isViewOverlapping(view, binding.deleteTv)) {
                    // Remove the text view from the layout
                    colorCodeTextView = DEFOULT_TEXT_COLOR_CODE
                    fontId = R.font.american_typewriter
                    binding.emojiContainer.removeView(view)
                    if (isLinkPreview) {
                        lastView = null
                        lastWidth = 0f
                        lastHeight = 0f
                        lastRotation = 0f
                    }
                } else {
                    val viewY = view.y
                    if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                        if (viewY > imageHeight - 80) {
                            colorCodeTextView = DEFOULT_TEXT_COLOR_CODE
                            fontId = R.font.american_typewriter
                            binding.emojiContainer.removeView(view)
                            if (isLinkPreview) {
                                lastView = null
                                lastWidth = 0f
                                lastHeight = 0f
                                lastRotation = 0f
                            }
                        } else if (viewY < 0) {
                            colorCodeTextView = DEFOULT_TEXT_COLOR_CODE
                            fontId = R.font.american_typewriter
                            binding.emojiContainer.removeView(view)
                            if (isLinkPreview) {
                                lastView = null
                                lastWidth = 0f
                                lastHeight = 0f
                                lastRotation = 0f
                            }
                        }
                    } else {
                        if (viewY > videoHeight - 80) {
                            colorCodeTextView = DEFOULT_TEXT_COLOR_CODE
                            fontId = R.font.american_typewriter
                            binding.emojiContainer.removeView(view)
                            if (isLinkPreview) {
                                lastView = null
                                lastWidth = 0f
                                lastHeight = 0f
                                lastRotation = 0f
                            }
                        } else if (viewY < 0) {
                            colorCodeTextView = DEFOULT_TEXT_COLOR_CODE
                            fontId = R.font.american_typewriter
                            binding.emojiContainer.removeView(view)
                            if (isLinkPreview) {
                                lastView = null
                                lastWidth = 0f
                                lastHeight = 0f
                                lastRotation = 0f
                            }
                        }
                    }
                }
                binding.deleteTv.visibility = View.GONE
                binding.tvDone.visibility = View.VISIBLE
                binding.llOption.visibility = View.VISIBLE
                isZoomAndRotate = false
            }

            MotionEvent.ACTION_OUTSIDE -> {
                isOutSide = true
                mode = NONE
                lastEvent = null
            }

            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                lastEvent = null
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isOutSide) {
                    if (mode == DRAG) {
                        isZoomAndRotate = false
                        view.animate().x(event.rawX + xCoOrdinate).y(event.rawY + yCoOrdinate).setDuration(0).start()
                        finalY = view.y
                        finalX = view.x
                        lastWidth = view.width.toFloat()
                        lastHeight = view.height.toFloat()
                    }
                    if (mode == ZOOM && event.pointerCount == 2) {
                        val newDist1 = spacing(event)
                        if (newDist1 > 10f) {
                            val scale = newDist1 / oldDist * view.scaleX
                            view.scaleX = scale
                            view.scaleY = scale
                        }
                        lastEvent?.let {
                            val newRot = rotation(event)
                            view.rotation = view.rotation + (newRot - d)
                        }
                    }
                }
            }
        }
    }

    private fun initializeFontList() {
        fontList.add(FontDetails(R.font.helvetica, "Classic"))
        fontList.add(FontDetails(R.font.qochy_demo, "Elegance"))
        fontList.add(FontDetails(R.font.retro_side, "Retro"))
        fontList.add(FontDetails(R.font.olivera, "Vintage"))
        fontList.add(FontDetails(R.font.american_typewriter, "AmericanTypewriter"))
        fontList.add(FontDetails(R.font.avenir_heavy, "Avenir-Heavy"))
        fontList.add(FontDetails(R.font.chalkboard_regular, "ChalkboardSE-Regular"))
        fontList.add(FontDetails(R.font.arial_mt, "ArialMT"))
        fontList.add(FontDetails(R.font.bangla_sangam_mn, "BanglaSangamMN"))
        fontList.add(FontDetails(R.font.liberator, "LIBERATOR"))
        fontList.add(FontDetails(R.font.muncie, "MUNCIE"))
        fontList.add(FontDetails(R.font.abraham_lincoln, "Abraham lincoln"))
        fontList.add(FontDetails(R.font.airship_27_regular, "AIRSHIP 27"))
        fontList.add(FontDetails(R.font.arvil_sans, "ARVIL"))
        fontList.add(FontDetails(R.font.bender_lnline, "BENDER"))
        fontList.add(FontDetails(R.font.blanch_condensed, "BLANCH"))
        fontList.add(FontDetails(R.font.cubano_regular_webfont, "CUBANO"))
        fontList.add(FontDetails(R.font.franchise_bold, "FRANCHISE"))
        fontList.add(FontDetails(R.font.geared_slab, "Geared Slab"))
        fontList.add(FontDetails(R.font.governor, "GOVERNOR"))
        fontList.add(FontDetails(R.font.haymaker, "HAYMAKER"))
        fontList.add(FontDetails(R.font.homestead_regular, "HOMESTEAD"))
        fontList.add(FontDetails(R.font.maven_pro_light_200, "Maven Pro Light"))
        fontList.add(FontDetails(R.font.mensch, "MENSCH"))
        fontList.add(FontDetails(R.font.sullivan_regular, "SULLIVAN"))
        fontList.add(FontDetails(R.font.tommaso, "TOMMASO"))
        fontList.add(FontDetails(R.font.valencia_regular, "VALENCIA"))
        fontList.add(FontDetails(R.font.vevey, "VEVEY"))
    }

    override fun onPause() {
        super.onPause()
        if (mimeType == MIME_TYPE_VIDEO_MP4) {
            if (mPlayer.isPlaying) {
                mPlayer.pause()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mimeType == MIME_TYPE_VIDEO_MP4) {
            if (!mPlayer.isPlaying) {
                mPlayer.play()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the player when activity is destroyed
        if (mimeType == MIME_TYPE_VIDEO_MP4) {
            mPlayer.release()
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmap(bitmap: Bitmap, exportMode: Int) {
        val cacheDir = context.cacheDir
        val specificCacheDir = File(cacheDir, "temp_bitmap")

        // Create the directory if it doesn't exist
        if (!specificCacheDir.exists()) {
            specificCacheDir.mkdirs()
        }

        val name = Calendar.getInstance().timeInMillis
        val file = File(cacheDir, "$name.png")
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()

            val taggedView = binding.emojiContainer.findViewById<CardView>(R.id.linkAttachmentCardView) ?: null
            val screenHeight = resources.displayMetrics.heightPixels
            val screenWidth = resources.displayMetrics.widthPixels
            if (taggedView != null && !isStory && !isChallenge) {
                finalX = taggedView.x
                finalY = taggedView.y
                lastHeight = taggedView.height.toFloat()
                lastWidth = taggedView.width.toFloat()
                lastRotation = taggedView.rotation
                window.decorView.post {
                    val insets = window.decorView.rootWindowInsets
                    if (insets != null) {
                        val statusBarHeight = insets.systemWindowInsetTop
                        val navigationBarHeight = insets.systemWindowInsetBottom
                        val heightExcludingBars = screenHeight - statusBarHeight - navigationBarHeight
                        finalX = (finalX * 100) / screenWidth.toFloat()
                        finalY = (finalY * 100) / heightExcludingBars.toFloat()
                        lastWidth = (lastWidth * 100) / screenWidth.toFloat()
                        lastHeight = (lastHeight * 100) / heightExcludingBars.toFloat()
                        linkAttachmentDetails?.lastHeight = lastHeight
                        linkAttachmentDetails?.lastWidth = lastWidth
                        linkAttachmentDetails?.finalY = finalY
                        linkAttachmentDetails?.finalX = finalX
                        linkAttachmentDetails?.lastRotation = lastRotation
                    }
                }
            }
            linkAttachmentDetails = LinkAttachmentDetails(finalX, finalY, lastHeight, lastWidth, lastRotation, attachUrl = attchmentUrl)

            if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                runOnUiThread { hideLoading() }
                if (!isStory && !isChallenge && !isShorts) {
                    if (musicResponse != null && !audioPath.isNullOrEmpty()) {
                        val savedFile = saveVideoToCacheDir(context, "Edited_video_${System.currentTimeMillis()}")
                        addMusicToImage(file.path, audioPath, savedFile.path)
                    } else {
                        val resultIntent = Intent()
                        resultIntent.putExtra("linkAttachmentDetails", linkAttachmentDetails) // Replace with your data
                        resultIntent.putExtra("file", file.path) // Replace with your data
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                } else {
                    if (listOfMultipleMedia.isNullOrEmpty()) {
                        if (musicResponse != null && !audioPath.isNullOrEmpty()) {
                            val savedFile = saveVideoToCacheDir(context, "Edited_video_${System.currentTimeMillis()}")
                            addMusicToImage(file.path, audioPath, savedFile.path)
                        } else {
                            openEditor(Uri.fromFile(File(file.path)))
                        }
                    } else {
                        listOfMultipleMedia?.let {
                            if (musicResponse != null && !audioPath.isNullOrEmpty()) {
                                val savedFile = saveVideoToCacheDir(
                                    context,
                                    "Edited_video_${System.currentTimeMillis()}"
                                )
                                addMusicToImage(file.path, audioPath, savedFile.path)
                            } else {
                                openEditor(Uri.fromFile(File(it[0].editedImagePath ?: it[0].mainImagePath ?: "")))
                            }
                        }
                    }
                }
            } else {
                val savedFile = saveVideoToCacheDir(context, "Edited_video_${System.currentTimeMillis()}")
                addTextToVideo(outputMediaUrl.ifEmpty { mediaUrl }, file.path, savedFile.path, exportMode)
            }
        } catch (e: IOException) {
            runOnUiThread { hideLoading() }
            e.printStackTrace()
        } finally {
            fileOutputStream?.close()
        }
    }

    private fun listenToViewModel() {
        deepArViewModel.addNewPostState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddNewPostViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }

                is AddNewPostViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressedDispatcher.onBackPressed()
                }

                is AddNewPostViewState.UploadImageCloudFlareSuccess -> {
                    if (linkAttachmentDetails != null) {
                        val listOfPositionArray = arrayListOf<Float>()
                        listOfPositionArray.add(linkAttachmentDetails?.finalX ?: 0.0F)
                        listOfPositionArray.add(linkAttachmentDetails?.finalY ?: 0.0F)
                        listOfPositionArray.add(linkAttachmentDetails?.lastWidth ?: 0.0F)
                        listOfPositionArray.add(linkAttachmentDetails?.lastHeight ?: 0.0F)
                        val addStoryRequest = AddStoryRequest(
                            type = "image",
                            image = it.imageUrl,
                            position = listOfPositionArray.toString(),
                            rotation = linkAttachmentDetails?.lastRotation,
                            webUrl = linkAttachmentDetails?.attachUrl,
                            height = imageHeight,
                            width = imageWidth
                        )
                        deepArViewModel.addStory(addStoryRequest)
                    } else {
                        val addStoryRequest = AddStoryRequest(
                            type = "image",
                            image = it.imageUrl,
                            height = imageHeight,
                            width = imageWidth
                        )
                        deepArViewModel.addStory(addStoryRequest)
                    }
                }

                is AddNewPostViewState.StoryUploadSuccess -> {
                    showLongToast(it.successMessage)
                    startActivity(MainHomeActivity.getIntent(this@SnapEditorActivity))
                }

                else -> {}
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true
            }
        }
        return false
    }

    private fun setDrawable(indexOfEmojis: Int) {
        when (indexOfEmojis) {
            0 -> {
                binding.smileEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
                binding.heartEmoji.background = null
                binding.happyEmoji.background = null
                binding.fireEmoji.background = null
                binding.kissEmoji.background = null
                binding.ghostEmoji.background = null
                binding.cryEmoji.background = null
                binding.hundredEmoji.background = null
            }

            1 -> {
                binding.smileEmoji.background = null
                binding.heartEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
                binding.happyEmoji.background = null
                binding.fireEmoji.background = null
                binding.kissEmoji.background = null
                binding.ghostEmoji.background = null
                binding.cryEmoji.background = null
                binding.hundredEmoji.background = null
            }
            2 -> {
                binding.smileEmoji.background = null
                binding.heartEmoji.background = null
                binding.happyEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
                binding.fireEmoji.background = null
                binding.kissEmoji.background = null
                binding.ghostEmoji.background = null
                binding.cryEmoji.background = null
                binding.hundredEmoji.background = null
            }
            3 -> {
                binding.smileEmoji.background = null
                binding.heartEmoji.background = null
                binding.happyEmoji.background = null
                binding.fireEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
                binding.kissEmoji.background = null
                binding.ghostEmoji.background = null
                binding.cryEmoji.background = null
                binding.hundredEmoji.background = null
            }
            4 -> {
                binding.smileEmoji.background = null
                binding.heartEmoji.background = null
                binding.happyEmoji.background = null
                binding.fireEmoji.background = null
                binding.kissEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
                binding.ghostEmoji.background = null
                binding.cryEmoji.background = null
                binding.hundredEmoji.background = null
            }
            5 -> {
                binding.smileEmoji.background = null
                binding.heartEmoji.background = null
                binding.happyEmoji.background = null
                binding.fireEmoji.background = null
                binding.kissEmoji.background = null
                binding.ghostEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
                binding.cryEmoji.background = null
                binding.hundredEmoji.background = null
            }
            6 -> {
                binding.smileEmoji.background = null
                binding.heartEmoji.background = null
                binding.happyEmoji.background = null
                binding.fireEmoji.background = null
                binding.kissEmoji.background = null
                binding.ghostEmoji.background = null
                binding.cryEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
                binding.hundredEmoji.background = null
            }
            7 -> {
                binding.smileEmoji.background = null
                binding.heartEmoji.background = null
                binding.happyEmoji.background = null
                binding.fireEmoji.background = null
                binding.kissEmoji.background = null
                binding.ghostEmoji.background = null
                binding.cryEmoji.background = null
                binding.hundredEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
            }
            else -> {
                binding.smileEmoji.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_selected)
                binding.heartEmoji.background = null
                binding.happyEmoji.background = null
                binding.fireEmoji.background = null
                binding.kissEmoji.background = null
                binding.ghostEmoji.background = null
                binding.cryEmoji.background = null
            }
        }
    }

    private fun undoLastEmoji() {
        if (emojiViews.isNotEmpty()) {
            // Remove the last emoji view from the container
            val lastEmojiView = emojiViews.removeAt(emojiViews.size - 1)
            binding.emojiContainer.removeView(lastEmojiView)

            // Optionally, reset lastX and lastY if needed
            if (emojiViews.isEmpty()) {
                lastX = -1f
                lastY = -1f
            } else {
                val lastEmoji = emojiViews.last()
                lastX = lastEmoji.left.toFloat()
                lastY = lastEmoji.top.toFloat()
            }
        }
    }

    private fun applyBlurEfect(x: Int, y: Int) {
        // Enable drawing cache
        binding.imagePreview.isDrawingCacheEnabled = true

        // Create bitmap from drawing cache
        val bitmap = Bitmap.createBitmap(binding.imagePreview.drawingCache)

        // Disable drawing cache after creating the bitmap
        binding.imagePreview.isDrawingCacheEnabled = false

        // Define the radius of the blur effect
        val blurRadius = 25f

        // Create a RenderScript context
        val rsContext = RenderScript.create(this)

        // Calculate the region to apply the blur effect
        val startX = maxOf(0, x) // adjust as needed
        val startY = maxOf(0, y) // adjust as needed
        val width = minOf(bitmap.width - startX, 75) // adjust as needed
        val height = minOf(bitmap.height - startY, 75) // adjust as needed

        // Check if the calculated width or height is positive
        if (width > 0 && height > 0) {
            // Create a bitmap to store the region to be blurred
            val regionBitmap = Bitmap.createBitmap(bitmap, startX, startY, width, height)

            // Apply blur effect only to the region
            val blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val blurInput = Allocation.createFromBitmap(rsContext, regionBitmap)
            val blurOutput = Allocation.createTyped(rsContext, blurInput.type)
            val blurScript = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))
            blurScript.setRadius(blurRadius)
            blurScript.setInput(blurInput)
            blurScript.forEach(blurOutput)
            blurOutput.copyTo(blurredBitmap)

            // Update the ImageView with the blurred region
            val imageView = ImageView(this).apply {
                setImageBitmap(blurredBitmap)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    leftMargin = startX
                    topMargin = startY
                }
            }
            binding.emojiContainer.addView(imageView)
        } else {
            // Log or handle the case where width or height is not positive
        }

        // Destroy the RenderScript context to free resources
        rsContext.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            data?.let {
                val resultUri = UCrop.getOutput(it)
                resultUri?.let { uri ->
                    binding.imagePreview.setImageURI(uri)
                    selectedItem = null
                    saveImageToLocalStorage(uri)
                }
            }
        } else if (requestCode == CROP_REQUEST && resultCode == RESULT_OK) {
            outputMediaUrl = cropVideoPath
            setupMediaIfNeeded(outputMediaUrl)
        } else if (resultCode == UCrop.RESULT_ERROR) {
            data?.let {
                val cropError = UCrop.getError(it)
                cropError?.let { error ->
                    Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                }
            }
        } else if (resultCode == RESULT_OK && requestCode == AddNewPostInfoActivity.REQUEST_CODE_CAMERA) {
            setResult(Activity.RESULT_OK, data)
            finish()
        } else if (resultCode == RESULT_OK && requestCode == CreateChallengeActivity.REQUEST_FOR_CHOOSE_PHOTO) {
            setResult(Activity.RESULT_OK, data)
            finish()
        } else if (resultCode == RESULT_OK && requestCode == ADD_MUSIC_REQUEST_CODE) {
            val musicInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra("MUSIC_INFO", MusicInfo::class.java)
            } else {
                data?.getParcelableExtra("MUSIC_INFO")
            }
            if (musicInfo != null) {
                audioPath = data?.getStringExtra("AUDIO_PATH") ?: ""
                this.musicResponse = musicInfo
                binding.rlSound.visibility = View.VISIBLE
                binding.musicTitleAppCompatTextView.text = musicResponse?.name
                binding.singerNameAppCompatTextView.text = getArtistsNames()
                Glide.with(
                    this@SnapEditorActivity
                ).load(musicResponse?.image?.lastOrNull()?.url).placeholder(R.drawable.dummy_profile_pic)
                    .centerCrop().into(binding.ivProfile)
            }
        }
    }

    fun getArtistsNames(): String? {
        return musicResponse?.artists?.all?.joinToString(separator = ", ") { it?.name ?: "" }
    }

    private fun saveImageToLocalStorage(imageUri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val dir = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_PICTURES)
            val demoDir = File(dir, "demo")
            if (!demoDir.exists()) {
                demoDir.mkdir()
            }
            val file = File(demoDir, "${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            mediaUrl = file.path
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEditor(inputImage: Uri) {
        val filePath = FileUtils.getPath(this@SnapEditorActivity, inputImage) ?: return
        when {
            isStory -> {
                if (mimeType == MIME_TYPE_IMAGE_JPEG && musicResponse == null) {
                    showLoading(true)
                    val taggedView = binding.emojiContainer.findViewById<CardView>(R.id.linkAttachmentCardView) ?: null
                    if (taggedView != null) {
                        linkAttachmentDetails?.lastHeight = taggedView.height.toFloat()
                        linkAttachmentDetails?.lastWidth = taggedView.width.toFloat()
                        linkAttachmentDetails?.finalY = taggedView.y
                        linkAttachmentDetails?.finalX = taggedView.x
                        linkAttachmentDetails?.lastRotation = taggedView.rotation
                        cloudFlareConfig?.let {
                            deepArViewModel.uploadImageToCloudFlare(this, it, File(filePath))
                        } ?: deepArViewModel.getCloudFlareConfig(false)
                    } else {
                        cloudFlareConfig?.let {
                            deepArViewModel.uploadImageToCloudFlare(this, it, File(filePath))
                        } ?: deepArViewModel.getCloudFlareConfig(false)
                    }
                } else {
                    showLoading(true)
                    if (playBackSpeed != 1f) {
                        updatePlaybackSpeed(inputImage)
                    } else {
                        compressVideo(filePath, inputImage)
                    }
                }
            }
            isChallenge -> {
                if (linkAttachmentDetails != null && !linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                    linkAttachmentDetails?.let {
                        if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                            val taggedView = binding.emojiContainer.findViewById<CardView>(R.id.linkAttachmentCardView) ?: null
                            if (taggedView != null) {
                                finalX = taggedView.x
                                finalY = taggedView.y
                                lastHeight = taggedView.height.toFloat()
                                lastWidth = taggedView.width.toFloat()
                                lastRotation = taggedView.rotation
                                if (mimeType == MIME_TYPE_VIDEO_MP4) {
                                    finalX = (finalX * 100) / videoWidth.toFloat()
                                    finalY = (finalY * 100) / videoHeight.toFloat()
                                    lastWidth = (lastWidth * 100) / videoWidth.toFloat()
                                    lastHeight = (lastHeight * 100) / videoHeight
                                } else {
                                    if (musicResponse != null && audioPath.isNotEmpty()) {
                                        finalX = (finalX * 100) / imageWidth.toFloat()
                                        finalY = (finalY * 100) / imageHeight.toFloat()
                                        lastWidth = (lastWidth * 100) / imageWidth.toFloat()
                                        lastHeight = (lastHeight * 100) / imageHeight
                                    }
                                }

                                linkAttachmentDetails?.lastHeight = lastHeight
                                linkAttachmentDetails?.lastWidth = lastWidth
                                linkAttachmentDetails?.finalY = finalY
                                linkAttachmentDetails?.finalX = finalX
                                linkAttachmentDetails?.lastRotation = lastRotation
                                val intent = Intent()
                                intent.putExtra("FILE_PATH", filePath)
                                if (mimeType != MIME_TYPE_VIDEO_MP4) {
                                    intent.putExtra("HEIGHT", imageHeight)
                                    intent.putExtra("WIDTH", imageWidth)
                                } else {
                                    intent.putExtra("HEIGHT", videoHeight)
                                    intent.putExtra("WIDTH", videoWidth)
                                }
                                intent.putExtra("LINK_ATTACHMENT_DETAILS", linkAttachmentDetails)
                                if (musicResponse != null && audioPath.isNotEmpty()) {
                                    intent.putExtra("MUSIC_INFO", musicResponse)
                                }
                                setResult(Activity.RESULT_OK, intent)
                                finish()
                            }
                        } else {
                            val intent = Intent()
                            intent.putExtra("FILE_PATH", filePath)
                            if (musicResponse != null && audioPath.isNotEmpty()) {
                                intent.putExtra("MUSIC_INFO", musicResponse)
                            }
                            if (mimeType != MIME_TYPE_VIDEO_MP4) {
                                intent.putExtra("HEIGHT", imageHeight)
                                intent.putExtra("WIDTH", imageWidth)
                            } else {
                                intent.putExtra("HEIGHT", videoHeight)
                                intent.putExtra("WIDTH", videoWidth)
                            }
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                    }
                } else if (musicResponse != null) {
                    if (mimeType == MIME_TYPE_VIDEO_MP4) {
                        val intent = Intent()
                        intent.putExtra("FILE_PATH", filePath)
                        if (mimeType != MIME_TYPE_VIDEO_MP4) {
                            intent.putExtra("HEIGHT", imageHeight)
                            intent.putExtra("WIDTH", imageWidth)
                        } else {
                            intent.putExtra("HEIGHT", videoHeight)
                            intent.putExtra("WIDTH", videoWidth)
                        }
                        intent.putExtra("MUSIC_INFO", musicResponse)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    } else {
                        val intent = Intent()
                        intent.putExtra("FILE_PATH", filePath)
                        if (mimeType != MIME_TYPE_VIDEO_MP4) {
                            intent.putExtra("HEIGHT", imageHeight)
                            intent.putExtra("WIDTH", imageWidth)
                        } else {
                            intent.putExtra("HEIGHT", videoHeight)
                            intent.putExtra("WIDTH", videoWidth)
                        }
                        intent.putExtra("MUSIC_INFO", musicResponse)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                } else {
                    val intent = Intent()
                    if (mimeType != MIME_TYPE_VIDEO_MP4) {
                        intent.putExtra("HEIGHT", imageHeight)
                        intent.putExtra("WIDTH", imageWidth)
                    } else {
                        intent.putExtra("HEIGHT", videoHeight)
                        intent.putExtra("WIDTH", videoWidth)
                    }
                    intent.putExtra("FILE_PATH", filePath)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
            isShorts -> {
                showLoading(true)
                if (playBackSpeed != 1f) {
                    updatePlaybackSpeed(inputImage)
                } else {
                    compressVideo(filePath, inputImage)
                }
            }
            else -> {
                if (mimeType == MIME_TYPE_VIDEO_MP4) {
                    val screenWidth = resources.displayMetrics.widthPixels
                    val taggedView = binding.emojiContainer.findViewById<CardView>(R.id.linkAttachmentCardView) ?: null
                    if (taggedView != null) {
                        finalX = taggedView.x
                        finalY = taggedView.y
                        lastHeight = taggedView.height.toFloat()
                        lastWidth = taggedView.width.toFloat()
                        lastRotation = taggedView.rotation
                        window.decorView.post {
                            val insets = window.decorView.rootWindowInsets
                            if (insets != null) {
                                val videoSize = getVideoDimensions(videoPath = filePath)
                                finalX = (finalX * 100) / screenWidth.toFloat()
                                finalY = (finalY * 100) / (videoSize?.height ?: 0).toFloat()
                                lastWidth = (lastWidth * 100) / screenWidth.toFloat()
                                lastHeight = (lastHeight * 100) / (videoSize?.height ?: 0)
                                linkAttachmentDetails?.lastHeight = lastHeight
                                linkAttachmentDetails?.lastWidth = lastWidth
                                linkAttachmentDetails?.finalY = finalY
                                linkAttachmentDetails?.finalX = finalX
                                linkAttachmentDetails?.lastRotation = lastRotation

                                val intent = Intent()
                                intent.putExtra(
                                    AddNewPostInfoActivity.INTENT_EXTRA_POST_TYPE,
                                    AddNewPostInfoActivity.POST_TYPE_POST_VIDEO
                                )
                                intent.putExtra(AddNewPostInfoActivity.INTENT_EXTRA_VIDEO_PATH, filePath)
                                if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                                    intent.putExtra(
                                        AddNewPostInfoActivity.LINK_ATTACHMENT_DETAILS,
                                        linkAttachmentDetails
                                    )
                                }
                                intent.putExtra("HEIGHT", videoHeight)
                                intent.putExtra("WIDTH", videoWidth)
                                if (musicResponse != null && !audioPath.isNullOrEmpty()) {
                                    intent.putExtra(AddNewPostInfoActivity.MUSIC_INFO, musicResponse)
                                }

                                intent.putExtra(AddNewPostInfoActivity.POST_TYPE_VIDEO_URI, inputImage)
                                setResult(Activity.RESULT_OK, intent)
                                finish()
                            }
                        }
                    } else {
                        val intent = Intent()
                        intent.putExtra(
                            AddNewPostInfoActivity.INTENT_EXTRA_POST_TYPE,
                            AddNewPostInfoActivity.POST_TYPE_POST_VIDEO
                        )
                        intent.putExtra(AddNewPostInfoActivity.INTENT_EXTRA_VIDEO_PATH, filePath)
                        if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                            intent.putExtra(AddNewPostInfoActivity.LINK_ATTACHMENT_DETAILS, linkAttachmentDetails)
                        }
                        if (musicResponse != null && !audioPath.isNullOrEmpty()) {
                            intent.putExtra(AddNewPostInfoActivity.MUSIC_INFO, musicResponse)
                        }
                        intent.putExtra("HEIGHT", videoHeight)
                        intent.putExtra("WIDTH", videoWidth)
                        intent.putExtra(AddNewPostInfoActivity.POST_TYPE_VIDEO_URI, inputImage)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                } else {
                    if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                        val screenHeight = resources.displayMetrics.heightPixels
                        val screenWidth = resources.displayMetrics.widthPixels
                        val taggedView = binding.emojiContainer.findViewById<CardView>(R.id.linkAttachmentCardView) ?: null
                        if (taggedView != null) {
                            finalX = taggedView.x
                            finalY = taggedView.y
                            lastHeight = taggedView.height.toFloat()
                            lastWidth = taggedView.width.toFloat()
                            lastRotation = taggedView.rotation
                            window.decorView.post {
                                val insets = window.decorView.rootWindowInsets
                                if (insets != null) {
                                    val statusBarHeight = insets.systemWindowInsetTop
                                    val navigationBarHeight = insets.systemWindowInsetBottom
                                    if (musicResponse != null && audioPath.isNotEmpty()) {
                                        val videoSize = getVideoDimensions(videoPath = filePath)
                                        finalX = (finalX * 100) / screenWidth.toFloat()
                                        finalY = (finalY * 100) / (videoSize?.height ?: 0).toFloat()
                                        lastWidth = (lastWidth * 100) / screenWidth.toFloat()
                                        lastHeight = (lastHeight * 100) / (videoSize?.height ?: 0)
                                    } else {
                                        val heightExcludingBars = screenHeight - statusBarHeight - navigationBarHeight
                                        finalX = (finalX * 100) / screenWidth.toFloat()
                                        finalY = (finalY * 100) / heightExcludingBars.toFloat()
                                        lastWidth = (lastWidth * 100) / screenWidth.toFloat()
                                        lastHeight = (lastHeight * 100) / heightExcludingBars.toFloat()
                                    }
                                    linkAttachmentDetails?.lastHeight = lastHeight
                                    linkAttachmentDetails?.lastWidth = lastWidth
                                    linkAttachmentDetails?.finalY = finalY
                                    linkAttachmentDetails?.finalX = finalX
                                    linkAttachmentDetails?.lastRotation = lastRotation
                                    val intent = Intent()
                                    intent.putExtra("file", filePath)
                                    if (musicResponse != null) {
                                        intent.putExtra("MUSIC_INFO", musicResponse)
                                    }
                                    intent.putExtra("HEIGHT", imageHeight)
                                    intent.putExtra("WIDTH", imageWidth)
                                    intent.putExtra("linkAttachmentDetails", linkAttachmentDetails)
                                    setResult(Activity.RESULT_OK, intent)
                                    finish()
                                }
                            }
                        }
                    } else {
                        val intent = Intent()
                        intent.putExtra("file", filePath)
                        if (musicResponse != null) {
                            intent.putExtra("MUSIC_INFO", musicResponse)
                        }

                        intent.putExtra("HEIGHT", imageHeight)
                        intent.putExtra("WIDTH", imageWidth)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                }
            }
        }
    }

    private fun compressVideo(videoPath: String, inputImage: Uri) {
        val videoUris = listOf(Uri.fromFile(File(videoPath)))
        duration = getVideoDuration(videoPath)
        lifecycleScope.launch {
            VideoCompressor.start(
                context = applicationContext,
                videoUris,
                isStreamable = false,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies,
                    subFolderName = resources.getString(R.string.application_name)
                ),
                configureWith = com.abedelazizshe.lightcompressorlibrary.config.Configuration(
                    quality = VideoQuality.HIGH,
                    videoNames = videoUris.map { uri -> uri.pathSegments.last() },
                    isMinBitrateCheckEnabled = false,
                ),
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {
                    }

                    override fun onStart(index: Int) {
                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        hideLoading()
                        if (isShorts) {
                            if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                                val taggedView = binding.emojiContainer.findViewById<CardView>(R.id.linkAttachmentCardView) ?: null
                                if (taggedView != null) {
                                    finalX = taggedView.x
                                    finalY = taggedView.y
                                    lastHeight = taggedView.height.toFloat()
                                    lastWidth = taggedView.width.toFloat()
                                    lastRotation = taggedView.rotation

                                    finalX = (finalX * 100) / videoWidth.toFloat()
                                    finalY = (finalY * 100) / videoHeight.toFloat()
                                    lastWidth = (lastWidth * 100) / videoWidth.toFloat()
                                    lastHeight = (lastHeight * 100) / videoHeight

                                    linkAttachmentDetails?.lastHeight = lastHeight
                                    linkAttachmentDetails?.lastWidth = lastWidth
                                    linkAttachmentDetails?.finalY = finalY
                                    linkAttachmentDetails?.finalX = finalX
                                    linkAttachmentDetails?.lastRotation = lastRotation

                                    val intent = AddNewPostInfoActivity.launchActivity(
                                        LaunchActivityData(
                                            this@SnapEditorActivity,
                                            postType = AddNewPostInfoActivity.POST_TYPE_VIDEO,
                                            imagePathList = arrayListOf(),
                                            videoPath = videoPath,
                                            tagName = tagName,
                                            videoUri = inputImage,
                                            linkAttachmentDetails = linkAttachmentDetails,
                                            listOfMultipleMedia = null,
                                            musicResponse = musicResponse
                                        )
                                    )
                                    startActivity(intent)
                                    finish()
                                }
                            } else {
                                val intent = AddNewPostInfoActivity.launchActivity(
                                    LaunchActivityData(
                                        this@SnapEditorActivity,
                                        postType = AddNewPostInfoActivity.POST_TYPE_VIDEO,
                                        imagePathList = arrayListOf(),
                                        videoPath = videoPath,
                                        tagName = tagName,
                                        videoUri = inputImage,
                                        linkAttachmentDetails = null,
                                        listOfMultipleMedia = null,
                                        musicResponse = musicResponse
                                    )
                                )
                                startActivity(intent)
                                finish()
                            }
                        } else if (isStory) {
                            if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                                val taggedView = binding.emojiContainer.findViewById<CardView>(R.id.linkAttachmentCardView) ?: null
                                if (taggedView != null) {
                                    finalX = taggedView.x
                                    finalY = taggedView.y
                                    lastHeight = taggedView.height.toFloat()
                                    lastWidth = taggedView.width.toFloat()
                                    lastRotation = taggedView.rotation
                                    val screenHeight = resources.displayMetrics.heightPixels
                                    val screenWidth = resources.displayMetrics.widthPixels
                                    window.decorView.post {
                                        val insets = window.decorView.rootWindowInsets
                                        if (insets != null) {
                                            val statusBarHeight = insets.systemWindowInsetTop
                                            val navigationBarHeight = insets.systemWindowInsetBottom
                                            val heightExcludingBars = screenHeight - statusBarHeight - navigationBarHeight
                                            finalX = (finalX * 100) / screenWidth.toFloat()
                                            finalY = (finalY * 100) / heightExcludingBars.toFloat()
                                            lastWidth = (lastWidth * 100) / screenWidth.toFloat()
                                            lastHeight = (lastHeight * 100) / heightExcludingBars.toFloat()
                                            linkAttachmentDetails?.lastHeight = lastHeight
                                            linkAttachmentDetails?.lastWidth = lastWidth
                                            linkAttachmentDetails?.finalY = finalY
                                            linkAttachmentDetails?.finalX = finalX
                                            linkAttachmentDetails?.lastRotation = lastRotation
                                            if (isOnline(this@SnapEditorActivity)) {
                                                if (path.toString().isNotEmpty()) {
                                                    cloudFlareConfig?.let {
                                                        startActivity(
                                                            MainHomeActivity.getIntentFromStoryUpload(
                                                                this@SnapEditorActivity,
                                                                StoryUploadData(
                                                                    cloudFlareConfig ?: CloudFlareConfig(),
                                                                    path.toString(),
                                                                    (duration / THOUSAND).toInt().toString(),
                                                                    linkAttachmentDetails = linkAttachmentDetails,
                                                                    musicResponse,
                                                                    videoHeight,
                                                                    videoWidth
                                                                )
                                                            )
                                                        )
                                                    } ?: deepArViewModel.getCloudFlareConfig(false)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (isOnline(this@SnapEditorActivity)) {
                                    if (path.toString().isNotEmpty()) {
                                        cloudFlareConfig?.let {
                                            startActivity(
                                                MainHomeActivity.getIntentFromStoryUpload(
                                                    this@SnapEditorActivity,
                                                    StoryUploadData(
                                                        cloudFlareConfig ?: CloudFlareConfig(),
                                                        path.toString(),
                                                        (duration / THOUSAND).toInt().toString(),
                                                        linkAttachmentDetails = null,
                                                        musicResponse,
                                                        videoHeight,
                                                        videoWidth
                                                    )
                                                )
                                            )
                                        } ?: deepArViewModel.getCloudFlareConfig(false)
                                    }
                                }
                            }
                        } else if (isChallenge) {
                            val intent = Intent()
                            intent.putExtra("FILE_PATH", videoPath)
                            if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                                intent.putExtra("LINK_ATTACHMENT_DETAILS", linkAttachmentDetails)
                            }
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        } else {
                            val intent = Intent()
                            intent.putExtra(
                                AddNewPostInfoActivity.INTENT_EXTRA_POST_TYPE,
                                AddNewPostInfoActivity.POST_TYPE_POST_VIDEO
                            )
                            intent.putExtra(AddNewPostInfoActivity.INTENT_EXTRA_VIDEO_PATH, videoPath)
                            if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                                intent.putExtra(AddNewPostInfoActivity.LINK_ATTACHMENT_DETAILS, linkAttachmentDetails)
                            }
                            intent.putExtra(AddNewPostInfoActivity.LIST_OF_MULTIPLE_MEDIA, listOfMultipleMedia)
                            intent.putExtra(AddNewPostInfoActivity.POST_TYPE_VIDEO_URI, inputImage)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        Timber.wtf(failureMessage)
                        hideLoading()
                    }

                    override fun onCancelled(index: Int) {
                        Timber.wtf("compression has been cancelled")
                        hideLoading()
                    }
                },
            )
        }
    }

    fun getVideoDuration(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        } finally {
            retriever.release()
        }
    }

    private fun updatePlaybackSpeed(inputImage: Uri) {
        if (!mediaUrl.isNullOrEmpty() && mimeType == MIME_TYPE_VIDEO_MP4) {
            val videoUrl = URL(Uri.fromFile(File(mediaUrl)).toString()).toString()
            val outputFilePath = getFilePath(videoFileName)
            val speed = if (playBackSpeed == 2f) "setpts=0.5*PTS" else if (playBackSpeed == 0.5f) "setpts=2.0*PTS" else "setpts=1.0*PTS"
            val command = arrayOf("-i", videoUrl, "-filter:v", speed, outputFilePath)

            FFmpeg.executeAsync(command) { _, returnCode ->
                if (returnCode == Config.RETURN_CODE_SUCCESS) {
                    val outputFile = File(outputFilePath)
                    val outputUrl = outputFile.absolutePath
                    mediaUrl = outputUrl
                    compressVideo(outputUrl, inputImage)
                } else {
                    compressVideo(mediaUrl, inputImage)
                }
            }
        }
    }

    private fun getFilePath(videoFileName: String): String? {
        // Use the cache directory
        val cacheDir = context.cacheDir // Ensure context is available in your function
        val dirPathFile = File(cacheDir, "temp_video") // Use a subdirectory in the cache directory

        // Create the storage directory if it does not exist
        if (!dirPathFile.exists()) {
            dirPathFile.mkdirs()
        }
        DownloadService.dirPath = dirPathFile.absolutePath
        return File(dirPathFile, videoFileName).absolutePath
    }

    private fun getVideoDuration(uri: Uri): Double {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        retriever.release()
        return (duration / THOUSAND).toDouble()
    }

    override fun getResult(uri: Uri) {
    }

    override fun onError(message: String) {
    }

    override fun onProgress(percentage: Int) {
    }
}
