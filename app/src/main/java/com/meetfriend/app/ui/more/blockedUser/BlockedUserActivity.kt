package com.meetfriend.app.ui.more.blockedUser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.follow.model.BlockInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityBlockedUserBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.more.blockedUser.view.BlockedUserAdapter
import com.meetfriend.app.ui.more.blockedUser.viewmodel.BlockUserViewState
import com.meetfriend.app.ui.more.blockedUser.viewmodel.BlockedUserViewModel
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FileUtils
import javax.inject.Inject

class BlockedUserActivity : BasicActivity() {
    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, BlockedUserActivity::class.java)
        }
    }

    private lateinit var binding: ActivityBlockedUserBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<BlockedUserViewModel>
    private lateinit var blockedUserViewModel: BlockedUserViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var blockedUserAdapter: BlockedUserAdapter
    private var list: ArrayList<BlockInfo>? = arrayListOf()
    private var selectedPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        blockedUserViewModel = getViewModelFromFactory(viewModelFactory)

        FileUtils.loadBannerAd(this, binding.adView, loggedInUserCache.getBannerAdId())
        listenToViewModel()
        listenToViewEvent()
        blockedUserViewModel.resetPaginationForBlockUser()
    }

    private fun listenToViewEvent() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        blockedUserAdapter = BlockedUserAdapter(this).apply {
            userClicks.subscribeAndObserveOnMainThread {
                selectedPosition = list?.indexOf(it) ?: -1
                blockedUserViewModel.blockUnBlockUser(it.friend_id, Constant.UNBLOCK_STATUS)
            }.autoDispose()
        }

        binding.blockedPeopleRV.apply {
            adapter = blockedUserAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                blockedUserViewModel.loadMoreBlockUser()
                            }
                        }
                    }
                }
            })
        }
    }

    fun listenToViewModel() {
        blockedUserViewModel.blockUserState.subscribeAndObserveOnMainThread {
            when (it) {
                is BlockUserViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is BlockUserViewState.BlockedUserData -> {
                    list?.clear()
                    it.blockedUserData.forEach { blockInfo ->
                        if (blockInfo.is_blocked_by_me == 1) {
                            list?.add(blockInfo)
                        }
                    }
                    blockedUserAdapter.listOfDataItems = it.blockedUserData
                    hideLoading()
                }
                is BlockUserViewState.LoadingState -> {
                    if (blockedUserAdapter.listOfDataItems.isNullOrEmpty()) {
                        showLoading(it.isLoading)
                    }
                }
                is BlockUserViewState.SuccessMessage -> {
                    list?.removeAt(selectedPosition)
                    if (isLastItemVisible()) blockedUserViewModel.loadMoreBlockUser()

                    blockedUserAdapter.listOfDataItems = list
                    if (blockedUserAdapter.listOfDataItems.isNullOrEmpty()) {
                        binding.tvNoFriends.isVisible = true
                    }
                }
                is BlockUserViewState.EmptyState -> {
                    binding.tvNoFriends.isVisible = true
                    hideLoading()
                }
                else -> {
                }
            }
        }.autoDispose()
    }

    private fun isLastItemVisible(): Boolean {
        val layoutManager = binding.blockedPeopleRV.layoutManager as LinearLayoutManager
        val position = layoutManager.findLastVisibleItemPosition()
        return position >= blockedUserAdapter.itemCount - 1
    }
}
