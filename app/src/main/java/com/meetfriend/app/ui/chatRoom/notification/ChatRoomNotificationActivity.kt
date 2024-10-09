package com.meetfriend.app.ui.chatRoom.notification

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.notification.model.AcceptRejectRequestRequest
import com.meetfriend.app.api.notification.model.NotificationActionState
import com.meetfriend.app.api.notification.model.NotificationItemInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityChatRoomNotificationBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.challenge.ChallengeDetailsActivity
import com.meetfriend.app.ui.chatRoom.notification.view.ChatRoomNotificationAdapter
import com.meetfriend.app.ui.chatRoom.notification.viewmodel.NotificationViewModel
import com.meetfriend.app.ui.chatRoom.notification.viewmodel.NotificationViewState
import com.meetfriend.app.ui.follow.request.FollowRequestActivity
import com.meetfriend.app.ui.home.shortdetails.ShortDetailsActivity
import com.meetfriend.app.ui.livestreaming.videoroom.LiveRoomActivity
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.mygifts.GiftsTransactionActivity
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utils.Constant.ALL
import com.meetfriend.app.utils.Constant.CHALLENGE
import com.meetfriend.app.utils.Constant.COMMENT
import com.meetfriend.app.utils.Constant.FOLLOW
import com.meetfriend.app.utils.Constant.GIFT
import com.meetfriend.app.utils.Constant.HOST_INVITE
import com.meetfriend.app.utils.Constant.POST
import com.meetfriend.app.utils.Constant.SENT_YOU_FOLLOW_REQUEST
import com.meetfriend.app.utils.Constant.SHORTS
import com.meetfriend.app.utils.FileUtils
import javax.inject.Inject

class ChatRoomNotificationActivity : BasicActivity() {

    companion object {
        fun getIntent(context: android.content.Context): Intent {
            return Intent(context, ChatRoomNotificationActivity::class.java)
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<NotificationViewModel>
    lateinit var notificationViewModel: NotificationViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var binding: ActivityChatRoomNotificationBinding

    private lateinit var notificationAdapter: ChatRoomNotificationAdapter
    private var deleteNotificationInfo = NotificationItemInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatRoomNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        notificationViewModel = getViewModelFromFactory(viewModelFactory)

        FileUtils.loadBannerAd(this, binding.adView, loggedInUserCache.getBannerAdId())
        listenToViewModel()
        listenToViewEvent()
        intent?.let {
            if (it.getBooleanExtra(MainHomeActivity.INTENT_IS_SWITCH_ACCOUNT, false)) {
                intent?.getIntExtra(MainHomeActivity.USER_ID, 0)?.let { userId ->
                    RxBus.publish(RxEvent.switchUserAccount(userId))
                }
            }
        }
    }

    private fun listenToViewEvent() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.tvClearAll.throttleClicks().subscribeAndObserveOnMainThread {
            deleteAlertDialog(ALL, true)
        }.autoDispose()

        initAdapter()

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            notificationViewModel.resetPaginationForNotification()
        }.autoDispose()
    }

    private fun initAdapter() {
        notificationAdapter = ChatRoomNotificationAdapter(this).apply {
            notificationState.subscribeAndObserveOnMainThread {
                when (it) {
                    is NotificationActionState.AcceptClick -> {
                        handleAcceptClick(it)
                    }
                    is NotificationActionState.ProfileImageClick -> {
                        handleProfileImageClick(it)
                    }
                    is NotificationActionState.RejectClick -> {
                        handleRejectClick(it)
                    }
                    is NotificationActionState.ContainerClick -> {
                        openDetail(
                            it.notificationItemInfo.typeId,
                            it.notificationItemInfo.type,
                            it.notificationItemInfo.message,
                            it.notificationItemInfo.fromUser?.id
                        )
                    }
                    is NotificationActionState.DeleteClick -> {
                        deleteNotificationInfo = it.notificationItemInfo
                        deleteAlertDialog(it.notificationItemInfo.id.toString(), false)
                    }
                    else -> {}
                }
            }.autoDispose()
        }
        binding.rvNotification.apply {
            layoutManager = LinearLayoutManager(
                this@ChatRoomNotificationActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = notificationAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                notificationViewModel.loadMoreNotification()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun handleRejectClick(it: NotificationActionState.RejectClick) {
        val typeId = it.notificationItemInfo.typeId
        notificationViewModel.acceptRejectRequest(
            AcceptRejectRequestRequest(
                conversationId = typeId,
                status = 2,
                fromUid = it.notificationItemInfo.fromUser?.id
            )
        )
    }

    private fun handleProfileImageClick(it: NotificationActionState.ProfileImageClick) {
        startActivity(
            MyProfileActivity
                .getIntentWithData(
                    this@ChatRoomNotificationActivity,
                    it.notificationItemInfo.fromUser?.id ?: 0
                )
        )
    }

    private fun handleAcceptClick(it: NotificationActionState.AcceptClick) {
        val typeId = it.notificationItemInfo.typeId
        notificationViewModel.acceptRejectRequest(
            AcceptRejectRequestRequest(
                conversationId = typeId,
                status = 1,
                fromUid = it.notificationItemInfo.fromUser?.id
            )
        )
    }

    private fun listenToViewModel() {
        notificationViewModel.notificationState.subscribeAndObserveOnMainThread {
            when (it) {
                is NotificationViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is NotificationViewState.LoadingState -> {
                    manageViewVisibility(it.isLoading)
                }
                is NotificationViewState.NotificationData -> {
                    setNotificationData(it.notificationList)
                }
                is NotificationViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is NotificationViewState.DeleteAllData -> {
                    if (it.deleteData) {
                        notificationViewModel.resetPaginationForNotification()
                        setNotificationData(emptyList())
                        binding.rvNotification.isVisible = false
                    }
                }

                is NotificationViewState.DeleteData -> {
                    if (it.deleteData) {
                        val list = notificationAdapter.listOfDataItems as ArrayList
                        list.remove(deleteNotificationInfo)
                        notificationAdapter.listOfDataItems = list
                    }
                    if (notificationAdapter.listOfDataItems?.size == 0) {
                        binding.tvNoSecurity.isVisible = true
                        binding.tvClearAll.isVisible = false
                    }
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun setNotificationData(notificationList: List<NotificationItemInfo>) {
        if (notificationList.isNullOrEmpty()) {
            binding.tvNoSecurity.isVisible = true
            binding.tvClearAll.isVisible = false
        } else {
            binding.tvNoSecurity.isVisible = false
            notificationAdapter.listOfDataItems = notificationList
            binding.tvClearAll.isVisible = true
        }
    }

    private fun manageViewVisibility(loading: Boolean) {
        if (notificationAdapter.listOfDataItems.isNullOrEmpty()) {
            if (loading) showLoading() else hideLoading()
        }
    }

    override fun onResume() {
        super.onResume()
        notificationViewModel.resetPaginationForNotification()
        notificationViewModel.notificationMarkAllRead()
    }

    private fun openDetail(id: Int?, from: String?, title: String?, otherUserId: Int?) {
        when (from) {
            POST, SHORTS -> handlePostOrShorts(id, from, title)
            GIFT -> openGiftsTransaction()
            CHALLENGE -> handleChallenge(id, title)
            FOLLOW -> handleFollow(title, otherUserId)
            HOST_INVITE -> openLiveRoom(id)
            else -> {
                // Handle unknown cases if necessary
            }
        }
    }

    private fun handlePostOrShorts(id: Int?, from: String?, title: String?) {
        val displayComment = from == SHORTS && title?.contains(COMMENT) == true
        startActivity(id?.let { ShortDetailsActivity.getIntent(this, it, displayComment) })
    }

    private fun openGiftsTransaction() {
        startActivity(GiftsTransactionActivity.getIntent(this))
    }

    private fun handleChallenge(id: Int?, title: String?) {
        if (title?.contains(GIFT) == true) {
            openGiftsTransaction()
        } else {
            startActivity(id?.let { ChallengeDetailsActivity.getIntent(this, it) })
        }
    }

    private fun handleFollow(title: String?, otherUserId: Int?) {
        when {
            title != null && title.contains(SENT_YOU_FOLLOW_REQUEST) ->
                startActivity(FollowRequestActivity.getIntent(this))
            else ->
                startActivity(MyProfileActivity.getIntentWithData(this, otherUserId ?: 0))
        }
    }

    private fun openLiveRoom(id: Int?) {
        startActivity(LiveRoomActivity.getIntent(this, liveId = id))
    }

    private fun deleteAlertDialog(id: String, deleteAll: Boolean) {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialog.setTitle(resources.getString(R.string.label_delete))
        if (deleteAll) {
            alertDialog.setMessage(resources.getString(R.string.label_you_sure_wen_to_clear_all))
        } else {
            alertDialog.setMessage(resources.getString(R.string.label_you_sure_wen_to_delete))
        }
        if (deleteAll) {
            alertDialog.setPositiveButton(resources.getString(R.string.label_yes)) { _: DialogInterface, _: Int ->
                notificationViewModel.deleteAllNotification()
            }
        } else {
            alertDialog.setPositiveButton(resources.getString(R.string.label_yes)) { _: DialogInterface, _: Int ->
                notificationViewModel.deleteNotification(id)
            }
        }
        alertDialog.setNegativeButton(resources.getString(R.string.label_no)) { _: DialogInterface?, _: Int ->
        }
        val alert: AlertDialog = alertDialog.create()
        alert.show()
    }
}
