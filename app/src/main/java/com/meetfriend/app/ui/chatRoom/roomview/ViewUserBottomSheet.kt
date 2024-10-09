package com.meetfriend.app.ui.chatRoom.roomview

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.RestrictUserRequest
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ViewUserBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.profile.ChatRoomProfileActivity
import com.meetfriend.app.ui.chatRoom.report.ReportUserBottomSheet
import com.meetfriend.app.ui.chatRoom.roomview.viewmodel.ViewUserViewModel
import com.meetfriend.app.ui.chatRoom.roomview.viewmodel.ViewUserViewState
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.ShareHelper
import javax.inject.Inject


class ViewUserBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: ViewUserBottomSheetBinding? = null
    private val binding get() = _binding!!

    companion object {

        private const val INTENT_CHAT_ROOM_INFO = "chatRoomInfo"
        private const val INTENT_MESSAGE_INFO = "messageInfo"
        private const val INTENT_LIST_OF_ADMIN_ID = "listOfAdminId"
        private const val INTENT_VOICE_CALL_STARTER_USER_ID = "callStarterUserId"

        fun newInstance(
            chatRoomInfo: ChatRoomInfo?,
            messageInfo: MessageInfo,
            callStarterId: Int,
            listOfAdminId: ArrayList<Int>
        ): ViewUserBottomSheet {
            val args = Bundle()
            chatRoomInfo.let { args.putParcelable(INTENT_CHAT_ROOM_INFO, it) }
            messageInfo.let { args.putParcelable(INTENT_MESSAGE_INFO, it) }
            callStarterId.let { args.putInt(INTENT_VOICE_CALL_STARTER_USER_ID, it) }
            listOfAdminId.let { args.putIntegerArrayList(INTENT_LIST_OF_ADMIN_ID, it) }
            val fragment = ViewUserBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ViewUserViewModel>
    lateinit var viewUserViewModel: ViewUserViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache


    lateinit var chatRoomInfo: ChatRoomInfo
    lateinit var messageInfo: MessageInfo
    private var callStarterId: Int = 0
    private var listOfAdminId: ArrayList<Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

        MeetFriendApplication.component.inject(this)
        viewUserViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewUserBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        loadDataFromIntent()
        listenToViewModel()
        listenToViewEvents()
    }

    private fun loadDataFromIntent() {
        chatRoomInfo = arguments?.getParcelable(INTENT_CHAT_ROOM_INFO)
            ?: throw IllegalStateException("No args provided")
        messageInfo = arguments?.getParcelable(INTENT_MESSAGE_INFO)
            ?: throw IllegalStateException("No args provided")
        callStarterId = arguments?.getInt(INTENT_VOICE_CALL_STARTER_USER_ID)
            ?: throw IllegalStateException("No args provided")
        listOfAdminId = arguments?.getIntegerArrayList(INTENT_LIST_OF_ADMIN_ID) ?: null

        Glide.with(this)
            .load(messageInfo.senderProfile)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        binding.tvUserName.text = messageInfo.senderName

        if (messageInfo.isOnline == 1) {
            binding.tvActiveStatus.text = getString(R.string.label_active)
        } else {
            binding.tvActiveStatus.text = getString(R.string.label_in_active)
        }
        if (messageInfo.senderId == loggedInUserCache.getLoggedInUserId()) {
            binding.llShareProfile.visibility = View.VISIBLE
            binding.llReportUser.visibility = View.GONE
        }
        manageViewVisibility()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private fun listenToViewEvents() {

        binding.llViewProfile.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(
                ChatRoomProfileActivity.getIntent(
                    requireContext(),
                    messageInfo.senderId?.toInt()
                )
            )
            dismissBottomSheet()
        }.autoDispose()

        binding.llKickOut.throttleClicks().subscribeAndObserveOnMainThread {
            openKickoutDialog()
        }.autoDispose()

        binding.llSlapUser.throttleClicks().subscribeAndObserveOnMainThread {
            prepareSlapUserData()
        }

        binding.llBanUser.throttleClicks().subscribeAndObserveOnMainThread {
            prepareBanUserData()
        }

        binding.llSendGift.throttleClicks().subscribeAndObserveOnMainThread {
            showToast(getString(R.string.label_coming_soon))
        }

        binding.llRequestGift.throttleClicks().subscribeAndObserveOnMainThread {
            showToast(getString(R.string.label_coming_soon))
        }

        binding.llReportUser.throttleClicks().subscribeAndObserveOnMainThread {
            val reportUserBottomSheet =
                chatRoomInfo.let { ReportUserBottomSheet.newInstance(messageInfo, chatRoomInfo) }
            reportUserBottomSheet.show(
                parentFragmentManager,
                ReportUserBottomSheet::class.java.name
            )
            dismissBottomSheet()
        }

        binding.llShareProfile.throttleClicks().subscribeAndObserveOnMainThread {
            messageInfo.senderId?.let { it1 ->
                ShareHelper.shareDeepLink(requireContext(), 5, it1, "",true) {
                    ShareHelper.shareText(requireContext(), it)
                }
            }
        }.autoDispose()

        binding.llRestrictUser.throttleClicks().subscribeAndObserveOnMainThread {
            viewUserViewModel.restrictUser(
                RestrictUserRequest(
                    chatRoomInfo.id,
                    messageInfo.senderId
                )
            )
            dismissBottomSheet()
        }
    }

    private fun listenToViewModel() {
        viewUserViewModel.chatRoomMessageState.subscribeAndObserveOnMainThread {
            when (it) {
                is ViewUserViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ViewUserViewState.GetNewSendMessage -> {
                    dismissBottomSheet()
                }
                is ViewUserViewState.BannedUserData -> {
                    dismissBottomSheet()
                }
                is ViewUserViewState.RestrictUser -> {

                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun manageViewVisibility() {
        val isOtherUserAdmin: Boolean =
            !listOfAdminId.isNullOrEmpty() && (listOfAdminId!!.contains(messageInfo.senderId))

        if (chatRoomInfo.roomType == 2 || chatRoomInfo.isAdmin == false || isOtherUserAdmin || messageInfo.senderId == loggedInUserCache.getLoggedInUserId()) {
            binding.llKickOut.visibility = View.GONE
            binding.llBanUser.visibility = View.GONE
            binding.llSlapUser.visibility = View.GONE
            binding.llRequestGift.visibility = View.GONE
            binding.llSendGift.visibility = View.GONE
            binding.llRestrictUser.visibility = View.GONE
        }
    }

    private fun openKickoutDialog() {
        val kickOutDialogFragment = messageInfo.let {
            chatRoomInfo.let { it1 ->
                KickOutDialogFragment.newInstance(
                    it,
                    it1,
                    false
                )
            }
        }
        kickOutDialogFragment?.continueClicks?.subscribeAndObserveOnMainThread {
            if (it) {
                dismissBottomSheet()
            }
        }
        kickOutDialogFragment?.show(childFragmentManager, ViewUserBottomSheet::class.java.name)
    }

    private fun prepareBanUserData() {
        val request = SendPrivateMessageRequest(
            senderId = loggedInUserCache.getLoggedInUserId(),
            senderName = loggedInUserCache.getChatUser()?.chatUserName,
            conversationId = messageInfo.conversationId,
            senderProfile = loggedInUserCache.getChatUser()?.chatUserProfile,
            kickoutUserId = messageInfo.senderId?.toInt(),
            messageType = Constant.MESSAGE_TYPE_BAN,
            message = loggedInUserCache.getChatUser()?.chatUserName.plus(" ")
                .plus(getString(R.string.label_ban)).plus(" ")
                .plus(messageInfo.senderName),
            age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
            gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender,
            conversationName = chatRoomInfo.roomName
        )
        viewUserViewModel.sendPrivateMessage(request)
    }

    private fun prepareSlapUserData() {
        val request = SendPrivateMessageRequest(
            senderId = loggedInUserCache.getLoggedInUserId(),
            senderName = loggedInUserCache.getChatUser()?.chatUserName,
            conversationId = messageInfo.conversationId,
            senderProfile = loggedInUserCache.getChatUser()?.chatUserProfile,
            messageType = Constant.MESSAGE_TYPE_SLAP,
            message = loggedInUserCache.getChatUser()?.chatUserName.plus(" ")
                .plus(getString(R.string.label_slap)).plus(" ")
                .plus(messageInfo.senderName),
            age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
            gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender,
            conversationName = chatRoomInfo.roomName
        )
        viewUserViewModel.sendPrivateMessage(request)
    }

    private fun dismissBottomSheet() {
        dismiss()
    }
}