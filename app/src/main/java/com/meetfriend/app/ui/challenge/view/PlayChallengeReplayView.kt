package com.meetfriend.app.ui.challenge.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.ChallengePostInfo
import com.meetfriend.app.api.post.model.ChallengePostPageState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewPlayChallengeReplayBinding
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.videoplayer.JZMediaExoKotlin
import com.meetfriend.app.videoplayer.JzvdStdOutgoer
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class PlayChallengeReplayView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val playShortsViewClicksSubject: PublishSubject<ChallengePostPageState> =
        PublishSubject.create()
    val playShortsViewClicks: Observable<ChallengePostPageState> =
        playShortsViewClicksSubject.hide()


    private var binding: ViewPlayChallengeReplayBinding? = null
    private lateinit var dataVideo: ChallengePostInfo


    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()


    init {
        inflateUi()
    }

    private fun inflateUi() {
        MeetFriendApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        val view = View.inflate(context, R.layout.view_play_challenge_replay, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding = ViewPlayChallengeReplayBinding.bind(view)

        binding?.apply {

            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ChallengePostPageState.UserProfileClick(dataVideo))
            }.autoDispose()

            likeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                updateLikeStatusCount()
            }.autoDispose()

            ivComment.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ChallengePostPageState.CommentClick(dataVideo))
            }.autoDispose()

            moreAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ChallengePostPageState.MoreClick(dataVideo))
            }.autoDispose()
            ivDownload.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ChallengePostPageState.ShareClick(dataVideo))
            }.autoDispose()

            watchCountAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ChallengePostPageState.ShareClick(dataVideo))
            }.autoDispose()

            profileRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(
                    ChallengePostPageState.UserChallengeReplyProfileClick(
                        dataVideo
                    )
                )
            }.autoDispose()

            llJoin.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(
                    ChallengePostPageState.JoinChallengeClick(
                        dataVideo
                    )
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(videoData: ChallengePostInfo) {
        this.dataVideo = videoData


        binding?.apply {
            tvUserName.text =
                if (!videoData.user?.userName.isNullOrEmpty() && videoData.user?.userName != "null") context.getString(
                    R.string.sign_at_the_rate
                ).plus(
                    videoData.user?.userName
                ) else context.getString(R.string.sign_at_the_rate).plus(
                    videoData.user?.firstName
                ).plus("_").plus(
                    videoData.user?.lastName
                )
            tvName.text =  if (!videoData.user?.userName.isNullOrEmpty() && videoData.user?.userName != "null") videoData.user?.userName
             else videoData.user?.firstName.plus(
                " "
            ).plus(videoData.user?.lastName)

            if (videoData.user?.isVerified == 1){
                ivAccountVerified.visibility= View.VISIBLE
            }else{
                ivAccountVerified.visibility= View.GONE
            }

            Glide.with(context)
                .load(dataVideo.user?.profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivUserProfileImage)
            tvChallengeDescription.text = dataVideo.description
            tvLikeCount.text = dataVideo.noOfLikesCount.toString()
            tvWatchCount.text = dataVideo.noOfViewsCount.toString()
            tvCommentCount.text = dataVideo.noOfCommentCount.toString()
            updateReelLike()
            tvShortsTime.text = videoData.createdAt

            if (dataVideo.getIsFilePathImage()) {
                jzvdStdOutgoer.isVisible = false
                photoView.isVisible = true
                Glide.with(context)
                    .load(videoData.filePath)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(photoView)
            } else {
                jzvdStdOutgoer.isVisible = true
                photoView.isVisible = false
                jzvdStdOutgoer.videoUrl = videoData.filePath
                val player: JzvdStdOutgoer? = jzvdStdOutgoer
                player?.apply {
                    val jzDataSource = JZDataSource(this.videoUrl)
                    jzDataSource.looping = true
                    this.setUp(
                        jzDataSource,
                        Jzvd.SCREEN_NORMAL,
                        JZMediaExoKotlin::class.java
                    )
                    startVideoAfterPreloading()
                }
            }

        }
    }

    private fun updateReelLike() {
        binding?.apply {
            if (dataVideo.isLikedCount == 1) {
                likeAppCompatImageView.setImageResource(R.drawable.ic_fill_heart)
            } else {
                likeAppCompatImageView.setImageResource(R.drawable.ic_heart)
            }

            tvLikeCount.text = dataVideo.noOfLikesCount?.prettyCount().toString()
        }
    }

    private fun updateLikeStatusCount() {
        if (dataVideo.isLikedCount == 0) {
            dataVideo.isLikedCount = 1
        } else {
            dataVideo.isLikedCount = 0
        }

        if (dataVideo.isLikedCount == 1) {
            dataVideo.noOfLikesCount = dataVideo.noOfLikesCount?.let { it + 1 } ?: 0
            updateReelLike()

            playShortsViewClicksSubject.onNext(ChallengePostPageState.AddReelLikeClick(dataVideo))
        } else {
            dataVideo.noOfLikesCount = dataVideo.noOfLikesCount?.let { it - 1 } ?: 0
            updateReelLike()
            playShortsViewClicksSubject.onNext(ChallengePostPageState.RemoveReelLikeClick(dataVideo))
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}