package com.meetfriend.app.ui.chatRoom.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.AbsListView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.meetfriend.app.api.profile.model.ChatRoomUserInfo
import com.meetfriend.app.api.profile.model.ProfileItemInfo
import com.meetfriend.app.api.profile.model.ProfileMoreActionState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityViewUserProfileBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.profile.view.ViewUserProfileAdapter
import com.meetfriend.app.ui.chatRoom.profile.viewmodel.ProfileViewModel
import com.meetfriend.app.ui.chatRoom.profile.viewmodel.ProfileViewState
import com.meetfriend.app.ui.myprofile.ReportMainUserBottomSheet
import com.meetfriend.app.utils.ShareHelper
import javax.inject.Inject


class ViewUserProfileActivity : BasicActivity() {

    companion object {
        private const val INTENT_USER_PROFILE_INFO = "INTENT_USER_PROFILE_INFO"
        private const val INTENT_LIST_OF_PROFILE_IMAGE = "INTENT_LIST_OF_PROFILE_IMAGE"
        private const val INTENT_SELECTED_IMAGE_INFO = "INTENT_SELECTED_IMAGE_INFO"

        fun getIntent(
            context: Context,
            listOfProfileImage: ArrayList<ProfileItemInfo>,
            selectedImageInfo: ProfileItemInfo,
            userProfileInfo: ChatRoomUserInfo?,
        ): Intent {
            val intent = Intent(context, ViewUserProfileActivity::class.java)
            intent.putParcelableArrayListExtra(INTENT_LIST_OF_PROFILE_IMAGE, listOfProfileImage)
            intent.putExtra(INTENT_SELECTED_IMAGE_INFO, selectedImageInfo)
            intent.putExtra(INTENT_USER_PROFILE_INFO, userProfileInfo)
            return intent
        }

    }

    private lateinit var binding: ActivityViewUserProfileBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ProfileViewModel>
    lateinit var profileViewModel: ProfileViewModel

    private lateinit var viewUserProfileAdapter: ViewUserProfileAdapter
    private var listOfProfileImages: ArrayList<ProfileItemInfo> = arrayListOf()
    lateinit var selectedImageInfo: ProfileItemInfo
    private var userProfileInfo: ChatRoomUserInfo? = null
    private var currentVisibleItem: ProfileItemInfo? = null
    private var deletedItem: ProfileItemInfo? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityViewUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        profileViewModel = getViewModelFromFactory(viewModelFactory)

        loadDataFromIntent()
        listenToViewModel()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            listOfProfileImages =
                it.getParcelableArrayListExtra(INTENT_LIST_OF_PROFILE_IMAGE) ?: return
            selectedImageInfo = it.getParcelableExtra(INTENT_SELECTED_IMAGE_INFO) ?: return
            userProfileInfo = it.getParcelableExtra(INTENT_USER_PROFILE_INFO)

            listenToViewEvent()
            binding.tvUserName.text = userProfileInfo?.chatUserName
        }
    }

    private fun listenToViewEvent() {
        viewUserProfileAdapter = ViewUserProfileAdapter(this)

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvProfileImage)

        binding.rvProfileImage.apply {
            adapter = viewUserProfileAdapter
            layoutManager = LinearLayoutManager(
                this@ViewUserProfileActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            onScrollDoneGetPosition {
                currentVisibleItem = listOfProfileImages[it]
            }
        }
        viewUserProfileAdapter.listOfDataItems = listOfProfileImages

        val mPos = listOfProfileImages.indexOfFirst {
            it.filePath == selectedImageInfo.filePath
        }
        if (mPos != -1) {
            binding.rvProfileImage.scrollToPosition(mPos)

        }

        binding.ivMore.throttleClicks().subscribeAndObserveOnMainThread {
            openMoreBottomSheet()
        }.autoDispose()

    }


    private fun listenToViewModel() {
        profileViewModel.profileState.subscribeAndObserveOnMainThread {
            when (it) {
                is ProfileViewState.DeleteProfileSuccessMessage -> {
                    listOfProfileImages.remove(deletedItem)
                    viewUserProfileAdapter.listOfDataItems = listOfProfileImages
                    RxBus.publish(RxEvent.DeleteProfileImage)
                }
                is ProfileViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ProfileViewState.LoadingState -> {

                }
                else -> {}
            }
        }.autoDispose()
    }

    fun RecyclerView.onScrollDoneGetPosition(onScrollUpdate: (Int) -> Unit) {
        this.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    AbsListView.OnScrollListener.SCROLL_STATE_FLING -> {
                    }
                    AbsListView.OnScrollListener.SCROLL_STATE_IDLE -> {
                        val currentPosition =
                            (this@onScrollDoneGetPosition.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        onScrollUpdate.invoke(currentPosition)
                    }
                    AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL -> {
                    }
                }
            }
        })
    }

    private fun openMoreBottomSheet() {
        deletedItem = if (currentVisibleItem != null) currentVisibleItem else selectedImageInfo
        val moreBottomSheet = OtherUserMoreBottomSheet.newInstance(deletedItem)
        val imageId = currentVisibleItem?.id ?: selectedImageInfo.id
        moreBottomSheet.profileMoreOptionState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                ProfileMoreActionState.DeleteClick -> {
                    val deleteOptionBottomSheet = ConfirmDeleteBottomSheet()
                    deleteOptionBottomSheet.deleteClicks.subscribeAndObserveOnMainThread {
                        profileViewModel.deleteProfileImage(imageId)
                    }.autoDispose()
                    deleteOptionBottomSheet.show(
                        supportFragmentManager,
                        ConfirmDeleteBottomSheet::class.java.name
                    )
                }
                is ProfileMoreActionState.ReportClick -> {
                    val reportUserBottomSheet = ReportMainUserBottomSheet.newInstance(0, true)
                    reportUserBottomSheet.show(
                        supportFragmentManager,
                        ReportMainUserBottomSheet::class.java.name
                    )
                }
                is ProfileMoreActionState.ShareClick -> {
                    shareProfileImage()
                }
            }
        }.autoDispose()
        moreBottomSheet.show(supportFragmentManager, OtherUserMoreBottomSheet::class.java.name)
    }

    private fun shareProfileImage() {
        ShareHelper.shareDeepLink(this, 4, 0, deletedItem?.filePath.toString(),true) {
            ShareHelper.shareText(this, it)
        }
    }
}