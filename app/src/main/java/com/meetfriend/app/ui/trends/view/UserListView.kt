package com.meetfriend.app.ui.trends.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ItemviewUserListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class UserListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val hashtagItemClicksSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val hashtagItemClicks: Observable<MeetFriendUser> = hashtagItemClicksSubject.hide()

    private lateinit var userInfo: MeetFriendUser
    private lateinit var binding: ItemviewUserListBinding

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.itemview_user_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ItemviewUserListBinding.bind(view)

        MeetFriendApplication.component.inject(this)

        binding.apply {
            hashTagItemRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
                hashtagItemClicksSubject.onNext(userInfo)
            }.autoDispose()
        }
    }

    fun bind(hashtagInfo: MeetFriendUser) {
        this.userInfo = hashtagInfo
        binding.hashTagNameAppCompatTextView.text = "${hashtagInfo.tagName?.replace("#", "")}"
        binding.viewCountAppCompatTextView.text = "${
            (hashtagInfo.totalPosts ?: 0).plus(hashtagInfo.totalShorts ?: 0).plus(hashtagInfo.totalChallenges ?: 0)
        } views"
    }
}