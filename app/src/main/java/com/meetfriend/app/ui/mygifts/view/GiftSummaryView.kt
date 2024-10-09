package com.meetfriend.app.ui.mygifts.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.api.gift.model.GiftWeeklyInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewGiftSummaryItemBinding
import com.meetfriend.app.utils.Constant
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GiftSummaryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val giftItemClicksSubject: PublishSubject<GiftWeeklyInfo> =
        PublishSubject.create()
    val giftItemClicks: Observable<GiftWeeklyInfo> = giftItemClicksSubject.hide()


    private lateinit var binding: ViewGiftSummaryItemBinding
    private lateinit var giftWeeklyInfo: GiftWeeklyInfo


    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_gift_summary_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewGiftSummaryItemBinding.bind(view)


        binding.apply {

        }

    }

    fun bind(giftWeeklyInfo: GiftWeeklyInfo) {
        this.giftWeeklyInfo = giftWeeklyInfo

        binding.tvTargetDate.text =
            giftWeeklyInfo.startDate.toString().plus(" - ").plus(giftWeeklyInfo.endDate.toString())
        binding.tvReceivedCoin.text =
            String.format("%.2f", giftWeeklyInfo.totalRecieved?.toDouble())
        binding.tvSentCoin.text =
            String.format("%.2f", giftWeeklyInfo.totalSent?.toDouble())
        var data = String.format(
            "%.2f",
            ((giftWeeklyInfo.coinBalance
                ?: 0.0) * Constant.CENT_VALUE.toDouble() * Constant.EXCHANGE_RATE.toDouble())
        )
        if (data.toDouble() < 0.0) {
            data = "0.0"
        }
        binding.tvTotalEarning.text = Constant.CURRENCY_SYMBOL.plus(" ").plus(data)
        binding.tvPreviousCoin.text =
            String.format("%.2f", giftWeeklyInfo.previousCoinBalance?.toDouble())
        binding.tvBalanceCoin.text =
            String.format("%.2f", giftWeeklyInfo.coinBalance?.toDouble())

    }
}