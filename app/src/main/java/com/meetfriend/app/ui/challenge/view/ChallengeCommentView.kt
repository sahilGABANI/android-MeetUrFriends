package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.graphics.PorterDuff
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.ChallengeComment
import com.meetfriend.app.api.post.model.ChallengeCommentReplyState
import com.meetfriend.app.api.post.model.ChallengeCommentState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewChallengeCommentScreenBinding
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ChallengeCommentView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val commentItemClickSubject: PublishSubject<ChallengeCommentState> =
        PublishSubject.create()
    val commentItemClick: Observable<ChallengeCommentState> = commentItemClickSubject.hide()
    private lateinit var binding: ViewChallengeCommentScreenBinding

    private lateinit var result: ChallengeComment
    private lateinit var challengeCommentReplayAdapter: ChallengeCommentReplayAdapter
    private var childCommentIsHide = true

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_challenge_comment_screen, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewChallengeCommentScreenBinding.bind(view)
        MeetFriendApplication.component.inject(this)
        initAdapter()
        binding.apply {
            tvReplyComment.throttleClicks().subscribeAndObserveOnMainThread {
                commentItemClickSubject.onNext(ChallengeCommentState.ReplyClick(result))
            }.autoDispose()
            llViewReplies.throttleClicks().subscribeAndObserveOnMainThread {
                if (childCommentIsHide) {
                    binding.rvChildComment.isVisible = true
                    childCommentIsHide = false
                    binding.tvViewMoreReplies.text = resources.getText(R.string.label_hide_reply)
                } else {
                    binding.rvChildComment.isVisible = false
                    childCommentIsHide = true
                    binding.tvViewMoreReplies.text = resources.getString(
                        R.string.view_more_replies,
                        result.childComments?.size.toString()
                    )
                }
            }.autoDispose()

            ivLike.throttleClicks().subscribeAndObserveOnMainThread {
                updateLikeStatusCount()
                commentItemClickSubject.onNext(ChallengeCommentState.AddCommentLikeClick(result))
            }.autoDispose()
            ivDelete.throttleClicks().subscribeAndObserveOnMainThread {
                commentItemClickSubject.onNext(ChallengeCommentState.DeleteClick(result))
            }.autoDispose()

            editAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                commentItemClickSubject.onNext(ChallengeCommentState.EditClick(result))
            }
            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                commentItemClickSubject.onNext(
                    ChallengeCommentState.UserProfileClick(
                        result.userId
                    )
                )
            }
            tvComment.setOnMentionClickListener { _, text ->
                result.mentionComments?.find { it.user?.userName == text.toString() }?.apply {
                    commentItemClickSubject.onNext(
                        ChallengeCommentState.UserProfileClick(
                            this.mentionUserId ?: 0
                        )
                    )
                }
            }
        }
    }

    private fun initAdapter() {
        challengeCommentReplayAdapter = ChallengeCommentReplayAdapter(context).apply {
            commentItemClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is ChallengeCommentReplyState.ReplyDeleteClick -> {
                        commentItemClickSubject.onNext(
                            ChallengeCommentState.ReplyDeleteClick(
                                result,
                                state.childComment
                            )
                        )
                    }

                    is ChallengeCommentReplyState.ReplyEditClick -> {
                        commentItemClickSubject.onNext(ChallengeCommentState.ReplyEditClick(state.childComment))
                    }

                    is ChallengeCommentReplyState.AddReplyCommentLikeClick -> {
                        commentItemClickSubject.onNext(
                            ChallengeCommentState.AddReplyCommentLikeClick(
                                state.dataVideo
                            )
                        )
                    }

                    is ChallengeCommentReplyState.UserProfileClick -> {
                        commentItemClickSubject.onNext(ChallengeCommentState.UserProfileClick(state.userId))
                    }

                    else -> {}
                }
            }.autoDispose()
        }
        binding.rvChildComment.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = challengeCommentReplayAdapter
        }
    }

    fun bind(result: ChallengeComment) {
        this.result = result
        binding.tvName.text =
            if (!result.user?.userName.isNullOrEmpty() &&
                result.user?.userName != "null"
            ) {
                result.user?.userName
            } else {
                result.user?.firstName.plus(" ")
                    .plus(result.user?.lastName)
            }

        binding.tvComment.text = result.content
        binding.tvTime.text = result.createdAt

        if (result.user?.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        Glide.with(this)
            .load(result.user?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfile)
        updateReelLike()

        binding.ivDelete.isVisible = result.user?.id == loggedInUserCache.getLoggedInUserId()
        binding.editAppCompatImageView.isVisible = result.user?.id == loggedInUserCache.getLoggedInUserId()
        binding.tvReplyComment.isVisible = result.user?.id != loggedInUserCache.getLoggedInUserId()
        challengeCommentReplayAdapter.listOfDataItems = result.childComments

        if (!result.childComments.isNullOrEmpty()) {
            if (childCommentIsHide) {
                binding.llViewReplies.isVisible = true
                binding.tvViewMoreReplies.text = resources.getString(
                    R.string.view_more_replies,
                    result.childComments?.size.toString()
                )
                binding.rvChildComment.isVisible = false
            } else {
                binding.rvChildComment.isVisible = true
                childCommentIsHide = false
                binding.tvViewMoreReplies.text = resources.getText(R.string.label_hide_reply)
            }
        } else {
            binding.llViewReplies.isVisible = false
        }
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
        result.isLikedCount = if (result.isLikedCount == 0) 1 else 0

        result.noOfLikesCount = if (result.isLikedCount == 1) {
            (result.noOfLikesCount ?: 0).plus(1)
        } else {
            if (result.noOfLikesCount ?: 0 > 0) (result.noOfLikesCount ?: 0).minus(1) else 0
        }
        updateReelLike()
    }
}
