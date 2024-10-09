package com.meetfriend.app.ui.livestreaming

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.databinding.BottomSheetLiveStreamingMoreOptionBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveStreamingMoreOptionBottomSheet : BaseBottomSheetDialogFragment() {

    private val optionClickSubject: PublishSubject<String> = PublishSubject.create()
    val optionClick: Observable<String> = optionClickSubject.hide()

    private var _binding: BottomSheetLiveStreamingMoreOptionBinding? = null
    private val binding get() = _binding!!

    private var isFromShorts = false
    private var isFromLive = false
    private var isLoggedInUser = false

    companion object {

        private const val INTENT_IS_FROM_SHORTS = "isFromShorts"
        private const val INTENT_IS_FROM_LIVE = "isFromLive"
        private const val INTENT_IS_LOGGED_IN_USER = "isLoggedInUser"

        fun newInstance(
            isFromShorts: Boolean,
            isFromLive: Boolean,
            isLoggedInUser: Boolean
        ): LiveStreamingMoreOptionBottomSheet {
            val args = Bundle()
            isFromShorts.let { args.putBoolean(INTENT_IS_FROM_SHORTS, it) }
            isFromLive.let { args.putBoolean(INTENT_IS_FROM_LIVE, it) }
            isLoggedInUser.let { args.putBoolean(INTENT_IS_LOGGED_IN_USER, it) }
            val fragment = LiveStreamingMoreOptionBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLiveStreamingMoreOptionBinding.inflate(inflater, container, false)
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

        isFromShorts = arguments?.getBoolean(INTENT_IS_FROM_SHORTS)
            ?: error("No args provided")
        isFromLive = arguments?.getBoolean(INTENT_IS_FROM_LIVE)
            ?: error("No args provided")
        isLoggedInUser = arguments?.getBoolean(INTENT_IS_LOGGED_IN_USER) ?: false
        binding.llReport.isVisible = !isLoggedInUser
        binding.llShare.isVisible = isFromLive
        binding.llCopyLink.isVisible = isFromShorts
        binding.llDelete.isVisible = isLoggedInUser && isFromShorts
        listenToViewEvent()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                return
            }
        }

    private fun listenToViewEvent() {
        binding.llReport.throttleClicks().subscribeAndObserveOnMainThread {
            optionClickSubject.onNext(binding.reportAppCompatTextView.text.toString())
            dismissBottomSheet()
        }.autoDispose()

        binding.llShare.throttleClicks().subscribeAndObserveOnMainThread {
            optionClickSubject.onNext(binding.shareAppCompatTextView.text.toString())
            dismissBottomSheet()
        }.autoDispose()

        binding.llCopyLink.throttleClicks().subscribeAndObserveOnMainThread {
            optionClickSubject.onNext(binding.copyLinkAppCompatTextView.text.toString())
            dismissBottomSheet()
        }.autoDispose()

        binding.llDelete.throttleClicks().subscribeAndObserveOnMainThread {
            optionClickSubject.onNext(binding.deleteAppCompatTextView.text.toString())
            dismissBottomSheet()
        }.autoDispose()

        binding.llEdit.throttleClicks().subscribeAndObserveOnMainThread {
            optionClickSubject.onNext(binding.editAppCompatTextView.text.toString())
            dismissBottomSheet()
        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
