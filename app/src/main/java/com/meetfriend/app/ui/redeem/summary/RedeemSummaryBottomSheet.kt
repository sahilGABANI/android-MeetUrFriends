package com.meetfriend.app.ui.redeem.summary

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetRedeemSummaryBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant

class RedeemSummaryBottomSheet : BaseBottomSheetDialogFragment() {
    companion object {

        private const val TOTAL_REDEEMABLE_AMOUNT = "totalRedeemableAmount"

        fun newInstance(totalAmount: String): RedeemSummaryBottomSheet {
            val args = Bundle()
            totalAmount.let { args.putString(TOTAL_REDEEMABLE_AMOUNT, it) }
            val fragment = RedeemSummaryBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: BottomSheetRedeemSummaryBinding? = null
    private val binding get() = _binding!!

    private var amount: String = "0.0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRedeemSummaryBinding.inflate(inflater, container, false)
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

        amount = arguments?.getString(TOTAL_REDEEMABLE_AMOUNT)
            ?: "0.0"

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
        binding.tvRedeemAmount.text = Constant.CURRENCY_SYMBOL.plus(String.format("%.2f", amount.toFloat()))
        val totalAmount = (amount.toFloat() * 4.0)/100.0
        val final = amount.toFloat() - totalAmount
        binding.tvCharges.text = Constant.CURRENCY_SYMBOL.plus(String.format("%.2f", totalAmount))
        binding.tvTotalEarning.text = Constant.CURRENCY_SYMBOL.plus(String.format("%.2f", final))

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }


}