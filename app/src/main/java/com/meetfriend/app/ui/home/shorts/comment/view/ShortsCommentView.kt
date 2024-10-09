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
import com.meetfriend.app.databinding.ViewShortsCommentBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.video.Post_comments
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ShortsCommentView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val commentClicksSubject: PublishSubject<ShortsCommentState> = PublishSubject.create()
    val commentClicks: Observable<ShortsCommentState> = commentClicksSubject.hide()

    private val replyCommentClicksSubject: PublishSubject<ShortsCommentState> =
        PublishSubject.create()
    val replyCommentClicks: Observable<ShortsCommentState> = replyCommentClicksSubject.hide()

    private lateinit var binding: ViewShortsCommentBinding
    private lateinit var postComments: Post_comments
    lateinit var shortsCommentReplyAdapter: ShortsCommentReplyAdapter
    private var childCommentIsHide = true

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_shorts_comment, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewShortsCommentBinding.bind(view)

        MeetFriendApplication.component.inject(this)

        binding.apply {
            shortsCommentReplyAdapter = ShortsCommentReplyAdapter(context).apply {
                replyCommentClicks.subscribe { replyCommentClicksSubject.onNext(it) }
            }
            rvReply.adapter = shortsCommentReplyAdapter

            tvDelete.throttleClicks().subscribeAndObserveOnMainThread {
                commentClicksSubject.onNext(ShortsCommentState.DeleteClick(postComments))
            }.autoDispose()

            tvReply.throttleClicks().subscribeAndObserveOnMainThread {
                commentClicksSubject.onNext(ShortsCommentState.ReplyClick(postComments))
            }.autoDispose()

            llViewReplies.throttleClicks().subscribeAndObserveOnMainThread {
                if (childCommentIsHide) {
                    binding.rvReply.isVisible = true
                    childCommentIsHide = false
                    binding.tvViewMoreReplies.text = resources.getText(R.string.label_hide_reply)
                } else {
                    binding.rvReply.isVisible = false
                    childCommentIsHide = true
                    binding.tvViewMoreReplies.text = resources.getString(
                        R.string.view_more_replies,
                        postComments.child_comments.size.toString()
                    )
                }
            }.autoDispose()

            tvEdit.throttleClicks().subscribeAndObserveOnMainThread {
                commentClicksSubject.onNext(ShortsCommentState.EditClick(postComments))
            }.autoDispose()

            tvComment.setOnMentionClickListener { view, text ->
                commentClicksSubject.onNext(
                    ShortsCommentState.MentionUserClick(
                        text.toString(),
                        postComments
                    )
                )
            }

            tvComment.isHyperlinkEnabled = true
            tvComment.hyperlinkColor = resources.getColor(R.color.sky_blue)
        }
    }

    fun bind(postComments: Post_comments) {
        this.postComments = postComments

        binding.tvName.text =
            if (!postComments.user?.userName.isNullOrEmpty() && postComments.user?.userName != "null") {
                postComments.user?.userName
            } else {
                postComments.user?.firstName.plus(
                    " "
                ).plus(postComments.user?.lastName)
            }
        binding.tvComment.text = postComments.content
        binding.tvTime.text = postComments.created_at

        if (postComments.user?.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        Glide.with(this)
            .load(postComments.user?.profile_photo)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfile)
        binding.tvDelete.isVisible = postComments.user?.id == loggedInUserCache.getLoggedInUserId()
        binding.tvEdit.isVisible = postComments.user?.id == loggedInUserCache.getLoggedInUserId()
        binding.tvReply.isVisible = postComments.user?.id != loggedInUserCache.getLoggedInUserId()
        shortsCommentReplyAdapter.listOfDataItems = postComments.child_comments

        if (!postComments.child_comments.isNullOrEmpty()) {
            if (childCommentIsHide) {
                binding.llViewReplies.isVisible = true
                binding.tvViewMoreReplies.text = resources.getString(
                    R.string.view_more_replies,
                    postComments.child_comments.size.toString()
                )
                binding.rvReply.isVisible = false
            } else {
                binding.rvReply.isVisible = true
                childCommentIsHide = false
                binding.tvViewMoreReplies.text = resources.getText(R.string.label_hide_reply)
            }
        } else {
            binding.llViewReplies.isVisible = false
        }
    }
}
