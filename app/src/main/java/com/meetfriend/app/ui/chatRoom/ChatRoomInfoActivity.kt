package com.meetfriend.app.ui.chatRoom

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
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.UpdateChatRoomRequest
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.profile.model.SelectedOption
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityChatRoomInfoBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.chatRoom.profile.ImagePickerOptionBottomSheet
import com.meetfriend.app.ui.chatRoom.view.ChatRoomAdminAdapter
import com.meetfriend.app.ui.chatRoom.view.ChatRoomParticipateAdapter
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomInfoViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomInfoViewState
import com.meetfriend.app.utils.FileUtils
import java.io.File
import javax.inject.Inject

class ChatRoomInfoActivity : BasicActivity() {

    companion object {
        private const val INTENT_CHATROOM_INFO = "INTENT_CHATROOM_INFO"
        fun getIntent(context: Context, chatRoomInfo: ChatRoomInfo): Intent {
            val intent = Intent(context, ChatRoomInfoActivity::class.java)
            intent.putExtra(INTENT_CHATROOM_INFO, chatRoomInfo)
            return intent
        }
    }

    private lateinit var binding: ActivityChatRoomInfoBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatRoomInfoViewModel>
    private lateinit var chatRoomInfoViewModel: ChatRoomInfoViewModel

    private lateinit var chatRoomAdminAdapter: ChatRoomAdminAdapter
    private lateinit var chatRoomParticipateAdapter: ChatRoomParticipateAdapter
    lateinit var chatRoomInfo: ChatRoomInfo
    private val MY_CAMERA_PERMISSION_CODE = 100
    private val CAMERA_REQUEST = 1888
    private lateinit var handlePathOz: HandlePathOz
    private var selectedImagePath: String = ""
    private var cloudFlareConfig: CloudFlareConfig? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatRoomInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        chatRoomInfoViewModel = getViewModelFromFactory(viewModelFactory)

        chatRoomInfoViewModel.getCloudFlareConfig()
        loadDataFromIntent()
        listenToViewEvent()
        listenToViewModel()

    }

    private fun loadDataFromIntent() {
        intent.let {
            chatRoomInfo = it.getParcelableExtra(INTENT_CHATROOM_INFO) ?: return
        }
        manageEditView(chatRoomInfo)
        binding.flDeleteContainer.isVisible = chatRoomInfo.isAdmin && chatRoomInfo.roomType == 1
        chatRoomInfoViewModel.getChatRoomInfo(chatRoomInfo.id)

    }

    private fun listenToViewEvent() {

        handlePathOz = HandlePathOz(this, listener)
        initAdapter()
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        chatRoomAdminAdapter = ChatRoomAdminAdapter(this)
        binding.rvAdmin.adapter = chatRoomAdminAdapter

        chatRoomParticipateAdapter = ChatRoomParticipateAdapter(this)
        binding.rvParticipate.adapter = chatRoomParticipateAdapter

        binding.ivEditRoomPic.throttleClicks().subscribeAndObserveOnMainThread {
            openSelectionBottomSheet()
        }.autoDispose()

        binding.ivChatRoomImage.throttleClicks().subscribeAndObserveOnMainThread {
            if (chatRoomInfo.isAdmin) {
                openSelectionBottomSheet()
            }
        }.autoDispose()

        binding.ivEditChatRoomName.throttleClicks().subscribeAndObserveOnMainThread {
            binding.tvChatRoomName.visibility = View.GONE
            binding.etChatRoomName.setText(binding.tvChatRoomName.text)
            binding.etChatRoomName.visibility = View.VISIBLE
            binding.ivEditChatRoomName.visibility = View.GONE
            binding.ivCheckRoomName.visibility = View.VISIBLE
            binding.etChatRoomName.requestFocus()
            showKeyboard()
        }.autoDispose()

        binding.ivCheckRoomName.throttleClicks().subscribeAndObserveOnMainThread {
            editChatRoomName()
        }.autoDispose()

        binding.ivEditRoomDescription.throttleClicks().subscribeAndObserveOnMainThread {
            binding.tvRoomDescription.visibility = View.GONE
            binding.etChatRoomDescription.setText(binding.tvRoomDescription.text)
            binding.etChatRoomDescription.visibility = View.VISIBLE
            binding.ivEditRoomDescription.visibility = View.GONE
            binding.ivCheckDescription.visibility = View.VISIBLE
            binding.etChatRoomDescription.requestFocus()
            showKeyboard()
        }.autoDispose()

        binding.ivCheckDescription.throttleClicks().subscribeAndObserveOnMainThread {
            editChatRoomDescription()
        }.autoDispose()

        binding.llDeleteContainer.throttleClicks().subscribeAndObserveOnMainThread {
            chatRoomInfoViewModel.deleteChatRoom(chatRoomInfo.id)
        }.autoDispose()

    }

    private fun initAdapter() {
        chatRoomParticipateAdapter = ChatRoomParticipateAdapter(this)
        binding.rvParticipate.adapter = chatRoomParticipateAdapter
    }

    private fun listenToViewModel() {
        chatRoomInfoViewModel.chatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatRoomInfoViewState.ChatRoomDetails -> {
                    loadData(it.chatRoomInfo)
                }
                is ChatRoomInfoViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ChatRoomInfoViewState.LoadingState -> {
                    manageLoadingState(it.isLoading)
                }
                is ChatRoomInfoViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is ChatRoomInfoViewState.ChatRoomUserInfo -> {
                    binding.tvParticipateCount.text =
                        getString(R.string._50_participate, it.chatRoomUserInfo?.size.toString())
                    chatRoomParticipateAdapter.listOfDataItems = it.chatRoomUserInfo
                }
                is ChatRoomInfoViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is ChatRoomInfoViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                  onBackPressedDispatcher.onBackPressed()
                }
                is ChatRoomInfoViewState.UploadImageCloudFlareSuccess -> {
                    val request = UpdateChatRoomRequest(
                        roomName = binding.tvChatRoomName.text.toString(),
                        roomDescription = binding.tvRoomDescription.text.toString(),
                        roomPic = it.imageUrl
                    )
                    chatRoomInfoViewModel.updateRoom(chatRoomInfo.id, request)
                }
                is ChatRoomInfoViewState.UpdatedChatRoomInfo -> {
                    manageEditRoomNameView()
                    manageEditChatRoomDescriptionView()
                    loadData(it.chatRoomInfo)
                    RxBus.publish(
                        RxEvent.UpdateChatRoom(
                            it.chatRoomInfo,
                            it.chatRoomInfo.roomName!!,
                            startVC = false,
                            fromProfile = false
                        )
                    )
                }
                is ChatRoomInfoViewState.DeleteLoadingState -> {
                    manageDeleteLoadingState(it.isLoading)
                }
                is ChatRoomInfoViewState.DeleteChatRoom -> {
                    finish()
                    RxBus.publish(RxEvent.DeleteChatRoom)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun loadData(chatRoomInfo: ChatRoomInfo?) {
        Glide.with(this)
            .load(chatRoomInfo?.filePath)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivChatRoomImage)
        binding.tvChatRoomName.text = chatRoomInfo?.roomName
        binding.tvRoomDescription.text = chatRoomInfo?.roomDescriprion
    }

    private fun manageLoadingState(isLoading: Boolean) {
        if (isLoading) {
            showLoading()
            binding.nestedScrollView.visibility = View.GONE

        } else {
            hideLoading()
            binding.nestedScrollView.visibility = View.VISIBLE

        }
    }

    private fun manageEditView(chatRoomInfo: ChatRoomInfo) {
        if (chatRoomInfo.isAdmin && chatRoomInfo.roomType == 1) {
            binding.flEditRoomDescription.visibility = View.VISIBLE
            binding.ivEditRoomPic.visibility = View.VISIBLE
            binding.ivEditRoomDescription.visibility = View.VISIBLE

        } else {
            binding.flEditRoomDescription.visibility = View.GONE
            binding.flEditUserNameIcon.visibility = View.GONE
            binding.ivEditRoomPic.visibility = View.GONE
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
                        FileUtils.openImagePicker(this@ChatRoomInfoActivity)
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

            val photo = data?.extras?.get("data") as Bitmap?
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
                    Glide.with(this@ChatRoomInfoActivity)
                        .load(filePath)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                        .into(binding.ivChatRoomImage)

                    cloudFlareConfig?.let {
                        uploadImageToCloudFlare(it)
                    } ?: chatRoomInfoViewModel.getCloudFlareConfig()
                }
            }
        }
    }

    private fun uploadImageToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            chatRoomInfoViewModel.uploadImageToCloudFlare(
                this,
                cloudFlareConfig,
                File(selectedImagePath)
            )
        }
    }

    private fun editChatRoomName() {
        if (!binding.etChatRoomName.text.toString().isNullOrEmpty()) {
            hideKeyboard()
            if (binding.etChatRoomName.text.toString() != chatRoomInfo.roomName) {
                chatRoomInfoViewModel.updateRoom(
                    chatRoomInfo.id,
                    UpdateChatRoomRequest(
                        roomName = binding.etChatRoomName.text.toString(),
                        roomDescription = binding.tvRoomDescription.text.toString(),
                        roomPic = chatRoomInfo.filePath
                    )
                )
            } else {
                binding.ivCheckRoomName.isVisible = false
                binding.ivEditChatRoomName.isVisible = true
                binding.etChatRoomName.isVisible = false
                binding.tvChatRoomName.isVisible = true
            }

        } else {
            showToast(getString(R.string.error_please_enter_room_name))
        }
    }

    private fun manageEditRoomNameView() {
        binding.ivEditChatRoomName.visibility = View.VISIBLE
        binding.ivCheckRoomName.visibility = View.GONE
        binding.tvChatRoomName.visibility = View.VISIBLE
        binding.etChatRoomName.visibility = View.GONE
    }

    private fun editChatRoomDescription() {
        if (!binding.etChatRoomDescription.text.toString().isNullOrEmpty()) {
            hideKeyboard()
            if (binding.etChatRoomDescription.text.toString() != chatRoomInfo.roomDescriprion) {
                chatRoomInfoViewModel.updateRoom(
                    chatRoomInfo.id,
                    UpdateChatRoomRequest(
                        roomName = binding.tvChatRoomName.text.toString(),
                        roomDescription = binding.etChatRoomDescription.text.toString(),
                        roomPic = chatRoomInfo.filePath
                    )
                )
            } else {
                binding.ivCheckDescription.isVisible = false
                binding.ivEditRoomDescription.isVisible = true
                binding.etChatRoomDescription.isVisible = false
                binding.tvRoomDescription.isVisible = true
            }

        } else {
            showToast(getString(R.string.label_room_description))
        }
    }

    private fun manageEditChatRoomDescriptionView() {
        binding.ivEditRoomDescription.visibility = View.VISIBLE
        binding.ivCheckDescription.visibility = View.GONE
        binding.tvRoomDescription.visibility = View.VISIBLE
        binding.etChatRoomDescription.visibility = View.GONE
    }

    private fun manageDeleteLoadingState(isLoading: Boolean) {
        binding.deleteProgressBar.isVisible = isLoading
        binding.deleteProgressBar.isVisible = isLoading
        binding.llDeleteContainer.isVisible = !isLoading
    }
}