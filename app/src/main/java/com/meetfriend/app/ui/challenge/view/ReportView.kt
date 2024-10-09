package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ReportChallengeItemBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ReportView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val reportItemClickSubject: PublishSubject<String> = PublishSubject.create()
    val reportItemClick: Observable<String> = reportItemClickSubject.hide()

    private var binding: ReportChallengeItemBinding? = null

    private lateinit var result: String

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()

    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.report_challenge_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ReportChallengeItemBinding.bind(view)

        binding?.apply {
            reportTitleAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                reportItemClickSubject.onNext(result)
            }.autoDispose()
        }
    }

    fun bind(result: String) {
        this.result = result
        binding?.reportTitleAppCompatTextView?.text = result
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}