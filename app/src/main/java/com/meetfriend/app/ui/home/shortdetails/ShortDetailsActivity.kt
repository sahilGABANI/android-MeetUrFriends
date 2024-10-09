package com.meetfriend.app.ui.home.shortdetails

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.text.SpannableStringBuilder
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import androidx.core.view.isVisible
import androidx.recyclerview.widget.OrientationHelper
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.api.post.model.PostLikeUnlikeRequest
import com.meetfriend.app.api.post.model.PostLikesInformation
import com.meetfriend.app.api.post.model.PostShareRequest
import com.meetfriend.app.api.post.model.ShortsCountRequest
import com.meetfriend.app.api.post.model.ShortsDetailPageState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityShortDetailsBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.video.Post
import com.meetfriend.app.ui.activities.FullScreenActivity
import com.meetfriend.app.ui.activities.VideoPlayActivity
import com.meetfriend.app.ui.giftsGallery.GiftGalleryBottomSheet
import com.meetfriend.app.ui.hashtag.HashTagListActivity
import com.meetfriend.app.ui.home.SharePostBottomSheet
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.shortdetails.view.PlayShortsDetailAdapter
import com.meetfriend.app.ui.home.shortdetails.viewmodel.ShortDetailsViewModel
import com.meetfriend.app.ui.home.shortdetails.viewmodel.ShortDetailsViewState
import com.meetfriend.app.ui.home.shorts.ShortsCommentBottomSheet
import com.meetfriend.app.ui.home.shorts.report.ReportDialogFragment
import com.meetfriend.app.ui.livestreaming.LiveStreamingMoreOptionBottomSheet
import com.meetfriend.app.ui.main.MainHomeActivity.Companion.INTENT_IS_SWITCH_ACCOUNT
import com.meetfriend.app.ui.main.MainHomeActivity.Companion.USER_ID
import com.meetfriend.app.ui.main.post.BottomSheetPostMoreItem
import com.meetfriend.app.ui.main.post.PostLikesDialog
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.ui.tag.TaggedUserActivity
import com.meetfriend.app.utilclasses.download.DownloadService
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.SHORT
import com.meetfriend.app.utils.FileUtils
import com.meetfriend.app.utils.ShareHelper
import com.meetfriend.app.utils.UiUtils
import com.meetfriend.app.videoplayer.JZMediaExoKotlin
import com.meetfriend.app.videoplayer.JzvdStdOutgoer
import com.meetfriend.app.videoplayer.OnViewPagerListener
import com.meetfriend.app.videoplayer.ViewPagerLayoutManager
import javax.inject.Inject
import kotlin.math.roundToInt

class ShortDetailsActivity : BasicActivity() {

    private lateinit var binding: ActivityShortDetailsBinding

    companion object {
        const val POST_ID = "POST_ID"
        const val INTENT_IS_COMMENT = "INTENT_IS_COMMENT"
        const val TAG = "ShortDetailsActivity"
        fun getIntent(context: Context, postId: Int, isComment: Boolean? = false): Intent {
            val intent = Intent(context, ShortDetailsActivity::class.java)
            intent.putExtra(POST_ID, postId)
            intent.putExtra(INTENT_IS_COMMENT, isComment)
            return intent
        }
    }

    private var postId: String = ""
    private var isLikes: Boolean = false
    private var videoData: Post? = null

    private lateinit var playShortsAdapter: PlayShortsDetailAdapter
    private lateinit var videoLayoutManager: ViewPagerLayoutManager
    private var isVideoInitCompleted = false
    private var mCurrentPosition = -1
    private var listOfReelsInfo: List<Post>? = null
    private var dataVideo: Post? = null
    private var displayComment = false
    private var deleteShortId: Int = -1
    private var isShortDelete: Boolean = false
    private var postStatus: String = ""
    private var likeUnlikeId: Int = -1

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ShortDetailsViewModel>
    private lateinit var shortDetailsViewModel: ShortDetailsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)

        binding = ActivityShortDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        shortDetailsViewModel = getViewModelFromFactory(viewModelFactory)

        loadDataFromIntent()
        listenToViewModel()
        initUI()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            val postId = it.getIntExtra(POST_ID, 0).toString()
            displayComment = it.getBooleanExtra(INTENT_IS_COMMENT, false)
            if (postId != "0") {
                this.postId = postId
                listenToViewEvents()
                shortDetailsViewModel.viewPost(postId)
            } else {
                onBackPressed()
            }
            if (it.getBooleanExtra(INTENT_IS_SWITCH_ACCOUNT, false)) {
                it.getIntExtra(USER_ID, 0).let { userId ->
                    RxBus.publish(RxEvent.switchUserAccount(userId))
                }
            }
        } ?: onBackPressed()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listenToViewEvents() {
        binding.backAppCompatImageViewq.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
            RxBus.publish(RxEvent.UserProfileLikeUnlike(likeUnlikeId, postStatus))
        }.autoDispose()

        videoLayoutManager = ViewPagerLayoutManager(this, OrientationHelper.VERTICAL)
        playShortsAdapter = PlayShortsDetailAdapter(this).apply {
            playShortsViewClicks.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is ShortsDetailPageState.UserProfileClick -> {
                        startActivity(
                            MyProfileActivity.getIntentWithData(
                                this@ShortDetailsActivity, state.dataVideo.user_id
                            )
                        )
                    }

                    is ShortsDetailPageState.AddReelLikeClick -> {
                        state.dataVideo.post?.let { likeShort(it, true) }
                    }
                    is ShortsDetailPageState.RemoveReelLikeClick -> {
                        state.dataVideo.post?.let { likeShort(it, false) }
                    }
                    is ShortsDetailPageState.CommentClick -> {
                        Jzvd.goOnPlayOnPause()
                        dataVideo = state.dataVideo.post
                        val bottomSheet = ShortsCommentBottomSheet.newInstance(state.dataVideo.post?.id.toString())
                        bottomSheet.show(supportFragmentManager, ShortsCommentBottomSheet::class.java.name)
                    }

                    is ShortsDetailPageState.ShareClick -> {
                        state.dataVideo.post?.id?.let {
                            ShareHelper.shareDeepLink(
                                this@ShortDetailsActivity, 1, it, "", true
                            ) { item ->
                                ShareHelper.shareText(this@ShortDetailsActivity, item)
                            }
                        }
                    }

                    is ShortsDetailPageState.OpenLinkAttachment -> {
                        state.dataVideo.post?.web_link?.let { openUrlInBrowser(it) }
                    }

                    is ShortsDetailPageState.FollowClick -> {
                        state.dataVideo.post?.user?.id?.let {
                            shortDetailsViewModel.followUnfollow(it)

                            val list = playShortsAdapter.listOfDataItems
                            list?.filter { item ->
                                item.post?.user_id == state.dataVideo.post.user.id
                            }?.forEach { item ->
                                item.followBack = 1
                            }
                            playShortsAdapter.listOfDataItems = list
                            notifyDataSetChanged()
                        }
                    }

                    is ShortsDetailPageState.MoreClick -> {
                        state.dataVideo.post?.let { openMoreOptionBottomSheet(it) }
                    }

                    is ShortsDetailPageState.MentionUserClick -> {
                        state.dataVideo.post?.let {
                            shortsMentionUserClick(it, state.mentionUser)
                        }
                    }
                    is ShortsDetailPageState.HashtagClick -> {
                        val tagsList = state.dataVideo.post?.post_hashtags
                        var isUserNotFound = true

                        if (!tagsList.isNullOrEmpty()) {
                            tagsList.forEach { mInfo ->
                                if (mInfo.tagName == state.mentionUser) {
                                    startActivity(
                                        HashTagListActivity.getIntent(
                                            this@ShortDetailsActivity,
                                            mInfo.hashtagId,
                                            mInfo.tagName,
                                            if (binding.reelsConstraintLayout.isVisible) 1 else 0
                                        )
                                    )
                                }
                                isUserNotFound = false
                            }
                        } else {
                            isUserNotFound = true
                        }
                        if (isUserNotFound) {
                            showToast(getString(R.string.label_hashtag_not_found))
                        }
                    }

                    is ShortsDetailPageState.DownloadClick -> {
                        shortsDownloadClick(state.dataVideo)
                    }

                    is ShortsDetailPageState.GiftClick -> {
                        val bottomSheet = GiftGalleryBottomSheet.newInstance(
                            state.dataVideo.post?.user?.id.toString(), state.dataVideo.post?.id.toString(), true
                        )
                        bottomSheet.giftResponseState.subscribeAndObserveOnMainThread {
                            when (it) {
                                is GiftItemClickStates.SendGiftInChatClick -> {
                                    state.dataVideo.post?.let { post ->
                                        post.total_gift_count += 1
                                    }
                                    playShortsAdapter.notifyDataSetChanged()
                                }
                                else -> {
                                }
                            }
                        }.autoDispose()
                        bottomSheet.show(
                            supportFragmentManager, GiftGalleryBottomSheet::class.java.name
                        )
                    }
                }
            }
        }

        binding.reelsRecyclerView.apply {
            layoutManager = videoLayoutManager
            adapter = playShortsAdapter
        }

        videoLayoutManager.setOnViewPagerListener(object : OnViewPagerListener {
            override fun onInitComplete() {
                if (isVideoInitCompleted) {
                    return
                }
                isVideoInitCompleted = true
                autoPlayVideo()
            }

            override fun onPageRelease(isNext: Boolean, position: Int) {
                return
            }

            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
                if (mCurrentPosition == position) {
                    return
                }
                autoPlayVideo()
                mCurrentPosition = position
            }
        })
    }

    private fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.android.chrome")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Chrome is not installed, open with any available browser
            intent.setPackage(null)
            startActivity(intent)
        }
    }

    private fun shortsMentionUserClick(dataVideo: Post, mentionUser: String) {
        val tagsList = dataVideo.mention_users
        var isUserNotFound = true

        if (!tagsList.isNullOrEmpty()) {
            tagsList.forEach { mInfo ->
                if (mInfo.user.userName == mentionUser) {
                    startActivity(
                        MyProfileActivity.getIntentWithData(
                            this@ShortDetailsActivity,
                            mInfo.user.id

                        )

                    )
                }
                isUserNotFound = false
            }
        } else {
            isUserNotFound = true
        }
        if (isUserNotFound) {
            showToast(getString(R.string.label_user_not_found))
        }
    }

    private fun shortsDownloadClick(dataVideo: Post) {
        XXPermissions.with(this@ShortDetailsActivity)
            .permission(Permission.READ_MEDIA_VIDEO)
            .request(object : OnPermissionCallback {
                override fun onGranted(
                    permissions: List<String>,
                    all: Boolean
                ) {
                    if (all) {
                        showLoading(true)
                        FileUtils.saveMeetUrFriendsLogo(this@ShortDetailsActivity)
                        val serviceIntent = Intent(
                            this@ShortDetailsActivity,
                            DownloadService::class.java
                        )
                        serviceIntent.putExtra(
                            "videoUrl",
                            dataVideo.original_video_url
                        )
                        serviceIntent.putExtra("videoId", dataVideo.post?.id)
                        this@ShortDetailsActivity.startService(serviceIntent)
                    }
                }

                override fun onDenied(
                    permissions: List<String>,
                    never: Boolean
                ) {
                    showLoading(false)

                    if (never) {
                        UiUtils.showToast(
                            this@ShortDetailsActivity,
                            resources.getString(R.string.label_storage_permissions_denied)
                        )
                        XXPermissions.startPermissionActivity(
                            this@ShortDetailsActivity,
                            permissions
                        )
                    } else {
                        UiUtils.showToast(
                            this@ShortDetailsActivity,
                            "Failed to obtain storage permission "
                        )
                    }
                }
            })
    }

    private fun openMoreOptionBottomSheet(dataVideo: Post) {
        val isLoggedInUser = loggedInUserCache.getLoggedInUserId() == dataVideo.user.id
        val bottomSheet: LiveStreamingMoreOptionBottomSheet = LiveStreamingMoreOptionBottomSheet.newInstance(
            isFromShorts = true,
            isFromLive = false,
            isLoggedInUser
        )

        bottomSheet.optionClick.subscribeAndObserveOnMainThread {
            when (it) {
                resources.getString(R.string.report) -> {
                    val reportDialog = ReportDialogFragment.newInstance(dataVideo.id, false, SHORT)
                    reportDialog.show(supportFragmentManager, ReportDialogFragment::class.java.name)
                }

                resources.getString(R.string.label_copy_link) -> {
                    ShareHelper.shareDeepLink(this@ShortDetailsActivity, 1, dataVideo.id, "", true) { item ->
                        val clipboard: ClipboardManager = this@ShortDetailsActivity.getSystemService(
                            CLIPBOARD_SERVICE
                        ) as ClipboardManager
                        val clip = ClipData.newPlainText("Url", item)
                        clipboard.setPrimaryClip(clip)
                        showToast(resources.getString(R.string.msg_copied_video_link))
                    }
                }

                resources.getString(R.string.label_delete) -> {
                    deletePost(dataVideo.id, true)
                }
                resources.getString(R.string.label_edit) -> {
                    startActivity(
                        AddNewPostInfoActivity.launchActivity(
                            LaunchActivityData(this,
                            AddNewPostInfoActivity.POST_TYPE_VIDEO,
                            shortsInfo = dataVideo
                            )
                        )
                    )
                }
            }
        }.autoDispose()
        bottomSheet.show(
            supportFragmentManager,
            LiveStreamingMoreOptionBottomSheet::class.java.name
        )
    }

    private fun listenToViewModel() {
        shortDetailsViewModel.shortDetailsState.subscribeAndObserveOnMainThread {
            when (it) {
                is ShortDetailsViewState.ErrorMessage -> {
                    showLoading(false)
                    if (it.errorMessage == "No record found.") {
                        showToast("this post is deleted")
                        onBackPressed()
                    } else {
                        showToast(it.errorMessage)
                    }
                }
                is ShortDetailsViewState.LoadingState -> {
                    showLoading(it.isLoading)
                }
                is ShortDetailsViewState.SuccessMessage -> {
                }
                is ShortDetailsViewState.PostData -> {
                    videoData = it.postData
                    videoData?.let { videoData ->
                        if (videoData.is_shared == 1) {
                            setPostData(videoData)
                        } else {
                            if (videoData.post != null) {
                                if (videoData.post.type == "shorts") {
                                    binding.postLinearLayout.visibility = View.GONE
                                    binding.reelsConstraintLayout.visibility = View.VISIBLE
                                    listOfReelsInfo = listOf(it.postData)
                                    playShortsAdapter.listOfDataItems = listOfReelsInfo
                                    if (displayComment) {
                                        val bottomSheet = ShortsCommentBottomSheet.newInstance(
                                            it.postData.id.toString()
                                        )
                                        bottomSheet.show(
                                            supportFragmentManager,
                                            ShortsCommentBottomSheet::class.java.name
                                        )
                                    }
                                } else {
                                    setPostData(videoData)
                                }
                            } else {
                                binding.sliderImageSlider.visibility = View.GONE
                                setPostData(videoData)
                            }
                        }
                    }
                }
                is ShortDetailsViewState.PostShareResponse -> {
                    showToast(it.message)
                }
                is ShortDetailsViewState.PostDeleteSuccessFully -> {
                    showToast(it.message)
                    if (deleteShortId != -1 && isShortDelete) {
                        isShortDelete = false
                        RxBus.publish(RxEvent.UserProfileShortDelete(deleteShortId))
                        deleteShortId = -1
                    }
                    finish()
                }
                else -> {
                }
            }
        }.autoDispose()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setPostData(videoData: Post) {
        binding.postLinearLayout.visibility = View.VISIBLE
        binding.reelsConstraintLayout.visibility = View.GONE

        val userName =
            if (!videoData.user.userName.isNullOrEmpty() && videoData.user.userName != "null") {
                videoData.user.userName
            } else {
                videoData.user.firstName.plus(
                    " "
                ).plus(videoData.user.lastName)
            }
        binding.userNameAppCompatTextView.text = userName

        if (videoData.user.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        val name = if (!videoData.user.userName.isNullOrEmpty() && videoData.user.userName != "null") {
            videoData.user.userName
        } else {
            videoData.user.firstName.plus(" ").plus(videoData.user.lastName)
        }
        binding.newUserNameAppCompatTextView.text = getString(R.string.sign_at_the_rate).plus(videoData.user.userName)

        Glide.with(
            this@ShortDetailsActivity
        ).load(videoData.user.profile_photo).placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder).into(binding.profileRoundedImageView)

        binding.postTimeAppCompatTextView.text = videoData.created_at
        binding.postsTitleAppCompatTextView.originalText = videoData.content.toString()
        videoData.content?.let {
            binding.postsTitleAppCompatTextView.isVisible = true
            binding.postsTitleAppCompatTextView.originalText = it
        }
        binding.totalCountAppCompatTextView.text = if (videoData.shortViews == 0) {
            ""
        } else {
            videoData.shortViews.prettyCount().toString()
        }

        binding.totalLikesAppCompatTextView.text =
            if (videoData.post_likes_count == 0) "" else videoData.post_likes_count.prettyCount().toString()
        var postComment = videoData.post_comments.size
        videoData.post_comments.forEach {
            postComment += it.child_comments.size
        }
        binding.totalCommentsAppCompatTextView.text = if (postComment == 0) "" else postComment.prettyCount().toString()

        binding.totalRepostAppCompatTextView.text =
            if (videoData.repostCount == 0) "" else videoData.repostCount.prettyCount().toString()

        binding.totalGiftAppCompatTextView.text =
            if (videoData.total_gift_count == 0) "" else videoData.total_gift_count.prettyCount().toString()

        binding.likeAppCompatImageView.setImageResource(
            if (videoData.is_liked_count == 1) R.drawable.ic_fill_heart else R.drawable.ic_gray_heart
        )
        isLikes = videoData.is_liked_count == 1
        val imageList: ArrayList<SlideModel> = arrayListOf() // Create image list

        if (videoData.is_shared == 1) {
            binding.otherUserProfileRoundedImageView.visibility = View.VISIBLE
            binding.otherUserAppCompatTextView.visibility = View.VISIBLE
            binding.otherUserPostTimeAppCompatTextView.visibility = View.VISIBLE
            binding.viewSharedTop.visibility = View.VISIBLE
            binding.viewSharedLeft.visibility = View.VISIBLE
            binding.viewSharedRight.visibility = View.VISIBLE
            binding.viewSharedBottom.visibility = View.VISIBLE
            binding.viewcomments.visibility = View.GONE
            binding.sliderImageSlider.isVisible =
                !videoData.shared_post?.post_media.isNullOrEmpty()
            setMargins(binding.sliderImageSlider, 65, 65, 5)
            videoData.user.let {
                binding.postTitleAppCompatTextView.originalText =
                    if (videoData.content != "null" && videoData.content != null) videoData.content else ""
                binding.otherUserAppCompatTextView.text =
                    if (!it.userName.isNullOrEmpty() && it.userName != "null") {
                        it.userName
                    } else {
                        "${it.firstName} ${it.lastName}"
                    }
                binding.otherUserPostTimeAppCompatTextView.text = String.format(videoData.created_at)

                if (videoData.shared_post?.content != "null" && videoData.shared_post?.content != null) {
                    binding.postTitleAppCompatTextView.originalText = videoData.shared_post.content
                    binding.postTitleAppCompatTextView.isVisible = true
                }

                if (videoData.user.isVerified == 0) {
                    binding.ivAccountVerifiedOther.visibility = View.GONE
                } else {
                    binding.ivAccountVerifiedOther.visibility = View.VISIBLE
                }

                binding.otherUserAppCompatTextView.text =
                    if (!videoData.shared_post?.user?.userName.isNullOrEmpty() && videoData.shared_post?.user?.userName != null) {
                        videoData.shared_post.user.userName
                    } else {
                        "${videoData.shared_post?.user?.firstName} ${videoData.shared_post?.user?.lastName}"
                    }
                binding.otherUserPostTimeAppCompatTextView.text = "${videoData.shared_post?.created_at}"
                binding.newUsersNameAppCompatTextView.text =
                    getString(R.string.sign_at_the_rate)
                            .plus(videoData.shared_post?.user?.userName)

                if (!videoData.shared_post?.post_media.isNullOrEmpty()) {
                    binding.videoPlayer.isVisible = videoData.shared_post?.post_media?.first()?.extension == ".m3u8"
                    if (binding.videoPlayer.isVisible) {
                        playPostVideo(videoData.shared_post?.post_media?.first()?.file_path ?: "")
                    }
                } else {
                    binding.videoPlayer.isVisible = false
                }

                Glide.with(this@ShortDetailsActivity).load(videoData.shared_post?.user?.profile_photo)
                    .placeholder(
                        ResourcesCompat.getDrawable(resources, R.drawable.image_placeholder, null)
                    ).centerCrop()
                    .into(binding.otherUserProfileRoundedImageView)

                if (!videoData.shared_post?.post_media.isNullOrEmpty()) {
                    if (videoData.shared_post?.post_media?.first()?.extension == ".m3u8") {
                        imageList.add(SlideModel(videoData.shared_post.post_media.first().thumbnail))
                    } else {
                        videoData.shared_post?.post_media?.forEach { item ->
                            imageList.add(SlideModel(item.file_path))
                        }
                    }
                }

                setRepostUserName(videoData)

                if (!videoData.tagged_users_list.isNullOrEmpty()) {
                    val tagUserName =
                        if (!(videoData.tagged_users_list[0].user.userName.isNullOrEmpty()) && videoData.tagged_users_list[0].user.userName != "null") {
                            videoData.tagged_users_list[0].user.userName
                        } else {
                            videoData.tagged_users_list[0].user.firstName.plus(
                                " "
                            ).plus(videoData.tagged_users_list[0].user.lastName)
                        }
                    var text = SpannableStringBuilder()
                    if (videoData.tagged_users_list.isNotEmpty()) {
                        if (videoData.tagged_users_list.size == 1) {
                            text = SpannableStringBuilder().bold { append(name) }.append(" ").append("is with ").bold {
                                append(
                                    tagUserName
                                )
                            }

                            binding.userNameAppCompatTextView.text = text
                            if (videoData.location != null) {
                                text.append(" ").append("in").append(" ").bold { append(videoData.location) }
                            }
                        } else {
                            text = SpannableStringBuilder().bold { append(name) }.append(" ").append("is with ").bold {
                                append(
                                    tagUserName
                                )
                            }.append(" and ").bold {
                                append(
                                    videoData.tagged_users_list.size.minus(1).toString().plus(" other")
                                )
                            }

                            if (videoData.location != null) {
                                text.append(" ").append("in").append(" ").bold { append(videoData.location) }
                            }
                        }
                    }
                    binding.userNameAppCompatTextView.text = text
                } else {
                    val text = SpannableStringBuilder()
                    if (videoData.location != null) {
                        text.bold { append(name) }.append(" in ").bold { append(videoData.location) }
                    } else {
                        text.append(name)
                    }
                    binding.userNameAppCompatTextView.text = text
                }
            }

            binding.postTitleAppCompatTextView.setOnTouchListener { _, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    val mOffset = binding.postTitleAppCompatTextView.getOffsetForPosition(
                        motionEvent.x,
                        motionEvent.y
                    )
                    var clickedSharedWord = FileUtils.findWordForRightHanded(
                        binding.postTitleAppCompatTextView.text.toString(),
                        mOffset
                    )

                    if (clickedSharedWord.contains("@")) {
                        clickedSharedWord = clickedSharedWord.substringAfter("@")
                        var isSharedUserFound = true

                        if (!videoData.shared_post?.shared_mention_users.isNullOrEmpty()) {
                            videoData.shared_post?.shared_mention_users?.forEach { mInfo ->
                                if (clickedSharedWord == mInfo.user.userName) {
                                    startActivity(
                                        MyProfileActivity.getIntentWithData(
                                            this,
                                            mInfo.user.id
                                        )
                                    )
                                    isSharedUserFound = false
                                }
                                if (isSharedUserFound) {
                                    showToast(resources.getString(R.string.label_user_not_found))
                                }
                            }
                        } else if (!videoData.shared_post?.post_hashtags.isNullOrEmpty()) {
                            videoData.shared_post?.post_hashtags?.forEach { mInfo ->
                                if (clickedSharedWord == mInfo.tagName) {
                                    startActivity(
                                        HashTagListActivity.getIntent(
                                            this,
                                            mInfo.hashtagId,
                                            mInfo.tagName,
                                            if (binding.reelsConstraintLayout.isVisible) 1 else 0
                                        )
                                    )
                                    isSharedUserFound = false
                                }
                                if (isSharedUserFound) {
                                    showToast(resources.getString(R.string.label_hashtag_not_found))
                                }
                            }
                        } else {
                            if (clickedSharedWord.isNotEmpty()) {
                                showToast(resources.getString(R.string.label_user_not_found))
                            }
                        }
                    }
                }
                false
            }
        } else {
            binding.otherUserProfileRoundedImageView.visibility = View.GONE
            binding.otherUserAppCompatTextView.visibility = View.GONE
            binding.ivAccountVerifiedOther.visibility = View.GONE
            binding.otherUserPostTimeAppCompatTextView.visibility = View.GONE
            binding.postTitleAppCompatTextView.visibility = View.GONE
            binding.viewSharedTop.visibility = View.GONE
            binding.viewSharedLeft.visibility = View.GONE
            binding.viewSharedRight.visibility = View.GONE
            binding.viewSharedBottom.visibility = View.GONE
            setMargins(binding.sliderImageSlider, 0, 0, 0)
            binding.sliderImageSlider.isVisible = videoData.post_media.isNotEmpty()

            if (videoData.post_media.isNotEmpty()) {
                binding.videoPlayer.isVisible = videoData.post_media.first().extension == ".m3u8"
                if (binding.videoPlayer.isVisible) {
                    playPostVideo(videoData.post_media.first().file_path)
                }
            } else {
                binding.videoPlayer.isVisible = false
            }

            if (videoData.post_media.isNotEmpty()) {
                if (videoData.post_media.first().extension == ".m3u8") {
                    imageList.add(SlideModel(videoData.post_media.first().thumbnail))
                } else {
                    videoData.post_media.forEach {
                        imageList.add(SlideModel(it.file_path))
                    }
                }
            }

            if (!videoData.tagged_users_list.isNullOrEmpty()) {
                val tagUserName = videoData.tagged_users_list[0].user.userName
                var text = SpannableStringBuilder()
                if (videoData.tagged_users_list.isNotEmpty()) {
                    if (videoData.tagged_users_list.size == 1) {
                        text = SpannableStringBuilder().bold { append(name) }.append(" ").append("is with ").bold {
                            append(
                                tagUserName
                            )
                        }

                        binding.userNameAppCompatTextView.text = text

                        manageUserNameClickEvent(text)

                        if (videoData.location != null) {
                            text.append(" ").append("in").append(" ").bold { append(videoData.location) }
                        }
                    } else {
                        text = SpannableStringBuilder().bold { append(name) }.append(" ").append("is with ").bold {
                            append(
                                tagUserName
                            )
                        }.append(" and ").bold {
                            append(
                                videoData.tagged_users_list.size.minus(1).toString().plus(" other")
                            )
                        }

                        if (videoData.location != null) {
                            text.append(" ").append("in").append(" ").bold { append(videoData.location) }
                        }
                    }
                }
                binding.userNameAppCompatTextView.text = text
                manageUserNameClickEvent(text)
            } else {
                val text = SpannableStringBuilder()
                if (videoData.location != null) {
                    text.bold { append(name) }.append(" in ").bold { append(videoData.location) }
                } else {
                    text.append(name)
                }
                binding.userNameAppCompatTextView.text = text
                manageUserNameClickEvent(text)
            }
        }

        binding.sliderImageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP)

        binding.sliderImageSlider.setItemClickListener(object : ItemClickListener {

            override fun onItemSelected(position: Int) {
                if (binding.playVideoAppCompatImageView.isVisible) {
                    val intent = Intent(this@ShortDetailsActivity, VideoPlayActivity::class.java)
                    val url = if (videoData.is_shared == 1) {
                        videoData.shared_post?.post_media?.firstOrNull()?.file_path
                    } else {
                        videoData.post_media.firstOrNull()?.file_path
                    }
                    if (url != null) {
                        intent.putExtra("path", url)
                        startActivity(intent)
                    } else {
                        throw IllegalArgumentException("Post Media is Empty.")
                    }
                } else {
                    val intent = Intent(this@ShortDetailsActivity, FullScreenActivity::class.java)
                    val url = if (videoData.is_shared == 1) {
                        videoData.shared_post?.post_media?.firstOrNull()?.file_path
                    } else {
                        videoData.post_media.firstOrNull()?.file_path
                    }
                    if (url != null) {
                        intent.putExtra("path", url)
                        startActivity(intent)
                    } else {
                        throw IllegalArgumentException("Post Media is Empty.")
                    }
                }
            }
        })

        binding.postsTitleAppCompatTextView.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val mOffset = binding.postsTitleAppCompatTextView.getOffsetForPosition(
                    motionEvent.x,
                    motionEvent.y
                )
                var clickedWord = FileUtils.findWordForRightHanded(
                    binding.postsTitleAppCompatTextView.text.toString(),
                    mOffset
                )

                if (clickedWord.contains("@")) {
                    clickedWord = clickedWord.substringAfter("@")
                    var isUserFound = true

                    if (!videoData.mention_users.isNullOrEmpty()) {
                        videoData.mention_users.forEach { mInfo ->
                            if (clickedWord == mInfo.user.userName) {
                                startActivity(
                                    MyProfileActivity.getIntentWithData(
                                        this,
                                        mInfo.user.id
                                    )
                                )
                                isUserFound = false
                            }
                            if (isUserFound) {
                                showToast(resources.getString(R.string.label_user_not_found))
                            }
                        }
                    } else {
                        if (clickedWord.isNotEmpty()) {
                            showToast(resources.getString(R.string.label_user_not_found))
                        }
                    }
                } else if (clickedWord.contains("#")) {
                    clickedWord = clickedWord.substringAfter("#")
                    var isUserFound = true

                    if (!videoData.post_hashtags.isNullOrEmpty()) {
                        videoData.post_hashtags.forEach { mInfo ->

                            val finalTagName = if ('#' == mInfo.tagName?.first()) {
                                mInfo.tagName.removePrefix(
                                    "#"
                                )
                            } else {
                                mInfo.tagName
                            }

                            if (clickedWord == finalTagName) {
                                startActivity(
                                    HashTagListActivity.getIntent(
                                        this,
                                        mInfo.hashtagId,
                                        mInfo.tagName ?: "",
                                        if (binding.reelsConstraintLayout.isVisible) 1 else 0
                                    )
                                )
                                isUserFound = false
                            }
                            if (isUserFound) {
                                showToast(resources.getString(R.string.label_user_not_found))
                            }
                        }
                    } else {
                        if (clickedWord.isNotEmpty()) {
                            showToast(resources.getString(R.string.label_user_not_found))
                        }
                    }
                }
            }
            false
        }
    }

    private fun setMargins(view: View, left: Int, right: Int, bottom: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, 20, right, bottom)
            view.requestLayout()
        }
    }

    private fun setRepostUserName(videoData: Post) {
        binding.apply {
            val name =
                if (!videoData.shared_post?.user?.userName.isNullOrEmpty() && videoData.shared_post?.user?.userName != "null") {
                    videoData.shared_post?.user?.userName
                } else {
                    videoData.shared_post?.user?.firstName.plus(" ").plus(videoData.shared_post?.user?.lastName)
                }

            if (videoData.shared_post?.type.equals("profile_photo", true)) {
                if (videoData.shared_post?.user?.gender.equals("Male", true)) {
                    binding.newUserNameAppCompatTextView.text = name.plus(" ").plus(getString(R.string.label_updated_his_profile_photo))
                } else if (videoData.shared_post?.user?.gender.equals("Female", true)) {
                    binding.otherUserAppCompatTextView.text = name.plus(" ").plus(
                        getString(
                            R.string.label_updated_her_profile_photo
                        )
                    )
                } else {
                    binding.otherUserAppCompatTextView.text = name.plus(" ").plus(
                        getString(
                            R.string.label_updated_profile_photo
                        )
                    )
                }
            } else {
                videoData.shared_post?.let {
                    if (!videoData.shared_post.tagged_users_list.isNullOrEmpty()) {
                        val tagUserName =
                            if (!videoData.shared_post.tagged_users_list[0].user.userName.isNullOrEmpty() && videoData.shared_post.tagged_users_list[0].user.userName != "null") {
                                videoData.shared_post.tagged_users_list[0].user.userName
                            } else {
                                videoData.shared_post.tagged_users_list[0].user.firstName.plus(
                                    " "
                                ).plus(videoData.shared_post.tagged_users_list[0].user.lastName)
                            }
                        var text = SpannableStringBuilder()
                        if (videoData.shared_post.tagged_users_list.isNotEmpty()) {
                            if (videoData.shared_post.tagged_users_list.size == 1) {
                                text = SpannableStringBuilder().bold { append(name) }.append(" ").append("is with ").bold {
                                    append(
                                        tagUserName
                                    )
                                }

                                userNameAppCompatTextView.text = text
                                manageRepostUserNameClickEvent(text)
                                if (videoData.location != null) {
                                    text.append(
                                        " "
                                    ).append("in").append(" ").bold { append(videoData.shared_post.location) }
                                }
                            } else {
                                text = SpannableStringBuilder().bold { append(name) }.append(" ").append("is with ").bold {
                                    append(
                                        tagUserName
                                    )
                                }.append(" and ").bold {
                                    append(
                                        videoData.shared_post.tagged_users_list.size.minus(1).toString().plus(" other")
                                    )
                                }

                                if (videoData.shared_post.location != null) {
                                    text.append(
                                        " "
                                    ).append("in").append(" ").bold { append(videoData.shared_post.location) }
                                }
                            }
                        }
                        binding.otherUserAppCompatTextView.text = text
                        manageRepostUserNameClickEvent(text)
                    } else {
                        val text = SpannableStringBuilder()

                        if (videoData.shared_post.location != null) {
                            text.bold { append(name) }.append(" in ").bold { append(videoData.shared_post.location) }
                        } else {
                            text.append(name)
                        }
                        binding.otherUserAppCompatTextView.text = text
                        manageRepostUserNameClickEvent(text)
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun manageUserNameClickEvent(text: SpannableStringBuilder) {
        val inIndex = text.indexOf("in")
        val withIndex = text.indexOf("with")
        val andIndex = text.indexOf("and")
        binding.userNameAppCompatTextView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val layout: Layout? = (v as TextView).layout
                val x = event.x
                val y = event.y
                if (layout != null) {
                    val line: Int = layout.getLineForVertical(y.roundToInt())
                    val offset: Int = layout.getOffsetForHorizontal(line, x)

                    if (!videoData?.tagged_users_list.isNullOrEmpty()) {
                        if (inIndex == -1) {
                            if (andIndex != -1) {
                                if (offset in (withIndex + 1)..<andIndex) {
                                    startActivity(
                                        videoData?.tagged_users_list?.first()?.user?.id?.let {
                                            MyProfileActivity.getIntentWithData(
                                                this,
                                                it
                                            )
                                        }
                                    )
                                } else if (offset > andIndex) {
                                    startActivity(
                                        TaggedUserActivity.getIntent(
                                            this,
                                            videoData?.tagged_users_list as ArrayList
                                        )
                                    )
                                }
                            } else {
                                if (offset > withIndex) {
                                    startActivity(
                                        videoData?.tagged_users_list?.first()?.user?.id?.let {
                                            MyProfileActivity.getIntentWithData(
                                                this,
                                                it
                                            )
                                        }
                                    )
                                }
                            }
                        } else {
                            if (andIndex != -1) {
                                if (offset in (andIndex + 1)..<inIndex) {
                                    startActivity(
                                        TaggedUserActivity.getIntent(
                                            this,
                                            videoData?.tagged_users_list as ArrayList
                                        )
                                    )
                                } else if (offset in (withIndex + 1)..<andIndex) {
                                    startActivity(
                                        videoData?.tagged_users_list?.first()?.user?.id?.let {
                                            MyProfileActivity.getIntentWithData(
                                                this,
                                                it
                                            )
                                        }
                                    )
                                }
                            } else {
                                if (offset in (withIndex + 1)..<inIndex) {
                                    startActivity(
                                        videoData?.tagged_users_list?.first()?.user?.id?.let {
                                            MyProfileActivity.getIntentWithData(
                                                this,
                                                it
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun manageRepostUserNameClickEvent(text: SpannableStringBuilder) {
        val inIndex = text.indexOf("in")
        val withIndex = text.indexOf("with")
        val andIndex = text.indexOf("and")
        binding.otherUserAppCompatTextView.setOnTouchListener { v, event ->
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
                                startActivity(
                                    videoData?.shared_post?.tagged_users_list?.first()?.user?.id?.let {
                                        MyProfileActivity.getIntentWithData(
                                            this,
                                            it
                                        )
                                    }
                                )
                            } else if (offset > andIndex) {
                                startActivity(
                                    TaggedUserActivity.getIntent(
                                        this,
                                        videoData?.shared_post?.tagged_users_list as ArrayList
                                    )
                                )
                            }
                        } else {
                            if (offset > withIndex) {
                                startActivity(
                                    videoData?.shared_post?.tagged_users_list?.first()?.user?.id?.let {
                                        MyProfileActivity.getIntentWithData(
                                            this,
                                            it
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        if (andIndex != -1) {
                            if (offset in (andIndex + 1)..<inIndex) {
                                startActivity(
                                    TaggedUserActivity.getIntent(
                                        this,
                                        videoData?.shared_post?.tagged_users_list as ArrayList
                                    )
                                )
                            } else if (offset in (withIndex + 1)..<andIndex) {
                                startActivity(
                                    videoData?.shared_post?.tagged_users_list?.first()?.user?.id?.let {
                                        MyProfileActivity.getIntentWithData(
                                            this,
                                            it
                                        )
                                    }
                                )
                            }
                        } else {
                            if (offset in (withIndex + 1)..<inIndex) {
                                startActivity(
                                    videoData?.shared_post?.tagged_users_list?.first()?.user?.id?.let {
                                        MyProfileActivity.getIntentWithData(
                                            this,
                                            it
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            true
        }
    }

    private fun initUI() {
        binding.commentLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            Jzvd.goOnPlayOnPause()
            val bottomSheet = ShortsCommentBottomSheet.newInstance(postId)
            bottomSheet.show(supportFragmentManager, ShortsCommentBottomSheet::class.java.name)
        }.autoDispose()

        binding.shareLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            Jzvd.goOnPlayOnPause()
            ShareHelper.shareDeepLink(
                this@ShortDetailsActivity,
                0,
                postId.toInt(),
                "",
                true
            ) {
                ShareHelper.shareText(this@ShortDetailsActivity, it)
            }
        }.autoDispose()

        binding.profileRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (videoData?.shared_post?.user_id != null) {
                videoData?.shared_post?.user_id?.let {
                    startActivity(
                        MyProfileActivity.getIntentWithData(
                            this,
                            it
                        )
                    )
                }
            } else {
                throw IllegalArgumentException("Shorts User id not getting.")
            }
        }.autoDispose()
        binding.otherUserProfileRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (videoData?.shared_post?.user_id != null) {
                videoData?.shared_post?.user_id?.let {
                    startActivity(
                        MyProfileActivity.getIntentWithData(
                            this,
                            it
                        )
                    )
                }
            } else {
                throw IllegalArgumentException("Shorts User id not getting.")
            }
        }.autoDispose()

        binding.backAppCompatImageViewq.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
            RxBus.publish(RxEvent.UserProfileLikeUnlike(likeUnlikeId, postStatus))
        }.autoDispose()

        binding.likeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (!isLikes) {
                isLikes = true
                likePost(true)
                binding.likeAppCompatImageView.setImageResource(R.drawable.ic_fill_heart)
                var count = 0
                if (binding.totalLikesAppCompatTextView.text.toString() != "" && binding.totalLikesAppCompatTextView.text != null) {
                    count = binding.totalLikesAppCompatTextView.text.toString().toInt()
                }
                binding.totalLikesAppCompatTextView.text = count.plus(1).toString()
                binding.totalLikesAppCompatTextView.visibility = View.VISIBLE
            } else {
                isLikes = false
                likePost(false)
                var count = 0
                if (binding.totalLikesAppCompatTextView.text.toString() != "" && binding.totalLikesAppCompatTextView.text != null) {
                    count = binding.totalLikesAppCompatTextView.text.toString().toInt()
                }
                binding.likeAppCompatImageView.setImageResource(R.drawable.ic_gray_heart)
                count = if (count <= 0) 0 else (count - 1)
                binding.totalLikesAppCompatTextView.text = count.toString()
                binding.totalLikesAppCompatTextView.visibility = if (count <= 0) View.INVISIBLE else View.VISIBLE
            }
        }.autoDispose()

        binding.repostLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            openShareBottomSheet()
        }.autoDispose()

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
            RxBus.publish(RxEvent.UserProfileLikeUnlike(likeUnlikeId, postStatus))
        }.autoDispose()

        binding.optionsAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            openPostMoreOptionBottomSheet()
        }.autoDispose()

        binding.giftLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            if (videoData?.user?.id != loggedInUserCache.getLoggedInUserId()) {
                val bottomSheet = GiftGalleryBottomSheet.newInstance(
                    videoData?.user?.id.toString(),
                    videoData?.id.toString(),
                    true
                )
                bottomSheet.giftResponseState.subscribeAndObserveOnMainThread {
                    when (it) {
                        is GiftItemClickStates.SendGiftInChatClick -> {
                            binding.totalGiftAppCompatTextView.text = videoData?.total_gift_count?.plus(1).toString()
                        }
                        else -> {
                        }
                    }
                }.autoDispose()
                bottomSheet.show(
                    supportFragmentManager,
                    GiftGalleryBottomSheet::class.java.name
                )
            }
        }.autoDispose()

        binding.totalLikesAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            val postLikesDialog = PostLikesDialog.newInstance(videoData?.post_likes as ArrayList<PostLikesInformation>)

            postLikesDialog.show(supportFragmentManager, PostLikesDialog::class.java.name)
        }.autoDispose()
    }

    private fun openPostMoreOptionBottomSheet() {
        if (videoData?.user_id != null) {
            val bottomSheetPostMoreItem: BottomSheetPostMoreItem = BottomSheetPostMoreItem.newInstanceWithoutData(
                videoData?.user_id!!
            )
            bottomSheetPostMoreItem.moreOptionClicks.subscribeAndObserveOnMainThread {
                when (it) {
                    resources.getString(R.string.label_copy) -> {
                        ShareHelper.shareDeepLink(
                            this@ShortDetailsActivity,
                            0,
                            postId.toInt(),
                            "",
                            true
                        ) { item ->
                            val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip: ClipData = ClipData.newPlainText("Copied!", item)
                            clipboard.setPrimaryClip(clip)
                            showToast("Link copied!!")
                        }
                    }
                    resources.getString(R.string.label_share) -> {
                        ShareHelper.shareDeepLink(
                            this@ShortDetailsActivity,
                            0,
                            postId.toInt(),
                            "",
                            true
                        ) { item ->
                            ShareHelper.shareText(this@ShortDetailsActivity, item)
                        }
                    }
                    resources.getString(R.string.label_delete) -> {
                        // deletedItem = state.postInfo
                        deletePost(videoData?.id, false)
                    }

                    resources.getString(R.string.labek_repost) -> {
                        openShareBottomSheet()
                    }

                    resources.getString(R.string.desc_report) -> {
//                                    onPostReported(position)
                        val reportDialog = dataVideo?.let { it1 ->
                            ReportDialogFragment.newInstance(
                                it1.id,
                                false,
                                SHORT
                            )
                        }
                        reportDialog?.show(
                            supportFragmentManager,
                            ReportDialogFragment::class.java.name
                        )
                    }
                }
            }.autoDispose()
            bottomSheetPostMoreItem.show(supportFragmentManager, "TAGS")
        } else {
            throw IllegalArgumentException("User Id Null")
        }
    }

    private fun deletePost(postId: Int?, isShort: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage("Are you sure, you want to delete this ".plus(if (isShort) "short?" else "post?"))
        builder.setPositiveButton(
            Html.fromHtml(
                "<font color='#B22827'>Delete</font>"
            )
        ) { dialog, _ ->
            dialog.dismiss()
            if (postId != null) {
                isShortDelete = isShort
                deleteShortId = postId
                shortDetailsViewModel.deletePost(postId)
            }
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun openShareBottomSheet() {
        val bottomSheet = SharePostBottomSheet()
        bottomSheet.shareClicks.subscribeAndObserveOnMainThread {
            val postShareId =(if (videoData?.is_shared == 1) videoData?.shared_post?.id else videoData?.id)
            if (postShareId != null) {
                val postShareRequest  = PostShareRequest(postShareId,it.privacy.toString(),it.about.toString(),it.mentionUserId)
                shortDetailsViewModel.postShare(postShareRequest)
            } else {
                throw IllegalArgumentException("Post Id Null.")
            }
        }.autoDispose()
        bottomSheet.show(
            supportFragmentManager,
            SharePostBottomSheet::class.java.name
        )
    }

    private fun likePost(like: Boolean) {
        try {
            val postLikedDisliked = if (like) {
                "like"
            } else {
                "unlike"
            }
            likeUnlikeId = postId.toInt()
            postStatus = postLikedDisliked
            val postLikeUnlikeRequest = PostLikeUnlikeRequest(postId.toInt(),postLikedDisliked)
            shortDetailsViewModel.postLikeDisLike(postLikeUnlikeRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun autoPlayVideo() {
        if (binding.reelsRecyclerView.getChildAt(0) == null) {
            return
        }
        val player: JzvdStdOutgoer? = binding.reelsRecyclerView.getChildAt(0).findViewById(R.id.jzvdStdOutgoer)
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

        shortDetailsViewModel.updateShortsCount(ShortsCountRequest(postId.toInt()))
    }

    private fun likeShort(dataVideo: Post, like: Boolean) {
        try {
            val postLikedDisliked = if (like) {
                Constant.LIKE
            } else {
                Constant.UNLIKE
            }
            likeUnlikeId = dataVideo.id
            postStatus = postLikedDisliked
            val postLikeUnlikeRequest = PostLikeUnlikeRequest(dataVideo.id, postLikedDisliked)
            shortDetailsViewModel.shortsLikeDisLike(postLikeUnlikeRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        Jzvd.goOnPlayOnPause()
        super.onPause()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        RxBus.listen(RxEvent.CommentUpdate::class.java).subscribeAndObserveOnMainThread {
            if (it.data?.post_comments?.isNotEmpty() == true) {
                var postComment = it.data.post_comments.size
                it.data.post_comments.forEach { item ->
                    postComment += item.child_comments.size
                }
                dataVideo?.post_comments = it.data.post_comments
                playShortsAdapter.notifyDataSetChanged()
                Jzvd.goOnPlayOnResume()
            } else {
                throw IllegalArgumentException("Post Comment is Empty.")
            }
        }.autoDispose()

        RxBus.listen(RxEvent.PostCommentUpdate::class.java).subscribeAndObserveOnMainThread {
            if (it.data?.post_comments?.isNotEmpty() == true) {
                var postComment = it.data.post_comments.size
                it.data.post_comments.forEach { item ->
                    postComment += item.child_comments.size
                }
                binding.totalCommentsAppCompatTextView.text = postComment.toString()
            } else {
                throw IllegalArgumentException("Post Comment is Empty.")
            }
        }.autoDispose()

        Jzvd.goOnPlayOnResume()

        RxBus.listen(RxEvent.ShowProgress::class.java).subscribeAndObserveOnMainThread {
            showLoading(it.isShow)
        }.autoDispose()

        RxBus.listen(RxEvent.ShortsEditedSuccessfully::class.java).subscribeAndObserveOnMainThread {
            shortDetailsViewModel.viewPost(postId)
            autoPlayVideo()
        }.autoDispose()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        RxBus.publish(RxEvent.UserProfileLikeUnlike(likeUnlikeId, postStatus))
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }

    private fun playPostVideo(url: String) {
        val player: JzvdStdOutgoer = binding.videoPlayer
        Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
        binding.videoPlayer.posterImageView?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        binding.videoPlayer.posterImageView?.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT

        player.apply {
            this.videoUrl = url
            this.posterImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_NORMAL,
                JZMediaExoKotlin::class.java
            )
            Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
            startVideoAfterPreloading()
        }
    }
}
