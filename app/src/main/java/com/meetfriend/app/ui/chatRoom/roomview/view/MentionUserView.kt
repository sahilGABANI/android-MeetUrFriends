package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.post.model.HashTagsResponse
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewMentionUserBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MentionUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val userClicksSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val userClicks: Observable<MeetFriendUser> = userClicksSubject.hide()

    private val hashTagClicksSubject: PublishSubject<HashTagsResponse> = PublishSubject.create()
    val hashTagClicks: Observable<HashTagsResponse> = hashTagClicksSubject.hide()

    private lateinit var binding: ViewMentionUserBinding
    private lateinit var mentionUserInfo: MeetFriendUser
    private lateinit var hashTagResponse: HashTagsResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_mention_user, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewMentionUserBinding.bind(view)

        binding.apply {

            llUserInfoContainer.throttleClicks().subscribeAndObserveOnMainThread {
                if(::mentionUserInfo.isInitialized) {
                    userClicksSubject.onNext(mentionUserInfo)
                } else if(::hashTagResponse.isInitialized) {
                    hashTagClicksSubject.onNext(hashTagResponse)
                }
            }
        }
    }

    fun bind(mentionUserInfo: MeetFriendUser) {
        this.mentionUserInfo = mentionUserInfo

        binding.tvUserName.text =
            if (mentionUserInfo.chatUserName.isNullOrEmpty()) mentionUserInfo.userName else mentionUserInfo.chatUserName

    }

    fun bindHashTag(hashTag: HashTagsResponse) {
        this.hashTagResponse = hashTag

        binding.tvUserName.text = hashTag.tagName
    }
}