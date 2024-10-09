package com.meetfriend.app.ui.chatRoom.roomview

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.DialogFragmentKickOutBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.roomview.viewmodel.KickOutViewModel
import com.meetfriend.app.ui.chatRoom.roomview.viewmodel.KickOutViewState
import com.meetfriend.app.utils.Constant
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class KickOutDialogFragment : BaseDialogFragment() {

    companion object {
        private const val INTENT_MESSAGE_INFO = "messageInfo"
        private const val INTENT_CHAT_ROOM_INFO = "chatRoomInfo"
        private const val INTENT_IS_FROM_LIVE = "isFromLive"


        fun newInstance(
            messageInfo: MessageInfo?,
            chatRoomInfo: ChatRoomInfo?,
            isFromLive: Boolean
        ): KickOutDialogFragment {
            val args = Bundle()
            messageInfo.let { args.putParcelable(INTENT_MESSAGE_INFO, it) }
            chatRoomInfo.let { args.putParcelable(INTENT_CHAT_ROOM_INFO, it) }
            isFromLive.let { args.putBoolean(INTENT_IS_FROM_LIVE, it) }
            val fragment = KickOutDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private val continueClicksSubject: PublishSubject<Boolean> = PublishSubject.create()
    val continueClicks: Observable<Boolean> = continueClicksSubject.hide()

    private val continueForLiveClicksSubject: PublishSubject<Int> = PublishSubject.create()
    val continueForLiveClicks: Observable<Int> = continueForLiveClicksSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<KickOutViewModel>
    lateinit var kickOutViewModel: KickOutViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: DialogFragmentKickOutBinding? = null
    private val binding get() = _binding!!

    private var is5MinSelected = false
    private var is10MinSelected = false
    private var is20MinSelected = false
    private var is30MinSelected = false
    private var isCustomSelected = false


    private var seconds = 0

    private var messageInfo: MessageInfo? = null
    private var chatRoomInfo: ChatRoomInfo? = null
    private var isFromLive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)

        MeetFriendApplication.component.inject(this)
        kickOutViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogFragmentKickOutBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadDataFromIntent()
        listenToViewModel()
        listenToViewEvent()
    }

    private fun loadDataFromIntent() {
        isFromLive = arguments?.getBoolean(INTENT_IS_FROM_LIVE) ?: false

        messageInfo = arguments?.getParcelable(INTENT_MESSAGE_INFO)
            ?: if (isFromLive) null else throw IllegalStateException("No args provided")
        chatRoomInfo = arguments?.getParcelable(INTENT_CHAT_ROOM_INFO)
            ?: if (isFromLive) null else throw IllegalStateException("No args provided")


        if (!isFromLive) {
            binding.tvUserName.text = messageInfo?.senderName

            val commentText = SpannableStringBuilder()
                .append(getString(R.string.label_your_have_kick_out_timer))
                .append("  ")
                .append("@")
                .append(messageInfo?.senderName)
            binding.tvUserName.text = commentText
        }
    }

    private fun listenToViewEvent() {
        binding.rl5MinContainer.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.etTime.text.isNullOrEmpty())
                manageOptionClick(5)

        }.autoDispose()

        binding.rl10MinContainer.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.etTime.text.isNullOrEmpty())
                manageOptionClick(10)
        }.autoDispose()

        binding.rl20MinContainer.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.etTime.text.isNullOrEmpty())
                manageOptionClick(20)
        }.autoDispose()

        binding.rl30MinContainer.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.etTime.text.isNullOrEmpty())
                manageOptionClick(30)
        }.autoDispose()

        binding.tvContinue.throttleClicks().subscribeAndObserveOnMainThread {

            if (isValidate()) {
                if (isFromLive) {
                    dismiss()
                    continueForLiveClicksSubject.onNext(seconds)

                } else {
                    prepareDataForKickout()
                    continueClicksSubject.onNext(true)
                }
            }

        }

        binding.etTime.textChanges().subscribeAndObserveOnMainThread {
            if (!binding.etTime.text.isNullOrEmpty()) {
                manageOptionClick(binding.etTime.text.toString().toInt())
            }
        }.autoDispose()

    }

    private fun isValidate(): Boolean {
        var isValidate = false
        isValidate = when {
            !is10MinSelected && !is5MinSelected && !is20MinSelected && !is30MinSelected && !isCustomSelected -> {
                showToast(getString(R.string.error_please_select_time))
                false
            }
            else -> true
        }
        return isValidate
    }

    private fun listenToViewModel() {
        kickOutViewModel.chatRoomMessageState.subscribeAndObserveOnMainThread {

            when (it) {
                is KickOutViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is KickOutViewState.GetNewSendMessage -> {
                    dismiss()
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun manageOptionClick(option: Int) {
        when (option) {
            5 -> {
                manage5MinClick()
            }
            10 -> {
                manage10MinClick()
            }
            20 -> {
                manage20MinClick()
            }
            30 -> {
                manage30MinClick()
            }
            else -> {
                manageCustomClick()
            }
        }
        seconds = option * 60
    }

    private fun manage5MinClick() {
        if (is10MinSelected || is20MinSelected || is30MinSelected) {
            binding.ivSelected10Min.visibility = View.GONE
            binding.ivUnselected10Min.visibility = View.VISIBLE
            binding.ivSelected20Min.visibility = View.GONE
            binding.ivUnselected20Min.visibility = View.VISIBLE
            binding.ivSelected30Min.visibility = View.GONE
            binding.ivUnselected30Min.visibility = View.VISIBLE
            binding.etTime.text?.clear()
            is10MinSelected = false
            is20MinSelected = false
            is30MinSelected = false
            isCustomSelected = false

        }
        if (is5MinSelected) {
            is5MinSelected = false
            binding.ivSelected5Min.visibility = View.GONE
            binding.ivUnselected5Min.visibility = View.VISIBLE
        } else {
            is5MinSelected = true
            binding.ivSelected5Min.visibility = View.VISIBLE
            binding.ivUnselected5Min.visibility = View.GONE
        }
    }

    private fun manage10MinClick() {
        if (is5MinSelected || is20MinSelected || is30MinSelected) {
            binding.ivSelected5Min.visibility = View.GONE
            binding.ivUnselected5Min.visibility = View.VISIBLE
            binding.ivSelected20Min.visibility = View.GONE
            binding.ivUnselected20Min.visibility = View.VISIBLE
            binding.ivSelected30Min.visibility = View.GONE
            binding.ivUnselected30Min.visibility = View.VISIBLE
            binding.etTime.text?.clear()

            is5MinSelected = false
            is20MinSelected = false
            is30MinSelected = false
            isCustomSelected = false

        }
        if (is10MinSelected) {
            is10MinSelected = false
            binding.ivSelected10Min.visibility = View.GONE
            binding.ivUnselected10Min.visibility = View.VISIBLE
        } else {
            is10MinSelected = true
            binding.ivSelected10Min.visibility = View.VISIBLE
            binding.ivUnselected10Min.visibility = View.GONE
        }
    }

    private fun manage20MinClick() {
        if (is10MinSelected || is5MinSelected || is30MinSelected) {
            binding.ivSelected10Min.visibility = View.GONE
            binding.ivUnselected10Min.visibility = View.VISIBLE
            binding.ivSelected5Min.visibility = View.GONE
            binding.ivUnselected5Min.visibility = View.VISIBLE
            binding.ivSelected30Min.visibility = View.GONE
            binding.ivUnselected30Min.visibility = View.VISIBLE
            binding.etTime.text?.clear()

            is10MinSelected = false
            is5MinSelected = false
            is30MinSelected = false
            isCustomSelected = false

        }
        if (is20MinSelected) {
            is20MinSelected = false
            binding.ivSelected20Min.visibility = View.GONE
            binding.ivUnselected20Min.visibility = View.VISIBLE
        } else {
            is20MinSelected = true
            binding.ivSelected20Min.visibility = View.VISIBLE
            binding.ivUnselected20Min.visibility = View.GONE
        }
    }

    private fun manage30MinClick() {
        if (is10MinSelected || is20MinSelected || is5MinSelected) {
            binding.ivSelected10Min.visibility = View.GONE
            binding.ivUnselected10Min.visibility = View.VISIBLE
            binding.ivSelected20Min.visibility = View.GONE
            binding.ivUnselected20Min.visibility = View.VISIBLE
            binding.ivSelected5Min.visibility = View.GONE
            binding.ivUnselected5Min.visibility = View.VISIBLE
            binding.etTime.text?.clear()

            is5MinSelected = false
            is10MinSelected = false
            is20MinSelected = false
            isCustomSelected = false

        }
        if (is30MinSelected) {
            is30MinSelected = false
            binding.ivSelected30Min.visibility = View.GONE
            binding.ivUnselected30Min.visibility = View.VISIBLE
        } else {
            is30MinSelected = true
            binding.ivSelected30Min.visibility = View.VISIBLE
            binding.ivUnselected30Min.visibility = View.GONE
        }
    }

    private fun manageCustomClick() {
        isCustomSelected = true
        binding.ivSelected10Min.visibility = View.GONE
        binding.ivUnselected10Min.visibility = View.VISIBLE
        binding.ivSelected20Min.visibility = View.GONE
        binding.ivUnselected20Min.visibility = View.VISIBLE
        binding.ivSelected5Min.visibility = View.GONE
        binding.ivUnselected5Min.visibility = View.VISIBLE
        binding.ivSelected30Min.visibility = View.GONE
        binding.ivUnselected30Min.visibility = View.VISIBLE

        is5MinSelected = false
        is10MinSelected = false
        is20MinSelected = false
        is30MinSelected = false

    }

    private fun prepareDataForKickout() {
        val request = SendPrivateMessageRequest(
            senderId = loggedInUserCache.getLoggedInUserId(),
            senderName = loggedInUserCache.getChatUser()?.chatUserName,
            conversationId = messageInfo?.conversationId,
            senderProfile = loggedInUserCache.getChatUser()?.chatUserProfile,
            messageType = Constant.MESSAGE_TYPE_KICK_OUT,
            kickoutUserId = messageInfo?.senderId,
            seconds = seconds,
            message = loggedInUserCache.getChatUser()?.chatUserName.plus(" ")
                .plus(getString(R.string.label_kick_out_message)).plus(" ")
                .plus(messageInfo?.senderName),
            age = loggedInUserCache.getLoggedInUser()?.loggedInUser?.age,
            gender = loggedInUserCache.getLoggedInUser()?.loggedInUser?.gender,
            conversationName = chatRoomInfo?.roomName
        )
        kickOutViewModel.sendPrivateMessage(request)
    }

}