package com.meetfriend.app.ui.follow


import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowClickStates
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentFollowersBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.follow.view.FollowersListAdapter
import com.meetfriend.app.ui.follow.viewmodel.FollowersState
import com.meetfriend.app.ui.follow.viewmodel.FollowersViewModel
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class FollowersFragment : BasicFragment() {

    companion object {
        private const val INTENT_USER_ID = "userId"

        fun newInstance(userId: Int): FollowersFragment {
            val args = Bundle()
            userId.let { args.putInt(INTENT_USER_ID, it) }
            val fragment = FollowersFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentFollowersBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<FollowersViewModel>
    private lateinit var followersViewModel: FollowersViewModel

    private lateinit var followersListAdapter: FollowersListAdapter
    private var userId = -1
    private var listOfFollowers: List<MeetFriendUser> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowersBinding.inflate(inflater, container, false)
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        followersViewModel = getViewModelFromFactory(viewModelFactory)

        userId = arguments?.getInt(INTENT_USER_ID) ?: throw IllegalStateException("No args provided")

        listenToViewEvent()
        listenToViewModel()

    }

    private fun listenToViewModel() {

        followersViewModel.followerStates.subscribeAndObserveOnMainThread {
            when (it) {
                is FollowersState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is FollowersState.FollowersData -> {
                    if (!it.followingData.isNullOrEmpty()) {
                        listOfFollowers = it.followingData
                    }
                    followersListAdapter.listOfDataItems = it.followingData
                   hideLoading()
                }
                is FollowersState.LoadingState -> {
                    if (followersListAdapter.listOfDataItems.isNullOrEmpty()) {
                        if(it.isLoading) showLoading() else hideLoading()
                    }
                }
                is FollowersState.SuccessMessage -> {

                }
                is FollowersState.EmptyState -> {
                    followersListAdapter.listOfDataItems = listOf()
                    if (followersListAdapter.listOfDataItems.isNullOrEmpty()) {
                        binding.llNoEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.llNoEmptyState.visibility = View.GONE
                    }
                    hideLoading()
                }
            }
        }.autoDispose()

    }

    private fun listenToViewEvent() {
        followersListAdapter = FollowersListAdapter(requireContext(), userId).apply {
            followersItemClicks.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is FollowClickStates.FollowClick -> {
                        val mPos = listOfFollowers.indexOfFirst { followUser ->
                            followUser.id == state.user.id
                        }
                        if (mPos != -1) {
                            if (state.user.isPrivate == 1) {
                                listOfFollowers[mPos].isRequested = 1

                            } else {
                                if (mPos != -1) {
                                    listOfFollowers[mPos].followStatus = 1
                                }
                            }
                        }
                        followersListAdapter.listOfDataItems = listOfFollowers
                        followersViewModel.followUnfollow(state.user.id)
                    }

                    is FollowClickStates.FollowingClick -> {
                        val mPos = listOfFollowers.indexOfFirst { followUser ->
                            followUser.id == state.user.id
                        }
                        if (mPos != -1) {
                            listOfFollowers[mPos].followStatus = 0
                            followersListAdapter.listOfDataItems = listOfFollowers
                        }
                        followersViewModel.followUnfollow(state.user.id)

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
                    is FollowClickStates.MoreClick -> {
                        val bottomSheet = BlockUserBottomSheet(state.user.id)
                        bottomSheet.show(
                            childFragmentManager,
                            BlockUserBottomSheet::class.java.name
                        )
                    }
                    else -> {}
                }
            }.autoDispose()
        }

        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL

        binding.rvFollowersList.apply {
            adapter = followersListAdapter
            layoutManager = llm

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                followersViewModel.loadMoreFollowers(userId,binding.etSearchLiveStreamingInviteUser.text.toString())
                            }
                        }
                    }
                }
            })
        }

        binding.etSearchLiveStreamingInviteUser.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .map { it.toString() } // Convert to String
            .subscribeAndObserveOnMainThread { search(it) }
    }

    private fun search(searchTerm: String) {
        followersViewModel.resetPaginationForFollowers(userId,searchTerm)
        followersListAdapter.notifyDataSetChanged()
    }
    override fun onResume() {
        super.onResume()
        if (!binding.etSearchLiveStreamingInviteUser.text.isNullOrEmpty()) {
            binding.etSearchLiveStreamingInviteUser.setText("")
        }
        followersViewModel.resetPaginationForFollowers(userId)
    }

}
