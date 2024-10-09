package com.meetfriend.app.ui.chatRoom.roomview

//import io.agora.rtc2.*
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.chat.model.*
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityViewChatRoomBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.newbase.view.SquareImageView
import com.meetfriend.app.socket.SocketDataManager
import com.meetfriend.app.ui.chatRoom.ChatRoomInfoActivity
import com.meetfriend.app.ui.chatRoom.create.UpdateSubscriptionBottomSheet
import com.meetfriend.app.ui.chatRoom.roomview.view.ChatRoomMessageAdapter
import com.meetfriend.app.ui.chatRoom.roomview.view.ChatRoomViewProfileImageAdapter
import com.meetfriend.app.ui.chatRoom.roomview.view.MentionUserAdapter
import com.meetfriend.app.ui.chatRoom.roomview.viewmodel.ChatRoomMessageViewModel
import com.meetfriend.app.ui.chatRoom.roomview.viewmodel.ChatRoomMessageViewState
import com.meetfriend.app.ui.chatRoom.videoCall.OneToOneVideoCallActivity
import com.meetfriend.app.utils.Constant
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.RtcEngineConfig
import io.agora.rtc.models.ChannelMediaOptions
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class ViewChatRoomActivity : BasicActivity() {

    companion object {
        private const val INTENT_CHAT_ROOM_INFO = "INTENT_CHAT_ROOM_INFO"

        fun getIntent(context: Context, chatRoomInfo: ChatRoomInfo): Intent {
            val intent = Intent(context, ViewChatRoomActivity::class.java)
            intent.putExtra(INTENT_CHAT_ROOM_INFO, chatRoomInfo)
            return intent
        }
    }

    private lateinit var binding: ActivityViewChatRoomBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatRoomMessageViewModel>
    private lateinit var chatRoomMessageViewModel: ChatRoomMessageViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    lateinit var socketDataManager: SocketDataManager

    private lateinit var chatRoomMessageAdapter: ChatRoomMessageAdapter
    private lateinit var chatRoomViewProfileImageAdapter: ChatRoomViewProfileImageAdapter
    private lateinit var chatRoomInfo: ChatRoomInfo
    private var messageList: MutableList<MessageInfo> = mutableListOf()
    private var perPage = 20
    private var agoraEngine: RtcEngine? = null
    private var handler: Handler? = null
    private var joinChatRoomResponse: JoinChatRoomResponse? = null
    private var listOfAdmin: MutableList<ChatRoomUser> = mutableListOf()
    private lateinit var mentionUserAdapter: MentionUserAdapter
    private var initialListOfMentionUser: List<MeetFriendUser> = listOf()
    private var voiceCallStarterUserId: Int = 0
    private var sendMessageCount: Int = 0
    private var staySafeMessage: Int = 0
    private var isCallStart: Boolean = false
    var tokenRoleBroadcaster = Constants.CLIENT_ROLE_BROADCASTER
    var tokenRoleAudience = Constants.CLIENT_ROLE_AUDIENCE
    private lateinit var warningMessageInfo: MessageInfo
    private var listOfExpiredAdmin: MutableList<ChatRoomUser> = mutableListOf()
    private var listOfAdminId: ArrayList<Int> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        chatRoomMessageViewModel = getViewModelFromFactory(viewModelFactory)

        loadDataFromIntent()

        chatRoomMessageViewModel.setCurrentChatInfo(chatRoomInfo)
        handler = Handler(Looper.getMainLooper())

        if (chatRoomInfo.roomType == 0) {
            chatRoomMessageViewModel.getChatRoomAdmin(GetChatRoomAdminRequest(chatRoomInfo.id))
        }

        checkPermissionGranted(this)
        listenToViewModel()
        listenToViewEvent()
        chatRoomMessageViewModel.observeMicAccessRequest()
        chatRoomMessageViewModel.observeAcceptMicAccessRequest()
        chatRoomMessageViewModel.observeRevokeMicAccess()
        warningMessageInfo =
            MessageInfo(messageType = MessageType.JoinF, id = 0, conversationId = chatRoomInfo.id)
        messageList.add(0, warningMessageInfo)

        val request = SendPrivateMessageRequest(
            conversationId = chatRoomInfo.id,
            senderId = loggedInUserCache.getLoggedInUserId(),
            message = loggedInUserCache.getChatUser()?.chatUserName.plus(" ").plus("Join Room"),
            messageType = Constant.MESSAGE_TYPE_JOIN,
            senderName = loggedInUserCache.getChatUser()?.chatUserName ?: "",
            conversationName = chatRoomInfo.roomName,
            senderProfile = loggedInUserCache.getChatUser()?.chatUserProfile,
            age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
            gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender
        )
        chatRoomMessageViewModel.sendPrivateMessage(request)

    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.RECORD_AUDIO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        setupVoiceSDKEngine()
                    } else {
                        checkPermissionGranted(this@ViewChatRoomActivity)
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast("Authorization is permanently denied, please manually grant permissions")
                    XXPermissions.startPermissionActivity(this@ViewChatRoomActivity, permissions)
                }
            })
    }

    private fun loadDataFromIntent() {
        this.chatRoomInfo = intent?.getParcelableExtra(INTENT_CHAT_ROOM_INFO) ?: return
        if (chatRoomInfo.roomType == 2) {
            if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                binding.tvChatRoomName.text = chatRoomInfo.user?.firstName

            } else {
                binding.tvChatRoomName.text = chatRoomInfo.receiver?.firstName
            }
        } else {
            binding.tvChatRoomName.text = chatRoomInfo.roomName
        }

        binding.ivCall.visibility =
            if (chatRoomInfo.roomType != 2 && chatRoomInfo.isAdmin) View.VISIBLE else View.GONE

        binding.llAdminProfileImageContainer.isVisible =
            chatRoomInfo.roomType == 0 && !chatRoomInfo.isAdmin
        binding.view.isVisible = chatRoomInfo.roomType == 0 && !chatRoomInfo.isAdmin
        binding.tvAdmin.isVisible = chatRoomInfo.roomType == 0 && chatRoomInfo.isAdmin

        binding.ivVideoCall.isVisible = chatRoomInfo.roomType == 2

    }

    private fun listenToViewEvent() {

        binding.ivVideoCall.throttleClicks().subscribeAndObserveOnMainThread {

            startActivity(OneToOneVideoCallActivity.getIntent(this, chatRoomInfo, true))
        }.autoDispose()

        binding.micOffAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            chatRoomMessageViewModel.sendMicAccessRequest(
                SendMicAccessRequestSocketRequest(
                    conversationId = chatRoomInfo.id,
                    userId = voiceCallStarterUserId,
                    senderId = loggedInUserCache.getLoggedInUserId(),
                    senderName = loggedInUserCache.getChatUser()?.chatUserName,
                    conversationName = chatRoomInfo.roomName,
                    senderProfile = loggedInUserCache.getChatUser()?.chatUserProfile
                )
            )

        }.autoDispose()

        binding.micAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            chatRoomMessageViewModel.revokeMicAccess(
                SendMicAccessRequestSocketRequest(
                    conversationId = chatRoomInfo.id,
                    userId = loggedInUserCache.getLoggedInUserId()!!
                )
            )
        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        chatRoomMessageAdapter = ChatRoomMessageAdapter(this).apply {
            messageItemClicks.subscribeAndObserveOnMainThread { messageInfo ->
                openViewUserBottomSheet(messageInfo)

            }.autoDispose()
        }

        val llm = LinearLayoutManager(this@ViewChatRoomActivity, RecyclerView.VERTICAL, true)
        llm.stackFromEnd = true

        binding.rvMessage.apply {
            layoutManager = llm
            adapter = chatRoomMessageAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy < 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount && pastVisibleItems >= 0) {
                                chatRoomMessageViewModel.loadMore(
                                    GetChatMessageRequest(
                                        conversationId = chatRoomInfo.id,
                                        perPage = perPage
                                    )
                                )
                            }
                        }


                    }
                }
            })


            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->

                if (bottom < oldBottom) {

                    binding.rvMessage.postDelayed({
                        binding.rvMessage.smoothScrollToPosition(
                            0
                        )

                    }, 100)
                }
            }
        }



        chatRoomViewProfileImageAdapter = ChatRoomViewProfileImageAdapter(this)

        binding.rvProfileImage.adapter = chatRoomViewProfileImageAdapter

        binding.etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Timber.tag(ViewChatRoomActivity::class.java.name).e("Focus")
                binding.rvMessage.smoothScrollToPosition(0)
            }
        }
        if (chatRoomInfo.roomType == 1) {
            if (chatRoomInfo.isjoined?.isRestrict == 1) {
                binding.ivAdd.visibility = View.GONE
                binding.sendMessageRelativeLayout.visibility = View.GONE
                binding.tvRestrictMessage.visibility = View.VISIBLE
                hideKeyboard()
            }
        } else {
            if (chatRoomInfo.isRestrict == 1) {
                binding.ivAdd.visibility = View.GONE
                binding.sendMessageRelativeLayout.visibility = View.GONE
                binding.tvRestrictMessage.visibility = View.VISIBLE
                hideKeyboard()
            }
        }



        binding.ivSend.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.etMessage.text.toString().isEmpty()) {
                showToast(resources.getString(R.string.error_please_enter_message))
            } else {
                val senderProfile = loggedInUserCache.getChatUserProfileImage()

                val message = binding.etMessage.text.toString()
                val request = SendPrivateMessageRequest(
                    conversationId = chatRoomInfo.id,
                    senderId = loggedInUserCache.getLoggedInUserId(),
                    message = message,
                    messageType = Constant.MESSAGE_TYPE_TEXT,
                    senderName = loggedInUserCache.getChatUser()?.chatUserName ?: "",
                    conversationName = chatRoomInfo.roomName,
                    senderProfile = senderProfile,
                    age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
                    gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender
                )
                chatRoomMessageViewModel.sendPrivateMessage(request)
                binding.etMessage.setText("")
                binding.rvMessage.smoothScrollToPosition(0)
            }
        }

        binding.ivCall.throttleClicks().subscribeAndObserveOnMainThread {
            binding.ivCall.isVisible = false
            binding.progressBarVoiceCall.visibility = View.VISIBLE
            chatRoomMessageViewModel.loadAgoraToken(tokenRoleBroadcaster)
        }.autoDispose()

        binding.ivCutCall.throttleClicks().subscribeAndObserveOnMainThread {
            agoraEngine?.leaveChannel()
        }.autoDispose()

        binding.ivMic.throttleClicks().subscribeAndObserveOnMainThread {
            binding.ivMicOff.isVisible = true
            binding.ivMic.isVisible = false
            if (chatRoomInfo.isAdmin) {
                agoraEngine?.muteLocalAudioStream(true)
            } else {
                agoraEngine?.muteAllRemoteAudioStreams(true)
            }
        }.autoDispose()

        binding.ivMicOff.throttleClicks().subscribeAndObserveOnMainThread {
            binding.ivMicOff.isVisible = false
            binding.ivMic.isVisible = true
            if (chatRoomInfo.isAdmin) {
                agoraEngine?.muteLocalAudioStream(false)
            } else {
                agoraEngine?.muteAllRemoteAudioStreams(false)
            }
        }.autoDispose()

        binding.ivSpeaker.throttleClicks().subscribeAndObserveOnMainThread {
            binding.ivSpeakerOff.isVisible = true
            binding.ivSpeaker.isVisible = false
            agoraEngine?.muteAllRemoteAudioStreams(true)

        }.autoDispose()

        binding.ivSpeakerOff.throttleClicks().subscribeAndObserveOnMainThread {
            binding.ivSpeakerOff.isVisible = false
            binding.ivSpeaker.isVisible = true
            agoraEngine?.muteAllRemoteAudioStreams(false)

        }.autoDispose()


        binding.ivMore.throttleClicks().subscribeAndObserveOnMainThread {
            if (chatRoomInfo.roomType != 2) {
                staySafeMessage = 0
                if (isCallStart && voiceCallStarterUserId == loggedInUserCache.getLoggedInUserId()) {
                    openMoreOptionBottomSheet()
                } else {
                    startActivity(ChatRoomInfoActivity.getIntent(this, chatRoomInfo))
                }
            }
        }.autoDispose()

        mentionUserAdapter = MentionUserAdapter(this).apply {
            userClicks.subscribeAndObserveOnMainThread { mentionUser ->
                val cursorPosition: Int = binding.etMessage.selectionStart
                val descriptionString = binding.etMessage.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                chatRoomMessageViewModel.searchUserClicked(
                    binding.etMessage.text.toString(),
                    subString,
                    mentionUser
                )
            }.autoDispose()
        }

        binding.rvMentionUserList.apply {
            layoutManager = LinearLayoutManager(this@ViewChatRoomActivity)
            adapter = mentionUserAdapter
        }

        binding.etMessage.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isEmpty()) {
                    binding.llMentionUserListContainer.visibility = View.GONE
                } else {
                    val lastChar = it.last().toString()
                    if (lastChar.contains("@")) {
                        mentionUserAdapter.listOfDataItems = initialListOfMentionUser
                        binding.llMentionUserListContainer.visibility = View.VISIBLE
                    } else {
                        val wordList = it.split(" ")
                        val lastWord = wordList.last()
                        val search: String = lastWord.substringAfterLast("@")
                        if (lastWord.contains("@")) {
                            chatRoomMessageViewModel.getUserForMention(
                                chatRoomInfo.id,
                                search
                            )
                        } else {
                            binding.llMentionUserListContainer.visibility = View.GONE
                        }
                    }
                }
            }.autoDispose()

        chatRoomMessageViewModel.getInitialUserForMention(chatRoomInfo.id)

    }

    private fun openSubscriptionBottomSheet() {
        val subscriptionBottomSheet = UpdateSubscriptionBottomSheet.newInstance(chatRoomInfo)
        subscriptionBottomSheet.show(
            supportFragmentManager,
            UpdateSubscriptionBottomSheet::class.java.name
        )
    }

    private fun openMoreOptionBottomSheet() {
        val viewChatRoomMoreOptionBottomSheet =
            ViewChatRoomMoreOptionBottomSheet.newInstance(chatRoomInfo)
        viewChatRoomMoreOptionBottomSheet.moreOptionClicks.subscribeAndObserveOnMainThread {
            openViewMicAccessBottomSheet(it)
        }.autoDispose()
        viewChatRoomMoreOptionBottomSheet.show(
            supportFragmentManager,
            ViewChatRoomMoreOptionBottomSheet::class.java.name
        )
    }

    private fun openViewMicAccessBottomSheet(chatRoomInfo: ChatRoomInfo) {
        val userListBottomSheet = ViewMicAccessUserBottomSheet.newInstance(chatRoomInfo)
        userListBottomSheet.removeClicks.subscribeAndObserveOnMainThread {
            chatRoomMessageViewModel.revokeMicAccess(
                SendMicAccessRequestSocketRequest(
                    userId = it.userId,
                    conversationId = it.conversationId
                )
            )
            userListBottomSheet.dismissAllowingStateLoss()
        }.autoDispose()
        userListBottomSheet.show(
            supportFragmentManager,
            ViewMicAccessUserBottomSheet::class.java.name
        )
    }

    private fun listenToViewModel() {
        chatRoomMessageViewModel.chatRoomMessageState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatRoomMessageViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ChatRoomMessageViewState.LoadChatMessageList -> {
                    messageList.addAll(it.listOfMessage)
                    if (chatRoomInfo.roomType == 0) {
                        if (!messageList.contains(warningMessageInfo) && sendMessageCount == 0) {
                            messageList.add(0, warningMessageInfo)
                            staySafeMessage++
                        } else {
                            if (staySafeMessage == 0) {
                                // Ensure sendMessageCount is within bounds
                                if (sendMessageCount <= messageList.size) {
                                    messageList.add(sendMessageCount, warningMessageInfo)
                                    staySafeMessage++
                                } else {
                                    // Handle the case where sendMessageCount is greater than the list size
                                    // You might want to append the warningMessageInfo at the end or handle it based on your logic
                                    messageList.add(warningMessageInfo)
                                    staySafeMessage++
                                }
                            }
                        }
                    }
                    chatRoomMessageAdapter.listOfDataItems = messageList
                    chatRoomMessageAdapter.notifyItemChanged(0)
                    if (chatRoomMessageViewModel.pageNo == 1) {
                        binding.rvMessage.smoothScrollToPosition(0)
                    }
                }
                is ChatRoomMessageViewState.LoadingState -> {

                }
                is ChatRoomMessageViewState.SuccessMessage -> {

                }
                is ChatRoomMessageViewState.GetNewSendMessage -> {
                    sendMessageCount++

                    if (it.chatMessage.senderId == loggedInUserCache.getLoggedInUserId() && it.chatMessage.messageType == MessageType.Join) {

                    } else {
                        if (it.chatMessage.conversationId == chatRoomInfo.id) {
                            messageList.add(0, it.chatMessage)
                            chatRoomMessageAdapter.listOfDataItems = messageList
                            chatRoomMessageAdapter.notifyItemChanged(0)
                            binding.rvMessage.smoothScrollToPosition(0)
                        }
                    }

                }
                is ChatRoomMessageViewState.KickedOutData -> {
                    if (it.chatMessage.conversationId == chatRoomInfo.id) {
                      onBackPressedDispatcher.onBackPressed()
                    }
                }
                is ChatRoomMessageViewState.BannedUserData -> {
                    if (it.chatMessage.conversationId == chatRoomInfo.id) {
                      onBackPressedDispatcher.onBackPressed()
                    }
                }
                is ChatRoomMessageViewState.RestrictUserData -> {
                    if (it.chatMessage.conversationId == chatRoomInfo.id) {
                        binding.ivAdd.visibility = View.GONE
                        binding.sendMessageRelativeLayout.visibility = View.GONE
                        binding.tvRestrictMessage.visibility = View.VISIBLE
                        hideKeyboard()
                    }
                }
                is ChatRoomMessageViewState.AgoraSDKVoiceCallChannel -> {
                    joinChannel(it.joinAgoraSDKVoiceCallChannel)
                }
                is ChatRoomMessageViewState.JoinRoomResponse -> {
                    joinChatRoomResponse = it.joinChatRoomResponse
                    it.joinChatRoomResponse.callUserId?.let { callUserId ->
                        voiceCallStarterUserId = callUserId

                    }
                    voiceCallUpdatedVisibility(!it.joinChatRoomResponse.callToken.isNullOrEmpty())
                    if (chatRoomInfo.roomType == 0) {
                        if (!joinChatRoomResponse?.callToken.isNullOrEmpty()) {
                            if (chatRoomInfo.isAdmin) {
                                if (voiceCallStarterUserId != loggedInUserCache.getLoggedInUserId()) {
                                    chatRoomMessageViewModel.loadAgoraToken(tokenRoleAudience)
                                }
                            } else {
                                chatRoomMessageViewModel.loadAgoraToken(tokenRoleAudience)

                            }
                        }
                    } else {
                        if (!chatRoomInfo.isAdmin && !joinChatRoomResponse?.callToken.isNullOrEmpty()) {
                            chatRoomMessageViewModel.loadAgoraToken(tokenRoleAudience)
                        }
                    }


                }
                is ChatRoomMessageViewState.EndVoiceCall -> {
                    voiceCallUpdatedVisibility(false)
                }
                is ChatRoomMessageViewState.ChatRoomAdminData -> {
                    it.listOfAdmin?.let { it1 ->
                        listOfAdmin = it1 as MutableList<ChatRoomUser>
                    }

                    listOfAdmin.forEach { it ->
                        checkExpiration(it)
                    }
                    listOfAdmin.removeAll(listOfExpiredAdmin)
                    listOfAdmin.forEach {
                        it.userId?.let { it1 -> listOfAdminId.add(it1) }
                    }
                    manageAdminProfileImageClick()
                }
                is ChatRoomMessageViewState.UserListForMention -> {
                    mentionUserViewVisibility(!it.listOfUserForMention.isNullOrEmpty())
                    mentionUserAdapter.listOfDataItems = it.listOfUserForMention
                }
                is ChatRoomMessageViewState.UpdateDescriptionText -> {
                    mentionUserViewVisibility(false)
                    binding.etMessage.setText(it.descriptionString)
                    binding.etMessage.setSelection(binding.etMessage.text.toString().length)
                }
                is ChatRoomMessageViewState.InitialUserListForMention -> {
                    initialListOfMentionUser = it.listOfUserForMention
                }
                is ChatRoomMessageViewState.JoinVoiceCallResponse -> {
                    binding.progressBarVoiceCall.visibility = View.VISIBLE
                    voiceCallStarterUserId = it.joinVoiceCallData.userId
                    voiceCallUpdatedVisibility(true)
                    if (chatRoomInfo.roomType == 0) {
                        if (chatRoomInfo.isAdmin) {
                            if (voiceCallStarterUserId != loggedInUserCache.getLoggedInUserId()) {
                                chatRoomMessageViewModel.loadAgoraToken(tokenRoleAudience)
                            }
                        } else {
                            chatRoomMessageViewModel.loadAgoraToken(tokenRoleAudience)

                        }

                    } else {
                        if (!chatRoomInfo.isAdmin) {
                            chatRoomMessageViewModel.loadAgoraToken(tokenRoleAudience)
                        }
                    }

                }
                is ChatRoomMessageViewState.ReceivedMicAccessRequest -> {
                    val micAccessRequestDialogFragment =
                        MicAccessRequestDialogFragment.newInstance(it.micAccessRequestData)
                    micAccessRequestDialogFragment.sendMicAccessState.subscribeAndObserveOnMainThread {
                        micAccessRequestDialogFragment.dismiss()
                        chatRoomMessageViewModel.acceptMicAccessRequest(it)
                    }
                    micAccessRequestDialogFragment.show(
                        supportFragmentManager,
                        "MicAccessRequestDialogFragment"
                    )
                }
                is ChatRoomMessageViewState.AcceptedMicAccessRequest -> {
                    agoraEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
                    binding.micAppCompatImageView.visibility = View.VISIBLE
                    binding.micOffAppCompatImageView.visibility = View.GONE
                    if (loggedInUserCache.getLoggedInUserId() != it.micAccessRequestData.userId) {
                        showToast(getString(R.string.toast_mic_access_request_accepted))
                    }
                }
                is ChatRoomMessageViewState.SendRequest -> {
                    showToast(getString(R.string.toast_sent_mic_access))
                }
                is ChatRoomMessageViewState.RevokeMicAccess -> {
                    agoraEngine?.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
                    binding.micAppCompatImageView.visibility = View.GONE
                    binding.micOffAppCompatImageView.visibility = View.VISIBLE
                    showToast(getString(R.string.toast_mic_access_is_revoke))
                }
                is ChatRoomMessageViewState.LoadChatMessageEmptyList -> {
                    it.listOfMessage?.let { it1 -> messageList.addAll(it1) }
                    if (chatRoomInfo.roomType == 0) {
                        if (!messageList.contains(warningMessageInfo) && sendMessageCount == 0) {
                            messageList.add(0, warningMessageInfo)
                            staySafeMessage++
                        }
                    }
                    chatRoomMessageAdapter.listOfDataItems = messageList
                    chatRoomMessageAdapter.notifyItemChanged(0)
                }
                is ChatRoomMessageViewState.JoinRoomSuccess -> {

                }
                else -> {}
            }

        }.autoDispose()
    }

    private fun checkExpiration(chatRoomUser: ChatRoomUser) {
        val c = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val getCurrentDateTime = sdf.format(c.time)
        val getMyTime = chatRoomUser.expireDate.toString()

        if (getMyTime.compareTo(getCurrentDateTime) < 0) {
            listOfExpiredAdmin.add(chatRoomUser)
        }
    }

    private fun setupVoiceSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = getString(R.string.new_agora_app_id)
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            voiceCallUpdatedVisibility(true)
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            voiceCallUpdatedVisibility(true)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            //Admin cut call. Need to leave channel and hide call ui
            //voiceCallUpdatedVisibility(false)
            handler = null
        }

        override fun onLeaveChannel(stats: RtcStats) {
            //Admin leave call. Need to call end call socket event and hide call ui
            voiceCallUpdatedVisibility(false)

            handler = null
            if (chatRoomInfo.roomType == 0) {
                if (chatRoomInfo.isAdmin) {
                    if (voiceCallStarterUserId == loggedInUserCache.getLoggedInUserId())
                        chatRoomMessageViewModel.updateVoiceCallEnded()

                }
            } else {
                if (chatRoomInfo.isAdmin)
                    chatRoomMessageViewModel.updateVoiceCallEnded()
            }
            isCallStart = false

        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            super.onTokenPrivilegeWillExpire(token)
            if (chatRoomInfo.roomType == 0) {
                if (chatRoomInfo.isAdmin) {
                    if (voiceCallStarterUserId == loggedInUserCache.getLoggedInUserId()) {
                        chatRoomMessageViewModel.loadAgoraToken(tokenRoleBroadcaster)
                    } else {
                        chatRoomMessageViewModel.loadAgoraToken(tokenRoleAudience)
                    }
                }
            } else {
                if (chatRoomInfo.isAdmin) {
                    chatRoomMessageViewModel.loadAgoraToken(tokenRoleBroadcaster)
                } else {
                    chatRoomMessageViewModel.loadAgoraToken(tokenRoleAudience)
                }

            }
        }
    }


    private fun voiceCallUpdatedVisibility(isVisible: Boolean) {
        runOnUiThread {
            binding.progressBarVoiceCall.visibility = View.GONE
            if (chatRoomInfo.isAdmin) {
                if (chatRoomInfo.roomType == 0) {
                    if (voiceCallStarterUserId == loggedInUserCache.getLoggedInUserId()) {
                        binding.llAdminCallContainer.isVisible = isVisible
                        binding.flSpeaker.isVisible = false
                        binding.micAccessFrameLayout.isVisible = false

                    } else {
                        binding.flSpeaker.isVisible = isVisible
                        binding.micAccessFrameLayout.isVisible = isVisible
                    }
                    binding.ivCall.isVisible = !isVisible && chatRoomInfo.roomType != 2
                } else {
                    binding.llAdminCallContainer.isVisible = isVisible
                    binding.ivCall.isVisible = !isVisible && chatRoomInfo.roomType != 2
                }
            } else {
                binding.flSpeaker.isVisible = isVisible
                binding.micAccessFrameLayout.isVisible = isVisible
            }

            if (isVisible) {
                if (binding.ivMicOff.isVisible) {
                    agoraEngine?.muteLocalAudioStream(true)
                }
                if (binding.flSpeaker.isVisible) {
                    binding.ivSpeaker.isVisible = true
                    binding.ivSpeakerOff.isVisible = false
                }
                binding.micAppCompatImageView.visibility = View.GONE
                binding.micOffAppCompatImageView.visibility = View.VISIBLE

            }
            isCallStart = isVisible
        }
    }

    private fun joinChannel(joinAgoraSDKVoiceCallChannel: JoinAgoraSDKVoiceCallChannel) {
        Timber.i("Join Channel Info %s", joinAgoraSDKVoiceCallChannel.toString())
        val options = ChannelMediaOptions()
        options.autoSubscribeAudio = true
//        // Set both clients as the BROADCASTER.
//        options.clientRoleType = joinAgoraSDKVoiceCallChannel.rolType
//        // Set the channel profile as BROADCASTING.
//        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

        agoraEngine?.setClientRole(joinAgoraSDKVoiceCallChannel.rolType)
        agoraEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        agoraEngine?.setDefaultAudioRoutetoSpeakerphone(true)

        agoraEngine?.joinChannel(
            joinAgoraSDKVoiceCallChannel.token,
            joinAgoraSDKVoiceCallChannel.channelName,
            options.toString(),
            joinAgoraSDKVoiceCallChannel.userId
        )
        if (joinAgoraSDKVoiceCallChannel.rolType == Constants.CLIENT_ROLE_BROADCASTER) {
            val voiceCallStartSocketRequest = VoiceCallStartSocketRequest(
                joinAgoraSDKVoiceCallChannel.conversationId,
                joinAgoraSDKVoiceCallChannel.token,
                joinAgoraSDKVoiceCallChannel.channelName,
                loggedInUserCache.getLoggedInUserId()!!
            )
            chatRoomMessageViewModel.updateVoiceCallStarted(voiceCallStartSocketRequest)
        }
    }

    private fun openViewUserBottomSheet(messageInfo: MessageInfo) {
        val viewUserBottomSheet =
            chatRoomInfo.let {
                ViewUserBottomSheet.newInstance(
                    it,
                    messageInfo,
                    voiceCallStarterUserId,
                    listOfAdminId
                )
            }
        viewUserBottomSheet.show(supportFragmentManager, ViewUserBottomSheet::class.java.name)
    }

    private fun callJoinRoomRefresh() {
        if (socketDataManager.isConnected) {
            chatRoomMessageViewModel.joinRoom(
                JoinRoomRequest(conversationId = chatRoomInfo.id)
            )
        } else {
            Handler().postDelayed({
                callJoinRoomRefresh()
            }, 100)
        }
    }

    private fun runTimer() {
        var seconds = 0
        handler?.post(object : Runnable {
            override fun run() {
                val hours: Int = seconds / 3600
                val minutes: Int = seconds % 3600 / 60
                val secs: Int = seconds % 60
                // Format the seconds into hours, minutes,
                // and seconds.
                val time: String = java.lang.String.format(
                    Locale.getDefault(),
                    "%d:%02d:%02d",
                    hours,
                    minutes,
                    secs
                )

                // Set the text view text.
                binding.timerTextView.text = time

                // If running is true, increment the
                // seconds variable.
                seconds++

                // Post the code again
                // with a delay of 1 second.
                handler?.postDelayed(this, 1000)
            }
        })
    }

    private fun manageAdminProfileImageClick() {
        when (listOfAdmin.size) {
            0 -> {
                manageNoAdmin()
            }
            1 -> {
                manageOneAdmin()
            }
            2 -> {
                manageTwoAdmin()
            }
            else -> {
                manageThreeAdmin()
            }
        }

    }

    private fun manageNoAdmin() {
        binding.ivFirstAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        binding.ivSecondAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        binding.ivThirdAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        binding.ivFourthAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        binding.ivFifthAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()
    }

    private fun manageOneAdmin() {
        binding.ivSecondAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        binding.ivThirdAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        binding.ivFourthAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        binding.ivFifthAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        loadAdminProfileImage(listOfAdmin.first().senderProfile.toString(), binding.ivFirstAdmin)

    }

    private fun manageTwoAdmin() {
        binding.ivThirdAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        binding.ivFourthAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()


        binding.ivFifthAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()
        loadAdminProfileImage(listOfAdmin.first().senderProfile.toString(), binding.ivFirstAdmin)
        loadAdminProfileImage(
            listOfAdmin.component2().senderProfile.toString(),
            binding.ivSecondAdmin
        )

    }

    private fun manageThreeAdmin() {
        binding.ivFourthAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        binding.ivFifthAdmin.throttleClicks().subscribeAndObserveOnMainThread {
            openSubscriptionBottomSheet()
        }.autoDispose()

        loadAdminProfileImage(listOfAdmin.first().senderProfile.toString(), binding.ivFirstAdmin)
        loadAdminProfileImage(
            listOfAdmin.component2().senderProfile.toString(),
            binding.ivSecondAdmin
        )
        loadAdminProfileImage(
            listOfAdmin.component3().senderProfile.toString(),
            binding.ivThirdAdmin
        )
    }

    private fun loadAdminProfileImage(image: String, container: SquareImageView) {
        Glide.with(this)
            .load(image)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(container)
    }

    private fun mentionUserViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.llMentionUserListContainer.visibility == View.GONE) {
            binding.llMentionUserListContainer.visibility = View.VISIBLE
        } else if (!isVisibility && binding.llMentionUserListContainer.visibility == View.VISIBLE) {
            binding.llMentionUserListContainer.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        messageList.clear()
        staySafeMessage = 0
        chatRoomMessageViewModel.resetPagination(
            GetChatMessageRequest(
                conversationId = chatRoomInfo.id,
                perPage = perPage
            )
        )
        callJoinRoomRefresh()

        if (binding.flSpeaker.isVisible) {
            binding.ivSpeaker.isVisible = true
            binding.ivSpeakerOff.isVisible = false
        }

        RxBus.listen(RxEvent.UpdateChatRoom::class.java).subscribeAndObserveOnMainThread {
            chatRoomInfo = it.chatRoomInfo

            if (it.fromProfile) {
                finish()
            } else {
                binding.tvChatRoomName.text = it.chatName
                binding.ivCall.visibility = View.GONE
                binding.llAdminProfileImageContainer.isVisible = false
                binding.tvAdmin.visibility = View.GONE
                binding.ivMore.visibility =
                    if (chatRoomInfo.roomType == 2) View.GONE else View.VISIBLE
            }

        }.autoDispose()

        RxBus.listen(RxEvent.DeleteChatRoom::class.java).subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        RxBus.listen(RxEvent.UpdatedAdminSubscription::class.java).subscribeAndObserveOnMainThread {
            binding.llAdminProfileImageContainer.isVisible = false
            binding.view.isVisible = false
            chatRoomInfo.isAdmin = true
            binding.ivMore.isVisible = true
            binding.tvAdmin.isVisible = true

        }.autoDispose()

    }

    override fun onDestroy() {
        agoraEngine?.leaveChannel()
        if (chatRoomInfo.roomType == 0) {
            if (chatRoomInfo.isAdmin) {
                if (voiceCallStarterUserId == loggedInUserCache.getLoggedInUserId()) {
                    chatRoomMessageViewModel.updateVoiceCallEnded()
                }
            }

        } else {
            if (chatRoomInfo.isAdmin) {
                chatRoomMessageViewModel.updateVoiceCallEnded()
            }
        }

        val request = SendPrivateMessageRequest(
            conversationId = chatRoomInfo.id,
            senderId = loggedInUserCache.getLoggedInUserId(),
            message = loggedInUserCache.getChatUser()?.chatUserName.plus(" ").plus("Left Room"),
            messageType = Constant.MESSAGE_TYPE_LEFT,
            senderName = loggedInUserCache.getChatUser()?.chatUserName ?: "",
            conversationName = chatRoomInfo.roomName,
            senderProfile = loggedInUserCache.getChatUser()?.chatUserProfile,
            age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
            gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender
        )
        chatRoomMessageViewModel.sendPrivateMessage(request)

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()

        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        handler = null
        staySafeMessage = 0

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        if (camera == 0 && audio == 0) {
            setupVoiceSDKEngine()
        }
    }

}
