package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.graphics.PorterDuff
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.ChildCommentItem
import com.meetfriend.app.api.post.model.ChallengeCommentReplyState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewChallengeCommentReplayBinding
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ChallengeCommentReplayView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val commentItemClickSubject: PublishSubject<ChallengeCommentReplyState> =
        PublishSubject.create()
    val commentItemClick: Observable<ChallengeCommentReplyState> = commentItemClickSubject.hide()
    private lateinit var binding: ViewChallengeCommentReplayBinding

    private lateinit var result: ChildCommentItem

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()

    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_challenge_comment_replay, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewChallengeCommentReplayBinding.bind(view)
        MeetFriendApplication.component.inject(this)

        binding.apply {

            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                commentItemClickSubject.onNext(
                    ChallengeCommentReplyState.UserProfileClick(
                        result.userId ?: 0
                    )
                )
            }.autoDispose()

            ivLike.throttleClicks().subscribeAndObserveOnMainThread {
                updateLikeStatusCount()

                commentItemClickSubject.onNext(
                    ChallengeCommentReplyState.AddReplyCommentLikeClick(
                        result
                    )
                )
            }.autoDispose()
            ivDelete.throttleClicks().subscribeAndObserveOnMainThread {
                commentItemClickSubject.onNext(ChallengeCommentReplyState.ReplyDeleteClick(result))
            }.autoDispose()
            editAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                commentItemClickSubject.onNext(ChallengeCommentReplyState.ReplyEditClick(result))
            }

            tvComment.setOnMentionClickListener { _, text ->
                result.mentionComments?.find { it.user?.userName == text.toString() }?.apply {
                    commentItemClickSubject.onNext(
                        ChallengeCommentReplyState.UserProfileClick(
                            this.mentionUserId ?: 0
                        )
                    )
                }
            }
        }
    }

    fun bind(result: ChildCommentItem) {
        this.result = result
        binding.tvName.text =
            if (!result.user?.userName.isNullOrEmpty() && result.user?.userName != "null") result.user?.userName else result.user?.firstName.plus(
                " "
            )
                .plus(result.user?.lastName)

        binding.tvComment.text = result.content
        binding.tvTime.text = result.createdAt

        if (result.user?.isVerified == 1){
            binding.ivAccountVerified.visibility= View.VISIBLE
        }else{
            binding.ivAccountVerified.visibility= View.GONE
        }

        Glide.with(this)
            .load(result.user?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfile)
        updateReelLike()

        binding.ivDelete.isVisible = result.user?.id == loggedInUserCache.getLoggedInUserId()
        binding.editAppCompatImageView.isVisible = result.user?.id == loggedInUserCache.getLoggedInUserId()
    }


    private fun updateReelLike() {
        binding.apply {
            if (result.isLikedCount == 1) {
                ivLike.setImageResource(R.drawable.ic_fill_heart)
                val tintColor = ContextCompat.getColor(context, R.color.like_tint_color)
                ivLike.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
            } else {
                ivLike.setImageResource(R.drawable.ic_heart)
                val tintColor = ContextCompat.getColor(context, R.color.your_tint_color)
                ivLike.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
            }

            val totalLikes = result.noOfLikesCount
            if (totalLikes != null) {
                if (totalLikes != 0) {
                    tvLikeCount.text = totalLikes.prettyCount().toString()
                    tvLikeCount.visibility = View.VISIBLE
                } else {
                    tvLikeCount.text = "0"
                    tvLikeCount.visibility = View.VISIBLE
                }
            } else {
                tvLikeCount.text = "0"
                tvLikeCount.visibility = View.VISIBLE
            }
        }
    }

    private fun updateLikeStatusCount() {
        if (result.isLikedCount == 0) {
            result.isLikedCount = 1
        } else {
            result.isLikedCount = 0
        }

        if (result.isLikedCount == 1) {
            result.noOfLikesCount = result.noOfLikesCount?.let { it + 1 } ?: 0
            updateReelLike()
        } else {
            result.noOfLikesCount = result.noOfLikesCount?.let { it - 1 } ?: 0
            updateReelLike()
        }
    }


}