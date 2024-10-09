package com.meetfriend.app.ui.redeem

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.redeem.model.RedeemRequestRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomsheetRedeemAmountBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.redeem.summary.RedeemSummaryBottomSheet
import com.meetfriend.app.ui.redeem.viewmodel.RedeemAmountViewModel
import com.meetfriend.app.ui.redeem.viewmodel.RedeemAmountViewState
import com.meetfriend.app.utils.Constant
import contractorssmart.app.utilsclasses.CommonMethods
import javax.inject.Inject

class RedeemAmountBottomSheet : BaseBottomSheetDialogFragment() {


    companion object {

        private const val TOTAL_REDEEMABLE_AMOUNT = "totalRedeemableAmount"
        private const val IS_FROM_MONETIZATION = "isFromMonetization"

        fun newInstance(totalAmount: String, isFromMonetization: Int): RedeemAmountBottomSheet {
            val args = Bundle()
            totalAmount.let { args.putString(TOTAL_REDEEMABLE_AMOUNT, it) }
            isFromMonetization.let { args.putInt(IS_FROM_MONETIZATION, it) }
            val fragment = RedeemAmountBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: BottomsheetRedeemAmountBinding? = null
    private val binding get() = _binding!!


    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<RedeemAmountViewModel>
    private lateinit var redeemAmountViewModel: RedeemAmountViewModel

    private var amount: String = "0.0"
    private var isFromMonetization: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetRedeemAmountBinding.inflate(inflater, container, false)
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

        MeetFriendApplication.component.inject(this)
        redeemAmountViewModel = getViewModelFromFactory(viewModelFactory)

        amount = arguments?.getString(TOTAL_REDEEMABLE_AMOUNT)
            ?: "0.0"
        isFromMonetization = arguments?.getInt(IS_FROM_MONETIZATION)
            ?: 0
        listenToViewModel()
        listenToViewEvent()


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

    private fun listenToViewEvent() {
        var otp: String? = null

        binding.tvCurrencyCode.text = Constant.CURRENCY_SYMBOL
        binding.etAmount.setText(amount)
        binding.otpView.setOtpCompletionListener {
            otp = it
            hideKeyboard()
        }

        binding.tvSendOTP.throttleClicks().subscribeAndObserveOnMainThread {
            val minAmount = 50.0 * Constant.EXCHANGE_RATE.toFloat()
            if (binding.etAmount.text.isNullOrEmpty()) {
                showToast(getString(R.string.label_enter_redeem_amount))
            } else if (binding.etAmount.text.toString().toFloat() <= minAmount) {
                showToast(getString(R.string.label_amount_must_be_at_least).plus(" ").plus(Constant.CURRENCY_SYMBOL).plus(String.format("%.2f",minAmount)))
            } else if ((binding.etAmount.text.toString().toFloat() > amount.toFloat())) {
                showToast(getString(R.string.validation_redeem_amount_must_be_lower))
            } else if (binding.etEmail.text.isNullOrEmpty()) {
                showToast(getString(R.string.label_enter_paypal_email_id))
            } else if (!CommonMethods.isValidEmail(binding.etEmail.text.toString().trim())) {
                showToast(getString(R.string.label_enter_valid_paypal_email_id))
            } else {
                callAPI()
            }

        }.autoDispose()

        binding.tvSubmit.throttleClicks().subscribeAndObserveOnMainThread {
            if (otp != null) {
                val redeemAmount =
                    binding.etAmount.text.toString().toFloat() / Constant.EXCHANGE_RATE.toFloat()

                redeemAmountViewModel.verifyRedeemOTP(
                    RedeemRequestRequest(
                        amount = redeemAmount.toString(),
                        paypalEmail = binding.etEmail.text.toString(),
                        otp = otp,
                        isMonetization = isFromMonetization,
                        serviceCharge = 4
                    )
                )
            } else {
                showToast(resources.getString(R.string.label_enter_otp))
            }

        }.autoDispose()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            this.dismiss()
        }.autoDispose()

        binding.tvResend.throttleClicks().subscribeAndObserveOnMainThread {
            callAPI()
        }.autoDispose()
    }

    private fun callAPI() {
        val redeemAmount =
            binding.etAmount.text.toString().toFloat() / Constant.EXCHANGE_RATE.toFloat()
        redeemAmountViewModel.sendRedeemRequest(
            RedeemRequestRequest(
                amount = redeemAmount.toString(),
                paypalEmail = binding.etEmail.text.toString(),
                isMonetization = isFromMonetization
            )
        )
    }

    private fun listenToViewModel() {
        redeemAmountViewModel.redeemAmountState.subscribeAndObserveOnMainThread {

            when (it) {
                is RedeemAmountViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is RedeemAmountViewState.LoadingState -> {
                    binding.flSubmitContainer.isVisible = !it.isLoading
                    binding.progressBar.isVisible = it.isLoading
                }
                is RedeemAmountViewState.RequestSuccess -> {
                    updateUI()
                }
                is RedeemAmountViewState.VerifyOTPSuccess -> {
                    showToast(it.successMessage)
                    openSummaryBottomSheet()
                    dismiss()
                }
            }
        }.autoDispose()
    }

    private fun openSummaryBottomSheet() {
        val bottomSheet = RedeemSummaryBottomSheet.newInstance(binding.etAmount.text.toString())
        bottomSheet.show(childFragmentManager, RedeemSummaryBottomSheet::class.java.name)
    }

    private fun updateUI() {
        binding.llOtpContainer.isVisible = true
        binding.tvSubmit.isVisible = true
        binding.tvSendOTP.isVisible = false
        binding.etEmail.isEnabled = false
        binding.etAmount.isEnabled = false
    }

}