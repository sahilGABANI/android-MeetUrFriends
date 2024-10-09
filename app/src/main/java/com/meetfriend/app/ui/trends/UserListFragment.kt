package com.meetfriend.app.ui.trends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentUserListBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.hideKeyboard
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.hashtag.HashTagListActivity
import com.meetfriend.app.ui.follow.viewmodel.FollowingState
import com.meetfriend.app.ui.follow.viewmodel.FollowingViewModel
import com.meetfriend.app.ui.trends.view.UserListAdapter
import javax.inject.Inject

class UserListFragment : BasicFragment() {

    companion object {
        private const val INTENT_SEARCH = "searchText"
        private const val INTENT_TREND = "INTENT_TREND"

        fun newInstanceWithSearch(fromtrend: Int, search: String? = null): UserListFragment {
            val args = Bundle()
            args.putInt(INTENT_TREND, fromtrend)
            args.putString(INTENT_SEARCH, search)
            val fragment = UserListFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<FollowingViewModel>
    private lateinit var followingViewModel: FollowingViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var userListAdapter: UserListAdapter
    private var listOfFollowing: List<MeetFriendUser> = listOf()
    private var count = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        followingViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        userListAdapter = UserListAdapter(requireContext()).apply {
            hashtagItemClicks.subscribeAndObserveOnMainThread { state ->
                startActivity(
                    HashTagListActivity.getIntent(
                        requireContext(),
                        state.id,
                        state.tagName ?: ""
                    )
                )
            }.autoDispose()
        }

        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL

        binding.rvFollowingList.apply {
            layoutManager = llm
            adapter = userListAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                followingViewModel.loadMoreFollowing()
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            followingViewModel.resetPaginationForFollowing("")
        }.autoDispose()
    }

    private fun listenToViewModel() {
        followingViewModel.followingStates.subscribeAndObserveOnMainThread {
            when (it) {
                is FollowingState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is FollowingState.FollowingData -> {
                    if (!it.followingData.isNullOrEmpty()) {
                        listOfFollowing = it.followingData
                    }
                    userListAdapter.listOfDataItems = it.followingData
                    hideShowNoData(listOfFollowing)
                    binding.progressBar.isVisible = false
                }

                is FollowingState.LoadingState -> {
                    if (userListAdapter.listOfDataItems.isNullOrEmpty()) {
                        binding.progressBar.isVisible = it.isLoading
                    }
                }

                FollowingState.EmptyState -> {
                    hideShowNoData(listOf())
                    binding.progressBar.isVisible = false
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(followingList: List<MeetFriendUser>) {
        if (arguments?.containsKey(INTENT_TREND) == true) {
            if (followingList.isNotEmpty()) {
                binding.llEmptyStateUsers.visibility = View.GONE
                binding.rvFollowingList.visibility = View.VISIBLE
            } else {
                binding.llEmptyStateUsers.visibility = View.VISIBLE
                binding.rvFollowingList.visibility = View.GONE
                hideKeyboard()
            }
            binding.llEmptyState.visibility = View.GONE
        } else {
            if (followingList.isNotEmpty()) {
                binding.llEmptyState.visibility = View.GONE
            } else {
                binding.llEmptyState.visibility = View.VISIBLE
            }
            binding.llEmptyStateUsers.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (count == 0) {
            if (arguments?.containsKey(INTENT_TREND) == true) {
                followingViewModel.resetPaginationForFollowing("")
                RxBus.listen(RxEvent.SearchTextUser::class.java).subscribeAndObserveOnMainThread {
                    followingViewModel.resetPaginationForFollowing(it.search.toString())
                }.autoDispose()
            } else {
                followingViewModel.resetPaginationForFollowing("")
            }
            count += 1
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        count = 0
    }
}
