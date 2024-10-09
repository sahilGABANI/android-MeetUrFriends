package com.meetfriend.app.ui.follow.request

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowRequestClickStates
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityFollowRequestBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.follow.request.view.FollowRequestAdapter
import com.meetfriend.app.ui.follow.request.viewmodel.FollowRequestState
import com.meetfriend.app.ui.follow.request.viewmodel.FollowRequestViewModel
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utils.FileUtils
import javax.inject.Inject

class FollowRequestActivity : BasicActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, FollowRequestActivity::class.java)
        }
    }

    private lateinit var binding: ActivityFollowRequestBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<FollowRequestViewModel>
    private lateinit var followRequestViewModel: FollowRequestViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var followRequestAdapter: FollowRequestAdapter
    private var selectedPosition = -1
    private var listOfFollowRequest: MutableList<MeetFriendUser>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFollowRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        followRequestViewModel = getViewModelFromFactory(viewModelFactory)

        FileUtils.loadBannerAd(this, binding.adView, loggedInUserCache.getBannerAdId())
        listenToViewModel()
        listenToViewEvent()
        followRequestViewModel.resetPaginationForFollowRequest()
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

        followRequestAdapter = FollowRequestAdapter(this).apply {
            userClicks.subscribeAndObserveOnMainThread {
                when (it) {
                    is FollowRequestClickStates.AcceptClick -> {
                        selectedPosition = listOfFollowRequest?.indexOf(it.user) ?: 0
                        followRequestViewModel.acceptFollowRequest(it.user.followerId ?: 0)
                    }
                    is FollowRequestClickStates.RejectClick -> {
                        selectedPosition = listOfFollowRequest?.indexOf(it.user) ?: 0
                        followRequestViewModel.rejectFollowRequest(it.user.followerId ?: 0)
                    }
                    is FollowRequestClickStates.ProfileClick -> {
                        startActivity(
                            MyProfileActivity.getIntentWithData(
                                this@FollowRequestActivity,
                                it.user.id
                            )
                        )
                    }
                }
            }.autoDispose()
        }
        binding.rvFollowRequests.apply {
            adapter = followRequestAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                followRequestViewModel.loadMoreFollowRequest()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun listenToViewModel() {
        followRequestViewModel.followRequestStates.subscribeAndObserveOnMainThread {
            when (it) {
                FollowRequestState.EmptyState -> {
                    if (followRequestAdapter.listOfDataItems.isNullOrEmpty()) {
                        binding.llEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.llEmptyState.visibility = View.GONE
                    }
                }
                is FollowRequestState.ErrorMessage -> {
                }
                is FollowRequestState.FollowRequestsData -> {
                    hideLoading()
                    listOfFollowRequest = it.requestData
                    followRequestAdapter.listOfDataItems = it.requestData
                }
                is FollowRequestState.LoadingState -> {
                    if (followRequestAdapter.listOfDataItems.isNullOrEmpty()) {
                        if (it.isLoading) showLoading() else hideLoading()
                    }
                }
                is FollowRequestState.SuccessMessage -> {
                    listOfFollowRequest?.removeAt(selectedPosition)
                    if (isLastItemVisible()) followRequestViewModel.loadMoreFollowRequest()
                    followRequestAdapter.listOfDataItems = listOfFollowRequest
                }
            }
        }.autoDispose()
    }

    private fun isLastItemVisible(): Boolean {
        val layoutManager = binding.rvFollowRequests.layoutManager as LinearLayoutManager
        val position = layoutManager.findLastVisibleItemPosition()
        return position >= followRequestAdapter.itemCount - 1
    }
}
