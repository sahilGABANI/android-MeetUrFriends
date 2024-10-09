package com.meetfriend.app.ui.challenge

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
import com.meetfriend.app.databinding.ActivityAddNewChallengeBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.create.PostCameraBottomSheet
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
import java.io.File

class AddNewChallengeActivity : BasicActivity(), PhotoListByAlbumAdapterCallback,
    VideoListByAlbumAdapterCallback {

    companion object {
        const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"
        private const val MEDIA_TYPE_VIDEO = "POST_TYPE_VIDEO"
        private const val MEDIA_TYPE_POST_VIDEO = "POST_TYPE_POST_VIDEO"
        private const val INTENT_MEDIA_TYPE = "INTENT_MEDIA_TYPE"
        private const val INTENT_IS_CHALLENGE = "INTENT_IS_CHALLENGE"

        fun launchActivity(
            context: Context,
            mediaType: String? = null,
            isChallenge: Boolean = false
        ): Intent {
            val intent = Intent(context, AddNewChallengeActivity::class.java)
            intent.putExtra(INTENT_MEDIA_TYPE, mediaType)
            intent.putExtra(INTENT_IS_CHALLENGE, isChallenge)
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

    lateinit var binding: ActivityAddNewChallengeBinding

    private var albumWithPhotoArrayList = ArrayList<AlbumPhotoModel>()
    private var photoModelArrayList = ArrayList<PhotoModel>()

    private var albumWithVideoArrayList = ArrayList<AlbumVideoModel>()
    private var videoModelArrayList = ArrayList<VideoModel>()

    private var selectedMediaType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddNewChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissionGranted(this)

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

    }

    private fun loadAlbumData() {
        if (selectedMediaType == MEDIA_TYPE_VIDEO) {
            loadAlbumWihVideos(this@AddNewChallengeActivity)
        } else {
            binding.ivNext.visibility = View.VISIBLE
            selectedMediaType = MEDIA_TYPE_IMAGE
            loadAlbumWihVideos(this@AddNewChallengeActivity)
            loadAlbumWihPhotos(this@AddNewChallengeActivity)
        }
    }

    private fun checkPermissionGranted(context: Context) {


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S_V2) {
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
                            XXPermissions.startPermissionActivity(
                                this@AddNewChallengeActivity,
                                permissions
                            )
                        } else {
                            showToast(getString(R.string.msg_permission_denied))
                        }
                    }

                })
        } else {
            XXPermissions.with(context)
                .permission(Permission.READ_MEDIA_IMAGES)
                .permission(Permission.READ_MEDIA_VIDEO)
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
                            XXPermissions.startPermissionActivity(
                                this@AddNewChallengeActivity,
                                permissions
                            )
                        } else {
                            showToast(getString(R.string.msg_permission_denied))
                        }
                    }
                })
        }
    }

    private fun listenToViewEvents() {

        selectedPhotoPathArrayList = ArrayList()
        selectedVideoPathArrayList = ArrayList()

        albumWithPhotoArrayList = ArrayList()
        photoModelArrayList = ArrayList()

        albumWithVideoArrayList = ArrayList()
        videoModelArrayList = ArrayList()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.ivNext.throttleClicks().subscribeAndObserveOnMainThread {
            if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                if (selectedPhotoPathArrayList.isNotEmpty()) {
                    var intent = Intent()
                    intent.putExtra("FILE_PATH", selectedPhotoPathArrayList.get(0))
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    startActivity(
                        AddNewPostInfoActivity.launchActivity(
                            LaunchActivityData(this,
                            AddNewPostInfoActivity.POST_TYPE_TEXT
                            )
                        )
                    )
                }
            } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                if (selectedVideoPathArrayList.isNotEmpty()) {
                    var intent = Intent()
                    intent.putExtra("FILE_PATH", selectedVideoPathArrayList.get(0))
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    startActivity(
                        AddNewPostInfoActivity.launchActivity(
                            LaunchActivityData(this,
                            AddNewPostInfoActivity.POST_TYPE_TEXT)
                        )
                    )
                }
            } else if (selectedMediaType == MEDIA_TYPE_POST_VIDEO) {

                var intent = Intent()
                intent.putExtra("FILE_PATH", selectedVideoPathArrayList.get(0))
                setResult(RESULT_OK, intent)
                finish()
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

        binding.rvPhotoList.layoutManager = GridLayoutManager(this, 4, RecyclerView.VERTICAL, false)
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
                        XXPermissions.startPermissionActivity(
                            this@AddNewChallengeActivity,
                            permissions
                        )
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

    //--------------------------Photo--------------------------
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
        photoModelArrayList.forEach {
            it.selected = false
        }
        photoModelArrayList.get(mPos).selected = true
        binding.rvPhotoList.adapter?.notifyDataSetChanged()
        aPhotoModel.path?.let { photoPath ->
            selectedPhotoPathArrayList.clear()
            if (selectedPhotoPathArrayList.contains(photoPath)) {
                selectedPhotoPathArrayList.remove(photoPath)
                checkSelectedMediaManageVisibility()
            } else {
                if (selectedPhotoPathArrayList.size < 1) {
                    selectedPhotoPathArrayList.add(photoPath)
                    checkSelectedMediaManageVisibility()
                }
            }
        }
    }

    //--------------------------Video--------------------------
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

    //--------------------------Common--------------------------
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
                            var intent = Intent()
                            intent.putExtra("FILE_PATH", filePath)
                            setResult(RESULT_OK, intent)
                            finish()
                        } else {
                            var intent = Intent()
                            intent.putExtra("FILE_PATH", filePath)
                            setResult(RESULT_OK, intent)
                            finish()
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