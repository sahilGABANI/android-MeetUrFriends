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
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentForgotPassBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.hideKeyboard
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.viewmodal.ForgotPasswordViewState
import com.meetfriend.app.viewmodal.RegisterViewModal
import javax.inject.Inject

class ForgotPassBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = ForgotPassBottomSheet()
    }

    private var _binding: FragmentForgotPassBottomSheetBinding? = null
    private val binding
        get() = _binding ?: error("ForgotPassBottomSheet should not be accessed when it's null")

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<RegisterViewModal>
    private lateinit var registerViewModal: RegisterViewModal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSViewDialogTheme)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPassBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        registerViewModal = getViewModelFromFactory(viewModelFactory)

        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        listenToViewEvents()

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
        initializeObservers()
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

    private fun listenToViewEvents() {
        with(binding) {
            dismissBtn.throttleClicks().subscribeAndObserveOnMainThread {
                dismiss()
            }.autoDispose()

            tvRegisterBtn.throttleClicks().subscribeAndObserveOnMainThread {
                if (binding.editEmail.text.toString().trim().equals("")) {
                    showToast("Email is empty")
                } else {
                    hideKeyboard()
                    forgotPasswordAPI()
                }
            }.autoDispose()
        }
    }

    private fun forgotPasswordAPI() {
        val mHashMap = HashMap<String, Any>()
        mHashMap["email_or_phone"] = binding.editEmail.text.toString().trim()
        registerViewModal.forgotPassword(mHashMap)
    }

    private fun initializeObservers() {
        if (activity != null) {
            registerViewModal.forgotPasswordState.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is ForgotPasswordViewState.LoadingState -> {
                        if (state.isLoading) {
                            CallProgressWheel.showLoadingDialog(requireContext())
                        } else {
                            CallProgressWheel.dismissLoadingDialog()
                        }
                    }

                    is ForgotPasswordViewState.ForgotPassword -> {
                        state.commonResponse.message?.let {
                            showToast(it)
                            dismiss()
                        }
                    }

                    is ForgotPasswordViewState.ErrorMessage -> {
                        showToast(state.errorMessage)
                    }
                    else -> {}
                }
            }.autoDispose()
        }
    }
}
