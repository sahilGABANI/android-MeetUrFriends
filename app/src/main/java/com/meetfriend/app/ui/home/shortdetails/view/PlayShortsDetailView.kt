package com.meetfriend.app.ui.home.shortdetails.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.post.model.ShortsDetailPageState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewPlayShortsBinding
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.video.Post
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class PlayShortsDetailView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val playShortsViewClicksSubject: PublishSubject<ShortsDetailPageState> =
        PublishSubject.create()
    val playShortsViewClicks: Observable<ShortsDetailPageState> = playShortsViewClicksSubject.hide()


    private var binding: ViewPlayShortsBinding? = null
    private lateinit var dataVideo: Post

    private var screenHeight: Int = 0
    private var screenWidth: Int = 0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    private var isTextViewClicked = false
    private var isUserLogin = false
    init {
        inflateUi()
        screenHeight = resources.displayMetrics.heightPixels
        screenWidth = resources.displayMetrics.widthPixels
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val statusBarHeight = insets.systemWindowInsetTop
            val navigationBarHeight = insets.systemWindowInsetBottom
            screenHeight = screenHeight - statusBarHeight - navigationBarHeight

            insets
        }
    }

    private fun inflateUi() {
        MeetFriendApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()
        isUserLogin = loggedInUserCache.getLoginUserToken() != null

        val view = View.inflate(context, R.layout.view_play_shorts, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding = ViewPlayShortsBinding.bind(view)

        binding?.apply {

            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ShortsDetailPageState.UserProfileClick(dataVideo))
            }.autoDispose()

            ivLike.throttleClicks().subscribeAndObserveOnMainThread {
                updateLikeStatusCount()
            }.autoDispose()

            ivComment.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ShortsDetailPageState.CommentClick(dataVideo))
            }.autoDispose()

            moreAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ShortsDetailPageState.MoreClick(dataVideo))
            }.autoDispose()

            shareAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ShortsDetailPageState.ShareClick(dataVideo))
            }.autoDispose()

            ivDownload.throttleClicks().subscribeAndObserveOnMainThread {
                playShortsViewClicksSubject.onNext(ShortsDetailPageState.DownloadClick(dataVideo))
            }.autoDispose()
            tvFollow.throttleClicks().subscribeAndObserveOnMainThread {
                tvFollow.isVisible = false
                playShortsViewClicksSubject.onNext(ShortsDetailPageState.FollowClick(dataVideo))
            }.autoDispose()
            giftAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                dataVideo.user?.let { user ->
                    if (user.id != loggedInUserCache.getLoggedInUserId()) {
                        playShortsViewClicksSubject.onNext(ShortsDetailPageState.GiftClick(dataVideo))
                    }
                }
            }.autoDispose()

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

            tvDescription.setOnMentionClickListener { view, text ->
                playShortsViewClicksSubject.onNext(
                    ShortsDetailPageState.MentionUserClick(
                        text.toString(),
                        dataVideo
                    )
                )
                isTextViewClicked = false

            }

            tvDescription.setOnHashtagClickListener { view, text ->
                playShortsViewClicksSubject.onNext(
                    ShortsDetailPageState.HashtagClick(
                        text.toString(),
                        dataVideo
                    )
                )
                isTextViewClicked = false

            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(videoData: Post?) {
        this.dataVideo = videoData!!
        binding?.apply {

            giftAppCompatImageView.isActivated = videoData.post?.user_id != loggedInUserId
            tvUserName.text = videoData.created_at
            tvName.text = if(!videoData.post?.user?.userName.isNullOrEmpty() && videoData.post?.user?.userName != "null") videoData.post?.user?.userName
            else videoData.post?.user?.firstName.plus(
                " "
            ).plus(videoData.post?.user?.lastName)
            Glide.with(context)
                .load(dataVideo.post?.user?.profile_photo)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivUserProfileImage)

            if (dataVideo.post?.user?.isVerified == 1){
                ivAccountVerified.visibility= View.VISIBLE
            }else{
                ivAccountVerified.visibility= View.GONE
            }

            tvDescription.setOnTouchListener { p0, _ ->
                p0?.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
            tvDescription.isVisible = !videoData.post?.content.isNullOrEmpty()
            tvShortsViewCount.text = if (videoData.post?.shortViews == 0) "" else videoData.post?.shortViews.toString()

            tvDescription.text = videoData.post?.content
            if (videoData.post?.user_id == loggedInUserId) {
                tvFollow.isVisible = false
                tvFollowing.isVisible = false
            } else {
                tvFollow.visibility = if (videoData.followBack == 0) View.VISIBLE else View.GONE
            }
            updateReelLike()
            videoData.post?.let { updateReelComment(it) }

            jzvdStdOutgoer.videoUrl = videoData.file_path
            if (videoData.width?.equals(null) == true && videoData.height?.equals(null) == true) {
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
                jzvdStdOutgoer.posterImageView.scaleType = ImageView.ScaleType.CENTER
                jzvdStdOutgoer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            } else if(videoData.width == 0 && videoData.height == 0) {
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
                jzvdStdOutgoer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                jzvdStdOutgoer.posterImageView.scaleType = ImageView.ScaleType.CENTER
            } else {
                if (videoData.width > (videoData.height ?: 0)) {
                    Jzvd.setVideoImageDisplayType(4)
                    jzvdStdOutgoer.posterImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    jzvdStdOutgoer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    jzvdStdOutgoer.posterImageView.scaleType = ImageView.ScaleType.CENTER
                    jzvdStdOutgoer.posterImageView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
                }
            }

            if (!dataVideo.post?.web_link.isNullOrEmpty()) {
                val inputString = dataVideo.post?.position
                linkAttachmentContainer.visibility = View.VISIBLE
                val trimmedString = inputString?.trim('[', ']')
                val stringArray = trimmedString?.split(",")
                val doubleList = stringArray?.map { it.toDouble() }
                var lastX = doubleList?.get(0)?.toFloat() ?: 0f
                var lastY = doubleList?.get(1)?.toFloat() ?: 0f
                var buttonWidth = doubleList?.get(2)?.toInt() ?: 0
                var buttonHeight = doubleList?.get(3)?.toInt() ?: 0
                val rotation = dataVideo.post?.rotationAngle ?: 0.0f
                val view = LayoutInflater.from(context).inflate(R.layout.link_click_view, linkAttachmentContainer, false)
                val buttonX = (lastX * screenWidth.toFloat()) / 100
                val buttonY = (lastY * screenHeight.toFloat()) / 100
                buttonWidth = buttonWidth * screenWidth / 100
                buttonHeight = buttonHeight * screenHeight / 100


                if (lastX != 0f && lastY != 0f) {
                    view.x = buttonX
                    view.y = buttonY
                    // Set the dimensions
                    val layoutParams = view.layoutParams
                    layoutParams.width = buttonWidth.toInt()
                    layoutParams.height = buttonHeight.toInt()
                    view.layoutParams = layoutParams

                    // Apply the rotation

                } else {
                    val layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
                        marginEnd = 40
                        marginStart = 40
                        width = buttonWidth.toInt()
                        height = buttonHeight.toInt()
                    }
                    view.layoutParams = layoutParams
                }
                view.rotation = rotation
                linkAttachmentContainer.removeAllViews()
                linkAttachmentContainer.addView(view)
                view.findViewById<LinearLayout>(R.id.llAttachment).setOnClickListener {
                    dataVideo.post?.web_link?.let { it1 ->
                        playShortsViewClicksSubject.onNext(ShortsDetailPageState.OpenLinkAttachment(dataVideo))
                    }
                }
            } else {
                if (linkAttachmentContainer.childCount > 0) {
                    linkAttachmentContainer.removeAllViews()
                    linkAttachmentContainer.visibility = View.GONE
                }
            }
            Glide.with(context).load(videoData.thumbnail).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(jzvdStdOutgoer.posterImageView)

            if (videoData.total_gift_count > 0) {
                tvGiftCount.text = videoData.total_gift_count.prettyCount().toString()
                tvGiftCount.visibility = View.VISIBLE

            } else {
                tvGiftCount.text = ""
                tvGiftCount.visibility = View.INVISIBLE

            }


            if (videoData.music_title != null) {
                musicLinearLayout.isVisible = true
                tvMusicName.text = videoData.music_title.plus(" • ").plus(videoData.artists)
            } else {
                musicLinearLayout.isVisible = true
                val userName = if(!videoData.post?.user?.userName.isNullOrEmpty() && videoData.post?.user?.userName != "null") videoData.post?.user?.userName else videoData.post?.user?.firstName.plus(
                    " "
                ).plus(videoData.post?.user?.lastName)
                tvMusicName.text = "${userName} • Original Audio"
                tvMusicName.isSelected = true
            }
        }
    }

    private fun updateReelLike() {
        binding?.apply {
            if (dataVideo.post?.is_liked_count == 1) {
                ivLike.setImageResource(R.drawable.ic_fill_heart)
            } else {
                ivLike.setImageResource(R.drawable.ic_heart)
            }

            val totalLikes = dataVideo.post?.post_likes_count
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

    private fun updateReelComment(videoData: Post) {
        binding?.apply {
            var postComment = videoData.post_comments.size
            videoData.post_comments.forEach {
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
        if (dataVideo.post?.is_liked_count == 0) {
            dataVideo.post?.is_liked_count = 1
        } else {
            dataVideo.post?.is_liked_count = 0

        }

        if (dataVideo.post?.is_liked_count == 1) {
            dataVideo.post_likes_count = dataVideo.post?.is_liked_count.let { it?.plus(1) ?: 0 }
            updateReelLike()

            playShortsViewClicksSubject.onNext(ShortsDetailPageState.AddReelLikeClick(dataVideo))
        } else {
            dataVideo.post_likes_count = dataVideo.post?.is_liked_count.let { it?.minus(1) ?: 0 }
            updateReelLike()
            playShortsViewClicksSubject.onNext(ShortsDetailPageState.RemoveReelLikeClick(dataVideo))
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

}