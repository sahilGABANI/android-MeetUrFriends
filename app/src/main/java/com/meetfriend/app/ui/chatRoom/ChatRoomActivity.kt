package com.meetfriend.app.ui.chatRoom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.profile.model.ProfileItemInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityChatRoomBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.activities.LoginActivity
import com.meetfriend.app.ui.chatRoom.create.CreateRoomActivity
import com.meetfriend.app.ui.chatRoom.notification.ChatRoomNotificationActivity
import com.meetfriend.app.ui.chatRoom.view.ChatRoomAdapter
import com.meetfriend.app.ui.chatRoom.view.ChatRoomTabAdapter
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewState
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FiXED_10_INT
import com.meetfriend.app.utils.Constant.FiXED_1_INT
import com.meetfriend.app.utils.Constant.FiXED_2_INT
import com.meetfriend.app.utils.Constant.FiXED_3_INT
import com.meetfriend.app.utils.Constant.FiXED_4_INT
import com.meetfriend.app.utils.Constant.FiXED_5_INT
import com.meetfriend.app.utils.Constant.FiXED_9_INT
import com.meetfriend.app.utils.FileUtils
import org.json.JSONObject
import javax.inject.Inject

class ChatRoomActivity : BasicActivity() {

    companion object {
        const val USER_NAME = "USER_NAME"
        fun getIntent(context: Context, chatUserName: String): Intent {
            val intent = Intent(context, ChatRoomActivity::class.java)
            intent.putExtra(USER_NAME, chatUserName)

            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatRoomViewModel>
    private lateinit var chatRoomViewModel: ChatRoomViewModel

    private lateinit var binding: ActivityChatRoomBinding
    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private var notificationCount: Int? = null
    private var preSelected: Int = R.id.joinRoom
    private var select = false

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var chatRoomTabAdapter: ChatRoomTabAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
        binding = ActivityChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mp?.timeEvent(Constant.SCREEN_TIME)
        chatRoomViewModel = getViewModelFromFactory(viewModelFactory)
        FileUtils.loadBannerAd(this, binding.adView, loggedInUserCache.getBannerAdId())
        if (intent.getStringExtra(USER_NAME).isNullOrEmpty()) {
            openBottomSheet()
        } else {
            listenToViewModel()
            listenToViewEvent()
        }
    }

    private fun openBottomSheet() {
        val bottomSheet = ChatRoomUserBottomSheet()
        bottomSheet.isCancelable = false
        bottomSheet.userCreated.subscribeAndObserveOnMainThread {
            if (it) {
                if (!this::chatRoomTabAdapter.isInitialized) {
                    chatRoomTabAdapter = ChatRoomTabAdapter(this)
                }
                listenToViewModel()
                listenToViewEvent()
            }
        }.autoDispose()
        bottomSheet.show(supportFragmentManager, ChatRoomUserBottomSheet::class.java.name)
    }

    private fun listenToViewEvent() {
        chatRoomTabAdapter = ChatRoomTabAdapter(this)
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.ivNotification.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(ChatRoomNotificationActivity.getIntent(this))
        }.autoDispose()

        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = FiXED_5_INT.toInt()
        binding.viewPager.adapter = chatRoomTabAdapter
        val menuView = binding.chatRoomNavigationBar.getChildAt(0) as BottomNavigationMenuView
        val itemView = menuView.getChildAt(3) as BottomNavigationItemView
        val messageBadgeView: View = LayoutInflater.from(this).inflate(R.layout.message_badge_view, menuView, false)
        val textView = messageBadgeView.findViewById<AppCompatTextView>(R.id.counter_badge)
        val messageBadgeViewSelected: View = LayoutInflater.from(
            this
        ).inflate(R.layout.message_badge_select_view, menuView, false)
        val textViewSelected = messageBadgeViewSelected.findViewById<AppCompatTextView>(R.id.counter_badge)
        var unreadCount = 0
        RxBus.listen(RxEvent.UnreadMessageBadge::class.java).subscribeAndObserveOnMainThread {
            if (it.count != null && it.count != 0) {
                unreadCount = it.count
                if (it.count > FiXED_9_INT) {
                    textViewSelected.text = "9+"
                    textView.text = "9+"
                } else {
                    textViewSelected.text = it.count.toString()
                    textView.text = it.count.toString()
                }
                itemView.removeView(messageBadgeView)
                itemView.removeView(messageBadgeViewSelected)
                itemView.addView(messageBadgeView)
                itemView.addView(messageBadgeViewSelected)
                if (select) {
                    messageBadgeView.visibility = View.GONE
                    messageBadgeViewSelected.visibility = View.VISIBLE
                } else {
                    messageBadgeView.visibility = View.VISIBLE
                    messageBadgeViewSelected.visibility = View.GONE
                }
            } else {
                itemView.removeView(messageBadgeView)
                itemView.removeView(messageBadgeViewSelected)
            }
        }.autoDispose()
        setUpBottomNavigationClickListener(
            messageBadgeView,
            messageBadgeViewSelected,
            unreadCount
        )
    }

    private fun setUpBottomNavigationClickListener(
        messageBadgeView: View,
        messageBadgeViewSelected: View,
        unreadCount: Int
    ) {
        binding.chatRoomNavigationBar.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.joinRoom -> handleJoinRoomClick(messageBadgeView, messageBadgeViewSelected)
                R.id.myRoom -> handleMyRoomClick(messageBadgeView, messageBadgeViewSelected)
                R.id.createRoom -> handleCreateRoomClick()
                R.id.message -> handleMessageClick(messageBadgeView, messageBadgeViewSelected, unreadCount)
                R.id.profile -> handleProfileClick(messageBadgeView, messageBadgeViewSelected)
                else -> false
            }
        }
    }

    private fun handleJoinRoomClick(messageBadgeView: View, messageBadgeViewSelected: View): Boolean {
        binding.viewPager.setCurrentItem(0, false)
        binding.tvHeader.text = getString(R.string.label_chat_room)
        binding.flNotification.visibility = View.VISIBLE
        preSelected = R.id.joinRoom
        if (select) {
            messageBadgeView.visibility = View.VISIBLE
            messageBadgeViewSelected.visibility = View.GONE
        }
        select = false
        return true
    }

    private fun handleMyRoomClick(messageBadgeView: View, messageBadgeViewSelected: View): Boolean {
        binding.viewPager.setCurrentItem(FiXED_1_INT, false)
        binding.tvHeader.text = getString(R.string.label_my_room)
        binding.flNotification.visibility = View.VISIBLE
        if (select) {
            messageBadgeView.visibility = View.VISIBLE
            messageBadgeViewSelected.visibility = View.GONE
        }
        preSelected = R.id.myRoom
        select = false
        return true
    }

    private fun handleCreateRoomClick(): Boolean {
        binding.viewPager.setCurrentItem(FiXED_2_INT, false)
        binding.flNotification.visibility = View.VISIBLE
        startActivity(CreateRoomActivity.getIntent(this))
        return true
    }

    private fun handleMessageClick(
        messageBadgeView: View,
        messageBadgeViewSelected: View,
        unreadCount: Int
    ): Boolean {
        binding.viewPager.setCurrentItem(FiXED_3_INT, false)
        binding.tvHeader.text = getString(R.string.title_message)
        binding.flNotification.visibility = View.VISIBLE
        preSelected = R.id.message
        if (!select && unreadCount != 0) {
            messageBadgeView.visibility = View.GONE
            messageBadgeViewSelected.visibility = View.VISIBLE
        }
        select = true
        return true
    }

    private fun handleProfileClick(messageBadgeView: View, messageBadgeViewSelected: View): Boolean {
        binding.viewPager.setCurrentItem(FiXED_4_INT, false)
        binding.tvHeader.text = getString(R.string.label_profile)
        binding.flNotification.visibility = View.GONE
        preSelected = R.id.profile
        if (select) {
            messageBadgeView.visibility = View.VISIBLE
            messageBadgeViewSelected.visibility = View.GONE
        }
        select = false
        return true
    }

    private fun listenToViewModel() {
        chatRoomViewModel.chatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatRoomViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ChatRoomViewState.LoadingState -> {
                }
                is ChatRoomViewState.SuccessMessage -> {
                }
                is ChatRoomViewState.ListOfChatRoom -> {
                    chatRoomAdapter.listOfDataItems = it.listOfChatRoom
                }
                is ChatRoomViewState.GetProfileData -> {
                    if (it.profileData.result?.data != null && it.profileData.result.data.size > 0) {
                        loadProfileImage(it.profileData.result.data.first())
                    }
                }
                is ChatRoomViewState.GetNotificationCount -> {
                    notificationCount = it.notificationCount
                    if (notificationCount!! > 0) {
                        binding.tvBadgeCount.visibility = View.VISIBLE

                        if (notificationCount!! < FiXED_10_INT) {
                            binding.tvBadgeCount.text = "" + notificationCount
                        } else {
                            binding.tvBadgeCount.text = "9+"
                        }
                    } else {
                        binding.tvBadgeCount.visibility = View.GONE
                    }
                }
                else -> {
                }
            }

            loggedInUserCache.userAuthenticationFail.subscribeAndObserveOnMainThread {
                loggedInUserCache.clearUserPreferences()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }.autoDispose()
        }.autoDispose()
    }

    private fun loadProfileImage(profileData: ProfileItemInfo) {
        loggedInUserCache.setChatUserProfileImage(profileData.filePath)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == FileUtils.PICK_IMAGE) && (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                for (fragment in supportFragmentManager.fragments) {
                    fragment.onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        chatRoomViewModel.getChatRoomUser()
        chatRoomViewModel.getNotificationCount()

        if (binding.chatRoomNavigationBar.selectedItemId == R.id.createRoom) {
            binding.chatRoomNavigationBar.selectedItemId = preSelected
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val props = JSONObject()
        props.put(Constant.SCREEN_TYPE, "chatroom")

        mp?.track(Constant.SCREEN_TIME, props)
    }
}
