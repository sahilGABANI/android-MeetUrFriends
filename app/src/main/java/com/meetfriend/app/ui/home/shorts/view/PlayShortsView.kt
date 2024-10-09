package com.meetfriend.app.ui.home.shorts.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.post.model.ShortsPageState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewPlayShortsBinding
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.video.DataVideo
import com.meetfriend.app.responseclasses.video.ShortPositionData
import com.meetfriend.app.utils.Constant.FiXED_100_FLOAT
import com.meetfriend.app.utils.Constant.FiXED_100_INT
import com.meetfriend.app.utils.Constant.FiXED_3_INT
import com.meetfriend.app.utils.Constant.FiXED_50_INT
import com.meetfriend.app.utils.Constant.FiXED_MARGIN_40
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class PlayShortsView(context: Context) : ConstraintLayoutWithLifecycle(context) {
    private val playShortsViewClicksSubject: PublishSubject<ShortsPageState> = PublishSubject.create()
    val playShortsViewClicks: Observable<ShortsPageState> = playShortsViewClicksSubject.hide()

    private var binding: ViewPlayShortsBinding? = null
    private lateinit var dataVideo: DataVideo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    var isTextViewClicked = false
    var isUserLogin = false

    init {
        inflateUi()
    }
    private fun inflateUi() {
        MeetFriendApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()
        isUserLogin = loggedInUserCache.getLoginUserToken() != null

        val view = View.inflate(context, R.layout.view_play_shorts, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding = ViewPlayShortsBinding.bind(view)

        RxBus.listen(RxEvent.RefreshPlayerView::class.java).subscribeAndObserveOnMainThread {
            Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
        }.autoDispose()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding?.apply {
            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                handleUserProfileClick()
            }.autoDispose()
            ivLike.throttleClicks().subscribeAndObserveOnMainThread {
                handleLikeClick()
            }.autoDispose()
            ivComment.throttleClicks().subscribeAndObserveOnMainThread {
                handleCommentClick()
            }.autoDispose()
            moreAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                handleMoreClick()
            }.autoDispose()
            shareAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                handleShareClick()
            }.autoDispose()
            ivDownload.throttleClicks().subscribeAndObserveOnMainThread {
                handleDownloadClick()
            }.autoDispose()
            tvFollow.throttleClicks().subscribeAndObserveOnMainThread {
                handleFollowClick()
            }.autoDispose()
            setupDescriptionClickListeners()
        }
    }

    private fun handleUserProfileClick() {
        if (isUserLogin) {
            playShortsViewClicksSubject.onNext(ShortsPageState.UserProfileClick(dataVideo))
        } else {
            playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
        }
    }

    private fun handleLikeClick() {
        if (isUserLogin) {
            updateLikeStatusCount()
        } else {
            playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
        }
    }

    private fun handleCommentClick() {
        if (isUserLogin) {
            playShortsViewClicksSubject.onNext(ShortsPageState.CommentClick(dataVideo))
        } else {
            playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
        }
    }

    private fun handleMoreClick() {
        if (isUserLogin) {
            playShortsViewClicksSubject.onNext(ShortsPageState.MoreClick(dataVideo))
        } else {
            playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
        }
    }

    private fun handleShareClick() {
        if (isUserLogin) {
            playShortsViewClicksSubject.onNext(ShortsPageState.ShareClick(dataVideo))
        } else {
            playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
        }
    }

    private fun handleDownloadClick() {
        if (isUserLogin) {
            playShortsViewClicksSubject.onNext(ShortsPageState.DownloadClick(dataVideo))
        } else {
            playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
        }
    }

    private fun handleFollowClick() {
        if (isUserLogin) {
            binding?.tvFollow?.isVisible = false
            playShortsViewClicksSubject.onNext(ShortsPageState.FollowClick(dataVideo))
        } else {
            playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
        }
    }

    private fun setupDescriptionClickListeners() {
        binding?.apply {
            tvDescription.setOnMentionClickListener { _, text ->
                if (isUserLogin) {
                    playShortsViewClicksSubject.onNext(ShortsPageState.MentionUserClick(text.toString(), dataVideo))
                    isTextViewClicked = false
                } else {
                    playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
                }
            }

            tvDescription.setOnHashtagClickListener { _, text ->
                if (isUserLogin) {
                    playShortsViewClicksSubject.onNext(ShortsPageState.HashtagClick(text.toString(), dataVideo))
                    isTextViewClicked = false
                } else {
                    playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
                }
            }

            tvDescription.throttleClicks().subscribeAndObserveOnMainThread {
                if (isTextViewClicked) {
                    tvDescription.maxLines = 1
                    isTextViewClicked = false
                } else {
                    tvDescription.maxLines = Integer.MAX_VALUE
                    isTextViewClicked = true
                    tvDescription.isSingleLine = false
                }
            }.autoDispose()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(videoData: DataVideo) {
        this.dataVideo = videoData

        binding?.apply {
            setupVideoPlayer(videoData)
            setupGiftButton(videoData)
            setupMusicView(videoData)
            setupLinkAttachmentView(dataVideo)
            setupUserInfo(videoData)
            setupReelStats(videoData)
            setupLiveBadge(videoData)
            setupGiftCount(videoData)
        }
    }

    private fun setupVideoPlayer(videoData: DataVideo) {
        binding?.apply {
            jzvdStdOutgoer.videoUrl = videoData.file_path
            setVideoDisplayType(videoData)

            Glide.with(context)
                .load(videoData.thumbnail)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(jzvdStdOutgoer.posterImageView)
        }
    }

    private fun setVideoDisplayType(videoData: DataVideo) {
        binding?.apply {
            if (videoData.width != null && videoData.height != null) {
                if (videoData.width >= videoData.height) {
                    Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
                    jzvdStdOutgoer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    jzvdStdOutgoer.posterImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                } else {
                    Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT)
                    jzvdStdOutgoer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    jzvdStdOutgoer.posterImageView.scaleType = ImageView.ScaleType.FIT_XY
                }
            } else {
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
                jzvdStdOutgoer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                jzvdStdOutgoer.posterImageView.scaleType = ImageView.ScaleType.FIT_CENTER
            }
        }
    }

    private fun setupGiftButton(videoData: DataVideo) {
        binding?.apply {
            if (videoData.user_id != loggedInUserId) {
                giftAppCompatImageView.isActivated = true
                giftAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                    if (isUserLogin) {
                        playShortsViewClicksSubject.onNext(ShortsPageState.GiftClick(videoData))
                    } else {
                        playShortsViewClicksSubject.onNext(ShortsPageState.OpenLoginPopup)
                    }
                }.autoDispose()
            } else {
                giftAppCompatImageView.isActivated = false
            }
        }
    }

    private fun setupMusicView(videoData: DataVideo) {
        binding?.apply {
            if (dataVideo.music_title != null) {
                musicLinearLayout.isVisible = true
                tvMusicName.text = dataVideo.music_title.plus(" • ").plus(videoData.artists)
            } else {
                musicLinearLayout.isVisible = true
                tvMusicName.text = getUserName(videoData).plus(" • Original Audio")
                tvMusicName.isSelected = true
            }
        }
    }

    private fun getUserName(videoData: DataVideo): String {
        return if (!videoData.post.user.userName.isNullOrEmpty() && videoData.post.user.userName != "null") {
            videoData.post.user.userName ?: ""
        } else {
            videoData.post.user.firstName.plus(" ").plus(videoData.post.user.lastName)
        }
    }

    private fun setupUserInfo(videoData: DataVideo) {
        binding?.apply {
            tvUserName.isVisible = !videoData.post.user.userName.isNullOrEmpty() &&
                videoData.post.user.userName != "null"
            tvUserName.text = videoData.post.created_at
            tvName.text = getUserName(videoData)
            ivAccountVerified.visibility = if (videoData.post.user.isVerified == 0) View.GONE else View.VISIBLE

            Glide.with(context)
                .load(dataVideo.post.user.profile_photo)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivUserProfileImage)
        }
    }

    private fun setupReelStats(videoData: DataVideo) {
        binding?.apply {
            tvDescription.setOnTouchListener { p0, _ ->
                p0?.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
            tvShortsViewCount.text = videoData.post.shortViews.toString()
            tvDescription.isVisible = !videoData.post.content.isNullOrEmpty()
            tvDescription.text = videoData.post.content
            tvShortsTime.text = videoData.createdAt

            if (videoData.user_id == loggedInUserId) {
                tvFollow.isVisible = false
                tvFollowing.isVisible = false
            } else {
                tvFollow.visibility = if (videoData.followBack == 0) View.VISIBLE else View.GONE
            }

            updateReelLike()
            updateReelComment()
        }
    }

    private fun setupLiveBadge(videoData: DataVideo) {
        binding?.apply {
            tvLiveBadge.isVisible = videoData.liveId != 0
        }
    }

    private fun setupGiftCount(videoData: DataVideo) {
        binding?.apply {
            if (videoData.post.total_gift_count > 0) {
                tvGiftCount.text = videoData.post.total_gift_count.prettyCount().toString()
                tvGiftCount.visibility = View.VISIBLE
            } else {
                tvGiftCount.text = ""
                tvGiftCount.visibility = View.INVISIBLE
            }
        }
    }

    private fun updateReelLike() {
        binding?.apply {
            if (dataVideo.post.is_liked_count == 1) {
                ivLike.setImageResource(R.drawable.ic_fill_heart)
            } else {
                ivLike.setImageResource(R.drawable.ic_heart)
            }

            val totalLikes = dataVideo.post.post_likes_count
            if (totalLikes != null) {
                if (totalLikes != 0) {
                    tvLikeCount.text = totalLikes.prettyCount().toString()
                    tvLikeCount.visibility = View.VISIBLE
                } else {
                    tvLikeCount.text = ""
                    tvLikeCount.visibility = View.INVISIBLE
                }
            } else {
                tvLikeCount.text = ""
                tvLikeCount.visibility = View.INVISIBLE
            }
        }
    }

    private fun updateReelComment() {
        binding?.apply {
            var postComment = dataVideo.post.post_comments.size
            dataVideo.post.post_comments.forEach {
                postComment += it.child_comments.size
            }
            val totalComments = postComment
            if (totalComments != null) {
                if (totalComments != 0) {
                    tvCommentCount.text = totalComments.prettyCount().toString()
                    tvCommentCount.visibility = View.VISIBLE
                } else {
                    tvCommentCount.text = ""
                    tvCommentCount.visibility = View.INVISIBLE
                }
            } else {
                tvCommentCount.text = ""
                tvCommentCount.visibility = View.INVISIBLE
            }
        }
    }

    private fun updateLikeStatusCount() {
        if (dataVideo.post.is_liked_count == 0) {
            dataVideo.post.is_liked_count = 1
        } else {
            dataVideo.post.is_liked_count = 0
        }
        if (dataVideo.post.is_liked_count == 1) {
            dataVideo.post.post_likes_count = dataVideo.post.post_likes_count.let { it + 1 }
            updateReelLike()
            playShortsViewClicksSubject.onNext(ShortsPageState.AddReelLikeClick(dataVideo))
        } else {
            dataVideo.post.post_likes_count = dataVideo.post.post_likes_count.let { it - 1 }
            updateReelLike()
            playShortsViewClicksSubject.onNext(ShortsPageState.RemoveReelLikeClick(dataVideo))
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    fun setupLinkAttachmentView(dataVideo: DataVideo) {
        if (!dataVideo.post.web_link.isNullOrEmpty()) {
            binding?.linkAttachmentContainer?.visibility = View.VISIBLE
            val calculatePosition = calculatePosition(dataVideo)
            val lastX = calculatePosition.buttonX
            val lastY = calculatePosition.buttonY
            val buttonWidth = calculatePosition.buttonWidth
            val buttonHeight = calculatePosition.buttonHeight
            val view = createLinkView(dataVideo, lastX, lastY, buttonWidth, buttonHeight)
            setupLinkClickListeners(view, dataVideo)
            binding?.linkAttachmentContainer?.removeAllViews()
            binding?.linkAttachmentContainer?.addView(view)
        } else {
            hideLinkAttachment()
        }
    }

    private fun calculatePosition(dataVideo: DataVideo): ShortPositionData {
        val inputString = dataVideo.post.position
        val trimmedString = inputString?.trim('[', ']')
        val stringArray = trimmedString?.split(",")
        val doubleList = stringArray?.map { it.toDouble() }

        val lastX = doubleList?.get(0)?.toFloat() ?: 0f
        val lastY = doubleList?.get(1)?.toFloat() ?: 0f
        var buttonWidth = doubleList?.get(2)?.toInt() ?: 0
        var buttonHeight = doubleList?.get(FiXED_3_INT)?.toInt() ?: 0

        val screenMetrics = resources.displayMetrics
        val screenHeight = screenMetrics.heightPixels - resources.getDimensionPixelSize(R.dimen._48sdp)
        val screenWidth = screenMetrics.widthPixels

        val buttonX = (lastX * screenWidth.toFloat()) / FiXED_100_INT
        val buttonY = (lastY * screenHeight.toFloat()) / FiXED_100_INT
        buttonWidth = buttonWidth * screenWidth / FiXED_100_INT
        buttonHeight = buttonHeight * screenHeight / FiXED_100_INT

        return ShortPositionData(buttonX, buttonY, buttonWidth, buttonHeight)
    }

    private fun createLinkView(
        dataVideo: DataVideo,
        lastX: Float,
        lastY: Float,
        buttonWidth: Int,
        buttonHeight: Int
    ): View {
        val view = LayoutInflater.from(
            context
        ).inflate(R.layout.link_click_view, binding?.linkAttachmentContainer, false)
        var layoutParams = view.layoutParams
        if (lastX != 0f && lastY != 0f) {
            val position = ShortPositionData(
                lastX,
                lastY,
                buttonWidth,
                buttonHeight
            )
            configureViewPosition(view, dataVideo, position)
        } else {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
                marginEnd = FiXED_MARGIN_40
                marginStart = FiXED_MARGIN_40
                width = buttonWidth
                height = buttonHeight
            }
            view.layoutParams = layoutParams
        }
        view.rotation = dataVideo.post.rotationAngle ?: 0.0f
        return view
    }

    private fun configureViewPosition(
        view: View,
        dataVideo: DataVideo,
        shortPositionData: ShortPositionData,
    ) {
        if (dataVideo.post.platform == "iOS") {
            view.x = shortPositionData.buttonX
            view.y = shortPositionData.buttonY + FiXED_100_FLOAT
            adjustViewDimensions(view, shortPositionData.buttonWidth, shortPositionData.buttonHeight + FiXED_50_INT)
        } else {
            view.x = shortPositionData.buttonX
            if ((dataVideo.width ?: 0) > (dataVideo.height ?: 0)) {
                view.y = shortPositionData.buttonY - FiXED_100_FLOAT
            } else {
                view.y = shortPositionData.buttonY
            }
            adjustViewDimensions(view, shortPositionData.buttonWidth, shortPositionData.buttonHeight)
        }
    }

    private fun adjustViewDimensions(view: View, width: Int, height: Int) {
        val layoutParams = view.layoutParams
        layoutParams.width = width
        layoutParams.height = height
        view.layoutParams = layoutParams
    }

    private fun setupLinkClickListeners(view: View, dataVideo: DataVideo) {
        view.findViewById<LinearLayout>(R.id.llAttachment).setOnClickListener {
            dataVideo.post.web_link?.let {
                playShortsViewClicksSubject.onNext(ShortsPageState.OpenLinkAttachment(dataVideo))
            }
        }
    }

    private fun hideLinkAttachment() {
        binding?.apply {
            if (linkAttachmentContainer.childCount > 0) {
                linkAttachmentContainer.removeAllViews()
                linkAttachmentContainer.visibility = View.GONE
            }
        }
    }
}
