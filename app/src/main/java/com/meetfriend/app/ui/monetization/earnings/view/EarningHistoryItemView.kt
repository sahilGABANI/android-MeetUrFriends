package com.meetfriend.app.ui.monetization.earnings.view

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.core.view.isVisible
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.monetization.model.AmountData
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ItemViewEarningHistoryBinding
import com.meetfriend.app.utils.Constant
import contractorssmart.app.utilsclasses.PreferenceHandler
import contractorssmart.app.utilsclasses.PreferenceHandler.readString
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

class EarningHistoryItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ItemViewEarningHistoryBinding? = null

    private var symbol = ""

    init {
        initUi()
    }

    private fun initUi() {
        var currencyCode = ""
        val view = View.inflate(context, R.layout.item_view_earning_history, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ItemViewEarningHistoryBinding.bind(view.rootView)
        var data = readString(context, "countryCode", "")
        if(data == "") {
            data = Locale.getDefault().toString()
            currencyCode = android.icu.util.Currency.getInstance(Locale(data)).currencyCode ?: ""
        } else {
            currencyCode = android.icu.util.Currency.getInstance(Locale("",data)).currencyCode ?: ""
        }
        symbol = Currency.getInstance(currencyCode).symbol ?: ""

    }

    fun bind(info: AmountData) {
        binding?.apply {
            tvCategory.text =
                context.getString(R.string.label_total).plus(" ").plus(info.categoryType)

            val multiply = String.format(
                "%.2f",
                (info.totalUsd?.toDouble() ?: 0.00) * (Constant.EXCHANGE_RATE.toDouble())
            )
            tvAmount.text = symbol.plus(" ").plus(multiply)
            setAmountInCompleteTask(info)

            val div: Float = (info.totalObject ?: "0").toFloat() / (info.totalTarget ?: 0).toFloat()
            val p = (div * 100)
            val ratio = if (p > 0 && p < 1) 1 else p.toInt()
            progress.progress = ratio

            if (progress.progress < 100) {
                setCardBackgroundColor(R.color.carmine_pink)
            } else {
                setCardBackgroundColor(R.color.mantis)
            }

        }
    }

    private fun setAmountInCompleteTask(info:AmountData) {
        val spanishGray = ContextCompat.getColor(context, R.color.icon_un_Selected)
        val smokyBlack = ContextCompat.getColor(context, R.color.smoky_black)

        binding?.apply {
            val text = SpannableStringBuilder()
                .color(spanishGray) {
                    append((info.totalObject ?: 0).toString())
                }
                .color(smokyBlack) {
                    append("/")
                    append(if(info.categoryType == "live") {(info.totalTarget ?: 100).toString().plus(" Min.")} else {(info.totalTarget ?: 100).toString()})
                }

            tvAmountInCompleteTask.text = text

            tvAmountInCompleteTask.isVisible = info.totalObject == info.totalTarget.toString()

        }
    }

    private fun setCardBackgroundColor(carminePink: Int) {
        val outerCardViewColor = ContextCompat.getColorStateList(
            context,
            carminePink
        )
        binding?.apply {
            outerCardView.setCardBackgroundColor(outerCardViewColor)
            tvAmount.setTextColor(outerCardViewColor)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

}