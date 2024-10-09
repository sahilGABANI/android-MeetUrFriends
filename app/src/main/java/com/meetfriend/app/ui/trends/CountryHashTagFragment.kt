package com.meetfriend.app.ui.trends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hbb20.CountryCodePicker
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.R
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
import com.meetfriend.app.ui.trends.view.HashtagListAdapter
import com.meetfriend.app.ui.trends.viewmodel.CountryHashTagViewModel
import com.meetfriend.app.ui.trends.viewmodel.CountryHashTagsViewState
import javax.inject.Inject

class CountryHashTagFragment : BasicFragment() {

    companion object {
        private const val INTENT_SEARCH = "searchText"

        fun newInstance(search: String? = null): CountryHashTagFragment {
            val args = Bundle()
            args.putString(INTENT_SEARCH, search)

            val fragment = CountryHashTagFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentHashtagListBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CountryHashTagViewModel>
    private lateinit var hashTagViewModel: CountryHashTagViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var hashtagListAdapter: HashtagListAdapter
    private var userId = -1
    private var listOfFollowing: List<HashtagResponse> = listOf()
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
            search = it.getString(INTENT_SEARCH) ?: ""
        }
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab_layout, null)
        val ccp: CountryCodePicker = customView.findViewById(R.id.ccp)
        ccp.setDetectCountryWithAreaCode(true)
        ccp.setAutoDetectedCountry(true)
        if (loggedInUserCache.getLoginUserToken() != null) {
            hashTagViewModel.resetPaginationForCountryHashtag(ccp.defaultCountryNameCode)
        }
        listenToViewModel()
        listenToViewEvent()
        RxBus.listen(RxEvent.SearchCountryHashtags::class.java).subscribeAndObserveOnMainThread {
            search = it.search
            if (loggedInUserCache.getLoginUserToken() != null) {
                hashTagViewModel.resetPaginationForCountryHashtag(search)
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
                                if (loggedInUserCache.getLoginUserToken() != null) {
                                    hashTagViewModel.loadMoreCountryHashtag(search)
                                }
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            if (loggedInUserCache.getLoginUserToken() != null) hashTagViewModel.resetPaginationForCountryHashtag(search)
        }.autoDispose()
    }

    private fun listenToViewModel() {
        hashTagViewModel.hashTagsState.subscribeAndObserveOnMainThread {
            when (it) {
                is CountryHashTagsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is CountryHashTagsViewState.HashtagList -> {
                    if (it.hashtagResponse.isNotEmpty()) {
                        listOfFollowing = it.hashtagResponse
                        hashtagListAdapter.listOfDataItems = it.hashtagResponse
                        hideShowNoData(listOfFollowing)
                        hideLoading()
                    } else {
                        hideShowNoData(listOfFollowing)
                        hideLoading()
                    }
                }

                is CountryHashTagsViewState.LoadingState -> {
                    if (hashtagListAdapter.listOfDataItems.isNullOrEmpty()) {
                        if (it.isLoading) {
                            showLoading()
                        } else {
                            hideLoading()
                        }
                        binding.llEmptyState.isVisible = !it.isLoading
                        binding.flData.isVisible = !it.isLoading
                    }
                }

                CountryHashTagsViewState.EmptyState -> {
                    hideShowNoData(listOfFollowing)
                    hideLoading()
                }

                else -> {

                }
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
}
