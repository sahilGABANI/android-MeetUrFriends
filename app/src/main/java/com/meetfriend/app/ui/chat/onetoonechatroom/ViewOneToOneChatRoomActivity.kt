package com.meetfriend.app.ui.chat.onetoonechatroom

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomUser
import com.meetfriend.app.api.chat.model.GetChatMessageRequest
import com.meetfriend.app.api.chat.model.JoinChatRoomResponse
import com.meetfriend.app.api.chat.model.JoinRoomRequest
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.MessageType
import com.meetfriend.app.api.chat.model.SeenMsgRequest
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.api.chat.model.TypingRequest
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.gift.model.AcceptRejectGiftRequest
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.gift.model.GiftsItemInfo
import com.meetfriend.app.api.message.model.EditMessageRequest
import com.meetfriend.app.api.message.model.MessageAction
import com.meetfriend.app.api.profile.model.SelectedOption
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityViewOneToOneChatRoomBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showKeyboard
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.socket.SocketDataManager
import com.meetfriend.app.ui.chat.onetoonechatroom.view.OneToOneChatMessageAdapter
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.ViewOneToOneChatViewModel
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.ViewOneToOneChatViewState
import com.meetfriend.app.ui.chatRoom.profile.ChatRoomProfileActivity
import com.meetfriend.app.ui.chatRoom.profile.ImagePickerOptionBottomSheet
import com.meetfriend.app.ui.chatRoom.profile.OtherUserFullProfileActivity
import com.meetfriend.app.ui.chatRoom.videoCall.OneToOneVideoCallActivity
import com.meetfriend.app.ui.coins.CoinPlansActivity
import com.meetfriend.app.ui.giftsGallery.GiftGalleryBottomSheet
import com.meetfriend.app.ui.home.shorts.PostCameraActivity
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.main.story.StoryInfoActivity
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.ui.storywork.models.ResultListResult
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FIX_100_MILLISECOND
import com.meetfriend.app.utils.Constant.FiXED_100_MILLISECOND
import com.meetfriend.app.utils.Constant.FiXED_20_INT
import com.meetfriend.app.utils.FileUtils
import java.io.File
import javax.inject.Inject

class ViewOneToOneChatRoomActivity : BasicActivity() {
    companion object {
        const val INTENT_CHAT_ROOM_INFO = "INTENT_CHAT_ROOM_INFO"
        const val INTENT_START_VIDEO_CALL = "INTENT_START_VIDEO_CALL"
        const val INTENT_VIDEO_CALL = "INTENT_VIDEO_CALL"
        const val INTENT_CAN_SEND_MESSAGE = "INTENT_CAN_SEND_MESSAGE"
        var storyId: Int = 0

        fun getIntent(context: Context, chatRoomInfo: ChatRoomInfo, startVC: Boolean): Intent {
            val intent = Intent(context, ViewOneToOneChatRoomActivity::class.java)
            intent.putExtra(INTENT_CHAT_ROOM_INFO, chatRoomInfo)
            intent.putExtra(INTENT_START_VIDEO_CALL, startVC)
            return intent
        }

        fun getIntentWithData(
            context: Context,
            chatRoomInfo: ChatRoomInfo,
            startVC: Boolean,
            canSendMessage: Boolean
        ): Intent {
            val intent = Intent(context, ViewOneToOneChatRoomActivity::class.java)
            intent.putExtra(INTENT_CHAT_ROOM_INFO, chatRoomInfo)
            intent.putExtra(INTENT_VIDEO_CALL, startVC)
            intent.putExtra(INTENT_START_VIDEO_CALL, false)
            intent.putExtra(INTENT_CAN_SEND_MESSAGE, canSendMessage)
            return intent
        }

        @JvmStatic
        fun getConversationId(viewOneToOneChatRoomActivity: ViewOneToOneChatRoomActivity): Int {
            return viewOneToOneChatRoomActivity.chatRoomInfo.id
        }
    }

    private lateinit var binding: ActivityViewOneToOneChatRoomBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ViewOneToOneChatViewModel>
    private lateinit var viewOneToOneChatViewModel: ViewOneToOneChatViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    lateinit var socketDataManager: SocketDataManager

    private lateinit var oneToOneChatMessageAdapter: OneToOneChatMessageAdapter
    private lateinit var chatRoomInfo: ChatRoomInfo
    private var messageList: MutableList<MessageInfo> = mutableListOf()
    private var perPage = FiXED_20_INT
    private var handler: Handler? = null
    private var joinChatRoomResponse: JoinChatRoomResponse? = null
    private var sendMessageCount: Int = 0
    private var position = 0
    private var editMessageInfo: MessageInfo? = null
    private var replyMessageInfo: MessageInfo? = null
    private var startVC: Boolean = false
    private var isNormalChat: Boolean = false

    private lateinit var handlePathOz: HandlePathOz
    private var selectedImagePath: String = ""
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var mediaType: String? = null
    private var canSendMessage = true
    private var acceptRequestInfo: MessageInfo? = null
    private var isTyping = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewOneToOneChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        viewOneToOneChatViewModel = getViewModelFromFactory(viewModelFactory)

        handler = Handler(Looper.getMainLooper())
        loadDataFromIntent()
        listenToViewEvent()
        listenToViewModel()
        viewOneToOneChatViewModel.getCloudFlareConfig()

        intent?.let {
            if (it.getBooleanExtra(MainHomeActivity.INTENT_IS_SWITCH_ACCOUNT, false)) {
                intent?.getIntExtra(MainHomeActivity.USER_ID, 0)?.let { userId ->
                    RxBus.publish(RxEvent.switchUserAccount(userId))
                }
            }
        }
    }

    private fun loadDataFromIntent() {
        this.chatRoomInfo = intent?.getParcelableExtra(INTENT_CHAT_ROOM_INFO) ?: return
        this.startVC = intent?.getBooleanExtra(INTENT_START_VIDEO_CALL, false) ?: return
        canSendMessage = intent?.getBooleanExtra(INTENT_CAN_SEND_MESSAGE, true) ?: true
        intent?.getBooleanExtra(INTENT_VIDEO_CALL, false)?.let {
            isNormalChat = it
        }

        if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
            binding.tvChatRoomName.text = chatRoomInfo.user?.firstName
            userOnline(chatRoomInfo.user)
            if (chatRoomInfo.user?.isVerified == 1) {
                binding.ivAccountVerified.visibility = View.VISIBLE
            } else {
                binding.ivAccountVerified.visibility = View.GONE
            }
        } else {
            binding.tvChatRoomName.text = chatRoomInfo.receiver?.firstName
            userOnline(chatRoomInfo.receiver)
            if (chatRoomInfo.receiver?.isVerified == 1) {
                binding.ivAccountVerified.visibility = View.VISIBLE
            } else {
                binding.ivAccountVerified.visibility = View.GONE
            }
        }
        if (startVC) {
            startVideoCall()
        }
        binding.tvCanNotSendMessage.isVisible = !canSendMessage
    }

    private fun userOnline(user: ChatRoomUser?) {
        if (user?.isOnline == 1) {
            binding.tvUserOnlineStatus.text = getString(R.string.online)
        } else {
            if (user?.lastSeen.isNullOrEmpty()) {
                binding.tvUserOnlineStatus.isVisible = false
            } else {
                binding.tvUserOnlineStatus.text =
                    getString(R.string.last_seen).plus(" : ").plus(
                        user?.lastSeen?.let {
                            FileUtils.lastSeenMessageTime(it)
                        }
                    )
            }
        }
    }

    private fun startVideoCall() {
        startActivity(OneToOneVideoCallActivity.getIntent(this, chatRoomInfo, true))
    }

    private fun deleteMessage(messageInfo: MessageInfo) {
        val builder =
            AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage(resources.getString(R.string.label_are_you_sure_you_want_to_delete_this_message))
        builder.setPositiveButton(
            Html.fromHtml(
                "<font color='#B22827'>Delete</font>"
            )
        ) { dialog, _ ->
            dialog.dismiss()
            viewOneToOneChatViewModel.updateMessage(
                EditMessageRequest(
                    messageInfo.id,
                    messageInfo.conversationId,
                    Constant.MESSAGE_TYPE_DELETE
                )
            )
            position = messageList.indexOf(messageInfo)
        }
        builder.setNegativeButton(
            resources.getString(R.string.label_cancel)
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun listenToViewEvent() {
        handlePathOz = HandlePathOz(this, listener)

        binding.ivVideoCall.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermissionGranted(this)
        }.autoDispose()

        val profileImage =
            if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                chatRoomInfo.user?.profilePhoto
            } else {
                chatRoomInfo.receiver?.profilePhoto
            }

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            viewOneToOneChatViewModel.leaveOneToOneRoom(chatRoomInfo.id)
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        oneToOneChatMessageAdapter =
            OneToOneChatMessageAdapter(
                this,
                if (canSendMessage) 1 else 0,
                loggedInUserCache.getLoggedInUserId(),
                profileImage
            ).apply {
                messageItemClicks.subscribeAndObserveOnMainThread {
                    handleMessageItemClick(it)
                }.autoDispose()
            }

        val llm = LinearLayoutManager(this, RecyclerView.VERTICAL, true)
        llm.stackFromEnd = true

        binding.rvMessage.apply {
            layoutManager = llm
            adapter = oneToOneChatMessageAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy < 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount && pastVisibleItems >= 0) {
                                viewOneToOneChatViewModel.loadMore(
                                    GetChatMessageRequest(
                                        conversationId = chatRoomInfo.id,
                                        perPage = perPage,
                                        roomType = chatRoomInfo.roomType
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
                    }, FIX_100_MILLISECOND)
                }
            }
        }

        binding.etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.rvMessage.smoothScrollToPosition(0)
            }
        }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Send typing indicator to other users when typing starts
                if (count > 0 && before == 0) {
                    viewOneToOneChatViewModel.startTyping(
                        TypingRequest(
                            chatRoomInfo.id,
                            if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                                chatRoomInfo.user?.id
                            } else {
                                chatRoomInfo.receiver?.id
                            }
                        )
                    )
                    binding.ivGift.isVisible = false
                    binding.ivRequestGift.isVisible = false
                } else if (count == 0 && before > 0) {
                    if (s?.length == 0) {
                        // Send typing indicator to other users when typing stops
                        viewOneToOneChatViewModel.stopTyping(
                            TypingRequest(
                                chatRoomInfo.id,
                                if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                                    chatRoomInfo.user?.id
                                } else {
                                    chatRoomInfo.receiver?.id
                                }
                            )
                        )
                        binding.ivGift.isVisible = true
                        binding.ivRequestGift.isVisible = true
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                return
            }
        })
        setupClickEvent()
    }

    private fun setupClickEvent() {
        val profileImage =
            if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                chatRoomInfo.user?.profilePhoto
            } else {
                chatRoomInfo.receiver?.profilePhoto
            }
        binding.ivSend.throttleClicks().subscribeAndObserveOnMainThread {
            if (canSendMessage) {
                manageSendMessage()
                binding.ivGift.isVisible = true
                binding.ivRequestGift.isVisible = true
            }
        }.autoDispose()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            binding.rlReplyContainer.visibility = View.GONE
            replyMessageInfo = null
        }.autoDispose()

        Glide.with(this).load(profileImage).error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder).into(binding.ivUserProfileImage)
        binding.ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            handleProfileClick()
        }.autoDispose()

        binding.tvChatRoomName.throttleClicks().subscribeAndObserveOnMainThread {
            handleChatRoomNameClick()
        }.autoDispose()

        binding.ivAdd.throttleClicks().subscribeAndObserveOnMainThread {
            if (canSendMessage) {
                openSelectionBottomSheet()
            }
            binding.rlReplyContainer.isVisible = false
            replyMessageInfo = null
            editMessageInfo = null
            binding.etMessage.setText("")
        }.autoDispose()

        binding.ivGift.throttleClicks().subscribeAndObserveOnMainThread {
            if (canSendMessage) {
                openSendGiftBottomSheet(true)
                binding.rlReplyContainer.isVisible = false
                replyMessageInfo = null
                editMessageInfo = null
                binding.etMessage.setText("")
            }
        }.autoDispose()

        binding.ivRequestGift.throttleClicks().subscribeAndObserveOnMainThread {
            if (canSendMessage) {
                openSendGiftBottomSheet(false)
                binding.rlReplyContainer.isVisible = false
                replyMessageInfo = null
                editMessageInfo = null
                binding.etMessage.setText("")
            }
        }.autoDispose()
    }

    private fun handleChatRoomNameClick() {
        if (isNormalChat) {
            val userId =
                if (chatRoomInfo.userId == loggedInUserCache.getLoggedInUserId()) {
                    chatRoomInfo.receiver?.id
                } else {
                    chatRoomInfo.userId
                }
            startActivity(userId?.let { it1 -> MyProfileActivity.getIntentWithData(this, it1) })
        } else {
            val userId =
                if (chatRoomInfo.userId == loggedInUserCache.getLoggedInUserId()) {
                    chatRoomInfo.receiver?.id
                } else {
                    chatRoomInfo.userId
                }
            startActivity(ChatRoomProfileActivity.getIntent(this, userId))
        }
    }

    private fun handleProfileClick() {
        if (isNormalChat) {
            val userId =
                if (chatRoomInfo.userId == loggedInUserCache.getLoggedInUserId()) {
                    chatRoomInfo.receiver?.id
                } else {
                    chatRoomInfo.userId
                }
            startActivity(userId?.let { it1 -> MyProfileActivity.getIntentWithData(this, it1) })
        } else {
            val userId =
                if (chatRoomInfo.userId == loggedInUserCache.getLoggedInUserId()) {
                    chatRoomInfo.receiver?.id
                } else {
                    chatRoomInfo.userId
                }
            startActivity(ChatRoomProfileActivity.getIntent(this, userId))
        }
    }

    private fun handleMessageItemClick(it: MessageAction) {
        when (it) {
            is MessageAction.Delete -> {
                deleteMessage(it.messageInfo)
            }
            is MessageAction.Edit -> {
                position = messageList.indexOf(it.messageInfo)
                editMessageInfo = it.messageInfo
                binding.etMessage.setText(it.messageInfo.message)
                binding.etMessage.requestFocus()
                binding.etMessage.setSelection(binding.etMessage.length())
                showKeyboard()
            }
            is MessageAction.More -> {
                openMoreBottomSheet(it.messageInfo)
            }
            is MessageAction.ViewProfile -> {
                handleViewProfile(it)
            }
            is MessageAction.ViewPhoto -> {
                startActivity(
                    OtherUserFullProfileActivity.getIntent(
                        this@ViewOneToOneChatRoomActivity,
                        it.messageInfo
                    )
                )
            }
            is MessageAction.ViewVideo -> {
                startActivity(
                    ViewVideoMessageActivity.getIntent(
                        this@ViewOneToOneChatRoomActivity,
                        it.messageInfo
                    )
                )
            }
            is MessageAction.AcceptGiftRequest -> {
                handleAcceptGift(it)
            }
            is MessageAction.RejectGiftRequest -> {
                handleRejectGift(it)
            }
            is MessageAction.StoryView -> {
                storyId = it.messageInfo.storyId ?: 0
                viewOneToOneChatViewModel.storyByUSer(it.messageInfo.requestToUser ?: 0)
            }
            else -> {}
        }
    }

    private fun handleViewProfile(it: MessageAction.ViewProfile) {
        if (isNormalChat) {
            startActivity(
                MyProfileActivity.getIntentWithData(
                    this@ViewOneToOneChatRoomActivity,
                    it.messageInfo.senderId ?: 0
                )
            )
        } else {
            startActivity(
                ChatRoomProfileActivity.getIntent(
                    this@ViewOneToOneChatRoomActivity,
                    it.messageInfo.senderId
                )
            )
        }
    }

    private fun handleAcceptGift(it: MessageAction.AcceptGiftRequest) {
        val position =
            oneToOneChatMessageAdapter.listOfDataItems?.indexOf(it.messageInfo)
        viewOneToOneChatViewModel.getMyEarningInfo()
        acceptRequestInfo = it.messageInfo

        it.messageInfo.requestStatus = 1
        if (position != null) {
            oneToOneChatMessageAdapter.notifyItemChanged(position)
        }
    }

    private fun handleRejectGift(it: MessageAction.RejectGiftRequest) {
        val position =
            oneToOneChatMessageAdapter.listOfDataItems?.indexOf(it.messageInfo)
        viewOneToOneChatViewModel.acceptRejectGiftRequest(
            AcceptRejectGiftRequest(
                Constant.MESSAGE_TYPE_REQUEST_GIFT,
                it.messageInfo.id,
                2,
                it.messageInfo.conversationId
            )
        )
        it.messageInfo.requestStatus = 2
        if (position != null) {
            oneToOneChatMessageAdapter.notifyItemChanged(position)
        }
    }

    private fun openSendGiftBottomSheet(isSend: Boolean) {
        val otherUserId =
            if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                chatRoomInfo.user?.id
            } else {
                chatRoomInfo.receiver?.id
            }
        val bottomSheet = GiftGalleryBottomSheet.newInstance(otherUserId.toString(), null, isSend)
        bottomSheet.giftResponseState.subscribeAndObserveOnMainThread {
            when (it) {
                is GiftItemClickStates.GiftItemClick -> {
                }
                is GiftItemClickStates.RequestGiftClick -> {
                    sendGiftMessage(Constant.MESSAGE_TYPE_REQUEST_GIFT, it.data)
                }
                is GiftItemClickStates.SendGiftInChatClick -> {
                    sendGiftMessage(Constant.MESSAGE_TYPE_SEND_GIFT, it.data)
                }
                else -> {}
            }
        }.autoDispose()
        bottomSheet.show(
            supportFragmentManager,
            GiftGalleryBottomSheet::class.java.name
        )
    }

    private fun sendGiftMessage(messageType: String, data: GiftsItemInfo) {
        val senderProfile = loggedInUserCache.getChatUserProfileImage()
        val request = SendPrivateMessageRequest(
            conversationId = chatRoomInfo.id,
            senderId = loggedInUserCache.getLoggedInUserId(),
            messageType = messageType,
            senderName = if (isNormalChat) {
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
                    ?: ""
            } else {
                loggedInUserCache.getChatUser()?.chatUserName ?: ""
            },
            conversationName = binding.tvChatRoomName.text.toString(),
            senderProfile = if (isNormalChat) {
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto
                    ?: ""
            } else {
                senderProfile
            },
            age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
            gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender,
            receiverId = if (chatRoomInfo.receiverId.equals(
                    loggedInUserCache.getLoggedInUserId()
                )
            ) {
                chatRoomInfo.userId
            } else {
                chatRoomInfo.receiverId
            },
            roomType = if (isNormalChat) 3 else 2,
            fileUrl = data.file_path,
            requestCoins = data.coins?.toInt(),
            message = data.name,
            requestStatus = 0,
            giftId = data.id

        )
        viewOneToOneChatViewModel.sendPrivateMessage(request)
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.RECORD_AUDIO)
            .permission(Permission.CAMERA)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        if (canSendMessage) {
                            startVideoCall()
                        }
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    if (never) {
                        showToast(
                            resources.getString(
                                R.string.label_authorization_is_permanently_denied_please_manually_grant_permissions
                            )
                        )
                        XXPermissions.startPermissionActivity(
                            this@ViewOneToOneChatRoomActivity,
                            permissions
                        )
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }
            })
    }

    private fun listenToViewModel() {
        viewOneToOneChatViewModel.viewOneToOneChatState.subscribeAndObserveOnMainThread {
            when (it) {
                is ViewOneToOneChatViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                    editMessageInfo = null
                    replyMessageInfo = null
                }
                is ViewOneToOneChatViewState.LoadChatMessageList -> handleLoadChatMessageList(it)
                is ViewOneToOneChatViewState.GetNewSendMessage -> handleGetNewSendMessage(it)
                is ViewOneToOneChatViewState.JoinRoomResponse -> {
                    joinChatRoomResponse = it.joinChatRoomResponse
                }

                is ViewOneToOneChatViewState.LoadChatMessageEmptyList -> handleLoadChatMessageEmptyList(it)
                is ViewOneToOneChatViewState.MessageDeleted -> handleMessageDelete(it)
                is ViewOneToOneChatViewState.MessageEdited -> {
                    editMessageInfo?.message = binding.etMessage.text.toString()
                    oneToOneChatMessageAdapter.notifyItemChanged(position)
                    editMessageInfo = null
                    binding.etMessage.setText("")
                }
                is ViewOneToOneChatViewState.MessageSaved -> {
                    val savedData = messageList.elementAt(position)
                    savedData.isSave = true
                    oneToOneChatMessageAdapter.notifyItemChanged(position)
                    showToast(getString(R.string.label_msg_saved_successfully))
                }
                is ViewOneToOneChatViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is ViewOneToOneChatViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressedDispatcher.onBackPressed()
                }
                is ViewOneToOneChatViewState.UploadImageCloudFlareSuccess -> sendPrivateImageMessage(it)
                is ViewOneToOneChatViewState.CloudFlareLoadingState -> showLoading(it.isLoading)
                is ViewOneToOneChatViewState.UploadVideoCloudFlareSuccess -> sendPrivateVideoMessage(it)
                is ViewOneToOneChatViewState.GiftRequestActionInfo -> handleGiftRequestActionInfo(it)
                is ViewOneToOneChatViewState.SendGiftSuccess -> handleSendGiftSuccess()
                is ViewOneToOneChatViewState.MyEarningData -> handleMyEarningData(it)
                is ViewOneToOneChatViewState.TypingInfo -> handleTypingInfo(it)
                is ViewOneToOneChatViewState.MessageSeen -> handleMessageSeen()
                is ViewOneToOneChatViewState.StoryResponseData -> handleStoryResponseData(it)
                else -> {}
            }
        }.autoDispose()
    }

    private fun handleLoadChatMessageEmptyList(it: ViewOneToOneChatViewState.LoadChatMessageEmptyList) {
        it.listOfMessage?.let { it1 -> messageList.addAll(it1) }
        oneToOneChatMessageAdapter.listOfDataItems = messageList
        oneToOneChatMessageAdapter.notifyItemChanged(0)
    }

    private fun handleSendGiftSuccess() {
        acceptRequestInfo?.let {
            viewOneToOneChatViewModel.acceptRejectGiftRequest(
                AcceptRejectGiftRequest(
                    Constant.MESSAGE_TYPE_REQUEST_GIFT,
                    it.id,
                    1,
                    it.conversationId
                )
            )
        }
    }

    private fun handleMessageSeen() {
        if (!messageList.isNullOrEmpty()) {
            if (messageList[0].senderId == loggedInUserCache.getLoggedInUserId()) {
                messageList.filter { it.isSeen == 1 }.forEach {
                    it.isSeen = 0
                }
                messageList[0].isSeen = 1
                oneToOneChatMessageAdapter.listOfDataItems = messageList
            }
        }
    }

    private fun handleGiftRequestActionInfo(it: ViewOneToOneChatViewState.GiftRequestActionInfo) {
        var selectedInfo: MessageInfo? = null
        oneToOneChatMessageAdapter.listOfDataItems?.forEach { mInfo ->
            if (mInfo.id == it.messageInfo.id) {
                selectedInfo = mInfo
            }
        }
        val pos = oneToOneChatMessageAdapter.listOfDataItems?.indexOf(selectedInfo)
        if (pos != null) {
            selectedInfo?.requestStatus = it.messageInfo.requestStatus
            oneToOneChatMessageAdapter.notifyItemChanged(pos)
        }
    }

    private fun handleMessageDelete(it: ViewOneToOneChatViewState.MessageDeleted) {
        val list = oneToOneChatMessageAdapter.listOfDataItems as ArrayList
        val seen = list[0].isSeen == 1
        if (position == 0 && seen) {
            list[1].isSeen = 1
        }
        list[position + 1].let {
            if (it.messageType == MessageType.Date) {
                list.removeAt(position + 1)
            }
        }
        list.removeAt(position)
        oneToOneChatMessageAdapter.listOfDataItems = list
    }

    private fun handleLoadChatMessageList(it: ViewOneToOneChatViewState.LoadChatMessageList) {
        messageList.addAll(it.listOfMessage)
        oneToOneChatMessageAdapter.listOfDataItems = messageList
        oneToOneChatMessageAdapter.notifyItemChanged(0)
        if (viewOneToOneChatViewModel.pageNo == 1) {
            Handler().postDelayed({ binding.rvMessage.smoothScrollToPosition(0) }, FIX_100_MILLISECOND)
        }
        messageList[0].let {
            viewOneToOneChatViewModel.seenMsg(
                SeenMsgRequest(
                    it.id,
                    chatRoomInfo.id,
                    if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                        chatRoomInfo.user?.id
                    } else {
                        chatRoomInfo.receiver?.id
                    }
                )
            )
        }
    }

    private fun handleMyEarningData(it: ViewOneToOneChatViewState.MyEarningData) {
        if (it.myEarningInfo?.totalCurrentCoins!! >= acceptRequestInfo?.requestCoins!!) {
            val toId =
                if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                    chatRoomInfo.user?.id
                } else {
                    chatRoomInfo.receiver?.id
                }
            viewOneToOneChatViewModel.sendGift(
                toId.toString(),
                acceptRequestInfo?.requestCoins?.toDouble() ?: 0.0,
                acceptRequestInfo?.giftId ?: 0
            )
        } else {
            showToast(resources.getString(R.string.label_you_dont_have_enough_coins_to_send_this_gift))
            startActivity(CoinPlansActivity.getIntent(this))
        }
    }

    private fun handleStoryResponseData(it: ViewOneToOneChatViewState.StoryResponseData) {
        val originalResult: List<ResultListResult> = it.storyList
        val updatedList = originalResult.map { resultListResult ->
            // Update isSelected for each story in the stories list of resultListResult
            val updatedStories = resultListResult.stories.map { story ->
                val isSelected = story.id == storyId
                story.copy(isSelected = isSelected)
            }
            // Create a new ResultListResult with the updated stories list
            resultListResult.copy(stories = ArrayList(updatedStories))
        }

        startActivity(
            StoryInfoActivity.getIntent(
                this@ViewOneToOneChatRoomActivity,
                updatedList as ArrayList<ResultListResult>
            )
        )
    }

    private fun handleTypingInfo(it: ViewOneToOneChatViewState.TypingInfo) {
        val info = MessageInfo(
            messageType = MessageType.Typing,
            id = 0,
            conversationId = chatRoomInfo.id,
            senderProfile = if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                chatRoomInfo.user?.profilePhoto
            } else {
                chatRoomInfo.receiver?.profilePhoto
            },
            senderId = if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                chatRoomInfo.user?.id
            } else {
                chatRoomInfo.receiver?.id
            }
        )
        if (it.isStart) {
            isTyping = true
            if (messageList[0].messageType != MessageType.Typing) {
                messageList.add(0, info)
                oneToOneChatMessageAdapter.listOfDataItems = messageList
            }
        } else {
            isTyping = false
            val data = messageList.filter { it.messageType == MessageType.Typing }
            if (data.isNotEmpty()) {
                messageList.remove(data.first())
                oneToOneChatMessageAdapter.listOfDataItems = messageList
                binding.rvMessage.smoothScrollToPosition(0)
            }
        }
    }

    private fun handleGetNewSendMessage(it: ViewOneToOneChatViewState.GetNewSendMessage) {
        val data = messageList.filter { it.messageType == MessageType.Typing }
        if (data.isNotEmpty()) {
            messageList.remove(data.first())
            oneToOneChatMessageAdapter.listOfDataItems = messageList
            binding.rvMessage.smoothScrollToPosition(0)
        }
        sendMessageCount++
        if (it.chatMessage.conversationId == chatRoomInfo.id) {
            if (it.chatMessage.isSeen == 1 ||
                it.chatMessage.senderId != loggedInUserCache.getLoggedInUserId()
            ) {
                val seenItem = messageList.filter { it.isSeen == 1 }
                seenItem.forEach { it.isSeen = 0 }
            }
            messageList.add(0, it.chatMessage)
            oneToOneChatMessageAdapter.listOfDataItems = messageList
            oneToOneChatMessageAdapter.notifyItemChanged(0)
            binding.rvMessage.smoothScrollToPosition(0)
            replyMessageInfo = null

            if (isTyping) {
                val info = MessageInfo(
                    messageType = MessageType.Typing,
                    id = 0,
                    conversationId = chatRoomInfo.id,
                    senderProfile = if (chatRoomInfo.receiver?.id
                        == loggedInUserCache.getLoggedInUserId()
                    ) {
                        chatRoomInfo.user?.profilePhoto
                    } else {
                        chatRoomInfo.receiver?.profilePhoto
                    },
                    senderId = if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                        chatRoomInfo.user?.id
                    } else {
                        chatRoomInfo.receiver?.id
                    }
                )
                messageList.add(0, info)
                oneToOneChatMessageAdapter.listOfDataItems = messageList
            }
        }
    }

    private fun sendPrivateVideoMessage(it: ViewOneToOneChatViewState.UploadVideoCloudFlareSuccess) {
        val senderProfile = loggedInUserCache.getChatUserProfileImage()
        val request = SendPrivateMessageRequest(
            conversationId = chatRoomInfo.id,
            senderId = loggedInUserCache.getLoggedInUserId(),
            videoUrl = it.videoId,
            fileUrl = it.thumbnail,
            messageType = Constant.VIDEO,
            senderName = if (isNormalChat) {
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
                    ?: ""
            } else {
                loggedInUserCache.getChatUser()?.chatUserName ?: ""
            },
            conversationName = chatRoomInfo.roomName,
            senderProfile = if (isNormalChat) {
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto
                    ?: ""
            } else {
                senderProfile
            },
            receiverId = if (chatRoomInfo.receiverId.equals(
                    loggedInUserCache.getLoggedInUserId()
                )
            ) {
                chatRoomInfo.userId
            } else {
                chatRoomInfo.receiverId
            },
            roomType = if (isNormalChat) 3 else 2

        )
        viewOneToOneChatViewModel.sendPrivateMessage(request)
    }

    private fun sendPrivateImageMessage(it: ViewOneToOneChatViewState.UploadImageCloudFlareSuccess) {
        val senderProfile = loggedInUserCache.getChatUserProfileImage()
        val request = SendPrivateMessageRequest(
            conversationId = chatRoomInfo.id,
            senderId = loggedInUserCache.getLoggedInUserId(),
            fileUrl = it.imageUrl,
            messageType = Constant.MESSAGE_TYPE_IMAGE,
            senderName = if (isNormalChat) {
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
                    ?: ""
            } else {
                loggedInUserCache.getChatUser()?.chatUserName ?: ""
            },
            conversationName = chatRoomInfo.roomName,
            senderProfile = if (isNormalChat) {
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto
                    ?: ""
            } else {
                senderProfile
            },
            receiverId = if (chatRoomInfo.receiverId.equals(
                    loggedInUserCache.getLoggedInUserId()
                )
            ) {
                chatRoomInfo.userId
            } else {
                chatRoomInfo.receiverId
            },
            roomType = if (isNormalChat) 3 else 2
        )
        viewOneToOneChatViewModel.sendPrivateMessage(request)
    }

    private fun callJoinRoomRefresh() {
        if (socketDataManager.isConnected) {
            viewOneToOneChatViewModel.joinRoom(
                JoinRoomRequest(conversationId = chatRoomInfo.id)
            )
        } else {
            Handler().postDelayed({
                callJoinRoomRefresh()
            }, FiXED_100_MILLISECOND)
        }
    }

    private fun openMoreBottomSheet(messageInfo: MessageInfo) {
        val moreBottomSheet = OneToOneMessageFeatureBottomSheet.newInstance(messageInfo)
        moreBottomSheet.messageActionClicks.subscribeAndObserveOnMainThread {
            when (it) {
                is MessageAction.Reply -> {
                    manageReply(it.messageInfo)
                }
                is MessageAction.Save -> {
                    if (it.messageInfo.isSave) {
                        showToast(getString(R.string.label_msg_already_saved))
                    } else {
                        position = messageList.indexOf(it.messageInfo)
                        viewOneToOneChatViewModel.updateMessage(
                            EditMessageRequest(
                                messageInfo.id,
                                messageInfo.conversationId,
                                Constant.MESSAGE_TYPE_SAVE
                            )
                        )
                    }
                }
                is MessageAction.Forward -> {
                    openForwardBottomSheet(it.messageInfo)
                }

                else -> {
                }
            }
        }.autoDispose()
        moreBottomSheet.show(
            supportFragmentManager,
            OneToOneMessageFeatureBottomSheet::class.java.name
        )
    }

    private fun openForwardBottomSheet(messageInfo: MessageInfo) {
        val bottomSheet = if (isNormalChat) {
            ForwardUserListBottomSheet.newInstanceWithData(messageInfo, isNormalChat)
        } else {
            ForwardUserListBottomSheet.newInstance(messageInfo)
        }

        bottomSheet.selectedPeople.subscribeAndObserveOnMainThread {
            val selectedPeople = it.joinToString()

            viewOneToOneChatViewModel.updateMessage(
                EditMessageRequest(
                    messageInfo.id,
                    messageInfo.conversationId,
                    Constant.MESSAGE_TYPE_FORWARD,
                    forwardIds = selectedPeople
                )
            )
        }.autoDispose()
        bottomSheet.show(supportFragmentManager, ForwardUserListBottomSheet::class.java.name)
    }

    private fun manageSendMessage() {
        if (binding.etMessage.text.toString().isEmpty()) {
            showToast(resources.getString(R.string.error_please_enter_message))
        } else if (editMessageInfo != null) {
            viewOneToOneChatViewModel.updateMessage(
                EditMessageRequest(
                    editMessageInfo!!.id,
                    editMessageInfo!!.conversationId,
                    Constant.MESSAGE_TYPE_EDITED,
                    binding.etMessage.text.toString()
                )
            )
        } else if (replyMessageInfo != null) {
            sendReplayMessage()
        } else {
            sendNormalMessage()
        }
    }

    private fun sendNormalMessage() {
        val senderProfile = loggedInUserCache.getChatUserProfileImage()
        val message = binding.etMessage.text.toString()
        val request = SendPrivateMessageRequest(
            conversationId = chatRoomInfo.id,
            senderId = loggedInUserCache.getLoggedInUserId(),
            message = message,
            messageType = Constant.MESSAGE_TYPE_TEXT,
            senderName = if (isNormalChat) {
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
                    ?: ""
            } else {
                loggedInUserCache.getChatUser()?.chatUserName ?: ""
            },
            conversationName = binding.tvChatRoomName.text.toString(),
            senderProfile = if (isNormalChat) {
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto
                    ?: ""
            } else {
                senderProfile
            },
            age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
            gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender,
            receiverId = if (chatRoomInfo.receiverId.equals(
                    loggedInUserCache.getLoggedInUserId()
                )
            ) {
                chatRoomInfo.userId
            } else {
                chatRoomInfo.receiverId
            },
            roomType = if (isNormalChat) 3 else 2
        )
        viewOneToOneChatViewModel.sendPrivateMessage(request)
        binding.etMessage.setText("")
        binding.rvMessage.smoothScrollToPosition(0)
    }

    private fun sendReplayMessage() {
        replyMessageInfo?.let { replyMessageInfo ->
            val replyName =
                if (replyMessageInfo.senderId == loggedInUserCache.getLoggedInUserId()) {
                    resources.getString(
                        R.string.label_you
                    )
                } else {
                    replyMessageInfo.senderName
                }
            val senderProfile = loggedInUserCache.getChatUserProfileImage()
            val message = binding.etMessage.text.toString()
            val request = SendPrivateMessageRequest(
                conversationId = chatRoomInfo.id,
                senderId = loggedInUserCache.getLoggedInUserId(),
                message = message,
                messageType = Constant.MESSAGE_TYPE_REPLY,
                senderName = if (isNormalChat) {
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
                        ?: ""
                } else {
                    loggedInUserCache.getChatUser()?.chatUserName ?: ""
                },
                conversationName = binding.tvChatRoomName.text.toString(),
                senderProfile = if (isNormalChat) {
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto
                        ?: ""
                } else {
                    senderProfile
                },
                replyId = replyMessageInfo.id,
                replyName = replyName,
                replyMessage = replyMessageInfo.message,
                receiverId = if (chatRoomInfo.receiverId.equals(
                        loggedInUserCache.getLoggedInUserId()
                    )
                ) {
                    chatRoomInfo.userId
                } else {
                    chatRoomInfo.receiverId
                },
                roomType = if (isNormalChat) 3 else 2

            )
            viewOneToOneChatViewModel.sendPrivateMessage(request)
            binding.etMessage.setText("")
            binding.rvMessage.smoothScrollToPosition(0)
            binding.rlReplyContainer.visibility = View.GONE
        }
        replyMessageInfo = null
    }

    private fun manageReply(messageInfo: MessageInfo) {
        binding.rlReplyContainer.visibility = View.VISIBLE
        if (messageInfo.senderId == loggedInUserCache.getLoggedInUserId()) {
            binding.tvReplyUserName.text = getString(R.string.label_you)
        } else {
            binding.tvReplyUserName.text = messageInfo.senderName
        }
        binding.tvReplyMessage.text = messageInfo.message.toString()
        replyMessageInfo = messageInfo
        showKeyboard()
    }

    private fun openSelectionBottomSheet() {
        val imageSelectionBottomSheet = ImagePickerOptionBottomSheet()
        imageSelectionBottomSheet.show(
            supportFragmentManager,
            ImagePickerOptionBottomSheet::class.java.name
        )

        imageSelectionBottomSheet.imageSelectionOptionClicks.subscribeAndObserveOnMainThread {
            when (it) {
                SelectedOption.Gallery.name -> {
                    selectMediaDialog(it)
                }

                SelectedOption.Camera.name -> {
                    selectMediaDialog(it)
                }
            }
        }.autoDispose()
    }

    private fun selectMediaDialog(selectionOption: String) {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
        if (selectionOption == SelectedOption.Gallery.name) {
            alertDialog.setTitle(resources.getString(R.string.label_select))
        } else {
            alertDialog.setTitle(resources.getString(R.string.label_capture))
        }
        alertDialog.setPositiveButton(Constant.PHOTO) { _: DialogInterface, _: Int ->
            if (selectionOption == SelectedOption.Gallery.name) {
                checkPermissionGrantedForSelectMedia(this, Constant.PHOTO)
            } else {
                mediaType = Constant.PHOTO
                checkPermissionGrantedForCamera(this, true)
            }
        }

        alertDialog.setNegativeButton(Constant.VIDEO) { _: DialogInterface?, _: Int ->
            if (selectionOption == SelectedOption.Gallery.name) {
                checkPermissionGrantedForSelectMedia(this, Constant.VIDEO)
            } else {
                mediaType = Constant.VIDEO
                checkPermissionGrantedForCamera(this, false)
            }
        }
        val alert: AlertDialog = alertDialog.create()
        alert.show()
    }

    private fun checkPermissionGrantedForSelectMedia(context: Context, mediaType: String) {
        XXPermissions.with(context)
            .permission(Permission.READ_MEDIA_IMAGES)
            .permission(Permission.READ_MEDIA_VIDEO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        if (mediaType == Constant.PHOTO) {
                            FileUtils.openImagePicker(this@ViewOneToOneChatRoomActivity)
                        } else if (mediaType == Constant.VIDEO) {
                            FileUtils.openVideoPicker(this@ViewOneToOneChatRoomActivity)
                        }
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }

    private fun checkPermissionGrantedForCamera(context: Context, isPhoto: Boolean) {
        XXPermissions.with(context)
            .permission(Permission.CAMERA)
            .permission(Permission.RECORD_AUDIO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startActivityForResultWithDefaultAnimation(
                            PostCameraActivity.launchActivity(
                                this@ViewOneToOneChatRoomActivity,
                                isPhoto
                            ),
                            PostCameraActivity.RC_CAPTURE_PICTURE
                        )
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    if (never) {
                        showToast(
                            resources.getString(
                                R.string.label_authorization_is_permanently_denied_please_manually_grant_permissions
                            )
                        )
                        XXPermissions.startPermissionActivity(
                            this@ViewOneToOneChatRoomActivity,
                            permissions
                        )
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if ((requestCode == FileUtils.PICK_IMAGE) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
                mediaType = Constant.PHOTO
            }
        } else if ((requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) and (resultCode == RESULT_OK)) {
            if (data != null) {
                val isCapturePhoto = data.getBooleanExtra(
                    PostCameraActivity.INTENT_EXTRA_IS_CAPTURE_PHOTO,
                    false
                )
                val filePath = data.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
                if (!filePath.isNullOrEmpty()) {
                    if (isCapturePhoto) {
                        sendMedia(filePath)
                    } else {
                        mediaType = Constant.VIDEO
                        sendMedia(filePath)
                    }
                }
            }
        } else if ((requestCode == FileUtils.PICK_Video) and (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                handlePathOz.getRealPath(it)
                mediaType = Constant.VIDEO
            }
        }
    }

    private fun uploadImageToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            viewOneToOneChatViewModel.uploadImageToCloudFlare(
                this,
                cloudFlareConfig,
                File(selectedImagePath)
            )
        }
    }

    private fun uploadVideoToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
        if (selectedImagePath.isNotEmpty()) {
            viewOneToOneChatViewModel.uploadVideoToCloudFlare(
                this,
                cloudFlareConfig,
                File(selectedImagePath)
            )
        }
    }

    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                sendMedia(filePath)
            }
        }
    }

    private fun sendMedia(filePath: String) {
        if (mediaType == Constant.PHOTO) {
            if (filePath.isNotEmpty()) {
                selectedImagePath = filePath

                cloudFlareConfig?.let {
                    uploadImageToCloudFlare(it)
                } ?: viewOneToOneChatViewModel.getCloudFlareConfig()
            }
        } else {
            if (filePath.isNotEmpty()) {
                selectedImagePath = filePath
                cloudFlareConfig?.let {
                    uploadVideoToCloudFlare(it)
                } ?: viewOneToOneChatViewModel.getCloudFlareConfig()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        messageList.clear()
        viewOneToOneChatViewModel.resetPagination(
            GetChatMessageRequest(
                conversationId = chatRoomInfo.id,
                perPage = perPage,
                roomType = chatRoomInfo.roomType
            )
        )
        callJoinRoomRefresh()

        RxBus.listen(RxEvent.UpdateChatRoom::class.java).subscribeAndObserveOnMainThread {
            chatRoomInfo = it.chatRoomInfo
            binding.tvChatRoomName.text = it.chatName
        }.autoDispose()

        RxBus.listen(RxEvent.DeleteChatRoom::class.java).subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        RxBus.listen(RxEvent.UpdateCanChat::class.java).subscribeAndObserveOnMainThread {
            canSendMessage = it.canSend
            binding.tvCanNotSendMessage.isVisible = !canSendMessage
        }.autoDispose()

        listenToViewEvent()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewOneToOneChatViewModel.stopTyping(
            TypingRequest(
                chatRoomInfo.id,
                if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                    chatRoomInfo.user?.id
                } else {
                    chatRoomInfo.receiver?.id
                }
            )
        )
        viewOneToOneChatViewModel.leaveOneToOneRoom(chatRoomInfo.id)
    }
}
