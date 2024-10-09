package com.meetfriend.app.ui.chat.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.CreateOneToOneChatRequest
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.profile.model.ChangeProfileRequest
import com.meetfriend.app.api.profile.model.ChatRoomUserProfileInfo
import com.meetfriend.app.api.profile.model.CreateChatRoomUserRequest
import com.meetfriend.app.api.profile.model.ProfileItemInfo
import com.meetfriend.app.api.profile.model.SelectedOption
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentChatRoomProfileBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.hideKeyboard
import com.meetfriend.app.newbase.extension.openKeyboard
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForGetProfile
import com.meetfriend.app.ui.chatRoom.profile.ConfirmDeleteBottomSheet
import com.meetfriend.app.ui.chatRoom.profile.ImagePickerOptionBottomSheet
import com.meetfriend.app.ui.chatRoom.profile.ViewUserProfileActivity
import com.meetfriend.app.ui.chatRoom.profile.view.PreviousProfileImageAdapter
import com.meetfriend.app.ui.chatRoom.profile.viewmodel.ProfileViewModel
import com.meetfriend.app.ui.chatRoom.profile.viewmodel.ProfileViewState
import com.meetfriend.app.ui.home.shorts.PostCameraActivity
import com.meetfriend.app.utils.FileUtils
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject

class ChatRoomProfileFragment : BasicFragment() {

    companion object {

        private const val INTENT_OTHER_USER_ID = "otherUserId"

        @JvmStatic
        fun newInstance(otherUserId: Int): ChatRoomProfileFragment {
            val args = Bundle()
            otherUserId.let { args.putInt(INTENT_OTHER_USER_ID, it) }
            val fragment = ChatRoomProfileFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentChatRoomProfileBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ProfileViewModel>
    lateinit var profileViewModel: ProfileViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId: Int = 0

    private lateinit var previousProfileImageAdapter: PreviousProfileImageAdapter
    private var otherUserId: Int? = null

    private lateinit var deletedItem: ProfileItemInfo

    private lateinit var handlePathOz: HandlePathOz
    private var selectedImagePath: String = ""
    private var listOfProfileImages: ArrayList<ProfileItemInfo> = arrayListOf()
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var currentProfileImage: String? = ""
    private var lastAddedImage: String? = ""
    private var profileData: MeetFriendResponseForGetProfile<ChatRoomUserProfileInfo>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatRoomProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        profileViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        otherUserId = arguments?.getInt(INTENT_OTHER_USER_ID)
            ?: throw IllegalStateException("No args provided")

        manageViewForOtherUser()
        listenToViewModel()
        listenToViewEvent()

        profileViewModel.getCloudFlareConfig()
        if (otherUserId != 0) {
            otherUserId?.let {
                profileViewModel.getOtherUserProfile(it)
            }
        } else {
            profileViewModel.getChatRoomUser()
        }
    }

    private fun listenToViewEvent() {
        handlePathOz = HandlePathOz(requireContext(), listener)

        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        previousProfileImageAdapter = PreviousProfileImageAdapter(requireContext()).apply {
            previousProfileImageViewClicks.subscribeAndObserveOnMainThread { item ->

                startActivity(
                    ViewUserProfileActivity.getIntent(
                        requireContext(),
                        listOfProfileImages,
                        item,
                        profileData?.data
                    )
                )
            }

            deleteProfileImageViewClicks.subscribeAndObserveOnMainThread { profileItemInfo ->
                val deleteOptionBottomSheet = ConfirmDeleteBottomSheet()
                deleteOptionBottomSheet.deleteClicks.subscribeAndObserveOnMainThread {
                    profileItemInfo.id.let { item ->
                        deletedItem = profileItemInfo
                        profileViewModel.deleteProfileImage(item)
                    }
                }.autoDispose()
                deleteOptionBottomSheet.show(
                    childFragmentManager,
                    ConfirmDeleteBottomSheet::class.java.name
                )
            }
        }
        binding.rvPreviousProfileImages.adapter = previousProfileImageAdapter

        binding.ivEdit.throttleClicks().subscribeAndObserveOnMainThread {
            binding.tvUserName.visibility = View.GONE
            binding.etUserName.setText(profileData?.data?.chatUserName)
            binding.etUserName.visibility = View.VISIBLE
            binding.ivEdit.visibility = View.GONE
            binding.ivCheck.visibility = View.VISIBLE
            binding.etUserName.requestFocus()
            binding.etUserName.openKeyboard(requireContext())
        }

        binding.ivCheck.throttleClicks().subscribeAndObserveOnMainThread {
            editUserName()
        }.autoDispose()

        binding.ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            if (otherUserId == 0) {
                openSelectionBottomSheet()
            }
        }

        binding.ivEditProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            openSelectionBottomSheet()
        }

        binding.llMessageContainer.throttleClicks().subscribeAndObserveOnMainThread {
            otherUserId?.let { receiverId ->
                profileViewModel.createOneToOneChat(
                    CreateOneToOneChatRequest(
                        receiverId = receiverId
                    )
                )
            }
        }.autoDispose()
        RxBus.listen(RxEvent.DeleteProfileImage::class.java).subscribeAndObserveOnMainThread {
            if (otherUserId != 0) {
                otherUserId?.let {
                    profileViewModel.getOtherUserProfile(it)
                }
            } else {
                profileViewModel.getChatRoomUser()
            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        profileViewModel.profileState.subscribeAndObserveOnMainThread {
            when (it) {
                is ProfileViewState.LoadingState -> {
                    if (it.isLoading) showLoading() else hideLoading()
                }
                is ProfileViewState.SuccessMessage -> {
                }
                is ProfileViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ProfileViewState.GetProfileData -> {
                    hideLoading()
                    loadData(it.profileData)
                }

                is ProfileViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is ProfileViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
                is ProfileViewState.UploadImageCloudFlareSuccess -> {
                    val request = ChangeProfileRequest(
                        profilePic = it.imageUrl,
                        userId = loggedInUserCache.getLoggedInUserId()
                    )
                    lastAddedImage = it.imageUrl
                    profileViewModel.editProfile(request)
                }

                is ProfileViewState.DeleteProfileSuccessMessage -> {
                    deleteProfileImage()
                }

                is ProfileViewState.EditProfileSuccessMessage -> {
                    profileViewModel.getChatRoomUser()
                }
                is ProfileViewState.ChatRoomSuccessMessage -> {
                    manageEditUserNameView()
                    profileViewModel.getChatRoomUser()
                }
                is ProfileViewState.GetChatRoomUserLoadingState -> {
                    manageViewVisibility(it.isLoading)
                }

                is ProfileViewState.EditUserNameErrorMessage -> {
                    manageEditUserNameView()
                }
                is ProfileViewState.CloudFlareLoadingState -> {
                    if (it.isLoading) showLoading() else hideLoading()
                }
                is ProfileViewState.OneToOneChatData -> {
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun manageViewForOtherUser() {
        if (otherUserId == 0) {
            binding.flEditUserNameIcon.visibility = View.VISIBLE
            binding.ivEditProfileImage.visibility = View.VISIBLE
            binding.llMessageOrCallContainer.visibility = View.GONE
        } else {
            if (otherUserId != loggedInUserCache.getLoggedInUserId()) {
                binding.flEditUserNameIcon.visibility = View.GONE
                binding.ivEditProfileImage.visibility = View.GONE
                binding.llMessageOrCallContainer.visibility = View.VISIBLE
            } else {
                binding.flEditUserNameIcon.visibility = View.VISIBLE
                binding.ivEditProfileImage.visibility = View.VISIBLE
                binding.llMessageOrCallContainer.visibility = View.GONE
            }
        }
    }

    private fun uploadImageToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            profileViewModel.uploadImageToCloudFlare(
                requireContext(),
                cloudFlareConfig,
                File(selectedImagePath)
            )
        }
    }

    private fun loadData(profileData: MeetFriendResponseForGetProfile<ChatRoomUserProfileInfo>) {
        this.profileData = profileData
        listOfProfileImages = profileData.result?.data as ArrayList<ProfileItemInfo>

        if (listOfProfileImages.isEmpty()) {
            binding.rvPreviousProfileImages.visibility = View.GONE
            binding.llEmptyState.visibility = View.VISIBLE
            Glide.with(this)
                .load(R.drawable.ic_empty_profile_placeholder)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(binding.ivUserProfileImage)
        } else {
            currentProfileImage = listOfProfileImages.first().filePath

            binding.llEmptyState.visibility = View.GONE

            Glide.with(this)
                .load(currentProfileImage)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(binding.ivUserProfileImage)

            previousProfileImageAdapter.listOfDataItems = listOfProfileImages
        }

        profileData.data?.let {
            binding.tvUserName.text = it.chatUserName
            binding.ivOnline.visibility = if (it.isOnline == 1) View.VISIBLE else View.GONE
        }
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
        } else if ((requestCode == PostCameraActivity.RC_CAPTURE_PICTURE)
            && (resultCode == AppCompatActivity.RESULT_OK)
        ) {
            val filePath = data?.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
            filePath?.let { updateImage(it) }
        }
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                updateImage(filePath)
            }
        }
    }

    private fun updateImage(filePath: String) {
        if (filePath.isNotEmpty()) {
            selectedImagePath = filePath
            Glide.with(requireContext())
                .load(filePath)
                .error(R.drawable.ic_empty_profile_placeholder)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.ivUserProfileImage)

            cloudFlareConfig?.let {
                uploadImageToCloudFlare(it)
            } ?: profileViewModel.getCloudFlareConfig()
        }
    }

    private fun manageViewVisibility(loading: Boolean) {
        if (loading) {
            binding.rvPreviousProfileImages.visibility = View.GONE
            showLoading()
        } else {
            binding.rvPreviousProfileImages.visibility = View.VISIBLE
            hideLoading()
        }
    }

    private fun editUserName() {
        hideKeyboard()
        if (isValidate()) {
            // hideKeyboard()
            if (binding.etUserName.text.toString() != profileData?.data?.chatUserName) {
                profileViewModel.createChatRoomUser(
                    CreateChatRoomUserRequest(
                        userId = loggedInUserId,
                        chatUserName = binding.etUserName.text.toString()
                    )
                )
            } else {
                binding.ivCheck.isVisible = false
                binding.ivEdit.isVisible = true
                binding.etUserName.isVisible = false
                binding.tvUserName.isVisible = true
            }
        } else {
            showToast(getString(R.string.error_please_enter_user_name))
        }
    }

    private fun manageEditUserNameView() {
        binding.ivEdit.visibility = View.VISIBLE
        binding.ivCheck.visibility = View.GONE
        binding.tvUserName.visibility = View.VISIBLE
        binding.etUserName.visibility = View.GONE
    }

    private fun isValidate(): Boolean {
        var isValidate = false
        val expression = "[0-9a-zA-Z._]*"
        val inputStr: CharSequence = binding.etUserName.text.toString()
        val pattern: Pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)

        if (binding.etUserName.text.toString().isNullOrEmpty()) {
            showToast(resources.getString(R.string.error_please_enter_user_name))
            isValidate = false
        } else if (!pattern.matcher(binding.etUserName.text.toString()).matches()) {
            isValidate = false
            showToast(resources.getString(R.string.error_special_characters_are_not_allowed))
        } else {
            isValidate = true
        }
        return isValidate
    }

    private fun deleteProfileImage() {
        listOfProfileImages.remove(deletedItem)
        if (listOfProfileImages.isEmpty()) {
            binding.rvPreviousProfileImages.visibility = View.GONE
            binding.llEmptyState.visibility = View.VISIBLE

            Glide.with(this)
                .load(R.drawable.ic_empty_profile_placeholder)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(binding.ivUserProfileImage)
        } else {
            previousProfileImageAdapter.listOfDataItems = listOfProfileImages
            Glide.with(this)
                .load(listOfProfileImages.first().filePath)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(binding.ivUserProfileImage)
        }
    }
}
