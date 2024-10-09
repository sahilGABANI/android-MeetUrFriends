package com.meetfriend.app.ui.camerakit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.isVisible
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.meetfriend.app.R
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.StoryUploadData
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.api.post.model.LinkAttachmentDetails
import com.meetfriend.app.api.post.model.MultipleImageDetails
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.application.MeetFriendApplication.Companion.context
import com.meetfriend.app.databinding.ActivitySnapPreviewBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.camerakit.model.LauncherGetIntent
import com.meetfriend.app.ui.camerakit.utils.MIME_TYPE_IMAGE_JPEG
import com.meetfriend.app.ui.camerakit.utils.MIME_TYPE_VIDEO_MP4
import com.meetfriend.app.ui.camerakit.view.MultipleMediaAdapter
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewModel
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewState
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.utilclasses.download.DownloadService
import com.meetfriend.app.utils.FileUtils
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL
import javax.inject.Inject

class SnapPreviewActivity : BasicActivity() {

    companion object {
        const val THOUSAND = 1000
        const val TEN_THOUSAND = 10000
        const val DURATION = 1000L
        const val ZERO_FIVE = 0.5f
        const val INTENT_VIDEO_PATH = "path"
        private const val BUNDLE_ARG_PLAYBACK_SPEED = "playback_speed"
        private const val BUNDLE_MIME_TYPE = "mime_type"
        var IS_SHORTS = "IS_SHORTS"
        var IS_CHALLENGE = "IS_CHALLENGE"
        var IS_STORY = "IS_STORY"
        const val LINK_ATTACHMENT_DETAILS = "LinkAttachmentDetails"
        const val LIST_OF_MULTIPLE_MEDIA = "listOfMultipleMedia"
        const val INTENT_TAG_NAME = "INTENT_TAG_NAME"

        fun getIntent(
            launcherGetIntent: LauncherGetIntent
        ): Intent {
            val intent = Intent(launcherGetIntent.context, SnapPreviewActivity::class.java)
            intent.putExtra(INTENT_VIDEO_PATH, launcherGetIntent.path)
            intent.putExtra(BUNDLE_MIME_TYPE, launcherGetIntent.mimeType)
            intent.putExtra(BUNDLE_ARG_PLAYBACK_SPEED, launcherGetIntent.playBackSpeed)
            intent.putExtra(IS_SHORTS, launcherGetIntent.isShorts)
            intent.putExtra(IS_CHALLENGE, launcherGetIntent.isChallenge)
            intent.putExtra(IS_STORY, launcherGetIntent.isStory)
            intent.putExtra(LINK_ATTACHMENT_DETAILS, launcherGetIntent.linkAttachmentDetails)
            intent.putParcelableArrayListExtra(LIST_OF_MULTIPLE_MEDIA, launcherGetIntent.listOfMultipleMedia)
            if (!launcherGetIntent.tagName.isNullOrEmpty()) {
                intent.putExtra(
                    SnapEditorActivity.INTENT_TAG_NAME,
                    launcherGetIntent.tagName
                )
            }
            return intent
        }
    }

    lateinit var binding: ActivitySnapPreviewBinding
    private var playBackSpeed = 1f
    private var mimeType = MIME_TYPE_IMAGE_JPEG
    private var player: ExoPlayer? = null
    private var isChallenge: Boolean = false
    private var isShorts: Boolean = false
    private var isStory: Boolean = false
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var duration: Long = 0L
    private var mediaUrl = ""
    private var tagName: String? = null
    private var linkAttachmentDetails: LinkAttachmentDetails? = null
    private var listOfMultipleMedia: ArrayList<MultipleImageDetails>? = arrayListOf()
    private lateinit var multipleMediaAdapter: MultipleMediaAdapter
    val videoFileName: String
        get() {
            val rnds = (0..TEN_THOUSAND).random()

            return "video_${System.currentTimeMillis()}_$rnds.mp4"
        }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddNewPostViewModel>
    private lateinit var deepArViewModel: AddNewPostViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySnapPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this@SnapPreviewActivity)
        deepArViewModel = getViewModelFromFactory(viewModelFactory)

        if (isOnline(this) && isStory) {
            deepArViewModel.getCloudFlareConfig(false)
        } else if (isStory) {
            showToast("No internet connection")
        }
        setIntentData()

        listenToViewModel()
        listenToViewEvents()
        if (mediaUrl.isNullOrEmpty()) {
            val imagePath = listOfMultipleMedia?.get(0)
                ?.editedImagePath ?: listOfMultipleMedia?.get(0)?.mainImagePath ?: ""
            setupMediaIfNeeded(imagePath)
            initAdapter()
        } else {
            setupMediaIfNeeded(mediaUrl)
        }

        if (isShorts || isChallenge || isStory) {
            binding.multipleMediaLayout.setBackgroundColor(Color.TRANSPARENT)
            binding.rvMultipleMedia.isVisible = false
        }
    }
    private fun setIntentData() {
        playBackSpeed = intent?.let { it.getFloatExtra(BUNDLE_ARG_PLAYBACK_SPEED, 1f) } ?: 1f
        mimeType = intent?.let { it.getStringExtra(BUNDLE_MIME_TYPE) } ?: MIME_TYPE_IMAGE_JPEG
        isShorts = intent?.getBooleanExtra(IS_SHORTS, false) ?: false
        isChallenge = intent?.getBooleanExtra(IS_CHALLENGE, false) ?: false
        isStory = intent?.getBooleanExtra(IS_STORY, false) ?: false
        linkAttachmentDetails = intent.getParcelableExtra(LINK_ATTACHMENT_DETAILS)
        listOfMultipleMedia = intent.getParcelableArrayListExtra(LIST_OF_MULTIPLE_MEDIA) ?: arrayListOf()
        mediaUrl = intent.getStringExtra(INTENT_VIDEO_PATH) ?: ""

        if (intent?.hasExtra(INTENT_TAG_NAME) == true) {
            tagName = intent?.getStringExtra(INTENT_TAG_NAME)
        }
    }
    private fun setupMediaIfNeeded(path: String) {
        if (player == null && mimeType == MIME_TYPE_VIDEO_MP4) {
            binding.rvMultipleMedia.visibility = View.GONE
            val exoPlayer = ExoPlayer.Builder(this).build()
            binding.videPlayer.player = exoPlayer

            val dataSourceFactory = DefaultDataSourceFactory(this)
            val mediaSource = ProgressiveMediaSource.Factory(
                dataSourceFactory
            ).createMediaSource(MediaItem.fromUri(path))
            val mainHandler = Handler(Looper.getMainLooper())
            mediaSource.addEventListener(
                mainHandler,
                object : MediaSourceEventListener {

                    override fun onLoadCompleted(
                        windowIndex: Int,
                        mediaPeriodId: MediaSource.MediaPeriodId?,
                        loadEventInfo: LoadEventInfo,
                        mediaLoadData: MediaLoadData
                    ) {
                        mediaSource.removeEventListener(this)
                        startPostponedEnterTransition()
                    }

                    override fun onLoadError(
                        windowIndex: Int,
                        mediaPeriodId: MediaSource.MediaPeriodId?,
                        loadEventInfo: LoadEventInfo,
                        mediaLoadData: MediaLoadData,
                        error: IOException,
                        wasCanceled: Boolean
                    ) {
                        mediaSource.removeEventListener(this)
                        finish()
                    }
                }
            )

            exoPlayer.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    binding.videPlayer.setBackgroundColor(Color.BLACK)
                }
            })

            exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            exoPlayer.playWhenReady = true
            exoPlayer.setPlaybackSpeed(playBackSpeed)
            exoPlayer.prepare(mediaSource, false, false)

            player = exoPlayer
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val width: Int = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
            val height: Int = Integer.valueOf(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            )
            retriever.release()
            val screenHeight = resources.displayMetrics.heightPixels
            if (height <= screenHeight) {
                binding.flContainer.layoutParams.apply {
                    this.height = height
                    this.width = width
                }
            }
        } else if (mimeType == MIME_TYPE_IMAGE_JPEG) {
            if (listOfMultipleMedia?.isNotEmpty() == true) {
                binding.rvMultipleMedia.visibility = View.VISIBLE

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
                            return false
                        }
                    }).into(binding.imagePreview)
                }
            } else {
                binding.imagePreview.post {
                    Glide.with(this).load(mediaUrl).listener(object : RequestListener<Drawable> {
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
                            val width = resource.intrinsicWidth
                            val height = resource.intrinsicHeight
                            binding.flContainer.layoutParams?.apply {
                                this.height = height
                                this.width = width
                            }
                            return false
                        }
                    }).into(binding.imagePreview)
                }
                binding.rvMultipleMedia.visibility = View.GONE
            }
        }
    }

    private fun initAdapter() {
        multipleMediaAdapter = MultipleMediaAdapter(this, true).apply {
            chatRoomItemClick.subscribeAndObserveOnMainThread { item ->
                val listOfMedia = multipleMediaAdapter.listOfDataItems
                listOfMedia?.forEach {
                    it.isSelected = false
                }
                listOfMedia?.find { it.mainImagePath == item.mainImagePath }?.apply {
                    isSelected = true
                }
                multipleMediaAdapter.listOfDataItems = listOfDataItems
                setupMediaIfNeeded(item.editedImagePath ?: item.mainImagePath ?: "")
            }.autoDispose()
            addMediaClick.subscribeAndObserveOnMainThread {
                onBackPressedDispatcher.onBackPressed()
            }.autoDispose()
            closeClick.subscribeAndObserveOnMainThread {
                val lastSelectItem = multipleMediaAdapter.listOfDataItems?.indexOf(it)
                val listOfMedia = multipleMediaAdapter.listOfDataItems as ArrayList
                lastSelectItem?.let {
                    listOfMedia.removeAt(lastSelectItem)
                }
                multipleMediaAdapter.listOfDataItems = listOfMedia
            }.autoDispose()
        }
        binding.rvMultipleMedia.apply {
            adapter = multipleMediaAdapter
            layoutManager = LinearLayoutManager(this@SnapPreviewActivity, RecyclerView.HORIZONTAL, false)
        }
        listOfMultipleMedia?.get(0)?.isSelected = true
        multipleMediaAdapter.listOfDataItems = listOfMultipleMedia
    }

    private fun listenToViewEvents() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
        binding.tvDone.throttleClicks().subscribeAndObserveOnMainThread {
            if (!isStory && !isChallenge && !isShorts) {
                if (listOfMultipleMedia.isNullOrEmpty()) {
                    openEditor(Uri.fromFile(File(mediaUrl)))
                } else {
                    listOfMultipleMedia?.let {
                        openEditor(Uri.fromFile(File(it[0].editedImagePath ?: it[0].mainImagePath ?: "")))
                    }
                }
            } else {
                if (listOfMultipleMedia.isNullOrEmpty()) {
                    openEditor(Uri.fromFile(File(mediaUrl)))
                } else {
                    listOfMultipleMedia?.let {
                        openEditor(Uri.fromFile(File(it[0].editedImagePath ?: it[0].mainImagePath ?: "")))
                    }
                }
            }
        }.autoDispose()
    }

    private fun openEditor(inputImage: Uri) {
        val filePath = FileUtils.getPath(this@SnapPreviewActivity, inputImage) ?: return
        if (isChallenge) {
            setChallengeIntent(filePath)
        } else if (isStory) {
            if (mimeType == MIME_TYPE_IMAGE_JPEG) {
                showLoading(true)
                cloudFlareConfig?.let {
                    deepArViewModel.uploadImageToCloudFlare(this, it, File(filePath))
                } ?: deepArViewModel.getCloudFlareConfig(false)
            } else {
                if (playBackSpeed != 1f) {
                    updatePlaybackSpeed(inputImage)
                } else {
                    compressVideo(mediaUrl, inputImage)
                }
            }
        } else if (
            inputImage.toString().lowercase().contains("mp4") ||
            inputImage.toString().lowercase().contains("mov") ||
            inputImage.toString()
                .lowercase().contains("MKV")
        ) {
            showLoading(true)
            if (playBackSpeed != 1f) {
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

    private fun setChallengeIntent(filePath: String) {
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
    }

    private fun compressVideo(videoPath: String, inputImage: Uri) {
        val videoUris = listOf(Uri.fromFile(File(videoPath)))
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: DURATION
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
                    override fun onProgress(index: Int, percent: Float) = Unit

                    override fun onStart(index: Int) {
                        Timber.wtf("onStart")
                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        hideLoading()
                        if (isShorts) {
                            val intent = AddNewPostInfoActivity.launchActivity(
                                LaunchActivityData(
                                    this@SnapPreviewActivity,
                                    postType = AddNewPostInfoActivity.POST_TYPE_VIDEO,
                                    imagePathList = arrayListOf(),
                                    videoPath = videoPath,
                                    tagName = tagName,
                                    videoUri = inputImage,
                                    linkAttachmentDetails = linkAttachmentDetails,
                                    listOfMultipleMedia = listOfMultipleMedia
                                )
                            )
                            startActivity(intent)
                            finish()
                        } else if (isStory) {
                            setIsStoryIntent(path)
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
    private fun setIsStoryIntent(path: String?) {
        if (isOnline(this@SnapPreviewActivity)) {
            if (path.toString().isNotEmpty()) {
                cloudFlareConfig?.let {
                    startActivity(
                        MainHomeActivity.getIntentFromStoryUpload(
                            this@SnapPreviewActivity,
                            StoryUploadData(
                                cloudFlareConfig ?: CloudFlareConfig(),
                                path.toString(),
                                (duration / THOUSAND).toInt().toString(),
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
                    if (linkAttachmentDetails != null && !linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
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
                            webUrl = linkAttachmentDetails?.attachUrl
                        )
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
                    startActivity(MainHomeActivity.getIntent(this@SnapPreviewActivity))
                }

                else -> {}
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } ?: false
    }

    private fun updatePlaybackSpeed(inputImage: Uri) {
        if (!mediaUrl.isNullOrEmpty() && mimeType == MIME_TYPE_VIDEO_MP4) {
            val videoUrl = URL(Uri.fromFile(File(mediaUrl)).toString()).toString()
            val outputFilePath = getFilePath(videoFileName)
            val speed = if (
                playBackSpeed == 2f
            ) { "setpts=0.5*PTS" } else if (playBackSpeed == ZERO_FIVE) { "setpts=2.0*PTS" } else { "setpts=1.0*PTS" }
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

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
