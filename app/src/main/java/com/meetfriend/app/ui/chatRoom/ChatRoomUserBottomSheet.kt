package com.meetfriend.app.ui.chatRoom


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.profile.model.ChangeProfileRequest
import com.meetfriend.app.api.profile.model.CreateChatRoomUserRequest
import com.meetfriend.app.api.profile.model.SelectedOption
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ChatRoomUserBottomsheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.chatRoom.profile.ImagePickerOptionBottomSheet
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomCreateUserViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomCreateUserViewState
import com.meetfriend.app.ui.home.shorts.PostCameraActivity
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utils.FileUtils
import contractorssmart.app.utilsclasses.PreferenceHandler
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject


class ChatRoomUserBottomSheet : BaseBottomSheetDialogFragment() {

    private val userCreatedSubject: PublishSubject<Boolean> = PublishSubject.create()
    val userCreated: Observable<Boolean> = userCreatedSubject.hide()

    private var _binding: ChatRoomUserBottomsheetBinding? = null
    private val binding get() = _binding!!


    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatRoomCreateUserViewModel>
    lateinit var chatRoomCreateUserViewModel: ChatRoomCreateUserViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var handlePathOz: HandlePathOz
    private var loggedInUserId: Int = 0
    private var selectedImagePath: String = ""

    private var cloudFlareConfig: CloudFlareConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        MeetFriendApplication.component.inject(this)
        chatRoomCreateUserViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChatRoomUserBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvents()
        listenToViewModel()
        chatRoomCreateUserViewModel.getCloudFlareConfig()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private fun listenToViewEvents() {

        handlePathOz = HandlePathOz(requireContext(), listener)

        binding.tvSubmit.setOnClickListener {
            if (isValidate()) {
                if (cloudFlareConfig != null)
                    chatRoomCreateUserViewModel.uploadImageToCloudFlare(
                        requireContext(),
                        cloudFlareConfig!!,
                        File(selectedImagePath)
                    )
            }
        }

        binding.profileRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            openSelectionBottomSheet()
        }.autoDispose()
    }

    private fun isValidate(): Boolean {
        val isValidate: Boolean
        val expression = "[0-9a-zA-Z._]*"
        val pattern: Pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)

        when {
            binding.etUsername.text.isNullOrEmpty() -> {
                showToast(getString(R.string.error_please_enter_user_name))
                isValidate = false
            }
            !pattern.matcher(binding.etUsername.text.toString()).matches() -> {
                isValidate = false
                showToast(resources.getString(R.string.error_special_characters_are_not_allowed))
            }

            (selectedImagePath.isNullOrEmpty()) -> {
                showToast(getString(R.string.error_please_select_user_profile))
                isValidate = false
            }

            else -> isValidate = true
        }
        return isValidate
    }

    private fun listenToViewModel() {
        chatRoomCreateUserViewModel.userCreateState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatRoomCreateUserViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is ChatRoomCreateUserViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    dismissBottomSheet()
                }
                is ChatRoomCreateUserViewState.UploadImageCloudFlareSuccess -> {
                    val request = ChangeProfileRequest(
                        profilePic = it.imageUrl,
                        userId = loggedInUserCache.getLoggedInUserId()
                    )
                    chatRoomCreateUserViewModel.editProfile(request)
                    loggedInUserCache.setChatUserProfileImage(it.imageUrl)
                }
                is ChatRoomCreateUserViewState.LoadingState -> {
                    manageLoadingState(it.isLoading)
                }
                is ChatRoomCreateUserViewState.ChatRoomSuccessMessage -> {
                    showLongToast(it.successMessage)
                    PreferenceHandler.writeBoolean(
                        requireActivity(),
                        PreferenceHandler.IS_CHAT_USER_CREATED,
                        true
                    )

                    userCreatedSubject.onNext(true)
                    dismissBottomSheet()

                }
                is ChatRoomCreateUserViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is ChatRoomCreateUserViewState.CreateChatUser -> {

                }
                is ChatRoomCreateUserViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ChatRoomCreateUserViewState.EditProfileSuccessMessage -> {
                    showLongToast(it.successMessage)

                    prepareCreateChatUserData()
                }
                is ChatRoomCreateUserViewState.CloudFlareLoadingState -> {
                    manageLoadingState(it.isLoading)
                }
                else -> {}
            }
        }.autoDispose()
    }


    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.READ_MEDIA_IMAGES)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        FileUtils.openImagePicker(requireActivity())
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)

            }
        } else if ((requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) and (resultCode == AppCompatActivity.RESULT_OK)) {
            val filePath = data?.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
            filePath?.let { addProfileImage(it) }
        } else {

        }
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {

                val filePath = pathOz.path
                addProfileImage(filePath)
            }
        }
    }

    private fun manageLoadingState(isLoading: Boolean) {
        if (isLoading) {
           CallProgressWheel.showLoadingDialog(requireContext())
        } else {
            CallProgressWheel.dismissLoadingDialog()
        }

    }

    private fun addProfileImage(filePath: String) {
        if (filePath.isNotEmpty()) {
            selectedImagePath = filePath
            Glide.with(requireContext())
                .load(filePath)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.ivUserProfileImage)

            if (cloudFlareConfig == null)
                chatRoomCreateUserViewModel.getCloudFlareConfig()
        }
    }

    private fun prepareCreateChatUserData() {
        val userName = binding.etUsername.text.toString()
        val request = CreateChatRoomUserRequest(
            userId = loggedInUserId,
            chatUserName = userName
        )

        chatRoomCreateUserViewModel.createChatRoomUser(request)
    }

    private fun openSelectionBottomSheet() {
        val imageSelectionBottomSheet = ImagePickerOptionBottomSheet()
        imageSelectionBottomSheet.show(
            childFragmentManager,
            ImagePickerOptionBottomSheet::class.java.name
        )

        imageSelectionBottomSheet.imageSelectionOptionClicks.subscribeAndObserveOnMainThread {

            when (it) {
                SelectedOption.Gallery.name -> {
                    checkPermissionGranted(requireContext())
                }

                SelectedOption.Camera.name -> {
                    checkPermissionGrantedForCamera(requireContext())
                }
            }

        }.autoDispose()
    }

    private fun checkPermissionGrantedForCamera(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.CAMERA)
            .permission(Permission.RECORD_AUDIO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startActivityForResultWithDefaultAnimation(
                            PostCameraActivity.launchActivity(
                                requireContext(),
                                true
                            ),
                            PostCameraActivity.RC_CAPTURE_PICTURE
                        )
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    if (never) {
                        showToast("Authorization is permanently denied, please manually grant permissions")
                        XXPermissions.startPermissionActivity(requireContext(), permissions)
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }
            })
    }


    private fun dismissBottomSheet() {
        dismiss()
    }
}