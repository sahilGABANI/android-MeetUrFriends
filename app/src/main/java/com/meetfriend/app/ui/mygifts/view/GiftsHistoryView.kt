package com.meetfriend.app.ui.mygifts.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.gift.model.GiftTransactionInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewGiftHistoryBinding
import com.meetfriend.app.utils.FileUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GiftsHistoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val historyItemClicksSubject: PublishSubject<GiftTransactionInfo> =
        PublishSubject.create()
    val historyItemClicks: Observable<GiftTransactionInfo> = historyItemClicksSubject.hide()


    private lateinit var binding: ViewGiftHistoryBinding
    private lateinit var giftTransactionInfo: GiftTransactionInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_gift_history, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewGiftHistoryBinding.bind(view)

        binding.apply {

        }

    }

    fun bind(giftTransactionInfo: GiftTransactionInfo) {
        this.giftTransactionInfo = giftTransactionInfo

        val userName = giftTransactionInfo.fromUser?.userName
        if (giftTransactionInfo.fromUser != null) binding.userNameAppCompatTextView.text =
            if(!userName.isNullOrEmpty() && userName != "null") userName else
            giftTransactionInfo.fromUser.firstName + " " + giftTransactionInfo.fromUser.lastName
        binding.totalCoinsAppCompatTextView.text = giftTransactionInfo.coins.toString()
        binding.dateAppCompatTextView.text = FileUtils.convertGiftDateFormat(giftTransactionInfo.createdAt)
        Glide.with(this).load(
            giftTransactionInfo.giftGallery?.file_path
        ).placeholder(R.drawable.ic_gift_chat).into(binding.giftAppCompatImageView)
    }
}