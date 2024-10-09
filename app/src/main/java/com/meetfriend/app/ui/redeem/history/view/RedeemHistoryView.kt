package com.meetfriend.app.ui.redeem.history.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.meetfriend.app.R
import com.meetfriend.app.api.redeem.model.RedeemHistoryInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewRedeemHistoryBinding
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FileUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class RedeemHistoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val historyItemClicksSubject: PublishSubject<RedeemHistoryInfo> =
        PublishSubject.create()
    val historyItemClicks: Observable<RedeemHistoryInfo> = historyItemClicksSubject.hide()


    private lateinit var binding: ViewRedeemHistoryBinding
    private lateinit var redeemHistoryInfo: RedeemHistoryInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_redeem_history, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewRedeemHistoryBinding.bind(view)

        binding.apply {

        }

    }

    fun bind(info: RedeemHistoryInfo) {
        this.redeemHistoryInfo = info

        binding.apply {
            tvTaskDate.text = FileUtils.redeemHistoryTime(info.createdAt ?: "")

            val convertedAmount :Float = (info.amount?.toFloat()?: 0F) * Constant.EXCHANGE_RATE.toFloat()
            tvAmount.text = Constant.CURRENCY_SYMBOL.plus(String.format("%.2f", convertedAmount))
            when (info.status) {
                0 -> {
                    tvStatus.text = resources.getString(R.string.label_pending)
                    setCardBackgroundColor(R.color.yellow_pending)
                    tvStatus.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.yellow_pending_light);
                }
                1 -> {
                    tvStatus.text = resources.getString(R.string.label_approved)
                    setCardBackgroundColor(R.color.mantis)
                    tvStatus.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.mantis_light);
                }
                else -> {
                    tvStatus.text = resources.getString(R.string.label_rejected)
                    setCardBackgroundColor(R.color.carmine_pink)
                    tvStatus.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.carmine_pink_light);
                }
            }

            val backgroundDrawable =
                ContextCompat.getDrawable(context, R.drawable.bg_join_container)
            tvStatus.background = backgroundDrawable
        }
    }

    private fun setCardBackgroundColor(color: Int) {
        val outerCardViewColor = ContextCompat.getColorStateList(
            context,
            color
        )
        binding.apply {
            outerCardView.setCardBackgroundColor(outerCardViewColor)
            tvStatus.setTextColor(outerCardViewColor)
            tvStatus.setBackgroundColor(color)

        }
    }
}