package com.meetfriend.app.ui.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.model.HomePagePostInfoState
import com.meetfriend.app.api.post.model.PostInformation
import com.meetfriend.app.api.post.model.PostLikeUnlikeRequest
import com.meetfriend.app.api.post.model.PostLikesInformation
import com.meetfriend.app.api.post.model.PostShareRequest
import com.meetfriend.app.application.MeetFriend
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentMainHomeBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.ui.activities.FullScreenActivity
import com.meetfriend.app.ui.activities.VideoPlayActivity
import com.meetfriend.app.ui.camerakit.SnapkitActivity
import com.meetfriend.app.ui.giftsGallery.GiftGalleryBottomSheet
import com.meetfriend.app.ui.hashtag.HashTagListActivity
import com.meetfriend.app.ui.home.SharePostBottomSheet
import com.meetfriend.app.ui.home.shorts.ShortsCommentBottomSheet
import com.meetfriend.app.ui.home.shorts.report.ReportDialogFragment
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewModel
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewState
import com.meetfriend.app.ui.main.post.BottomSheetPostMoreItem
import com.meetfriend.app.ui.main.post.PostLikesDialog
import com.meetfriend.app.ui.main.story.AddStoryActivity
import com.meetfriend.app.ui.main.story.StoryInfoActivity
import com.meetfriend.app.ui.main.view.MainHomeAdapter
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.ui.storywork.models.HomePageStoryInfoState
import com.meetfriend.app.ui.storywork.models.ResultListResult
import com.meetfriend.app.ui.tag.TaggedUserActivity
import com.meetfriend.app.utilclasses.ads.AdsManager
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.CONTENT_ID
import com.meetfriend.app.utils.Constant.CONTENT_TYPE
import com.meetfriend.app.utils.Constant.LIKE
import com.meetfriend.app.utils.Constant.LIKE_CONTENT
import com.meetfriend.app.utils.Constant.POST
import com.meetfriend.app.utils.Constant.SCREEN_TIME
import com.meetfriend.app.utils.Constant.SCREEN_TYPE
import com.meetfriend.app.utils.Constant.SEND_GIFT
import com.meetfriend.app.utils.Constant.SHARE_CONTENT
import com.meetfriend.app.utils.Constant.UNLIKE
import com.meetfriend.app.utils.ShareHelper
import com.meetfriend.app.videoplayer.JZMediaExoKotlin
import com.meetfriend.app.videoplayer.JzvdStdOutgoer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainHomeFragment : BasicFragment() {

    private var _binding: FragmentMainHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainHomeAdapter: MainHomeAdapter
    private val viewedPostIds = HashSet<Int>()

    private lateinit var deletedItem: PostInformation
    var reportDialog: Dialog? = null

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MainHomeViewModel>
    private lateinit var mainHomeViewModel: MainHomeViewModel
    private var countForAds = 0
    private var autoplayFirst = true
    private var currentItemIsVideo = false

    private var mCurrentPosition = -1

    private val hasNextItem: Boolean
        get() = mCurrentPosition < (mainHomeAdapter.listOfPosts?.size ?: 0) - 1

    private val nextVideoUrls: List<String>?
        get() {
            val listOfItems = mainHomeAdapter.listOfPosts
            if (!listOfItems.isNullOrEmpty() && hasNextItem) {
                val listData: java.util.ArrayList<String> = arrayListOf()

                listOfItems.forEach {
                    if (!it.postMedia.isNullOrEmpty()) {
                        if (it.postMedia.first().extension == ".m3u8") {
                            it.postMedia.forEach { item ->
                                if (!item.filePath.isNullOrEmpty()) {
                                    listData.add(item.filePath)
                                }
                            }
                        }
                    }
                }

                listData.remove("")
                return listData
            }
            return null
        }

    private var onShareIntent = false

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1002

        @JvmStatic
        fun newInstance() = MainHomeFragment()
        const val TIMER = 1000L
        const val PAGE_NO = 10
        const val FIFTY = 50L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
        mainHomeViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMainHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        RxBus.publish(RxEvent.RefreshFoYouFragment)
        RxBus.publish(RxEvent.RefreshChallengeFragment)

        initUI()
        listenToViewModel()
    }
    private fun handleGetPostResponse(state: MainHomeViewState.GetPostResponse) {
        binding.mediaRecyclerView.setItemViewCacheSize(state.postList.size)
    }
    private fun listenToViewModel() {
        mainHomeAdapter.listOfStory = arrayListOf()
        mainHomeViewModel.mainHomeState.subscribeAndObserveOnMainThread {
            when (it) {
                is MainHomeViewState.ErrorMessage -> handleErrorMessage(it)
                is MainHomeViewState.GetPostResponse -> handlePostResponse(it)
                is MainHomeViewState.GetAllStoryInfo -> handleStoryInfo(it)
                is MainHomeViewState.PostDeleteSuccessFully -> handlePostDeleteSuccess(it)
                is MainHomeViewState.PostLikeUnlikeResponse -> handlePostLikeUnlike(it)
                is MainHomeViewState.PostReportOrHideMessage -> handleReportOrHideMessage(it)
                is MainHomeViewState.PostShareResponse -> handlePostShareResponse(it)
                is MainHomeViewState.SuccessMessage -> handleSuccessMessage(it)
                is MainHomeViewState.AddStoryResponse -> handleAddStoryResponse(it)
                is MainHomeViewState.StoryLoadingState -> handleStoryLoadingState(it)
                else -> {}
            }
        }.autoDispose()
    }

    private fun handleErrorMessage(state: MainHomeViewState.ErrorMessage) {
        showToast(state.errorMessage)
    }

    private fun handlePostResponse(state: MainHomeViewState.GetPostResponse) {
        binding.mediaRecyclerView.setItemViewCacheSize(state.postList.size)

        if (mainHomeAdapter.listOfPosts.isNullOrEmpty() && state.postList.isEmpty()) {
            binding.noPostsLinearLayout.isVisible = true
        }
        binding.mediaRecyclerView.recycledViewPool.clear()
        binding.mediaRecyclerView.setItemViewCacheSize(state.postList.size)
        mainHomeAdapter.listOfPosts = state.postList

        autoplayVideoIfRequired()

        postDelayedAction()
    }

    private fun autoplayVideoIfRequired() {
        if (!mainHomeAdapter.listOfPosts.isNullOrEmpty()) {
            mainHomeAdapter.listOfPosts?.elementAt(0)?.let { postInfo ->
                val data = if (postInfo.isShared == 1) postInfo.sharedPost?.postMedia else postInfo.postMedia
                if (!data.isNullOrEmpty() && data.firstOrNull()?.extension == ".m3u8" && autoplayFirst) {
                    Observable.timer(1000, TimeUnit.MILLISECONDS)
                        .subscribeAndObserveOnMainThread {
                            autoplayFirst = false
                            RxBus.publish(RxEvent.StartVideo(false))
                        }.autoDispose()
                }
            }
        }
    }

    private fun postDelayedAction() {
        Handler(Looper.getMainLooper()).postDelayed({
            val layoutManager = binding.mediaRecyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

            if (firstVisibleItemPosition > -1) {
                val itemType = mainHomeAdapter.getItemViewType(firstVisibleItemPosition)

                if (itemType != 1) {
                    autoPlayVideo()
                } else {
                    Jzvd.goOnPlayOnPause()
                }
            }
        }, 50)
    }

    private fun handleStoryInfo(state: MainHomeViewState.GetAllStoryInfo) {
        mainHomeAdapter.listOfStory = state.storyListInfo as ArrayList<ResultListResult>
    }

    private fun handlePostDeleteSuccess(state: MainHomeViewState.PostDeleteSuccessFully) {
        showToast(state.message)
        val listOfPostData = mainHomeAdapter.listOfPosts as ArrayList<PostInformation>
        listOfPostData.remove(deletedItem)
        mainHomeAdapter.listOfPosts = listOfPostData

        if (currentItemIsVideo && Jzvd.CURRENT_JZVD?.state == Jzvd.STATE_PAUSE) {
            Jzvd.goOnPlayOnResume()
        }
    }

    private fun handlePostLikeUnlike(state: MainHomeViewState.PostLikeUnlikeResponse) {
        val props = JSONObject().apply {
            put(CONTENT_TYPE, "post")
            put(CONTENT_ID, state.postId)
        }
        mp?.track(LIKE_CONTENT, props)
    }

    private fun handleReportOrHideMessage(state: MainHomeViewState.PostReportOrHideMessage) {
        showToast(state.message)
    }

    private fun handlePostShareResponse(state: MainHomeViewState.PostShareResponse) {
        val props = JSONObject().apply {
            put(Constant.POST_ID, state.postId)
        }
        mp?.track(Constant.REPOST_POST, props)

        if (loggedInUserCache.getLoginUserToken() != null) {
            mainHomeViewModel.resetPaginationForPost()
        }
    }

    private fun handleSuccessMessage(state: MainHomeViewState.SuccessMessage) {
        showToast(state.successMessage)
    }

    private fun handleAddStoryResponse(state: MainHomeViewState.AddStoryResponse) {
        showToast(state.message)
        if (loggedInUserCache.getLoginUserToken() != null) {
            mainHomeViewModel.pullToRefreshStory(10)
        }
    }

    private fun handleStoryLoadingState(state: MainHomeViewState.StoryLoadingState) {
        showLoading(state.isLoading)
    }

    override fun onResume() {
        super.onResume()
        RxBus.publish(RxEvent.RefreshFoYouFragment)
        RxBus.publish(RxEvent.RefreshChallengeFragment)
        RxBus.publish(RxEvent.MessageUnreadCount)

        if (isResumed) {
            autoplayFirst = true
            mp?.timeEvent(SCREEN_TIME)
            RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(isResumed))
        }
        RxBus.listen(RxEvent.StoryWatched::class.java).subscribeAndObserveOnMainThread {
            if (this.isVisible && countForAds == 0) {
                countForAds = 1
                AdsManager.getAdsManager()
                    .openInterstitialAds(
                        requireActivity(),
                        loggedInUserCache.getInterstitialAdId(),
                        object : AdsManager.InterstitialAdsCallback {
                            override fun InterstitialResponse(nativeAd: InterstitialAd?) {
                                if (nativeAd != null) {
                                    nativeAd.show(requireActivity())
                                } else {
                                    Timber.tag("Ads").i("The interstitial ad wasn't ready yet.")
                                }
                            }

                            override fun adsOnLoaded() {
                                return
                            }
                        }
                    )
            }
        }.autoDispose()
    }

    override fun onStop() {
        super.onStop()
        val props = JSONObject()
        props.put(SCREEN_TYPE, "post")

        mp?.track(SCREEN_TIME, props)
    }

    private fun deletePost(postInfo: PostInformation) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setMessage("Are you sure, you want to delete this post?")
        builder.setPositiveButton(
            Html.fromHtml("<font color='#B22827'>Delete</font>", Html.FROM_HTML_MODE_LEGACY)
        ) { dialog, _ ->
            dialog.dismiss()
            deletedItem = postInfo
            Jzvd.goOnPlayOnPause()
            mainHomeViewModel.deletePost(deletedItem.id)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initUI() {
        if (loggedInUserCache.getLoginUserToken() != null) mainHomeViewModel.resetPaginationForPost()

        if (loggedInUserCache.getLoginUserToken() != null) mainHomeViewModel.pullToRefreshStory(10)

        inItAdapter()

        mainHomeAdapter.userId = loggedInUserCache.getLoggedInUserId()

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            autoplayFirst = true
            RxBus.publish(RxEvent.RefreshFoYouFragment)
            RxBus.publish(RxEvent.RefreshChallengeFragment)
            binding.swipeRefreshLayout.isRefreshing = false
            if (loggedInUserCache.getLoginUserToken() != null) mainHomeViewModel.pullToRefreshStory(10)
            if (loggedInUserCache.getLoginUserToken() != null) mainHomeViewModel.resetPaginationForPost()
            RxBus.publish(RxEvent.MessageUnreadCount)
        }.autoDispose()

        RxBus.listen(RxEvent.RefreshHomePagePostPlayVideo::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.isVisible) {
                nextVideoUrls?.let { item ->
                    MeetFriend.exoCacheManager.prepareCacheVideos(item)
                }

                if (mCurrentPosition > -1) {
                    val itemType = mainHomeAdapter.getItemViewType(mCurrentPosition)
                    if (itemType == 1) {
                        Jzvd.goOnPlayOnPause()
                    } else {
                        if (onShareIntent) {
                            onShareIntent = false
                            if (isVisible) {
                                autoPlayVideo()
                            }
                        } else {
                            Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                                RxBus.publish(RxEvent.StartVideo(false))
                            }.autoDispose()
                        }
                    }
                }
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun inItAdapter() {
        mainHomeAdapter = MainHomeAdapter(requireContext()).apply {
            handleStoryClicks(this)
        }
        binding.mediaRecyclerView.apply {
            adapter = mainHomeAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        checkItemVisibility()
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                if (loggedInUserCache.getLoginUserToken() != null) mainHomeViewModel.loadMorePost()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun loadMoreStories() {
        loggedInUserCache.getLoginUserToken()?.let {
            mainHomeViewModel.loadMoreStory(10)
        }
    }

    private fun handleStoryResponse(state: HomePageStoryInfoState.StoryResponseData) {
        val list = mainHomeAdapter.listOfStory ?: arrayListOf()
        val index = list.indexOf(state.storyListResponse)


        if (index != -1) {
            list.forEach { it.isSelected = false }
            list[index].isSelected = !list[index].isSelected
            startActivity(StoryInfoActivity.getIntent(requireContext(), list))
        } else {
            mainHomeAdapter.listOfStory = arrayListOf()
        }
    }

    private fun handleStoryClicks(mainHomeAdapter: MainHomeAdapter) {
        mainHomeAdapter.apply {
            setupAddStoryClickHandler()
            setupStoryViewClickHandler()
            setupPostViewByUserHandler()
            setupPostViewClickHandler()
        }
    }

    private fun MainHomeAdapter.setupAddStoryClickHandler() {
        addStoryViewClicks.subscribeAndObserveOnMainThread {
            requestCameraPermissions {
                startActivity(AddStoryActivity.launchActivity(requireContext(), "photo"))
            }
        }.autoDispose()
    }

    private fun MainHomeAdapter.setupStoryViewClickHandler() {
        storyViewClicks.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is HomePageStoryInfoState.LoadMoreStories -> loadMoreStories()
                is HomePageStoryInfoState.StoryResponseData -> handleStoryResponse(state)
                is HomePageStoryInfoState.AddStoryResponseInfo -> handleAddStoryResponse()
                else -> {}
            }
        }.autoDispose()
    }

    private fun MainHomeAdapter.setupPostViewByUserHandler() {
        postViewByUser.observeOn(Schedulers.computation())
            .filter { postId ->
                val isProcessed = viewedPostIds.contains(postId)
                if (!isProcessed) {
                    viewedPostIds.add(postId)
                }
                !isProcessed
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { id ->
                mainHomeViewModel.postViewByUser(id.toString())
            }.autoDispose()
    }

    private fun MainHomeAdapter.setupPostViewClickHandler() {
        postViewClicks.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is HomePagePostInfoState.UserProfileClick -> handleUserProfileClick(state)
                is HomePagePostInfoState.OptionsClick -> handlePostOptionsClick(state)
                is HomePagePostInfoState.PostMediaClick -> handlePostMediaClick(state)
                is HomePagePostInfoState.OpenLinkAttachmentMultiple -> openUrlInBrowser(state.uri)
                is HomePagePostInfoState.PostLikeClick -> handlePostLikeClick(state)
                is HomePagePostInfoState.AddPostLikeClick -> handleAddPostLikeClick(state)
                is HomePagePostInfoState.RemovePostLikeClick -> handleRemovePostLikeClick(state)
                is HomePagePostInfoState.PostLikeCountClick -> showPostLikesDialog(state)
                is HomePagePostInfoState.CommentClick -> showPostsCommentBottomSheet(state)
                is HomePagePostInfoState.ShareClick -> sharePost(state)
                is HomePagePostInfoState.RepostClick -> repostPost(state)
                is HomePagePostInfoState.TagPeopleClick -> openTaggedPeopleActivity(state)
                is HomePagePostInfoState.TagPeopleFirstUserClick -> openFirstTaggedUserProfile(state)
                is HomePagePostInfoState.OtherUserProfileClick -> openOtherUserProfile(state)
                is HomePagePostInfoState.TagRepostPeopleClick -> openRepostTaggedPeopleActivity(state)
                is HomePagePostInfoState.TagRepostPeopleFirstUserClick -> openFirstRepostTaggedUserProfile(state)
                is HomePagePostInfoState.PostMediaVideoClick -> playPostMediaVideo(state)
                is HomePagePostInfoState.MentionUserNavigationClick -> navigateToMentionUserProfile(state)
                is HomePagePostInfoState.ShowUserNotFoundToast -> showUserNotFoundToast(state)
                is HomePagePostInfoState.MentionHashTagNavigationClick -> navigateToHashTagList(state)
                is HomePagePostInfoState.ShowHashtagNotFoundToast -> showHashtagNotFoundToast(state)
                is HomePagePostInfoState.GiftClick -> showGiftGalleryBottomSheet(state)
                else -> {}
            }
        }.autoDispose()
    }

    private fun navigateToMentionUserProfile(state: HomePagePostInfoState.MentionUserNavigationClick) {
        startActivity(MyProfileActivity.getIntentWithData(requireContext(), state.userId))
    }

    private fun showUserNotFoundToast(state: HomePagePostInfoState.ShowUserNotFoundToast) {
        if (state.isShow) {
            showToast("User not found")
        }
    }

    private fun navigateToHashTagList(state: HomePagePostInfoState.MentionHashTagNavigationClick) {
        startActivity(HashTagListActivity.getIntent(requireContext(), state.hashTagId, state.hashTagName))
    }

    private fun showHashtagNotFoundToast(state: HomePagePostInfoState.ShowHashtagNotFoundToast) {
        if (state.isShow) {
            showToast("Hashtag not found")
        }
    }

    private fun showGiftGalleryBottomSheet(state: HomePagePostInfoState.GiftClick) {
        val bottomSheet = GiftGalleryBottomSheet.newInstance(
            state.postInfo.user.id.toString(),
            state.postInfo.id.toString(),
            true,
            isFromWhere = GiftGalleryBottomSheet.INTENT_IS_FROM_POST
        )
        bottomSheet.giftResponseState.subscribeAndObserveOnMainThread {
            when (it) {
                is GiftItemClickStates.SendGiftInChatClick -> {
                    val props = JSONObject()
                    props.put(CONTENT_TYPE, "post")
                    props.put(CONTENT_ID, state.postInfo.id)
                    mp?.track(SEND_GIFT, props)
                    state.postInfo.totalGiftCount = state.postInfo.totalGiftCount?.plus(1)
                    mainHomeAdapter.notifyDataSetChanged()
                }
                else -> {}
            }
        }.autoDispose()
        bottomSheet.show(childFragmentManager, GiftGalleryBottomSheet::class.java.name)
    }

    private fun showPostLikesDialog(state: HomePagePostInfoState.PostLikeCountClick) {
        val postLikesDialog = PostLikesDialog.newInstance(state.postInfo.postLikes as ArrayList<PostLikesInformation>)
        postLikesDialog.show(childFragmentManager, PostLikesDialog::class.java.name)
    }

    private fun showPostsCommentBottomSheet(state: HomePagePostInfoState.CommentClick) {
        val bottomSheet = ShortsCommentBottomSheet.newInstance(state.postInfo.id.toString(), true)
        bottomSheet.show(childFragmentManager, ShortsCommentBottomSheet::class.java.name)
    }

    private fun sharePost(state: HomePagePostInfoState.ShareClick) {
        ShareHelper.shareDeepLink(requireContext(), 0, state.postInfo.id, "", true) {
            ShareHelper.shareText(requireContext(), it)
        }
    }

    private fun repostPost(state: HomePagePostInfoState.RepostClick) {
        val bottomSheet = SharePostBottomSheet()
        bottomSheet.shareClicks.subscribeAndObserveOnMainThread {
            val postShareId = if (state.postInfo.isShared == 1) state.postInfo.sharedPost?.id ?: 0 else state.postInfo.id
            val postShareRequest  = PostShareRequest(postShareId,it.privacy.toString(),it.about.toString(),it.mentionUserId)
            mainHomeViewModel.postShare(postShareRequest)

            val listOfPostData = mainHomeAdapter.listOfPosts as ArrayList<PostInformation>
            state.postInfo.content = it.about
            listOfPostData.add(0, state.postInfo)
            mainHomeAdapter.listOfPosts = listOfPostData
        }.autoDispose()
        bottomSheet.show(childFragmentManager, SharePostBottomSheet::class.java.name)
    }

    private fun openTaggedPeopleActivity(state: HomePagePostInfoState.TagPeopleClick) {
        startActivity(TaggedUserActivity.getIntent(requireContext(), state.postInfo.taggedUsersList as ArrayList))
    }

    private fun openFirstTaggedUserProfile(state: HomePagePostInfoState.TagPeopleFirstUserClick) {
        state.postInfo.taggedUsersList?.firstOrNull()?.user?.id?.let {
            startActivity(MyProfileActivity.getIntentWithData(requireContext(), it))
        }
    }

    private fun openOtherUserProfile(state: HomePagePostInfoState.OtherUserProfileClick) {
        if (currentItemIsVideo && Jzvd.CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.state == Jzvd.STATE_PAUSE) {
            Jzvd.goOnPlayOnResume()
        }
        startActivity(MyProfileActivity.getIntentWithData(requireContext(), state.postInfo.sharedPost?.userId ?: 0))
    }

    private fun openRepostTaggedPeopleActivity(state: HomePagePostInfoState.TagRepostPeopleClick) {
        startActivity(TaggedUserActivity.getIntent(requireContext(), state.postInfo?.taggedUsersList as ArrayList))
    }

    private fun openFirstRepostTaggedUserProfile(state: HomePagePostInfoState.TagRepostPeopleFirstUserClick) {
        state.postInfo?.taggedUsersList?.firstOrNull()?.user?.id?.let {
            startActivity(MyProfileActivity.getIntentWithData(requireContext(), it))
        }
    }

    private fun playPostMediaVideo(state: HomePagePostInfoState.PostMediaVideoClick) {
        val intent = Intent(requireActivity(), VideoPlayActivity::class.java)
        val url = if (state.postInfo.isShared == 1) {
            state.postInfo.sharedPost?.postMedia?.get(state.position)?.filePath
        } else {
            state.postInfo.postMedia?.get(state.position)?.filePath
        }
        intent.putExtra("path", url)
        requireActivity().startActivity(intent)
    }

    private fun handleUserProfileClick(state: HomePagePostInfoState.UserProfileClick) {
        if (currentItemIsVideo && Jzvd.CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.state == Jzvd.STATE_PAUSE) {
            Jzvd.goOnPlayOnResume()
        }
        startActivity(
            MyProfileActivity.getIntentWithData(requireContext(), state.postInfo.userId ?: 0)
        )
    }

    private fun handlePostMediaClick(state: HomePagePostInfoState.PostMediaClick) {
        val url = if (state.postInfo.isShared == 1) {
            state.postInfo.sharedPost?.postMedia?.get(state.position)?.filePath
        } else {
            state.postInfo.postMedia?.get(state.position)?.filePath
        }
        val intent = Intent(requireActivity(), FullScreenActivity::class.java).apply {
            putExtra("url", url)
        }
        requireActivity().startActivity(intent)
    }

    private fun handlePostLikeClick(state: HomePagePostInfoState.PostLikeClick) {
        val postLikedDisliked: String = if (state.postInfo.isLikedCount == 0) {
            UNLIKE
        } else {
            LIKE
        }
        state.postInfo.isLikedCount = if (postLikedDisliked == UNLIKE) 0 else 1
        state.postInfo.postLikesCount = state.postInfo.postLikesCount?.let {
            if (postLikedDisliked == UNLIKE) it - 1 else it + 1
        }
        val postLikeUnlikeRequest = PostLikeUnlikeRequest(state.postInfo.id,postLikedDisliked)
        mainHomeViewModel.postLikeDisLike(postLikeUnlikeRequest)
    }

    private fun handleAddPostLikeClick(state: HomePagePostInfoState.AddPostLikeClick) {
        val postLikeUnlikeRequest = PostLikeUnlikeRequest(state.postInfo.id,LIKE)
        mainHomeViewModel.postLikeDisLike(postLikeUnlikeRequest)
    }

    private fun handleRemovePostLikeClick(state: HomePagePostInfoState.RemovePostLikeClick) {
        val postLikeUnlikeRequest = PostLikeUnlikeRequest(state.postInfo.id,LIKE)
        mainHomeViewModel.postLikeDisLike(postLikeUnlikeRequest)
    }

    private fun handlePostOptionsClick(state: HomePagePostInfoState.OptionsClick) {
        val bottomSheetPostMoreItem = BottomSheetPostMoreItem.newInstanceWithoutData(state.postInfo.userId ?: 0)
        bottomSheetPostMoreItem.moreOptionClicks.subscribeAndObserveOnMainThread {
            when (it) {
                resources.getString(R.string.label_copy) -> copyPostLink(state)
                resources.getString(R.string.label_share) -> sharePost(state)
                resources.getString(R.string.label_delete) -> deletePost(state.postInfo)
                resources.getString(R.string.labek_repost) -> repostPost(state)
                resources.getString(R.string.desc_report) -> reportPost(state)
            }
        }.autoDispose()
        bottomSheetPostMoreItem.show(childFragmentManager, "TAGS")
    }

    private fun copyPostLink(state: HomePagePostInfoState.OptionsClick) {
        ShareHelper.shareDeepLink(requireContext(), 0, state.postInfo.id, "", true) { item ->
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied!", item)
            clipboard.setPrimaryClip(clip)
            showToast("Link copied!!")
        }
    }

    private fun sharePost(state: HomePagePostInfoState.OptionsClick) {
        onShareIntent = true
        ShareHelper.shareDeepLink(requireContext(), 0, state.postInfo.id, "", true) { item ->
            ShareHelper.shareText(requireContext(), item)
            val props = JSONObject().apply {
                put(CONTENT_TYPE, "post")
                put(CONTENT_ID, state.postInfo.id)
            }
            mp?.track(SHARE_CONTENT, props)
        }
    }

    private fun repostPost(state: HomePagePostInfoState.OptionsClick) {
        val bottomSheet = SharePostBottomSheet()
        bottomSheet.shareClicks.subscribeAndObserveOnMainThread { item ->

            val postShareId = if (state.postInfo.isShared == 1) state.postInfo.sharedPost?.id ?: 0 else state.postInfo.id
            val postShareRequest  = PostShareRequest(postShareId,item.privacy.toString(),item.about.toString(),item.mentionUserId)
            mainHomeViewModel.postShare(postShareRequest)
        }.autoDispose()
        bottomSheet.show(childFragmentManager, SharePostBottomSheet::class.java.name)
    }

    private fun reportPost(state: HomePagePostInfoState.OptionsClick) {
        val reportDialog = ReportDialogFragment.newInstance(state.postInfo.id, false, POST)
        reportDialog.optionClick.subscribeAndObserveOnMainThread {
            val props = JSONObject().apply {
                put(CONTENT_TYPE, "post")
                put(CONTENT_ID, state.postInfo.id)
            }
            mp?.track(Constant.REPORT_CONTENT, props)
        }
        reportDialog.show(childFragmentManager, ReportDialogFragment::class.java.name)
    }

    private fun handleAddStoryResponse() {
        // Permission handling logic for API levels
        val cameraPermissions = getCameraPermissions()
        val missingPermissions = cameraPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), cameraPermissions, CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            startActivity(SnapkitActivity.getIntent(requireContext(), isStory = true))
        }
    }


    private fun getCameraPermissions() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    } else {
        arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    }

    private fun requestCameraPermissions(onGranted: () -> Unit) {
        XXPermissions.with(requireContext())
            .permission(Permission.CAMERA)
            .permission(Permission.RECORD_AUDIO)
            .permission(Permission.READ_MEDIA_IMAGES)
            .permission(Permission.READ_MEDIA_VIDEO)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) onGranted()
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    handlePermissionDenied(permissions, never)
                }
            })
    }

    private fun handlePermissionDenied(permissions: MutableList<String>, never: Boolean) {
        if (never) {
            showToast("Authorization is permanently denied, please manually grant permissions")
            XXPermissions.startPermissionActivity(requireContext(), permissions)
        } else {
            showToast(getString(R.string.msg_permission_denied))
        }
    }

    private fun autoPlayVideo() {
        if (binding.mediaRecyclerView.getChildAt(0) == null) {
            return
        }

        if (binding.mediaRecyclerView.getChildAt(0) != null && binding.mediaRecyclerView.getChildAt(0)
                .findViewById<ViewPager2>(R.id.sliderImageSlider) != null &&
            binding.mediaRecyclerView.getChildAt(0).findViewById<ViewPager2>(R.id.sliderImageSlider)
                .getChildAt(0).findViewById<JzvdStdOutgoer>(R.id.outgoerVideoPlayer) != null
        ) {
            val player: JzvdStdOutgoer? = binding.mediaRecyclerView.getChildAt(
                0
            ).findViewById<ViewPager2>(R.id.sliderImageSlider).getChildAt(0)
                .findViewById(R.id.outgoerVideoPlayer)
            player?.apply {
                player.posterImageView.scaleType = ImageView.ScaleType.CENTER_CROP
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

    private fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.android.chrome")
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            // Chrome is not installed, open with any available browser
            intent.setPackage(null)
            startActivity(intent)
        }
    }

    private fun checkItemVisibility() {
        val layoutManager = binding.mediaRecyclerView.layoutManager as LinearLayoutManager
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        if (mCurrentPosition != firstVisibleItemPosition) {
            mCurrentPosition = firstVisibleItemPosition

            nextVideoUrls?.let {
                Timber.i("nextVideoUrl $nextVideoUrls")
                MeetFriend.exoCacheManager.prepareCacheVideos(it)
            }

            val itemType = mainHomeAdapter.getItemViewType(firstVisibleItemPosition)
            if (itemType != 1) {
                autoPlayVideo()
                currentItemIsVideo = true
            } else {
                currentItemIsVideo = false
                Jzvd.goOnPlayOnPause()
            }
        } else {
            val itemType = mainHomeAdapter.getItemViewType(firstVisibleItemPosition)
            if (itemType != 1) {
                currentItemIsVideo = true
                autoPlayVideo()
            } else {
                currentItemIsVideo = false
                Jzvd.goOnPlayOnPause()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(false))
        Jzvd.goOnPlayOnPause()
    }
}
