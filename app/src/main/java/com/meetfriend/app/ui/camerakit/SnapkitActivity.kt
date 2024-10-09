package com.meetfriend.app.ui.camerakit

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.text.Html
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.arthenica.mobileffmpeg.*
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.StoryUploadData
import com.meetfriend.app.api.effect.model.CenterZoomLayoutManager
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.api.post.model.LinkAttachmentDetails
import com.meetfriend.app.api.post.model.MultipleImageDetails
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.application.MeetFriendApplication.Companion.context
import com.meetfriend.app.databinding.ActivitySnapkitBinding
import com.meetfriend.app.ffmpeg.FFmpegCallBack
import com.meetfriend.app.ffmpeg.FFmpegUtils
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.camerakit.model.AgeRestrictionDialogState
import com.meetfriend.app.ui.camerakit.model.ChildDialogState
import com.meetfriend.app.ui.camerakit.model.LauncherGetIntent
import com.meetfriend.app.ui.camerakit.model.StandardPromptDialogState
import com.meetfriend.app.ui.camerakit.model.TermsOfServiceState
import com.meetfriend.app.ui.camerakit.termsofservice.AgeRestrictionFragment
import com.meetfriend.app.ui.camerakit.termsofservice.ChildDialogFragment
import com.meetfriend.app.ui.camerakit.termsofservice.StandardPromptDialog
import com.meetfriend.app.ui.camerakit.termsofservice.TermsOfServiceFragment
import com.meetfriend.app.ui.camerakit.utils.LensesAdapter
import com.meetfriend.app.ui.camerakit.utils.MIME_TYPE_IMAGE_JPEG
import com.meetfriend.app.ui.camerakit.utils.MIME_TYPE_VIDEO_MP4
import com.meetfriend.app.ui.camerakit.view.MultipleMediaAdapter
import com.meetfriend.app.ui.challenge.CreateChallengeActivity
import com.meetfriend.app.ui.dual.DualCameraActivity
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewModel
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewState
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.main.story.AddStoryActivity
import com.meetfriend.app.utilclasses.download.DownloadService
import com.meetfriend.app.utils.FileUtils
import com.snap.camerakit.*
import com.snap.camerakit.common.Consumer
import com.snap.camerakit.lenses.LensesComponent
import com.snap.camerakit.lenses.whenHasSome
import com.snap.camerakit.support.camera.AllowsCameraFlash
import com.snap.camerakit.support.camera.AllowsSnapshotCapture
import com.snap.camerakit.support.camera.AllowsVideoCapture
import com.snap.camerakit.support.camerax.CameraXImageProcessorSource
import com.snap.camerakit.support.permissions.HeadlessFragmentPermissionRequester
import com.snap.camerakit.support.widget.SnapButtonView
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.*
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SnapkitActivity : BasicActivity() {
    lateinit var binding: ActivitySnapkitBinding
    private lateinit var cameraKitSession: Session
    private lateinit var imageProcessorSource: CameraXImageProcessorSource
    private lateinit var rootContainer: RelativeLayout
    private lateinit var previewGestureHandler: View
    private val recordingDuration: Long = 12000 // Duration of recording in milliseconds (120 seconds)
    private var elapsedTime: Long = 0
    private val updateInterval = 100L
    private lateinit var lensesAdapter: LensesAdapter
    private var selectedLensPosition = 0
    private var isCameraFacingFront = true
    private val processorExecutor = Executors.newSingleThreadExecutor()
    private var permissionRequest: Closeable? = null
    private var videoRecording: Closeable? = null
    private var lensRepositorySubscription: Closeable? = null

    private var listOfLense: ArrayList<LensesComponent.Lens> = arrayListOf()
    private var REQUEST_CODE_PICK_IMAGE = 20001
    private var REQUEST_CODE_FOR_EDTIOR = 100

    private var handler = Handler(Looper.getMainLooper())
    private var isChallenge: Boolean = false
    private var isShorts: Boolean = false
    private var isStory: Boolean = false
    private var isDualCameraSupported: Boolean = false
    private var mediaType: String? = "photo"
    private var playbackSpeed = 1f
    private var countTimerDisposable: Disposable? = null
    private var timerStatus = 0
    private var videoCaptureTime = 0
    private var cameraIconTint: ColorStateList? = null
    private val imageUris = mutableListOf<String>()
    private var currentImageIndex = 0
    private var lastSelectItem = 0
    private var totalDuration = 0
    private var lastVideoTotalDuration: Double = 0.0
    private val listOfMultipleMedia = arrayListOf<MultipleImageDetails>()
    private lateinit var multipleMediaAdapter: MultipleMediaAdapter


    private lateinit var countDownTimer: CountDownTimer
    private var isRunning: Boolean = false

    private var isRecording = false
    private var startTime: Long = 0
    private var tagName: String? = null

    companion object {
        var IS_SHORTS = "IS_SHORTS"
        var IS_CHALLENGE = "IS_CHALLENGE"
        var IS_STORY = "IS_STORY"
        const val INTENT_TAG_NAME = "INTENT_TAG_NAME"
        const val IS_DELETE_VIDEO: String = "IS_DELETE_VIDEO"
        const val TAG = "SnapkitActivity"

        fun getIntent(
            context: Context,
            isShorts: Boolean = false,
            isChallenge: Boolean = false,
            isStory: Boolean = false,
            tagName: String? = null

        ): Intent {
            val intent = Intent(context, SnapkitActivity::class.java)
            intent.putExtra(IS_SHORTS, isShorts)
            intent.putExtra(IS_CHALLENGE, isChallenge)
            intent.putExtra(IS_STORY, isStory)

            if (!tagName.isNullOrEmpty()) intent.putExtra(INTENT_TAG_NAME, tagName)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddNewPostViewModel>
    private lateinit var deepArViewModel: AddNewPostViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var linkAttachmentDetails: LinkAttachmentDetails? = null
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var duration: Long = 0L
    val videoFileName: String
        get() {
            val rnds = (0..10000).random()

            return "video_${System.currentTimeMillis()}_${rnds}.mp4"
        }
    lateinit var mimeType: String
    private var mediaUrl = ""

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySnapkitBinding.inflate(layoutInflater)
        setContentView(binding.root)
        deleteAllPreviousCacheFiles(this)
        deleteAllPreviousCacheVideoFiles(this)
        isDualCameraSupported = isDualCameraSupported(this)
        if (!isDualCameraSupported) {
            binding.cameraDualButton.visibility = View.GONE
            binding.dualCameraTextView.visibility = View.GONE
        }

        MeetFriendApplication.component.inject(this)
        deepArViewModel = getViewModelFromFactory(viewModelFactory)

        // Checking if Camera Kit is supported on this device or not.
        if (!supported(this)) {
            Toast.makeText(this, getString(R.string.camera_kit_not_supported), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        isShorts = intent?.getBooleanExtra(IS_SHORTS, false) ?: false
        isChallenge = intent?.getBooleanExtra(IS_CHALLENGE, false) ?: false
        isStory = intent?.getBooleanExtra(IS_STORY, false) ?: false
        if (intent?.hasExtra(INTENT_TAG_NAME) == true) {
            tagName = intent?.getStringExtra(INTENT_TAG_NAME)
        }
        rootContainer = binding.rootContainer

        if (isOnline(this) && isStory) {
            deepArViewModel.getCloudFlareConfig(false)
        } else if (isStory) {
            showToast("No internet connection")
        }
        val tapGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(event: MotionEvent): Boolean {
                flipCamera()
                return super.onDoubleTap(event)
            }

        })

        val captureGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (mediaType == "photo") {
                    if (timerStatus != 0) {
                        displayTimer()
                    } else {
                        binding.captureButton.onCaptureRequestListener?.onStart(SnapButtonView.CaptureType.SNAPSHOT)
                    }
                } else if (mediaType == "video") {
                    if (timerStatus != 0) {
                        if (isRecording) {
                            binding.captureButton.onCaptureRequestListener?.onEnd(SnapButtonView.CaptureType.CONTINUOUS)
                            isRecording = false
                            updateCaptureButtonColor(false)
                        } else {
                            if (totalDuration <= 120) {
                                binding.captureButton.onCaptureRequestListener?.onStart(SnapButtonView.CaptureType.CONTINUOUS)
                                isRecording = true
                                updateCaptureButtonColor(true)
                                if (videoCaptureTime != 0) {
                                    val videoTimerInSecond = videoCaptureTime + 1
                                    val videoTimerInMilisecond = videoTimerInSecond * 1000
                                    Observable.timer(videoTimerInMilisecond.toLong(), TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                                        runOnUiThread {
                                            binding.captureButton.onCaptureRequestListener?.onEnd(SnapButtonView.CaptureType.CONTINUOUS)
                                            isRecording = false
                                            updateCaptureButtonColor(false)
                                        }
                                    }.autoDispose()
                                }
                            } else {
                                showToast("Capture limit reached. Try trimming the video.")
                            }
                        }
                    } else {
                        if (isRecording) {
                            binding.captureButton.onCaptureRequestListener?.onEnd(SnapButtonView.CaptureType.CONTINUOUS)
                            isRecording = false
                            updateCaptureButtonColor(false)
                        } else {
                            if (totalDuration <= 120) {
                                runOnUiThread {
                                    binding.captureButton.onCaptureRequestListener?.onStart(SnapButtonView.CaptureType.CONTINUOUS)
                                    isRecording = true
                                    updateCaptureButtonColor(true)
                                }
                            } else {
                                showToast("Capture limit reached. Try trimming the video.")
                            }
                        }
                    }

                }
                return super.onSingleTapConfirmed(e)
            }
        })
        initAdapter()
        if (isShorts) {
            binding.speedButton.visibility = View.VISIBLE
            binding.mediaTypeLinearLayout.visibility = View.GONE
            mediaType = "video"
            binding.videoAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.white, null
                )
            )
            binding.photoAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7, null
                )
            )

        } else {
            binding.speedButton.visibility = View.GONE
            mediaType = "photo"
            binding.videoAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7, null
                )
            )
            binding.photoAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.white, null
                )
            )

        }
        previewGestureHandler = binding.previewGestureHandler.apply {
            setOnTouchListener { _, event ->
                tapGestureDetector.onTouchEvent(event)
                when (event.action) {
                    MotionEvent.ACTION_UP -> performClick()
                }
                true
            }
        }

        // App can either use Camera Kit's CameraXImageProcessorSource (which is part of the :support-camerax
        // dependency) or their own input/output and later attach it to the Camera Kit session.
        imageProcessorSource = CameraXImageProcessorSource(
            context = this, lifecycleOwner = this, executorService = processorExecutor, videoOutputDirectory = cacheDir
        )
        cameraKitSession = Session(this) {
            apiToken("eyJhbGciOiJIUzI1NiIsImtpZCI6IkNhbnZhc1MyU0hNQUNQcm9kIiwidHlwIjoiSldUIn0.eyJhdWQiOiJjYW52YXMtY2FudmFzYXBpIiwiaXNzIjoiY2FudmFzLXMyc3Rva2VuIiwibmJmIjoxNzIxODEyODU1LCJzdWIiOiI5YjU5MDZjNS0wNjVjLTRhOGYtOTRkNS0wZTRmOTY2ZTJiY2Z-U1RBR0lOR35jOTQ3Yjc1MC1iMmM5LTRlNjItOTE0Yi03N2VlZmNlNGJhMzQifQ.OX5rj9VPADugN5CJWNC3Is1NvXxMFxaRXkab1o4iXcw")
            imageProcessorSource(imageProcessorSource)
            attachTo(binding.cameraKitStub)
            safeRenderAreaProcessorSource(SafeRenderAreaProcessorSource(this@SnapkitActivity))
            configureLenses {
                // When CameraKit is configured to manage its own views by providing a view stub,
                // lenses touch handling might consume all events due to the fact that it needs to perform gesture
                // detection internally. If application needs to handle gestures on top of it then LensesComponent
                // provides a way to dispatch all touch events unhandled by active lens back.
                dispatchTouchEventsTo(previewGestureHandler)
            }
        }
        Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
            if (!loggedInUserCache.getsnapAgreementPermissionAskedPref()) {
                openStandardPromptDialog()
            }
        }.autoDispose()
        getOsPermissions()
        listenToViewModel()
        // This block demonstrates how to query the repository to get all Lenses from a Camera Kit
        // group. You can query from multiple groups or pre-fetch all Lenses before even user opens
        // the Camera Kit integration. Camera Kit APIs are thread safe - so it's safe to call them
        // from here.
        if (loggedInUserCache.getsnapAgreementPermissionAskedPref()) {
            lensRepositorySubscription = cameraKitSession.lenses.repository.observe(
                LensesComponent.Repository.QueryCriteria.Available(setOf("2ed3a231-ba5c-4d8e-8525-3539ddcb26a3"))
            ) { result ->
                result.whenHasSome { lenses ->
                    listOfLense = lenses as ArrayList<LensesComponent.Lens>
                    val demoLens = object : LensesComponent.Lens {
                        override val id: String = ""
                        override val groupId: String = ""
                        override val name: String = ""
                        override val iconUri: String = ""
                        override val icons: Set<LensesComponent.Lens.Media.Image> = emptySet()
                        override val vendorData: Map<String, String> = emptyMap()
                        override val preview: LensesComponent.Lens.Preview? = null
                        override val previews: Set<LensesComponent.Lens.Media> = emptySet()
                        override val facingPreference: LensesComponent.Lens.Facing? = null
                        override val snapcodes: Set<LensesComponent.Lens.Media> = emptySet()
                    }
                    listOfLense.add(0, demoLens)
                    binding.lensesList.smoothScrollToPosition(0)
                    runOnUiThread {
                        lensesAdapter.submitList(listOfLense)
                    }
                }
            }
        }

        binding.apply {

            timerButton.throttleClicks().subscribeAndObserveOnMainThread {
                if (mediaType == "photo") {
                    if (timerStatus == 0) {
                        timerStatus = 1
                        videoCaptureTime = 0
                        timerButton.backgroundTintList = ContextCompat.getColorStateList(
                            this@SnapkitActivity, R.color.white
                        )
                        timerButton.imageTintList = ContextCompat.getColorStateList(
                            this@SnapkitActivity, R.color.black
                        )
                    } else {
                        timerStatus = 0
                        timerButton.setImageResource(R.drawable.ic_snap_timer)
                        videoCaptureTime = 0
                        timerButton.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.color_transparent)
                        timerButton.imageTintList = ContextCompat.getColorStateList(
                            this@SnapkitActivity, R.color.white
                        )
                    }
                } else {
                    if (timerStatus == 0) {
                        timerStatus = 2
                        val bottomsheet = VideoTimerBottomSheet.newInstance()
                        bottomsheet.videoTimerState.subscribeAndObserveOnMainThread {
                            videoCaptureTime = it
                            if (it != 0) {
                                timerButton.setImageResource(R.drawable.ic_snap_timer)
                                timerButton.backgroundTintList = ContextCompat.getColorStateList(
                                    this@SnapkitActivity, R.color.white
                                )
                                timerButton.imageTintList = ContextCompat.getColorStateList(
                                    this@SnapkitActivity, R.color.black
                                )
                            } else {
                                timerButton.setImageResource(R.drawable.ic_snap_timer)
                                videoCaptureTime = 0
                                timerButton.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.color_transparent)
                                timerButton.imageTintList = ContextCompat.getColorStateList(
                                    this@SnapkitActivity, R.color.white
                                )
                            }
                        }.autoDispose()
                        bottomsheet.show(
                            supportFragmentManager, VideoTimerBottomSheet::class.java.name
                        )
                    } else {
                        timerStatus = 0
                        timerButton.setImageResource(R.drawable.ic_snap_timer)
                        videoCaptureTime = 0
                        timerButton.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.color_transparent)
                        timerButton.imageTintList = ContextCompat.getColorStateList(
                            this@SnapkitActivity, R.color.white
                        )
                    }
                }


            }.autoDispose()

            captureButton.apply {
                addPreviewFeature()
            }

            captureButton.setOnTouchListener { _, event ->
                captureGestureDetector.onTouchEvent(event)
                true
            }

            cameraFlipButton.throttleClicks().subscribeAndObserveOnMainThread {
                flipCamera()
            }.autoDispose()
            Handler(Looper.getMainLooper()).postDelayed({
                animateTextView(flipCameraTextView)
                animateTextView(flashTextView)
                if (isDualCameraSupported) {
                    animateTextView(dualCameraTextView)
                }
                if (mediaType == "video" || isShorts) {
                    animateTextView(playbackSpeedTextView)
                }
                animateTextView(timerTextView)
                animateTextView(galleryTextView)
            }, 1000)

            Handler(Looper.getMainLooper()).postDelayed({
                binding.llText.isVisible = false
            }, 5000)

            binding.speedButton.throttleClicks().subscribeAndObserveOnMainThread {
                binding.llPlayBackSpeedPopup.isVisible = !binding.llPlayBackSpeedPopup.isVisible
                if (playbackSpeed == 1f) {
                    binding.tvSpeedNormal.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.color_tab_purple)
                    binding.tvSpeedSlow.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.white_50_opacity)
                    binding.tvSpeedUltra.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.white_50_opacity)
                }
            }.autoDispose()

            binding.flashOnButton.throttleClicks().subscribeAndObserveOnMainThread {
                runOnUiThread {
                    imageProcessorSource.useFlashConfiguration(
                        AllowsCameraFlash.FlashConfiguration.Disabled
                    )
                }
                binding.flashOnButton.isVisible = false
                binding.flashOffButton.isVisible = true
            }.autoDispose()
            binding.flashOffButton.throttleClicks().subscribeAndObserveOnMainThread {
                runOnUiThread {
                    imageProcessorSource.useFlashConfiguration(
                        AllowsCameraFlash.FlashConfiguration.Enabled(0, TimeUnit.SECONDS)
                    )
                }
                binding.flashOnButton.isVisible = true
                binding.flashOffButton.isVisible = false
            }.autoDispose()

            ivClose.throttleClicks().subscribeAndObserveOnMainThread {
                finish()
            }.autoDispose()

            cameraDualButton.throttleClicks().subscribeAndObserveOnMainThread {
                startActivity(
                    DualCameraActivity.getIntent(
                        this@SnapkitActivity, isShorts, isChallenge, isStory
                    )
                )
            }.autoDispose()

            lensesList.apply {
                lensesAdapter = LensesAdapter { selectedLens ->
                    val index = listOfLense.indexOf(selectedLens)
                    applyLens(selectedLens, index)
                    this.smoothScrollToPosition(listOfLense.indexOf(selectedLens))
                }
                val lmanager = CenterZoomLayoutManager(
                    this@SnapkitActivity, LinearLayoutManager.HORIZONTAL, false
                )
                layoutManager = lmanager
                val snapHelper = LinearSnapHelper()
                snapHelper.attachToRecyclerView(this)
                adapter = lensesAdapter
                this.smoothScrollToPosition(0)

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                        super.onScrolled(recyclerView, dx, dy)
                        val index = getCenterZoomedItemPosition(this@apply)
                        if (index > 0) applyLens(listOfLense[index], index) else clearLenses()
                    }
                })
            }

            openGalleryRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
                when {
                    isStory -> {
                        when (mediaType) {
                            "video" -> {
                                if (totalDuration <= 120) {
                                    startActivityForResult(
                                        AddStoryActivity.launchActivity(
                                            this@SnapkitActivity, mediaType.toString(), isSingleSelection = false, selectLimit = 1
                                        ), AddStoryActivity.REQUEST_FOR_CHOOSE_VIDEO
                                    )
                                } else {
                                    showToast("Capture limit reached. Try trimming the video.")
                                }
                            }

                            "photo" -> {
                                startActivityForResult(
                                    AddStoryActivity.launchActivity(
                                        this@SnapkitActivity, mediaType.toString(), isSingleSelection = true, selectLimit = 1
                                    ), AddStoryActivity.REQUEST_FOR_CHOOSE_PHOTO
                                )
                            }

                            else -> {
                                intent.type = "image/* video/*"
                                intent.putExtra(
                                    Intent.EXTRA_ALLOW_MULTIPLE, false
                                ) // Multiple selection for image/video
                                galleryOpen.launch(intent)
                            }
                        }
                    }

                    isShorts -> {
                        if (totalDuration <= 120) {
                            startActivityForResult(
                                AddStoryActivity.launchActivity(
                                    this@SnapkitActivity, mediaType.toString(), isSingleSelection = false, selectLimit = 1
                                ), AddStoryActivity.REQUEST_FOR_CHOOSE_VIDEO
                            )
                        } else {
                            showToast("Capture limit reached. Try trimming the video.")
                        }
                    }

                    isChallenge -> {
                        try {
                            when (mediaType) {
                                "video" -> {
                                    if (totalDuration <= 120) {
                                        startActivityForResult(
                                            AddStoryActivity.launchActivity(
                                                this@SnapkitActivity, mediaType.toString(), isSingleSelection = false, selectLimit = 1
                                            ), AddStoryActivity.REQUEST_FOR_CHOOSE_VIDEO
                                        )
                                    } else {
                                        showToast("Capture limit reached. Try trimming the video.")
                                    }
                                }

                                "photo" -> {
                                    startActivityForResult(
                                        AddStoryActivity.launchActivity(
                                            this@SnapkitActivity, mediaType.toString(), isSingleSelection = true, selectLimit = 1
                                        ), AddStoryActivity.REQUEST_FOR_CHOOSE_PHOTO
                                    )
                                }

                                else -> {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        putExtra(
                                            Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")
                                        )
                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    }
                                    galleryOpen.launch(intent)
                                }
                            }
                        } catch (ex: ActivityNotFoundException) {
                            showToast("No Gallery APP installed")
                        }
                    }

                    else -> {
                        try {
                            when (mediaType) {
                                "video" -> {
                                    if (totalDuration <= 120) {
                                        startActivityForResult(
                                            AddStoryActivity.launchActivity(
                                                this@SnapkitActivity, mediaType.toString(), isSingleSelection = false, selectLimit = 1
                                            ), AddStoryActivity.REQUEST_FOR_CHOOSE_VIDEO
                                        )
                                    } else {
                                        showToast("Capture limit reached. Try trimming the video.")
                                    }

                                }

                                "photo" -> {
                                    if (listOfMultipleMedia.size < 5) {
                                        val selectLimit = 5 - listOfMultipleMedia.size
                                        startActivityForResult(
                                            AddStoryActivity.launchActivity(
                                                this@SnapkitActivity, mediaType.toString(), isSingleSelection = false, selectLimit = selectLimit
                                            ), AddStoryActivity.REQUEST_FOR_CHOOSE_PHOTO
                                        )
                                    } else {
                                        showToast("You can add maximum 5 photos.")
                                    }

                                }

                                else -> {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        putExtra(
                                            Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")
                                        )
                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    }
                                    galleryOpen.launch(intent)

                                }
                            }
                        } catch (ex: ActivityNotFoundException) {
                            showToast("No Gallery APP installed")
                        }
                    }
                }

            }

            tvSpeedSlow.throttleClicks().subscribeAndObserveOnMainThread {
                tvPlayBackSpeed.text = getString(R.string.label_slow_motion)
                tvPlayBackSpeedNumber.text = getString(R.string.label_speed_slow_number)
                tvPlayBackSpeedNumber.isVisible = true
                llPlayBackSpeedPopup.isVisible = false
                playbackSpeed = 0.5f
                val sdk = Build.VERSION.SDK_INT
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    binding.speedButton.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@SnapkitActivity, R.drawable.rounded_button_selected
                        )
                    )
                } else {
                    binding.speedButton.background = ContextCompat.getDrawable(
                        this@SnapkitActivity, R.drawable.rounded_button_selected
                    )
                }
                cameraIconTint = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.black
                )
                binding.speedButton.imageTintList = cameraIconTint
                binding.tvSpeedSlow.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.color_tab_purple)
                binding.tvSpeedNormal.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.white_50_opacity)
                binding.tvSpeedUltra.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.white_50_opacity)
                ContextCompat.getColorStateList(this@SnapkitActivity, R.color.white_50_opacity)
            }.autoDispose()
            binding.photoAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                mediaType = "photo"
                binding.videoAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.color_A7A7A7, null
                    )
                )
                binding.photoAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.white, null
                    )
                )
                binding.playbackSpeedTextView.visibility = View.GONE
                binding.speedButton.visibility = View.GONE
            }

            binding.videoAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                mediaType = "video"
                binding.videoAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.white, null
                    )
                )
                binding.photoAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.color_A7A7A7, null
                    )
                )
                binding.playbackSpeedTextView.visibility = View.VISIBLE
                binding.speedButton.visibility = View.VISIBLE
            }

            tvSpeedNormal.throttleClicks().subscribeAndObserveOnMainThread {
                tvPlayBackSpeed.text = getString(R.string.label_normal)
                tvPlayBackSpeedNumber.text = getString(R.string.label_speed_normal_number)
                tvPlayBackSpeedNumber.isVisible = false
                llPlayBackSpeedPopup.isVisible = false
                playbackSpeed = 1f
                val sdk = Build.VERSION.SDK_INT
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    binding.speedButton.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@SnapkitActivity, R.drawable.rounded_button
                        )
                    )
                }
                cameraIconTint = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.white
                )
                binding.speedButton.imageTintList = cameraIconTint
                binding.tvSpeedNormal.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.color_tab_purple)
                binding.tvSpeedSlow.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.white_50_opacity)
                binding.tvSpeedUltra.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.white_50_opacity)
            }.autoDispose()

            tvSpeedUltra.throttleClicks().subscribeAndObserveOnMainThread {
                tvPlayBackSpeed.text = getString(R.string.label_ultra_speed)
                tvPlayBackSpeedNumber.text = getString(R.string.label_speed_fast_number)
                tvPlayBackSpeedNumber.isVisible = true
                llPlayBackSpeedPopup.isVisible = false
                playbackSpeed = 2f
                val sdk = Build.VERSION.SDK_INT
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    binding.speedButton.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@SnapkitActivity, R.drawable.rounded_button_selected
                        )
                    )
                } else {
                    binding.speedButton.background = ContextCompat.getDrawable(
                        this@SnapkitActivity, R.drawable.rounded_button_selected
                    )
                }
                cameraIconTint = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.black
                )
                binding.speedButton.imageTintList = cameraIconTint
                binding.tvSpeedUltra.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.color_tab_purple)
                binding.tvSpeedNormal.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.white_50_opacity)
                binding.tvSpeedSlow.backgroundTintList = ContextCompat.getColorStateList(this@SnapkitActivity, R.color.white_50_opacity)
            }.autoDispose()


            binding.ivSelectLense.throttleClicks().subscribeAndObserveOnMainThread {
                binding.lensesList.isVisible = true
                binding.ivSelectLense.isVisible = false
                binding.lensesList.smoothScrollToPosition(0)
            }.autoDispose()

            binding.ivCloseFilter.throttleClicks().subscribeAndObserveOnMainThread {
                binding.ivSelectLense.isVisible = false
                binding.lensesList.isVisible = true
                binding.lensesList.smoothScrollToPosition(0)
                binding.ivCloseFilter.visibility = View.GONE
            }.autoDispose()

            binding.tvUndo.throttleClicks().subscribeAndObserveOnMainThread {
                if (listOfMultipleMedia.isNotEmpty()) {
                    deletePost()
                } else {
                    binding.multipleMediaLayout.isVisible = false
                }
            }.autoDispose()
            binding.photoAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                mediaType = "photo"
                binding.videoAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.color_A7A7A7, null
                    )
                )
                binding.photoAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.white, null
                    )
                )
                binding.playbackSpeedTextView.visibility = View.GONE
                binding.speedButton.visibility = View.GONE
            }

            binding.videoAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                mediaType = "video"
                binding.videoAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.white, null
                    )
                )
                binding.photoAppCompatTextView.setTextColor(
                    resources.getColor(
                        R.color.color_A7A7A7, null
                    )
                )
                binding.playbackSpeedTextView.visibility = View.VISIBLE
                binding.speedButton.visibility = View.VISIBLE
            }

            tvSpeedNormal.throttleClicks().subscribeAndObserveOnMainThread {
                tvPlayBackSpeed.text = getString(R.string.label_normal)
                tvPlayBackSpeedNumber.text = getString(R.string.label_speed_normal_number)
                tvPlayBackSpeedNumber.isVisible = false
                llPlayBackSpeedPopup.isVisible = false
                playbackSpeed = 1f
                val sdk = Build.VERSION.SDK_INT
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    binding.speedButton.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@SnapkitActivity, R.drawable.rounded_button
                        )
                    )
                } else {
                    binding.speedButton.background = ContextCompat.getDrawable(
                        this@SnapkitActivity, R.drawable.rounded_button
                    )
                }
                cameraIconTint = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.white
                )
                binding.speedButton.imageTintList = cameraIconTint
                binding.tvSpeedNormal.backgroundTintList = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.color_tab_purple
                )
                binding.tvSpeedSlow.backgroundTintList = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.white_50_opacity
                )
                binding.tvSpeedUltra.backgroundTintList = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.white_50_opacity
                )
            }.autoDispose()

            tvSpeedUltra.throttleClicks().subscribeAndObserveOnMainThread {
                tvPlayBackSpeed.text = getString(R.string.label_ultra_speed)
                tvPlayBackSpeedNumber.text = getString(R.string.label_speed_fast_number)
                tvPlayBackSpeedNumber.isVisible = true
                llPlayBackSpeedPopup.isVisible = false
                playbackSpeed = 2f
                val sdk = Build.VERSION.SDK_INT
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    binding.speedButton.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@SnapkitActivity, R.drawable.rounded_button_selected
                        )
                    )
                } else {
                    binding.speedButton.background = ContextCompat.getDrawable(
                        this@SnapkitActivity, R.drawable.rounded_button_selected
                    )
                }
                cameraIconTint = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.black
                )
                binding.speedButton.imageTintList = cameraIconTint
                binding.tvSpeedUltra.backgroundTintList = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.color_tab_purple
                )
                binding.tvSpeedNormal.backgroundTintList = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.white_50_opacity
                )
                binding.tvSpeedSlow.backgroundTintList = ContextCompat.getColorStateList(
                    this@SnapkitActivity, R.color.white_50_opacity
                )
            }.autoDispose()

            binding.tvUpload.throttleClicks().subscribeAndObserveOnMainThread {
                if (listOfMultipleMedia.isNullOrEmpty()) {
                    // Handle the case where the list is empty
                    return@subscribeAndObserveOnMainThread
                }
                mimeType = if (mediaType == "video") MIME_TYPE_VIDEO_MP4 else MIME_TYPE_IMAGE_JPEG
                if (mediaType == "video") {
                    if (listOfMultipleMedia.size == 1) {
                        mediaUrl = listOfMultipleMedia.get(0).editedVideoPath ?: listOfMultipleMedia.get(0).mainVideoPath ?: ""
                        MediaScannerConnection.scanFile(
                            this@SnapkitActivity,
                            arrayOf(mediaUrl),
                            null
                        ) { path, uri ->
                            Log.d("MediaScanner", "Scanned $path:")
                            Log.d("MediaScanner", "-> URI: $uri")
                            openEditor(uri)
                        }
                    } else {
                        runOnUiThread {
                            showLoading(true)
                        }
                        val listOfMediaUrl = ArrayList<File>()
                        listOfMultipleMedia.forEach {
                            if (!it.editedVideoPath.isNullOrEmpty()) {
                                it.editedVideoPath?.let {
                                    listOfMediaUrl.add(File(it))
                                }
                            } else {
                                if (!it.mainVideoPath.isNullOrEmpty()) {
                                    it.mainVideoPath?.let {
                                        listOfMediaUrl.add(File(it))
                                    }
                                }
                            }
                        }

                        val savedFile = saveVideoToCacheDir(context, "Final_Merged_video_${System.currentTimeMillis()}")
                        lifecycleScope.launch {
                            mergeVideosWithFFmpeg(listOfMediaUrl, savedFile.path) { success ->
                                runOnUiThread { hideLoading() }
                                if (success) {
                                    runOnUiThread {
                                        hideLoading()
                                    }
                                    mediaUrl = savedFile.path
                                    openEditor(Uri.fromFile(File(mediaUrl)))

                                } else {
                                    hideLoading()
                                }
                            }
                        }
                    }
                } else {
                    listOfMultipleMedia.let {
                        openEditor(Uri.fromFile(File(it[0].editedImagePath ?: it[0].mainImagePath ?: "")))
                    }
                }

            }.autoDispose()

            binding.tvDone.throttleClicks().subscribeAndObserveOnMainThread {

                if (listOfMultipleMedia.isNullOrEmpty()) {
                    // Handle the case where the list is empty
                    return@subscribeAndObserveOnMainThread
                }
                val mimeType = if (mediaType == "video") MIME_TYPE_VIDEO_MP4 else MIME_TYPE_IMAGE_JPEG
                if (mediaType == "video") {
                    if (listOfMultipleMedia.size == 1) {
                        val mediaUrl = listOfMultipleMedia.get(0).editedVideoPath ?: listOfMultipleMedia.get(0).mainVideoPath ?: ""

                        val intent = SnapEditorActivity.getIntent(
                            this@SnapkitActivity, mediaUrl, mimeType, playbackSpeed, isShorts, isChallenge, isStory, false, true, listOfMultipleMedia,tagName = tagName
                        )
                        startActivityForResult(
                            intent, AddNewPostInfoActivity.REQUEST_CODE_CAMERA
                        )

                    } else {
                        runOnUiThread {
                            showLoading(true)
                        }
                        val listOfMediaUrl = ArrayList<File>()
                        listOfMultipleMedia.forEach {
                            if (!it.editedVideoPath.isNullOrEmpty()) {
                                it.editedVideoPath?.let {
                                    listOfMediaUrl.add(File(it))
                                }
                            } else {
                                if (!it.mainVideoPath.isNullOrEmpty()) {
                                    it.mainVideoPath?.let {
                                        listOfMediaUrl.add(File(it))
                                    }
                                }
                            }
                        }

                        val savedFile = saveVideoToCacheDir(context, "Final_Merged_video_${System.currentTimeMillis()}")
                        lifecycleScope.launch {
                            mergeVideosWithFFmpeg(listOfMediaUrl, savedFile.path) { success ->
                                runOnUiThread { hideLoading() }
                                if (success) {
                                    runOnUiThread {
                                        hideLoading()
                                    }
                                    val intent = SnapEditorActivity.getIntent(
                                        this@SnapkitActivity,
                                        savedFile.path,
                                        mimeType,
                                        playbackSpeed,
                                        isShorts,
                                        isChallenge,
                                        isStory,
                                        false,
                                        true,
                                        listOfMultipleMedia,
                                        tagName = tagName
                                    )
                                    startActivityForResult(
                                        intent, AddNewPostInfoActivity.REQUEST_CODE_CAMERA
                                    )
                                } else {
                                    hideLoading()
                                }
                            }
                        }
                    }
                } else {
                    val intent = SnapPreviewActivity.getIntent(
                        LauncherGetIntent(
                        this@SnapkitActivity,
                        "",
                        mimeType,
                        playbackSpeed,
                        isShorts,
                        isChallenge,
                        isStory,
                        linkAttachmentDetails = null,
                        listOfMultipleMedia,
                        tagName = tagName
                    )
                    )
                    startActivityForResult(
                        intent, AddNewPostInfoActivity.REQUEST_CODE_CAMERA
                    )
                }

            }.autoDispose()



            RxBus.listen(RxEvent.CreatePost::class.java).subscribeAndObserveOnMainThread {
                setActResult(it.postType, arrayListOf(it.filePath), it.filePath, "", it.inputImage)
                finish()
            }.autoDispose()

            binding.previewGestureHandler.throttleClicks().subscribeAndObserveOnMainThread {
                if (binding.llPlayBackSpeedPopup.isVisible) binding.llPlayBackSpeedPopup.isVisible = false
            }.autoDispose()

            RxBus.listen(RxEvent.CreateChallenge::class.java).subscribeAndObserveOnMainThread {
                val intent = Intent()
                if (it.linkAttachmentDetails != null) {
                    it.linkAttachmentDetails.let {
                        intent.putExtra("LINK_ATTACHMENT_DETAILS", it)
                    }
                }
                intent.putExtra("FILE_PATH", it.filePath)
                setResult(RESULT_OK, intent)
                finish()
            }.autoDispose()
        }
    }

    private fun openStandardPromptDialog() {
        val standardPromptDialog = StandardPromptDialog.newInstance().apply {
            standardPromptDialogState.subscribeAndObserveOnMainThread {
                when (it) {
                    is StandardPromptDialogState.AcceptClick -> {
                        dismiss()
                        openAgeRestrictionDialog()
                    }

                    is StandardPromptDialogState.CloseDialogClick -> {
                        dismiss()
                    }
                }
            }.autoDispose()
        }
        standardPromptDialog.show(supportFragmentManager, "StandardPromptDialog")
    }

    private fun openAgeRestrictionDialog() {
        val ageRestrictionDialog = AgeRestrictionFragment.newInstance().apply {
            ageRestrictionDialogState.subscribeAndObserveOnMainThread {
                when (it) {
                    is AgeRestrictionDialogState.AdultClick -> {
                        dismiss()
                        openTermsOfService()
                    }

                    is AgeRestrictionDialogState.ChildClick -> {
                        dismiss()
                        openChildDialog()
                    }

                    is AgeRestrictionDialogState.CloseDialogClick -> {
                        dismiss()
                    }
                }
            }.autoDispose()
        }
        ageRestrictionDialog.show(supportFragmentManager, "AgeRestrictionFragment")
    }

    private fun openChildDialog() {
        val childDialogFragment = ChildDialogFragment.newInstance().apply {
            childDialogState.subscribeAndObserveOnMainThread {
                when (it) {
                    is ChildDialogState.CloseDialogClick -> {
                        dismiss()
                    }

                    is ChildDialogState.IAmGuardianClick -> {
                        dismiss()
                        openTermsOfService()
                    }
                }
            }.autoDispose()
        }
        childDialogFragment.show(supportFragmentManager, "ChildDialogFragment")
    }

    private fun openTermsOfService() {
        val childDialogFragment = TermsOfServiceFragment.newInstance().apply {
            termsOfServiceDialogState.subscribeAndObserveOnMainThread {
                when (it) {
                    is TermsOfServiceState.CloseDialogClick -> {
                        dismiss()
                    }

                    is TermsOfServiceState.AcceptClick -> {
                        loggedInUserCache.setsnapAgreementPermissionAskedPref(true)
                        dismiss()
                        lensRepositorySubscription = cameraKitSession.lenses.repository.observe(
                            LensesComponent.Repository.QueryCriteria.Available(setOf("2ed3a231-ba5c-4d8e-8525-3539ddcb26a3"))
                        ) { result ->
                            result.whenHasSome { lenses ->
                                listOfLense = lenses as ArrayList<LensesComponent.Lens>
                                val demoLens = object : LensesComponent.Lens {
                                    override val id: String = ""
                                    override val groupId: String = ""
                                    override val name: String = ""
                                    override val iconUri: String = ""
                                    override val icons: Set<LensesComponent.Lens.Media.Image> = emptySet()
                                    override val vendorData: Map<String, String> = emptyMap()
                                    override val preview: LensesComponent.Lens.Preview? = null
                                    override val previews: Set<LensesComponent.Lens.Media> = emptySet()
                                    override val facingPreference: LensesComponent.Lens.Facing? = null
                                    override val snapcodes: Set<LensesComponent.Lens.Media> = emptySet()
                                }
                                listOfLense.add(0, demoLens)
                                binding.lensesList.smoothScrollToPosition(0)
                                runOnUiThread {
                                    lensesAdapter.submitList(listOfLense)
                                }
                            }
                        }
                    }

                    else -> {

                    }
                }
            }.autoDispose()
        }
        childDialogFragment.show(supportFragmentManager, "TermsOfServiceFragment")
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

    private fun openEditor(inputImage: Uri) {
        val filePath = FileUtils.getPath(this, inputImage) ?: return
        if (isChallenge) {
            if (linkAttachmentDetails != null) {
                linkAttachmentDetails?.let {
                    val intent = Intent()
                    intent.putExtra("FILE_PATH", filePath)
                    if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                        intent.putExtra("LINK_ATTACHMENT_DETAILS", linkAttachmentDetails)
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            } else {
                val intent = Intent()
                intent.putExtra("FILE_PATH", filePath)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        } else if (isStory) {
            if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                showLoading(true)
                cloudFlareConfig?.let {
                    deepArViewModel.uploadImageToCloudFlare(this, it, File(filePath))
                } ?: deepArViewModel.getCloudFlareConfig(false)
            } else {
                showLoading(true)
                if (playbackSpeed != 1f) {
                    updatePlaybackSpeed(inputImage)
                } else {
                    compressVideo(mediaUrl, inputImage)
                }
            }
        } else if (inputImage.toString().lowercase().contains("mp4") || inputImage.toString().lowercase().contains("mov") || inputImage.toString()
                .lowercase().contains("MKV")
        ) {

            showLoading(true)
            if (playbackSpeed != 1f) {
                updatePlaybackSpeed(inputImage)
            } else {
                compressVideo(mediaUrl, inputImage)
            }
        } else {
            val intent = Intent()
            intent.putExtra(AddNewPostInfoActivity.INTENT_EXTRA_POST_TYPE, AddNewPostInfoActivity.POST_TYPE_IMAGE)
            intent.putExtra(AddNewPostInfoActivity.INTENT_EXTRA_IMAGE_PATH_LIST, arrayListOf(filePath))
            if (!linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                intent.putExtra(AddNewPostInfoActivity.LINK_ATTACHMENT_DETAILS, linkAttachmentDetails)
            }
            intent.putExtra(AddNewPostInfoActivity.LIST_OF_MULTIPLE_MEDIA, listOfMultipleMedia)
            intent.putExtra(AddNewPostInfoActivity.POST_TYPE_VIDEO_URI, inputImage)
            setResult(Activity.RESULT_OK, intent)
            finish()
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
                            webUrl = linkAttachmentDetails?.attachUrl,)
                        deepArViewModel.addStory(addStoryRequest)
                    } else {
                        val addStoryRequest = AddStoryRequest(
                            type = "image",
                            image = it.imageUrl
                        )
                        deepArViewModel.addStory(addStoryRequest)
                    }
                }

                is AddNewPostViewState.StoryUploadSuccess -> {
                    showLongToast(it.successMessage)
                    startActivity(MainHomeActivity.getIntent(this@SnapkitActivity))
                }

                else -> {}
            }
        }
    }

    private fun updatePlaybackSpeed(inputImage: Uri) {
        if (!mediaUrl.isNullOrEmpty() && mimeType == MIME_TYPE_VIDEO_MP4) {
            val videoUrl = URL(Uri.fromFile(File(mediaUrl)).toString()).toString()
            val outputFilePath = getFilePath(videoFileName)
            val speed = if (playbackSpeed == 2f) "setpts=0.5*PTS" else if (playbackSpeed == 0.5f) "setpts=2.0*PTS" else "setpts=1.0*PTS"
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

    fun getFilePath(videoFileName: String): String? {

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

    private fun compressVideo(videoPath: String, inputImage: Uri) {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), this.resources.getString(R.string.application_name)
        )
        val videoUris = listOf(Uri.fromFile(File(videoPath)))
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 1000L
        lifecycleScope.launch {
            VideoCompressor.start(
                context = applicationContext,
                videoUris,
                isStreamable = false,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies, subFolderName = resources.getString(R.string.application_name)
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
                            val intent = AddNewPostInfoActivity.launchActivity(
                                LaunchActivityData(this@SnapkitActivity,
                                postType = AddNewPostInfoActivity.POST_TYPE_VIDEO,
                                imagePathList = arrayListOf(),
                                videoPath = videoPath,
                                tagName = tagName,
                                videoUri = inputImage,
                                linkAttachmentDetails = linkAttachmentDetails,
                                listOfMultipleMedia = null
                                )
                            )
                            startActivity(intent)
                            finish()
                        } else if (isStory) {
                            if (isOnline(this@SnapkitActivity)) {
                                if (path.toString().isNotEmpty()) {
                                    cloudFlareConfig?.let {
                                        startActivity(
                                            MainHomeActivity.getIntentFromStoryUpload(
                                                this@SnapkitActivity,
                                                StoryUploadData(
                                                    cloudFlareConfig ?: CloudFlareConfig(),
                                                    path.toString(),
                                                    (duration / 1000).toInt().toString(),
                                                    linkAttachmentDetails = linkAttachmentDetails,
                                                    null,
                                                    null,
                                                    null
                                                )
                                            )
                                        )
                                    } ?: deepArViewModel.getCloudFlareConfig(false)
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
                            intent.putExtra(AddNewPostInfoActivity.INTENT_EXTRA_POST_TYPE, AddNewPostInfoActivity.POST_TYPE_POST_VIDEO)
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

    private fun isDualCameraSupported(context: Context): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = cameraManager.cameraIdList

        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

            capabilities?.let {
                if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)) {
                    return true
                }
            }
        }
        return false
    }

    private fun deletePost() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage("Delete Clip?")
        builder.setPositiveButton(
            Html.fromHtml(
                "<font color='#B22827'>Delete</font>"
            )
        ) { dialog, _ ->
            dialog.dismiss()
            if (mediaType == "video") {
                val item = getVideoDuration(File(listOfMultipleMedia.lastOrNull()?.mainVideoPath!!).toUri())
                totalDuration -= item.toInt()
            }
            listOfMultipleMedia.removeLast()
            if (listOfMultipleMedia.isEmpty()) {
                totalDuration = 0
                binding.multipleMediaLayout.isVisible = false
                if (!isShorts) {
                    binding.mediaTypeLinearLayout.visibility = View.VISIBLE
                }
            } else {
                binding.multipleMediaLayout.isVisible = true
                binding.mediaTypeLinearLayout.visibility = View.GONE
                multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
            }
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
        animator.duration = 1000 // 1 second
        animator.start()
    }

    private fun applyLens(lens: LensesComponent.Lens, index: Int) {
        val usingCorrectCamera = isCameraFacingFront.xor(lens.facingPreference != LensesComponent.Lens.Facing.FRONT)
        if (!usingCorrectCamera) flipCamera()
        cameraKitSession.lenses.processor.apply(lens) { success ->
            if (success) {
                runOnUiThread {
                    selectedLensPosition = index
                    lensesAdapter.select(lens)
                    binding.ivCloseFilter.isVisible = index != 0
                }
            }
        }
    }

    private fun clearLenses() {
        cameraKitSession.lenses.processor.clear { success ->
            if (success) {
                runOnUiThread {
                    selectedLensPosition = 0
                    lensesAdapter.select(listOfLense[0])
                    lensesAdapter.deselect()
                }
            }
        }
    }

    private fun flipCamera() {
        runOnUiThread {
            imageProcessorSource.startPreview(!isCameraFacingFront)
            isCameraFacingFront = !isCameraFacingFront
        }
    }

    override fun onDestroy() {
        permissionRequest?.close()
        lensRepositorySubscription?.close()
        cameraKitSession.close()
        processorExecutor.shutdown()
        countTimerDisposable?.dispose()
        stopVideoRecording()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        stopVideoRecording()
    }

    override fun onStop() {
        super.onStop()
        stopVideoRecording()
    }

    private fun stopVideoRecording() {
        if (isRecording) {
            videoRecording?.close()
            videoRecording = null
            stopRecordingProgress()
        }
    }

    private fun getOsPermissions() {
        val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        // HeadlessFragmentPermissionRequester is part of the :support-permissions, which allows you to easily handle
        // permission requests
        permissionRequest = HeadlessFragmentPermissionRequester(this, requiredPermissions.toSet()) { permissions ->
            if (requiredPermissions.mapNotNull(permissions::get).all { it }) {
                imageProcessorSource.startPreview(isCameraFacingFront)
            }
        }
    }

    //...
    private fun SnapButtonView.addPreviewFeature() {
        val onImageTaken: (Bitmap) -> Unit = { bitmap ->
            runOnUiThread {
                showLoading(true)
                val timestamp = System.currentTimeMillis()
                val filename = "$timestamp.png"
                val file = File(context.cacheDir, filename).apply { createNewFile() }
                val bos = ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }
                val bitmapData: ByteArray = bos.toByteArray()



                FileOutputStream(file).use { fos ->
                    fos.write(bitmapData)
                    fos.flush()
                }
                handleImageCapture(file)
            }
        }

        val onVideoTaken: (File) -> Unit = { video ->

            runOnUiThread {
                handleVideoCapture(video)
            }
        }

        onCaptureRequestListener = object : SnapButtonView.OnCaptureRequestListener {
            override fun onStart(captureType: SnapButtonView.CaptureType) {
                if (mediaType == "photo") {
                    (imageProcessorSource as? AllowsSnapshotCapture)?.takeSnapshot(onImageTaken)
                } else if (mediaType == "video") {
                    if (videoRecording == null) {
                        startRecordingProgress()
                        Handler(Looper.getMainLooper()).postDelayed({
                            startTimer()
                        }, 1000)
                        binding.ivCloseFilter.isVisible = false
                        videoRecording = (imageProcessorSource as? AllowsVideoCapture)?.takeVideo(onVideoTaken)
                    }
                }
            }

            override fun onEnd(captureType: SnapButtonView.CaptureType) {
                if (mediaType == "video") {
                    if (selectedLensPosition != 0) {
                        binding.ivCloseFilter.isVisible = true
                    }
                    videoRecording?.close()
                    videoRecording = null
                    stopRecordingProgress()
                    stopTimer()
                }
            }
        }
    }

    private fun startRecordingProgress() {
        elapsedTime = 0
        isRecording = true
        startTime = SystemClock.elapsedRealtime()
        binding.progressBar.visibility = View.VISIBLE

        binding.progressBar.max = recordingDuration.toInt()
        handler.post(updateProgressRunnable)
    }

    private fun stopRecordingProgress() {
        isRecording = false
        binding.progressBar.visibility = View.GONE
        handler.removeCallbacks(updateProgressRunnable)
    }


    private val updateProgressRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isRecording) return

            elapsedTime += updateInterval
            binding.progressBar.progress = elapsedTime.toInt()

            if (elapsedTime >= recordingDuration) {
                // Restart recording
                elapsedTime = 0
                binding.progressBar.progress = 0
            }

            handler.postDelayed(this, updateInterval)
        }
    }

    private fun handleImageCapture(file: File) {
        if (isChallenge || isStory) {
            val mimeType = MIME_TYPE_IMAGE_JPEG
            hideLoading()
            val intent = SnapEditorActivity.getIntent(
                this@SnapkitActivity, file.path, mimeType, playbackSpeed, isShorts, isChallenge, isStory, false, false, listOfMultipleMedia
            )
            startActivityForResult(intent, AddNewPostInfoActivity.REQUEST_CODE_CAMERA)
        } else {
            binding.tvDoneLinearLayout.visibility = View.GONE
            binding.tvUploadLinearLayout.gravity = Gravity.END
            if (listOfMultipleMedia.size < 5) {
                binding.multipleMediaLayout.isVisible = true
                val multipleImageDetails = MultipleImageDetails().apply {
                    mainImagePath = file.path
                    isVideo = false
                }
                listOfMultipleMedia.add(multipleImageDetails)
                multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                binding.mediaTypeLinearLayout.visibility = View.GONE
                hideLoading()
            } else {
                hideLoading()
                Toast.makeText(this@SnapkitActivity, "You can add maximum 5 photos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleVideoCapture(video: File) {
        binding.ivSelectedFilter.isVisible = false
        if (totalDuration <= 121) {
            val multipleImageDetails = MultipleImageDetails().apply {
                mainVideoPath = video.path
                isVideo = true
            }
            listOfMultipleMedia.add(multipleImageDetails)
            binding.multipleMediaLayout.isVisible = true
            binding.mediaTypeLinearLayout.visibility = View.GONE
            multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
        } else {
            showToast("Capture limit reached. Try trimming the video.")
        }
    }

    private fun updateCaptureButtonColor(isRecording: Boolean) {
        runOnUiThread {
            if (isRecording) {
                binding.captureButton.background = ContextCompat.getDrawable(this, R.drawable.capturebutton_background)
                binding.lensesList.isVisible = false
                if (selectedLensPosition != null && selectedLensPosition != 0) {
                    binding.ivSelectedFilter.isVisible = true
                    val lens = listOfLense.get(selectedLensPosition)
                    Glide.with(this@SnapkitActivity).load(lens.icons.find { it is LensesComponent.Lens.Media.Image.Webp }?.uri)
                        .into(binding.ivSelectedFilter)
                }
            } else {
                binding.lensesList.isVisible = true
                binding.captureButton.background = null
            }
        }
    }

    fun setActResult(
        postType: String, imagePathList: java.util.ArrayList<String>, videopath: String?, tagName: String?, videoUri: Uri?
    ) {
        val data = Intent()
        data.putExtra(AddNewPostInfoActivity.INTENT_EXTRA_POST_TYPE, postType)
        data.putExtra(AddNewPostInfoActivity.INTENT_EXTRA_IMAGE_PATH_LIST, imagePathList)
        data.putExtra(AddNewPostInfoActivity.INTENT_EXTRA_VIDEO_PATH, videopath)
        data.putExtra(AddNewPostInfoActivity.INTENT_TAG_NAME, tagName)
        data.putExtra(AddNewPostInfoActivity.POST_TYPE_VIDEO_URI, videoUri)
        setResult(RESULT_OK, data)
        finish()
    }


    private fun startTimer() {
        if (isRunning) return
        isRunning = true
        isRecording = true
        binding.tvVideoTimer.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                totalDuration++
                binding.tvVideoTimer.text = formatTime(totalDuration.toLong())
                if (totalDuration >= 121) {
                    runOnUiThread {
                        binding.captureButton.onCaptureRequestListener?.onEnd(SnapButtonView.CaptureType.CONTINUOUS)
                        isRecording = false
                        updateCaptureButtonColor(false)
                    }
                }
            }

            override fun onFinish() {
                // Not used because the duration is Long.MAX_VALUE
            }
        }.start()
    }

    private fun stopTimer() {
        if (!isRunning) return
        isRunning = false
        isRecording = false
        countDownTimer.cancel()
        binding.tvVideoTimer.visibility = View.GONE
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    /**
     * Simple implementation of a [Source] for a [SafeRenderAreaProcessor] that calculates a safe render area [Rect]
     * that is between the top and selected lens container present in the [MainActivity].
     */
    private class SafeRenderAreaProcessorSource(mainActivity: SnapkitActivity) : Source<SafeRenderAreaProcessor> {

        private val mainActivityReference = WeakReference(mainActivity)

        override fun attach(processor: SafeRenderAreaProcessor): Closeable {

            return processor.connectInput(object : SafeRenderAreaProcessor.Input {

                override fun subscribeTo(onSafeRenderAreaAvailable: Consumer<Rect>): Closeable {
                    val activity = mainActivityReference.get()
                    if (activity == null) {
                        return Closeable { }
                    } else {
                        fun updateSafeRenderRegionIfNecessary() {
                        }
                        // The processor might subscribe to the input when views are laid out already so we can attempt
                        // to calculate the safe render area already
                        activity.runOnUiThread {
                            updateSafeRenderRegionIfNecessary()
                        }
                        // Otherwise we start listening for layout changes to update the safe render rect continuously
                        val onLayoutChangeListener =
                            View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                                    updateSafeRenderRegionIfNecessary()
                                }
                            }

                        activity.rootContainer.addOnLayoutChangeListener(onLayoutChangeListener)
                        return Closeable {
                            activity.rootContainer.removeOnLayoutChangeListener(
                                onLayoutChangeListener
                            )
                        }
                    }
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_IMAGE) {
            data?.let {
                val postType = it.getStringExtra(AddNewPostInfoActivity.INTENT_EXTRA_POST_TYPE)
                val imagePath = it.getStringArrayListExtra(AddNewPostInfoActivity.INTENT_EXTRA_IMAGE_PATH_LIST)
                val tagName = it.getStringExtra(AddNewPostInfoActivity.INTENT_TAG_NAME)
                if (!postType.isNullOrEmpty()) {
                    if (!imagePath.isNullOrEmpty()) {
                        setActResult(postType, imagePath, null, tagName, null)
                        finish()
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_FOR_EDTIOR) {
            val imagePath = data?.getStringExtra("file")
            val linkAttachmentDetails = data?.getParcelableExtra<LinkAttachmentDetails>("linkAttachmentDetails")
            val musicResponse = data?.getParcelableExtra<MusicInfo>("MUSIC_INFO")
            val contentHeight = data?.getIntExtra("HEIGHT", 0) ?: 0
            val contentWidth = data?.getIntExtra("WIDTH", 0) ?: 0
            if (mediaType == "video") {
                listOfMultipleMedia[lastSelectItem].editedVideoPath = imagePath
                listOfMultipleMedia[lastSelectItem].width = contentWidth
                listOfMultipleMedia[lastSelectItem].height = contentHeight
                lastVideoTotalDuration = getVideoDuration(File(listOfMultipleMedia.get(lastSelectItem).mainVideoPath!!).toUri())
                imagePath?.let {
                    val trimmerVideoDuration = getVideoDuration(it.toUri())

                    if (trimmerVideoDuration == 0.0) {
                        listOfMultipleMedia.removeAt(lastSelectItem)
                        val newDuration = lastVideoTotalDuration - trimmerVideoDuration
                        totalDuration -= newDuration.toInt()
                        if (listOfMultipleMedia.isEmpty()) {
                            totalDuration = 0
                            binding.multipleMediaLayout.isVisible = false
                            if (!isShorts) {
                                binding.mediaTypeLinearLayout.visibility = View.VISIBLE
                            }
                        }
                    } else {
                        val newDuration = lastVideoTotalDuration - trimmerVideoDuration
                        totalDuration -= newDuration.toInt()
                    }
                    multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                }
            } else {
                if (musicResponse != null) {
                    listOfMultipleMedia[lastSelectItem].musicInfo = musicResponse
                    listOfMultipleMedia[lastSelectItem].isMusicVideo = true
                    listOfMultipleMedia[lastSelectItem].musicVideoPath = imagePath
                    listOfMultipleMedia[lastSelectItem].width = contentWidth
                    listOfMultipleMedia[lastSelectItem].height = contentHeight
                    linkAttachmentDetails?.let {
                        if (!it.attachUrl.isNullOrEmpty()) {
                            listOfMultipleMedia[lastSelectItem].finalX = it.finalX
                            listOfMultipleMedia[lastSelectItem].finalY = it.finalY
                            listOfMultipleMedia[lastSelectItem].lastHeight = it.lastHeight
                            listOfMultipleMedia[lastSelectItem].lastWidth = it.lastWidth
                            listOfMultipleMedia[lastSelectItem].lastRotation = it.lastRotation
                            listOfMultipleMedia[lastSelectItem].attachUrl = it.attachUrl
                        }
                    }
                } else {
                    listOfMultipleMedia[lastSelectItem].editedImagePath = imagePath
                    listOfMultipleMedia[lastSelectItem].width = contentWidth
                    listOfMultipleMedia[lastSelectItem].height = contentHeight
                    linkAttachmentDetails?.let {
                        if (!it.attachUrl.isNullOrEmpty()) {
                            listOfMultipleMedia[lastSelectItem].finalX = it.finalX
                            listOfMultipleMedia[lastSelectItem].finalY = it.finalY
                            listOfMultipleMedia[lastSelectItem].lastHeight = it.lastHeight
                            listOfMultipleMedia[lastSelectItem].lastWidth = it.lastWidth
                            listOfMultipleMedia[lastSelectItem].lastRotation = it.lastRotation
                            listOfMultipleMedia[lastSelectItem].attachUrl = it.attachUrl
                        }
                    }
                }

            }
            val lastClickItemIsDelete = data?.getBooleanExtra(IS_DELETE_VIDEO, false)
            if (lastClickItemIsDelete == true) {
                listOfMultipleMedia.removeAt(lastSelectItem)
                if (listOfMultipleMedia.isEmpty()) {
                    totalDuration = 0
                    binding.multipleMediaLayout.isVisible = false
                    if (!isShorts) {
                        binding.mediaTypeLinearLayout.visibility = View.VISIBLE
                    }
                } else {
                    totalDuration -= lastVideoTotalDuration.toInt()
                    binding.multipleMediaLayout.isVisible = true
                    binding.mediaTypeLinearLayout.visibility = View.GONE
                    multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                }

            }
            multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
        } else if (resultCode == RESULT_OK && requestCode == AddStoryActivity.REQUEST_FOR_CHOOSE_PHOTO) {
            val imagePathList = data?.getStringArrayListExtra("FILE_PATH")
            when {
                isStory -> {
                    if (!imagePathList.isNullOrEmpty() && imagePathList.size == 1) {
                        val mimeType = MIME_TYPE_IMAGE_JPEG
                        hideLoading()
                        val intent = SnapEditorActivity.getIntent(
                            this@SnapkitActivity,
                            imagePathList[0],
                            mimeType,
                            playbackSpeed,
                            isShorts,
                            isChallenge,
                            isStory,
                            false,
                            false,
                            listOfMultipleMedia
                        )
                        startActivityForResult(
                            intent, AddNewPostInfoActivity.REQUEST_CODE_CAMERA
                        )
                    }
                }

                isChallenge -> {
                    if (!imagePathList.isNullOrEmpty() && imagePathList.size == 1) {
                        val mimeType = MIME_TYPE_IMAGE_JPEG
                        hideLoading()
                        val intent = SnapEditorActivity.getIntent(
                            this@SnapkitActivity,
                            imagePathList[0],
                            mimeType,
                            playbackSpeed,
                            isShorts,
                            isChallenge,
                            isStory,
                            isClip = false,
                            isFromPreview = false,
                            listOfMultipleMedia = listOfMultipleMedia
                        )
                        startActivityForResult(
                            intent, CreateChallengeActivity.REQUEST_FOR_CHOOSE_PHOTO
                        )
                    }
                }

                isShorts -> {

                }

                else -> {
                    if (!imagePathList.isNullOrEmpty()) {
                        imagePathList.forEach {
                            val totalCount = listOfMultipleMedia.size
                            if (totalCount < 5) {
                                val multipleImageDetails = MultipleImageDetails()
                                multipleImageDetails.mainImagePath = it
                                multipleImageDetails.isVideo = false
                                binding.tvDoneLinearLayout.visibility = View.GONE
                                binding.tvUploadLinearLayout.gravity = Gravity.END
                                listOfMultipleMedia.add(multipleImageDetails)
                                multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                                binding.multipleMediaLayout.isVisible = true
                                binding.mediaTypeLinearLayout.visibility = View.GONE

                            } else {
                                Toast.makeText(this, "You can add maximum 5 photos.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

        } else if (resultCode == RESULT_OK && requestCode == AddStoryActivity.REQUEST_FOR_CHOOSE_VIDEO) {
            val videoPathList = data?.getStringArrayListExtra("FILE_PATH")
            videoPathList?.forEach {
                val duration = getVideoDuration(File(it).toUri())
                totalDuration += duration.toInt()
                val multipleImageDetails = MultipleImageDetails()
                multipleImageDetails.mainVideoPath = it
                multipleImageDetails.isVideo = true
                listOfMultipleMedia.add(multipleImageDetails)
                binding.multipleMediaLayout.isVisible = true
                multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                if (listOfMultipleMedia.size > 0) {
                    binding.mediaTypeLinearLayout.visibility = View.GONE
                }
            }
        } else if (resultCode == RESULT_OK && requestCode == AddNewPostInfoActivity.REQUEST_CODE_CAMERA) {
            setResult(Activity.RESULT_OK, data)
            finish()
        } else if (resultCode == RESULT_OK && requestCode == CreateChallengeActivity.REQUEST_FOR_CHOOSE_PHOTO) {
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    private val galleryOpen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val clipData = result.data?.clipData
            if (clipData != null) {
                if (mediaType == "photo") {
                    if (isStory) {
                        val mimeType = MIME_TYPE_IMAGE_JPEG
                        hideLoading()
                        val selectedUri = clipData.getItemAt(0).uri
                        val filePath = FileUtils.getPath(this@SnapkitActivity, selectedUri) ?: return@registerForActivityResult
                        var media = ""
                        copyMediaExternalToCashMemory(filePath) { newfilepath ->
                            media = if (!newfilepath.isNullOrEmpty()) {
                                newfilepath
                            } else {
                                filePath
                            }
                        }

                        val intent = SnapEditorActivity.getIntent(
                            this@SnapkitActivity, media, mimeType, playbackSpeed, isShorts, isChallenge, isStory, false, false, listOfMultipleMedia
                        )
                        startActivityForResult(
                            intent, AddNewPostInfoActivity.REQUEST_CODE_CAMERA
                        )
                    } else {
                        for (i in 0 until clipData.itemCount) {
                            if (listOfMultipleMedia.size < 5) {
                                val selectedUri = clipData.getItemAt(i).uri
                                val filePath = FileUtils.getPath(this@SnapkitActivity, selectedUri) ?: return@registerForActivityResult
                                val multipleImageDetails = MultipleImageDetails()
                                multipleImageDetails.isVideo = false
                                copyMediaExternalToCashMemory(filePath) { newfilepath ->
                                    if (!newfilepath.isNullOrEmpty()) {
                                        multipleImageDetails.mainImagePath = newfilepath
                                    } else {
                                        multipleImageDetails.mainImagePath = filePath
                                    }
                                }
                                listOfMultipleMedia.add(multipleImageDetails)
                                binding.multipleMediaLayout.isVisible = true
                                multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                                binding.mediaTypeLinearLayout.visibility = View.GONE
                            } else {
                                Toast.makeText(this, "You can add maximum 5 photos. two else ", Toast.LENGTH_SHORT).show()
                                break
                            }
                        }
                    }
                } else {
                    for (i in 0 until clipData.itemCount) {
                        val selectedUri = clipData.getItemAt(i).uri
                        val filePath = FileUtils.getPath(this@SnapkitActivity, selectedUri) ?: return@registerForActivityResult
                        var media = ""
                        copyMediaExternalToCashMemory(filePath) { newfilepath ->
                            media = if (!newfilepath.isNullOrEmpty()) {
                                newfilepath
                            } else {
                                filePath
                            }
                        }
                        val mimeType = if (filePath.lowercase().contains("mp4") || filePath.lowercase().contains("mov") || filePath.lowercase()
                                .contains("MKV")
                        ) MIME_TYPE_VIDEO_MP4 else MIME_TYPE_IMAGE_JPEG
                        if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                            if (totalDuration <= 115) {
                                imageUris.add(media)
                            }
                        } else {
                            val duration = getVideoDuration(selectedUri)
                            totalDuration += duration.toInt()
                            if (totalDuration <= 120) {
                                imageUris.add(media)
                            }
                        }

                    }
                    showLoading(true)
                    processNextImage()
                }
            } else {

                val intent = result.data ?: return@registerForActivityResult
                val selectedUri = intent.data ?: return@registerForActivityResult
                val filePath = FileUtils.getPath(this@SnapkitActivity, selectedUri) ?: return@registerForActivityResult
                val mimeType = if (filePath.lowercase().contains("mp4") || filePath.lowercase().contains("mov") || filePath.lowercase()
                        .contains("MKV")
                ) MIME_TYPE_VIDEO_MP4 else MIME_TYPE_IMAGE_JPEG
                if (mediaType == "photo") {
                    if (isChallenge || isStory) {
                        val intent = SnapEditorActivity.getIntent(
                            this@SnapkitActivity, filePath, mimeType, playbackSpeed, isShorts, isChallenge, isStory, false, false, listOfMultipleMedia
                        )
                        if (isChallenge) {
                            startActivityForResult(
                                intent, CreateChallengeActivity.REQUEST_FOR_CHOOSE_PHOTO
                            )
                        } else {
                            startActivityForResult(
                                intent, AddNewPostInfoActivity.REQUEST_CODE_CAMERA
                            )
                        }
                    } else {
                        val totalCount = listOfMultipleMedia.size
                        if (totalCount < 5) {
                            val multipleImageDetails = MultipleImageDetails()
                            multipleImageDetails.mainImagePath = filePath
                            multipleImageDetails.isVideo = false
                            listOfMultipleMedia.add(multipleImageDetails)
                            multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                            binding.multipleMediaLayout.isVisible = true
                            binding.mediaTypeLinearLayout.visibility = View.GONE

                        } else {
                            Toast.makeText(this, "You can add maximum 5 photos.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                        if (totalDuration <= 115) {
                            val savedFile = saveVideoToCacheDir(context, "Edited_video_${System.currentTimeMillis()}")
                            generateVideoFromImage(File(filePath), savedFile.path, false)
                        }
                    } else {
                        val duration = getVideoDuration(selectedUri)
                        totalDuration += duration.toInt()
                        if (totalDuration <= 120) {
                            val multipleImageDetails = MultipleImageDetails()
                            multipleImageDetails.mainVideoPath = filePath
                            multipleImageDetails.isVideo = true
                            listOfMultipleMedia.add(multipleImageDetails)
                            multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                            binding.multipleMediaLayout.isVisible = true
                            binding.mediaTypeLinearLayout.visibility = View.GONE
                        } else {
                            showToast("Capture limit reached. Try trimming the video.")
                        }
                    }
                }
            }
        }
    }

    private fun copyMediaExternalToCashMemory(inputFilePath: String, callback: (String) -> Unit) {
        val mainDir = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS)
        val childDir = File(mainDir, "MufTempFile")
        if (!childDir.exists()) {
            childDir.mkdirs()
        }

        val sourceFile = File(inputFilePath)
        val fileName = sourceFile.name
        val destFile = File(childDir, fileName)
        try {
            val inputStream = FileInputStream(sourceFile)
            val outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            inputStream.close()
            outputStream.close()
            callback(destFile.path.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            callback("")
        }
    }

    private fun processNextImage() {
        if (currentImageIndex < imageUris.size) {
            val imageUri = imageUris[currentImageIndex]
            val mimeType = if (imageUri.lowercase().contains("mp4") || imageUri.lowercase().contains("mov") || imageUri.lowercase()
                    .contains("MKV")
            ) MIME_TYPE_VIDEO_MP4 else MIME_TYPE_IMAGE_JPEG
            if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                val savedFile = saveVideoToCacheDir(context, "Edited_video_${System.currentTimeMillis()}")
                generateVideoFromImage(File(imageUri), savedFile.path, true)
            } else {
                val duration = getVideoDuration(File(imageUri).toUri())
                totalDuration += duration.toInt()
                val multipleImageDetails = MultipleImageDetails()
                multipleImageDetails.mainVideoPath = imageUri
                multipleImageDetails.isVideo = true
                listOfMultipleMedia.add(multipleImageDetails)
                multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                binding.multipleMediaLayout.visibility = View.VISIBLE
                binding.mediaTypeLinearLayout.visibility = View.GONE
                currentImageIndex++
                processNextImage()
            }
        } else {
            hideLoading()
            multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
        }
    }

    private fun initAdapter() {
        multipleMediaAdapter = MultipleMediaAdapter(this, false).apply {
            chatRoomItemClick.subscribeAndObserveOnMainThread {
                lastSelectItem = listOfMultipleMedia.indexOf(it)
                if (it.isVideo == true) {
                    val imageUri = it.editedVideoPath ?: it.mainVideoPath
                    if (imageUri != null) {
                        val mimeType = if (imageUri.lowercase().contains("mp4") || imageUri.lowercase().contains("mov") || imageUri.lowercase()
                                .contains("MKV")
                        ) MIME_TYPE_VIDEO_MP4 else MIME_TYPE_IMAGE_JPEG
                        val intent = SnapEditorActivity.getIntent(
                            this@SnapkitActivity, imageUri, mimeType, playbackSpeed, isShorts, isChallenge, isStory, true, false, listOfMultipleMedia
                        )
                        startActivityForResult(intent, REQUEST_CODE_FOR_EDTIOR)
                    }
                } else {
                    val imageUri = it.editedImagePath ?: it.mainImagePath
                    if (imageUri != null) {
                        val mimeType = if (imageUri.lowercase().contains("mp4") || imageUri.lowercase().contains("mov") || imageUri.lowercase()
                                .contains("MKV")
                        ) MIME_TYPE_VIDEO_MP4 else MIME_TYPE_IMAGE_JPEG
                        val intent = SnapEditorActivity.getIntent(
                            this@SnapkitActivity, imageUri, mimeType, playbackSpeed, isShorts, isChallenge, isStory, true, false, listOfMultipleMedia
                        )
                        startActivityForResult(intent, REQUEST_CODE_FOR_EDTIOR)
                    }
                }

            }.autoDispose()
            closeClick.subscribeAndObserveOnMainThread {
                lastSelectItem = listOfMultipleMedia.indexOf(it)
                val listOfMedia = multipleMediaAdapter.listOfDataItems as ArrayList
                listOfMedia.removeAt(lastSelectItem)
                if (listOfMedia.isEmpty()) {
                    totalDuration = 0
                    binding.multipleMediaLayout.isVisible = false
                    if (!isShorts) {
                        binding.mediaTypeLinearLayout.visibility = View.VISIBLE
                    }
                } else {
                    multipleMediaAdapter.listOfDataItems = listOfMedia
                    binding.multipleMediaLayout.isVisible = true
                    binding.mediaTypeLinearLayout.visibility = View.GONE
                    multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                }
            }.autoDispose()
        }
        binding.rvMultipleMedia.apply {
            adapter = multipleMediaAdapter
            layoutManager = LinearLayoutManager(this@SnapkitActivity, RecyclerView.HORIZONTAL, false)
        }
        runOnUiThread {
            multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
        }
    }

    private fun mergeVideosWithFFmpeg(inputVideos: List<File>, outputVideo: String, callback: (Boolean) -> Unit) {
        val commonWidth = 1080
        val commonHeight = 1920

        val inputFiles = inputVideos.map { it.path }.toTypedArray()

        val videoFilters = mutableListOf<String>()
        val audioFilters = mutableListOf<String>()
        val videoConcatParts = mutableListOf<String>()
        val audioConcatParts = mutableListOf<String>()

        var videoCount = 0
        var audioCount = 0

        inputFiles.forEachIndexed { index, path ->
            videoFilters.add("[$index:v]scale=$commonWidth:$commonHeight,setsar=1[v$index]")
            videoConcatParts.add("[v$index]")
            videoCount++

            val hasAudio = checkIfFileHasAudio(path)
            if (hasAudio) {
                audioFilters.add("[$index:a]anull[a$index]")
                audioConcatParts.add("[a$index]")
                audioCount++
            }
        }

        val filterComplex = buildString {
            append(videoFilters.joinToString(";"))
            if (audioFilters.isNotEmpty()) {
                append(";")
                append(audioFilters.joinToString(";"))
            }
            append(";")
            append(videoConcatParts.joinToString("") + "concat=n=$videoCount:v=1[outv]")
            if (audioCount > 0) {
                append(";")
                append(audioConcatParts.joinToString("") + "concat=n=$audioCount:v=0:a=1[outa]")
            }
        }


        val command = mutableListOf<String>(
            *inputFiles.flatMap { listOf("-i", it) }.toTypedArray(), "-filter_complex", filterComplex, "-map", "[outv]"
        )

        if (audioCount > 0) {
            command.add("-map")
            command.add("[outa]")
        }

        command.addAll(
            listOf(
                "-c:v", "mpeg4", // Specify video codec
                "-c:a", "aac", // Specify audio codec
                "-q:v", "6", "-preset", "ultrafast", // Optional: Adjust encoding speed
                "-r", "30", // Frame rate
                outputVideo
            )
        )


        FFmpegUtils().callFFmpegQuery(command.toTypedArray(), object : FFmpegCallBack {
            override fun process(logMessage: LogMessage) {
                Timber.tag("FFmpeg").d("process = $logMessage")
            }

            override fun success() {
                Timber.tag("FFmpeg").d("success")
                callback(true)
            }

            override fun cancel() {
                Timber.tag("FFmpeg").d("cancel, please try again.")
                callback(false)
            }

            override fun failed() {
                Timber.tag("FFmpeg").d("failed, please try again.")
                callback(false)
            }

        })
    }

    private fun checkIfFileHasAudio(filePath: String): Boolean {
        val probeResult = FFprobe.getMediaInformation(filePath)
        return probeResult.streams.any { it.type == "audio" }
    }

    private fun generateVideoFromImage(imageFile: File, outputFilePath: String, isNext: Boolean) {

        val imageWidth = 1080
        val imageHeight = 1720

        val silentMusic = R.raw.silent_music_five_second
        val tempMusicFile = File(this.cacheDir, "temp_music.mp3")

        if (!copyRawResourceToFile(this, silentMusic, tempMusicFile)) {
            Timber.tag("FFmpeg").e("Failed to copy music file to temporary location")
            hideLoading()
            return
        }

        Timber.tag("FFmpeg").i("silentMusic = " + tempMusicFile.absolutePath)

        // FFmpeg command to create a video from image with music
        val command = arrayOf(
            "-loop", "1", "-i", imageFile.absolutePath, "-i", tempMusicFile.absolutePath, // Use the temporary music file
            "-c:v", "mpeg4", "-q:v", "3", "-t", "5", // Duration of the video (in seconds)
            "-s", "${imageWidth}x${imageHeight}", // Size of the video
            "-c:a", "aac", // Audio codec for the output
            "-shortest", // Finish encoding when the shortest input stream ends
            outputFilePath
        )

        FFmpegUtils().callFFmpegQuery(command, object : FFmpegCallBack {
            override fun process(logMessage: LogMessage) {
                Timber.tag("FFmpeg").d("process = $logMessage")
            }

            override fun success() {
                Timber.tag("FFmpeg").d("success")
                hideLoading()
                binding.multipleMediaLayout.isVisible = true
                val multipleImageDetails = MultipleImageDetails()
                multipleImageDetails.mainVideoPath = outputFilePath
                multipleImageDetails.isVideo = true
                listOfMultipleMedia.add(multipleImageDetails)
                multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
                binding.mediaTypeLinearLayout.visibility = View.GONE
                val duration = getVideoDuration(File(outputFilePath).toUri())
                totalDuration += duration.toInt()
                if (isNext) {
                    showLoading(true)
                    currentImageIndex++
                    processNextImage()
                }
            }

            override fun cancel() {
                hideLoading()
                Timber.tag("FFmpeg").d("cancel, please try again.")
            }

            override fun failed() {
                hideLoading()
                Timber.tag("FFmpeg").d("failed, please try again.")
            }

        })

    }

    private fun copyRawResourceToFile(context: Context, resourceId: Int, outputFile: File): Boolean {
        return try {
            val inputStream: InputStream = context.resources.openRawResource(resourceId)
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            inputStream.close()
            outputStream.close()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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

    fun getCenterZoomedItemPosition(recyclerView: RecyclerView): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val orientation = layoutManager.orientation

        // Calculate the center position of the RecyclerView
        val center = if (orientation == LinearLayoutManager.HORIZONTAL) recyclerView.width / 2
        else recyclerView.height / 2

        var closestChildPosition = RecyclerView.NO_POSITION
        var closestDistance = Float.MAX_VALUE

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val childCenter = if (orientation == LinearLayoutManager.HORIZONTAL) {
                (layoutManager.getDecoratedLeft(child) + layoutManager.getDecoratedRight(child)) / 2
            } else {
                (layoutManager.getDecoratedTop(child) + layoutManager.getDecoratedBottom(child)) / 2
            }

            val distance = Math.abs(childCenter - center)
            if (distance < closestDistance) {
                closestDistance = distance.toFloat()
                closestChildPosition = layoutManager.getPosition(child)
            }
        }

        return closestChildPosition
    }

    fun displayTimer() {
        var countTime = 4
        countTimerDisposable?.dispose()
        countTimerDisposable = Observable.interval(1, TimeUnit.SECONDS).subscribeAndObserveOnMainThread {
            countTime--
            binding.tvTimer.isVisible = true
            binding.tvTimer.text = countTime.toString()
            if (countTime <= 0) {
                binding.tvTimer.isVisible = false
                countTimerDisposable?.dispose()
                if (mediaType == "photo") {
                    binding.captureButton.onCaptureRequestListener?.onStart(SnapButtonView.CaptureType.SNAPSHOT)
                }
            }
        }

    }


    private fun getVideoDuration(uri: Uri): Double {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        retriever.release()
        return (duration / 1000).toDouble()
    }

    fun deleteAllPreviousCacheFiles(context: Context) {
        val cacheDir = context.cacheDir
        val specificCacheDir = File(cacheDir, "temp_bitmap")

        if (specificCacheDir.exists()) {
            Timber.tag("deleteFileRecursively").d(" is deleted ..11")
            val files = specificCacheDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.delete()) {
                        Timber.tag("deleteFileRecursively").d("${file.absolutePath} is deleted successfully")
                    } else {
                        Timber.tag("deleteFileRecursively").e("${file.absolutePath} is not deleted")
                    }
                }
            }
            if (specificCacheDir.delete()) {
                Timber.tag("deleteFileRecursively").d("${specificCacheDir.absolutePath} is deleted successfully1")
            } else {
                Timber.tag("deleteAllPreviousFiles").e("$specificCacheDir is not deleted")
            }
        } else {
            Timber.tag("deleteFileRecursively").d("${specificCacheDir.absolutePath} does not exist")
        }
    }

    fun deleteAllPreviousCacheVideoFiles(context: Context) {
        Timber.tag("deleteFileRecursively").d(" V is deleted ..")
        val cacheDir = context.cacheDir
        val specificCacheDir = File(cacheDir, "temp_Video")

        if (specificCacheDir.exists()) {
            Timber.tag("deleteFileRecursively").d("V is deleted ..11")
            val files = specificCacheDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.delete()) {
                        Timber.tag("deleteFileRecursively").d("${file.absolutePath} V is deleted successfully")
                    } else {
                        Timber.tag("deleteFileRecursively").e("${file.absolutePath}  V is not deleted")
                    }
                }
            }
            if (specificCacheDir.delete()) {
                Timber.tag("deleteFileRecursively").d("${specificCacheDir.absolutePath} V is deleted successfully1")
            } else {
                Timber.tag("deleteAllPreviousFiles").e("$specificCacheDir V is not deleted")
            }
        } else {
            Timber.tag("deleteFileRecursively").d("${specificCacheDir.absolutePath} V does not exist")
        }
    }

}
