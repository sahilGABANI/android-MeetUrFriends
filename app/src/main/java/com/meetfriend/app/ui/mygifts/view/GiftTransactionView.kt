package com.meetfriend.app.ui.mygifts.view

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.meetfriend.app.R
import com.meetfriend.app.api.gift.model.GiftTransaction
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewGiftHistoryItemBinding
import com.meetfriend.app.utils.FileUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GiftTransactionView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val historyItemClicksSubject: PublishSubject<GiftTransaction> =
        PublishSubject.create()
    val historyItemClicks: Observable<GiftTransaction> = historyItemClicksSubject.hide()


    private lateinit var binding: ViewGiftHistoryItemBinding
    private lateinit var giftTransactionInfo: GiftTransaction

    private lateinit var giftsHistoryAdapter: GiftsHistoryAdapter


    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_gift_history_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewGiftHistoryItemBinding.bind(view)

        giftsHistoryAdapter = GiftsHistoryAdapter(context)

        binding.apply {
            rvTargetData.adapter = giftsHistoryAdapter
            rvTargetData.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

    }

    fun bind(giftTransactionInfo: GiftTransaction) {
        this.giftTransactionInfo = giftTransactionInfo

        if(FileUtils.isStringToday(giftTransactionInfo.date.toString())) {
            binding.tvTargetDate.text = context.getString(R.string.label_today)
        } else {
            binding.tvTargetDate.text = giftTransactionInfo.date

        }
        giftsHistoryAdapter.listOfDataItems = giftTransactionInfo.data
    }
}