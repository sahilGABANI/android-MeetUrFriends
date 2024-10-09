package com.meetfriend.app.ui.trends

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
import com.meetfriend.app.responseclasses.video.DataVideo
import com.meetfriend.app.ui.hashtag.HashTagListActivity
import com.meetfriend.app.ui.giftsGallery.GiftGalleryBottomSheet
import com.meetfriend.app.ui.home.shorts.ShortsCommentBottomSheet
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
import com.meetfriend.app.utils.Constant.FiXED_1000_MILLISECOND
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
import javax.inject.Inject
import kotlin.properties.Delegates

class HashtagShortsFragment : BasicFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = HashtagShortsFragment()

        var HASHTAG_ID = "HASHTAG_ID"
        var HASHTAG_NAME = "HASHTAG_NAME"
        var FROM_WHERE = "FROM_WHERE"
        var INTENT_SEARCH = "INTENT_SEARCH"

        @JvmStatic
        fun getInstanceForHashTag(
            hashTagId: Int,
            hashTagName: String,
            fromWhere: Int,
            search: String? = null
        ): HashtagShortsFragment {
            val forYouShortsFragment = HashtagShortsFragment()

            val bundle: Bundle = Bundle()
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
    private var listOfShortsInfo: List<DataVideo> = listOf()
    private var listOfPositionByAds: ArrayList<Int> = arrayListOf()
    private var dataVideo: DataVideo? = null
    private var loggedInUserId by Delegates.notNull<Int>()
    private var isFVisible: Boolean = false
    private var deleteIndex: Int? = -1
    private val viewedShortsIds = HashSet<Int>()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ShortsViewModel>
    private lateinit var shortsViewModel: ShortsViewModel

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

        arguments?.let {
            hashTagId = requireArguments().getInt(HASHTAG_ID)
            hashTagName = requireArguments().getString(HASHTAG_NAME)
            fromWhere = requireArguments().getInt(FROM_WHERE)
        }

        binding.useThisHashtagAppCompatTextView.visibility = View.GONE

        RxBus.listen(RxEvent.SearchTextShorts::class.java).subscribeAndObserveOnMainThread {
            shortsViewModel.resetPaginationForHashTagPost(hashTagId, it.search.toString())
        }.autoDispose()

        RxBus.listen(RxEvent.RefreshFoYouFragment::class.java)
            .subscribeAndObserveOnMainThread {
                Jzvd.goOnPlayOnPause()
            }.autoDispose()

        RxBus.listen(RxEvent.PlayHashtagVideo::class.java).subscribeAndObserveOnMainThread {
            isFVisible = it.isPlay
            if (isResumed) {
                if (!isFVisible) {
                    Jzvd.goOnPlayOnPause()
                } else {
                    autoPlayVideo()
                }
            }
        }.autoDispose()

        RxBus.listen(RxEvent.RefreshHashtagVideoFragment::class.java)
            .subscribeAndObserveOnMainThread {
                Handler(Looper.getMainLooper()).postDelayed({
                    autoPlayVideo()
                }, FiXED_1000_MILLISECOND)
            }.autoDispose()

        loggedInUserId = loggedInUserCache.getLoggedInUserId()
        listenToViewModel()
        listenToViewEvents()
    }

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

    private fun listenToViewEvents() {
        setupAdapter()
        setupRecyclerView()
        setupViewPagerListener()
        setupSwipeRefresh()
    }

    private fun setupAdapter() {
        videoLayoutManager = ViewPagerLayoutManager(requireContext(), OrientationHelper.VERTICAL)
        playShortsAdapter = PlayShortsAdapter(requireContext(), requireActivity()).apply {
            if (::playShortsAdapter.isInitialized) {
                if (loggedInUserId != 0) {
                    observeShortsViews(playShortsAdapter)
                    observeAdapterClicks(playShortsAdapter)
                }
            }
        }


    }

    private fun observeShortsViews(playShortsAdapter: PlayShortsAdapter) {
        playShortsAdapter.shortsViewByUser
            .observeOn(Schedulers.computation())
            .filter { postId ->
                val isProcessed = viewedShortsIds.contains(postId)
                if (!isProcessed) viewedShortsIds.add(postId)
                !isProcessed
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { id ->
                loggedInUserCache.getLoginUserToken()?.let {
                    shortsViewModel.updateShortsCount(ShortsCountRequest(id))
                }
            }.autoDispose()
    }

    private fun observeAdapterClicks(playShortsAdapter: PlayShortsAdapter) {
        playShortsAdapter.playShortsViewClicks.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is ShortsPageState.UserProfileClick -> handleUserProfileClick(state)
                is ShortsPageState.AddReelLikeClick -> likePost(state.dataVideo, true)
                is ShortsPageState.RemoveReelLikeClick -> likePost(state.dataVideo, false)
                is ShortsPageState.CommentClick -> handleCommentClick(state)
                is ShortsPageState.ShareClick -> handleShareClick(state)
                is ShortsPageState.OpenLinkAttachment -> state.dataVideo.post.web_link?.let { openUrlInBrowser(it) }
                is ShortsPageState.DownloadClick -> handleDownloadClick(state)
                is ShortsPageState.FollowClick -> handleFollowClick(state)
                is ShortsPageState.MoreClick -> openMoreOptionBottomSheet(state.dataVideo)
                is ShortsPageState.MentionUserClick -> handleMentionUserClick(state)
                is ShortsPageState.HashtagClick -> handleHashtagClick(state)
                is ShortsPageState.GiftClick -> handleGiftClick(state)
                else -> {}
            }
        }.autoDispose()
    }

    private fun handleGiftClick(state: ShortsPageState.GiftClick) {
        val bottomSheet = GiftGalleryBottomSheet.newInstance(
            state.dataVideo.user_id.toString(),
            state.dataVideo.posts_id.toString(),
            true
        )
        bottomSheet.giftResponseState.subscribeAndObserveOnMainThread {
            when (it) {
                is GiftItemClickStates.SendGiftInChatClick -> {
                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "shorts")
                    props.put(Constant.CONTENT_ID, state.dataVideo.id)

                    mp?.track(Constant.SEND_GIFT, props)

                    state.dataVideo.post.total_gift_count =
                        state.dataVideo.post.total_gift_count.plus(1)
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

    private fun handleHashtagClick(state: ShortsPageState.HashtagClick) {
        val tagsList = state.dataVideo.post_hashtags
        var isUserNotFound = true

        if (!tagsList.isNullOrEmpty()) {
            tagsList.forEach { mInfo ->

                var finalTagName =
                    if ('#' == mInfo.tagName?.firstOrNull()) {
                        mInfo.tagName.removePrefix(
                            "#"
                        )
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

    private fun handleFollowClick(state: ShortsPageState.FollowClick) {
        shortsViewModel.followUnfollow(state.dataVideo.post.user.id)
        val list = playShortsAdapter.listOfDataItems
        list?.filter { it.user_id == state.dataVideo.post.user.id }?.forEach {
            it.followBack = 1
        }
        playShortsAdapter.listOfDataItems = list
        playShortsAdapter.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        binding.reelsRecyclerView.apply {
            layoutManager = videoLayoutManager
            adapter = playShortsAdapter
        }
    }

    private fun setupViewPagerListener() {
        videoLayoutManager.setOnViewPagerListener(object : OnViewPagerListener {
            override fun onInitComplete() {
                if (!isVideoInitCompleted) {
                    isVideoInitCompleted = true
                    nextVideoUrls?.let { MeetFriend.exoCacheManager.prepareCacheVideos(it) }
                }
            }

            override fun onPageRelease(isNext: Boolean, position: Int) = Unit

            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
                if (mCurrentPosition != position) {
                    mCurrentPosition = position
                    handlePageSelected()
                }
            }
        })
    }

    private fun handlePageSelected() {
        nextVideoUrls?.let { MeetFriend.exoCacheManager.prepareCacheVideos(it) }
        if (mCurrentPosition % loggedInUserCache.getScrollCountAd().toInt() == 0 &&
            !listOfPositionByAds.contains(mCurrentPosition)
        ) {
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
        autoPlayVideo()
        playShortsAdapter.listOfDataItems = listOfShortsInfo
        shortsViewModel.loadMorePostH(hashTagId)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.refreshes()
            .subscribeAndObserveOnMainThread {
                isVideoInitCompleted = false
                binding.swipeRefreshLayout.isRefreshing = false
                shortsViewModel.resetPaginationForHashTagPost(hashTagId)
                listOfPositionByAds = arrayListOf()
            }.autoDispose()
    }

    // Helper functions for handling specific state actions
    private fun handleUserProfileClick(state: ShortsPageState.UserProfileClick) {
        if (state.dataVideo.liveId != 0) {
            startActivity(LiveRoomActivity.getIntent(requireContext(), null, state.dataVideo.liveId))
        } else {
            startActivity(MyProfileActivity.getIntentWithData(requireContext(), state.dataVideo.user_id))
        }
    }

    private fun handleCommentClick(state: ShortsPageState.CommentClick) {
        Jzvd.goOnPlayOnPause()
        dataVideo = state.dataVideo
        val bottomSheet = ShortsCommentBottomSheet.newInstance(state.dataVideo.posts_id.toString())
        bottomSheet.show(childFragmentManager, ShortsCommentBottomSheet::class.java.name)
    }

    private fun handleShareClick(state: ShortsPageState.ShareClick) {
        ShareHelper.shareDeepLink(requireContext(), 1, state.dataVideo.posts_id, "", true) {
            ShareHelper.shareText(requireContext(), it)
            val props = JSONObject().apply {
                put(Constant.CONTENT_TYPE, "shorts")
                put(Constant.CONTENT_ID, dataVideo?.id)
            }
            mp?.track(Constant.SHARE_CONTENT, props)
        }
    }

    private fun handleDownloadClick(state: ShortsPageState.DownloadClick) {
        XXPermissions.with(requireContext())
            .permission(Permission.READ_MEDIA_VIDEO)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
                        showProgress(true)
                        saveMeetUrFriendsLogo(requireContext())
                        startDownloadService(state.dataVideo)
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    showProgress(false)
                    handlePermissionDenied(never, permissions)
                }
            })
    }

    private fun startDownloadService(dataVideo: DataVideo) {
        val serviceIntent = Intent(requireActivity(), DownloadService::class.java).apply {
            putExtra("videoUrl", dataVideo.originalVideoUrl)
            putExtra("videoId", dataVideo.id)
        }
        requireActivity().startService(serviceIntent)

        val props = JSONObject().apply { put(SHORTS_ID, dataVideo.id) }
        mp?.track(DOWNLOAD_SHORTS, props)
    }

    private fun handlePermissionDenied(never: Boolean, permissions: List<String>) {
        if (never) {
            UiUtils.showToast(requireContext(), resources.getString(R.string.label_storage_permissions_denied))
            XXPermissions.startPermissionActivity(requireActivity(), permissions)
        } else {
            UiUtils.showToast(requireContext(), "Failed to obtain storage permission")
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

    private fun openMoreOptionBottomSheet(dataVideo: DataVideo) {
        val isLoggedInUser = loggedInUserCache.getLoggedInUserId() == dataVideo.user_id
        val bottomSheet = LiveStreamingMoreOptionBottomSheet.newInstance(
            isFromShorts = true,
            isFromLive = false,
            isLoggedInUser
        )

        bottomSheet.optionClick.subscribeAndObserveOnMainThread {
            when (it) {
                resources.getString(R.string.report) -> handleReportOption(dataVideo)
                resources.getString(R.string.label_copy_link) -> handleCopyLinkOption(dataVideo)
                resources.getString(R.string.label_delete) -> handleDeleteOption(dataVideo)
            }
        }.autoDispose()

        bottomSheet.show(childFragmentManager, LiveStreamingMoreOptionBottomSheet::class.java.name)
    }

    private fun handleReportOption(dataVideo: DataVideo) {
        val reportDialog = ReportDialogFragment.newInstance(dataVideo.posts_id, false, SHORT)
        reportDialog.optionClick.subscribeAndObserveOnMainThread {
            val props = JSONObject().apply {
                put(Constant.CONTENT_TYPE, "shorts")
                put(Constant.CONTENT_ID, dataVideo.id)
            }
            mp?.track(Constant.REPORT_CONTENT, props)
        }
        reportDialog.show(childFragmentManager, ReportDialogFragment::class.java.name)
    }

    private fun handleCopyLinkOption(dataVideo: DataVideo) {
        ShareHelper.shareDeepLink(requireContext(), 1, dataVideo.posts_id, "", true) {
            val clipboard = requireActivity().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Url", it)
            clipboard.setPrimaryClip(clip)
            showToast(resources.getString(R.string.msg_copied_video_link))

            val props = JSONObject().apply {
                put(Constant.CONTENT_TYPE, "shorts")
                put(Constant.CONTENT_ID, dataVideo.id)
            }
            mp?.track(Constant.SHARE_CONTENT, props)
        }
    }

    private fun handleDeleteOption(dataVideo: DataVideo) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setMessage(resources.getString(R.string.label_are_you_sure_you_want_to_delete_shorts))
        builder.setPositiveButton(Html.fromHtml("<font color='#B22827'>Delete</font>")) { dialog, _ ->
            dialog.dismiss()
            deleteIndex = playShortsAdapter.listOfDataItems?.indexOf(dataVideo)
            shortsViewModel.deleteShort(dataVideo.posts_id)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun setShortsInfo(shortsList: List<DataVideo>) {
        showProgress(false)
        listOfShortsInfo = shortsList
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

        val player: JzvdStdOutgoer? =
            binding.reelsRecyclerView.getChildAt(0).findViewById(R.id.jzvdStdOutgoer)
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
        if (isVideoInitCompleted) {
            Jzvd.goOnPlayOnPause()
        }
    }

    override fun onResume() {
        super.onResume()
        shortsViewModel.resetPaginationForHashTagPost(hashTagId, "")

        RxBus.listen(RxEvent.CommentUpdate::class.java).subscribeAndObserveOnMainThread {
            dataVideo?.post = it.data!!
            playShortsAdapter.notifyDataSetChanged()
            Jzvd.goOnPlayOnResume()
        }.autoDispose()
        shortsViewModel.resetPaginationForHashTagPost(hashTagId)
        RxBus.listen(RxEvent.ShowProgress::class.java).subscribeAndObserveOnMainThread {
            showProgress(it.isShow)
        }.autoDispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        Jzvd.releaseAllVideos()
        isVideoInitCompleted = false
    }
}
