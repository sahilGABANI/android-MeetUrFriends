package com.meetfriend.app.ui.helpnsupport

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.profile.model.SelectedOption
import com.meetfriend.app.api.profile.model.SendHelpRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityHelpFormBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.profile.ImagePickerOptionBottomSheet
import com.meetfriend.app.ui.helpnsupport.viewmodel.HelpFormViewModel
import com.meetfriend.app.ui.helpnsupport.viewmodel.HelpFormViewState
import com.meetfriend.app.ui.home.shorts.PostCameraActivity
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FileUtils
import java.io.File
import javax.inject.Inject

class HelpFormActivity : BasicActivity() {

    private lateinit var binding: ActivityHelpFormBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HelpFormViewModel>
    lateinit var helpFormViewModel: HelpFormViewModel

    private lateinit var handlePathOz: HandlePathOz
    private var mediaType: String? = null
    private var selectedImagePath: String = ""
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var mediaUrl = ""

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, HelpFormActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        helpFormViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        initUI()
        helpFormViewModel.getCloudFlareConfig()
    }

    private fun initUI() {
        handlePathOz = HandlePathOz(this, listener)

        binding.ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        binding.uploadLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            openSelectionBottomSheet()
        }.autoDispose()

        binding.saveAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            validate()
            openInterstitialAds()
            Constant.CLICK_COUNT++
        }.autoDispose()
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                sendMedia(filePath)
            }
        }
    }

    private fun sendMedia(filePath: String) {
        if (mediaType == Constant.PHOTO) {
            if (filePath.isNotEmpty()) {
                selectedImagePath = filePath

                cloudFlareConfig?.let {
                    uploadImageToCloudFlare(it)
                } ?: helpFormViewModel.getCloudFlareConfig()
            }
        } else {
            if (filePath.isNotEmpty()) {
                selectedImagePath = filePath
                cloudFlareConfig?.let {
                    uploadVideoToCloudFlare(it)
                } ?: helpFormViewModel.getCloudFlareConfig()
            }
        }
    }

    private fun validate() {
        if (binding.edtName.text.isNullOrEmpty()) {
            showToast(resources.getString(R.string.label_enter_your_name))
            return
        } else if (binding.edtEmail.text.isNullOrEmpty()) {
            showToast(resources.getString(R.string.label_enter_your_emailid))
            return
        } else if (binding.phone.text.isNullOrEmpty()) {
            showToast(getString(R.string.label_enter_your_phoneno))
            return
        } else if (binding.query.text.isNullOrEmpty()) {
            showToast(getString(R.string.label_enter_your_query))
            return
        } else if (mediaUrl.isNullOrEmpty() || mediaUrl == "") {
            showToast(getString(R.string.label_validate_query_media_selection))
            return
        } else {
            helpFormViewModel.sendFeedback(
                SendHelpRequest(
                    binding.edtName.text.toString(),
                    binding.edtEmail.text.toString(),
                    binding.phone.text.toString(),
                    binding.query.text.toString(),
                    mediaUrl
                )
            )
        }
    }

    private fun listenToViewModel() {
        helpFormViewModel.helpFormState.subscribeAndObserveOnMainThread {
            when (it) {
                is HelpFormViewState.LoadingState -> {
                    if (it.isLoading) {
                        showLoading()
                    } else {
                        hideLoading()
                    }
                }
                is HelpFormViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                    finish()
                }
                is HelpFormViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                    hideLoading()
                    binding.saveAppCompatTextView.isVisible = true
                }

                is HelpFormViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is HelpFormViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressedDispatcher.onBackPressed()
                }
                is HelpFormViewState.UploadImageCloudFlareSuccess -> {
                    mediaUrl = it.imageUrl
                    binding.photoAppCompatImageView.isVisible = true
                    binding.addImageConatainer.isVisible = false
                    Glide.with(this)
                        .load(it.imageUrl)
                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                        .into(binding.photoAppCompatImageView)
                }
                is HelpFormViewState.CloudFlareLoadingState -> {
                    showLoading(it.isLoading)
                }
                is HelpFormViewState.UploadVideoCloudFlareSuccess -> {
                    mediaUrl = it.videoId
                    binding.photoAppCompatImageView.isVisible = true
                    binding.addImageConatainer.isVisible = false
                    Glide.with(this)
                        .load(it.thumbnail)
                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                        .into(binding.photoAppCompatImageView)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun openSelectionBottomSheet() {
        val imageSelectionBottomSheet = ImagePickerOptionBottomSheet()
        imageSelectionBottomSheet.show(
            supportFragmentManager,
            ImagePickerOptionBottomSheet::class.java.name
        )

        imageSelectionBottomSheet.imageSelectionOptionClicks.subscribeAndObserveOnMainThread {
            when (it) {
                SelectedOption.Gallery.name -> {
                    selectMediaDialog(it)
                }

                SelectedOption.Camera.name -> {
                    selectMediaDialog(it)
                }
            }
        }.autoDispose()
    }

    private fun checkPermissionGrantedForSelectMedia(
        context: Context,
        mediaType: String,
        permission: String
    ) {
        XXPermissions.with(context)
            .permission(permission)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        if (mediaType == Constant.PHOTO) {
                            FileUtils.openImagePicker(this@HelpFormActivity)
                        } else if (mediaType == Constant.VIDEO) {
                            FileUtils.openVideoPicker(this@HelpFormActivity)
                        }
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }

    private fun checkPermissionGrantedForCamera(context: Context, isPhoto: Boolean) {
        XXPermissions.with(context)
            .permission(Permission.CAMERA)
            .permission(Permission.RECORD_AUDIO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startActivityForResultWithDefaultAnimation(
                            PostCameraActivity.launchActivity(
                                this@HelpFormActivity,
                                isPhoto
                            ),
                            PostCameraActivity.RC_CAPTURE_PICTURE
                        )
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    if (never) {
                        showToast("Authorization is permanently denied, please manually grant permissions")
                        XXPermissions.startPermissionActivity(
                            this@HelpFormActivity,
                            permissions
                        )
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }
            })
    }

    private fun selectMediaDialog(selectionOption: String) {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
        if (selectionOption == SelectedOption.Gallery.name) {
            alertDialog.setTitle("Select")
        } else {
            alertDialog.setTitle("Capture")
        }

        alertDialog.setPositiveButton("photo") { dialogInterface: DialogInterface, i: Int ->
            if (selectionOption == SelectedOption.Gallery.name) {
                val permission = Permission.READ_MEDIA_IMAGES
                checkPermissionGrantedForSelectMedia(this, Constant.PHOTO, permission)
            } else {
                mediaType = Constant.PHOTO
                checkPermissionGrantedForCamera(this, true)
            }
        }

        alertDialog.setNegativeButton("video") { dialog: DialogInterface?, which: Int ->
            if (selectionOption == SelectedOption.Gallery.name) {
                val permission = Permission.READ_MEDIA_IMAGES
                checkPermissionGrantedForSelectMedia(this, Constant.VIDEO, permission)
            } else {
                mediaType = Constant.VIDEO
                checkPermissionGrantedForCamera(this, false)
            }
        }
        val alert: AlertDialog = alertDialog.create()
        alert.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
                mediaType = Constant.PHOTO
            }
        } else if ((requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) and (resultCode == RESULT_OK)) {
            if (data != null) {
                val isCapturePhoto = data.getBooleanExtra(
                    PostCameraActivity.INTENT_EXTRA_IS_CAPTURE_PHOTO,
                    false
                )
                val filePath = data.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
                if (!filePath.isNullOrEmpty()) {
                    if (isCapturePhoto) {
                        sendMedia(filePath)
                    } else {
                        mediaType = Constant.VIDEO
                        sendMedia(filePath)
                    }
                }
            }
        } else if ((requestCode == FileUtils.PICK_Video) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
                mediaType = Constant.VIDEO
            }
        }
    }

    private fun uploadImageToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            helpFormViewModel.uploadImageToCloudFlare(
                this,
                cloudFlareConfig,
                File(selectedImagePath)
            )
        }
    }

    private fun uploadVideoToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            helpFormViewModel.uploadVideoToCloudFlare(
                this,
                cloudFlareConfig,
                File(selectedImagePath)
            )
        }
    }
}
