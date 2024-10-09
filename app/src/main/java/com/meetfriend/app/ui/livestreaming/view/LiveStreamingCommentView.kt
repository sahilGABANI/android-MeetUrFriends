package com.meetfriend.app.ui.livestreaming.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.MessageType
import com.meetfriend.app.api.livestreaming.model.LiveEventSendOrReadComment
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewLiveEventCommentBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveStreamingCommentView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val commentClicksSubject: PublishSubject<LiveEventSendOrReadComment> =
        PublishSubject.create()
    val commentClicks: Observable<LiveEventSendOrReadComment> = commentClicksSubject.hide()

    private lateinit var binding: ViewLiveEventCommentBinding
    private lateinit var liveEventSendOrReadComment: LiveEventSendOrReadComment

    private lateinit var comment: LiveEventSendOrReadComment

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_live_event_comment, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewLiveEventCommentBinding.bind(view)

        binding.apply {
            userNameLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                commentClicksSubject.onNext(comment)
            }
        }
    }

    fun bind(liveEventSendOrReadComment: LiveEventSendOrReadComment) {
        comment = liveEventSendOrReadComment

        this.liveEventSendOrReadComment = liveEventSendOrReadComment

        binding.let {
            it.userNameTextView.text = liveEventSendOrReadComment.username ?: ""

            it.userCommentTextView.text = liveEventSendOrReadComment.comment ?: ""
            Glide.with(context)
                .load(
                    if (liveEventSendOrReadComment.type == MessageType.JoinF.toString()) {
                        R.drawable.ic_app_logo_
                    } else {
                        liveEventSendOrReadComment.profileUrl
                            ?: ""
                    }
                )
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .into(it.ivProfileImage)

            binding.ivUserVerified.isVisible = liveEventSendOrReadComment.isVerified == 1
        }
    }
}
