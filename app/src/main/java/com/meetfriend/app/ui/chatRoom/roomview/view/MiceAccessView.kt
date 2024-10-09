package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.MiceAccessInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewMiceAccessListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MiceAccessView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val removeMiceAccessClicksSubject: PublishSubject<MiceAccessInfo> =
        PublishSubject.create()
    val removeMiceAccessClicks: Observable<MiceAccessInfo> = removeMiceAccessClicksSubject.hide()

    private lateinit var binding: ViewMiceAccessListBinding

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_mice_access_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewMiceAccessListBinding.bind(view)
    }

    fun bind(miceAccessInfo: MiceAccessInfo) {
        binding.apply {
            tvuserName.text = miceAccessInfo.chatUserName
            ivRemove.throttleClicks().subscribeAndObserveOnMainThread {
                removeMiceAccessClicksSubject.onNext(miceAccessInfo)
            }.autoDispose()
        }

    }
}