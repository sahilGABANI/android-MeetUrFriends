package com.meetfriend.app.ui.follow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowClickStates
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentFollowingBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.hideKeyboard
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.follow.view.FollowingListAdapter
import com.meetfriend.app.ui.follow.viewmodel.FollowingState
import com.meetfriend.app.ui.follow.viewmodel.FollowingViewModel
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class FollowingFragment : BasicFragment() {

    companion object {
        private const val INTENT_USER_ID = "userId"
        private const val INTENT_TREND = "INTENT_TREND"

        fun newInstance(userId: Int): FollowingFragment {
            val args = Bundle()
            userId.let { args.putInt(INTENT_USER_ID, it) }
            val fragment = FollowingFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentFollowingBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<FollowingViewModel>
    private lateinit var followingViewModel: FollowingViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var followingListAdapter: FollowingListAdapter
    private var userId = -1
    private var listOfFollowing: List<MeetFriendUser> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        followingViewModel = getViewModelFromFactory(viewModelFactory)

        userId = arguments?.getInt(INTENT_USER_ID) ?: throw IllegalStateException("No args provided")

        if(arguments?.containsKey(INTENT_TREND) ?: false) {
            followingViewModel.resetPaginationForFollowing("")
            RxBus.listen(RxEvent.SearchTextUser::class.java).subscribeAndObserveOnMainThread {
                followingViewModel.resetPaginationForFollowing(it.search.toString())
            }.autoDispose()
        } else {
            followingViewModel.resetPaginationForFollowing(userId)
        }

        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        followingListAdapter = FollowingListAdapter(requireContext(), userId).apply {
            followingItemClicks.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is FollowClickStates.FollowClick -> {
                        val mPos = listOfFollowing.indexOfFirst { followUser ->
                            followUser.id == state.user.id
                        }
                        if (state.user.isPrivate == 1) {
                            listOfFollowing[mPos].isRequested = 1

                        } else {
                            if (mPos != -1) {
                                listOfFollowing[mPos].followStatus = 1
                            }
                        }
                        followingViewModel.followUnfollow(state.user.id)
                        followingListAdapter.listOfDataItems = listOfFollowing

                    }
                    is FollowClickStates.FollowingClick -> {
                        val mPos =
                            listOfFollowing.indexOfFirst { followUser ->
                                followUser.id == state.user.id
                            }

                        if (mPos != -1) {
                            if (loggedInUserCache.getLoggedInUserId() == userId) {
                                val tempList = listOfFollowing.toMutableList()
                                tempList.removeAt(mPos)
                                listOfFollowing = tempList
                                followingListAdapter.listOfDataItems = tempList
                            } else {
                                listOfFollowing[mPos].followStatus = 0
                                followingListAdapter.listOfDataItems = listOfFollowing
                            }
                            hideShowNoData(listOfFollowing)
                        }
                        followingViewModel.followUnfollow(state.user.id)
                    }
                    is FollowClickStates.ProfileClick -> {
                        if (state.user.userBlockedYou == 0) {
                            startActivity(
                                MyProfileActivity.getIntentWithData(
                                    requireActivity(),
                                    state.user.id
                                )
                            )
                        } else {
                            showToast("You were blocked")
                        }
                    }
                    else -> {

                    }
                }
            }
        }

        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL

        binding.rvFollowingList.apply {
            layoutManager = llm
            adapter = followingListAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                followingViewModel.loadMoreFollowing(userId,binding.etSearchFollowing.text.toString())
                            }
                        }
                    }

                }
            })
        }
        binding.etSearchFollowing.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .map { it.toString() } // Convert to String
            .subscribeAndObserveOnMainThread { search(it)}
    }

    private fun search(searchTerm: String) {
        followingViewModel.resetPaginationForFollowing(userId,searchTerm)
        followingListAdapter.notifyDataSetChanged()
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
                    followingListAdapter.listOfDataItems = it.followingData
                    hideShowNoData(listOfFollowing)
                    hideLoading()

                }
                is FollowingState.LoadingState -> {
                    if (followingListAdapter.listOfDataItems.isNullOrEmpty()) {
                       if(it.isLoading) showLoading() else hideLoading()
                    }
                }
                is FollowingState.SuccessMessage -> {

                }
                is FollowingState.EmptyState -> {
                    followingListAdapter.listOfDataItems = listOf()
                    hideShowNoData(listOf())
                    hideLoading()
                }
            }
        }.autoDispose()

    }

    private fun hideShowNoData(followingList: List<MeetFriendUser>) {
        if(arguments?.containsKey(INTENT_TREND) == true) {
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
        followingViewModel.resetPaginationForFollowing(userId)
        if (!binding.etSearchFollowing.text.isNullOrEmpty()) {
            binding.etSearchFollowing.setText("")
        }
    }
}


