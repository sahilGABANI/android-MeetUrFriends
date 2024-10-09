package com.meetfriend.app.ui.camerakit

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.databinding.BottomsheetVideoTimerBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.Locale

class VideoTimerBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        const val SIXTINE = 60

        fun newInstance(): VideoTimerBottomSheet {
            val args = Bundle()
            val fragment = VideoTimerBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: BottomsheetVideoTimerBinding? = null
    private val binding get() = _binding!!

    private val videoTimerStateSubject: PublishSubject<Int> = PublishSubject.create()
    val videoTimerState: Observable<Int> = videoTimerStateSubject.hide()

    private var progress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetVideoTimerBinding.inflate(inflater, container, false)
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

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        }

    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            videoTimerStateSubject.onNext(0)
            dismiss()
        }.autoDispose()

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update the timer display
                this@VideoTimerBottomSheet.progress = progress
                binding.tvTimer.text = convertSecondsToTimeFormat(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Handle start tracking touch
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Handle stop tracking touch
            }
        })

        binding.tvSetTimer.throttleClicks().subscribeAndObserveOnMainThread {
            videoTimerStateSubject.onNext(progress)
            dismiss()
        }.autoDispose()
    }

    fun convertSecondsToTimeFormat(seconds: Int): String {
        val minutes = seconds / SIXTINE
        val remainingSeconds = seconds % SIXTINE
        return String.format(Locale.getDefault(),"%02d:%02d", minutes, remainingSeconds)
    }
}
