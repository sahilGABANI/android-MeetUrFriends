package com.meetfriend.app.ui.livestreaming.videoroom

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityLiveRoomBinding
import com.meetfriend.app.newbase.ActivityManager.Companion.getInstance
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.livestreaming.LiveStreamingActivity
import com.meetfriend.app.ui.livestreaming.LiveSummaryBottomSheet
import com.meetfriend.app.ui.livestreaming.videoroom.view.LiveRoomAdapter
import com.meetfriend.app.ui.livestreaming.videoroom.viewmodel.VideoRoomsViewModel
import com.meetfriend.app.ui.livestreaming.videoroom.viewmodel.VideoRoomsViewState
import com.meetfriend.app.ui.livestreaming.watchlive.WatchLiveStreamingActivity
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.utils.Constant.FiXED_2000_MILLISECOND
import com.meetfriend.app.utils.FileUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class LiveRoomActivity : BasicActivity() {

    companion object {

        private const val LIVE_EVENT_INFO = "LIVE_EVENT_INFO"
        const val LIVE_ID = "LIVE_ID"
        const val INTENT_IS_JOIN = "INTENT_IS_JOIN"
        fun getIntent(
            context: Context,
            liveEventInfo: LiveEventInfo? = null,
            liveId: Int? = null,
            isJoin: Boolean? = false
        ): Intent {
            val intent = Intent(context, LiveRoomActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra(LIVE_EVENT_INFO, liveEventInfo)
            intent.putExtra(LIVE_ID, liveId)
            intent.putExtra(INTENT_IS_JOIN, isJoin)
            return intent
        }
    }

    private val verifySubject: PublishSubject<Unit> = PublishSubject.create()
    val verify: Observable<Unit> = verifySubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VideoRoomsViewModel>
    private lateinit var videoRoomsViewModel: VideoRoomsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUser: MeetFriendUser? = null

    lateinit var binding: ActivityLiveRoomBinding
    private lateinit var liveRoomAdapter: LiveRoomAdapter
    private var liveId: Int? = 0
    private var isJoin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        videoRoomsViewModel = getViewModelFromFactory(viewModelFactory)

        intent?.let {
            liveId = it.getIntExtra(LIVE_ID, 0)
            if (this.liveId != 0 && this.liveId != null) {
                showLoading()
            }
            isJoin = it.getBooleanExtra(INTENT_IS_JOIN, false)
            if (isJoin) {
                RxBus.publish(RxEvent.CloseLive)
            }
        }

        FileUtils.loadBannerAd(this, binding.adView, loggedInUserCache.getBannerAdId())
        listenToViewModel()
        listenToViewEvent()
        loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser

        intent?.let {
            if (it.getBooleanExtra(MainHomeActivity.INTENT_IS_SWITCH_ACCOUNT, false)) {
                intent?.getIntExtra(MainHomeActivity.USER_ID, 0)?.let { userId ->
                    RxBus.publish(RxEvent.switchUserAccount(userId))
                }
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            liveId = it.getIntExtra(LIVE_ID, 0)
            if (this.liveId != 0 && this.liveId != null) {
                showLoading()
            }
            isJoin = it.getBooleanExtra(INTENT_IS_JOIN, false)
            if (isJoin) {
                RxBus.publish(RxEvent.CloseLive)
            }
        }
    }

    private fun listenToViewModel() {
        videoRoomsViewModel.videoRoomsState.subscribeAndObserveOnMainThread {
            when (it) {
                is VideoRoomsViewState.LoadingState -> {
                }
                is VideoRoomsViewState.ErrorMessage -> {}
                is VideoRoomsViewState.SuccessMessage -> {}
                is VideoRoomsViewState.LoadAllActiveEventList -> {
                    if (liveId != 0 && liveId != null) {
                        var isLiveEnded = false
                        it.allActiveEventInfo.forEach { liveEventInfo ->
                            if (liveEventInfo.id == liveId) {
                                if (liveEventInfo.isKicked == 1) {
                                    showToast(getString(R.string.label_kickout_from_live))
                                } else {
                                    Handler().postDelayed({
                                        startActivity(
                                            WatchLiveStreamingActivity.getIntentWihData(
                                                liveEventInfo
                                            )
                                        )
                                    }, FiXED_2000_MILLISECOND)
                                }
                                liveId = 0
                                isLiveEnded = false
                            } else {
                                isLiveEnded = true
                            }
                        }
                        if (isLiveEnded) {
                            showToast("This Live Is Ended")
                            hideLoading()
                        }
                    }

                    binding.llNoData.visibility = View.GONE
                    binding.rvLiveRoomList.visibility = View.VISIBLE

                    it.allActiveEventInfo.apply {
                        liveRoomAdapter.listOfDataItems = it.allActiveEventInfo
                    }
                }
                VideoRoomsViewState.LiveEventEnd -> {
                    videoRoomsViewModel.getAllActiveLiveEvent()
                }
                VideoRoomsViewState.EmptyLiveEvent -> {
                    hideLoading()
                    binding.llNoData.visibility = View.VISIBLE
                    binding.rvLiveRoomList.visibility = View.GONE
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        RxBus.listen(RxEvent.DisplayLiveSummary::class.java).subscribeAndObserveOnMainThread {
            val foregroundActivity = getInstance().foregroundActivity
            if (foregroundActivity?.localClassName.equals(
                    "ui.livestreaming.videoroom.LiveRoomActivity",
                    ignoreCase = true
                )
            ) {
                val bottomSheet = LiveSummaryBottomSheet.newInstance(it.liveSummaryInfo)
                bottomSheet.show(supportFragmentManager, LiveSummaryBottomSheet::class.java.name)
            }
        }.autoDispose()

        intent?.getParcelableExtra<LiveEventInfo>(LIVE_EVENT_INFO)?.let {
            openWatchLiveEventActivity(it)
        }
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            videoRoomsViewModel.getAllActiveLiveEvent()
            binding.swipeRefreshLayout.isRefreshing = false
        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.fabStartLiveStreaming.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermissions(callBack = {
                startActivity(LiveStreamingActivity.getIntent(this@LiveRoomActivity))
            })
        }.autoDispose()

        liveRoomAdapter = LiveRoomAdapter(this).apply {
            roomClicks.subscribeAndObserveOnMainThread {
                openWatchLiveEventActivity(it)
            }.autoDispose()
        }

        binding.rvLiveRoomList.apply {
            layoutManager =
                GridLayoutManager(this@LiveRoomActivity, 2, LinearLayoutManager.VERTICAL, false)
            adapter = liveRoomAdapter
        }

        RxBus.listen(RxEvent.RemoveCoHost::class.java).subscribeAndObserveOnMainThread {
            videoRoomsViewModel.getAllActiveLiveEvent()
        }.autoDispose()
    }

    private fun checkPermissions(callBack: () -> Unit) {
        XXPermissions.with(this)
            .permission(
                listOf(
                    Permission.CAMERA,
                    Permission.RECORD_AUDIO,
                )
            )
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
                        callBack.invoke()
                    } else {
                        showToast(getString(R.string.msg_some_permission_denied))
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        showToast(getString(R.string.msg_permission_permanently_denied))
                        XXPermissions.startPermissionActivity(this@LiveRoomActivity, permissions)
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }
            })
    }

    private fun openWatchLiveEventActivity(liveEventInfo: LiveEventInfo) {
        checkPermissions(callBack = {
            if (liveEventInfo.isLock == 1) {
                openVerifyDialog(liveEventInfo)
            } else {
                if (liveEventInfo.isKicked == 1) {
                    showToast(getString(R.string.label_kickout_from_live))
                } else {
                    startActivity(WatchLiveStreamingActivity.getIntent(this, liveEventInfo))
                }
            }
        })
    }

    private fun openVerifyDialog(liveEventInfo: LiveEventInfo) {
        val liveEventLockDialogFragment = LiveStreamingEnterPasswordDialog()
        liveEventLockDialogFragment.apply {
            verify.subscribeAndObserveOnMainThread {
                startActivity(
                    WatchLiveStreamingActivity.getIntent(
                        this@LiveRoomActivity,
                        liveEventInfo
                    )
                )
                liveEventLockDialogFragment.dismiss()
            }.autoDispose()
        }
        liveEventLockDialogFragment.show(
            supportFragmentManager,
            "LiveEventLockDialogFragment"
        )
    }

    override fun onPause() {
        super.onPause()
        hideLoading()
    }

    override fun onResume() {
        super.onResume()
        videoRoomsViewModel.getAllActiveLiveEvent()
    }
}
