package com.meetfriend.app.ui.challenge.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.ChallengeItem
import com.meetfriend.app.api.challenge.model.ChallengeType
import com.meetfriend.app.api.post.model.ChallengePageState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewPlayChallengeBinding
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utilclasses.UtilsClass
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.properties.Delegates
import android.widget.FrameLayout
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.view.ViewGroup
import android.widget.ImageView
import com.meetfriend.app.api.challenge.model.User
import com.meetfriend.app.api.post.model.PositionData
import com.meetfriend.app.api.post.model.SetUpLinkViewData
import com.meetfriend.app.ui.main.view.MainVideoPostView.Companion.FIXED_INT_3
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FiXED_1000_INT
import com.meetfriend.app.utils.Constant.FiXED_100_FLOAT
import com.meetfriend.app.utils.Constant.FiXED_100_INT
import com.meetfriend.app.utils.Constant.FiXED_130_INT
import com.meetfriend.app.utils.Constant.FiXED_150_INT
import com.meetfriend.app.utils.Constant.FiXED_200_INT
import com.meetfriend.app.utils.Constant.FiXED_210_FLOAT
import com.meetfriend.app.utils.Constant.FiXED_24_INT
import com.meetfriend.app.utils.Constant.FiXED_30_FLOAT
import com.meetfriend.app.utils.Constant.FiXED_365_INT
import com.meetfriend.app.utils.Constant.FiXED_60_INT
import com.meetfriend.app.utils.Constant.FiXED_MARGIN_40

class PlayChallengeView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val playShortsViewClicksSubject: PublishSubject<ChallengePageState> = PublishSubject.create()
    val playShortsViewClicks: Observable<ChallengePageState> = playShortsViewClicksSubject.hide()

    private var binding: ViewPlayChallengeBinding? = null
    private lateinit var dataVideo: ChallengeItem

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    private var challengeType: ChallengeType = ChallengeType.AllChallenge

    init {
        inflateUi()
    }

    private fun inflateUi() {
        MeetFriendApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()
        val view = View.inflate(context, R.layout.view_play_challenge, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding = ViewPlayChallengeBinding.bind(view)
        setupRxBusListeners()
        setupClickListeners()
    }

    private fun setupRxBusListeners() {
        RxBus.listen(RxEvent.RefreshPlayerView::class.java).subscribeAndObserveOnMainThread {
            Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
        }
    }

    private fun setupClickListeners() {
        binding?.apply {
            ivUserProfileImage.setClickAction {
                playShortsViewClicksSubject.onNext(ChallengePageState.UserProfileClick(dataVideo))
            }

            likeAppCompatImageView.setClickAction {
                updateLikeStatusCount()
            }

            ivComment.setClickAction {
                playShortsViewClicksSubject.onNext(ChallengePageState.CommentClick(dataVideo))
            }

            moreAppCompatImageView.setClickAction {
                playShortsViewClicksSubject.onNext(ChallengePageState.MoreClick(dataVideo))
            }

            ivDownload.setClickAction {
                playShortsViewClicksSubject.onNext(ChallengePageState.ShareClick(dataVideo))
            }

            replyChallenge.setClickAction {
                handleReplyChallengeClick()
            }

            ivWinnerUser.setClickAction {
                playShortsViewClicksSubject.onNext(
                    ChallengePageState.UserChallengeWinnerProfileClick(dataVideo)
                )
            }

            llJoin.setClickAction {
                locationPermission()
            }

            tvFollow.setClickAction {
                tvFollow.isVisible = false
                playShortsViewClicksSubject.onNext(ChallengePageState.FollowClick(dataVideo))
            }

            tvChallengeDescription.setOnHashtagClickListener { _, text ->
                playShortsViewClicksSubject.onNext(
                    ChallengePageState.HashtagClick(text.toString(), dataVideo)
                )
            }
        }
    }

    private fun handleReplyChallengeClick() {
        if ((dataVideo.challengePostUser?.size ?: 0) > 0) {
            playShortsViewClicksSubject.onNext(ChallengePageState.UserChallengeReplyProfileClick(dataVideo))
        } else {
            Toast.makeText(context, "No user joined challenge yet.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun View.setClickAction(action: () -> Unit) {
        throttleClicks().subscribeAndObserveOnMainThread { action() }.autoDispose()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(videoData: ChallengeItem?, challengeT: ChallengeType) {
        challengeType = challengeT
        if (videoData != null) {
            this.dataVideo = videoData
        }
        binding?.apply {
            setupUserInfo()
            setupUserProfileImages()
            setupChallengeDetails()
            setupChallengePostUsers()
            setupChallengeStatus()

            if (dataVideo.getIsFilePathImage()) {
                setupViewsVisibility()
                loadImageIntoPhotoView(videoData)
                setupLinkAttachment(videoData)
            } else {
                setupMusicInfo(videoData)
                setupVideoDisplayType(videoData)
                setupJzvdStd(videoData)
                setLinkAttachmentContainerSize(videoData)
                if (!dataVideo.webLink.isNullOrEmpty()) {
                    linkAttachmentContainer.visibility = View.VISIBLE
                    setupLinkAttachmentForVideo(dataVideo)
                } else {
                    hideLinkAttachment()
                }
            }
        }
    }

    private fun setupLinkAttachmentForVideo(dataVideo: ChallengeItem) {
        val inputString = dataVideo.position
        val parsePositionData = parsePositionData(inputString)
        val dimension = getFullScreenDimensions(dataVideo)

        val buttonX = (parsePositionData.x * dimension.second) / FiXED_100_INT
        val buttonY = (parsePositionData.y * dimension.first) / FiXED_100_INT

        val rotation = dataVideo.rotationAngle ?: 0.0f
        val view = inflateLinkView()

        if (parsePositionData.x != 0f && parsePositionData.y != 0f) {
            val setUpLinkViewData = SetUpLinkViewData(
                dataVideo.platform,
                buttonX,
                buttonY,
                parsePositionData.width,
                parsePositionData.height
            )
            setLinkViewPosition(
                setUpLinkViewData,
                view
            )
        } else {
            setLinkViewDefaultLayoutParams(view, parsePositionData.width, parsePositionData.height)
        }

        view.rotation = rotation
        updateLinkAttachmentContainer(view)
        setLinkClickListener(view, dataVideo)
    }

    private fun parsePositionData(inputString: String?): PositionData {
        val trimmedString = inputString?.trim('[', ']')
        val stringArray = trimmedString?.split(",")
        val doubleList = stringArray?.map { it.toDouble() }

        val lastX = doubleList?.get(0)?.toFloat() ?: 0f
        val lastY = doubleList?.get(1)?.toFloat() ?: 0f
        val buttonWidth = doubleList?.get(2)?.toInt() ?: 0
        val buttonHeight = doubleList?.get(FIXED_INT_3)?.toInt() ?: 0
        return PositionData(lastX, lastY, buttonWidth, buttonHeight)
    }

    private fun getFullScreenDimensions(dataVideo: ChallengeItem): Pair<Int, Int> {
        val fullScreenHeight = dataVideo.height ?: resources.displayMetrics.heightPixels
        val fullScreenWidth = dataVideo.width ?: resources.displayMetrics.widthPixels
        return Pair(fullScreenHeight, fullScreenWidth)
    }

    private fun inflateLinkView(): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.link_click_view, binding?.linkAttachmentContainer, false)
    }

    private fun setLinkViewPosition(
        setUpLinkViewData: SetUpLinkViewData,
        view: View
    ) {
        if (setUpLinkViewData.platform == "ios") {
            view.x = setUpLinkViewData.buttonX - FiXED_100_FLOAT
            view.y = setUpLinkViewData.buttonY - FiXED_30_FLOAT
            val layoutParams = view.layoutParams
            layoutParams.width = (setUpLinkViewData.buttonWidth + FiXED_200_INT).toInt()
            layoutParams.height = if (setUpLinkViewData.buttonHeight < FiXED_130_INT) {
                FiXED_150_INT
            } else {
                setUpLinkViewData.buttonHeight.toInt()
            }
            view.layoutParams = layoutParams
        } else {
            view.x = setUpLinkViewData.buttonX
            view.y = setUpLinkViewData.buttonY
            val layoutParams = view.layoutParams
            layoutParams.width = setUpLinkViewData.buttonWidth
            layoutParams.height = setUpLinkViewData.buttonHeight
            view.layoutParams = layoutParams
        }
    }

    private fun setLinkViewDefaultLayoutParams(view: View, buttonWidth: Int, buttonHeight: Int) {
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
            marginEnd = FiXED_MARGIN_40
            marginStart = FiXED_MARGIN_40
            width = buttonWidth
            height = buttonHeight
        }
        view.layoutParams = layoutParams
    }

    private fun updateLinkAttachmentContainer(view: View) {
        binding?.apply {
            linkAttachmentContainer.removeAllViews()
            linkAttachmentContainer.addView(view)
        }
    }

    private fun setLinkClickListener(view: View, dataVideo: ChallengeItem) {
        view.findViewById<LinearLayout>(R.id.llAttachment).setOnClickListener {
            playShortsViewClicksSubject.onNext(ChallengePageState.OpenLinkAttachment(dataVideo))
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

    private fun setupMusicInfo(videoData: ChallengeItem?) {
        binding?.apply {
            if (videoData?.musicTitle != null) {
                musicLinearLayout.isVisible = true
                tvMusicName.text = videoData.musicTitle.plus(" • ").plus(videoData.artists)
                tvMusicName.isSelected = true
            } else {
                musicLinearLayout.isVisible = true
                val userName = videoData?.user?.userName?.takeIf { !it.isNullOrEmpty() && it != "null" }
                    ?: videoData?.user?.let { it.firstName.plus(" ").plus(it.lastName) }
                tvMusicName.text = "$userName • Original Audio"
                tvMusicName.isSelected = true
            }
        }
    }

    private fun setupVideoDisplayType(videoData: ChallengeItem?) {
        binding?.apply {
            if (videoData?.width != null && videoData.height != null) {
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

    private fun setupJzvdStd(videoData: ChallengeItem?) {
        binding?.apply {
            jzvdStdOutgoer.isVisible = true
            photoView.isVisible = false
            photoLayout.isVisible = false
            videoLayout.isVisible = true

            Glide.with(context)
                .load(videoData?.filePath)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(jzvdStdOutgoer.posterImageView)

            jzvdStdOutgoer.setUp(videoData?.filePath, "")
            jzvdStdOutgoer.jzDataSource.looping = true
            jzvdStdOutgoer.startVideoAfterPreloading()
        }
    }

    private fun setLinkAttachmentContainerSize(videoData: ChallengeItem?) {
        binding?.apply {
            linkAttachmentContainer.layoutParams?.apply {
                this.height = videoData?.height ?: resources.displayMetrics.heightPixels
                this.width = videoData?.width ?: resources.displayMetrics.widthPixels
            }
        }
    }

    private fun setupLinkAttachment(dataVideo: ChallengeItem?) {
        binding?.apply {
            if (!dataVideo?.webLink.isNullOrEmpty()) {
                linkAttachmentContainer.visibility = View.VISIBLE
                val position = dataVideo?.position?.trim('[', ']')?.split(",")?.map { it.toDouble() }
                position?.let {
                    val lastX = it[0].toFloat()
                    val lastY = it[1].toFloat()
                    val buttonWidth = it[2].toInt()
                    val buttonHeight = it.getOrNull(3)?.toInt() ?: 0
                    val rotation = dataVideo.rotationAngle ?: 0.0f
                    setupLinkView(lastX, lastY, buttonWidth, buttonHeight, rotation, dataVideo)
                }
            } else {
                clearLinkAttachment()
            }
        }
    }

    private fun setupLinkView(
        lastX: Float,
        lastY: Float,
        buttonWidth: Int,
        buttonHeight: Int,
        rotation: Float,
        dataVideo: ChallengeItem
    ) {
        binding?.apply {
            val view = LayoutInflater.from(context).inflate(R.layout.link_click_view, linkAttachmentContainer, false)
            setViewPositionAndSize(view, lastX, lastY, buttonWidth, buttonHeight, dataVideo.platform)
            view.rotation = rotation
            linkAttachmentContainer.removeAllViews()
            linkAttachmentContainer.addView(view)
            view.findViewById<LinearLayout>(R.id.llAttachment).setOnClickListener {
                playShortsViewClicksSubject.onNext(ChallengePageState.OpenLinkAttachment(dataVideo))
            }
        }
    }

    private fun setViewPositionAndSize(
        view: View,
        lastX: Float,
        lastY: Float,
        buttonWidth: Int,
        buttonHeight: Int,
        platform: String?
    ) {
        if (lastX != 0f && lastY != 0f) {
            if (platform == "ios") {
                view.x = lastX
                view.y = calculateIosY(lastY)
                val layoutParams = view.layoutParams
                layoutParams.width = buttonWidth + FiXED_200_INT
                layoutParams.height = if (buttonHeight < FiXED_130_INT) FiXED_130_INT else buttonHeight
                view.layoutParams = layoutParams
            } else {
                view.x = lastX
                view.y = lastY
                val layoutParams = view.layoutParams
                layoutParams.width = buttonWidth
                layoutParams.height = buttonHeight
                view.layoutParams = layoutParams
            }
        } else {
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                marginEnd = FiXED_MARGIN_40
                marginStart = FiXED_MARGIN_40
                width = buttonWidth
                height = buttonHeight
            }
            view.layoutParams = layoutParams
        }
    }

    private fun calculateIosY(lastY: Float): Float {
        return if ((dataVideo.width ?: 0) > (dataVideo.height ?: 0)) {
            lastY + FiXED_100_FLOAT
        } else {
            lastY + FiXED_210_FLOAT
        }
    }

    private fun clearLinkAttachment() {
        binding?.apply {
            if (linkAttachmentContainer.childCount > 0) {
                linkAttachmentContainer.removeAllViews()
                linkAttachmentContainer.visibility = View.GONE
            }
        }
    }

    private fun setupViewsVisibility() {
        binding?.apply {
            jzvdStdOutgoer.isVisible = false
            videoLayout.isVisible = false
            photoView.isVisible = true
            photoLayout.isVisible = true
            musicLinearLayout.isVisible = false
        }
    }

    private fun loadImageIntoPhotoView(videoData: ChallengeItem?) {
        binding?.apply {
            Glide.with(context)
                .load(videoData?.filePath)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(photoView)

            photoView.post {
                Glide.with(context)
                    .load(videoData?.filePath)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            setLinkAttachmentContainerSize(resource.intrinsicWidth, resource.intrinsicHeight)
                            return false
                        }
                    }).into(photoView)
            }
        }
    }

    private fun setLinkAttachmentContainerSize(width: Int, height: Int) {
        binding?.apply {
            linkAttachmentContainer.layoutParams?.apply {
                this.width = width
                this.height = height
            }
        }
    }

    private fun setupUserInfo() {
        binding?.apply {
            tvUserName.text = context.getString(R.string.sign_at_the_rate).plus(dataVideo.user?.userName)
            tvShortsTime.text = dataVideo.createdAt
            tvName.text = if (!dataVideo.user?.userName.isNullOrEmpty() && dataVideo.user?.userName != "null") {
                dataVideo.user?.userName
            } else {
                dataVideo.user?.firstName.plus(" ").plus(dataVideo.user?.lastName)
            }

            Glide.with(context)
                .load(dataVideo.user?.profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivUserProfileImage)

            ivAccountVerified.visibility = if (dataVideo.user?.isVerified == 1) View.VISIBLE else View.GONE

            if (dataVideo.user?.id == loggedInUserId) {
                tvFollow.isVisible = false
                tvFollowing.isVisible = false
            } else {
                tvFollow.visibility = if (dataVideo.followBack == 0) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupChallengeDetails() {
        binding?.apply {
            tvChallengeName.visibility = if (dataVideo.title.isNullOrEmpty()) View.GONE else View.VISIBLE
            tvChallengeDescription.visibility = if (dataVideo.description.isNullOrEmpty()) View.GONE else View.VISIBLE
            tvChallengeName.text = dataVideo.title
            tvChallengeDescription.text = dataVideo.description
            tvLikeCount.text = dataVideo.noOfLikesCount.toString()
            tvWatchCount.text = dataVideo.noOfViewsCount.toString()
            tvCommentCount.text = dataVideo.noOfCommentCount.toString()
            tvCountry.text = dataVideo.challengeCountry?.firstOrNull()?.countryData?.name
        }
    }

    private fun setupUserProfileImages() {
        binding?.apply {
            if (dataVideo.status == 2 && dataVideo.winnerUser != null) {
                ivWinnerUser.visibility = View.VISIBLE
                Glide.with(context)
                    .load(dataVideo.winnerUser?.profilePhoto)
                    .placeholder(R.drawable.ic_empty_profile_placeholder)
                    .error(R.drawable.ic_empty_profile_placeholder)
                    .into(ivWinnerProfile)
            } else {
                ivWinnerUser.visibility = View.INVISIBLE
            }
        }
    }

    private fun setupChallengePostUsers() {
        binding?.apply {
            profileRelativeLayout.visibility = if (dataVideo.challengePostCount ?: 0 > 0) View.VISIBLE else View.GONE

            dataVideo.challengePostUser?.let {
                replyCountAppCompatTextView.text = dataVideo.challengePostCount.toString()

                if (it.size > Constant.FiXED_3_INT) {
                    setupThreeProfileImages(it)
                } else if (it.size > Constant.FiXED_2_INT) {
                    setupTwoProfileImages(it)
                } else if (it.size > 1) {
                    setupOneProfileImage(it)
                } else {
                    hideProfileImages()
                }
            }
        }
    }

    private fun setupChallengeStatus() {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val date1 = Date()
        val date2: Date = simpleDateFormat.parse(dataVideo?.dateTo + " " + dataVideo?.timeTo)
        val date3: Date = simpleDateFormat.parse(dataVideo?.dateFrom + " " + dataVideo?.timeFrom)

        when (dataVideo.status) {
            0 -> handleUpcomingChallenge(date1, date3)
            1 -> handleLiveChallenge(date1, date2)
            2 -> handleCompletedChallenge()
            else -> handleDefaultChallengeStatus()
        }
    }

    private fun setupThreeProfileImages(it: List<User>) {
        binding?.apply {
            rlFirstContainer.visibility = View.VISIBLE
            rlSecondContainer.visibility = View.VISIBLE
            rlThirdContainer.visibility = View.VISIBLE

            Glide.with(context)
                .load(it[0].profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivFirstUser)

            Glide.with(context)
                .load(it[1].profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivSecondUser)

            Glide.with(context)
                .load(it[Constant.FiXED_2_INT].profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivThirdUser)
        }
    }

    private fun setupTwoProfileImages(it: List<User>) {
        binding?.apply {
            rlFirstContainer.visibility = View.VISIBLE
            rlSecondContainer.visibility = View.VISIBLE
            rlThirdContainer.visibility = View.GONE

            Glide.with(context)
                .load(it[0].profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivFirstUser)

            Glide.with(context)
                .load(it[1].profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivSecondUser)
        }
    }

    private fun setupOneProfileImage(it: List<User>) {
        binding?.apply {
            rlFirstContainer.visibility = View.VISIBLE
            rlSecondContainer.visibility = View.GONE
            rlThirdContainer.visibility = View.GONE

            Glide.with(context)
                .load(it[0].profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(ivFirstUser)
        }
    }

    private fun hideProfileImages() {
        binding?.apply {
            rlFirstContainer.visibility = View.GONE
            rlSecondContainer.visibility = View.GONE
            rlThirdContainer.visibility = View.GONE
            moreAppCompatTextView.visibility = View.GONE
        }
    }

    private fun handleUpcomingChallenge(date1: Date, date3: Date) {
        binding?.apply {
            llLive.isVisible = false
            llUpcoming.isVisible = true
            llComplete.isVisible = false
            itemLinearLayout.isVisible = true
            tvStartTime.text = "${dataVideo?.timeFrom} ${UtilsClass.formatDate(dataVideo?.dateFrom)}"
            val time = printUpcommingDifference(date3, date1)
            if (time.equals("Challenge Upcoming", ignoreCase = true)) {
                llLive.isVisible = false
            } else {
                llLive.isVisible = true
                val time1 = printDifference(date1, date3)
                if (time1.equals("Challenge Over", ignoreCase = true)) {
                    itemLinearLayout.isVisible = false
                } else {
                    itemAppCompatTextView.text = "Time left:"
                    tvStartTime.text = time1
                }
            }
        }
    }

    private fun handleLiveChallenge(date1: Date, date2: Date) {
        binding?.apply {
            llLive.isVisible = true
            llUpcoming.isVisible = false
            llComplete.isVisible = false
            itemLinearLayout.isVisible = true
            val time = printDifference(date1, date2)
            if (time.equals("Challenge Over", ignoreCase = true)) {
                itemLinearLayout.isVisible = false
                llLive.isVisible = false
                llComplete.isVisible = true
            } else {
                itemAppCompatTextView.text = "Time left:"
                tvStartTime.text = time
            }
        }
    }

    private fun handleCompletedChallenge() {
        binding?.apply {
            llLive.isVisible = false
            llUpcoming.isVisible = false
            llComplete.isVisible = true
            itemLinearLayout.isVisible = false
        }
    }

    private fun handleDefaultChallengeStatus() {
        binding?.apply {
            llLive.isVisible = false
            llUpcoming.isVisible = true
            llComplete.isVisible = false
            itemLinearLayout.isVisible = true
            tvStartTime.text = "${dataVideo.timeFrom} ${UtilsClass.formatDate(dataVideo.dateFrom)}"
        }
    }

    fun shouldShowJoin(dataVideo: ChallengeItem): Boolean {
        val cityAll = dataVideo.challengeCity?.any { it?.cityData?.name == "All" } == true
        val stateAll = dataVideo.challengeState?.any { it?.stateData?.name == "All" } == true
        val countryAll = dataVideo.challengeCountry?.any { it?.countryData?.name == "All" } == true

        val userCityMatch = dataVideo.challengeCity?.any { it?.cityData?.name == dataVideo.userCity } == true
        val userStateMatch = dataVideo.challengeState?.any { it?.stateData?.name == dataVideo.userState } == true
        val userCountryMatch = dataVideo.challengeCountry?.any {
            it?.countryData?.name == dataVideo.userCountry
        } == true

        return when {
            cityAll && stateAll && countryAll -> true
            cityAll && stateAll && userCountryMatch -> true
            cityAll && userStateMatch -> true
            userCityMatch -> true
            else -> false
        }
    }

    private fun updateReelLike() {
        binding?.apply {
            if (dataVideo.isLikeCount == 1) {
                likeAppCompatImageView.setImageResource(R.drawable.ic_fill_heart)
            } else {
                likeAppCompatImageView.setImageResource(R.drawable.ic_heart)
            }
            tvLikeCount.text = dataVideo.noOfLikesCount?.prettyCount().toString()
        }
    }

    private fun updateLikeStatusCount() {
        if (challengeType == ChallengeType.MyChallenge) {
            handleMyChallengeLike()
        } else {
            toggleLikeStatus()
            updateLikeCount()
        }
    }

    private fun handleMyChallengeLike() {
        playShortsViewClicksSubject.onNext(ChallengePageState.ReelLikeClick(dataVideo))
    }

    private fun toggleLikeStatus() {
        dataVideo.isLikeCount = if (dataVideo.isLikeCount == 0) 1 else 0
    }

    private fun updateLikeCount() {
        if (dataVideo.isLikeCount == 1) {
            dataVideo.noOfLikesCount = dataVideo.noOfLikesCount?.let { it + 1 } ?: 1
            updateReelLike()
            playShortsViewClicksSubject.onNext(ChallengePageState.AddReelLikeClick(dataVideo))
        } else {
            dataVideo.noOfLikesCount = (dataVideo.noOfLikesCount ?: 0).coerceAtLeast(1) - 1
            updateReelLike()
            playShortsViewClicksSubject.onNext(ChallengePageState.RemoveReelLikeClick(dataVideo))
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    fun printDifference(startDate: Date, endDate: Date): String {
        val differenceInTime = endDate.time - startDate.time
        val differenceInSeconds = ((differenceInTime / FiXED_1000_INT) % FiXED_60_INT)
        val elapsedMinutes = ((differenceInTime / (FiXED_1000_INT * FiXED_60_INT)) % FiXED_60_INT)
        val elapsedHours = ((differenceInTime / (FiXED_1000_INT * FiXED_60_INT * FiXED_60_INT)) % FiXED_24_INT)
        val secondsInDay = FiXED_1000_INT * FiXED_60_INT * FiXED_60_INT * FiXED_24_INT
        val daysInYear = FiXED_365_INT
        val elapsedDays = (differenceInTime / secondsInDay) % daysInYear
        return when {
            elapsedDays > 0 -> {
                String.format(Locale.getDefault(), "%d Day Left", elapsedDays)
            }

            elapsedHours > 0 -> {
                String.format(Locale.getDefault(), "%d Hour Left", elapsedHours)
            }

            elapsedMinutes > 0 -> {
                String.format(Locale.getDefault(), "%d Minutes Left", elapsedMinutes)
            }

            differenceInSeconds > 0 -> {
                String.format(Locale.getDefault(), "%d Seconds Left", differenceInSeconds)
            }

            else -> {
                "Challenge Over"
            }
        }
    }

    fun printUpcommingDifference(startDate: Date, endDate: Date): String {
        val differenceInTime = endDate.time - startDate.time
        val differenceInSeconds = ((differenceInTime / FiXED_1000_INT) % FiXED_60_INT)
        val elapsedMinutes = ((differenceInTime / (FiXED_1000_INT * FiXED_60_INT)) % FiXED_60_INT)
        val elapsedHours = ((differenceInTime / (FiXED_1000_INT * FiXED_60_INT * FiXED_60_INT)) % FiXED_24_INT)
        val secondsInDay = FiXED_1000_INT * FiXED_60_INT * FiXED_60_INT * FiXED_24_INT
        val daysInYear = FiXED_365_INT
        val elapsedDays = (differenceInTime / secondsInDay) % daysInYear
        return when {
            elapsedDays > 0 -> {
                String.format(Locale.getDefault(), "%d Day Left", elapsedDays)
            }

            elapsedHours > 0 -> {
                String.format(Locale.getDefault(), "%d Hour Left", elapsedHours)
            }

            elapsedMinutes > 0 -> {
                String.format(Locale.getDefault(), "%d Minutes Left", elapsedMinutes)
            }

            differenceInSeconds > 0 -> {
                String.format(Locale.getDefault(), "%d Seconds Left", differenceInSeconds)
            }

            else -> {
                "Challenge Upcoming"
            }
        }
    }

    private fun locationPermission() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            playShortsViewClicksSubject.onNext(ChallengePageState.JoinChallengeClick(dataVideo))
        } else {
            locationPermissionDialog(listOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION"))
        }
    }

    private fun locationPermissionDialog(permissions: List<String>) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(resources.getString(R.string.label_location_permission))
        builder.setMessage(resources.getString(R.string.desc_why_location_permission_required))
        builder.setPositiveButton(
            resources.getString(R.string.label_yes)
        ) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            XXPermissions.startPermissionActivity(context, permissions)
        }

        builder.setNegativeButton(resources.getString(R.string.label_cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }
}
