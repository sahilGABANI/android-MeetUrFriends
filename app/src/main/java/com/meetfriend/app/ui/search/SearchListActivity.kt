package com.meetfriend.app.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivitySearchListBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.livestreaming.inviteuser.view.InviteUserInLiveStreamingAdapter
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.ui.search.viewmodel.SearchListViewModel
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SearchListActivity : BasicActivity() {

    private lateinit var binding: ActivitySearchListBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SearchListViewModel>
    private lateinit var searchListViewModel: SearchListViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var inviteFriendsLiveStreamAdapter: InviteUserInLiveStreamingAdapter
    private var listOfSearchUser: ArrayList<MeetFriendUser> = arrayListOf()
    private var userId = -1

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, SearchListActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)

        binding = ActivitySearchListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchListViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewEvent()
        initUI()
        listenToViewModel()
    }

    private fun initUI() {

        userId = loggedInUserCache.getLoggedInUserId()

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.searchUserAppCompatEditText.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isNotEmpty()) {
                    searchListViewModel.resetPaginationForFollowers(it.toString())
                } else {
                    inviteFriendsLiveStreamAdapter.listOfDataItems = null
                    binding.noFoundAppCompatTextView.visibility = View.VISIBLE
                }
            }.autoDispose()
    }

    private fun listenToViewEvent() {
        inviteFriendsLiveStreamAdapter = InviteUserInLiveStreamingAdapter(
            this@SearchListActivity,
            true,
            0
        ,"CreateChat","").apply {
            createChatViewClicks.subscribeAndObserveOnMainThread {
                startActivity(MyProfileActivity.getIntentWithData(this@SearchListActivity, it.id))
            }.autoDispose()
        }

        val llm = LinearLayoutManager(this@SearchListActivity)
        llm.orientation = LinearLayoutManager.VERTICAL

        binding.recyclerViewSearchListing.apply {
            adapter = inviteFriendsLiveStreamAdapter
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
                                if (!binding.searchUserAppCompatEditText.text.isNullOrEmpty())
                                    searchListViewModel.loadMoreFollowers(binding.searchUserAppCompatEditText.text.toString())
                            }
                        }
                    }
                }
            })
        }

    }

    private fun listenToViewModel() {
        searchListViewModel.searchUserState.subscribeAndObserveOnMainThread {
            when (it) {
                is SearchListViewModel.SearchUserViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is SearchListViewModel.SearchUserViewState.LoadingState -> {
                }
                is SearchListViewModel.SearchUserViewState.SearchUserList -> {
                    showLoading(false)
                    if (it.searchUserList.size > 0) {
                        binding.noFoundAppCompatTextView.visibility = View.GONE
                        listOfSearchUser = (it.searchUserList)
                        inviteFriendsLiveStreamAdapter.listOfDataItems = it.searchUserList
                    } else {
                        listOfSearchUser = arrayListOf()
                        inviteFriendsLiveStreamAdapter.listOfDataItems = it.searchUserList
                        binding.noFoundAppCompatTextView.visibility = View.VISIBLE
                    }
                }
                else -> {

                }
            }
        }.autoDispose()
    }
}