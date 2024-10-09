package com.meetfriend.app.ui.home.create

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.databinding.ActivityAddNewPostBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.home.create.croppostimages.CropPostImagesActivity
import com.meetfriend.app.ui.home.shorts.PostCameraActivity
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

class AddNewPostActivity :
    BasicActivity(),
    PhotoListByAlbumAdapterCallback,
    VideoListByAlbumAdapterCallback {

    companion object {
        const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"
        private const val MEDIA_TYPE_VIDEO = "POST_TYPE_VIDEO"
        private const val MEDIA_TYPE_POST_VIDEO = "POST_TYPE_POST_VIDEO"
        private const val INTENT_MEDIA_TYPE = "INTENT_MEDIA_TYPE"
        private const val INTENT_TAG_NAME = "INTENT_TAG_NAME"
        private const val INTENT_IS_FROM_DEEPAR = "INTENT_IS_FROM_DEEPAR"
        const val FOUR = 4
        const val FIVE = 5

        fun launchActivity(
            context: Context,
            mediaType: String? = null,
            tagName: String? = null,
            isFromDeepAR: Boolean? = false
        ): Intent {
            val intent = Intent(context, AddNewPostActivity::class.java)
            intent.putExtra(INTENT_MEDIA_TYPE, mediaType)
            intent.putExtra(INTENT_TAG_NAME, tagName)
            intent.putExtra(INTENT_IS_FROM_DEEPAR, isFromDeepAR)
            return intent
        }

        private var selectedPhotoPathArrayList = ArrayList<String>()

        fun isPhotoSelected(photoModel: PhotoModel): Int {
            for (i in 0 until selectedPhotoPathArrayList.size) {
                if (selectedPhotoPathArrayList[i] == photoModel.path) {
                    return i
                }
            }
            return -1
        }

        private var isPermissionGrantedAfterDenied: Boolean = false
        private var isCameraPermissionGrantedAfterDenied: Boolean = false

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

    lateinit var binding: ActivityAddNewPostBinding

    private var albumWithPhotoArrayList = ArrayList<AlbumPhotoModel>()
    private var photoModelArrayList = ArrayList<PhotoModel>()

    private var albumWithVideoArrayList = ArrayList<AlbumVideoModel>()
    private var videoModelArrayList = ArrayList<VideoModel>()

    private var selectedMediaType = ""
    private var isFromDeepAR = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddNewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.let {
            selectedMediaType = it.getStringExtra(INTENT_MEDIA_TYPE).toString()
            isFromDeepAR = it.getBooleanExtra(INTENT_IS_FROM_DEEPAR, false)

            if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                binding.titleAppCompatTextView.text =
                    resources.getString(R.string.label_create_shorts)
                binding.cvMultipleSelect.visibility = View.GONE
            } else {
                binding.titleAppCompatTextView.text =
                    resources.getString(R.string.label_create_post)
            }
        }
        checkPermissionGranted(this)

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()
    }

    private fun loadAlbumData() {
        if (selectedMediaType == MEDIA_TYPE_VIDEO) {
            loadAlbumWihVideos(this@AddNewPostActivity)
        } else {
            binding.ivNext.visibility = View.VISIBLE
            selectedMediaType = MEDIA_TYPE_IMAGE
            loadAlbumWihVideos(this@AddNewPostActivity)
            loadAlbumWihPhotos(this@AddNewPostActivity)
        }
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.READ_MEDIA_IMAGES)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        isPermissionGrantedAfterDenied = false
                        listenToViewEvents()
                        loadAlbumData()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    if (never) {
                        showToast("Authorization is permanently denied, please manually grant permissions")
                        isPermissionGrantedAfterDenied = true
                        XXPermissions.startPermissionActivity(this@AddNewPostActivity, permissions)
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
        binding

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.ivNext.throttleClicks().subscribeAndObserveOnMainThread {
            if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                if (selectedPhotoPathArrayList.isNotEmpty()) {
                    if (isFromDeepAR) {
                        val data = Intent()
                        data.putExtra(
                            AddNewPostInfoActivity.INTENT_EXTRA_POST_TYPE,
                            AddNewPostInfoActivity.POST_TYPE_IMAGE
                        )
                        data.putExtra(
                            AddNewPostInfoActivity.INTENT_EXTRA_IMAGE_PATH_LIST,
                            selectedPhotoPathArrayList
                        )
                        data.putExtra(
                            AddNewPostInfoActivity.INTENT_TAG_NAME,
                            intent?.getStringExtra(INTENT_TAG_NAME)
                        )
                        setResult(RESULT_OK, data)
                        finish()
                    } else {
                        startActivity(
                            AddNewPostInfoActivity.launchActivity(
                                LaunchActivityData(
                                    this,
                                    postType = AddNewPostInfoActivity.POST_TYPE_IMAGE,
                                    imagePathList = selectedPhotoPathArrayList,
                                    tagName = intent?.getStringExtra(INTENT_TAG_NAME)
                                )
                            )
                        )
                    }
                } else {
                    startActivity(
                        AddNewPostInfoActivity.launchActivity(
                            LaunchActivityData(
                                this,
                                AddNewPostInfoActivity.POST_TYPE_TEXT,
                                tagName = intent?.getStringExtra(INTENT_TAG_NAME)
                            )
                        )
                    )
                }
            } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                if (selectedVideoPathArrayList.isNotEmpty()) {
                    startActivity(
                        AddNewPostInfoActivity.launchActivity(
                            LaunchActivityData(
                                this,
                                postType = AddNewPostInfoActivity.POST_TYPE_VIDEO,
                                videoPath = selectedVideoPathArrayList.first(),
                                tagName = intent?.getStringExtra(INTENT_TAG_NAME)
                            )
                        )
                    )
                } else {
                    startActivity(
                        AddNewPostInfoActivity.launchActivity(
                            LaunchActivityData(
                                this,
                                AddNewPostInfoActivity.POST_TYPE_TEXT,
                                tagName = intent?.getStringExtra(INTENT_TAG_NAME)
                            )
                        )
                    )
                }
            } else if (selectedMediaType == MEDIA_TYPE_POST_VIDEO) {
                startActivity(
                    AddNewPostInfoActivity.launchActivity(
                        LaunchActivityData(
                            this,
                            postType = AddNewPostInfoActivity.POST_TYPE_POST_VIDEO,
                            videoPath = selectedVideoPathArrayList.first(),
                            tagName = intent?.getStringExtra(INTENT_TAG_NAME)
                        )
                    )
                )
            }
        }.autoDispose()

        binding.tvAlbumName.throttleClicks().subscribeAndObserveOnMainThread {
            if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                showPhotoAlbumListDialog()
            } else if (selectedMediaType == MEDIA_TYPE_VIDEO || selectedMediaType == MEDIA_TYPE_POST_VIDEO) {
                showVideoAlbumListDialog()
            }
        }.autoDispose()

        binding.cvMultipleSelect.throttleClicks().subscribeAndObserveOnMainThread {
            if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                if (albumWithVideoArrayList.isNotEmpty()) {
                    selectedMediaType = MEDIA_TYPE_POST_VIDEO
                    binding.ivSwitchMedia.setImageResource(R.drawable.ic_post_multiple_select)
                    selectedPhotoPathArrayList.clear()
                    selectedVideoPathArrayList.clear()
                    checkSelectedMediaManageVisibility()
                    loadSelectedAlbumWithVideoList(0)
                } else {
                    showToast(getString(R.string.msg_there_are_no_videos))
                }
            } else if (selectedMediaType == MEDIA_TYPE_POST_VIDEO) {
                if (albumWithPhotoArrayList.isNotEmpty()) {
                    selectedMediaType = MEDIA_TYPE_IMAGE
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

        binding.cvCamera.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermissionGrantedForCamera(this)
        }.autoDispose()

        binding.rvPhotoList.layoutManager = GridLayoutManager(this, FOUR, RecyclerView.VERTICAL, false)
    }

    private fun checkPermissionGrantedForCamera(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.CAMERA)
            .permission(Permission.RECORD_AUDIO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        cameraClick()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    if (never) {
                        showToast("Authorization is permanently denied, please manually grant permissions")
                        isCameraPermissionGrantedAfterDenied = true
                        XXPermissions.startPermissionActivity(this@AddNewPostActivity, permissions)
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }
            })
    }

    private fun cameraClick() {
        if (selectedMediaType == MEDIA_TYPE_VIDEO) {
            startActivityForResultWithDefaultAnimation(
                PostCameraActivity.launchActivity(this, false),
                PostCameraActivity.RC_CAPTURE_PICTURE
            )
        } else {
            val bsFragment = PostCameraBottomSheet()
            bsFragment.postCameraItemClicks.subscribeAndObserveOnMainThread {
                selectedMediaType = if (it) {
                    MEDIA_TYPE_IMAGE
                } else {
                    MEDIA_TYPE_POST_VIDEO
                }
                bsFragment.dismissBottomSheet()
                startActivityForResultWithDefaultAnimation(
                    PostCameraActivity.launchActivity(this, it),
                    PostCameraActivity.RC_CAPTURE_PICTURE
                )
            }.autoDispose()
            bsFragment.show(supportFragmentManager, PostCameraBottomSheet::class.java.name)
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
        aPhotoModel.path?.let { photoPath ->
            if (selectedPhotoPathArrayList.contains(photoPath)) {
                selectedPhotoPathArrayList.remove(photoPath)
                checkSelectedMediaManageVisibility()
            } else {
                if (selectedPhotoPathArrayList.size < FIVE) {
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
            if (albumWithVideoArrayList.isNotEmpty()) {
                loadSelectedAlbumWithVideoList(0)
            } else {
                showToast(getString(R.string.msg_there_are_no_videos))
            }
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
        videoModelArrayList.forEach {
            it.isSelected = false
        }
        videoModelArrayList.get(mPos).isSelected = true
        binding.rvPhotoList.adapter?.notifyDataSetChanged()
        selectedVideoPathArrayList.clear()

        aVideoModel.filePath?.let { photoPath ->
            if (selectedVideoPathArrayList.contains(photoPath)) {
                selectedVideoPathArrayList.remove(photoPath)
                checkSelectedMediaManageVisibility()
            } else {
                if (selectedVideoPathArrayList.size < 1) {
                    selectedVideoPathArrayList.add(photoPath)
                    checkSelectedMediaManageVisibility()
                }
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

                Glide.with(this)
                    .load(selectedPhotoPathArrayList.last())
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivSelectedPhoto)
            }
        } else if (selectedMediaType == MEDIA_TYPE_VIDEO || selectedMediaType == MEDIA_TYPE_POST_VIDEO) {
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
        if (selectedMediaType == MEDIA_TYPE_VIDEO) {
            if (selectedMediaFilePathArrayList.isNotEmpty()) {
                binding.ivNext.visibility = View.VISIBLE
                binding.tvSelectMediaHint.visibility = View.GONE
            } else {
                binding.ivNext.visibility = View.GONE
                binding.tvSelectMediaHint.visibility = View.VISIBLE
            }
        } else {
            if (selectedMediaFilePathArrayList.isNotEmpty()) {
                binding.ivNext.visibility = View.VISIBLE
                binding.tvSelectMediaHint.visibility = View.GONE
            } else {
                binding.ivNext.visibility = View.VISIBLE
                binding.tvSelectMediaHint.visibility = View.VISIBLE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val isCapturePhoto = data.getBooleanExtra(
                        PostCameraActivity.INTENT_EXTRA_IS_CAPTURE_PHOTO,
                        false
                    )
                    val filePath = data.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
                    if (!filePath.isNullOrEmpty()) {
                        if (isCapturePhoto) {
                            val filePathArrayList = ArrayList<String>()
                            filePathArrayList.add(filePath)
                            val intent = CropPostImagesActivity.getIntent(
                                this,
                                imagePathList = filePathArrayList
                            )
                            startActivity(intent)
                        } else {
                            val intent = AddNewPostInfoActivity.launchActivity(
                                LaunchActivityData(
                                    this,
                                    postType = selectedMediaType,
                                    videoPath = filePath,
                                    tagName = intent?.getStringExtra(INTENT_TAG_NAME)
                                )
                            )
                            startActivity(intent)
                        }
                    }
                }
            }
        }
        if (isPermissionGrantedAfterDenied) {
            listenToViewEvents()
            loadAlbumData()
        }

        if (isCameraPermissionGrantedAfterDenied) {
            val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            val audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

            if (camera == 0 && audio == 0) {
                cameraClick()
            }
        }
    }
}
