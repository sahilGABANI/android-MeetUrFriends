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
import com.meetfriend.app.api.hashtag.model.HashtagResponse
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentHashtagListBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.hashtag.HashTagListActivity
import com.meetfriend.app.ui.hashtag.viewmodel.HashTagViewModel
import com.meetfriend.app.ui.hashtag.viewmodel.HashTagsViewState
import com.meetfriend.app.ui.trends.view.HashtagListAdapter
import javax.inject.Inject

class HashtagListFragment : BasicFragment() {

    companion object {
        private const val INTENT_SEARCH = "searchText"
        private const val INTENT_IS_FROM = "isFrom"

        fun newInstance(search: String? = null, isFrom: Int = 1): HashtagListFragment {
            val args = Bundle()
            args.putString(INTENT_SEARCH, search)
            args.putInt(INTENT_IS_FROM, isFrom)

            val fragment = HashtagListFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentHashtagListBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HashTagViewModel>
    private lateinit var hashTagViewModel: HashTagViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var hashtagListAdapter: HashtagListAdapter
    private var userId = -1
    private var listOfFollowing: List<HashtagResponse> = listOf()
    private var isFrom = 0
    private var search = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHashtagListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        hashTagViewModel = getViewModelFromFactory(viewModelFactory)

        arguments?.let {
            isFrom = it.getInt(INTENT_IS_FROM)
            search = it.getString(INTENT_SEARCH) ?: ""
        }

        listenToViewModel()
        listenToViewEvent()

        RxBus.listen(RxEvent.SearchTextHashtags::class.java).subscribeAndObserveOnMainThread {
            if (isFrom == 1) {
                search = it.search
                if (loggedInUserCache.getLoginUserToken() != null) {
                    hashTagViewModel.resetPaginationForFollowing(it.search)
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        hashtagListAdapter = HashtagListAdapter(requireContext(), userId).apply {
            hashtagItemClicks.subscribeAndObserveOnMainThread { state ->
                startActivity(HashTagListActivity.getIntent(requireContext(), state.id, state.tagName ?: ""))
            }.autoDispose()
        }

        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL

        binding.rvFollowingList.apply {
            layoutManager = llm
            adapter = hashtagListAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                if (loggedInUserCache.getLoginUserToken() != null) hashTagViewModel.loadMoreFollowing()
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            if (loggedInUserCache.getLoginUserToken() != null) {
                hashTagViewModel.resetPaginationForFollowing("")
            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        hashTagViewModel.hashTagsState.subscribeAndObserveOnMainThread {
            when (it) {
                is HashTagsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is HashTagsViewState.HashtagList -> {
                    if (it.hashtagResponse.size > 0) {
                        if (isFrom == 1 && search == "") {
                            listOfFollowing = listOf()
                            hideShowNoData(listOfFollowing)
                        } else {
                            listOfFollowing = it.hashtagResponse
                            hashtagListAdapter.listOfDataItems = it.hashtagResponse
                            hideShowNoData(listOfFollowing)
                            hideLoading()
                        }
                    } else {
                        hideShowNoData(listOfFollowing)
                        hideLoading()
                    }
                }
                is HashTagsViewState.LoadingState -> {
                    if (hashtagListAdapter.listOfDataItems.isNullOrEmpty()) {
                        if (it.isLoading) showLoading() else hideLoading()
                        binding.llEmptyState.isVisible = !it.isLoading
                        binding.flData.isVisible = !it.isLoading
                    }
                }
                is HashTagsViewState.EmptyState -> {
                    hideShowNoData(listOfFollowing)
                    hideLoading()
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(followingList: List<HashtagResponse>) {
        if (followingList.isNotEmpty()) {
            binding.llEmptyState.visibility = View.GONE
            binding.rvFollowingList.visibility = View.VISIBLE
        } else {
            binding.llEmptyState.visibility = View.VISIBLE
            binding.rvFollowingList.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (loggedInUserCache.getLoginUserToken() != null) {
            hashTagViewModel.resetPaginationForFollowing("")
        }
    }
}
