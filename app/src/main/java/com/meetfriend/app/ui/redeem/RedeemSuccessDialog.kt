package com.meetfriend.app.ui.redeem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.databinding.DialogRedeemSuccessBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant

class RedeemSuccessDialog : BaseDialogFragment() {


    companion object {

        private const val IS_REDEEM_SUCCESS = "isRedeemSuccess"
        private const val REDEEM_AMOUNT = "redeemAmount"

        fun newInstance(isSuccess: Boolean,amount:String?): RedeemSuccessDialog {
            val args = Bundle()
            isSuccess.let { args.putBoolean(IS_REDEEM_SUCCESS, it) }
            amount.let { args.putString(REDEEM_AMOUNT, it) }
            val fragment = RedeemSuccessDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: DialogRedeemSuccessBinding? = null
    private val binding get() = _binding!!

    private var isSuccess: Boolean? = true
    private var amount: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogRedeemSuccessBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isSuccess = arguments?.getBoolean(IS_REDEEM_SUCCESS)
            ?: true
        amount = arguments?.getString(REDEEM_AMOUNT)

        listenToViewEvent()
    }


    private fun listenToViewEvent() {
        binding.tvRedeemSuccess.isVisible = isSuccess == true
        binding.tvRedeemRejected.isVisible = isSuccess == false
        val redeemAmount = (amount?.toFloat() ?: 0F) * Constant.EXCHANGE_RATE.toFloat()
        binding.tvAmount.text = Constant.CURRENCY_SYMBOL.plus(String.format("%.2f",redeemAmount))

        if (isSuccess == true) {
            Glide.with(this)
                .load(R.drawable.ic_redeem_sucess)
                .into(binding.ivRedeemStatus)

        } else {
            Glide.with(this)
                .load(R.drawable.ic_redeem_reject)
                .into(binding.ivRedeemStatus)
        }
        binding.tvOk.throttleClicks().subscribeAndObserveOnMainThread {
            dialog?.dismiss()
        }.autoDispose()

    }
}