package com.meetfriend.app.ui.trends.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.api.hashtag.model.HashtagResponse
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ItemviewHashtagListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HashtagListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val hashtagItemClicksSubject: PublishSubject<HashtagResponse> = PublishSubject.create()
    val hashtagItemClicks: Observable<HashtagResponse> = hashtagItemClicksSubject.hide()

    private lateinit var binding: ItemviewHashtagListBinding
    private lateinit var hashtagResponseInfo: HashtagResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.itemview_hashtag_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ItemviewHashtagListBinding.bind(view)

        binding.apply {

            hashTagItemRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
                hashtagItemClicksSubject.onNext(hashtagResponseInfo)
            }.autoDispose()
        }

    }

    fun bind(hashtagInfo: HashtagResponse) {
        this.hashtagResponseInfo = hashtagInfo
        binding.hashTagNameAppCompatTextView.text = "${hashtagResponseInfo.tagName?.replace("#", "")}"
        binding.viewCountAppCompatTextView.text = "${(hashtagResponseInfo.totalPosts ?: 0).plus(hashtagResponseInfo.totalShorts ?: 0).plus(hashtagResponseInfo.totalChallenges ?: 0)} views"
    }
}