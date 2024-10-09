package com.meetfriend.app.ui.bottomsheet.auth

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.meetfriend.app.R
import com.meetfriend.app.databinding.FragmentAddAccountBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant

class AddAccountBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = AddAccountBottomSheet()
    }

    private var _binding: FragmentAddAccountBottomSheetBinding? = null
    private val binding
        get() = _binding ?: error("LoginBottomSheet should not be accessed when it's null")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeR)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAccountBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val bottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheetView =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheetView != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.skipCollapsed = true
                behavior.isDraggable = false
                behavior.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }
        }

        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        binding.tvLoginBtn.throttleClicks().subscribeAndObserveOnMainThread {
            openLoginBottomSheet()
            Constant.CLICK_COUNT++
        }.autoDispose()

        binding.tvRegisterBtn.throttleClicks().subscribeAndObserveOnMainThread {
            openRegisterBottomSheet()
            Constant.CLICK_COUNT++
        }.autoDispose()

        binding.closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }
}
