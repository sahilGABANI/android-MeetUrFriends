package com.meetfriend.app.ui.livestreaming

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.livestreaming.model.LiveUserOption
import com.meetfriend.app.databinding.LiveUserOptionBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveUserOptionBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: LiveUserOptionBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val liveUserClicksSubject: PublishSubject<LiveUserOption> = PublishSubject.create()
    val liveUserClicks: Observable<LiveUserOption> = liveUserClicksSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LiveUserOptionBottomSheetBinding.inflate(inflater, container, false)
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

        listenToViewEvents()
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

    private fun listenToViewEvents() {
        binding.llViewProfile.throttleClicks().subscribeAndObserveOnMainThread {
            liveUserClicksSubject.onNext(LiveUserOption.ViewProfile)
            dismiss()
        }.autoDispose()

        binding.llKickOut.throttleClicks().subscribeAndObserveOnMainThread {
            liveUserClicksSubject.onNext(LiveUserOption.KickOut)
            dismiss()
        }.autoDispose()

        binding.llRestrictUser.throttleClicks().subscribeAndObserveOnMainThread {
            liveUserClicksSubject.onNext(LiveUserOption.Restrict)
            dismiss()
        }.autoDispose()

        binding.llCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }
}
