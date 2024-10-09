package com.meetfriend.app.ui.follow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowClickStates
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentSuggestedBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.follow.view.SuggestedListAdapter
import com.meetfriend.app.ui.follow.viewmodel.SuggestedUserViewModel
import com.meetfriend.app.ui.follow.viewmodel.SuggestedUserViewState
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SuggestedFragment : BasicFragment() {

    companion object {

        @JvmStatic
        fun newInstance() = SuggestedFragment()
    }

    private var _binding: FragmentSuggestedBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SuggestedUserViewModel>
    private lateinit var suggestedUserViewModel: SuggestedUserViewModel

    private lateinit var suggestedListAdapter: SuggestedListAdapter
    private var listOfSuggestedUser: List<MeetFriendUser> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuggestedBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        suggestedUserViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        suggestedListAdapter = SuggestedListAdapter(requireContext()).apply {
            suggestedItemClicks.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is FollowClickStates.FollowClick -> {
                        val mPos = listOfSuggestedUser.indexOfFirst { followUser ->
                            followUser.id == state.user.id
                        }
                        if (mPos != -1) {
                            if (state.user.isPrivate == 1) {
                                listOfSuggestedUser[mPos].isRequested = 1

                            } else {
                                if (mPos != -1) {
                                    listOfSuggestedUser[mPos].followStatus = 1
                                }
                            }
                        }
                        suggestedUserViewModel.followUnfollow(state.user.id)
                        suggestedListAdapter.listOfDataItems = listOfSuggestedUser

                    }

                    is FollowClickStates.FollowingClick -> {
                        val mPos = listOfSuggestedUser.indexOfFirst { followUser ->
                            followUser.id == state.user.id
                        }
                        if (mPos != -1) {
                            listOfSuggestedUser[mPos].followStatus = 0
                            suggestedListAdapter.listOfDataItems = listOfSuggestedUser
                        }
                        suggestedUserViewModel.followUnfollow(state.user.id)
                    }
                    is FollowClickStates.ProfileClick -> {
                        startActivity(
                            MyProfileActivity.getIntentWithData(
                                requireContext(),
                                state.user.id
                            )
                        )

                    }
                    is FollowClickStates.CancelClick -> {
                        suggestedUserViewModel.cancelFriendRequest(state.user.id)
                        val list = suggestedListAdapter.listOfDataItems as ArrayList
                        list.remove(state.user)
                        if (isLastItemVisible()) suggestedUserViewModel.loadMoreSuggestedUser(binding.searchUserAppCompatEditText.text.toString())
                        suggestedListAdapter.listOfDataItems = list
                    }
                    else -> {}

                }
            }.autoDispose()
        }

        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL

        binding.rvSuggestedList.apply {
            adapter = suggestedListAdapter
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
                                suggestedUserViewModel.loadMoreSuggestedUser(binding.searchUserAppCompatEditText.text.toString())
                            }
                        }
                    }
                }
            })
        }

        binding.searchUserAppCompatEditText.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isNotEmpty()) {
                    suggestedUserViewModel.resetPaginationForSuggestedUser(it.toString())
                } else {
                    suggestedUserViewModel.resetPaginationForSuggestedUser("")
                }
            }.autoDispose()
    }

    private fun listenToViewModel() {
        suggestedUserViewModel.suggestedUserStates.subscribeAndObserveOnMainThread {
            when (it) {
                is SuggestedUserViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is SuggestedUserViewState.SuggestedUserData -> {
                    hideLoading()
                    if (!it.suggestedUserData.isNullOrEmpty()) {
                        binding.llEmptyState.visibility = View.GONE
                        listOfSuggestedUser = it.suggestedUserData
                    }
                    else{
                        binding.llEmptyState.visibility = View.VISIBLE
                    }
                    suggestedListAdapter.listOfDataItems = it.suggestedUserData
                }
                is SuggestedUserViewState.LoadingState -> {
                    if (suggestedListAdapter.listOfDataItems.isNullOrEmpty()) {
                        if(it.isLoading) showLoading() else hideLoading()
                    }
                }
                is SuggestedUserViewState.SuccessMessage -> {

                }
                is SuggestedUserViewState.EmptyState -> {
                   hideLoading()
                    suggestedListAdapter.listOfDataItems = listOf()
                    if (suggestedListAdapter.listOfDataItems.isNullOrEmpty()) {
                        binding.llEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.llEmptyState.visibility = View.GONE
                    }
                }
            }
        }.autoDispose()

    }

    private fun isLastItemVisible(): Boolean {
        val layoutManager = binding.rvSuggestedList.layoutManager as LinearLayoutManager
        val position = layoutManager.findLastVisibleItemPosition()
        return position >= suggestedListAdapter.itemCount - 1
    }

    override fun onResume() {
        super.onResume()
        if (!binding.searchUserAppCompatEditText.text.isNullOrEmpty()) {
            binding.searchUserAppCompatEditText.setText("")
        }
        suggestedUserViewModel.resetPaginationForSuggestedUser("")
    }


}