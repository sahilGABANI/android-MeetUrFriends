package com.meetfriend.app.ui.messages.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.chat.model.CreateOneToOneChatRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityCreateMessageBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chat.onetoonechatroom.ViewOneToOneChatRoomActivity
import com.meetfriend.app.ui.livestreaming.inviteuser.view.InviteUserInLiveStreamingAdapter
import com.meetfriend.app.ui.livestreaming.inviteuser.view.SelectedGroupAdapter
import com.meetfriend.app.ui.messages.viewmodel.CreateMessageState
import com.meetfriend.app.ui.messages.viewmodel.CreateMessageViewModel
import io.reactivex.schedulers.Schedulers
import java.util.HashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CreateMessageActivity : BasicActivity() {

    companion object {

        const val INTENT_EXTRA_WHO_CAN_WATCH_HASHMAP = "INTENT_EXTRA_WHO_CAN_WATCH_HASHMAP"
        fun getIntentWithData(
            context: Context,
            openFrom: String,
            totalCoins: String,
            taggedPeopleHashMap: HashMap<Int, Pair<String?, String?>> = hashMapOf()
        ): Intent {
            val intent = Intent(context, CreateMessageActivity::class.java)
            intent.putExtra("isOpenFrom", openFrom)
            intent.putExtra("TotalCoins", totalCoins)
            intent.putExtra(INTENT_EXTRA_WHO_CAN_WATCH_HASHMAP, taggedPeopleHashMap)
            return intent
        }

        fun getIntentWithDataS(context: Context, openFrom: String, totalCoins: String): Intent {
            val intent = Intent(context, CreateMessageActivity::class.java)
            intent.putExtra("isOpenFrom", openFrom)
            intent.putExtra("TotalCoins", totalCoins)
            return intent
        }
    }

    private var openFrom: String? = null
    private var totalCoins: String? = null
    lateinit var binding: ActivityCreateMessageBinding
    private var listOfFollowing: List<MeetFriendUser> = listOf()
    private var taggedPeopleList: ArrayList<MeetFriendUser> = arrayListOf()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateMessageViewModel>
    private lateinit var createMessageViewModel: CreateMessageViewModel
    private var listOfUser: List<MeetFriendUser> = listOf()
    private var listOfFollowUser: List<MeetFriendUser> = listOf()
    private var taggedPeopleHashMap = HashMap<Int, Pair<String?, String?>>()
    private var peopleForTagArrayList = ArrayList<MeetFriendUser>()
    private lateinit var inviteFriendsLiveStreamAdapter: InviteUserInLiveStreamingAdapter
    private lateinit var selectedGroupAdapter: SelectedGroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        createMessageViewModel = getViewModelFromFactory(viewModelFactory)
        openFrom = intent.getStringExtra("isOpenFrom")
        totalCoins = intent.getStringExtra("TotalCoins")
        loadDataFromIntent()
        listenToViewModel()
        listenToViewEvent()
        hideShowDoneButton()
        selectCleartextChange()
        createMessageViewModel.resetPaginationForInviteUser()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            if (it.hasExtra(INTENT_EXTRA_WHO_CAN_WATCH_HASHMAP)) {
                val taggedPeopleHashMap = it.getSerializableExtra(INTENT_EXTRA_WHO_CAN_WATCH_HASHMAP)
                if (taggedPeopleHashMap != null) {
                    this.taggedPeopleHashMap = taggedPeopleHashMap as HashMap<Int, Pair<String?, String?>>
                    taggedPeopleList = taggedPeopleHashMap.map {
                        MeetFriendUser(id = it.key, userName = it.value.first, profilePhoto = it.value.second)
                    } as ArrayList<MeetFriendUser>
                }
            }
        }
    }
    private fun listenToViewEvent() {
        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        if (openFrom == "CreateChat") {
            binding.ivNext.visibility = View.GONE
            binding.rlShowCount.visibility = View.GONE
            binding.tvHeader.text = resources.getText(R.string.label_create_chat)
        } else if (openFrom == "sendCoin") {
            binding.ivNext.visibility = View.GONE
            binding.rlShowCount.visibility = View.GONE
            binding.tvHeader.text = resources.getText(R.string.send_coin)
        } else {
            binding.ivNext.visibility = View.VISIBLE
            binding.rlShowCount.visibility = View.VISIBLE
            binding.tvHeader.text = resources.getText(R.string.label_who_can_watch)
        }
        binding.searchUserAppCompatEditText.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isNotEmpty()) {
                    createMessageViewModel.getUserForNormalChat(it.toString())
                } else {
                    if (listOfUser.isNotEmpty()) updateAdapter(listOfUser, true)
                }
            }.autoDispose()
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            createMessageViewModel.resetPaginationForInviteUser()
        }.autoDispose()

        binding.tvSelectedAll.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.tvSelectedAll.text == "Clear All") {
                binding.tvSelectedAll.text = "Select All"
                listOfFollowUser.forEach { it.isSelected = false }
                inviteFriendsLiveStreamAdapter.listOfDataItems = listOfFollowUser

                taggedPeopleHashMap.clear()
                taggedPeopleList = arrayListOf()
                selectedGroupAdapter.listOfUsers = taggedPeopleList
                binding.tvSelectedUSerCount.text = "${taggedPeopleHashMap.size} Person"
                inviteFriendsLiveStreamAdapter.notifyDataSetChanged()
                selectedGroupAdapter.notifyDataSetChanged()
            } else {
                taggedPeopleHashMap.clear()
                taggedPeopleList = arrayListOf()
                binding.tvSelectedAll.text = "Clear All"
                listOfFollowUser.forEach { it.isSelected = true }
                inviteFriendsLiveStreamAdapter.listOfDataItems = listOfFollowUser
                listOfFollowUser.forEach {
                    taggedPeopleHashMap[it.id] = Pair(it.userName, it.profilePhoto)
                    taggedPeopleList.add(it)
                }
                selectedGroupAdapter.listOfUsers = listOfFollowUser
                binding.tvSelectedUSerCount.text = "${taggedPeopleHashMap.size} Person"
                inviteFriendsLiveStreamAdapter.notifyDataSetChanged()
                selectedGroupAdapter.notifyDataSetChanged()
            }
            hideShowDoneButton()
            selectCleartextChange()
        }.autoDispose()

        selectedGroupAdapter = SelectedGroupAdapter(this@CreateMessageActivity).apply {
            removeItemClick.subscribeAndObserveOnMainThread { user ->
                taggedPeopleList.remove(user)
                selectedGroupAdapter.listOfUsers = taggedPeopleList
                taggedPeopleHashMap.remove(user.id)
                listOfFollowUser.find { it.id == user.id }?.apply {
                    isSelected = false
                }
                inviteFriendsLiveStreamAdapter.listOfDataItems = listOfFollowUser

                binding.tvSelectedUSerCount.text = "${taggedPeopleHashMap.size} Person"
                hideShowDoneButton()
                selectCleartextChange()
            }
        }
        binding.selectedRecyclerView.apply {
            adapter = selectedGroupAdapter
        }
    }

    private fun listenToViewModel() {
        createMessageViewModel.createChatState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is CreateMessageState.ErrorMessage -> {
                    showToast(state.errorMessage)
                }
                is CreateMessageState.LoadingState -> {
                    if (listOfFollowUser.isNullOrEmpty() && state.isLoading) showLoading() else hideLoading()
                }
                is CreateMessageState.SuccessMessage -> {
                    showToast(state.successMessage)
                }
                is CreateMessageState.FollowersData -> {
                    state.followingData?.let {
                        listOfUser = it
                        updateAdapter(it, false)
                    }
                    listOfFollowUser = state.followingData ?: listOf()

                    if (!state.followingData.isNullOrEmpty()) {
                        listOfFollowing = state.followingData
                    }
                    val peopleForTagList = state.followingData
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
                    inviteFriendsLiveStreamAdapter.listOfDataItems = state.followingData
                    hideShowDoneButton()
                    selectCleartextChange()
                }

                is CreateMessageState.OneToOneChatData -> {
                    startActivity(
                        ViewOneToOneChatRoomActivity.getIntentWithData(
                            this,
                            state.chatRoomInfo,
                            true,
                            canSendMessage = true
                        )
                    )
                }
                is CreateMessageState.UserListForNormalChat -> {
                    state.listOfUserForMention?.let {
                        updateAdapter(it, true)
                    }
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun updateAdapter(listOfFollowResponse: List<MeetFriendUser>, isFromSearch: Boolean) {
        // Initialize a map to store isSelected state from listOfFollowUser
        val followUserMap = listOfFollowUser.associateBy { it.id }

        inviteFriendsLiveStreamAdapter = InviteUserInLiveStreamingAdapter(
            this,
            true,
            0,
            openFrom ?: "",
            totalCoins ?: ""
        ).apply {
            createChatViewClicks.subscribeAndObserveOnMainThread {
                createMessageViewModel.createOneToOneChat(CreateOneToOneChatRequest(it.id))
            }.autoDispose()

            onBackViewClick.subscribeAndObserveOnMainThread {
                onBackPressedDispatcher.onBackPressed()
            }.autoDispose()

            userClicks.subscribeAndObserveOnMainThread { clickedUser ->
                val userId = clickedUser.id
                val isSelected = !clickedUser.isSelected // Toggle isSelected state

                // Update clickedUser's isSelected state
                clickedUser.isSelected = isSelected

                // Update taggedPeopleHashMap based on the clickedUser's selection status
                if (isSelected) {
                    taggedPeopleHashMap[userId] =
                        Pair(clickedUser.userName, clickedUser.profilePhoto)
                } else {
                    taggedPeopleHashMap.remove(userId)
                }

                // Update the corresponding user's isSelected state in listOfFollowUser
                followUserMap[userId]?.isSelected = isSelected

                // Update taggedPeopleList based on the updated taggedPeopleHashMap
                taggedPeopleList = ArrayList(
                    taggedPeopleHashMap.map {
                        MeetFriendUser(
                            id = it.key,
                            userName = it.value.first,
                            profilePhoto = it.value.second
                        )
                    }
                )

                // Notify adapters about the changes
                selectedGroupAdapter.listOfUsers = taggedPeopleList
                notifyDataSetChanged()

                // Update UI components
                binding.tvSelectedUSerCount.text = "${taggedPeopleHashMap.size} Person"
                hideShowDoneButton()
                selectCleartextChange()
            }.autoDispose()

            binding.tvSelectedUSerCount.text = "${taggedPeopleHashMap.size} Person"
            binding.ivNext.throttleClicks().subscribeAndObserveOnMainThread {
                setResult(Activity.RESULT_OK, Intent().putExtra(INTENT_EXTRA_WHO_CAN_WATCH_HASHMAP, taggedPeopleHashMap))
                finish()
            }.autoDispose()
        }

        // Initialize isSelected state for each user in listOfUser
        for (user in listOfFollowResponse) {
            val followUser = followUserMap[user.id]
            user.isSelected = followUser?.isSelected ?: false
        }

        inviteFriendsLiveStreamAdapter.listOfDataItems = listOfFollowResponse
        selectedGroupAdapter.listOfUsers = taggedPeopleList

        binding.rvUserList.apply {
            adapter = inviteFriendsLiveStreamAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                createMessageViewModel.loadMoreInviteUser()
                            }
                        }
                    }
                }
            })
        }
        if (listOfFollowResponse.isNotEmpty()) {
            binding.searchUserAppCompatEditText.isVisible = true
            binding.llEmptyState.visibility = View.GONE
        } else {
            binding.searchUserAppCompatEditText.isVisible = isFromSearch
            binding.llEmptyState.visibility = View.VISIBLE
        }
    }
    private fun hideShowDoneButton() {
        if (taggedPeopleHashMap.isEmpty()) {
            binding.ivNext.visibility = View.GONE
        } else {
            binding.ivNext.visibility = View.VISIBLE
        }
    }
    private fun selectCleartextChange() {
        if (listOfUser.size != taggedPeopleHashMap.size) {
            binding.tvSelectedAll.text = "Select All"
        } else {
            binding.tvSelectedAll.text = "Clear All"
        }
    }
}
