package com.meetfriend.app.ui.home.shorts.comment.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.post.model.ShortsCommentState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewShortsCommentReplyBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.video.Child_comments
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ShortsCommentReplyView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val replyCommentClicksSubject: PublishSubject<ShortsCommentState> =
        PublishSubject.create()
    val replyCommentClicks: Observable<ShortsCommentState> = replyCommentClicksSubject.hide()

    private lateinit var binding: ViewShortsCommentReplyBinding
    private lateinit var childComments: Child_comments

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_shorts_comment_reply, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewShortsCommentReplyBinding.bind(view)

        MeetFriendApplication.component.inject(this)

        binding.apply {
            tvDelete.throttleClicks().subscribeAndObserveOnMainThread {
                replyCommentClicksSubject.onNext(ShortsCommentState.ReplyDeleteClick(childComments))
            }.autoDispose()

            tvEdit.throttleClicks().subscribeAndObserveOnMainThread {
                replyCommentClicksSubject.onNext(ShortsCommentState.ReplyEditClick(childComments))
            }.autoDispose()

            tvComment.setOnMentionClickListener { view, text ->
                replyCommentClicksSubject.onNext(
                    ShortsCommentState.ReplyMentionUserClick(
                        text.toString(),
                        childComments
                    )
                )
            }
        }
    }

    fun bind(childComments: Child_comments) {
        this.childComments = childComments
        binding.tvName.text =
            if (!childComments.user.userName.isNullOrEmpty() && childComments.user.userName != "null") {
                childComments.user.userName
            } else {
                childComments.user.firstName.plus(
                    " "
                ).plus(childComments.user.lastName)
            }
        binding.tvUserName.text =
            resources.getString(R.string.sign_at_the_rate).plus(childComments.user.userName)
        binding.tvComment.text = childComments.content
        binding.tvTime.text = childComments.created_at

        if (childComments.user.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        Glide.with(this)
            .load(childComments.user.profile_photo)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfile)
        binding.tvDelete.isVisible = childComments.user.id == loggedInUserCache.getLoggedInUserId()
        binding.tvEdit.isVisible = childComments.user.id == loggedInUserCache.getLoggedInUserId()
    }
}
