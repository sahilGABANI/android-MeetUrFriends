package com.meetfriend.app.ui.coins.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.meetfriend.app.R
import com.meetfriend.app.api.gift.model.CoinPlanInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewCoinPlansBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CoinPlanView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val planItemClicksSubject: PublishSubject<CoinPlanInfo> = PublishSubject.create()
    val planItemClicks: Observable<CoinPlanInfo> = planItemClicksSubject.hide()


    private lateinit var binding: ViewCoinPlansBinding
    private lateinit var coinPlanInfo: CoinPlanInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_coin_plans, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewCoinPlansBinding.bind(view)

        binding.apply {
            cvContainer.throttleClicks().subscribeAndObserveOnMainThread {
                planItemClicksSubject.onNext(coinPlanInfo)
            }.autoDispose()
        }
    }

    fun bind(coinPlanInfo: CoinPlanInfo, isSelected: Boolean) {
        this.coinPlanInfo = coinPlanInfo

        binding.apply {
            totalCoinsAppCompatTextView.text = coinPlanInfo.coins
            if (coinPlanInfo.discount.toString()
                    .equals("0", ignoreCase = true) || coinPlanInfo.discount.toString()
                    .equals("", ignoreCase = true)
            ) {
                discountAppCompatTextView.visibility = GONE
                mDiscountCount.visibility = GONE
            } else {
                mDiscountCount.text = resources.getString(R.string.label_plus_sign)
                    .plus(coinPlanInfo.discountCoins.toString())
            }

            priceAppCompatTextView.text = context.getString(R.string.sign_dollar).plus(
                coinPlanInfo.amount!!.toDouble()
                    .minus(if (coinPlanInfo.amount!!.toInt() == 10) 0.04 else 0.01)
            )

        }

        if (isSelected) {
            binding.planDataLinearLayout.background =
                ContextCompat.getDrawable(context, R.drawable.bg_selected_coin_plan)
        } else {
            binding.planDataLinearLayout.background = null
        }

    }
}