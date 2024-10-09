package com.meetfriend.app.ui.chatRoom.roomview

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.databinding.BottomSheetChatRoomMoreOptionBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.ChatRoomInfoActivity
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


class ViewChatRoomMoreOptionBottomSheet : BaseBottomSheetDialogFragment() {

    private val moreOptionClickSubject: PublishSubject<ChatRoomInfo> = PublishSubject.create()
    val moreOptionClicks: Observable<ChatRoomInfo> = moreOptionClickSubject.hide()

    private var _binding: BottomSheetChatRoomMoreOptionBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val INTENT_CHAT_ROOM_INFO = "chatRoomInfo"

        fun newInstance(chatRoomInfo: ChatRoomInfo?): ViewChatRoomMoreOptionBottomSheet {
            val args = Bundle()
            chatRoomInfo.let { args.putParcelable(INTENT_CHAT_ROOM_INFO, it) }
            val fragment = ViewChatRoomMoreOptionBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    lateinit var chatRoomInfo: ChatRoomInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetChatRoomMoreOptionBinding.inflate(inflater, container, false)
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
        chatRoomInfo = arguments?.getParcelable(INTENT_CHAT_ROOM_INFO)
            ?: throw IllegalStateException("No args provided")

        listenToViewEvents()

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
        binding.tvChatUserList.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
            startActivity(ChatRoomInfoActivity.getIntent(requireContext(), chatRoomInfo))
        }.autoDispose()

        binding.tvMIcAccessList.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
            moreOptionClickSubject.onNext(chatRoomInfo)
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()

    }

    private fun dismissBottomSheet() {
        dismiss()
    }
}