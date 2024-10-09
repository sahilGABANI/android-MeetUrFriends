package com.meetfriend.app.ui.main.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.Layout
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.model.HomePagePostInfoState
import com.meetfriend.app.api.post.model.PositionData
import com.meetfriend.app.api.post.model.PostInformation
import com.meetfriend.app.api.post.model.PostMediaInformation
import com.meetfriend.app.api.post.model.SharedPost
import com.meetfriend.app.api.post.model.TaggedUser
import com.meetfriend.app.api.post.model.User
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.AdUnifiedBinding
import com.meetfriend.app.databinding.VideoPostItemViewBinding
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.FileUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.roundToInt

class MainVideoPostView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    companion object {
        const val TAG = "MainVideoPostView"
        var descriptionHeight: Int = 0
        var remainingHeight: Int = 0
        var FIXED_ASPECT_RATIO = 0.52
        var FIXED_MARGIN_52 = 52
        var FIXED_MARGIN_15 = 15
        var FIXED_INT_100 = 100
        var FIXED_INT_20 = 20
        var FIXED_INT_3 = 3
    }

    private val postViewClicksSubject: PublishSubject<HomePagePostInfoState> = PublishSubject.create()
    val postViewClicks: Observable<HomePagePostInfoState> = postViewClicksSubject.hide()

    private var binding: VideoPostItemViewBinding? = null
    private lateinit var postDetails: PostInformation

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var currentNativeAd: NativeAd? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        MeetFriendApplication.component.inject(this)
        val view = View.inflate(context, R.layout.video_post_item_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = VideoPostItemViewBinding.bind(view)
        binding?.apply {
            profileRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
                postViewClicksSubject.onNext(HomePagePostInfoState.UserProfileClick(postDetails))
            }

            optionsAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                postViewClicksSubject.onNext(HomePagePostInfoState.OptionsClick(postDetails))
            }

            likeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                updateLikeStatusCount()
            }.autoDispose()

            totalLikesAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                postViewClicksSubject.onNext(HomePagePostInfoState.PostLikeCountClick(postDetails))
            }.autoDispose()

            commentLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                postViewClicksSubject.onNext(HomePagePostInfoState.CommentClick(postDetails))
            }.autoDispose()

            shareLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                postViewClicksSubject.onNext(HomePagePostInfoState.ShareClick(postDetails))
            }.autoDispose()

            repostLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                postViewClicksSubject.onNext(HomePagePostInfoState.RepostClick(postDetails))
            }.autoDispose()

            otherUserProfileRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
                postViewClicksSubject.onNext(HomePagePostInfoState.OtherUserProfileClick(postDetails))
            }.autoDispose()

            giftLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                if (postDetails.user.id != loggedInUserCache.getLoggedInUserId()) {
                    postViewClicksSubject.onNext(HomePagePostInfoState.GiftClick(postDetails))
                }
            }.autoDispose()

            RxBus.listen(RxEvent.PostCommentUpdate::class.java).subscribeAndObserveOnMainThread {
                if (it.data?.id == postDetails.id) {
                    var postComment = it.data.post_comments.size
                    it.data.post_comments.forEach { item ->
                        postComment += item.child_comments.size
                    }
                    binding?.totalCommentsAppCompatTextView?.text =
                        if (postComment == 0) {
                            ""
                        } else {
                            postComment.prettyCount().toString()
                        }
                }
            }.autoDispose()
        }
    }

    private fun aspectRatioMediaViewDescription(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val aspectRatio = width.toFloat() / height.toFloat()
            val screenHeight = context.resources.displayMetrics.heightPixels
            val maxHeight = screenHeight * FIXED_ASPECT_RATIO // Adjust this factor as needed

            var newHeight = (context.resources.displayMetrics.widthPixels / aspectRatio).toInt()

            if (newHeight > maxHeight) {
                newHeight = maxHeight.toInt()
            }

            binding?.sliderImageSlider?.layoutParams?.height = newHeight
            binding?.linkAttachmentContainer?.layoutParams?.height = newHeight
        }
    }

    private fun aspectRatioMediaView(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val aspectRatio = width.toFloat() / height.toFloat()
            val screenHeight = context.resources.displayMetrics.heightPixels
            val toolbarHeight = resources.getDimensionPixelSize(R.dimen._45sdp)
            val bottomBarHeight = resources.getDimensionPixelSize(R.dimen._48sdp)
            val containerHeight = resources.getDimensionPixelSize(R.dimen._45sdp)
            val profileImageHeight = resources.getDimensionPixelSize(R.dimen._60sdp)
            remainingHeight = screenHeight - (toolbarHeight + bottomBarHeight + containerHeight + profileImageHeight)
            var newHeight = (context.resources.displayMetrics.widthPixels / aspectRatio).toInt()
            if (newHeight > remainingHeight) {
                newHeight = remainingHeight
            }
            binding?.sliderImageSlider?.layoutParams?.height = newHeight
            binding?.linkAttachmentContainer?.layoutParams?.height = newHeight
        }
    }

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables")
    fun bind(post: PostInformation, position: Int) {
        this.postDetails = post
        binding?.apply {
            handleAdsDisplay(position)
            loadUserProfileImage(post)
            setUserNameDefault(post.user)
            setPostDetails(post)
            handlePostMediaDescription(post)
            handlePostMedia(post)
            handleLikes(post)
            val imageList: ArrayList<String> = arrayListOf()

            if (post.isShared == 1) {
                otherUserProfileRoundedImageView.visibility = View.VISIBLE
                otherUserAppCompatTextView.visibility = View.VISIBLE
                otherUserPostTimeAppCompatTextView.visibility = View.VISIBLE
                ivTimeDottedOtherUserPost.visibility = View.VISIBLE
                postTitleAppCompatTextView.visibility =
                    if (post.sharedPost?.content != null && post.sharedPost.content != "") View.VISIBLE else View.GONE
                viewSharedTop.visibility = View.VISIBLE
                viewSharedLeft.visibility = View.VISIBLE
                viewSharedRight.visibility = View.VISIBLE
                viewSharedBottom.visibility = View.VISIBLE
                setupLinkAttachment(0)
                setMargins(
                    sliderImageSlider,
                    FIXED_MARGIN_52,
                    FIXED_MARGIN_15,
                    FIXED_MARGIN_52,
                    FIXED_MARGIN_15
                )
                if (post.sharedPost?.postMedia.isNullOrEmpty()) {
                    playVideoAppCompatImageView.isVisible = false
                }
                post.sharedPost?.let { sharedPost ->
                    postTitleAppCompatTextView.originalText = sharedPost.content.toString()
                    otherUserUserNameAppCompatTextView.text =
                        context.getString(R.string.sign_at_the_rate).plus(sharedPost.user?.userName)

                    // Set visibility of account verification icon
                    ivAccountVerifiedOther.visibility = if (sharedPost.user?.isVerified == 0) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                    // Set post time
                    otherUserPostTimeAppCompatTextView.text = "${sharedPost.createdAt}"

                    // Load profile image
                    Glide.with(context)
                        .load(sharedPost.user?.profilePhoto)
                        .placeholder(resources.getDrawable(R.drawable.image_placeholder, null))
                        .centerCrop()
                        .into(otherUserProfileRoundedImageView)

                    // Handle post media
                    handlePostMedia(sharedPost, imageList)

                    // Set up text touch listener
                    setupTouchListenerForMentions(sharedPost)

                    setRepostUserName()
                    setUserName(post.user)
                }
            } else {
                setUserName(post.user)
                otherUserProfileRoundedImageView.visibility = View.GONE
                otherUserAppCompatTextView.visibility = View.GONE
                otherUserPostTimeAppCompatTextView.visibility = View.GONE
                ivTimeDottedOtherUserPost.visibility = View.GONE
                otherUserUserNameAppCompatTextView.visibility = View.GONE
                ivAccountVerifiedOther.visibility = View.GONE
                postTitleAppCompatTextView.visibility = View.GONE
                viewSharedTop.visibility = View.GONE
                viewSharedLeft.visibility = View.GONE
                viewSharedRight.visibility = View.GONE
                viewSharedBottom.visibility = View.GONE
                setMargins(sliderImageSlider, 0, FIXED_INT_20, 0, 0)
                if (post.postMedia.isNullOrEmpty()) {
                    playVideoAppCompatImageView.isVisible = false
                }

                if (!post.postMedia.isNullOrEmpty()) {
                    if (post.postMedia.first().extension == ".m3u8") {
                        imageList.add(post.postMedia.first().thumbnail.toString())
                    } else {
                        post.postMedia.forEach {
                            imageList.add(it.filePath.toString())
                        }
                    }
                }
            }

            setupLinkAttachment(0)
            val data = if (postDetails.isShared == 1) postDetails.sharedPost?.postMedia else postDetails.postMedia
            if (!data.isNullOrEmpty()) {
                val homePagePostMediaAdapter = PostMediaAdapter(context)
                homePagePostMediaAdapter.apply {
                    mediaPhotoViewClick.subscribeAndObserveOnMainThread {
                        postViewClicksSubject.onNext(
                            HomePagePostInfoState.PostMediaClick(
                                postDetails,
                                sliderImageSlider.currentItem
                            )
                        )
                    }.autoDispose()
                    mediaVideoViewClick.subscribeAndObserveOnMainThread {
                        postViewClicksSubject.onNext(
                            HomePagePostInfoState.PostMediaVideoClick(
                                postDetails,
                                sliderImageSlider.currentItem
                            )
                        )
                    }.autoDispose()
                }

                homePagePostMediaAdapter.deviceHeight = height
                sliderImageSlider.adapter = homePagePostMediaAdapter
                sliderImageSlider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                        // This method is called when the user is scrolling between pages
                    }

                    override fun onPageSelected(position: Int) {
                        if (post.isShared == 1) {
                            setupLinkAttachment(position)
                            if (!post.sharedPost?.postMedia?.get(position)?.musicTitle.isNullOrEmpty()) {
                                llSharedPostMusicInfo.isVisible = true
                                Glide.with(context).asGif().load(R.raw.music_theme).into(ivSharedPostMusicLyricsWav)
                                val musicName = post.sharedPost?.postMedia?.get(position)?.musicTitle.plus(", ")
                                    .plus(post.sharedPost?.postMedia?.get(position)?.artists)
                                tvSharedPostMusicName.text = musicName
                            } else {
                                llMusicInfo.isVisible = false
                                llSharedPostMusicInfo.isVisible = false
                            }
                        } else {
                            setupLinkAttachment(position)
                            if (!post.postMedia?.get(position)?.musicTitle.isNullOrEmpty()) {
                                llMusicInfo.isVisible = true
                                Glide.with(context).asGif().load(R.raw.music_theme).into(ivMusicLyricsWav)
                                val musicName = post.postMedia?.get(
                                    position
                                )?.musicTitle.plus(", ").plus(post.postMedia?.get(position)?.artists)
                                tvMusicName.text = musicName
                            } else {
                                llMusicInfo.isVisible = false
                                llSharedPostMusicInfo.isVisible = false
                            }
                        }
                    }

                    override fun onPageScrollStateChanged(state: Int) {
                        // This method is called when the scroll state changes
                        when (state) {
                            ViewPager2.SCROLL_STATE_IDLE -> {
                                // The pager is idle
                            }

                            ViewPager2.SCROLL_STATE_DRAGGING -> {
                                // The pager is being dragged
                            }

                            ViewPager2.SCROLL_STATE_SETTLING -> {
                                // The pager is settling after a drag or a fake drag
                            }
                        }
                    }
                })

                val data = if (postDetails.isShared == 1) postDetails.sharedPost?.postMedia else postDetails.postMedia
                if (!data.isNullOrEmpty()) {
                    homePagePostMediaAdapter.postMediaType = 1
                    homePagePostMediaAdapter.listOfDataItems = data
                }
                indicator.setViewPager2(sliderImageSlider)

                if ((data?.size ?: 0) > 1) {
                    indicator.visibility = View.VISIBLE
                } else {
                    indicator.visibility = View.GONE
                }
            } else {
                indicator.visibility = View.GONE
            }

            totalRepostAppCompatTextView.text = if (post.repostCount?.equals(0) == true) {
                ""
            } else {
                post.repostCount.toString()
            }

            totalGiftAppCompatTextView.text = if (post.totalGiftCount?.equals(0) == true) {
                ""
            } else {
                post.totalGiftCount?.prettyCount().toString()
            }

            val hasMedia = !post.postMedia.isNullOrEmpty()
            val isSharedAndHasImages = (post.isShared == 1) && imageList.isNotEmpty()
            sliderImageSlider.isVisible = hasMedia || isSharedAndHasImages
            postsTitleAppCompatTextViewDetails.setOnTouchListener { _, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    val mOffset = postsTitleAppCompatTextViewDetails.getOffsetForPosition(
                        motionEvent.x,
                        motionEvent.y
                    )
                    var clickedWord = FileUtils.findWordForRightHanded(
                        postsTitleAppCompatTextViewDetails.text.toString(),
                        mOffset
                    )

                    if (clickedWord.contains("@")) {
                        clickedWord = clickedWord.substringAfter("@")
                        var isUserFound = false
                        if (!post.mentionUsers.isNullOrEmpty()) {
                            post.mentionUsers.forEach { mInfo ->
                                if (clickedWord == mInfo.user.userName) {
                                    postViewClicksSubject.onNext(
                                        HomePagePostInfoState.MentionUserNavigationClick(
                                            mInfo.user.id
                                        )
                                    )
                                    isUserFound = true
                                }
                            }
                            postViewClicksSubject.onNext(
                                HomePagePostInfoState.ShowUserNotFoundToast(
                                    !isUserFound
                                )
                            )
                        } else {
                            if (clickedWord.isNotEmpty()) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.ShowUserNotFoundToast(
                                        true
                                    )
                                )
                            }
                        }
                    } else if (clickedWord.contains("#")) {
                        clickedWord = clickedWord.substringAfter("#")
                        var isUserFound = false
                        if (!post.postHashtags.isNullOrEmpty()) {
                            post.postHashtags.forEach { mInfo ->

                                val finalTagName = if ('#' == mInfo.tagName?.first()) {
                                    mInfo.tagName.removePrefix(
                                        "#"
                                    )
                                } else {
                                    mInfo.tagName
                                }
                                if (clickedWord == finalTagName) {
                                    postViewClicksSubject.onNext(
                                        HomePagePostInfoState.MentionHashTagNavigationClick(
                                            mInfo.hashtagId,
                                            mInfo.tagName ?: ""
                                        )
                                    )
                                    isUserFound = true
                                }
                            }
                            postViewClicksSubject.onNext(
                                HomePagePostInfoState.ShowHashtagNotFoundToast(
                                    !isUserFound
                                )
                            )
                        } else {
                            if (clickedWord.isNotEmpty()) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.ShowHashtagNotFoundToast(
                                        true
                                    )
                                )
                            }
                        }
                    }
                }
                false
            }
        }
    }

    private fun handleAdsDisplay(position: Int) {
        if (position != 0 && position % loggedInUserCache.getScrollCountAd().toInt() == 0) {
            initializeMobileAdsSdk()
        }
    }

    // Separate function to load user profile image
    private fun loadUserProfileImage(post: PostInformation) {
        binding?.apply {
            Glide.with(context)
                .load(post.user.profilePhoto)
                .placeholder(ResourcesCompat.getDrawable(resources, R.drawable.image_placeholder, null))
                .into(profileRoundedImageView)
        }
    }

    // Separate function to set user details
    private fun setUserNameDefault(user: User) {
        binding?.apply {
            newUserNameAppCompatTextView.text = context.getString(R.string.sign_at_the_rate).plus(user.userName)
        }
    }

    // Separate function to set post details (likes, comments, reposts)
    private fun setPostDetails(post: PostInformation) {
        binding?.apply {
            postTimeAppCompatTextView.text = post.createdAt
            totalLikesAppCompatTextView.text = post.postLikesCount?.takeIf { it > 0 }?.prettyCount()?.toString() ?: ""

            val postComment = calculateTotalComments(post)
            totalCommentsAppCompatTextView.text = if (postComment == 0) "" else postComment.prettyCount().toString()

            totalCountAppCompatTextView.text = post.shortViews?.takeIf { it > 0 }?.prettyCount()?.toString() ?: ""
            totalRepostAppCompatTextView.text = post.repostCount?.takeIf { it > 0 }?.prettyCount()?.toString() ?: ""

            totalSharesAppCompatTextView.visibility = View.INVISIBLE
            postsTitleAppCompatTextViewDetails.visibility = if (!post.content.isNullOrEmpty()) View.VISIBLE else View.GONE
            postsTitleAppCompatTextViewDetails.originalText = post.content.toString()
        }
    }

    // Separate function to calculate total comments
    private fun calculateTotalComments(post: PostInformation): Int {
        var postComment = post.postComments?.size ?: 0
        post.postComments?.forEach {
            postComment += it.childComments?.size ?: 0
        }
        return postComment
    }

    // Separate function to handle media description aspect ratio
    private fun handlePostMediaDescription(post: PostInformation) {
        binding?.apply {
            postsTitleAppCompatTextViewDetails.post {
                descriptionHeight = postsTitleAppCompatTextViewDetails.height
                if (descriptionHeight != 0) {
                    val postMedia = post.postMedia
                    if (!postMedia.isNullOrEmpty()) {
                        val media = postMedia.first()
                        media.height?.toInt()?.let { mediaHeight ->
                            media.width?.toInt()?.let { mediaWidth ->
                                if (mediaHeight != 0 && mediaWidth != 0) {
                                    aspectRatioMediaViewDescription(mediaWidth, mediaHeight)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Separate function to handle post media aspect ratio
    private fun handlePostMedia(post: PostInformation) {
        val postMedia = post.postMedia
        if (!postMedia.isNullOrEmpty()) {
            val media = postMedia.first()
            val mediaHeight = media.height?.toInt() ?: 0
            val mediaWidth = media.width?.toInt() ?: 0

            if (mediaHeight != 0
                && mediaWidth != 0
            ) {
                aspectRatioMediaView(mediaWidth, mediaHeight)
            }
        }
    }

    // Separate function to handle like functionality
    private fun handleLikes(post: PostInformation) {
        binding?.apply {
            post.isLikedCount?.let {
                likeAppCompatImageView.setImageResource(
                    if (it >= 1) R.drawable.ic_fill_heart else R.drawable.ic_gray_heart
                )
            }
        }
    }

    private fun setupLinkAttachment(position: Int) {
        binding?.apply {
            if (postDetails.isShared == 1) {
                val media = postDetails.sharedPost?.postMedia?.get(position)
                if (media?.getIsFilePathImage() == true && !media.webLink.isNullOrEmpty()) {
                    setupLinkAttachmentForImage(position, media)
                } else if (!media?.webLink.isNullOrEmpty()) {
                    setupLinkAttachmentForOther(position, media)
                } else {
                    linkAttachmentContainer.isVisible = false
                }
            } else {
                val media = postDetails.postMedia?.get(position)
                if (media?.getIsFilePathImage() == true && !media.webLink.isNullOrEmpty()) {
                    setupLinkAttachmentForImage(position, media)
                } else if (!media?.webLink.isNullOrEmpty()) {
                    setupLinkAttachmentForOther(position, media)
                } else {
                    linkAttachmentContainer.isVisible = false
                }
            }
        }
    }

    private fun setupLinkAttachmentForImage(position: Int, media: PostMediaInformation) {
        binding?.apply {
            val coordinates = parsePosition(media.position)

            val originalHeight = media.height?.toFloat() ?: 0f
            val newHeight = sliderImageSlider.layoutParams.height.toFloat()
            val (newX, newY) = convertCoordinates(coordinates.x, coordinates.y, originalHeight, newHeight)

            setupLinkView(newX, newY, coordinates.width, coordinates.height, media.rotationAngle ?: 0.0f, position)
        }
    }

    private fun setupLinkAttachmentForOther(position: Int, media: PostMediaInformation?) {
        binding?.apply {
            val coordinates = parsePosition(media?.position)

            val fullScreenHeight = context.resources.displayMetrics.heightPixels
            val fullScreenWidth = context.resources.displayMetrics.widthPixels
            val newHeight = sliderImageSlider.layoutParams.height.toFloat()

            val buttonX = (coordinates.x * fullScreenWidth) / FIXED_INT_100
            val buttonY = (coordinates.y * newHeight) / FIXED_INT_100
            val scaledButtonWidth = (coordinates.width * fullScreenWidth) / FIXED_INT_100
            val scaledButtonHeight = (coordinates.height * fullScreenHeight) / FIXED_INT_100
            val rotation = media?.rotationAngle ?: 0.0f
            setupLinkView(buttonX, buttonY, scaledButtonWidth, scaledButtonHeight, rotation, position)
        }
    }

    private fun parsePosition(inputString: String?): PositionData {
        val trimmedString = inputString?.trim('[', ']')
        val stringArray = trimmedString?.split(",")
        val doubleList = stringArray?.map { it.toDouble() }

        val lastX = doubleList?.get(0)?.toFloat() ?: 0f
        val lastY = doubleList?.get(1)?.toFloat() ?: 0f
        val buttonWidth = doubleList?.get(2)?.toInt() ?: 0
        val buttonHeight = doubleList?.get(FIXED_INT_3)?.toInt() ?: 0
        return PositionData(lastX, lastY, buttonWidth, buttonHeight)
    }

    private fun setupLinkView(
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        rotation: Float,
        position: Int
    ) {
        binding?.apply {
            val view = LayoutInflater.from(context).inflate(R.layout.link_preview, linkAttachmentContainer, false)
            val layoutParams = view.layoutParams.apply {
                this.width = width
                this.height = height
            }

            view.layoutParams = layoutParams
            view.rotation = rotation
            view.x = x
            view.y = y

            linkAttachmentContainer.isVisible = true
            linkAttachmentContainer.removeAllViews()
            linkAttachmentContainer.addView(view)

            view.findViewById<LinearLayout>(R.id.llAttachment).setOnClickListener {
                postDetails.sharedPost?.postMedia?.get(position)?.webLink?.let {
                    postViewClicksSubject.onNext(HomePagePostInfoState.OpenLinkAttachmentMultiple(it))
                }
            }
        }
    }

    private fun setMargins(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (view.layoutParams is MarginLayoutParams) {
            val p = view.layoutParams as MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }

    /**
     * Handles the media in a shared post.
     */
    private fun handlePostMedia(sharedPost: SharedPost, imageList: MutableList<String>) {
        val postMedia = sharedPost.postMedia
        if (!postMedia.isNullOrEmpty()) {
            val firstMedia = postMedia.first()
            if (firstMedia.extension == ".m3u8") {
                imageList.add(firstMedia.thumbnail.toString())
            } else {
                postMedia.forEach { media ->
                    imageList.add(media.filePath.toString())
                }
            }
        }
    }

    /**
     * Sets up touch listener for mentions in the shared post.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListenerForMentions(sharedPost: SharedPost) {
        binding?.apply {
            postTitleAppCompatTextView.setOnTouchListener { _, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    val mOffset = postTitleAppCompatTextView.getOffsetForPosition(motionEvent.x, motionEvent.y)
                    val clickedSharedWord = FileUtils.findWordForRightHanded(
                        postTitleAppCompatTextView.text.toString(),
                        mOffset
                    )

                    if (clickedSharedWord.contains("@")) {
                        handleMentionClick(sharedPost, clickedSharedWord)
                    }
                }
                false
            }
        }
    }

    /**
     * Handles the click on mention words.
     */
    private fun handleMentionClick(sharedPost: SharedPost, clickedSharedWord: String) {
        var word = clickedSharedWord.substringAfter("@")
        var isSharedUserFound = false

        sharedPost.sharedMentionUsers?.let { mentionUsers ->
            mentionUsers.forEach { mentionUser ->
                if (word == mentionUser.user.userName) {
                    postViewClicksSubject.onNext(HomePagePostInfoState.MentionUserNavigationClick(mentionUser.user.id))
                    isSharedUserFound = true
                }
            }
        }

        // Show toast if user not found
        postViewClicksSubject.onNext(HomePagePostInfoState.ShowUserNotFoundToast(!isSharedUserFound))

        if (word.isNotEmpty() && sharedPost.sharedMentionUsers.isNullOrEmpty()) {
            postViewClicksSubject.onNext(HomePagePostInfoState.ShowUserNotFoundToast(true))
        }
    }

    private fun setUserName(user: User) {
        binding?.apply {
            setAccountVerificationVisibility(user.isVerified)
            val name = getUserName(user)

            when {
                postDetails.type.equals("profile_photo", true) -> {
                    userNameAppCompatTextView.text = getProfilePhotoText(name, user.gender ?: "")
                }

                !postDetails.taggedUsersList.isNullOrEmpty() -> {
                    postDetails.taggedUsersList?.let {
                        handleTaggedUser(it.firstOrNull(), name)
                    }
                }

                else -> {
                    userNameAppCompatTextView.text = getLocationText(name, postDetails.location)
                }
            }
        }
    }

    private fun setAccountVerificationVisibility(isVerified: Int) {
        binding?.apply {
            ivAccountVerified.visibility = if (isVerified == 1) View.VISIBLE else View.GONE
        }
    }

    private fun handleTaggedUser(taggedUser: TaggedUser?, name: String) {
        taggedUser?.let {
            val tagUserName = it.user.userName.toString()
            val text = getTaggedUserText(name, tagUserName)
            binding?.apply {
                userNameAppCompatTextView.text = text
            }
            manageUserNameClickEvent(text)
        }
    }

    private fun getProfilePhotoText(name: String?, gender: String): CharSequence {
        return when {
            gender.equals(
                "Male",
                true
            ) -> name.plus(" ").plus(context.getString(R.string.label_updated_his_profile_photo))

            gender.equals("Female", true) -> name.plus(" ").plus(
                context.getString(
                    R.string.label_updated_her_profile_photo
                )
            )

            else -> name.plus(" ").plus(
                context.getString(
                    R.string.label_updated_profile_photo
                )
            )
        }
    }

    private fun getTaggedUserText(name: String?, tagUserName: String): SpannableStringBuilder {
        return if (postDetails.taggedUsersList!!.size == 1) {
            SpannableStringBuilder().bold { append(name) }.append(" ").append("is with ").bold {
                append(tagUserName)
            }.apply {
                if (postDetails.location != null) {
                    append(" ").append("in").append(" ").bold { append(postDetails.location) }
                }
            }
        } else {
            SpannableStringBuilder().bold { append(name) }.append(" ").append("is with ").bold {
                append(tagUserName)
            }.append(" and ").bold {
                append(postDetails.taggedUsersList!!.size.minus(1).toString().plus(" other"))
            }.apply {
                if (postDetails.location != null) {
                    append(" ").append("in").append(" ").bold { append(postDetails.location) }
                }
            }
        }
    }

    private fun setRepostUserName() {
        binding?.apply {
            val name = getUserName(postDetails.sharedPost?.user)

            when {
                postDetails.sharedPost?.type.equals("profile_photo", true) -> {
                    otherUserAppCompatTextView.text =
                        getProfilePhotoUpdateText(name, postDetails.sharedPost?.user?.gender)
                }

                !postDetails.sharedPost?.taggedUsersList.isNullOrEmpty() -> {
                    postDetails.sharedPost?.taggedUsersList?.let {
                        handleTaggedUsers(name, it)
                    }
                }

                else -> {
                    otherUserAppCompatTextView.text = getLocationText(name, postDetails.sharedPost?.location)
                }
            }
        }
    }

    private fun getUserName(user: User?): String {
        return if (!user?.userName.isNullOrEmpty() && user?.userName != "null") {
            user?.userName.toString()
        } else {
            "${user?.firstName} ${user?.lastName}"
        }
    }

    private fun getProfilePhotoUpdateText(name: String, gender: String?): String {
        val actionText = when (gender?.lowercase()) {
            "male" -> R.string.label_updated_his_profile_photo
            "female" -> R.string.label_updated_her_profile_photo
            else -> R.string.label_updated_profile_photo
        }
        return "$name ${context.getString(actionText)}"
    }

    private fun handleTaggedUsers(name: String, taggedUsers: List<TaggedUser>) {
        val tagUserName = taggedUsers.first().user.userName
        if (tagUserName != null) {
            val text = if (taggedUsers.size == 1) {
                getSingleTaggedUserText(name, tagUserName, postDetails.sharedPost?.location)
            } else {
                getMultipleTaggedUsersText(name, tagUserName, taggedUsers.size, postDetails.sharedPost?.location)
            }

            binding?.apply { otherUserAppCompatTextView.text = text }
            manageRepostUserNameClickEvent(text)
        }
    }

    private fun getSingleTaggedUserText(name: String, tagUserName: String, location: String?): SpannableStringBuilder {
        val text = SpannableStringBuilder()
            .bold { append(name) }
            .append(" is with ")
            .bold { append(tagUserName) }

        location?.let {
            text.append(" in ").bold { append(it) }
        }
        return text
    }

    private fun getMultipleTaggedUsersText(
        name: String,
        tagUserName: String,
        taggedCount: Int,
        location: String?
    ): SpannableStringBuilder {
        val otherCount = taggedCount - 1
        val text = SpannableStringBuilder()
            .bold { append(name) }
            .append(" is with ")
            .bold { append(tagUserName) }
            .append(" and ")
            .bold { append("$otherCount other") }

        location?.let {
            text.append(" in ").bold { append(it) }
        }
        return text
    }

    private fun getLocationText(name: String, location: String?): SpannableStringBuilder {
        return if (location != null) {
            SpannableStringBuilder().bold { append(name) }.append(" in ").bold { append(location) }
        } else {
            SpannableStringBuilder().append(name)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun manageRepostUserNameClickEvent(text: SpannableStringBuilder) {
        val inIndex = text.indexOf("in")
        val withIndex = text.indexOf("with")
        val andIndex = text.indexOf("and")
        binding?.otherUserAppCompatTextView?.setOnTouchListener { v, event ->
            if (event.action === MotionEvent.ACTION_DOWN) {
                val layout: Layout? = (v as TextView).layout
                val x = event.x
                val y = event.y
                if (layout != null) {
                    val line: Int = layout.getLineForVertical(y.roundToInt())
                    val offset: Int = layout.getOffsetForHorizontal(line, x)

                    if (inIndex == -1) {
                        if (andIndex != -1) {
                            if (offset in (withIndex + 1)..<andIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagRepostPeopleFirstUserClick(
                                        postDetails.sharedPost
                                    )
                                )
                            } else if (offset > andIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagRepostPeopleClick(
                                        postDetails.sharedPost
                                    )
                                )
                            }
                        } else {
                            if (offset > withIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagRepostPeopleFirstUserClick(
                                        postDetails.sharedPost
                                    )
                                )
                            }
                        }
                    } else {
                        if (andIndex != -1) {
                            if (offset in (andIndex + 1)..<inIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagRepostPeopleClick(
                                        postDetails.sharedPost
                                    )
                                )
                            } else if (offset in (withIndex + 1)..<andIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagRepostPeopleFirstUserClick(
                                        postDetails.sharedPost
                                    )
                                )
                            }
                        } else {
                            if (offset in (withIndex + 1)..<inIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagRepostPeopleFirstUserClick(
                                        postDetails.sharedPost
                                    )
                                )
                            }
                        }
                    }
                }
            }
            true
        }
    }

    override fun onDestroy() {
        binding = null
        currentNativeAd?.destroy()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun manageUserNameClickEvent(text: SpannableStringBuilder) {
        val inIndex = text.indexOf("in")
        val withIndex = text.indexOf("with")
        val andIndex = text.indexOf("and")
        binding?.userNameAppCompatTextView?.setOnTouchListener { v, event ->
            if (event.action === MotionEvent.ACTION_DOWN) {
                val layout: Layout? = (v as TextView).layout
                val x = event.x
                val y = event.y
                if (layout != null) {
                    val line: Int = layout.getLineForVertical(y.roundToInt())
                    val offset: Int = layout.getOffsetForHorizontal(line, x)
                    if (inIndex == -1) {
                        if (andIndex != -1) {
                            if (offset in (withIndex + 1)..<andIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagPeopleFirstUserClick(
                                        postDetails
                                    )
                                )
                            } else if (offset > andIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagPeopleClick(
                                        postDetails
                                    )
                                )
                            }
                        } else {
                            if (offset > withIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagPeopleFirstUserClick(postDetails)
                                )
                            }
                        }
                    } else {
                        if (andIndex != -1) {
                            if (offset in (andIndex + 1)..<inIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagPeopleClick(postDetails)
                                )
                            } else if (offset in (withIndex + 1)..<andIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagPeopleFirstUserClick(postDetails)
                                )
                            }
                        } else {
                            if (offset in (withIndex + 1)..<inIndex) {
                                postViewClicksSubject.onNext(
                                    HomePagePostInfoState.TagPeopleFirstUserClick(postDetails)
                                )
                            }
                        }
                    }
                }
            }
            true
        }
    }

    private fun updateLikeStatusCount() {
        if (postDetails.isLikedCount == 0) {
            postDetails.isLikedCount = 1
        } else {
            postDetails.isLikedCount = 0
        }
        if (postDetails.isLikedCount == 1) {
            postDetails.postLikesCount = postDetails.postLikesCount.let { (it ?: 0) + 1 }
            updatePostLike()
            postViewClicksSubject.onNext(HomePagePostInfoState.AddPostLikeClick(postDetails))
        } else {
            postDetails.postLikesCount = postDetails.postLikesCount.let { (it ?: 0) - 1 }
            updatePostLike()
            postViewClicksSubject.onNext(HomePagePostInfoState.RemovePostLikeClick(postDetails))
        }
    }

    private fun updatePostLike() {
        binding?.apply {
            if (postDetails.isLikedCount == 1) {
                likeAppCompatImageView.setImageResource(R.drawable.ic_fill_heart)
            } else {
                likeAppCompatImageView.setImageResource(R.drawable.ic_gray_heart)
            }

            val totalLikes = postDetails.postLikesCount
            if (totalLikes != null) {
                if (totalLikes != 0) {
                    totalLikesAppCompatTextView.text = totalLikes.prettyCount().toString()
                    totalLikesAppCompatTextView.visibility = View.VISIBLE
                } else {
                    totalLikesAppCompatTextView.text = ""
                    totalLikesAppCompatTextView.visibility = View.INVISIBLE
                }
            } else {
                totalLikesAppCompatTextView.text = ""
                totalLikesAppCompatTextView.visibility = View.INVISIBLE
            }
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        MobileAds.initialize(context) {
            refreshAd()
        }
    }

    private fun refreshAd() {
        val builder = AdLoader.Builder(context, loggedInUserCache.getNativeAdId())

        builder.forNativeAd { nativeAd ->
            val activityDestroyed = false
            if (activityDestroyed) {
                nativeAd.destroy()
                return@forNativeAd
            }

            currentNativeAd?.destroy()
            currentNativeAd = nativeAd
            val layoutInflater: LayoutInflater = LayoutInflater.from(context)
            val unifiedAdBinding = AdUnifiedBinding.inflate(layoutInflater)
            FileUtils.loadNativeAd(nativeAd, unifiedAdBinding)
            binding?.adFrame?.removeAllViews()
            binding?.adFrame?.addView(unifiedAdBinding.root)
        }

        val videoOptions = VideoOptions.Builder().setStartMuted(false).build()

        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()

        builder.withNativeAdOptions(adOptions)

        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                return
            }
        }).build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun convertCoordinates(
        originalX: Float,
        originalY: Float,
        originalHeight: Float,
        newHeight: Float
    ): Pair<Float, Float> {
        val scaleFactor = newHeight / originalHeight
        val newX = originalX * scaleFactor
        val newY = originalY * scaleFactor
        return Pair(newX, newY)
    }
}

