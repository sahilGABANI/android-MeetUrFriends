package com.meetfriend.app.ui.chatRoom.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
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
import com.meetfriend.app.api.profile.model.*
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityChatRoomProfileBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForGetProfile
import com.meetfriend.app.ui.chat.onetoonechatroom.ViewOneToOneChatRoomActivity
import com.meetfriend.app.ui.chatRoom.profile.view.PreviousProfileImageAdapter
import com.meetfriend.app.ui.chatRoom.profile.viewmodel.ProfileViewModel
import com.meetfriend.app.ui.chatRoom.profile.viewmodel.ProfileViewState
import com.meetfriend.app.utils.FileUtils
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject


class ChatRoomProfileActivity : BasicActivity() {

    companion object {
        private const val INTENT_OTHER_USER_ID = "INTENT_OTHER_USER_ID"
        fun getIntent(context: Context, otherUserId: Int?): Intent {
            val intent = Intent(context, ChatRoomProfileActivity::class.java)
            intent.putExtra(INTENT_OTHER_USER_ID, otherUserId)
            return intent
        }
    }

    private lateinit var binding: ActivityChatRoomProfileBinding

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
    private val MY_CAMERA_PERMISSION_CODE = 100
    private val CAMERA_REQUEST = 1888
    private var videoCallClick: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatRoomProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        profileViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        intent?.let {
            otherUserId = it.getIntExtra(INTENT_OTHER_USER_ID, 0)
        }

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

        handlePathOz = HandlePathOz(this, listener)

        binding.ivBack.setOnClickListener {
          onBackPressedDispatcher.onBackPressed()
        }

        previousProfileImageAdapter = PreviousProfileImageAdapter(this).apply {
            previousProfileImageViewClicks.subscribeAndObserveOnMainThread { item ->

                startActivity(
                    ViewUserProfileActivity.getIntent(
                        this@ChatRoomProfileActivity,
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
                    supportFragmentManager,
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
            showKeyboard()
        }

        binding.ivCheck.throttleClicks().subscribeAndObserveOnMainThread {
            editUserName()
        }.autoDispose()

        binding.ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            if (otherUserId == 0)
                openSelectionBottomSheet()

        }

        binding.ivEditProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            openSelectionBottomSheet()
        }

        binding.llMessageContainer.throttleClicks().subscribeAndObserveOnMainThread {
            videoCallClick = false

            otherUserId?.let { receiverId ->
                profileViewModel.createOneToOneChat(
                    CreateOneToOneChatRequest(
                        receiverId = receiverId
                    )
                )
            }
        }.autoDispose()

        binding.llVideoCallContainer.throttleClicks().subscribeAndObserveOnMainThread {
            checkVideoCallPermissionGranted(this)
            videoCallClick = true
        }.autoDispose()

    }

    private fun checkVideoCallPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.RECORD_AUDIO)
            .permission(Permission.CAMERA)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        otherUserId?.let { receiverId ->
                            profileViewModel.createOneToOneChat(
                                CreateOneToOneChatRequest(
                                    receiverId = receiverId
                                )
                            )
                        }
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast("Authorization is permanently denied, please manually grant permissions")
                    XXPermissions.startPermissionActivity(this@ChatRoomProfileActivity, permissions)
                }
            })
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
                    loadData(it.profileData)
                }

                is ProfileViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is ProfileViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                  onBackPressedDispatcher.onBackPressed()
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
//                    listOfProfileImages.add(0, ProfileItemInfo(lastAddedImage, id = 0))
//                    previousProfileImageAdapter.listOfDataItems = listOfProfileImages
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
                    finish()
                    startActivity(
                        ViewOneToOneChatRoomActivity.getIntent(
                            this,
                            it.chatRoomInfo,
                            videoCallClick
                        )
                    )
                    RxBus.publish(
                        RxEvent.UpdateChatRoom(
                            it.chatRoomInfo,
                            profileData?.data?.chatUserName.toString(),
                            videoCallClick,
                            true
                        )
                    )
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
                this,
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
            supportFragmentManager,
            ImagePickerOptionBottomSheet::class.java.name
        )

        imageSelectionBottomSheet.imageSelectionOptionClicks.subscribeAndObserveOnMainThread {

            when (it) {
                SelectedOption.Gallery.name -> {
                    checkPermissionGranted(this)
                }

                SelectedOption.Camera.name -> {
                    checkPermissionGrantedForCamera()
                }
            }

        }.autoDispose()
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.READ_MEDIA_IMAGES)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        FileUtils.openImagePicker(this@ChatRoomProfileActivity)
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

    private fun checkPermissionGrantedForCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                ), MY_CAMERA_PERMISSION_CODE
            )
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
            } else {
                showToast(getString(R.string.msg_permission_denied))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)

            }
        } else if ((requestCode == CAMERA_REQUEST) and (resultCode == RESULT_OK)) {

            val photo = data!!.extras!!["data"] as Bitmap?
            val uri = photo?.let { getImageUri(applicationContext, it) }
            uri?.also {
                handlePathOz.getRealPath(it)
            }
        } else {

        }

    }

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val path: String =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                if (filePath.isNotEmpty()) {
                    selectedImagePath = filePath
                    Glide.with(this@ChatRoomProfileActivity)
                        .load(filePath)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                        .into(binding.ivUserProfileImage)

                    cloudFlareConfig?.let {
                        uploadImageToCloudFlare(it)
                    } ?: profileViewModel.getCloudFlareConfig()
                }
            }
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

    private fun isValidate(): Boolean {
        var isValidate = false
        val expression = "[0-9a-zA-Z._]*"
        val inputStr: CharSequence = binding.etUserName.text.toString()
        val pattern: Pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher: Matcher = pattern.matcher(inputStr)

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

    private fun editUserName() {
        if (isValidate()) {
            hideKeyboard()
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
        }
    }

    private fun manageEditUserNameView() {
        binding.ivEdit.visibility = View.VISIBLE
        binding.ivCheck.visibility = View.GONE
        binding.tvUserName.visibility = View.VISIBLE
        binding.etUserName.visibility = View.GONE
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