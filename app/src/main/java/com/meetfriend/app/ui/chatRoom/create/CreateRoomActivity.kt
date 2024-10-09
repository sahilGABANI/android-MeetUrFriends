package com.meetfriend.app.ui.chatRoom.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.meetfriend.app.api.chat.model.CreateChatRoomRequest
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.profile.model.SelectedOption
import com.meetfriend.app.api.subscription.model.SubscriptionOption
import com.meetfriend.app.api.subscription.model.TempPlanInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityCreateRoomBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.chatRoom.create.view.PlanItemAdapter
import com.meetfriend.app.ui.chatRoom.create.viewmodel.CreateChatRoomViewModel
import com.meetfriend.app.ui.chatRoom.create.viewmodel.CreateChatRoomViewState
import com.meetfriend.app.ui.chatRoom.profile.ImagePickerOptionBottomSheet
import com.meetfriend.app.ui.chatRoom.subscription.SubscriptionActivity
import com.meetfriend.app.ui.home.shorts.PostCameraActivity
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FileUtils
import com.meetfriend.app.utils.FileUtils.PICK_IMAGE
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

class CreateRoomActivity : BasicActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, CreateRoomActivity::class.java)
        }
    }

    private lateinit var binding: ActivityCreateRoomBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateChatRoomViewModel>
    private lateinit var createChatRoomViewModel: CreateChatRoomViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    var loggedInUserId = 0
    private var selectedImagePath: String = ""
    private lateinit var handlePathOz: HandlePathOz
    private var cloudFlareConfig: CloudFlareConfig? = null
    private lateinit var planItemAdapter: PlanItemAdapter
    private var listOfPlans: MutableList<TempPlanInfo> = mutableListOf()
    lateinit var selectedPlanInfo: TempPlanInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        createChatRoomViewModel = getViewModelFromFactory(viewModelFactory)

        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        loadPlanData()
        listenToViewEvent()
        listenToViewModel()
        createChatRoomViewModel.getCloudFlareConfig()

    }

    private fun listenToViewEvent() {

        handlePathOz = HandlePathOz(this, listener)

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.tvCreateRoom.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                cloudFlareConfig?.let {
                    uploadImageToCloudFlare(it)
                } ?: createChatRoomViewModel.getCloudFlareConfig()
            }
            openInterstitialAds()
            Constant.CLICK_COUNT++
        }.autoDispose()

        binding.createRoomRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            openSelectionBottomSheet()
        }.autoDispose()

        planItemAdapter = PlanItemAdapter(this).apply {

            planItemClicks.subscribeAndObserveOnMainThread {
                selectedPlanInfo = it
                planItemAdapter.selectedRoomType = it.roomType

            }.autoDispose()
        }
        binding.rvPlans.adapter = planItemAdapter

        planItemAdapter.listOfDataItems = listOfPlans

        binding.ivEditProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            openSelectionBottomSheet()
        }.autoDispose()

    }

    private fun listenToViewModel() {

        createChatRoomViewModel.createChatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is CreateChatRoomViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                    manageViewVisibility(false)
                }

                is CreateChatRoomViewState.SuccessMessage -> {
                    finish()
                }

                is CreateChatRoomViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is CreateChatRoomViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                  onBackPressedDispatcher.onBackPressed()
                }
                is CreateChatRoomViewState.UploadImageCloudFlareSuccess -> {

                    val request = CreateChatRoomRequest(
                        userId = loggedInUserId,
                        roomName = binding.etRoomName.text.toString(),
                        roomDescriprion = binding.etRoomDescription.text.toString(),
                        roomPic = it.imageUrl,
                        roomType = 1
                    )
                    createChatRoomViewModel.createChatRoom(request)
                }
                is CreateChatRoomViewState.LoadingState -> {
                    manageViewVisibility(it.isLoading)
                }
                is CreateChatRoomViewState.CreateRoomSuccess -> {
                    RxBus.publish(RxEvent.CreateChatRoom)

                    val props = JSONObject()
                    props.put("content_type", "chatroom")


                    mp?.track("Create Content", props)

                    startActivity(
                        SubscriptionActivity.getIntent(
                            this@CreateRoomActivity,
                            selectedPlanInfo,
                            it.chatRoomInfo.id, SubscriptionOption.ChatRoom.name
                        )
                    )
                    finish()
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun isValidate(): Boolean {
        var isValidate = false
        when {
            selectedImagePath.isNullOrEmpty() -> {
                showToast(getString(R.string.error_please_select_room_image))
                isValidate = false
            }

            binding.etRoomName.text.isNullOrEmpty() -> {
                showToast(getString(R.string.error_please_enter_room_name))
                isValidate = false
            }

            binding.etRoomDescription.text.isNullOrEmpty() -> {
                showToast(getString(R.string.error_please_enter_room_description))
                isValidate = false
            }

            else -> isValidate = true
        }
        return isValidate
    }

    private fun manageViewVisibility(loading: Boolean) {
        if (loading) {
            binding.tvCreateRoom.visibility = View.GONE
            showLoading()
        } else {
            binding.tvCreateRoom.visibility = View.VISIBLE
           hideLoading()
        }
    }

    private fun uploadImageToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            createChatRoomViewModel.uploadImageToCloudFlare(
                this,
                cloudFlareConfig,
                File(selectedImagePath)
            )
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
                    checkPermissionGrantedForCamera(this)
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
                                this@CreateRoomActivity,
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
                        XXPermissions.startPermissionActivity(this@CreateRoomActivity, permissions)
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
                        FileUtils.openImagePicker(this@CreateRoomActivity)
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
        if ((requestCode == PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
            }
        } else if ((requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) and (resultCode == RESULT_OK)) {

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
            Glide.with(this@CreateRoomActivity)
                .load(filePath)
                .error(R.drawable.ic_empty_profile_placeholder)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.ivUserProfileImage)
        }
    }

    private fun loadPlanData() {

        listOfPlans.add(
            TempPlanInfo(
                coinImage = null,
                roomType = getString(R.string.label_primary_room),
                amount = getString(R.string.price_7_99),
                duration = getString(R.string.label_1_month),
                isSelected = false,
                month = 1
            )
        )

        listOfPlans.add(
            TempPlanInfo(
                coinImage = null,
                roomType = getString(R.string.label_basic_room),
                amount = getString(R.string.price_23_99),
                duration = getString(R.string.label_3_month),
                isSelected = false,
                month = 3

            )
        )

        listOfPlans.add(
            TempPlanInfo(
                coinImage = null,
                roomType = getString(R.string.label_standard_room),
                amount = getString(R.string.price_47_99),
                duration = getString(R.string.label_6_month),
                isSelected = false,
                month = 6
            )
        )
        listOfPlans.add(
            TempPlanInfo(
                coinImage = null,
                roomType = getString(R.string.label_premium_room),
                amount = getString(R.string.price_95_99),
                duration = getString(R.string.label_12_month),
                isSelected = false,
                month = 12
            )
        )
        selectedPlanInfo = listOfPlans.first()
    }

}