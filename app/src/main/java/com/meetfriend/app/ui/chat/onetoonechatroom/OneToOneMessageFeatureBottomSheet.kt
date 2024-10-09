package com.meetfriend.app.ui.chat.onetoonechatroom

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.message.model.MessageAction
import com.meetfriend.app.databinding.BottomSheetOneToOneMessageFeatureBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OneToOneMessageFeatureBottomSheet : BaseBottomSheetDialogFragment() {

    private val messageActionClickSubject: PublishSubject<MessageAction> = PublishSubject.create()
    val messageActionClicks: Observable<MessageAction> = messageActionClickSubject.hide()

    private var _binding: BottomSheetOneToOneMessageFeatureBinding? = null
    private val binding get() = _binding!!

    lateinit var messageInfo: MessageInfo

    companion object {

        private const val INTENT_MESSAGE_INFO = "messageInfo"

        fun newInstance(messageInfo: MessageInfo): OneToOneMessageFeatureBottomSheet {
            val args = Bundle()
            messageInfo.let { args.putParcelable(INTENT_MESSAGE_INFO, it) }
            val fragment = OneToOneMessageFeatureBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetOneToOneMessageFeatureBinding.inflate(inflater, container, false)
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

        messageInfo = arguments?.getParcelable(INTENT_MESSAGE_INFO) ?: throw IllegalStateException("No args provided")
        listenToViewModel()
        listenToViewEvents()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    private fun listenToViewEvents() {
        binding.llCopy.throttleClicks().subscribeAndObserveOnMainThread {
            messageActionClickSubject.onNext(MessageAction.Copy(messageInfo))
            copyMessage()
        }.autoDispose()

        binding.llReply.throttleClicks().subscribeAndObserveOnMainThread {
            messageActionClickSubject.onNext(MessageAction.Reply(messageInfo))
            dismissBottomSheet()
        }.autoDispose()

        binding.llSave.throttleClicks().subscribeAndObserveOnMainThread {
            messageActionClickSubject.onNext(MessageAction.Save(messageInfo))
            dismissBottomSheet()
        }.autoDispose()

        binding.llForward.throttleClicks().subscribeAndObserveOnMainThread {
            messageActionClickSubject.onNext(MessageAction.Forward(messageInfo))
            dismissBottomSheet()
        }.autoDispose()
    }

    private fun listenToViewModel() {
    }

    private fun copyMessage() {
        val clipboardManager = binding.llCopy.context.applicationContext.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager
        val clip = ClipData.newPlainText("message", messageInfo.message)
        clipboardManager.setPrimaryClip(clip)

        showToast(getString(R.string.label_message_copied))
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }
}
