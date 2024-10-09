package com.meetfriend.app.ui.home.shorts

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.OrientationHelper
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
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.api.post.model.PostLikeUnlikeRequest
import com.meetfriend.app.api.post.model.ShortsCountRequest
import com.meetfriend.app.api.post.model.ShortsPageState
import com.meetfriend.app.application.MeetFriend
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentForYouShortsBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.video.DataVideo
import com.meetfriend.app.responseclasses.video.Post
import com.meetfriend.app.ui.activities.WelcomeActivity
import com.meetfriend.app.ui.camerakit.SnapkitActivity
import com.meetfriend.app.ui.giftsGallery.GiftGalleryBottomSheet
import com.meetfriend.app.ui.giftsGallery.GiftGalleryBottomSheet.Companion.INTENT_IS_FROM_SHORTS
import com.meetfriend.app.ui.hashtag.HashTagListActivity
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.shorts.report.ReportDialogFragment
import com.meetfriend.app.ui.home.shorts.view.PlayShortsAdapter
import com.meetfriend.app.ui.home.shorts.viewmodel.ShortsViewModel
import com.meetfriend.app.ui.home.shorts.viewmodel.ShortsViewState
import com.meetfriend.app.ui.livestreaming.LiveStreamingMoreOptionBottomSheet
import com.meetfriend.app.ui.livestreaming.videoroom.LiveRoomActivity
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utilclasses.ads.AdsManager
import com.meetfriend.app.utilclasses.download.DownloadService
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.DOWNLOAD_SHORTS
import com.meetfriend.app.utils.Constant.FiXED_100_MILLISECOND
import com.meetfriend.app.utils.Constant.FiXED_2000_MILLISECOND
import com.meetfriend.app.utils.Constant.FiXED_200_MILLISECOND
import com.meetfriend.app.utils.Constant.FiXED_3_INT
import com.meetfriend.app.utils.Constant.SHORT
import com.meetfriend.app.utils.Constant.SHORTS_ID
import com.meetfriend.app.utils.FileUtils.saveMeetUrFriendsLogo
import com.meetfriend.app.utils.ShareHelper
import com.meetfriend.app.utils.UiUtils
import com.meetfriend.app.videoplayer.JZMediaExoKotlin
import com.meetfriend.app.videoplayer.JzvdStdOutgoer
import com.meetfriend.app.videoplayer.OnViewPagerListener
import com.meetfriend.app.videoplayer.ViewPagerLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

class ForYouShortsFragment : BasicFragment() {
    companion object {
        const val TAG = "ForYouShortsFragment"

        @JvmStatic
        fun newInstance() = ForYouShortsFragment()

        var HASHTAG_ID = "HASHTAG_ID"
        var HASHTAG_NAME = "HASHTAG_NAME"
        var FROM_WHERE = "FROM_WHERE"
        var INTENT_SEARCH = "INTENT_SEARCH"
        var PRE_FETCH_ITEM_COUNT = 10

        @JvmStatic
        fun getInstanceForHashTag(
            hashTagId: Int,
            hashTagName: String,
            fromWhere: Int,
            search: String? = null
        ): ForYouShortsFragment {
            val forYouShortsFragment = ForYouShortsFragment()
            val bundle = Bundle()
            bundle.putInt(HASHTAG_ID, hashTagId)
            bundle.putString(HASHTAG_NAME, hashTagName)
            bundle.putString(INTENT_SEARCH, search)
            bundle.putInt(FROM_WHERE, fromWhere)
            forYouShortsFragment.arguments = bundle
            return forYouShortsFragment
        }
    }

    private var _binding: FragmentForYouShortsBinding? = null
    private val binding get() = _binding!!

    private lateinit var playShortsAdapter: PlayShortsAdapter
    private lateinit var videoLayoutManager: ViewPagerLayoutManager

    private var isVideoInitCompleted = false
    private var mCurrentPosition = 0
    private var listOfShortsInfo: ArrayList<DataVideo> = arrayListOf()
    private var listOfPositionByAds: ArrayList<Int> = arrayListOf()
    private var dataVideo: DataVideo? = null
    private var loggedInUserId by Delegates.notNull<Int>()
    private var isFVisible: Boolean = false
    private var deleteIndex: Int? = -1

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ShortsViewModel>
    private lateinit var shortsViewModel: ShortsViewModel
    private var isDelete = false
    private val viewedShortsIds = HashSet<Int>()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    var reportDialog: Dialog? = null

    var hashTagId: Int = 0
    var hashTagName: String? = null
    var fromWhere: Int? = null

    private val hasNextItem: Boolean
        get() = mCurrentPosition < (playShortsAdapter.listOfDataItems?.size ?: 0) - 1

    private val nextVideoUrls: List<String>?
        get() {
            val currentPosition = mCurrentPosition
            val listOfItems = playShortsAdapter.listOfDataItems
            if (!listOfItems.isNullOrEmpty() && hasNextItem) {
                val endIndex = minOf(currentPosition + FiXED_3_INT, listOfItems.size)
                return listOfItems.subList(currentPosition + 1, endIndex).map { it.file_path }
            }
            return null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForYouShortsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        shortsViewModel = getViewModelFromFactory(viewModelFactory)
        listenToArgumentsData()

        RxBus.listen(RxEvent.RefreshFoYouFragment::class.java).subscribeAndObserveOnMainThread {
            Jzvd.goOnPlayOnPause()
        }.autoDispose()

        RxBus.listen(RxEvent.PlayVideo::class.java).subscribeAndObserveOnMainThread {
            isFVisible = it.isPlay

            if (isResumed) {
                if (!isFVisible) {
                    Jzvd.goOnPlayOnPause()
                } else {
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            autoPlayVideo()
                        },
                        FiXED_2000_MILLISECOND
                    )
                }
            }
        }.autoDispose()

        RxBus.listen(RxEvent.RefreshFoYouPlayFragment::class.java).subscribeAndObserveOnMainThread {
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    autoPlayVideo()
                },
                FiXED_200_MILLISECOND
            )
        }.autoDispose()

        RxBus.listen(RxEvent.RefreshForYouPlayFragment::class.java).subscribeAndObserveOnMainThread {
            Timber.tag("MeetUrFriends1").e("Listen for RefreshForYouPlayFragment")
            Handler(Looper.getMainLooper()).postDelayed({
                autoPlayVideo()
            }, FiXED_2000_MILLISECOND)
        }.autoDispose()

        RxBus.listen(RxEvent.PauseForYouShorts::class.java).subscribeAndObserveOnMainThread {
            Timber.tag("MeetUrFriends1").e("Listen for RefreshForYouPlayFragment")
            Jzvd.goOnPlayOnPause()
        }.autoDispose()
        RxBus.listen(RxEvent.PlayForYouShorts::class.java).subscribeAndObserveOnMainThread {
            Timber.tag("MeetUrFriends1").e("Listen for RefreshForYouPlayFragment")
            Jzvd.goOnPlayOnResume()
        }.autoDispose()

        loggedInUserId = loggedInUserCache.getLoggedInUserId()
        listenToViewModel()
        listenToViewEvents()
    }

    private fun listenToArgumentsData() {
        if (arguments?.containsKey(HASHTAG_ID) == true) {
            extractArguments()

            if (fromWhere == 0) {
                showHashtagButton()
            } else {
                hideHashtagButton()
                subscribeToSearchTextShorts()
            }
        } else {
            hideHashtagButton()
        }
    }

    private fun extractArguments() {
        arguments?.let {
            hashTagId = requireArguments().getInt(HASHTAG_ID)
            hashTagName = requireArguments().getString(HASHTAG_NAME)
            fromWhere = requireArguments().getInt(FROM_WHERE)
        }
    }

    private fun showHashtagButton() {
        binding.useThisHashtagAppCompatTextView.visibility = View.VISIBLE
        binding.useThisHashtagAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            requestPermissions()
        }
    }

    private fun hideHashtagButton() {
        binding.useThisHashtagAppCompatTextView.visibility = View.GONE
    }

    private fun requestPermissions() {
        XXPermissions.with(requireContext())
            .permission(Permission.CAMERA)
            .permission(Permission.RECORD_AUDIO)
            .permission(Permission.READ_MEDIA_IMAGES)
            .permission(Permission.READ_MEDIA_VIDEO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        startSnapkitActivity()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    handlePermissionDenied(permissions, never)
                }
            })
    }

    private fun startSnapkitActivity() {
        startActivity(SnapkitActivity.getIntent(requireContext(), true, tagName = hashTagName))
    }

    private fun handlePermissionDenied(permissions: MutableList<String>, never: Boolean) {
        if (never) {
            showToast("Authorization is permanently denied, please manually grant permissions")
            XXPermissions.startPermissionActivity(requireContext(), permissions)
        } else {
            showToast(getString(R.string.msg_permission_denied))
        }
    }

    private fun subscribeToSearchTextShorts() {
        RxBus.listen(RxEvent.SearchTextShorts::class.java).subscribeAndObserveOnMainThread {
            shortsViewModel.resetPaginationForHashTagPost(hashTagId, it.search.toString())
        }.autoDispose()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listenToViewModel() {
        shortsViewModel.shortsState.subscribeAndObserveOnMainThread {
            when (it) {
                is ShortsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is ShortsViewState.ShortsLikeUnlikeResponse -> {
                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "shorts")
                    props.put(Constant.CONTENT_ID, it.postId)

                    mp?.track(Constant.LIKE_CONTENT, props)
                }

                is ShortsViewState.ShortsResponse -> {
                    setShortsInfo(it.shortsList)
                }

                is ShortsViewState.SuccessMessage -> {
                    if (deleteIndex != -1) {
                        val list = playShortsAdapter.listOfDataItems as ArrayList
                        list.removeAt(deleteIndex ?: 0)
                        playShortsAdapter.listOfDataItems = list
                        playShortsAdapter.notifyDataSetChanged()
                    }
                    showToast(it.successMessage)
                    deleteIndex = -1
                }

                else -> {
                }
            }
        }.autoDispose()
    }

    private fun openLoginPopup() {
        val loginBottomSheet = WelcomeActivity.newInstance()
        loginBottomSheet.show(childFragmentManager, WelcomeActivity::class.java.name)
    }

//    @SuppressLint("NotifyDataSetChanged")
//    private fun listenToViewEvents() {
//        videoLayoutManager = ViewPagerLayoutManager(requireContext(), OrientationHelper.VERTICAL)
//        videoLayoutManager.isItemPrefetchEnabled = true
//        videoLayoutManager.isMeasurementCacheEnabled = true
//        videoLayoutManager.initialPrefetchItemCount = PRE_FETCH_ITEM_COUNT
//        videoLayoutManager.isSmoothScrollbarEnabled = true
//        playShortsAdapter = PlayShortsAdapter(requireContext(), requireActivity()).apply {
//            shortsViewByUser.observeOn(Schedulers.computation()).filter { postId ->
//                val isProcessed = viewedShortsIds.contains(postId)
//                if (!isProcessed) {
//                    viewedShortsIds.add(postId)
//                }
//                !isProcessed
//            }.observeOn(AndroidSchedulers.mainThread()).subscribe { id ->
//                if (loggedInUserCache.getLoginUserToken() != null) {
//                    shortsViewModel.updateShortsCount(ShortsCountRequest(id))
//                }
//            }.autoDispose()
//
//            playShortsViewClicks.subscribeAndObserveOnMainThread { state ->
//                when (state) {
//                    is ShortsPageState.UserProfileClick -> {
//                        if (state.dataVideo.liveId != 0) {
//                            startActivity(
//                                LiveRoomActivity.getIntent(
//                                    requireContext(), null, state.dataVideo.liveId
//                                )
//                            )
//                        } else {
//                            startActivity(
//                                MyProfileActivity.getIntentWithData(
//                                    requireContext(), state.dataVideo.user_id
//                                )
//                            )
//                        }
//                    }
//
//                    is ShortsPageState.AddReelLikeClick -> {
//                        likePost(state.dataVideo, true)
//                    }
//
//                    is ShortsPageState.RemoveReelLikeClick -> {
//                        likePost(state.dataVideo, false)
//                    }
//
//                    is ShortsPageState.OpenLinkAttachment -> {
//                        state.dataVideo.post.web_link?.let { openUrlInBrowser(it) }
//                    }
//
//                    is ShortsPageState.CommentClick -> {
//                        Jzvd.goOnPlayOnPause()
//                        dataVideo = state.dataVideo
//                        val bottomSheet = ShortsCommentBottomSheet.newInstance(state.dataVideo.posts_id.toString())
//                        bottomSheet.show(childFragmentManager, ShortsCommentBottomSheet::class.java.name)
//                    }
//
//                    is ShortsPageState.ShareClick -> {
//                        ShareHelper.shareDeepLink(
//                            requireContext(), 1, state.dataVideo.posts_id, "", true
//                        ) {
//                            ShareHelper.shareText(requireContext(), it)
//                            val props = JSONObject()
//                            props.put(Constant.CONTENT_TYPE, "shorts")
//                            props.put(Constant.CONTENT_ID, dataVideo?.id)
//
//                            mp?.track(Constant.SHARE_CONTENT, props)
//                        }
//                    }
//
//                    is ShortsPageState.DownloadClick -> {
//                        XXPermissions.with(requireContext())
//                            .permission(Permission.READ_MEDIA_VIDEO)
//                            .request(object : OnPermissionCallback {
//                                override fun onGranted(
//                                    permissions: List<String>,
//                                    all: Boolean
//                                ) {
//                                    if (all) {
//                                        showProgress(true)
//                                        saveMeetUrFriendsLogo(requireContext())
//                                        val serviceIntent = Intent(
//                                            requireActivity(), DownloadService::class.java
//                                        )
//                                        serviceIntent.putExtra(
//                                            "videoUrl", state.dataVideo.originalVideoUrl
//                                        )
//                                        serviceIntent.putExtra("videoId", state.dataVideo.id)
//                                        requireActivity().startService(serviceIntent)
//
//                                        val props = JSONObject()
//                                        props.put(SHORTS_ID, state.dataVideo.id)
//
//                                        mp?.track(DOWNLOAD_SHORTS, props)
//                                    }
//                                }
//
//                                override fun onDenied(
//                                    permissions: List<String>,
//                                    never: Boolean
//                                ) {
//                                    showProgress(false)
//                                    if (never) {
//                                        UiUtils.showToast(
//                                            requireContext(),
//                                            resources.getString(R.string.label_storage_permissions_denied)
//                                        )
//                                        XXPermissions.startPermissionActivity(
//                                            requireActivity(), permissions
//                                        )
//                                    } else {
//                                        UiUtils.showToast(
//                                            requireContext(), " Failed to obtain storage permission "
//                                        )
//                                    }
//                                }
//                            })
//                    }
//
//                    is ShortsPageState.FollowClick -> {
//                        shortsViewModel.followUnfollow(state.dataVideo.post.user.id)
//                        val list = playShortsAdapter.listOfDataItems
//                        list?.filter { it.user_id == state.dataVideo.post.user.id }?.forEach {
//                            it.followBack = 1
//                        }
//                        playShortsAdapter.listOfDataItems = list
//                        notifyDataSetChanged()
//                    }
//
//                    is ShortsPageState.MoreClick -> {
//                        openMoreOptionBottomSheet(state.dataVideo)
//                    }
//
//                    is ShortsPageState.MentionUserClick -> {
//                        val tagsList = state.dataVideo.post.mention_users
//                        var isUserNotFound = true
//
//                        if (!tagsList.isNullOrEmpty()) {
//                            tagsList.forEach { mInfo ->
//                                if (mInfo.user.userName == state.mentionUser) {
//                                    startActivity(
//                                        MyProfileActivity.getIntentWithData(
//                                            requireContext(), mInfo.user.id
//                                        )
//                                    )
//                                }
//                                isUserNotFound = false
//                            }
//                        } else {
//                            isUserNotFound = true
//                        }
//                        if (isUserNotFound) {
//                            showToast(getString(R.string.label_user_not_found))
//                        }
//                    }
//
//                    is ShortsPageState.HashtagClick -> {
//                        val tagsList = state.dataVideo.postHashtags
//                        var isUserNotFound = true
//
//                        if (!tagsList.isNullOrEmpty()) {
//                            tagsList.forEach { mInfo ->
//                                val finalTagName = if ('#' == mInfo.tagName?.first()) {
//                                    mInfo.tagName.removePrefix("#")
//                                } else {
//                                    mInfo.tagName
//                                }
//                                if (finalTagName == state.mentionUser) {
//                                    startActivity(
//                                        HashTagListActivity.getIntent(
//                                            requireContext(), mInfo.hashtagId, mInfo.tagName ?: "", 1
//                                        )
//                                    )
//                                }
//                                isUserNotFound = false
//                            }
//                        } else {
//                            isUserNotFound = true
//                        }
//                        if (isUserNotFound) {
//                            showToast(getString(R.string.label_hashtag_not_found))
//                        }
//                    }
//
//                    is ShortsPageState.GiftClick -> {
//                        val bottomSheet = GiftGalleryBottomSheet.newInstance(
//                            state.dataVideo.user_id.toString(),
//                            state.dataVideo.posts_id.toString(),
//                            true,
//                            isFromWhere = INTENT_IS_FROM_SHORTS
//                        )
//                        bottomSheet.giftResponseState.subscribeAndObserveOnMainThread {
//                            when (it) {
//                                is GiftItemClickStates.SendGiftInChatClick -> {
//                                    val props = JSONObject()
//                                    props.put(Constant.CONTENT_TYPE, "shorts")
//                                    props.put(Constant.CONTENT_ID, state.dataVideo.id)
//
//                                    mp?.track(Constant.SEND_GIFT, props)
//
//                                    state.dataVideo.post.total_gift_count += 1
//                                    playShortsAdapter.notifyDataSetChanged()
//                                }
//
//                                else -> {
//                                }
//                            }
//                        }.autoDispose()
//                        bottomSheet.show(
//                            childFragmentManager, GiftGalleryBottomSheet::class.java.name
//                        )
//                    }
//
//                    is ShortsPageState.OpenLoginPopup -> {
//                        openLoginPopup()
//                    }
//
//                    else -> {}
//                }
//            }
//        }
//        binding.reelsRecyclerView.apply {
//            layoutManager = videoLayoutManager
//            adapter = playShortsAdapter
//        }
//
//        videoLayoutManager.setOnViewPagerListener(object : OnViewPagerListener {
//            override fun onInitComplete() {
//                if (isVideoInitCompleted) {
//                    return
//                }
//                isVideoInitCompleted = true
//                nextVideoUrls?.let {
//                    Timber.i("nextVideoUrl $nextVideoUrls")
//                    MeetFriend.exoCacheManager.prepareCacheVideos(it)
//                }
//            }
//
//            override fun onPageRelease(isNext: Boolean, position: Int) {
//                if (mCurrentPosition == position) {
//                    if (isDelete) {
//                        isDelete = false
//                        Jzvd.releaseAllVideos()
//                        Handler()?.postDelayed({
//                            autoPlayVideo()
//                        }, FiXED_100_MILLISECOND)
//                    }
//                }
//            }
//
//            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
//                Timber.tag("ForYouOnPageSelected").d("onPageSelected: position -> $position")
//                Timber.tag("ForYouOnPageSelected").e("onPageSelected: mCurrentPosition -> $mCurrentPosition")
//
//                if (mCurrentPosition == position) {
//                    return
//                }
//                mCurrentPosition = position
//                Timber.tag(
//                    "ForYouOnPageSelected"
//                ).d("onPageSelected: mCurrentPosition = position -> mCurrentPosition: $mCurrentPosition")
//                nextVideoUrls?.let {
//                    Timber.i("nextVideoUrl $nextVideoUrls")
//                    MeetFriend.exoCacheManager.prepareCacheVideos(it)
//                }
//                if (position != 0 &&
//                    position % loggedInUserCache.getScrollCountAd().toInt() == 0 && !listOfPositionByAds.contains(
//                        position
//                    )
//                ) {
//                    listOfPositionByAds.add(position)
//                    AdsManager.getAdsManager()
//                        .openInterstitialAds(
//                            requireActivity(),
//                            loggedInUserCache.getInterstitialAdId(),
//                            object : AdsManager.InterstitialAdsCallback {
//                                override fun InterstitialResponse(nativeAd: InterstitialAd?) {
//                                    if (nativeAd != null) {
//                                        nativeAd.show(requireActivity())
//                                    }
//                                }
//
//                                override fun adsOnLoaded() {
//                                    return
//                                }
//                            }
//                        )
//                }
//                autoPlayVideo()
//                playShortsAdapter.listOfDataItems = listOfShortsInfo
//                if (isBottom) {
//                    if (arguments?.containsKey(HASHTAG_ID) == true) {
//                        shortsViewModel.loadMorePostH(hashTagId)
//                    } else {
//                        shortsViewModel.loadMoreShorts(0)
//                    }
//                }
//            }
//        })
//
//        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
//            isVideoInitCompleted = false
//            binding.swipeRefreshLayout.isRefreshing = false
//
//            if (hashTagId != 0) {
//                shortsViewModel.resetPaginationForHashTagPost(hashTagId)
//            } else {
//                shortsViewModel.resetPaginationForShorts(0)
//            }
//
//            listOfPositionByAds = arrayListOf()
//        }.autoDispose()
//    }

    private fun listenToViewEvents() {
        setupLayoutManager()
        setupAdapter()
        setupRecyclerView()
        setupSwipeRefresh()
    }

    private fun setupLayoutManager() {
        videoLayoutManager = ViewPagerLayoutManager(requireContext(), OrientationHelper.VERTICAL).apply {
            isItemPrefetchEnabled = true
            isMeasurementCacheEnabled = true
            initialPrefetchItemCount = PRE_FETCH_ITEM_COUNT
            isSmoothScrollbarEnabled = true
        }
    }

    private fun setupAdapter() {
        playShortsAdapter = PlayShortsAdapter(requireContext(), requireActivity()).apply {
            observeShortsViewEvents()
        }
    }

    private fun PlayShortsAdapter.observeShortsViewEvents() {
        shortsViewByUser.observeOn(Schedulers.computation()).filter { postId ->
            val isProcessed = viewedShortsIds.contains(postId)
            if (!isProcessed) {
                viewedShortsIds.add(postId)
            }
            !isProcessed
        }.observeOn(AndroidSchedulers.mainThread()).subscribe { id ->
            if (loggedInUserCache.getLoginUserToken() != null) {
                shortsViewModel.updateShortsCount(ShortsCountRequest(id))
            }
        }.autoDispose()

        playShortsViewClicks.subscribeAndObserveOnMainThread { state ->
            handleShortsPageState(state)
        }.autoDispose()
    }

    private fun handleShortsPageState(state: ShortsPageState) {
        when (state) {
            is ShortsPageState.UserProfileClick -> handleUserProfileClick(state)
            is ShortsPageState.AddReelLikeClick -> likePost(state.dataVideo, true)
            is ShortsPageState.RemoveReelLikeClick -> likePost(state.dataVideo, false)
            is ShortsPageState.OpenLinkAttachment -> openLinkInBrowser(state.dataVideo)
            is ShortsPageState.CommentClick -> openCommentSection(state.dataVideo)
            is ShortsPageState.ShareClick -> handleShareClick(state.dataVideo)
            is ShortsPageState.DownloadClick -> handleDownloadClick(state.dataVideo)
            is ShortsPageState.FollowClick -> handleFollowClick(state.dataVideo)
            is ShortsPageState.MoreClick -> openMoreOptionBottomSheet(state.dataVideo)
            is ShortsPageState.MentionUserClick -> handleMentionUserClick(state)
            is ShortsPageState.HashtagClick -> handleHashtagClick(state)
            is ShortsPageState.GiftClick -> openGiftGallery(state)
            is ShortsPageState.OpenLoginPopup -> openLoginPopup()
        }
    }

    private fun handleMentionUserClick(state: ShortsPageState.MentionUserClick) {
        val tagsList = state.dataVideo.post.mention_users
        var isUserNotFound = true

        if (!tagsList.isNullOrEmpty()) {
            tagsList.forEach { mInfo ->
                if (mInfo.user.userName == state.mentionUser) {
                    startActivity(
                        MyProfileActivity.getIntentWithData(
                            requireContext(),
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

    private fun handleHashtagClick(state: ShortsPageState.HashtagClick) {
        val tagsList = state.dataVideo.post_hashtags
        var isUserNotFound = true

        if (!tagsList.isNullOrEmpty()) {
            tagsList.forEach { mInfo ->
                val finalTagName = if ('#' == mInfo.tagName?.first()) {
                    mInfo.tagName.removePrefix("#")
                } else {
                    mInfo.tagName
                }
                if (finalTagName == state.mentionUser) {
                    startActivity(
                        HashTagListActivity.getIntent(
                            requireContext(),
                            mInfo.hashtagId,
                            mInfo.tagName ?: "",
                            1
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

    @SuppressLint("NotifyDataSetChanged")
    private fun openGiftGallery(state: ShortsPageState.GiftClick) {
        val bottomSheet = GiftGalleryBottomSheet.newInstance(
            state.dataVideo.user_id.toString(),
            state.dataVideo.posts_id.toString(),
            true,
            isFromWhere = INTENT_IS_FROM_SHORTS
        )
        bottomSheet.giftResponseState.subscribeAndObserveOnMainThread {
            when (it) {
                is GiftItemClickStates.SendGiftInChatClick -> {
                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "shorts")
                    props.put(Constant.CONTENT_ID, state.dataVideo.id)

                    mp?.track(Constant.SEND_GIFT, props)

                    state.dataVideo.post.total_gift_count += 1
                    playShortsAdapter.notifyDataSetChanged()
                }

                else -> {
                }
            }
        }.autoDispose()
        bottomSheet.show(
            childFragmentManager,
            GiftGalleryBottomSheet::class.java.name
        )
    }

    private fun openCommentSection(dataVideo: DataVideo) {
        Jzvd.goOnPlayOnPause()
        this.dataVideo = dataVideo
        val bottomSheet = ShortsCommentBottomSheet.newInstance(dataVideo.posts_id.toString())
        bottomSheet.show(childFragmentManager, ShortsCommentBottomSheet::class.java.name)
    }

    private fun handleUserProfileClick(state: ShortsPageState.UserProfileClick) {
        if (state.dataVideo.liveId != 0) {
            startActivity(
                LiveRoomActivity.getIntent(
                    requireContext(),
                    null,
                    state.dataVideo.liveId
                )
            )
        } else {
            startActivity(
                MyProfileActivity.getIntentWithData(
                    requireContext(),
                    state.dataVideo.user_id
                )
            )
        }
    }

    private fun openLinkInBrowser(dataVideo: DataVideo) {
        dataVideo.post.web_link?.let { openUrlInBrowser(it) }
    }

    private fun handleFollowClick(dataVideo: DataVideo) {
        shortsViewModel.followUnfollow(dataVideo.post.user.id)
        val list = playShortsAdapter.listOfDataItems
        list?.filter { it.user_id == dataVideo.post.user.id }?.forEach {
            it.followBack = 1
        }
        playShortsAdapter.listOfDataItems = list
        playShortsAdapter.notifyDataSetChanged()
    }

    private fun handleDownloadClick(dataVideo: DataVideo) {
        XXPermissions.with(requireContext())
            .permission(Permission.READ_MEDIA_VIDEO)
            .request(object : OnPermissionCallback {
                override fun onGranted(
                    permissions: List<String>,
                    all: Boolean
                ) {
                    if (all) {
                        showProgress(true)
                        saveMeetUrFriendsLogo(requireContext())
                        val serviceIntent = Intent(
                            requireActivity(),
                            DownloadService::class.java
                        )
                        serviceIntent.putExtra(
                            "videoUrl",
                            dataVideo.originalVideoUrl
                        )
                        serviceIntent.putExtra("videoId", dataVideo.id)
                        requireActivity().startService(serviceIntent)

                        val props = JSONObject()
                        props.put(SHORTS_ID, dataVideo.id)

                        mp?.track(DOWNLOAD_SHORTS, props)
                    }
                }

                override fun onDenied(
                    permissions: List<String>,
                    never: Boolean
                ) {
                    showProgress(false)
                    if (never) {
                        UiUtils.showToast(
                            requireContext(),
                            resources.getString(R.string.label_storage_permissions_denied)
                        )
                        XXPermissions.startPermissionActivity(
                            requireActivity(),
                            permissions
                        )
                    } else {
                        UiUtils.showToast(
                            requireContext(),
                            " Failed to obtain storage permission "
                        )
                    }
                }
            })
    }

    private fun handleShareClick(dataVideo: DataVideo) {
        ShareHelper.shareDeepLink(
            requireContext(),
            1,
            dataVideo.posts_id,
            "",
            true
        ) {
            ShareHelper.shareText(requireContext(), it)
            val props = JSONObject()
            props.put(Constant.CONTENT_TYPE, "shorts")
            props.put(Constant.CONTENT_ID, dataVideo?.id)

            mp?.track(Constant.SHARE_CONTENT, props)
        }
    }

    private fun setupRecyclerView() {
        binding.reelsRecyclerView.apply {
            layoutManager = videoLayoutManager
            adapter = playShortsAdapter
        }

        videoLayoutManager.setOnViewPagerListener(object : OnViewPagerListener {
            override fun onInitComplete() {
                handleVideoInitCompletion()
            }

            override fun onPageRelease(isNext: Boolean, position: Int) {
                handlePageRelease(position)
            }

            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
                handlePageSelection(position, isBottom)
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.refreshes()
            .subscribeAndObserveOnMainThread {
                refreshContent()
            }.autoDispose()
    }

    private fun handleVideoInitCompletion() {
        if (isVideoInitCompleted) return
        isVideoInitCompleted = true
        nextVideoUrls?.let { MeetFriend.exoCacheManager.prepareCacheVideos(it) }
    }

    private fun handlePageRelease(position: Int) {
        if (mCurrentPosition == position && isDelete) {
            isDelete = false
            Jzvd.releaseAllVideos()
            Handler().postDelayed({ autoPlayVideo() }, FiXED_100_MILLISECOND)
        }
    }

    private fun handlePageSelection(position: Int, isBottom: Boolean) {
        if (mCurrentPosition == position) return
        mCurrentPosition = position

        nextVideoUrls?.let { MeetFriend.exoCacheManager.prepareCacheVideos(it) }

        if (shouldShowAds(position)) {
            showInterstitialAds()
        }

        autoPlayVideo()
        if (isBottom) loadMoreShorts()
    }

    private fun shouldShowAds(position: Int): Boolean {
        return position != 0 &&
            position % loggedInUserCache.getScrollCountAd().toInt() == 0 &&
            !listOfPositionByAds.contains(position)
    }

    private fun showInterstitialAds() {
        listOfPositionByAds.add(mCurrentPosition)
        AdsManager.getAdsManager().openInterstitialAds(
            requireActivity(),
            loggedInUserCache.getInterstitialAdId(),
            object : AdsManager.InterstitialAdsCallback {
                override fun InterstitialResponse(nativeAd: InterstitialAd?) {
                    nativeAd?.show(requireActivity())
                }

                override fun adsOnLoaded() = Unit
            }
        )
    }

    private fun loadMoreShorts() {
        if (arguments?.containsKey(HASHTAG_ID) == true) {
            shortsViewModel.loadMorePostH(hashTagId)
        } else {
            shortsViewModel.loadMoreShorts(0)
        }
    }

    private fun refreshContent() {
        isVideoInitCompleted = false
        binding.swipeRefreshLayout.isRefreshing = false
        if (hashTagId != 0) {
            shortsViewModel.resetPaginationForHashTagPost(hashTagId)
        } else {
            shortsViewModel.resetPaginationForShorts(0)
        }
        listOfPositionByAds = arrayListOf()
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

    private fun openMoreOptionBottomSheet(dataVideo: DataVideo) {
        val isLoggedInUser = loggedInUserCache.getLoggedInUserId() == dataVideo.user_id
        val bottomSheet: LiveStreamingMoreOptionBottomSheet = LiveStreamingMoreOptionBottomSheet.newInstance(
            isFromShorts = true,
            isFromLive = false,
            isLoggedInUser
        )

        bottomSheet.optionClick.subscribeAndObserveOnMainThread {
            when (it) {
                resources.getString(R.string.report) -> {
                    handleReportShortBottomSheet(dataVideo)
                }

                resources.getString(R.string.label_copy_link) -> {
                    handleCopyLink(dataVideo)
                }

                resources.getString(R.string.label_delete) -> {
                    handleDeleteShortAlertDialog(dataVideo)
                }

                resources.getString(R.string.label_edit) -> {
                    handleEditShort(dataVideo)
                }

                else -> {}
            }
        }.autoDispose()
        bottomSheet.show(childFragmentManager, LiveStreamingMoreOptionBottomSheet::class.java.name)
    }

    private fun handleEditShort(dataVideo: DataVideo) {
        val post: Post = if (!dataVideo.post.post_media.isNullOrEmpty()) {
            dataVideo.post
        } else {
            dataVideo.post.copy(post_media = arrayListOf())
        }
        startActivity(
            AddNewPostInfoActivity.launchActivity(
                LaunchActivityData(
                    requireContext(),
                    AddNewPostInfoActivity.POST_TYPE_VIDEO,
                    shortsInfo = post
                )
            )
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleDeleteShortAlertDialog(dataVideo: DataVideo) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setMessage(resources.getString(R.string.label_are_you_sure_you_want_to_delete_shorts))
        builder.setPositiveButton(
            Html.fromHtml(
                "<font color='#B22827'>Delete</font>"
            )
        ) { dialog, _ ->
            dialog.dismiss()
            isDelete = true
            shortsViewModel.deleteShort(dataVideo.posts_id)
            listOfShortsInfo.remove(dataVideo)
            playShortsAdapter.listOfDataItems = listOfShortsInfo
            playShortsAdapter.notifyDataSetChanged()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun handleCopyLink(dataVideo: DataVideo) {
        ShareHelper.shareDeepLink(requireContext(), 1, dataVideo.posts_id, "", true) {
            val clipboard: ClipboardManager = requireActivity().getSystemService(
                AppCompatActivity.CLIPBOARD_SERVICE
            ) as ClipboardManager
            val clip = ClipData.newPlainText("Url", it)
            clipboard.setPrimaryClip(clip)
            showToast(resources.getString(R.string.msg_copied_video_link))
            val props = JSONObject()
            props.put(Constant.CONTENT_TYPE, "shorts")
            props.put(Constant.CONTENT_ID, dataVideo.id)

            mp?.track(Constant.SHARE_CONTENT, props)
        }
    }

    private fun handleReportShortBottomSheet(dataVideo: DataVideo) {
        val reportDialog = ReportDialogFragment.newInstance(dataVideo.posts_id, false, SHORT)
        reportDialog.optionClick.subscribeAndObserveOnMainThread {
            val props = JSONObject()
            props.put(Constant.CONTENT_TYPE, "shorts")
            props.put(Constant.CONTENT_ID, dataVideo.id)

            mp?.track(Constant.REPORT_CONTENT, props)
        }
        reportDialog.show(childFragmentManager, ReportDialogFragment::class.java.name)
    }

    private fun setShortsInfo(shortsList: List<DataVideo>) {
        showProgress(false)
        listOfShortsInfo = shortsList as ArrayList
        listOfShortsInfo.firstOrNull()?.file_path?.let {
            MeetFriend.exoCacheManager.prepareCacheVideo(it)
        }
        playShortsAdapter.listOfDataItems = shortsList
        if (arguments?.containsKey(HASHTAG_ID) == true) {
            binding.noShortsHashTagLinearLayout.isVisible = listOfShortsInfo.isNullOrEmpty()
            binding.tvNoShorts.isVisible = false
        } else {
            binding.tvNoShorts.isVisible = listOfShortsInfo.isNullOrEmpty()
            binding.noShortsHashTagLinearLayout.isVisible = false
        }
    }

    private fun likePost(dataVideo: DataVideo, like: Boolean) {
        try {
            val postLikedDisliked = if (like) {
                Constant.LIKE
            } else {
                Constant.UNLIKE
            }
            val postLikeUnlikeRequest = PostLikeUnlikeRequest(dataVideo.post.id, postLikedDisliked)
            shortsViewModel.shortsLikeDisLike(postLikeUnlikeRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showProgress(isVisible: Boolean) {
        if (isVisible) {
            CallProgressWheel.showLoadingDialog(requireActivity())
        } else {
            CallProgressWheel.dismissLoadingDialog()
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
    }

    override fun onPause() {
        super.onPause()

        Timber.tag("MeetUrFriends1").e("isVideoInitCompleted: $isVideoInitCompleted")
        if (isVideoInitCompleted) {
            Jzvd.goOnPlayOnPause()
        }
    }

    override fun onResume() {
        super.onResume()
        RxBus.listen(RxEvent.CommentUpdate::class.java).subscribeAndObserveOnMainThread {
            dataVideo?.post = it.data!!
            playShortsAdapter.notifyDataSetChanged()
            Jzvd.goOnPlayOnResume()
        }.autoDispose()
        Timber.tag("MeetUrFriends1").e("Resume isVideoInitCompleted: $isVideoInitCompleted")
        RxBus.listen(RxEvent.ShowProgress::class.java).subscribeAndObserveOnMainThread {
            showProgress(it.isShow)
        }.autoDispose()

        if (hashTagId != 0) {
            shortsViewModel.resetPaginationForHashTagPost(hashTagId)
        } else if (mCurrentPosition == 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                Timber.tag(
                    "ForYouOnPageSelected"
                ).i("onResume -> resetPaginationForShorts & mCurrentPosition: $mCurrentPosition")
                shortsViewModel.resetPaginationForShorts(0)
            }, FiXED_200_MILLISECOND)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag("getChildAt").e("Destroy")
        Jzvd.releaseAllVideos()
        isVideoInitCompleted = false
    }
}
