package com.meetfriend.app.ui.main.post

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.databinding.ActivityBottomSheetPostMoreItemBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BottomSheetPostMoreItem : BaseBottomSheetDialogFragment() {

    private val moreOptionClickSubject: PublishSubject<String> = PublishSubject.create()
    val moreOptionClicks: Observable<String> = moreOptionClickSubject.hide()

    private var _binding: ActivityBottomSheetPostMoreItemBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val INTENT_USER_ID = "userId"

        fun newInstanceWithoutData(userId: Int): BottomSheetPostMoreItem {
            val args = Bundle()
            userId.let { args.putInt(INTENT_USER_ID, it) }
            val fragment = BottomSheetPostMoreItem()
            fragment.arguments = args
            return fragment
        }
    }

    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityBottomSheetPostMoreItemBinding.inflate(inflater, container, false)
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
        userId =
            arguments?.getInt(INTENT_USER_ID) ?: throw IllegalStateException("No args provided")

        binding.llDelete.isVisible = userId.toString() == getUserId()
        binding.llReport.isVisible = userId.toString() != getUserId()

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
        binding.llCopy.throttleClicks().subscribeAndObserveOnMainThread {
            moreOptionClickSubject.onNext(resources.getString(R.string.label_copy))
            dismissBottomSheet()
        }
        binding.llShare.throttleClicks().subscribeAndObserveOnMainThread {
            moreOptionClickSubject.onNext(resources.getString(R.string.label_share))
            dismissBottomSheet()
        }
        binding.llRepost.throttleClicks().subscribeAndObserveOnMainThread {
            moreOptionClickSubject.onNext(resources.getString(R.string.labek_repost))
            dismissBottomSheet()
        }
        binding.llDelete.throttleClicks().subscribeAndObserveOnMainThread {
            moreOptionClickSubject.onNext(resources.getString(R.string.label_delete))
            dismissBottomSheet()
        }
        binding.llReport.throttleClicks().subscribeAndObserveOnMainThread {
            moreOptionClickSubject.onNext(resources.getString(R.string.desc_report))
            dismissBottomSheet()
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}
