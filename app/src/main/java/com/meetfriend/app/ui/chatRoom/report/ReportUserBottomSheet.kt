package com.meetfriend.app.ui.chatRoom.report

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.ReportUserRequest
import com.meetfriend.app.api.hashtag.model.ReportHashtagRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetReportUserBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.report.viewmodel.ReportUserViewModel
import com.meetfriend.app.ui.chatRoom.report.viewmodel.ReportUserViewState
import javax.inject.Inject

class ReportUserBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetReportUserBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val INTENT_CHAT_ROOM_INFO = "chatRoomInfo"
        private const val INTENT_MESSAGE_INFO = "messageInfo"
        private const val INTENT_CHALLENGE_ID = "challengeId"

        fun newInstance(
            messageInfo: MessageInfo,
            chatRoomInfo: ChatRoomInfo
        ): ReportUserBottomSheet {
            val args = Bundle()
            chatRoomInfo.let { args.putParcelable(INTENT_CHAT_ROOM_INFO, it) }
            messageInfo.let { args.putParcelable(INTENT_MESSAGE_INFO, it) }
            val fragment = ReportUserBottomSheet()
            fragment.arguments = args
            return fragment
        }

        fun newInstanceReportUser(
            challengeId: Int
        ): ReportUserBottomSheet {
            val args = Bundle()
            challengeId.let { args.putInt(INTENT_CHALLENGE_ID, it) }

            val fragment = ReportUserBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReportUserViewModel>
    lateinit var reportUserViewModel: ReportUserViewModel

    lateinit var chatRoomInfo: ChatRoomInfo
    lateinit var messageInfo: MessageInfo
    lateinit var selectReason: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
        MeetFriendApplication.component.inject(this)
        reportUserViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReportUserBinding.inflate(inflater, container, false)
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
        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        reportUserViewModel.reportUserState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReportUserViewState.ReportUser -> {
                    dismissBottomSheet()
                }
                is ReportUserViewState.ReportHashtag -> {
                    dismissBottomSheet()
                }
                is ReportUserViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun loadDataFromIntent() {
        if(arguments?.containsKey(INTENT_CHAT_ROOM_INFO) == true)
            chatRoomInfo = arguments?.getParcelable(INTENT_CHAT_ROOM_INFO) ?: throw IllegalStateException("No args provided")
        if(arguments?.containsKey(INTENT_MESSAGE_INFO) == true)
            messageInfo = arguments?.getParcelable(INTENT_MESSAGE_INFO) ?: throw IllegalStateException("No args provided")
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
        binding.tvSpam.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvSpam.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvBullying.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvBullying.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvNudity.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvNudity.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvDoNotLike.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvDoNotLike.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvHateSpeech.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvHateSpeech.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvViolence.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvViolence.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvFalseInfo.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvFalseInfo.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvScam.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvScam.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvSuicide.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvSuicide.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvSaleIllegal.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvSaleIllegal.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvIntellectualProperty.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvIntellectualProperty.text.toString()
            apiCall()
        }.autoDispose()
    }

    private fun apiCall() {

        if(arguments?.containsKey(INTENT_CHALLENGE_ID) == true) {
            var challengeId = arguments?.getInt(INTENT_CHALLENGE_ID) ?: 0
            reportUserViewModel.reportHashtag(ReportHashtagRequest(challengeId, selectReason))
        } else {
            reportUserViewModel.reportUser(
                ReportUserRequest(
                    messageInfo.senderId,
                    chatRoomInfo.id,
                    selectReason
                )
            )
        }
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

}