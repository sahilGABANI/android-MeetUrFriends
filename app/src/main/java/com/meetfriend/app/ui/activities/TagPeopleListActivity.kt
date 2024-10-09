package com.meetfriend.app.ui.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityTagPeopleListBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.follow.viewmodel.FollowingState
import com.meetfriend.app.ui.follow.viewmodel.FollowingViewModel
import com.meetfriend.app.ui.home.create.view.TagPeopleListAdapter
import java.util.HashMap
import javax.inject.Inject

class TagPeopleListActivity : BasicActivity() {

    companion object {
        const val INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP = "INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP"
        fun launchActivity(context: Context, taggedPeopleHashMap: HashMap<Int, String?>): Intent {
            val intent = Intent(context, TagPeopleListActivity::class.java)
            intent.putExtra(INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP, taggedPeopleHashMap)
            return intent
        }
    }

    private lateinit var listOfFollowing: List<MeetFriendUser>
    lateinit var binding: ActivityTagPeopleListBinding
    private lateinit var tagPeopleAdapter: TagPeopleListAdapter

    private var friendsList: ArrayList<MeetFriendUser>? = ArrayList()

    private var peopleForTagArrayList = ArrayList<MeetFriendUser>()

    private var taggedPeopleHashMap = HashMap<Int, String?>()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<FollowingViewModel>
    private lateinit var followingViewModel: FollowingViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagPeopleListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MeetFriendApplication.component.inject(this)
        followingViewModel = getViewModelFromFactory(viewModelFactory)
        followingViewModel.resetPaginationForFollowing(loggedInUserCache.getLoggedInUserId())
        loadDataFromIntent()
        listenToViewModel()
        listenToViewEvent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            if (it.hasExtra(INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP)) {
                val taggedPeopleHashMap = it.getSerializableExtra(INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP)
                if (taggedPeopleHashMap != null) {
                    this.taggedPeopleHashMap = taggedPeopleHashMap as HashMap<Int, String?>
                }
            }
        }
    }

    private fun listenToViewEvent() {
        peopleForTagArrayList = ArrayList()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        tagPeopleAdapter = TagPeopleListAdapter(this).apply {
            userClicks.subscribeAndObserveOnMainThread {
                if (it.isSelected) {
                    if (taggedPeopleHashMap.containsKey(it.id)) {
                        taggedPeopleHashMap.remove(it.id)
                    }
                } else {
                    if (!taggedPeopleHashMap.containsKey(it.id)) {
                        taggedPeopleHashMap[it.id] = it.userName
                    }
                }
                val mPos = peopleForTagArrayList.indexOf(it)
                if (mPos != -1) {
                    peopleForTagArrayList[mPos].isSelected = !it.isSelected
                }
                tagPeopleAdapter.notifyDataSetChanged()
                hideShowDoneButton()
            }.autoDispose()
        }
        binding.tagPeopleRV.apply {
            layoutManager = LinearLayoutManager(this@TagPeopleListActivity, LinearLayoutManager.VERTICAL, false)
            adapter = tagPeopleAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                followingViewModel.loadMoreFollowing(loggedInUserCache.getLoggedInUserId())
                            }
                        }
                    }
                }
            })
        }
        tagPeopleAdapter.listOfDataItems = friendsList

        binding.ivNext.throttleClicks().subscribeAndObserveOnMainThread {
            setResult(Activity.RESULT_OK, Intent().putExtra(INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP, taggedPeopleHashMap))
            finish()
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
                    val peopleForTagList = it.followingData
                    if (peopleForTagList != null) {
                        for (i in peopleForTagList.indices) {
                            if (taggedPeopleHashMap.containsKey(peopleForTagList[i].id)) {
                                peopleForTagList[i].isSelected = true
                            }
                        }
                    }
                    if (peopleForTagList != null) {
                        peopleForTagArrayList.addAll(peopleForTagList)
                    }
                    tagPeopleAdapter.listOfDataItems = it.followingData
                    if (peopleForTagArrayList.isNullOrEmpty()) {
                        binding.llNoData.visibility = View.VISIBLE
                    } else {
                        binding.llNoData.visibility = View.GONE
                    }

                    hideShowDoneButton()
                }
                is FollowingState.SuccessMessage -> {
                }
                else -> {
                }
            }
        }.autoDispose()
    }

    private fun hideShowDoneButton() {
        if (taggedPeopleHashMap.isEmpty()) {
            binding.ivNext.visibility = View.INVISIBLE
        } else {
            binding.ivNext.visibility = View.VISIBLE
        }
    }
}
