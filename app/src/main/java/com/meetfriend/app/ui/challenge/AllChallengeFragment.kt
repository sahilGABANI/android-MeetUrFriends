package com.meetfriend.app.ui.challenge

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.OrientationHelper
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.ChallengeCountRequest
import com.meetfriend.app.api.challenge.model.ChallengeItem
import com.meetfriend.app.api.challenge.model.ChallengeType
import com.meetfriend.app.api.post.model.ChallengePageState
import com.meetfriend.app.application.MeetFriend
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentAllChallengeBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getEnum
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.putEnum
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.challenge.bottomsheet.ChallengeBottomSheetFragment
import com.meetfriend.app.ui.challenge.bottomsheet.ChallengeCommentBottomSheetFragment
import com.meetfriend.app.ui.challenge.view.PlayChallengeAdapter
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewModel
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewState
import com.meetfriend.app.ui.hashtag.HashTagListActivity
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utilclasses.ads.AdsManager
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.CHALLENGE_ID
import com.meetfriend.app.utils.Constant.FIX_500_MILLISECOND
import com.meetfriend.app.utils.Constant.FiXED_6_INT
import com.meetfriend.app.utils.Constant.JOIN_CHALLENGE
import com.meetfriend.app.utils.ShareHelper
import com.meetfriend.app.videoplayer.JZMediaMp4ExoKotlin
import com.meetfriend.app.videoplayer.JzvdStdOutgoer
import com.meetfriend.app.videoplayer.OnViewPagerListener
import com.meetfriend.app.videoplayer.ViewPagerLayoutManager
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.properties.Delegates

class AllChallengeFragment : BasicFragment() {

    companion object {
        const val CHALLENGE_TYPE = "CHALLENGE_TYPE"

        @JvmStatic
        fun newInstance(challengeType: ChallengeType): AllChallengeFragment {
            val args = Bundle()
            args.putEnum(CHALLENGE_TYPE, challengeType)
            val fragment = AllChallengeFragment()
            fragment.arguments = args
            return fragment
        }

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
        ): AllChallengeFragment {
            val allChallengeFragment = AllChallengeFragment()

            val bundle: Bundle = Bundle()
            bundle.putInt(HASHTAG_ID, hashTagId)
            bundle.putString(HASHTAG_NAME, hashTagName)
            bundle.putString(INTENT_SEARCH, search)

            bundle.putInt(FROM_WHERE, fromWhere)

            allChallengeFragment.arguments = bundle

            return allChallengeFragment
        }
    }

    private var country: String? = null
    private var city: String? = null
    private var state: String? = null
    private var _binding: FragmentAllChallengeBinding? = null
    private val binding get() = _binding!!

    private lateinit var playShortsAdapter: PlayChallengeAdapter
    private lateinit var videoLayoutManager: ViewPagerLayoutManager

    private var isVideoInitCompleted = false
    private var mCurrentPosition = -1
    private var listOfChallengeInfo: List<ChallengeItem> = listOf()
    private var listOfPositionByAds: ArrayList<Int> = arrayListOf()
    private var loggedInUserId by Delegates.notNull<Int>()

    private var challengeType: ChallengeType = ChallengeType.LiveChallenge

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChallengeViewModel>
    private lateinit var challengeViewModel: ChallengeViewModel
    private var isFVisible: Boolean = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationPermissionGranted: Boolean = true

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    var reportDialog: Dialog? = null
    private var currentLocation: String? = null

    var hashTagId: Int = 0
    var hashTagName: String? = null
    private var fromWhere: Int? = null
    private lateinit var dataVideo: ChallengeItem
    private var count = 0

    private val hasNextItem: Boolean
        get() = mCurrentPosition < (playShortsAdapter.listOfDataItems?.size ?: 0) - 1

    private val nextVideoUrls: List<String>?
        get() {
            val currentPosition = mCurrentPosition
            val listOfItems = playShortsAdapter.listOfDataItems
            if (!listOfItems.isNullOrEmpty() && hasNextItem) {
                val endIndex = minOf(currentPosition + 3, listOfItems.size)
                return listOfItems.subList(currentPosition + 1, endIndex).map { it.filePath ?: "" }
            }
            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllChallengeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MeetFriendApplication.component.inject(this)
        challengeViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()
        arguments?.let {
            challengeType = it.getEnum(CHALLENGE_TYPE, ChallengeType.LiveChallenge)
        }
        listenToViewModel()
        listenToViewEvents()

        RxBus.listen(RxEvent.RefreshChallengeFragment::class.java).subscribeAndObserveOnMainThread {
            Jzvd.goOnPlayOnPause()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        challengeViewModel.challengeState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChallengeViewState.ChallengeLikeSuccess -> {
                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "challenge")
                    props.put(Constant.CONTENT_ID, it.challengeId)

                    mp?.track(Constant.SHARE_CONTENT, props)
                }

                is ChallengeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is ChallengeViewState.ListOfAllChallenge -> {
                    setShortsInfo(it.listOfAllChallenge)

                    if (it.listOfAllChallenge.isNotEmpty()) {
                        challengeViewModel.challengeViewByUser(it.listOfAllChallenge[0].id.toString())
                    }
                }

                is ChallengeViewState.ListOfAllChallengeHashTag -> {
                    setShortsInfo(it.listOfAllChallenge)

                    if (it.listOfAllChallenge.isNotEmpty()) {
                        challengeViewModel.challengeViewByUser(it.listOfAllChallenge[0].id.toString())
                    } else {
                        if (hashTagId != 0) {
                            binding.noPostsLinearLayout.isVisible = true
                        }
                    }
                }

                is ChallengeViewState.ListOfMyChallenge -> {
                    setShortsInfo(it.listOfAllChallenge)

                    if (it.listOfAllChallenge.isNotEmpty()) {
                        challengeViewModel.challengeViewByUser(it.listOfAllChallenge[0].id.toString())
                        binding.tvNoShorts.isVisible = false
                    }
                }

                is ChallengeViewState.ListOfCompleteChallenge -> {
                    setShortsInfo(it.listOfAllChallenge)

                    if (it.listOfAllChallenge.isNotEmpty()) {
                        challengeViewModel.challengeViewByUser(it.listOfAllChallenge[0].id.toString())
                    }
                }

                is ChallengeViewState.ListOfLiveChallenge -> {
                    setShortsInfo(it.listOfAllChallenge)

                    if (it.listOfAllChallenge.isNotEmpty()) {
                        challengeViewModel.challengeViewByUser(it.listOfAllChallenge[0].id.toString())
                    }
                }

                is ChallengeViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }

                is ChallengeViewState.EmptyState -> {
                    binding.tvNoShorts.isVisible = true
                }

                else -> {
                }
            }
        }.autoDispose()
    }

    @SuppressLint("MissingPermission")
    private fun locationPermission() {
        XXPermissions.with(
            this
        ).permission(Permission.ACCESS_COARSE_LOCATION).permission(Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    locationPermissionGranted = all
                    val task = fusedLocationProviderClient.lastLocation
                    task.addOnSuccessListener { location ->
                        location?.let {
                            val lat = it.latitude
                            val longi = it.longitude
                            currentLocation = "$lat, $longi"
                            Timber.tag("OkhttpClient").i("locationPermission : $currentLocation")
                            getAddress(lat, longi)
                        }
                    }.addOnFailureListener { exception ->
                        exception.localizedMessage?.let {
                            showLongToast(it)
                        }
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    Timber.tag("OkhttpClient").i("locationPermission : $permissions")
                    locationPermissionGranted = false
                }
            })
    }

    private fun getAddress(latitude: Double, longitude: Double) {
        try {
            val geo = Geocoder(requireActivity(), Locale.getDefault())
            val addresses = geo.getFromLocation(
                latitude,
                longitude,
                1
            )
            city = addresses?.get(0)?.locality ?: ""
            state = addresses?.get(0)?.adminArea ?: ""
            country = addresses?.get(0)?.countryName ?: ""
            Timber.tag("OkhttpClient").i("City : $city, \n state :$state, \n country : $country")
        } catch (e: Exception) {
            Timber.tag("OkhttpClient").i("Failed : $e")
        }
        val challengeBottomSheetFragment = ChallengeBottomSheetFragment.newInstance(
            dataVideo.id ?: 0,
            dataVideo.title ?: ""
        )
        challengeBottomSheetFragment.optionClick.subscribeAndObserveOnMainThread { challengeId ->
            val list = playShortsAdapter.listOfDataItems

            list?.find { it.id == challengeId.toInt() }?.apply {
                status = 1
            }

            val props = JSONObject()
            props.put(CHALLENGE_ID, challengeId)

            mp?.track(JOIN_CHALLENGE, props)
            playShortsAdapter.listOfDataItems = list
        }
        challengeBottomSheetFragment.show(
            childFragmentManager,
            ChallengeBottomSheetFragment::class.java.name
        )
    }

    private fun handleMoreClick(state: ChallengePageState.MoreClick) {
        val userId = loggedInUserCache.getLoggedInUserId()
        val dataVideoUserId = state.dataVideo.user?.id ?: 0
        val isSameUser = userId == dataVideoUserId
        val challengeReactions = state.dataVideo.challengeReactions

        // Check if live join is rejected
        val isRejectLiveJoin = when {
            isSameUser -> false
            state.dataVideo.status != 1 -> false
            challengeReactions?.status != 0 -> false
            else -> {
                val cityHasAll = state.dataVideo.challengeCity?.any { it?.cityData?.name == "All" } == true
                val stateHasAll = state.dataVideo.challengeState?.any { it?.stateData?.name == "All" } == true
                val countryHasAll = state.dataVideo.challengeCountry?.any { it?.countryData?.name == "All" } == true

                // Check if user matches challenge locations
                val isCityValid = state.dataVideo.challengeCity?.any {
                    it?.cityData?.name == state.dataVideo.userCity
                } == true
                val isStateValid = state.dataVideo.challengeState?.any {
                    it?.stateData?.name == state.dataVideo.userState
                } == true
                val isCountryValid = state.dataVideo.challengeCountry?.any {
                    it?.countryData?.name == state.dataVideo.userCountry
                } == true

                when {
                    cityHasAll && stateHasAll && countryHasAll -> true
                    stateHasAll && isCountryValid -> true
                    cityHasAll && isStateValid -> true
                    isCityValid -> true
                    else -> false
                }
            }
        }

        openMoreOptionBottomSheet(
            state.dataVideo.id ?: 0,
            dataVideoUserId,
            isRejectLiveJoin
        )
    }

    private fun listenToViewEvents() {
        setupVideoLayoutManager()
        setupPlayShortsAdapter()

        binding.reelsRecyclerView.apply {
            layoutManager = videoLayoutManager
            adapter = playShortsAdapter
        }

        setupViewPagerListener()
        setupSwipeRefresh()
        subscribeToRxBusEvents()
    }

    private fun setupVideoLayoutManager() {
        videoLayoutManager = ViewPagerLayoutManager(requireContext(), OrientationHelper.VERTICAL)
    }

    private fun setupPlayShortsAdapter() {
        playShortsAdapter = PlayChallengeAdapter(requireContext(), challengeType).apply {
            if (loggedInUserId != 0) {
                playShortsViewClicks.subscribeAndObserveOnMainThread { state ->
                    handlePlayShortsViewClick(state)
                }
            }
        }
    }

    private fun handlePlayShortsViewClick(state: ChallengePageState) {
        when (state) {
            is ChallengePageState.HashtagClick -> handleHashtagClick(state)
            is ChallengePageState.UserProfileClick -> handleUserProfileClick(state)
            is ChallengePageState.UserChallengeWinnerProfileClick -> handleUserChallengeWinnerProfileClick(state)
            is ChallengePageState.ReelLikeClick -> showLikeChallengeOption(state.dataVideo)
            is ChallengePageState.AddReelLikeClick -> handleAddReelLikeClick(state)
            is ChallengePageState.RemoveReelLikeClick -> handleRemoveReelLikeClick(state)
            is ChallengePageState.CommentClick -> showCommentBottomSheet(state)
            is ChallengePageState.ShareClick -> shareVideo(state)
            is ChallengePageState.MoreClick -> handleMoreClick(state)
            is ChallengePageState.JoinChallengeClick -> handleJoinChallengeClick(state)
            is ChallengePageState.OpenLinkAttachment -> openLinkAttachment(state)
            is ChallengePageState.UserChallengeReplyProfileClick -> handleUserChallengeReplyProfileClick(state)
            is ChallengePageState.FollowClick -> handleFollowClick(state)
            else -> {}
        }
    }

    private fun handleHashtagClick(state: ChallengePageState.HashtagClick) {
        val tagsList = state.dataVideo.postHashtags
        var isUserNotFound = true

        tagsList?.forEach { mInfo ->
            if (mInfo.tagName?.replace("#", "") == state.mentionUser) {
                startActivity(
                    HashTagListActivity.getIntent(
                        requireContext(),
                        mInfo.hashtagId,
                        mInfo.tagName,
                        2
                    )
                )
                isUserNotFound = false
            }
        }

        if (isUserNotFound) {
            showToast(getString(R.string.label_hashtag_not_found))
        }
    }

    private fun handleUserProfileClick(state: ChallengePageState.UserProfileClick) {
        state.dataVideo.user?.id?.let { userId ->
            startActivity(MyProfileActivity.getIntentWithData(requireContext(), userId))
        }
    }

    private fun handleUserChallengeWinnerProfileClick(state: ChallengePageState.UserChallengeWinnerProfileClick) {
        state.dataVideo.winnerUser?.id?.let { winnerId ->
            startActivity(MyProfileActivity.getIntentWithData(requireContext(), winnerId))
        }
    }

    private fun showLikeChallengeOption(state: ChallengeItem) {
        val likeChallengeOptionBottomSheet = LikeChallengeOptionBottomSheet.newInstance(state.id ?: 0)
        likeChallengeOptionBottomSheet.show(childFragmentManager, LikeChallengeOptionBottomSheet::javaClass.name)
    }

    private fun handleAddReelLikeClick(state: ChallengePageState.AddReelLikeClick) {
        if (ChallengeType.MyChallenge == challengeType) {
            showLikeChallengeOption(state.dataVideo)
        } else {
            likePost(state.dataVideo)
        }
    }

    private fun handleRemoveReelLikeClick(state: ChallengePageState.RemoveReelLikeClick) {
        if (ChallengeType.MyChallenge == challengeType) {
            showLikeChallengeOption(state.dataVideo)
        } else {
            likePost(state.dataVideo)
        }
    }

    private fun showCommentBottomSheet(state: ChallengePageState.CommentClick) {
        state.dataVideo.id?.let { videoId ->
            val challengeCommentBottomSheetFragment =
                ChallengeCommentBottomSheetFragment.newInstance(
                    videoId,
                    state.dataVideo.user?.profilePhoto
                )
            challengeCommentBottomSheetFragment.optionClick.subscribeAndObserveOnMainThread {
                val props = JSONObject().apply {
                    put(Constant.CONTENT_TYPE, "challenge")
                    put(Constant.CONTENT_ID, videoId)
                }
                mp?.track(Constant.COMMENT_CONTENT, props)
            }
            challengeCommentBottomSheetFragment.show(
                childFragmentManager,
                ChallengeCommentBottomSheetFragment::class.java.name
            )
        }
    }

    private fun shareVideo(state: ChallengePageState.ShareClick) {
        state.dataVideo.id?.let { videoId ->
            ShareHelper.shareDeepLink(requireContext(), FiXED_6_INT, videoId, "", true) {
                ShareHelper.shareText(requireContext(), it)
                showToast(getString(R.string.msg_copied_video_link))
                val props = JSONObject().apply {
                    put(Constant.CONTENT_TYPE, "challenge")
                    put(Constant.CONTENT_ID, videoId)
                }
                mp?.track(Constant.SHARE_CONTENT, props)
            }
        }
    }

    private fun handleJoinChallengeClick(state: ChallengePageState.JoinChallengeClick) {
        dataVideo = state.dataVideo
        locationPermission()
    }

    private fun openLinkAttachment(state: ChallengePageState.OpenLinkAttachment) {
        dataVideo = state.dataVideo
        state.dataVideo.webLink?.let { openUrlInBrowser(it) }
    }

    private fun handleUserChallengeReplyProfileClick(state: ChallengePageState.UserChallengeReplyProfileClick) {
        state.dataVideo.id?.let { postId ->
            startActivity(ChallengeReplyActivity.newInstance(requireContext(), challengeId = postId))
        }
    }

    private fun handleFollowClick(state: ChallengePageState.FollowClick) {
        challengeViewModel.followUnfollow(state.dataVideo.user?.id ?: 0)
        playShortsAdapter.listOfDataItems?.filter { it.user?.id == state.dataVideo.user?.id }?.forEach {
            it.followBack = 1
        }
        playShortsAdapter.notifyDataSetChanged()
    }

    private fun setupViewPagerListener() {
        videoLayoutManager.setOnViewPagerListener(object : OnViewPagerListener {
            override fun onInitComplete() {
                handleInitComplete()
            }

            override fun onPageRelease(isNext: Boolean, position: Int) {
                // Handle page release if needed
            }

            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
                handlePageSelected(position, isBottom)
            }
        })
    }

    private fun handleInitComplete() {
        if (isVideoInitCompleted) return
        isVideoInitCompleted = true
        nextVideoUrls?.let {
            Timber.i("nextVideoUrl $nextVideoUrls")
            MeetFriend.exoCacheManager.prepareCacheVideos(it)
        }
    }

    private fun handlePageSelected(position: Int, isBottom: Boolean) {
        if (mCurrentPosition == position) return

        nextVideoUrls?.let {
            MeetFriend.exoCacheManager.prepareCacheVideos(it)
        }

        if (position < listOfChallengeInfo.size) {
            val postId = listOfChallengeInfo[position].id
            challengeViewModel.challengeViewByUser(postId.toString())
        }

        handleAdLoading(position)

        autoPlayVideo()
        mCurrentPosition = position
        playShortsAdapter.listOfDataItems = listOfChallengeInfo
        if (isBottom) loadMoreChallenges()
    }

    private fun handleAdLoading(position: Int) {
        if (position != 0 &&
            position % loggedInUserCache.getScrollCountAd().toInt() == 0 &&
            !listOfPositionByAds.contains(position)
        ) {
            listOfPositionByAds.add(position)
            AdsManager.getAdsManager().openInterstitialAds(
                requireActivity(),
                loggedInUserCache.getInterstitialAdId(),
                object : AdsManager.InterstitialAdsCallback {
                    override fun InterstitialResponse(nativeAd: InterstitialAd?) {
                        nativeAd?.show(requireActivity())
                    }

                    override fun adsOnLoaded() {
                        return
                    }
                }
            )
        }
    }

    private fun loadMoreChallenges() {
        when (challengeType) {
            ChallengeType.AllChallenge -> {
                if (loggedInUserCache.getLoginUserToken() != null) {
                    challengeViewModel.loadMoreAllChallenge()
                }
            }

            ChallengeType.MyChallenge -> {
                if (loggedInUserCache.getLoginUserToken() != null) {
                    challengeViewModel.loadMoreMyChallenge()
                }
            }

            ChallengeType.LiveChallenge -> {
                if (loggedInUserCache.getLoginUserToken() != null) {
                    challengeViewModel.loadMoreLiveChallenge()
                }
            }

            ChallengeType.CompletedChallenge -> {
                if (loggedInUserCache.getLoginUserToken() != null) {
                    challengeViewModel.loadMoreCompleteChallenge()
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            isVideoInitCompleted = false
            binding.swipeRefreshLayout.isRefreshing = false
            if (fromWhere == 0 || fromWhere == 1) {
                challengeViewModel.resetPaginationForHashTagPost(hashTagId)
            } else {
                apiCalling()
            }
            listOfPositionByAds = arrayListOf()
        }.autoDispose()
    }

    private fun subscribeToRxBusEvents() {
        RxBus.listen(RxEvent.PlayChallengeVideo::class.java).subscribeAndObserveOnMainThread {
            isFVisible = it.isPlay
            if (playShortsAdapter.listOfDataItems.isNullOrEmpty()) {
                playShortsAdapter.listOfDataItems = listOfChallengeInfo
                Handler(Looper.getMainLooper()).postDelayed({
                    manageAutoPlay(it.type)
                }, FIX_500_MILLISECOND)
            } else {
                manageAutoPlay(it.type)
            }
        }.autoDispose()
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

    private fun manageAutoPlay(type: ChallengeType) {
        if (isResumed) {
            if (!isFVisible) {
                Jzvd.goOnPlayOnPause()
            } else {
                if (type == challengeType) {
                    autoPlayVideo()
                }
            }
        }
    }

    private fun apiCalling() {
        when (challengeType) {
            ChallengeType.AllChallenge -> {
                if (loggedInUserCache.getLoginUserToken() != null) {
                    challengeViewModel.resetPaginationForAllChallenge()
                }
            }

            ChallengeType.MyChallenge -> {
                if (loggedInUserCache.getLoginUserToken() != null) {
                    challengeViewModel.resetPaginationForMyChallenge()
                }
            }

            ChallengeType.LiveChallenge -> {
                if (loggedInUserCache.getLoginUserToken() != null) {
                    challengeViewModel.resetPaginationForLiveChallenge()
                }
            }

            ChallengeType.CompletedChallenge -> {
                if (loggedInUserCache.getLoginUserToken() != null) {
                    challengeViewModel.resetPaginationForCompleteChallenge()
                }
            }
        }
    }

    private fun openMoreOptionBottomSheet(
        challengeId: Int,
        challengeUserId: Int,
        isRejectLiveJoin: Boolean
    ) {
        val bottomSheet: MoreOptionBottomSheet = MoreOptionBottomSheet.newInstance(
            challengeId = challengeId,
            challengeUserId,
            isRejectLiveJoin
        )

        bottomSheet.optionClick.subscribeAndObserveOnMainThread {
            when (it) {
                resources.getString(R.string.report) -> {
                    val reportDialog = ReportOptionBottomSheet.newInstance(challengeId, null)
                    reportDialog.optionClick.subscribeAndObserveOnMainThread {
                        val props = JSONObject()
                        props.put(Constant.CONTENT_TYPE, "challenge")
                        props.put(Constant.CONTENT_ID, challengeId)

                        mp?.track(Constant.REPORT_CONTENT, props)
                    }
                    reportDialog.show(
                        childFragmentManager,
                        ReportOptionBottomSheet::class.java.name
                    )
                }

                resources.getString(R.string.label_copy_link) -> {
                    ShareHelper.shareDeepLink(requireContext(), FiXED_6_INT, challengeId, "", true) {
                        val clipBoard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("Share challenge link", it)
                        clipBoard.setPrimaryClip(clipData)
                        showToast(resources.getString(R.string.msg_copied_video_link))
                    }
                }

                resources.getString(R.string.label_delete) -> {
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setCancelable(false)
                    builder.setMessage(resources.getString(R.string.label_are_you_sure_you_want_to_delete_challenge))
                    builder.setPositiveButton(resources.getString(R.string.label_yes)) { dialog, _ ->
                        dialog.dismiss()
                        val list = playShortsAdapter.listOfDataItems as ArrayList<ChallengeItem>
                        val item = list.find { it.id == challengeId }
                        list.remove(item)
                        playShortsAdapter.listOfDataItems = list
                        binding.tvNoShorts.isVisible = list.size == 0
                        challengeViewModel.deleteChallenge(ChallengeCountRequest(challengeId))
                    }
                    builder.setNegativeButton(resources.getString(R.string.label_cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    val alert = builder.create()
                    alert.show()
                }
            }
        }.autoDispose()
        bottomSheet.show(childFragmentManager, MoreOptionBottomSheet::class.java.name)
    }

    private fun setShortsInfo(shortsList: List<ChallengeItem>) {
        showProgress(false)
        listOfChallengeInfo = shortsList
        listOfChallengeInfo.forEach {
            it.let {
                it.userCity = city
                it.userState = state
                it.userCountry = country
            }
        }

        if (isFVisible) {
            playShortsAdapter.listOfDataItems = listOfChallengeInfo
        }
    }

    private fun likePost(dataVideo: ChallengeItem) {
        try {
            if (dataVideo.id != null) {
                challengeViewModel.challengeLikeUnLike(ChallengeCountRequest(dataVideo.id))
            }
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
                JZMediaMp4ExoKotlin::class.java
            )
            startVideoAfterPreloading()
        }
    }

    override fun onPause() {
        super.onPause()
        Jzvd.goOnPlayOnPause()
    }

    override fun onResume() {
        super.onResume()
        if (count == 0) {
            if (arguments?.containsKey(HASHTAG_ID) == true) {
                isFVisible = true
                arguments?.let {
                    hashTagId = requireArguments().getInt(HASHTAG_ID)
                    hashTagName = requireArguments().getString(HASHTAG_NAME)
                    fromWhere = requireArguments().getInt(FROM_WHERE)
                }

                if (fromWhere == 0) {
                    binding.useThisHashtagAppCompatTextView.visibility = View.VISIBLE
                    binding.useThisHashtagAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                        XXPermissions.with(
                            requireContext()
                        ).permission(Permission.CAMERA).permission(Permission.RECORD_AUDIO)
                            .permission(
                                Permission.READ_MEDIA_IMAGES
                            ).permission(Permission.READ_MEDIA_VIDEO).request(object : OnPermissionCallback {

                                override fun onGranted(
                                    permissions: MutableList<String>,
                                    all: Boolean
                                ) {
                                    if (all) {
                                        startActivity(
                                            CreateChallengeActivity.getIntent(
                                                requireContext(),
                                                hashTagName
                                            )
                                        )
                                    }
                                }

                                override fun onDenied(
                                    permissions: MutableList<String>,
                                    never: Boolean
                                ) {
                                    super.onDenied(permissions, never)
                                    if (never) {
                                        showToast(
                                            "Authorization is permanently denied, please manually grant permissions"
                                        )
                                        XXPermissions.startPermissionActivity(
                                            requireContext(),
                                            permissions
                                        )
                                    } else {
                                        showToast(getString(R.string.msg_permission_denied))
                                    }
                                }
                            })
                    }
                    challengeViewModel.resetPaginationForHashTagPost(hashTagId, null)
                } else {
                    binding.useThisHashtagAppCompatTextView.visibility = View.GONE
                    challengeViewModel.resetPaginationForHashTagPost(hashTagId, "")
                    RxBus.listen(RxEvent.SearchTextChallenges::class.java).subscribeAndObserveOnMainThread {
                        challengeViewModel.resetPaginationForHashTagPost(
                            hashTagId,
                            it.search
                        )
                    }.autoDispose()
                }
            } else {
                binding.useThisHashtagAppCompatTextView.visibility = View.GONE
                apiCalling()
            }
            count += 1
        }

        if (arguments?.containsKey(HASHTAG_ID) ?: false) {
            isFVisible = true
        }

        if (isResumed && !isVideoInitCompleted) {
            Jzvd.goOnPlayOnPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        count = 0
        Jzvd.releaseAllVideos()
        isVideoInitCompleted = false
    }
}
