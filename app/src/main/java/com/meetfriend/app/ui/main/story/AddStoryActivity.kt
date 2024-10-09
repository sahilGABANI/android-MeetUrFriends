package com.meetfriend.app.ui.main.story

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.StoryUploadData
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityAddStoryBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.home.create.PostCameraBottomSheet
import com.meetfriend.app.ui.home.shorts.PostCameraActivity
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewModel
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewState
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.mediapicker.adapter.PhotoListByAlbumAdapter
import com.meetfriend.app.ui.mediapicker.adapter.VideoListByAlbumAdapter
import com.meetfriend.app.ui.mediapicker.fragments.PhotoAlbumListBSDialogFragment
import com.meetfriend.app.ui.mediapicker.fragments.VideoAlbumListBSDialogFragment
import com.meetfriend.app.ui.mediapicker.interfaces.PhotoListByAlbumAdapterCallback
import com.meetfriend.app.ui.mediapicker.interfaces.VideoListByAlbumAdapterCallback
import com.meetfriend.app.ui.mediapicker.model.AlbumPhotoModel
import com.meetfriend.app.ui.mediapicker.model.AlbumVideoModel
import com.meetfriend.app.ui.mediapicker.model.PhotoModel
import com.meetfriend.app.ui.mediapicker.model.VideoModel
import com.meetfriend.app.ui.mediapicker.utils.PhotoUtils
import com.meetfriend.app.ui.mediapicker.utils.VideoUtils
import com.meetfriend.app.utils.Constant
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class AddStoryActivity : BasicActivity(), PhotoListByAlbumAdapterCallback, VideoListByAlbumAdapterCallback {

    companion object {
        private const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"
        private const val MEDIA_TYPE_VIDEO = "POST_TYPE_VIDEO"
        private const val MEDIA_TYPE = "MEDIA_TYPE"
        private const val SELECT_LIMIT = "SELECT_LIMIT"
        private const val IS_SINGLE_SELECTION = "IS_SINGLE_SELECTION"
        const val REQUEST_FOR_CHOOSE_PHOTO = 1889
        const val REQUEST_FOR_CHOOSE_VIDEO = 1888
        const val FOUR = 4
        const val THOUSAND = 1000
        const val THOUSAND_DURATION = 1000L
        const val ONE_HUNDRED_EIGHTY = 180

        fun launchActivity(context: Context, mediaType: String): Intent {
            return Intent(context, AddStoryActivity::class.java).putExtra(MEDIA_TYPE, mediaType)
        }

        fun launchActivity(context: Context, mediaType: String, isSingleSelection: Boolean, selectLimit: Int): Intent {
            val intent = Intent(context, AddStoryActivity::class.java)
            intent.putExtra(MEDIA_TYPE, mediaType)
            intent.putExtra(SELECT_LIMIT, selectLimit)
            intent.putExtra(IS_SINGLE_SELECTION, isSingleSelection)
            return intent
        }

        private var selectedPhotoPathArrayList = ArrayList<String>()
        private var isCameraPermissionGrantedAfterDenied = false
        private var duration: Long = 0L
        private var selectLimit: Int = 0
        private var isSingleSelection: Boolean = false

        fun isPhotoSelected(photoModel: PhotoModel): Int {
            for (i in 0 until selectedPhotoPathArrayList.size) {
                if (selectedPhotoPathArrayList[i] == photoModel.path) {
                    return i
                }
            }
            return -1
        }

        private var selectedVideoPathArrayList = ArrayList<String>()

        fun isVideoSelected(videoModel: VideoModel): Int {
            for (i in 0 until selectedVideoPathArrayList.size) {
                if (selectedVideoPathArrayList[i] == videoModel.filePath) {
                    return i
                }
            }
            return -1
        }
    }

    private lateinit var binding: ActivityAddStoryBinding

    private var albumWithPhotoArrayList = ArrayList<AlbumPhotoModel>()
    private var photoModelArrayList = ArrayList<PhotoModel>()

    private var albumWithVideoArrayList = ArrayList<AlbumVideoModel>()
    private var videoModelArrayList = ArrayList<VideoModel>()
    private val arrayList: java.util.ArrayList<Uri> = java.util.ArrayList()

    private var selectedMediaType = MEDIA_TYPE_IMAGE

    private var isPermissionGrantedAfterDenied: Boolean = false

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MainHomeViewModel>
    private lateinit var mainHomeViewModel: MainHomeViewModel
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var mediaType: String = "photo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)

        binding = ActivityAddStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainHomeViewModel = getViewModelFromFactory(viewModelFactory)
        mediaType = intent?.getStringExtra(MEDIA_TYPE) ?: "photo"
        selectLimit = intent?.getIntExtra(SELECT_LIMIT, 0) ?: 0
        isSingleSelection = intent?.getBooleanExtra(IS_SINGLE_SELECTION, false) ?: false
        selectedMediaType = if (mediaType == "photo") MEDIA_TYPE_IMAGE else MEDIA_TYPE_VIDEO
        checkPermissionGranted(this)
        listenToViewEvents()
        listenToViewModel()
        if (isOnline(this)) {
            mainHomeViewModel.getCloudFlareConfig(false)
        } else {
            showToast("No internet connection")
        }
        if (selectedMediaType == MEDIA_TYPE_IMAGE) {
            binding.tvAlbumName.text = resources.getString(R.string.label_all_photos)
            loadAlbumWihPhotos(this@AddStoryActivity)
            loadSelectedAlbumWithPhotoList(0)
            binding.ivSwitchMedia.setImageResource(R.drawable.ic_video_play)
            binding.cvMultipleSelect.visibility = View.GONE
        } else {
            binding.tvAlbumName.text = resources.getString(R.string.label_all_videos)
            binding.cvMultipleSelect.visibility = View.GONE
            loadAlbumWihVideos(this@AddStoryActivity)
            loadAlbumWihPhotos(this)
            loadSelectedAlbumWithVideoList(0)
            binding.ivSwitchMedia.setImageResource(R.drawable.ic_post_multiple_select)
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)

        return capabilities?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } ?: false
    }

    private fun listenToViewModel() {
        mainHomeViewModel.mainHomeState.subscribeAndObserveOnMainThread {
            when (it) {
                is MainHomeViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }

                is MainHomeViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressedDispatcher.onBackPressed()
                }

                is MainHomeViewState.UploadImageCloudFlareSuccess -> {
                    val addStoryRequest = AddStoryRequest(
                        type = "image",
                        image = it.imageUrl
                    )
                    mainHomeViewModel.addStoryImage(addStoryRequest)
                }

                is MainHomeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is MainHomeViewState.AddStoryResponse -> {
                    hideLoading()
                    mp?.track(Constant.ADD_STORY)
                    val type = "Stories count"
                    mp?.people?.increment(type, 1.0)
                    selectedPhotoPathArrayList.clear()
                    selectedVideoPathArrayList.clear()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    startActivity(MainHomeActivity.getIntent(this))
                    finish()
                }

                is MainHomeViewState.StoryLoadingState -> {
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context).permission(Permission.READ_MEDIA_IMAGES).request(object : OnPermissionCallback {

            override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                if (all) {
                    isPermissionGrantedAfterDenied = false
                    listenToViewEvents()
                    loadAlbumWihPhotos(this@AddStoryActivity)
                    loadAlbumWihVideos(this@AddStoryActivity)
                } else {
                    showToast(getString(R.string.msg_permission_denied))
                }
            }

            override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                super.onDenied(permissions, never)
                if (never) {
                    isPermissionGrantedAfterDenied = true

                    XXPermissions.startPermissionActivity(this@AddStoryActivity, permissions)
                } else {
                    showToast(getString(R.string.msg_permission_denied))
                }
            }
        })
    }

    private fun listenToViewEvents() {
        selectedPhotoPathArrayList = ArrayList()
        selectedVideoPathArrayList = ArrayList()

        albumWithPhotoArrayList = ArrayList()
        photoModelArrayList = ArrayList()

        albumWithVideoArrayList = ArrayList()
        videoModelArrayList = ArrayList()
        binding.apply {
            ivClose.throttleClicks().subscribeAndObserveOnMainThread {
                onBackPressedDispatcher.onBackPressed()
            }.autoDispose()
            ivNext.throttleClicks().subscribeAndObserveOnMainThread {
                heandelNextButton()
            }.autoDispose()
            tvAlbumName.throttleClicks().subscribeAndObserveOnMainThread {
                if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                    showPhotoAlbumListDialog()
                } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                    showVideoAlbumListDialog()
                }
            }.autoDispose()
            cvMultipleSelect.throttleClicks().subscribeAndObserveOnMainThread {
                if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                    if (albumWithVideoArrayList.isNotEmpty()) {
                        binding.ivSwitchMedia.setImageResource(R.drawable.ic_post_multiple_select)
                        selectedPhotoPathArrayList.clear()
                        selectedVideoPathArrayList.clear()
                        checkSelectedMediaManageVisibility()
                        loadSelectedAlbumWithVideoList(0)
                    } else {
                        showToast(getString(R.string.msg_there_are_no_videos))
                    }
                } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                    if (albumWithPhotoArrayList.isNotEmpty()) {
                        binding.ivSwitchMedia.setImageResource(R.drawable.ic_video_play)
                        selectedPhotoPathArrayList.clear()
                        selectedVideoPathArrayList.clear()
                        checkSelectedMediaManageVisibility()
                        loadSelectedAlbumWithPhotoList(0)
                    } else {
                        showToast(getString(R.string.msg_there_are_no_photos))
                    }
                }
            }.autoDispose()
            cvCamera.throttleClicks().subscribeAndObserveOnMainThread {
                checkPermissionGrantedForCamera(this@AddStoryActivity)
            }.autoDispose()
            rvPhotoList.layoutManager = GridLayoutManager(this@AddStoryActivity, FOUR, RecyclerView.VERTICAL, false)
        }
    }
    private fun heandelNextButton() {
        if (selectedMediaType == MEDIA_TYPE_IMAGE) {
            if (selectedPhotoPathArrayList.isNotEmpty()) {
                var intent = Intent()
                intent.putStringArrayListExtra("FILE_PATH", selectedPhotoPathArrayList)
                setResult(RESULT_OK, intent)
                finish()
            } else {
                finish()
            }
        } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
            if (selectedVideoPathArrayList.isNotEmpty()) {
                var intent = Intent()
                intent.putStringArrayListExtra("FILE_PATH", selectedVideoPathArrayList)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun checkPermissionGrantedForCamera(context: Context) {
        XXPermissions.with(
            context
        ).permission(Permission.CAMERA).permission(Permission.RECORD_AUDIO).request(object : OnPermissionCallback {

            override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                if (all) {
                    cameraClick()
                }
            }

            override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                super.onDenied(permissions, never)
                if (never) {
                    isCameraPermissionGrantedAfterDenied = true
                    showToast("Authorization is permanently denied, please manually grant permissions")
                    XXPermissions.startPermissionActivity(this@AddStoryActivity, permissions)
                } else {
                    showToast(getString(R.string.msg_permission_denied))
                }
            }
        })
    }

    private fun cameraClick() {
        val bsFragment = PostCameraBottomSheet()
        bsFragment.postCameraItemClicks.subscribeAndObserveOnMainThread {
            selectedMediaType = if (it) MEDIA_TYPE_IMAGE else MEDIA_TYPE_VIDEO
            bsFragment.dismissBottomSheet()
            startActivityForResultWithDefaultAnimation(
                PostCameraActivity.launchActivity(this, it),
                PostCameraActivity.RC_CAPTURE_PICTURE
            )
        }.autoDispose()
        bsFragment.show(supportFragmentManager, PostCameraBottomSheet::class.java.name)
    }

    private fun compressVideo() {
        val videoUris = listOf(Uri.fromFile(File(selectedVideoPathArrayList[0])))
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(selectedVideoPathArrayList[0])
        duration = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLong() ?: THOUSAND_DURATION
        if (duration / THOUSAND > ONE_HUNDRED_EIGHTY) {
            showToast("Video length is greater than 3 minutes")
        } else {
            showToast("Compressing video, Please wait...")
            lifecycleScope.launch {
                VideoCompressor.start(
                    context = applicationContext,
                    videoUris,
                    isStreamable = false,
                    sharedStorageConfiguration = SharedStorageConfiguration(
                        saveAt = SaveLocation.movies,
                        subFolderName = resources.getString(R.string.application_name)
                    ),
                    configureWith = Configuration(
                        quality = VideoQuality.HIGH,
                        videoNames = videoUris.map { uri -> uri.pathSegments.last() },
                        isMinBitrateCheckEnabled = false,
                    ),
                    listener = object : CompressionListener {
                        override fun onProgress(index: Int, percent: Float) {
                            return
                        }

                        override fun onStart(index: Int) {
                            return
                        }

                        override fun onSuccess(index: Int, size: Long, path: String?) {
                            if (isOnline(this@AddStoryActivity)) {
                                if (path.toString().isNotEmpty()) {
                                    cloudFlareConfig?.let {
                                        startActivity(
                                            MainHomeActivity.getIntentFromStoryUpload(
                                                this@AddStoryActivity,
                                                StoryUploadData(
                                                    cloudFlareConfig ?: CloudFlareConfig(),
                                                    path.toString(),
                                                    (duration / 1000).toInt().toString(),
                                                    linkAttachmentDetails = null,
                                                    null,
                                                    null,
                                                    null
                                                )
                                            )
                                        )
                                    } ?: mainHomeViewModel.getCloudFlareConfig(false)
                                }
                            }
                        }

                        override fun onFailure(index: Int, failureMessage: String) {
                            hideLoading()
                        }

                        override fun onCancelled(index: Int) {
                            hideLoading()
                        }
                    },
                )
            }
        }
    }

    fun addStory() {
        showLoading()
        if (selectedMediaType == MEDIA_TYPE_IMAGE) {
            if (isOnline(this@AddStoryActivity)) {
                cloudFlareConfig?.let {
                    mainHomeViewModel.uploadImageToCloudFlare(this, it, File(selectedPhotoPathArrayList.first()))
                } ?: mainHomeViewModel.getCloudFlareConfig(false)
            }
        } else {
            if (isOnline(this@AddStoryActivity)) {
                cloudFlareConfig?.let {
                    startActivity(
                        MainHomeActivity.getIntentFromStoryUpload(
                            this@AddStoryActivity,
                            StoryUploadData(
                                cloudFlareConfig ?: CloudFlareConfig(),
                                selectedVideoPathArrayList.first(),
                                (duration / THOUSAND).toInt().toString(),
                                linkAttachmentDetails = null,
                                null,
                                null,
                                null
                            )
                        )
                    )
                } ?: mainHomeViewModel.getCloudFlareConfig(false)
            }
        }
    }

    // --------------------------Photo--------------------------
    private fun loadAlbumWihPhotos(context: Context) {
        PhotoUtils.loadAlbumsWithPhotoList(context, onAlbumLoad = {
            albumWithPhotoArrayList = it
            if (albumWithPhotoArrayList.isNotEmpty()) {
                loadSelectedAlbumWithPhotoList(0)
            } else {
                showToast(getString(R.string.msg_there_are_no_photos))
            }
        })
    }

    private fun loadSelectedAlbumWithPhotoList(mPos: Int) {
        val albumModelKT = albumWithPhotoArrayList[mPos]
        binding.tvAlbumName.text = albumModelKT.albumName
        photoModelArrayList = albumModelKT.photoModelArrayList
        binding.rvPhotoList.adapter = PhotoListByAlbumAdapter(this, photoModelArrayList, this)
    }

    private fun showPhotoAlbumListDialog() {
        val bsFragment = PhotoAlbumListBSDialogFragment(albumWithPhotoArrayList)
        bsFragment.itemClick.subscribeAndObserveOnMainThread {
            bsFragment.dismissBottomSheet()
            loadSelectedAlbumWithPhotoList(it)
        }.autoDispose()
        bsFragment.show(supportFragmentManager, PhotoAlbumListBSDialogFragment::class.java.name)
    }

    override fun onPhotoItemClick(mPos: Int) {
        val aPhotoModel = photoModelArrayList[mPos]
        if (isSingleSelection) {
            photoModelArrayList.forEach {
                it.selected = false
            }
        }
        photoModelArrayList.get(mPos).selected = true
        binding.rvPhotoList.adapter?.notifyDataSetChanged()

        aPhotoModel.path?.let { photoPath ->
            if (isSingleSelection) {
                selectedPhotoPathArrayList.clear()
                arrayList.clear()
            }
            aPhotoModel.uri?.let { arrayList.add(it) }
            if (selectedPhotoPathArrayList.contains(photoPath)) {
                selectedPhotoPathArrayList.remove(photoPath)
                checkSelectedMediaManageVisibility()
            } else {
                if (selectedPhotoPathArrayList.size < selectLimit) {
                    selectedPhotoPathArrayList.add(photoPath)
                    checkSelectedMediaManageVisibility()
                }
            }
        }
    }

    // --------------------------Video--------------------------
    private fun loadAlbumWihVideos(context: Context) {
        VideoUtils.loadAlbumsWithVideoList(context, onAlbumLoad = {
            albumWithVideoArrayList = it
        })
    }

    private fun loadSelectedAlbumWithVideoList(mPos: Int) {
        val albumModelKT = albumWithVideoArrayList[mPos]
        binding.tvAlbumName.text = albumModelKT.albumName
        videoModelArrayList = albumModelKT.videoModelArrayList
        binding.rvPhotoList.adapter = VideoListByAlbumAdapter(this, videoModelArrayList, this)
    }

    private fun showVideoAlbumListDialog() {
        val bsFragment = VideoAlbumListBSDialogFragment(albumWithVideoArrayList)
        bsFragment.itemClick.subscribeAndObserveOnMainThread {
            bsFragment.dismissBottomSheet()
            loadSelectedAlbumWithVideoList(it)
        }.autoDispose()
        bsFragment.show(supportFragmentManager, VideoAlbumListBSDialogFragment::class.java.name)
    }

    override fun onVideoItemClick(mPos: Int) {
        val aVideoModel = videoModelArrayList[mPos]
        videoModelArrayList.get(mPos).isSelected = true
        binding.rvPhotoList.adapter?.notifyDataSetChanged()
        aVideoModel.filePath?.let { photoPath ->
            if (selectedVideoPathArrayList.contains(photoPath)) {
                selectedVideoPathArrayList.remove(photoPath)
                checkSelectedMediaManageVisibility()
            } else {
                selectedVideoPathArrayList.add(photoPath)
                checkSelectedMediaManageVisibility()
            }
        }
    }

    // --------------------------Common--------------------------
    private fun checkSelectedMediaManageVisibility() {
        val selectedMediaFilePathArrayList = ArrayList<String>()

        binding.videoView.visibility = View.GONE
        binding.ivSelectedPhoto.visibility = View.GONE

        if (selectedMediaType == MEDIA_TYPE_IMAGE) {
            if (selectedPhotoPathArrayList.isNotEmpty()) {
                selectedMediaFilePathArrayList.addAll(selectedPhotoPathArrayList)

                binding.ivSelectedPhoto.visibility = View.VISIBLE

                Glide.with(
                    this
                ).load(
                    selectedPhotoPathArrayList.last()
                ).centerCrop().transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivSelectedPhoto)
            }
        } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
            if (selectedVideoPathArrayList.isNotEmpty()) {
                selectedMediaFilePathArrayList.addAll(selectedVideoPathArrayList)

                binding.videoView.visibility = View.VISIBLE

                val uri: Uri = Uri.parse(selectedVideoPathArrayList.last())
                val mediaController = MediaController(this)
                mediaController.setAnchorView(binding.videoView)
                binding.videoView.setMediaController(mediaController)
                binding.videoView.setVideoURI(uri)
                binding.videoView.requestFocus()
                binding.videoView.start()
            }
        }

        if (selectedMediaFilePathArrayList.isNotEmpty()) {
            binding.ivNext.visibility = View.VISIBLE
            binding.tvSelectMediaHint.visibility = View.GONE
        } else {
            binding.ivNext.visibility = View.INVISIBLE
            binding.tvSelectMediaHint.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PostCameraActivity.RC_CAPTURE_PICTURE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                setSelectedMeadia(data)
            }
        }

        if (isPermissionGrantedAfterDenied) {
            loadAlbumWihPhotos(this@AddStoryActivity)
            loadAlbumWihVideos(this@AddStoryActivity)
        }
        if (isCameraPermissionGrantedAfterDenied) {
            val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            val audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

            if (camera == 0 && audio == 0) {
                cameraClick()
            }
        }
    }
    private fun setSelectedMeadia(data: Intent) {
        val filePath = data.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
        if (!filePath.isNullOrEmpty()) {
            if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                selectedPhotoPathArrayList.clear()
                selectedPhotoPathArrayList.add(filePath)
                addStory()
            } else {
                selectedVideoPathArrayList.clear()
                selectedVideoPathArrayList.add(filePath)
                compressVideo()
            }
        }
    }
}
