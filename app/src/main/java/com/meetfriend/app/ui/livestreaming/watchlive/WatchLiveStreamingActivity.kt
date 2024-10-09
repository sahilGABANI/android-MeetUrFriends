package com.meetfriend.app.ui.livestreaming.watchlive

import ai.deepar.ar.*
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.util.DisplayMetrics
import android.util.Size
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.makeramen.roundedimageview.RoundedImageView
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.MessageType
import com.meetfriend.app.api.effect.model.CenterZoomLayoutManager
import com.meetfriend.app.api.effect.model.EffectResponse
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.livestreaming.model.*
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityWatchLiveStreamingBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.socket.SocketDataManager
import com.meetfriend.app.ui.chatRoom.roomview.KickOutDialogFragment
import com.meetfriend.app.ui.chatRoom.roomview.ViewUserBottomSheet
import com.meetfriend.app.ui.coins.CoinPlansActivity
import com.meetfriend.app.ui.giftsGallery.GiftGalleryBottomSheet
import com.meetfriend.app.ui.home.shorts.report.ReportDialogFragment
import com.meetfriend.app.ui.livestreaming.DeepARRenderer
import com.meetfriend.app.ui.livestreaming.LiveStreamAcceptRejectDialog
import com.meetfriend.app.ui.livestreaming.LiveStreamingMoreOptionBottomSheet
import com.meetfriend.app.ui.livestreaming.LiveUserOptionBottomSheet
import com.meetfriend.app.ui.livestreaming.game.GameResultBottomSheet
import com.meetfriend.app.ui.livestreaming.game.GameTopContributorBottomSheet
import com.meetfriend.app.ui.livestreaming.game.PlayGameAcceptRejectDialog
import com.meetfriend.app.ui.livestreaming.videoroom.LiveRoomActivity
import com.meetfriend.app.ui.livestreaming.view.LiveEffectsAdapter
import com.meetfriend.app.ui.livestreaming.view.LiveStreamingCommentAdapter
import com.meetfriend.app.ui.livestreaming.watchlive.viewmodel.WatchLiveVideoListState
import com.meetfriend.app.ui.livestreaming.watchlive.viewmodel.WatchLiveVideoViewModel
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.LIVE
import com.meetfriend.app.utils.DeeparDetails
import com.meetfriend.app.utils.FileUtils.toDate
import com.meetfriend.app.utils.ShareHelper
import com.meetfriend.app.utils.UiUtils
import com.petersamokhin.android.floatinghearts.HeartsView
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class WatchLiveStreamingActivity : BasicActivity(), AREventListener {
    companion object {
        private const val LIVE_EVENT_INFO = "LIVE_EVENT_INFO"
        private const val NEW_LIVE_EVENT_INFO = "NEW_LIVE_EVENT_INFO"

        fun getIntent(context: Context, liveEventInfo: LiveEventInfo): Intent {
            val intent =
                Intent(MeetFriendApplication.context, WatchLiveStreamingActivity::class.java)
            intent.putExtra(LIVE_EVENT_INFO, liveEventInfo)
            return intent
        }

        fun getIntentWihData(liveEventInfo: LiveEventInfo): Intent {
            val intent =
                Intent(MeetFriendApplication.context, WatchLiveStreamingActivity::class.java)
            intent.putExtra(NEW_LIVE_EVENT_INFO, liveEventInfo)
            return intent
        }
    }

    private lateinit var gestureDetector: GestureDetector
    lateinit var binding: ActivityWatchLiveStreamingBinding
    lateinit var liveStreamingCommentAdapter: LiveStreamingCommentAdapter

    private var engine: RtcEngine? = null
    private var myUid = 0
    private var joined = false
    private var handler: Handler? = null
    private lateinit var liveEventInfo: LiveEventInfo
    private var isCoHost: Boolean = false
    private var isPause: Boolean = false
    private var firstCoHost: Boolean = false
    private var secondCoHost: Boolean = false
    private var thirdCoHost: Boolean = false
    private var fourthCoHost: Boolean = false
    private var isKickUser: Int? = null

    private var firstCoHostUid = 0 // It is always main host UID
    private var secondCoHostUid = 0
    private var thirdCoHostUid = 0
    private var fourthCoHostUid = 0
    private var countTimerDisposable: Disposable? = null
    private var isInitialEmit = 0
    private var acceptClick = false
    private var channelAlreadyJoined = false

    private var listOfCoHostId: ArrayList<Int> = arrayListOf()
    private var topGifterInfo: TopGifterInfo? = null
    private var isGameStarted: Boolean = false
    private var listOfCoHostInfo: CoHostListInfo? = null
    private var firstFollowUid: Int = 0
    private var secondFollowUid: Int = 0
    private var thirdFollowUid: Int = 0
    private var fourthFollowUid: Int = 0
    private var centsValue: String? = null
    private var newListItems: ArrayList<EffectResponse> = arrayListOf()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<WatchLiveVideoViewModel>
    private lateinit var watchLiveVideoViewModel: WatchLiveVideoViewModel

    @Inject
    lateinit var socketDataManager: SocketDataManager


    // Default camera lens value, change to CameraSelector.LENS_FACING_BACK to initialize with back camera
    private val defaultLensFacing = CameraSelector.LENS_FACING_FRONT
    private var lensFacing = defaultLensFacing
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private val NUMBER_OF_BUFFERS = 2

    private var buffers: Array<ByteBuffer?>? = arrayOfNulls(NUMBER_OF_BUFFERS)
    private var allocatedBufferSize = 0
    private var currentBuffer = 0

    private var deepAR: DeepAR? = null
    private var surfaceView: GLSurfaceView? = null
    private var renderer: DeepARRenderer? = null
    private var callInProgress = false
    private lateinit var liveEffectsAdapter: LiveEffectsAdapter

    private var buffersInitialized = false
    private var imageAnalysis: ImageAnalysis? = null
    var liveStreamNoOfCoHostHashMap = HashMap<Int, String>()
    private val imageAnalyzer = ImageAnalysis.Analyzer { image ->
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        if (!buffersInitialized) {
            buffersInitialized = true
            initializeBuffers(ySize + uSize + vSize)
        }
        val byteData = ByteArray(ySize + uSize + vSize)
        val width = image.width
        val yStride = image.planes[0].rowStride
        val uStride = image.planes[1].rowStride
        val vStride = image.planes[2].rowStride
        var outputOffset = 0
        if (width == yStride) {
            yBuffer[byteData, outputOffset, ySize]
            outputOffset += ySize
        } else {
            var inputOffset = 0
            while (inputOffset < ySize) {
                yBuffer.position(inputOffset)
                yBuffer[byteData, outputOffset, Math.min(yBuffer.remaining(), width)]
                outputOffset += width
                inputOffset += yStride
            }
        }
        //U and V are swapped
        if (width == vStride) {
            vBuffer[byteData, outputOffset, vSize]
            outputOffset += vSize
        } else {
            var inputOffset = 0
            while (inputOffset < vSize) {
                vBuffer.position(inputOffset)
                vBuffer[byteData, outputOffset, Math.min(vBuffer.remaining(), width)]
                outputOffset += width
                inputOffset += vStride
            }
        }
        if (width == uStride) {
            uBuffer[byteData, outputOffset, uSize]
            outputOffset += uSize
        } else {
            var inputOffset = 0
            while (inputOffset < uSize) {
                uBuffer.position(inputOffset)
                uBuffer[byteData, outputOffset, Math.min(uBuffer.remaining(), width)]
                outputOffset += width
                inputOffset += uStride
            }
        }
        buffers!![currentBuffer]?.put(byteData)
        buffers!![currentBuffer]?.position(0)
        if (deepAR != null) {
            deepAR?.receiveFrame(
                buffers!![currentBuffer],
                image.width, image.height,
                image.imageInfo.rotationDegrees,
                lensFacing == CameraSelector.LENS_FACING_FRONT,
                DeepARImageFormat.YUV_420_888,
                image.planes[1].pixelStride
            )
        }
        currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS
        image.close()
    }
    private val getThumbModel: HeartsView.Model by lazy {
        val bitmap = AppCompatResources.getDrawable(
            this@WatchLiveStreamingActivity,
            R.drawable.ic_fill_heart
        )?.toBitmap()
        HeartsView.Model(
            0,
            bitmap!!
        )
    }


    private val listOfLiveEventSendOrReadComment: ArrayList<LiveEventSendOrReadComment> =
        arrayListOf()

    private var user1ContributorList: List<TopGifter>? = null
    private var user2ContributorList: List<TopGifter>? = null
    private var totalCoins = 0.0
    private var isHide = true
    private var isEffectSelected = false

    private fun initDeepar() {
        callInProgress = false

        var llm = CenterZoomLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        liveEffectsAdapter = LiveEffectsAdapter(this@WatchLiveStreamingActivity).apply {
            effectClicks.subscribeAndObserveOnMainThread { response ->
                isEffectSelected = true
                MeetFriendApplication.assetManager?.let { assetManager ->
                    try {
                        val stream: InputStream = assetManager.open(response.effectFileName)
                        deepAR?.switchEffect(response.type, stream)
                    } catch (e: Exception) {
                    }
                    hideEffects()
                    liveEffectsAdapter.listOfEffects?.indexOf(response)?.let { it1 ->
                        binding.typeOfEffectsRecyclerView.smoothScrollToPosition(it1)
                    }
                }
            }
        }

        binding.typeOfEffectsRecyclerView.apply {
            adapter = liveEffectsAdapter
            layoutManager = llm

            addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollHorizontally(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        Toast.makeText(this@WatchLiveStreamingActivity, "Last", Toast.LENGTH_LONG)
                            .show()

                        liveEffectsAdapter.listOfEffects?.lastOrNull()?.let { response ->

                            try {
                                MeetFriendApplication.assetManager?.let { assetManager ->
                                    val stream: InputStream =
                                        assetManager.open(response.effectFileName)
                                    deepAR?.switchEffect(response.type, stream)
                                }
                            } catch (e: Exception) {
                            }
                            hideEffects()
                        }
                    }
                }
            })
        }

        newListItems = DeeparDetails.getMasks()
        liveEffectsAdapter.listOfEffects = DeeparDetails.getMasks()

        binding.maskFlipAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.maskFlipAppCompatImageView.isSelected = true
            binding.filtersFlipAppCompatImageView.isSelected = false
            binding.effectsFlipAppCompatImageView.isSelected = false
            binding.backgroundAppCompatImageView.isSelected = false
            newListItems = DeeparDetails.getMasks()
            liveEffectsAdapter.listOfEffects = DeeparDetails.getMasks()
            showEffects()
        }

        binding.filtersFlipAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.maskFlipAppCompatImageView.isSelected = false
            binding.filtersFlipAppCompatImageView.isSelected = true
            binding.effectsFlipAppCompatImageView.isSelected = false
            binding.backgroundAppCompatImageView.isSelected = false
            newListItems = DeeparDetails.getFilters()

            liveEffectsAdapter.listOfEffects = DeeparDetails.getFilters()
            showEffects()
        }

        binding.effectsFlipAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.maskFlipAppCompatImageView.isSelected = false
            binding.filtersFlipAppCompatImageView.isSelected = false
            binding.effectsFlipAppCompatImageView.isSelected = true
            binding.backgroundAppCompatImageView.isSelected = false
            newListItems = DeeparDetails.getEffects()

            liveEffectsAdapter.listOfEffects = DeeparDetails.getEffects()
            showEffects()
        }

        binding.backgroundAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.maskFlipAppCompatImageView.isSelected = false
            binding.filtersFlipAppCompatImageView.isSelected = false
            binding.effectsFlipAppCompatImageView.isSelected = false
            binding.backgroundAppCompatImageView.isSelected = true
            newListItems = DeeparDetails.getBackground()

            liveEffectsAdapter.listOfEffects = DeeparDetails.getBackground()
            showEffects()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepAR = DeepAR(this)
        deepAR!!.setLicenseKey("24498eaaaa0063ce289681785b8275f1c9ffebdfe0e20f089be95b3c5234d9447d7aa983f37b9adb")
        deepAR!!.initialize(this, this)

        binding = ActivityWatchLiveStreamingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        MeetFriendApplication.component.inject(this)
        watchLiveVideoViewModel = getViewModelFromFactory(viewModelFactory)
        initDeepar()
        mp?.timeEvent(Constant.SCREEN_TIME)

        gestureDetector = GestureDetector(
            this.applicationContext,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    watchLiveVideoViewModel.sendHeart()
                    return super.onDoubleTap(e)
                }
            })


        handler = Handler(Looper.getMainLooper())

        if (intent.hasExtra(LIVE_EVENT_INFO)) {
            liveEventInfo = intent.getParcelableExtra(LIVE_EVENT_INFO) ?: return
            if (liveEventInfo.reopenscreen) {
                updateAsCoHost()
            }
        } else if (intent.hasExtra(NEW_LIVE_EVENT_INFO)) {

            liveEventInfo = intent.getParcelableExtra(NEW_LIVE_EVENT_INFO) ?: return
        }
        watchLiveVideoViewModel.updateLiveEventInfo(liveEventInfo)

        updateUserInfo()
        listenToViewModel()
        listenToViewEvent()

    }

    private fun updateUserInfo() {

        binding.apply {

            Glide.with(this@WatchLiveStreamingActivity)
                .load(liveEventInfo.profilePhoto ?: "")
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .into(userProfileImageView)


            nameTextView.text = liveEventInfo.firstName?.plus(" ").plus(liveEventInfo.lastName)
            userNameTextView.text = liveEventInfo.chatUserName

            binding.ivVerified.isVisible = liveEventInfo.isVerified == 1

            val followersCount = SpannableStringBuilder()
                .append(liveEventInfo.noOfFollowers?.prettyCount().toString())
                .append(" ")
                .append(resources.getString(R.string.followers))


            followersCountAppCompatTextView.text = followersCount

            binding.flFollowContainer.isVisible =
                liveEventInfo.userId != loggedInUserCache.getLoggedInUserId()

            if (liveEventInfo.followStatus == 0 && liveEventInfo.followingStatus == 0) {
                tvFollow.visibility = View.VISIBLE
                tvFollowing.visibility = View.GONE
                tvFollowBack.visibility = View.GONE
                tvRequested.visibility = View.GONE

            } else if (liveEventInfo.followStatus == 0 && liveEventInfo.followingStatus == 1) {
                tvFollowBack.visibility = View.VISIBLE
                tvFollow.visibility = View.GONE
                tvFollowing.visibility = View.GONE
                tvRequested.visibility = View.GONE

            } else if (liveEventInfo.followStatus == 1 && liveEventInfo.followingStatus == 0) {
                tvFollow.visibility = View.GONE
                tvFollowing.visibility = View.VISIBLE
                tvFollowBack.visibility = View.GONE
                tvRequested.visibility = View.GONE


            } else {
                tvFollow.visibility = View.GONE
                tvFollowing.visibility = View.VISIBLE
                tvFollowBack.visibility = View.GONE
                tvRequested.visibility = View.GONE
            }

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listenToViewModel() =
        watchLiveVideoViewModel.watchLiveVideoListStates.subscribeAndObserveOnMainThread {
            when (it) {
                is WatchLiveVideoListState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                  onBackPressed()
                }

                is WatchLiveVideoListState.SuccessMessage -> {}
                is WatchLiveVideoListState.SuccessJoinCoHostMessage -> {
                    updateAsCoHost()
                }

                is WatchLiveVideoListState.UpdateComment -> {
                    if (liveEventInfo.isAllowPlayGame == 1) {
                        if (isCoHost) {
                            if (it.sendOrReadComment.toUserId == loggedInUserCache.getLoggedInUserId()
                                    .toString() || it.sendOrReadComment.userId == loggedInUserCache.getLoggedInUserId()
                            )
                                displayComment(it.sendOrReadComment)
                        } else {
                            if ((liveEventInfo.userId == it.sendOrReadComment.userId) || it.sendOrReadComment.userId == loggedInUserCache.getLoggedInUserId() || it.sendOrReadComment.toUserId == liveEventInfo.userId.toString()) {
                                displayComment(it.sendOrReadComment)
                            }
                        }

                    } else {
                        displayComment(it.sendOrReadComment)
                    }

                }
                is WatchLiveVideoListState.InviteCoHostNotification -> {}
                is WatchLiveVideoListState.JoinEventTokenInfo -> {
                    this.isCoHost = it.isCoHost
                    var coHostLimit = if (liveEventInfo.isAllowPlayGame == 1) {
                        2
                    } else {
                        4
                    }
                    isCoHost = if ((it.response.totalCohost ?: 0) < coHostLimit) {
                        isCoHost
                    } else {
                        false
                    }

                    if (isCoHost) {
                        binding.flipCameraAppCompatImageView.visibility = View.VISIBLE
                        binding.flMuteUnMute.visibility = View.VISIBLE
                        binding.giftAppCompatImageView.isVisible = false
                        binding.tulipBoxImageAppCompatImageView.isVisible = false
                        updateCommentsContainerParam(130)
                        binding.flSpeakerOnOff.visibility = View.GONE

                    } else {
                        binding.flipCameraAppCompatImageView.visibility = View.GONE
                        binding.flMuteUnMute.visibility = View.GONE
                        binding.flSpeakerOnOff.visibility = View.VISIBLE
                        binding.giftAppCompatImageView.isVisible = true

                    }
                    joinChannel(it.liveEventInfo.channelId, it.liveEventInfo.token, isCoHost)
                    if (acceptClick) {
                        if ((it.response.totalCohost ?: 0) < coHostLimit) {

                            if (liveEventInfo.hostStatus ?: 0 < 1) {
                                liveEventInfo.hostStatus = 1
                                liveEventInfo.reopenscreen = true
                                liveEventInfo.isCoHost = 1
                            }
                            updateAsCoHost()

                        }
                    }
                }

                is WatchLiveVideoListState.LiveWatchingCount -> {
                    val liveWatchingCount =
                        if (it.liveWatchingCount < 0) 0 else it.liveWatchingCount
                    binding.tagTextView.text =
                        liveWatchingCount.prettyCount().toString()
                }

                is WatchLiveVideoListState.LoadingState -> {

                }

                is WatchLiveVideoListState.LiveEventEnd -> {
                    if (liveEventInfo.channelId == it.endGameResponse.roomId) {
                      onBackPressed()
                    }
                }

                is WatchLiveVideoListState.KickUserComment -> {
                    if (it.liveEventKickUser.userId == loggedInUserCache.getLoggedInUserId()) {
                        isKickUser = 1
                        //showLongToast(getString(R.string.msg_kick_user))
                      onBackPressed()
                    }
                }

                is WatchLiveVideoListState.LiveHeart -> {
                    binding.likeTextView.text = it.sendHeartSocketEvent.heartCounter.toString()
                    binding.likeImageView.isSelected = !binding.likeImageView.isSelected
                    if (getThumbModel != null)
                        binding.heartsView.emitHeart(getThumbModel)
                }

                is WatchLiveVideoListState.KickedOutUser -> {
                    if (it.liveEventKickUser.userId == loggedInUserCache.getLoggedInUserId()) {
                      onBackPressed()
                    }
                }

                is WatchLiveVideoListState.RestrictUser -> {
                    if (it.liveEventKickUser.userId == loggedInUserCache.getLoggedInUserId()) {
                        binding.editPart.isVisible = false
                        binding.tvRestrictWriting.isVisible = true
                        hideKeyboard()
                    }
                }

                is WatchLiveVideoListState.JoinCoHost -> {
                    listOfCoHostInfo = it.coHostSocketEvent
                    it.coHostSocketEvent.hosts?.forEach {
                        if (it.userId != loggedInUserCache.getLoggedInUserId()) {
                            it.userId?.let { userId ->
                                watchLiveVideoViewModel.checkFollowCoHost(
                                    userId.toInt()
                                )
                            }
                        }
                    }

                    setCoHostName()

                    if ((it.coHostSocketEvent.hosts?.size ?: 0) > 1) {

                        var coHostId: Int? = null
                        var coHostUName: String? = null
                        var coHostProfilePhoto: String? = null
                        it.coHostSocketEvent.hosts?.forEach {
                            if (it.isHost == false) {
                                coHostId = it.userId
                                coHostUName = it.name
                                coHostProfilePhoto = it.profilephoto
                            }
                        }


                        it.coHostSocketEvent.hosts?.forEach {
                            if (isCoHost) {
                                if (it.userId == liveEventInfo.userId) {
                                    binding.tvUser2UserName.text = liveEventInfo.userName
                                    Glide.with(this)
                                        .load(liveEventInfo.profilePhoto)
                                        .error(R.drawable.ic_empty_profile_placeholder)
                                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                                        .into(binding.ivUser2ProfileImage)
                                } else {
                                    binding.tvUser1UserName.text = it.name
                                    Glide.with(this)
                                        .load(it.profilephoto)
                                        .error(R.drawable.ic_empty_profile_placeholder)
                                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                                        .into(binding.ivUser1UserImage)
                                }

                            } else {
                                handler?.postDelayed({
                                    if (it.isHost == true) {
                                        liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
                                            if (key == coHostId) {
                                                if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                                                    binding.tvUser1UserName.text = coHostUName
                                                    Glide.with(this)
                                                        .load(coHostProfilePhoto)
                                                        .error(R.drawable.ic_empty_profile_placeholder)
                                                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                                                        .into(binding.ivUser1UserImage)

                                                    binding.tvUser2UserName.text = it.name
                                                    Glide.with(this)
                                                        .load(it.profilephoto)
                                                        .error(R.drawable.ic_empty_profile_placeholder)
                                                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                                                        .into(binding.ivUser2ProfileImage)
                                                    return@postDelayed
                                                } else if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                                                    binding.tvUser1UserName.text = it.name
                                                    Glide.with(this)
                                                        .load(it.profilephoto)
                                                        .error(R.drawable.ic_empty_profile_placeholder)
                                                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                                                        .into(binding.ivUser1UserImage)

                                                    binding.tvUser2UserName.text = coHostUName
                                                    Glide.with(this)
                                                        .load(coHostProfilePhoto)
                                                        .error(R.drawable.ic_empty_profile_placeholder)
                                                        .placeholder(R.drawable.ic_empty_profile_placeholder)
                                                        .into(binding.ivUser2ProfileImage)
                                                    return@postDelayed
                                                }
                                            }
                                        }

                                    }

                                }, 1000)
                            }
                        }
                    }

                }

                is WatchLiveVideoListState.GameStarted -> {
                    binding.winProgressIndicator.progress = 50
                    if (it.startGameInfo.liveId == liveEventInfo.id) {
                        isGameStarted = true
                        manageStartGameAnimation()
                    }

                }

                is WatchLiveVideoListState.GameEnded -> {
                    if (it.endGameInfo.liveId == liveEventInfo.id) {
                        isGameStarted = false
                        manageEndGame(it.endGameInfo)
                        manageEndGameWinCount(it.endGameInfo)
                    }

                }

                is WatchLiveVideoListState.TopGifterInfo -> {
                    isInitialEmit += 1

                    if (it.liveEventKickUser.gameStarted == 1) {
                        if (!binding.winProgressIndicator.isVisible) {
                            binding.winProgressIndicator.isVisible = true
                            binding.llUser1WinContainer.isVisible = true
                            binding.llUser2WinContainer.isVisible = true
                            binding.tvGiftCountUser1.isVisible = true
                            binding.tvGiftCountUser2.isVisible = true
                        }
                        isGameStarted = true
                    } else {
                        isGameStarted = false
                        binding.winProgressIndicator.isVisible = false
                        binding.tvGiftCountUser1.isVisible = false
                        binding.tvGiftCountUser2.isVisible = false
                        binding.tvGameTimer.isVisible = false
                    }
                    if (it.liveEventKickUser.progress.isNullOrEmpty()) {

                    } else {
                        if (isCoHost) {
                            it.liveEventKickUser.progress?.forEach {
                                if (it.userId == liveEventInfo.userId) {
                                    binding.tvGiftCountUser2.text = it.totalCoins.toString()
                                    binding.tvUser2WinCount.text = it.totalWinCount.toString()
                                } else {
                                    binding.tvGiftCountUser1.text = it.totalCoins.toString()
                                    binding.tvUser1WinCount.text = it.totalWinCount.toString()
                                }
                            }

                            user1ContributorList =
                                it.liveEventKickUser.topgifter?.filter { it.toId == loggedInUserCache.getLoggedInUserId() }
                            user2ContributorList =
                                it.liveEventKickUser.topgifter?.filter { it.toId != loggedInUserCache.getLoggedInUserId() }
                        } else {
                            topGifterInfo = it.liveEventKickUser
                            if (isInitialEmit > 1)
                                manageGameGiftCount(it.liveEventKickUser)
                        }

                    }
                    user1ContributorList?.forEach {
                        it.index = user1ContributorList?.indexOf(it) ?: 0
                    }

                    user2ContributorList?.forEach {
                        it.index = user2ContributorList?.indexOf(it) ?: 0
                    }
                    manageProgressOfGame()
                    if (isGameStarted) {
                        manageUser1ContributorImage()
                        manageUser2ContributorImage()
                    }


                }

                is WatchLiveVideoListState.ReceiveGift -> {
                    if (isCoHost) {
                        if (it.receiveGiftInfo.receiver?.id == liveEventInfo.userId) {
                            displayReceiveGiftInfo(it.receiveGiftInfo, false)
                        } else {
                            displayReceiveGiftInfo(it.receiveGiftInfo, true)
                        }
                    } else {

                        liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
                            if (key == it.receiveGiftInfo.receiver?.id) {
                                if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                                    displayReceiveGiftInfo(it.receiveGiftInfo, true)
                                } else if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                                    displayReceiveGiftInfo(it.receiveGiftInfo, false)
                                }
                            }
                        }

                    }


                }

                is WatchLiveVideoListState.PlayGameRequestInfo -> {
                    if (isTransactionSafe) {
                        openGameRequestDialog()
                    }
                }

                is WatchLiveVideoListState.RemovedCohostInfo -> {
                  onBackPressed()
                }

                is WatchLiveVideoListState.SendTulipGiftSuccess -> {
                    sendGift(29, 1)
                    watchLiveVideoViewModel.getMyEarningInfo()

                }

                is WatchLiveVideoListState.MyEarningData -> {
                    totalCoins = it.myEarningInfo?.totalCurrentCoins ?: 0.0
                }

                is WatchLiveVideoListState.CoHostFollowData -> {
                    it.coHostFollowInfo?.let { info -> manageCoHostFollowOption(info) }
                }

                is WatchLiveVideoListState.FollowSuccess -> {
                    showToast(it.message)
                }

                is WatchLiveVideoListState.CoinCentsData -> {
                    centsValue = it.coinCentsInfo?.value
                }
            }
        }.autoDispose()

    private fun displayReceiveGiftInfo(receiveGiftInfo: ReceiveGiftInfo, user1: Boolean) {
        var container = binding.llUser1ReceiveGiftInfo
        var senderImage = binding.ivUser1GifterImage
        var senderName = binding.tvUser1GifterName
        var giftName = binding.tvUser1GiftName
        var giftImage = binding.ivUser1Gift
        var giftQuantity = binding.tvUser1ComboCount
        var verifiedImage = binding.ivVerifiedUser1Gifter

        if (!user1) {
            container = binding.llUser2ReceiveGiftInfo
            senderImage = binding.ivUser2GifterImage
            senderName = binding.tvUser2GifterName
            giftName = binding.tvUser2GiftName
            giftImage = binding.ivUser2Gift
            giftQuantity = binding.tvUser2ComboCount
            verifiedImage = binding.ivVerifiedUser2Gifter
        }
        val anim: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        container.isVisible = true
        container.startAnimation(anim)

        Glide.with(this)
            .load(receiveGiftInfo.sender?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(senderImage)

        senderName.text = receiveGiftInfo.sender?.userName
        verifiedImage.isVisible = receiveGiftInfo.sender?.isVerified == 1
        giftName.text =
            resources.getString(R.string.label_sent).plus(" ").plus(receiveGiftInfo.gift?.name)

        Glide.with(this)
            .load(receiveGiftInfo.gift?.file_path)
            .placeholder(R.drawable.place_holder_image)
            .error(R.drawable.place_holder_image)
            .into(giftImage)

        giftQuantity.text = resources.getString(R.string.label_x)
            .plus(
                if ((receiveGiftInfo.quantity ?: 0) > 0) receiveGiftInfo.quantity.toString() else 1
            )

        if(receiveGiftInfo.isCombo == 0) {
            handler?.postDelayed({ container.visibility = View.GONE }, 4000)
        } else {
            handler?.postDelayed({ container.visibility = View.GONE }, 10000)
        }

    }

    private fun manageUser1ContributorImage() {
        if (!user1ContributorList.isNullOrEmpty()) {
            when (user1ContributorList?.size) {
                0 -> {
                    binding.llUser1ContributorImage.isVisible = false
                }

                1 -> {
                    binding.llUser1ContributorImage.isVisible = true
                    user1ContributorList?.let {
                        setProfileImage(
                            binding.ivTopGifter1User1Image,
                            it[0].profilePhoto
                        )
                    }
                    binding.ivTopGifter2User1Image.isVisible = false
                    binding.ivTopGifter3user1Image.isVisible = false

                }

                2 -> {
                    binding.llUser1ContributorImage.isVisible = true
                    binding.ivTopGifter2User1Image.isVisible = true
                    binding.ivTopGifter3user1Image.isVisible = true
                    user1ContributorList?.let {
                        setProfileImage(
                            binding.ivTopGifter1User1Image,
                            it[0].profilePhoto
                        )
                        setProfileImage(
                            binding.ivTopGifter2User1Image,
                            it[1].profilePhoto
                        )
                    }
                    binding.ivTopGifter3user1Image.isVisible = false

                }

                else -> {
                    binding.llUser1ContributorImage.isVisible = true
                    binding.ivTopGifter2User1Image.isVisible = true
                    binding.ivTopGifter3user1Image.isVisible = true
                    user1ContributorList?.let {
                        setProfileImage(
                            binding.ivTopGifter1User1Image,
                            it[0].profilePhoto
                        )
                        setProfileImage(
                            binding.ivTopGifter2User1Image,
                            it[1].profilePhoto
                        )
                        setProfileImage(
                            binding.ivTopGifter3user1Image,
                            it[2].profilePhoto
                        )
                    }

                }
            }

        }
    }

    private fun manageUser2ContributorImage() {
        if (!user2ContributorList.isNullOrEmpty()) {
            when (user2ContributorList?.size) {
                0 -> {
                    binding.flUser2ContributorImage.isVisible = false
                }

                1 -> {
                    binding.flUser2ContributorImage.isVisible = true
                    user2ContributorList?.let {
                        setProfileImage(
                            binding.ivTopGifter1user2Image,
                            it[0].profilePhoto
                        )
                    }

                    binding.ivTopGifter2user2Image.isVisible = false
                    binding.ivTopGifter3user2Image.isVisible = false

                }

                2 -> {
                    binding.flUser2ContributorImage.isVisible = true
                    binding.ivTopGifter2user2Image.isVisible = true
                    binding.ivTopGifter3user2Image.isVisible = true

                    user2ContributorList?.let {
                        setProfileImage(
                            binding.ivTopGifter1user2Image,
                            it[0].profilePhoto
                        )
                        setProfileImage(
                            binding.ivTopGifter2user2Image,
                            it[1].profilePhoto
                        )
                    }

                    binding.ivTopGifter3user2Image.isVisible = false

                }

                else -> {
                    binding.flUser2ContributorImage.isVisible = true
                    binding.ivTopGifter2user2Image.isVisible = true
                    binding.ivTopGifter3user2Image.isVisible = true

                    user2ContributorList?.let {
                        setProfileImage(
                            binding.ivTopGifter1user2Image,
                            it[0].profilePhoto
                        )
                        setProfileImage(
                            binding.ivTopGifter2user2Image,
                            it[1].profilePhoto
                        )
                        setProfileImage(
                            binding.ivTopGifter3user2Image,
                            it[2].profilePhoto
                        )
                    }
                }
            }

        }
    }

    fun setProfileImage(imageView: RoundedImageView, path: String?) {
        if (path == null && equals("")) {
            return
        }
        Glide.with(this)
            .load(path)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(imageView)
    }

    private fun listenToViewEvent() {
        watchLiveVideoViewModel.getCoinInCents()

        binding.rlHeader.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.rlHeader.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        binding.firstCoHostRelativeLayout.setOnTouchListener { _, p1 ->
            gestureDetector.onTouchEvent(p1)
            true
        }

        binding.rlHeader.viewTreeObserver.addOnGlobalLayoutListener {

            val view: View = findViewById(R.id.rlHeader)
            val rootViewHeight = view.rootView.height
            val location = IntArray(2)
            binding.llFooter.getLocationOnScreen(location)
            val height = (location[1] + binding.llFooter.measuredHeight) as Int
            var deff = rootViewHeight - height
            if (deff > 500) {
                binding.settingsAppCompatImageView.isVisible = true
                if (!isCoHost) {
                    binding.tulipBoxImageAppCompatImageView.isVisible = true
                    binding.giftAppCompatImageView.isVisible = true
                }


                if (isCoHost) {
                    binding.flipCameraAppCompatImageView.isVisible = true
                    binding.flMuteUnMute.isVisible = true
                    binding.effectsFlipAppCompatImageView.isVisible = true
                    binding.maskFlipAppCompatImageView.isVisible = true
                    binding.filtersFlipAppCompatImageView.isVisible = true
                    binding.backgroundAppCompatImageView.isVisible = true

                    updateCommentsContainerParam(130)

                } else {
                    binding.flSpeakerOnOff.isVisible = true
                }
            }
        }

        if (!isCoHost) {
            binding.giftAppCompatImageView.isVisible = true
            binding.tulipBoxImageAppCompatImageView.isVisible = true
        }

        if (liveEventInfo.isRestrict == 1) {
            binding.editPart.isVisible = false
            binding.tvRestrictWriting.isVisible = true
        }

        RxBus.listen(RxEvent.UpdateLiveCoHost::class.java).subscribeAndObserveOnMainThread {
            if (isTransactionSafe) {
                if (it.liveEventInfo.id == liveEventInfo.id) {
                    openUpdateCoHostStatusDialog(true)
                } else {
                    openLeaveCurrentLiveDialog(it.liveEventInfo)
                }
            }

        }.autoDispose()

        liveStreamingCommentAdapter = LiveStreamingCommentAdapter(this).apply {
            commentClicks.subscribeAndObserveOnMainThread {
                if (it.userId != loggedInUserCache.getLoggedInUserId()) {
                    if (it.type != MessageType.JoinF.toString()) {
                        if (isCoHost) {
                            if (it.userId != liveEventInfo.userId && it.userId != firstCoHostUid && it.userId != secondCoHostUid && it.userId != thirdCoHostUid && it.userId != fourthCoHostUid) {
                                openLiveUserBottomSheet(it.userId, liveEventInfo.id)
                            } else {
                                startActivity(
                                    MyProfileActivity.getIntentWithData(
                                        this@WatchLiveStreamingActivity,
                                        it.userId
                                    )
                                )
                            }
                        } else {
                            startActivity(
                                MyProfileActivity.getIntentWithData(
                                    this@WatchLiveStreamingActivity,
                                    it.userId ?: 0
                                )
                            )

                        }
                    }
                }
            }
        }
        binding.commentRecyclerView.apply {
            val linearLayoutManager =
                LinearLayoutManager(this@WatchLiveStreamingActivity, RecyclerView.VERTICAL, false)
            linearLayoutManager.stackFromEnd = true
            layoutManager = linearLayoutManager
            adapter = liveStreamingCommentAdapter
        }

        binding.exitAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressed()
        }.autoDispose()

        binding.sendImageView.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            if (binding.commentEditTextView.text.toString().isNotEmpty()) {
                watchLiveVideoViewModel.sendComment(
                    binding.commentEditTextView.text.toString(),
                    if (isCoHost) loggedInUserCache.getLoggedInUserId()
                        .toString() else liveEventInfo.userId.toString()
                )

                val props = JSONObject()
                props.put(Constant.CONTENT_TYPE, "live")
                props.put(Constant.CONTENT_ID, liveEventInfo.id)

                mp?.track(Constant.COMMENT_CONTENT, props)
            }
            binding.commentEditTextView.setText("")
        }.autoDispose()

        binding.flipCameraAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (isCoHost) {
                lensFacing =
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                //unbind immediately to avoid mirrored frame.
                var cameraProvider: ProcessCameraProvider? = null
                try {
                    cameraProvider = cameraProviderFuture?.get()
                    cameraProvider?.unbindAll()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                setupCamera()
            }
        }.autoDispose()

        binding.settingsAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            openMoreOptionBottomSheet()
        }.autoDispose()

        engine =
            RtcEngine.create(this, getString(R.string.new_agora_app_id), iRtcEngineEventHandler)

        if (liveEventInfo.isCoHost == 1) {
            when (liveEventInfo.hostStatus) {
                1 -> {
                    watchLiveVideoViewModel.joinLiveEvent(
                        isCoHost = true,
                        isFromNotification = false,
                        "",
                        liveEventInfo
                    )
                }

                2 -> {
                    watchLiveVideoViewModel.joinLiveEvent(
                        isCoHost = false,
                        isFromNotification = false,
                        "",
                        liveEventInfo
                    )
                }

                else -> {
                    //Show popup and based on user action request for token and join
                    openUpdateCoHostStatusDialog(false)
                }
            }
        } else {
            watchLiveVideoViewModel.joinLiveEvent(
                isCoHost = false,
                isFromNotification = false,
                "",
                liveEventInfo
            )
        }

        val followersCount = SpannableStringBuilder()
            .append(liveEventInfo.noOfFollowers?.prettyCount().toString())
            .append(" ")
            .append(resources.getString(R.string.followers))


        binding.followersCountAppCompatTextView.text = followersCount

        binding.userProfileImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (loggedInUserCache.getLoggedInUserId() != liveEventInfo.userId) {
                startActivity(MyProfileActivity.getIntentWithData(this, liveEventInfo.userId))
            }
        }.autoDispose()

        binding.userNameTextView.throttleClicks().subscribeAndObserveOnMainThread {
            if (loggedInUserCache.getLoggedInUserId() != liveEventInfo.userId) {
                startActivity(MyProfileActivity.getIntentWithData(this, liveEventInfo.userId))
            }
        }.autoDispose()
        binding.commentEditTextView.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus ->
                binding.settingsAppCompatImageView.isVisible = !hasFocus
                binding.flipCameraAppCompatImageView.isVisible = !hasFocus
                binding.tulipBoxImageAppCompatImageView.isVisible = !hasFocus
                binding.giftAppCompatImageView.isVisible = !hasFocus
                binding.flMuteUnMute.isVisible = !hasFocus
                binding.flSpeakerOnOff.isVisible = !hasFocus

            }

        KeyboardVisibilityEvent.setEventListener(
            this
        ) {
            binding.settingsAppCompatImageView.isVisible = !it
            if (!isCoHost) {
                binding.tulipBoxImageAppCompatImageView.isVisible = !it
                binding.giftAppCompatImageView.isVisible = !it
            }


            if (isCoHost) {
                binding.flipCameraAppCompatImageView.isVisible = !it
                binding.flMuteUnMute.isVisible = !it
                binding.effectsFlipAppCompatImageView.isVisible = !it
                binding.maskFlipAppCompatImageView.isVisible = !it
                binding.filtersFlipAppCompatImageView.isVisible = !it
                binding.backgroundAppCompatImageView.isVisible = !it
                if (it) {
                    updateCommentsContainerParam(60)
                } else {
                    updateCommentsContainerParam(130)
                }
            } else {
                binding.flSpeakerOnOff.isVisible = !it
            }
        }
        binding.userNameTextView.throttleClicks().subscribeAndObserveOnMainThread {
            if (!isCoHost) {
            }
        }.autoDispose()


        binding.tvFollow.throttleClicks().subscribeAndObserveOnMainThread {
            watchLiveVideoViewModel.followUnfollow(liveEventInfo.userId)
            binding.tvFollow.isVisible = false

            if (liveEventInfo.isPrivate == 0) {
                binding.tvFollowing.isVisible = true
            } else {
                binding.tvRequested.isVisible = true
            }
        }.autoDispose()

        binding.tvFollowing.throttleClicks().subscribeAndObserveOnMainThread {
            watchLiveVideoViewModel.followUnfollow(liveEventInfo.userId)
            binding.tvFollowing.isVisible = false

            if (liveEventInfo.followingStatus == 1) {
                binding.tvFollowBack.isVisible = true
            } else {
                binding.tvFollow.isVisible = true
            }
        }.autoDispose()

        binding.tvFollowBack.throttleClicks().subscribeAndObserveOnMainThread {
            watchLiveVideoViewModel.followUnfollow(liveEventInfo.userId)
            binding.tvFollowBack.isVisible = false

            if (liveEventInfo.isPrivate == 0) {
                binding.tvFollowing.isVisible = true
            } else {
                binding.tvRequested.isVisible = true
            }

        }.autoDispose()

        binding.muteAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            engine?.muteLocalAudioStream(false)
            binding.muteAppCompatImageView.isVisible = false
            binding.unMuteAppCompatImageView.isVisible = true
        }.autoDispose()

        binding.unMuteAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            engine?.muteLocalAudioStream(true)
            binding.unMuteAppCompatImageView.isVisible = false
            binding.muteAppCompatImageView.isVisible = true
        }.autoDispose()



        binding.speakerOnAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            engine?.muteAllRemoteAudioStreams(true)
            binding.speakerOnAppCompatImageView.isVisible = false
            binding.speakerOffAppCompatImageView.isVisible = true
        }.autoDispose()

        binding.speakerOffAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            engine?.muteAllRemoteAudioStreams(false)
            binding.speakerOffAppCompatImageView.isVisible = false
            binding.speakerOnAppCompatImageView.isVisible = true
        }.autoDispose()

        binding.giftAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val bottomSheet = GiftGalleryBottomSheet.newInstance(
                liveEventInfo.userId.toString(), liveEventInfo.id.toString(),
                isSend = true,
                isFromLive = true,
                isFromWhere = GiftGalleryBottomSheet.INTENT_IS_FROM_WATCH_LIVE
            )
            bottomSheet.giftResponseState.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is GiftItemClickStates.SendGiftInGameClick -> {
                        val props = JSONObject()
                        props.put(Constant.CONTENT_TYPE, "live")
                        props.put(Constant.CONTENT_ID, liveEventInfo.id)

                        mp?.track(Constant.SEND_GIFT, props)
                        sendGift(state.data.id, state.data.quantity)
                    }

                    else -> {

                    }
                }
            }.autoDispose()
            bottomSheet.show(supportFragmentManager, GiftGalleryBottomSheet::class.java.name)
        }.autoDispose()

        binding.tulipBoxImageAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (totalCoins >= 1.0) {
                watchLiveVideoViewModel.sendGiftInLive(
                    liveEventInfo.userId.toString(), liveEventInfo.id.toString(),
                    1.0, 29, 1
                )
            } else {
                showToast("You don't have enough coins to send gift.")
                startActivity(CoinPlansActivity.getIntent(this))
            }

        }.autoDispose()

        binding.llUser1ContributorImage.throttleClicks().subscribeAndObserveOnMainThread {
            val bottomSheet =
                GameTopContributorBottomSheet.newInstance(user1ContributorList as ArrayList<TopGifter>)
            bottomSheet.show(supportFragmentManager, GameTopContributorBottomSheet::class.java.name)
        }.autoDispose()

        binding.flUser2ContributorImage.throttleClicks().subscribeAndObserveOnMainThread {
            val bottomSheet =
                GameTopContributorBottomSheet.newInstance(user2ContributorList as ArrayList<TopGifter>)
            bottomSheet.show(supportFragmentManager, GameTopContributorBottomSheet::class.java.name)
        }.autoDispose()

        binding.ivFirstFollow.throttleClicks().subscribeAndObserveOnMainThread {
            val firstCoHostId =
                liveStreamNoOfCoHostHashMap.entries.find { it.value == LiveStreamNoOfCoHost.FirstCoHost.toString() }?.key
            firstCoHostId?.let { it1 -> watchLiveVideoViewModel.followUnfollow(it1) }
            binding.ivFirstFollow.isVisible = false
        }.autoDispose()

        binding.ivSecondFollow.throttleClicks().subscribeAndObserveOnMainThread {
            watchLiveVideoViewModel.followUnfollow(secondCoHostUid)
            binding.ivSecondFollow.isVisible = false

        }.autoDispose()

        binding.ivThirdFollow.throttleClicks().subscribeAndObserveOnMainThread {
            watchLiveVideoViewModel.followUnfollow(thirdCoHostUid)
            binding.ivThirdFollow.isVisible = false

        }.autoDispose()

        binding.ivFourthFollow.throttleClicks().subscribeAndObserveOnMainThread {
            watchLiveVideoViewModel.followUnfollow(fourthCoHostUid)
            binding.ivFourthFollow.isVisible = false

        }.autoDispose()

    }

    private fun sendGift(id: Int, quantity: Int?) {
        watchLiveVideoViewModel.sendGiftInGame(
            SendGiftInGameRequest(
                id.toString(),
                loggedInUserCache.getLoggedInUserId().toString(),
                liveEventInfo.userId.toString(),
                liveEventInfo.id.toString(),
                liveEventInfo.channelId,
                quantity,
                liveEventInfo.isAllowPlayGame
            )
        )
    }

    private fun openLiveUserBottomSheet(userId: Int?, liveId: Int?) {
        val bottomSheet = LiveUserOptionBottomSheet()
        bottomSheet.liveUserClicks.subscribeAndObserveOnMainThread { state ->
            when (state) {
                LiveUserOption.KickOut -> {
                    openKickOutDialog(userId, liveId)
                }

                LiveUserOption.Restrict -> {
                    watchLiveVideoViewModel.kickOutUserFromLive(
                        LiveEventKickUser(
                            userId = userId,
                            actionType = Constant.MESSAGE_TYPE_RESTRICT,
                            seconds = 0,
                            roomId = liveId,
                            restrictBy = loggedInUserCache.getLoggedInUserId()

                        )
                    )
                }

                LiveUserOption.ViewProfile -> {
                    startActivity(userId?.let {
                        MyProfileActivity.getIntentWithData(
                            this,
                            it
                        )
                    })
                }
            }

        }.autoDispose()
        bottomSheet.show(supportFragmentManager, LiveUserOptionBottomSheet::class.java.name)
    }

    private fun openKickOutDialog(userId: Int?, liveId: Int?) {
        val kickOutDialogFragment = KickOutDialogFragment.newInstance(null, null, true)

        kickOutDialogFragment.continueForLiveClicks.subscribeAndObserveOnMainThread {
            watchLiveVideoViewModel.kickOutUserFromLive(
                LiveEventKickUser(
                    userId = userId,
                    actionType = Constant.MESSAGE_TYPE_KICK_OUT,
                    seconds = it,
                    roomId = liveId
                )
            )
        }
        kickOutDialogFragment.show(supportFragmentManager, ViewUserBottomSheet::class.java.name)
    }

    private fun displayComment(sendOrReadComment: LiveEventSendOrReadComment) {
        if (sendOrReadComment.liveId == liveEventInfo.id) {

            listOfLiveEventSendOrReadComment.add(sendOrReadComment)
            liveStreamingCommentAdapter.listOfDataItems = listOfLiveEventSendOrReadComment
            liveStreamingCommentAdapter.notifyDataSetChanged()

            binding.commentRecyclerView.scrollToPosition(
                listOfLiveEventSendOrReadComment.size - 1
            )
        }

    }

    private fun openMoreOptionBottomSheet() {
        val bottomSheet = LiveStreamingMoreOptionBottomSheet.newInstance(
            isFromShorts = false,
            isFromLive = true,
            false
        )
        bottomSheet.optionClick.subscribeAndObserveOnMainThread {
            when (it) {
                resources.getString(R.string.label_share) -> {
                    ShareHelper.shareDeepLink(
                        this,
                        3,
                        liveEventInfo.id,
                        "", true
                    ) {
                        ShareHelper.shareText(this, it)

                        val props = JSONObject()
                        props.put(Constant.CONTENT_TYPE, "live")
                        props.put(Constant.CONTENT_ID, liveEventInfo.id)

                        mp?.track(Constant.SHARE_CONTENT, props)
                    }
                }

                resources.getString(R.string.report) -> {
                    val reportDialog =
                        ReportDialogFragment.newInstance(liveEventInfo.id, true, LIVE)
                    reportDialog.optionClick.subscribeAndObserveOnMainThread {
                        val props = JSONObject()
                        props.put(Constant.CONTENT_TYPE, "shorts")
                        props.put(Constant.CONTENT_ID, liveEventInfo.id)

                        mp?.track(Constant.REPORT_CONTENT, props)
                    }
                    reportDialog.show(supportFragmentManager, ReportDialogFragment::class.java.name)
                }
            }
        }.autoDispose()

        bottomSheet.show(
            supportFragmentManager,
            LiveStreamingMoreOptionBottomSheet::class.java.name
        )
    }

    private fun joinChannel(
        channelId: String,
        accessToken: String,
        isCoHost: Boolean
    ) {

        channelAlreadyJoined = true
        engine?.setDefaultAudioRoutetoSpeakerphone(true)
        engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)

        if (isCoHost) {
            val surfaceView = RtcEngine.CreateRendererView(this)
            engine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            // Enable video module
            engine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            engine?.setDefaultAudioRoutetoSpeakerphone(true)
            engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
            engine?.enableVideo()
            engine?.enableAudio()
            // Setup video encoding configs
            engine?.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )
        } else {
            engine?.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
            // Enable video module
            engine?.enableVideo()
            engine?.enableAudio()
        }
        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
        option.autoSubscribeVideo = true
        val res =
            engine?.joinChannel(
                accessToken,
                channelId,
                option.toString(),
                loggedInUserCache.getLoggedInUserId()
            ) ?: return
        if (res != 0) {
            return
        }
    }

    private fun welcomeMessage() {
        runOnUiThread {
            val liveEventInfo = LiveEventSendOrReadComment(
                roomId = null,
                liveId = null,
                userId = null,
                id = 0.toString(),
                name = null,
                username = getString(R.string.label_meeturfriend_live),
                comment = getString(R.string.desc_live_welcome_message),
                type = MessageType.JoinF.toString()
            )

            listOfLiveEventSendOrReadComment.add(0, liveEventInfo)

            if (!this.liveEventInfo.eventName.isNullOrEmpty()) {
                val liveEventDescriptionInfo = LiveEventSendOrReadComment(
                    roomId = null,
                    liveId = null,
                    userId = null,
                    id = liveEventInfo.userId.toString(),
                    name = null,
                    username = this.liveEventInfo.chatUserName,
                    comment = this.liveEventInfo.eventName.toString(),
                    type = MessageType.Text.toString(),
                    profileUrl = this.liveEventInfo.profilePhoto,
                    isVerified = this.liveEventInfo.isVerified
                )
                listOfLiveEventSendOrReadComment.add(1, liveEventDescriptionInfo)
            }

            liveStreamingCommentAdapter.listOfDataItems = listOfLiveEventSendOrReadComment
            liveStreamingCommentAdapter.notifyDataSetChanged()

            watchLiveVideoViewModel.sendComment(
                getString(R.string.label_joined_message),
                if (isCoHost) {
                    loggedInUserCache.getLoggedInUserId()
                        .toString()
                } else {
                    liveEventInfo.userId.toString()
                }
            )
            val props = JSONObject()
            props.put(Constant.CONTENT_TYPE, "live")
            props.put(Constant.CONTENT_ID, liveEventInfo.liveId)

            mp?.track(Constant.COMMENT_CONTENT, props)
        }
    }

    private fun updateAsCoHost() {

        if (!liveStreamNoOfCoHostHashMap.containsKey(loggedInUserCache.getLoggedInUserId())) {

            binding.userNameTextView.text =
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
            binding.ivVerified.isVisible =
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified == 1

            val followersCount = SpannableStringBuilder()
                .append(
                    loggedInUserCache.getLoggedInUser()?.loggedInUser?.noOfFollowers?.prettyCount()
                        .toString()
                )
                .append(" ")
                .append(resources.getString(R.string.followers))

            binding.followersCountAppCompatTextView.text = followersCount
            binding.flFollowContainer.isVisible = false

            Glide.with(this@WatchLiveStreamingActivity)
                .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto ?: "")
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .into(binding.userProfileImageView)

            if (liveEventInfo.reopenscreen) {
                setupCamera()
                setup()
            }
            // Create render view by RtcEngine
            isCoHost = true
            firstCoHost = true
            binding.flipCameraAppCompatImageView.visibility = View.VISIBLE
            binding.flMuteUnMute.visibility = View.VISIBLE
            binding.flSpeakerOnOff.visibility = View.GONE
            binding.settingsAppCompatImageView.visibility = View.VISIBLE
            val surfaceView = RtcEngine.CreateRendererView(this)
            if (binding.coHostFirstVideo.childCount > 0) {
                binding.coHostFirstVideo.removeAllViews()
            }
            surfaceView.setZOrderMediaOverlay(true)
            binding.llFirstAndThirdCoHost.isVisible = true
            binding.coHostFirstVideo.visibility = View.VISIBLE

            binding.coHostFirstVideo.addView(
                binding.firstCoHostRelativeLayout,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            binding.firstCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
            if (liveStreamNoOfCoHostHashMap.size > 0) {

                liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
                    if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                        secondCoHostUid = key
                        secondCoHost = true
                        handler?.post {
                            if (binding.coHostSecondVideo.childCount > 0) {
                                binding.coHostSecondVideo.removeAllViews()
                            }
                            // Create render view by RtcEngine
                            val surfaceView =
                                RtcEngine.CreateRendererView(this@WatchLiveStreamingActivity)
                            surfaceView.setZOrderMediaOverlay(true)
                            // Add to the remote container
                            binding.coHostSecondVideo.addView(
                                surfaceView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                            // Setup remote video to render
                            engine?.setupRemoteVideo(
                                VideoCanvas(
                                    surfaceView,
                                    VideoCanvas.RENDER_MODE_HIDDEN,
                                    key
                                )
                            )

                            updateUIForMultipleHost()
                            binding.llSecondAndFourthCoHost.isVisible = true
                            binding.coHostSecondVideo.visibility = View.VISIBLE

                            if (secondCoHostUid != loggedInUserCache.getLoggedInUserId()) {
                                binding.coHostSecondVideo.addView(
                                    binding.secondCoHostRelativeLayout,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                                binding.secondCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
                            }
                        }
                    }

                    if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                        thirdCoHostUid = key
                        thirdCoHost = true
                        handler?.post {
                            if (binding.coHostThirdVideo.childCount > 0) {
                                binding.coHostThirdVideo.removeAllViews()
                            }
                            // Create render view by RtcEngine
                            val surfaceView =
                                RtcEngine.CreateRendererView(this@WatchLiveStreamingActivity)
                            surfaceView.setZOrderMediaOverlay(true)
                            // Add to the remote container
                            binding.coHostThirdVideo.addView(
                                surfaceView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                            // Setup remote video to render
                            engine?.setupRemoteVideo(
                                VideoCanvas(
                                    surfaceView,
                                    VideoCanvas.RENDER_MODE_HIDDEN,
                                    key
                                )
                            )
                            updateUIForMultipleHost()
                            binding.llFirstAndThirdCoHost.isVisible = true
                            binding.coHostThirdVideo.visibility = View.VISIBLE
                            if (thirdCoHostUid != loggedInUserCache.getLoggedInUserId()) {
                                binding.coHostThirdVideo.addView(
                                    binding.thirdCoHostRelativeLayout,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                                binding.thirdCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
                            }

                        }

                    }
                    if (value == LiveStreamNoOfCoHost.ThirdCoHost.type) {
                        fourthCoHostUid = key
                        fourthCoHost = true
                        handler?.post {

                            // Create render view by RtcEngine
                            val surfaceView =
                                RtcEngine.CreateRendererView(this@WatchLiveStreamingActivity)
                            surfaceView.setZOrderMediaOverlay(true)
                            // Add to the remote container
                            if (binding.coHostFourVideo.childCount > 0) {
                                binding.coHostFourVideo.removeAllViews()
                            }
                            binding.coHostFourVideo.addView(
                                surfaceView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                            // Setup remote video to render
                            engine?.setupRemoteVideo(
                                VideoCanvas(
                                    surfaceView,
                                    VideoCanvas.RENDER_MODE_HIDDEN,
                                    key
                                )
                            )

                            updateUIForMultipleHost()

                            binding.llSecondAndFourthCoHost.isVisible = true
                            binding.coHostFourVideo.visibility = View.VISIBLE

                            if (fourthCoHostUid != loggedInUserCache.getLoggedInUserId()) {
                                binding.coHostFourVideo.addView(
                                    binding.fourCoHostRelativeLayout,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                                binding.fourCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
                            }
                        }
                    }
                }
            }

            liveStreamNoOfCoHostHashMap[loggedInUserCache.getLoggedInUserId()] =
                LiveStreamNoOfCoHost.FirstCoHost.type
            if (secondCoHostUid != 0) {
                liveStreamNoOfCoHostHashMap[secondCoHostUid] =
                    LiveStreamNoOfCoHost.SecondCoHost.type
            }
            if (thirdCoHostUid != 0) {
                liveStreamNoOfCoHostHashMap[thirdCoHostUid] = LiveStreamNoOfCoHost.ThirdCoHost.type
            }
            if (fourthCoHostUid != 0) {
                liveStreamNoOfCoHostHashMap[fourthCoHostUid] =
                    LiveStreamNoOfCoHost.FourthCoHost.type
            }
            setCoHostName()


            // Setup local video to render your local camera preview
            engine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            engine?.setDefaultAudioRoutetoSpeakerphone(true)
            engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
            // Enable video module
            engine?.enableVideo()
            engine?.enableAudio()
            // Setup video encoding configs
            engine?.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )

        }

    }

    private fun setFirstCoHostVideo(uid: Int) {
        liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.FirstCoHost.type

        firstCoHost = true
        handler?.post {
            if (binding.coHostFirstVideo.childCount > 0) {
                binding.coHostFirstVideo.removeAllViews()
            }
            // Create render view by RtcEngine
            val surfaceView = RtcEngine.CreateRendererView(this@WatchLiveStreamingActivity)
            surfaceView.setZOrderMediaOverlay(true)
            // Add to the remote container
            binding.coHostFirstVideo.addView(
                surfaceView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            engine?.enableAudio()
            engine?.enableVideo()
            // Setup remote video to render
            engine?.setupRemoteVideo(
                VideoCanvas(
                    surfaceView,
                    VideoCanvas.RENDER_MODE_HIDDEN,
                    uid
                )
            )
            if (liveStreamNoOfCoHostHashMap.size == 1) {
                updateUIForSingleHost()
            } else {
                updateUIForMultipleHost()
            }

            binding.llFirstAndThirdCoHost.isVisible = true
            binding.coHostFirstVideo.visibility = View.VISIBLE

            if (liveEventInfo.isAllowPlayGame == 0 && firstCoHostUid != loggedInUserCache.getLoggedInUserId()) {
                binding.coHostFirstVideo.addView(
                    binding.firstCoHostRelativeLayout,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                binding.firstCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
            } else {
                binding.coHostFirstVideo.addView(
                    binding.firstCoHostRelativeLayout,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                binding.firstCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)

            }
        }
    }

    private fun setSecondCoHostVideo(uid: Int) {
        liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.SecondCoHost.type
        secondCoHost = true
        secondCoHostUid = uid

        handler?.post {
            if (binding.coHostSecondVideo.childCount > 0) {
                binding.coHostSecondVideo.removeAllViews()
            }
            // Create render view by RtcEngine
            val surfaceView = RtcEngine.CreateRendererView(this@WatchLiveStreamingActivity)
            surfaceView.setZOrderMediaOverlay(true)
            // Add to the remote container
            binding.coHostSecondVideo.addView(
                surfaceView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            engine?.enableAudio()
            engine?.enableVideo()
            // Setup remote video to render
            engine?.setupRemoteVideo(
                VideoCanvas(
                    surfaceView,
                    VideoCanvas.RENDER_MODE_HIDDEN,
                    uid
                )
            )
            updateUIForMultipleHost()
            binding.llSecondAndFourthCoHost.isVisible = true
            binding.coHostSecondVideo.visibility = View.VISIBLE

            if (secondCoHostUid != loggedInUserCache.getLoggedInUserId()) {
                binding.coHostSecondVideo.addView(
                    binding.secondCoHostRelativeLayout,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                binding.secondCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
            }
        }

        handler?.postDelayed({
            if (listOfCoHostId.size > 1) {
                if (isInitialEmit > 0) {
                    topGifterInfo?.let {
                        manageGameGiftCount(it)
                    }
                }
            }
        }, 3000)
    }

    private fun setThirdCoHostVideo(uid: Int) {
        liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.ThirdCoHost.type
        thirdCoHost = true
        thirdCoHostUid = uid
        handler?.post {
            if (binding.coHostThirdVideo.childCount > 0) {
                binding.coHostThirdVideo.removeAllViews()
            }
            // Create render view by RtcEngine
            val surfaceView = RtcEngine.CreateRendererView(this@WatchLiveStreamingActivity)
            surfaceView.setZOrderMediaOverlay(true)
            // Add to the remote container
            binding.coHostThirdVideo.addView(
                surfaceView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            // Setup remote video to render
            engine?.enableAudio()
            engine?.enableVideo()
            engine?.setupRemoteVideo(
                VideoCanvas(
                    surfaceView,
                    VideoCanvas.RENDER_MODE_HIDDEN,
                    uid
                )
            )
            updateUIForMultipleHost()
            binding.llFirstAndThirdCoHost.isVisible = true
            binding.coHostThirdVideo.visibility = View.VISIBLE
            if (thirdCoHostUid != loggedInUserCache.getLoggedInUserId()) {
                binding.coHostThirdVideo.addView(
                    binding.thirdCoHostRelativeLayout,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                binding.thirdCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
            }

        }
    }

    private fun setFourthCoHostVideo(uid: Int) {

        liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.FourthCoHost.type
        fourthCoHost = true
        fourthCoHostUid = uid
        handler?.post {
            if (binding.coHostFourVideo.childCount > 0) {
                binding.coHostFourVideo.removeAllViews()
            }
            // Create render view by RtcEngine
            val surfaceView = RtcEngine.CreateRendererView(this@WatchLiveStreamingActivity)
            surfaceView.setZOrderMediaOverlay(true)
            // Add to the remote container
            binding.coHostFourVideo.addView(
                surfaceView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            // Setup remote video to render
            engine?.enableAudio()
            engine?.enableVideo()
            engine?.setupRemoteVideo(
                VideoCanvas(
                    surfaceView,
                    VideoCanvas.RENDER_MODE_HIDDEN,
                    uid
                )
            )

            updateUIForMultipleHost()

            binding.llSecondAndFourthCoHost.isVisible = true
            binding.coHostFourVideo.visibility = View.VISIBLE

            if (fourthCoHostUid != loggedInUserCache.getLoggedInUserId()) {
                binding.coHostFourVideo.addView(
                    binding.fourCoHostRelativeLayout,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                binding.fourCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
            }
        }
    }

    /**
     * IRtcEngineEventHandler is an abstract class providing default implementation.
     * The SDK uses this class to report to the app on SDK runtime events.
     */
    private val iRtcEngineEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onWarning(warn: Int) {
        }

        override fun onError(err: Int) {
        }

        override fun onLeaveChannel(stats: RtcStats) {
            super.onLeaveChannel(stats)
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            myUid = uid
            joined = true
            renderer?.isCallInProgress = true
            welcomeMessage()
            // firstCoHostUid = uid

        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            listOfCoHostId.add(uid)

            if (uid == liveEventInfo.userId) {
                if (!firstCoHost) {
                    setFirstCoHostVideo(uid)
                    return
                } else {
                    if (!secondCoHost) {
                        setSecondCoHostVideo(uid)
                        return
                    }
                    if (!thirdCoHost) {
                        setThirdCoHostVideo(uid)
                        return

                    }
                    if (!fourthCoHost) {
                        setFourthCoHostVideo(uid)
                        return
                    }
                }
            } else {
                if (!secondCoHost) {
                    setSecondCoHostVideo(uid)
                    return
                }
                if (!thirdCoHost) {
                    setThirdCoHostVideo(uid)
                    return

                }
                if (!fourthCoHost) {
                    setFourthCoHostVideo(uid)
                    return

                }
            }

        }

        override fun onUserOffline(uid: Int, reason: Int) {
            handler?.post {
                if (uid == liveEventInfo.userId) {
                    if (liveEventInfo.isAllowPlayGame == 1) {
                        if (!isCoHost) {
                        }
                      onBackPressed()

                    } else {
                        watchLiveVideoViewModel.endLiveEvent()
                        watchLiveVideoViewModel.sendLiveEndEvent()
                    }
                }
                liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
                    if (key == uid) {
                        if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                            firstCoHost = false
                            handler?.post {
                                if (binding.coHostFirstVideo.childCount > 0) {
                                    binding.coHostFirstVideo.removeAllViews()
                                }

                                binding.coHostFirstVideo.visibility = View.GONE
                                if (!binding.coHostThirdVideo.isVisible) {
                                    binding.llFirstAndThirdCoHost.visibility = View.GONE
                                }
                            }
                        }
                        if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                            secondCoHost = false
                            handler?.post {
                                if (binding.coHostSecondVideo.childCount > 0) {
                                    binding.coHostSecondVideo.removeAllViews()
                                }
                                if (!binding.coHostFourVideo.isVisible) {
                                    binding.llSecondAndFourthCoHost.isVisible = false
                                }
                                binding.coHostSecondVideo.visibility = View.GONE
                            }


                        }
                        if (value == LiveStreamNoOfCoHost.ThirdCoHost.type) {
                            thirdCoHost = false
                            handler?.post {
                                if (binding.coHostThirdVideo.childCount > 0) {
                                    binding.coHostThirdVideo.removeAllViews()
                                }
                                if (!binding.coHostFirstVideo.isVisible) {
                                    binding.llFirstAndThirdCoHost.isVisible = false
                                }
                                binding.coHostThirdVideo.visibility = View.GONE
                            }

                        }
                        if (value == LiveStreamNoOfCoHost.FourthCoHost.type) {
                            liveStreamNoOfCoHostHashMap[uid] =
                                LiveStreamNoOfCoHost.FourthCoHost.type
                            fourthCoHost = false
                            handler?.post {
                                if (binding.coHostFourVideo.childCount > 0) {
                                    binding.coHostFourVideo.removeAllViews()
                                }
                                if (!binding.coHostSecondVideo.isVisible) {
                                    binding.llSecondAndFourthCoHost.isVisible = false
                                }
                                binding.coHostFourVideo.visibility = View.GONE
                            }
                        }
                        handler?.postDelayed({
                            if (!binding.coHostThirdVideo.isVisible && !binding.coHostFourVideo.isVisible && !binding.coHostSecondVideo.isVisible) {
                                updateUIForSingleHost()
                            }
                        }, 1000)

                    }

                }
            }
        }

        override fun onClientRoleChanged(oldRole: Int, newRole: Int) {

        }
    }

    private fun openUpdateCoHostStatusDialog(isFromNotification: Boolean) {
        if (!isGameStarted) {
            val dialog =
                LiveStreamAcceptRejectDialog(liveEventInfo, isFrom = LivePopupType.JoinAsCoHost)
            dialog.apply {
                isCancelable = false
                inviteCoHostStatus.subscribeAndObserveOnMainThread {
                    if (it) {
                        showEffects()
                        binding.maskFlipAppCompatImageView.visibility = View.VISIBLE
                        binding.filtersFlipAppCompatImageView.visibility = View.VISIBLE
                        binding.effectsFlipAppCompatImageView.visibility = View.VISIBLE
                        binding.backgroundAppCompatImageView.visibility = View.VISIBLE
                        binding.localPreview.visibility = View.VISIBLE
                        updateCommentsContainerParam(130)
                    }
                    dismissDialog()
                    acceptClick = it
                    if (isFromNotification) {
                        if (it) {
                            watchLiveVideoViewModel.joinLiveEvent(
                                isCoHost = true,
                                isFromNotification = true,
                                "",
                                liveEventInfo
                            )
                            updateUIForMultipleHost()
                        } else {
                            watchLiveVideoViewModel.joinLiveEvent(
                                isCoHost = false,
                                isFromNotification = true,
                                "",
                                liveEventInfo
                            )
                        }
                    } else {
                        if (it) {
                            watchLiveVideoViewModel.joinLiveEvent(
                                isCoHost = true,
                                isFromNotification = true,
                                "",
                                liveEventInfo
                            )
                            updateUIForMultipleHost()

                        } else {
                            watchLiveVideoViewModel.joinLiveEvent(
                                isCoHost = false,
                                isFromNotification = false,
                                "",
                                liveEventInfo
                            )
                        }
                    }
                }
            }
            dialog.show(supportFragmentManager, LiveStreamAcceptRejectDialog::class.java.name)
        }
    }

    private fun updateCommentsContainerParam(rightMargin: Int) {
        val relativeParams: RelativeLayout.LayoutParams =
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
        relativeParams.setMargins(0, 0, rightMargin, 0)

        relativeParams.addRule(RelativeLayout.ALIGN_PARENT_START)
        relativeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        binding.flCommentsContainer.layoutParams = relativeParams
    }


    override fun onDestroy() {
        super.onDestroy()

        val props = JSONObject()
        props.put(Constant.SCREEN_TYPE, "live")

        mp?.track(Constant.SCREEN_TIME, props)

        Glide.with(applicationContext).clear(binding.ivUser1UserImage)
        Glide.with(applicationContext).clear(binding.ivUser2ProfileImage)
        if (isCoHost) {
            if (liveEventInfo.isAllowPlayGame == 1) {
                if (isGameStarted) {
                    watchLiveVideoViewModel.endGameInLive(
                        StartGameRequestRequest(
                            liveEventInfo.channelId,
                            liveEventInfo.id,
                            liveEventInfo.userId
                        )
                    )
                }
            }
        }


        watchLiveVideoViewModel.updateLiveViewerCount(
            UpdateLiveViewerCountRequest(
                liveEventInfo.channelId,
                liveEventInfo.id,
                loggedInUserCache.getLoggedInUserId(),
                liveEventInfo.isAllowPlayGame
            )
        )

        engine?.leaveChannel()
        handler?.post { RtcEngine.destroy() }
        countTimerDisposable?.dispose()
        engine = null

        if (deepAR == null) {
            return
        }
        deepAR!!.setAREventListener(null)
        deepAR!!.release()
        deepAR = null
        RxBus.publish(RxEvent.RemoveCoHost)

    }

    private fun updateTimerText(secondToTime: Time) {
        val minString = secondToTime.min.toString().padStart(2, '0')
        val secondString = secondToTime.second.toString().padStart(2, '0')
        binding.tvGameTimer.text =
            resources.getString(R.string.label_vs).plus(" ")
                .plus(minString.plus(":").plus(secondString))
    }

    private fun startTimer(time: Int) {
        var countTime = time
        countTimerDisposable?.dispose()
        countTimerDisposable =
            Observable.interval(1, TimeUnit.SECONDS).subscribeAndObserveOnMainThread {
                countTime--
                updateTimerText(countTime.secondToTime())

                if (countTime <= 0) {
                    countTimerDisposable?.dispose()
                    binding.tvGameTimer.isVisible = false
                    binding.winProgressIndicator.isVisible = false
                }
            }
    }


    private fun manageStartGameAnimation() {
        binding.tvGameTimer.isVisible = true
        binding.winProgressIndicator.isVisible = true
        binding.llUser1WinContainer.isVisible = true
        binding.llUser2WinContainer.isVisible = true
        binding.tvGiftCountUser1.isVisible = true
        binding.tvGiftCountUser2.isVisible = true
        startTimer(300)


        if (!isCoHost) {
            handler?.postDelayed({
                displayStartGameAnimation()
            }, 2000)
        } else {
            displayStartGameAnimation()
        }

        binding.secondCoHostRelativeLayout.setOnTouchListener { _, p1 ->
            gestureDetector.onTouchEvent(p1)
            true
        }

        binding.firstCoHostRelativeLayout.setOnTouchListener { _, p1 ->
            gestureDetector.onTouchEvent(p1)
            false
        }

        handler?.postDelayed({ binding.llGameUser2Info.visibility = View.GONE }, 5000)
        handler?.postDelayed({ binding.llGameUser1Info.visibility = View.GONE }, 5000)
        handler?.postDelayed({ binding.ivGameStarted.visibility = View.GONE }, 5000)

    }

    private fun displayStartGameAnimation() {
        val anim: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        binding.llGameUser1Info.isVisible = true
        binding.llGameUser1Info.startAnimation(anim)

        binding.llGameUser2Info.isVisible = true
        binding.llGameUser2Info.startAnimation(anim)

        binding.ivGameStarted.isVisible = true
        binding.ivGameStarted.startAnimation(anim)
    }

    private fun openGameRequestDialog() {
        val dialog =
            PlayGameAcceptRejectDialog(
                liveEventInfo.chatUserName,
                liveEventInfo.profilePhoto,
                liveEventInfo.isVerified
            )
        dialog.apply {
            isCancelable = false
            gameRequestStates.subscribeAndObserveOnMainThread {

                watchLiveVideoViewModel.startGameInLive(
                    StartGameRequestRequest(
                        liveEventInfo.channelId,
                        liveEventInfo.id,
                        liveEventInfo.userId,
                    )
                )
                dismissDialog()
            }
        }
        dialog.show(supportFragmentManager, PlayGameAcceptRejectDialog::class.java.name)
    }


    private fun manageGameGiftCount(liveEventKickUser: TopGifterInfo) {
        if (liveEventKickUser.gameStarted == 1) {

            displayTimerToAttendee(liveEventKickUser)
            liveEventKickUser.progress?.forEach {
                liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
                    if (key == it.userId) {
                        if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                            binding.tvGiftCountUser1.text = it.totalCoins.toString()
                            binding.tvUser1WinCount.text = it.totalWinCount.toString()
                            user1ContributorList =
                                liveEventKickUser.topgifter?.filter { it.toId == key }

                        } else if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                            binding.tvGiftCountUser2.text = it.totalCoins.toString()
                            binding.tvUser2WinCount.text = it.totalWinCount.toString()
                            user2ContributorList =
                                liveEventKickUser.topgifter?.filter { it.toId == key }

                        }
                    }
                }
            }
            manageProgressOfGame()

        }
    }

    private fun manageEndGameWinCount(liveEventKickUser: GameResultInfo) {
        liveEventKickUser.gameResult?.forEach {
            liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
                if (key == it.userId) {
                    if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                        binding.tvUser1WinCount.text = it.totalWinCount.toString()

                    } else if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                        binding.tvUser2WinCount.text = it.totalWinCount.toString()
                    }
                }
            }
        }
    }


    private fun updateUIForSingleHost() {
        binding.tvGiftCountUser1.isVisible = false
        binding.tvGiftCountUser2.isVisible =false
        binding.llUser1WinContainer.isVisible = false
        binding.llUser2WinContainer.isVisible = false
        binding.llFirstCoHostData.isVisible = false
        val layoutParams: RelativeLayout.LayoutParams =
            binding.llFooter.layoutParams as RelativeLayout.LayoutParams
        layoutParams.removeRule(RelativeLayout.BELOW)

        val layoutParamss: RelativeLayout.LayoutParams =
            binding.llLiveContainer.layoutParams as RelativeLayout.LayoutParams
        layoutParamss.removeRule(RelativeLayout.BELOW)

        binding.llMainLiveStreaming.background = resources.getDrawable(R.color.color_transparent)

        binding.llLiveContainer.layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
        binding.llLiveContainer.requestLayout()
        binding.ivCommentShadow.isVisible = false

        val layoutParams2: RelativeLayout.LayoutParams =
            binding.llUser1ReceiveGiftInfo.layoutParams as RelativeLayout.LayoutParams
        layoutParams2.removeRule(RelativeLayout.ABOVE)
        layoutParams2.addRule(RelativeLayout.CENTER_VERTICAL)

        val layoutParams3: RelativeLayout.LayoutParams =
            binding.llUser2ReceiveGiftInfo.layoutParams as RelativeLayout.LayoutParams
        layoutParams3.removeRule(RelativeLayout.ABOVE)
        layoutParams3.addRule(RelativeLayout.CENTER_VERTICAL)
    }

    private fun updateUIForMultipleHost() {
        binding.llLiveContainer.layoutParams.height =
            resources.getDimensionPixelSize(R.dimen._300sdp)
        binding.llLiveContainer.requestLayout()

        val layoutParams: RelativeLayout.LayoutParams =
            binding.llFooter.layoutParams as RelativeLayout.LayoutParams
        layoutParams.addRule(RelativeLayout.BELOW, R.id.llLiveContainer)

        val layoutParamss: RelativeLayout.LayoutParams =
            binding.llLiveContainer.layoutParams as RelativeLayout.LayoutParams
        layoutParamss.addRule(RelativeLayout.BELOW, R.id.winProgressIndicator)

        binding.llMainLiveStreaming.background = resources.getDrawable(R.color.color_FF141A26)

        binding.ivCommentShadow.isVisible = true

        val layoutParams2: RelativeLayout.LayoutParams =
            binding.llUser1ReceiveGiftInfo.layoutParams as RelativeLayout.LayoutParams
        layoutParams2.removeRule(RelativeLayout.CENTER_VERTICAL)
        layoutParams2.addRule(RelativeLayout.ABOVE, R.id.llFirstCoHostData)

        val layoutParams3: RelativeLayout.LayoutParams =
            binding.llUser2ReceiveGiftInfo.layoutParams as RelativeLayout.LayoutParams
        layoutParams3.removeRule(RelativeLayout.CENTER_VERTICAL)
        layoutParams3.addRule(RelativeLayout.ABOVE, R.id.llSecondCoHostData)
    }


    private fun setCoHostName() {
        handler?.postDelayed({
            listOfCoHostInfo?.hosts?.forEach {
                liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
                    if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                        if (key == it.userId) {
                            if (it.userId != liveEventInfo.userId && it.userId != loggedInUserCache.getLoggedInUserId()) {
                                binding.tvFirstCoHostName.text = it.name
                            } else {
                                binding.tvFirstCoHostName.text = it.name
                                binding.llFirstCoHostData.visibility = View.VISIBLE

                            }
                        }
                    }

                    if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                        if (key == it.userId) {

                            if (it.userId != loggedInUserCache.getLoggedInUserId()) {
                                binding.tvSecondCoHostName.text = it.name
                                binding.ivSecondVerified.isVisible = it.isVerified == 1
                            } else {
                                binding.llSecondCoHostData.visibility = View.INVISIBLE
                            }

                        }
                    }
                    if (value == LiveStreamNoOfCoHost.ThirdCoHost.type) {
                        if (key == it.userId) {
                            if (it.userId != liveEventInfo.userId && it.userId != loggedInUserCache.getLoggedInUserId()) {
                                binding.tvThirdCoHostName.text = it.name
                                binding.ivThirdVerified.isVisible = it.isVerified == 1
                            } else {
                                binding.llThirdCoHostData.isVisible = false

                            }
                        }
                    }
                    if (value == LiveStreamNoOfCoHost.FourthCoHost.type) {
                        if (key == it.userId) {
                            if (it.userId != liveEventInfo.userId && it.userId != loggedInUserCache.getLoggedInUserId()) {
                                binding.tvFourthCoHostName.text = it.name
                                binding.ivFourthVerified.isVisible = it.isVerified == 1
                            } else {
                                binding.llFourthCoHostData.isVisible = false
                            }
                        }
                    }
                }
            }
        }, 2000)
    }


    private fun manageProgressOfGame() {
        val count1 = binding.tvGiftCountUser1.text.toString().toDouble()
        val count2 = binding.tvGiftCountUser2.text.toString().toDouble()

        if (count1 != 0.0 || count2 != 0.0) {
            val sum = count1 + count2
            val divide: Double = count1 / sum
            val ratio = divide * 100.0

            if (ratio < 10) {
                binding.winProgressIndicator.progress = 10
            } else if (ratio > 90) {
                binding.winProgressIndicator.progress = 90
            } else {
                binding.winProgressIndicator.progress = ratio.toInt()
            }

        } else {
            binding.winProgressIndicator.progress = 50
        }
    }


    private fun manageEndGame(endGameInfo: GameResultInfo) {
        binding.tvGameTimer.isVisible = false
        binding.winProgressIndicator.isVisible = false
        binding.winProgressIndicator.progress = 50
        binding.llUser1ContributorImage.isVisible = false
        binding.flUser2ContributorImage.isVisible = false
        binding.tvGiftCountUser1.isVisible = false
        binding.tvGiftCountUser2.isVisible =false

        binding.secondCoHostRelativeLayout.setOnTouchListener { _, p1 ->
            gestureDetector.onTouchEvent(p1)
            false
        }

        binding.firstCoHostRelativeLayout.setOnTouchListener { _, p1 ->
            gestureDetector.onTouchEvent(p1)
            false
        }

        val anim: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        if (binding.tvGiftCountUser1.text.toString()
                .toInt() > binding.tvGiftCountUser2.text.toString().toInt()
        ) {
            binding.ivUser1GameResultText.setImageResource(R.drawable.ic_win_text)
            binding.ivUser1GameResultEmoji.setImageResource(R.drawable.ic_game_win_emoji)
            binding.ivUser2GameResultText.setImageResource(R.drawable.ic_loss_text)
            binding.ivUser2GameResultEmoji.setImageResource(R.drawable.ic_game_loss_emoji)

            binding.llUser1GameResult.isVisible = true
            binding.llUser1GameResult.startAnimation(anim)

            binding.llUser2GameResult.isVisible = true
            binding.llUser2GameResult.startAnimation(anim)

        } else if (binding.tvGiftCountUser1.text.toString()
                .toInt() == binding.tvGiftCountUser2.text.toString().toInt()
        ) {
            binding.llDrawGameResult.isVisible = true
            binding.llDrawGameResult.startAnimation(anim)

        } else {
            binding.ivUser2GameResultText.setImageResource(R.drawable.ic_win_text)
            binding.ivUser2GameResultEmoji.setImageResource(R.drawable.ic_game_win_emoji)
            binding.ivUser1GameResultText.setImageResource(R.drawable.ic_loss_text)
            binding.ivUser1GameResultEmoji.setImageResource(R.drawable.ic_game_loss_emoji)

            binding.llUser1GameResult.isVisible = true
            binding.llUser1GameResult.startAnimation(anim)

            binding.llUser2GameResult.isVisible = true
            binding.llUser2GameResult.startAnimation(anim)
        }

        binding.tvGiftCountUser1.text = resources.getString(R.string._0)
        binding.tvGiftCountUser2.text = resources.getString(R.string._0)

        handler?.postDelayed({
            binding.llUser1GameResult.isVisible = false
            binding.llUser2GameResult.isVisible = false
            binding.llDrawGameResult.isVisible = false
            binding.winProgressIndicator.progress = 50
        }, 5000)

        val data = endGameInfo.gameResult?.filter { it.userId == loggedInUserCache.getLoggedInUserId() }
        data?.forEach {
            it.centsValue = centsValue
        }
        if (isCoHost) {
            handler?.postDelayed({
                val bottomSheet = data?.let { GameResultBottomSheet.newInstance(it.first()) }
                handler?.postDelayed({ bottomSheet?.dismiss() }, 10000)
                bottomSheet?.show(supportFragmentManager, GameResultBottomSheet::class.java.name)
            }, 5000)
        }
    }


    private fun displayTimerToAttendee(liveEventKickUser: TopGifterInfo) {
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        val newOffset: Int = cal.timeZone.getOffset(liveEventKickUser.playStartTime.toLong())
        val now: Long =
            if (newOffset < 0) liveEventKickUser.playStartTime.toLong() * 1000 + newOffset else liveEventKickUser.playStartTime.toLong() * 1000 - newOffset
        val startGameTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now).toDate()
        val currentTime =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().timeInMillis)
                .toDate()
        val diff = startGameTime.time - currentTime.time
        val sec = (diff / 1000).toInt()
        binding.tvGameTimer.isVisible = true
        startTimer(sec)

    }


    override fun onResume() {
        super.onResume()
        watchLiveVideoViewModel.getMyEarningInfo()
        if (liveEventInfo.id != -1) {
            if (socketDataManager.isConnected) {
                watchLiveVideoViewModel.joinLiveRoom(liveEventInfo.isAllowPlayGame ?: 0)
            } else {
                handler?.postDelayed({
                    watchLiveVideoViewModel.joinLiveRoom(liveEventInfo.isAllowPlayGame ?: 0)
                }, 3000)
            }
        }
        if (surfaceView != null && isCoHost) {
            setupCamera()
            surfaceView!!.onResume()
        }
        RxBus.listen(RxEvent.CloseLive::class.java).subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

    }


    private fun openLeaveCurrentLiveDialog(data: LiveEventInfo) {
        if (!isGameStarted) {
            val dialog = LiveStreamAcceptRejectDialog(data, isFrom = LivePopupType.JoinLive)
            dialog.apply {
                isCancelable = false
                inviteCoHostStatus.subscribeAndObserveOnMainThread {

                    if (it) {
                        startActivity(
                            LiveRoomActivity.getIntent(
                                this@WatchLiveStreamingActivity,
                                liveId = data.id, isJoin = false
                            )
                        )
                        dismissDialog()
                        finish()

                    } else {
                        dismissDialog()
                    }
                }
            }
            dialog.show(supportFragmentManager, LiveStreamAcceptRejectDialog::class.java.name)
        }

    }

    private fun manageCoHostFollowOption(followInfo: CoHostFollowInfo) {

        liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
            if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                if (key == followInfo.userId) {
                    binding.ivFirstFollow.isVisible = followInfo.isFollow == 0
                    firstFollowUid = key
                }
            }

            if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                if (key == followInfo.userId) {
                    if (key == followInfo.userId) {
                        binding.ivSecondFollow.isVisible = followInfo.isFollow == 0
                        secondFollowUid = key
                    }
                }
            }
            if (value == LiveStreamNoOfCoHost.ThirdCoHost.type) {
                if (key == followInfo.userId) {
                    if (key == followInfo.userId) {
                        binding.ivThirdFollow.isVisible = followInfo.isFollow == 0
                        thirdFollowUid = key
                    }
                }
            }
            if (value == LiveStreamNoOfCoHost.FourthCoHost.type) {
                if (key == followInfo.userId) {
                    if (key == followInfo.userId) {
                        binding.ivFourthFollow.isVisible = followInfo.isFollow == 0
                        fourthFollowUid = key
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ),
            1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (grantResult in grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, requestCode)
                return
            }
        }
    }

    fun setup() {
        setupVideoConfig()
        surfaceView = GLSurfaceView(this)
        surfaceView!!.setEGLContextClientVersion(2)
        surfaceView!!.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        renderer = DeepARRenderer(deepAR, engine, this)
        surfaceView!!.setEGLContextFactory(DeepARRenderer.MyContextFactory(renderer))
        surfaceView!!.setRenderer(renderer)
        surfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        val local = findViewById<FrameLayout>(R.id.localPreview)
        local.addView(surfaceView)
        engine!!.setExternalVideoSource(
            true,
            true,
            true
        )
    }

    private fun getScreenOrientation(): Int {
        val rotation = windowManager.defaultDisplay.rotation
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val width = dm.widthPixels
        val height = dm.heightPixels
        val orientation: Int
        // if the device's natural orientation is portrait:
        orientation = if ((rotation == Surface.ROTATION_0
                    || rotation == Surface.ROTATION_180) && height > width ||
            (rotation == Surface.ROTATION_90
                    || rotation == Surface.ROTATION_270) && width > height
        ) {
            when (rotation) {
                Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                else -> {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        } else {
            when (rotation) {
                Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                else -> {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        }
        return orientation
    }

    private fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture!!.addListener({
            try {
                val cameraProvider = cameraProviderFuture!!.get()
                bindImageAnalysis(cameraProvider)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindImageAnalysis(cameraProvider: ProcessCameraProvider) {
        val cameraResolutionPreset = CameraResolutionPreset.P1920x1080
        val width: Int
        val height: Int
        val orientation = getScreenOrientation()
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            width = cameraResolutionPreset.width
            height = cameraResolutionPreset.height
        } else {
            width = cameraResolutionPreset.height
            height = cameraResolutionPreset.width
        }
        val cameraResolution = Size(width, height)
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(cameraResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis?.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer)
        buffersInitialized = false
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }

    private fun initializeBuffers(size: Int) {
        if (buffers == null) {
            buffers = arrayOfNulls(NUMBER_OF_BUFFERS)
        }
        for (i in 0 until NUMBER_OF_BUFFERS) {
            buffers!![i] = ByteBuffer.allocateDirect(size)
            buffers!![i]?.order(ByteOrder.nativeOrder())
            buffers!![i]?.position(0)
        }
        allocatedBufferSize = size
    }

    override fun onPause() {
        super.onPause()
        if (surfaceView != null) {
            isPause = true
            surfaceView!!.onPause()
        }
    }

    override fun onStop() {
        var cameraProvider: ProcessCameraProvider? = null
        try {
            cameraProvider = cameraProviderFuture?.get()
            cameraProvider?.unbindAll()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        super.onStop()
    }

    private fun setupVideoConfig() {
        engine!!.enableVideo()
        engine!!.setExternalVideoSource(
            true,
            true,
            true
        )

        // Please go to this page for detailed explanation
        // https://docs.agora.io/en/Video/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_rtc_engine.html#af5f4de754e2c1f493096641c5c5c1d8f
        engine!!.setVideoEncoderConfiguration(
            VideoEncoderConfiguration( // Agora seems to work best with "Square" resolutions (Aspect Ratio 1:1)
                // At least when used in combination with DeepAR
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            )
        )
    }

    override fun screenshotTaken(bitmap: Bitmap?) {}

    override fun videoRecordingStarted() {}

    override fun videoRecordingFinished() {}

    override fun videoRecordingFailed() {}

    override fun videoRecordingPrepared() {}

    override fun shutdownFinished() {}

    override fun initialized() {
        try {
            deepAR!!.switchEffect("mask", "none")
        } catch (e: Exception) {
        }
    }

    override fun faceVisibilityChanged(b: Boolean) {}

    override fun imageVisibilityChanged(s: String?, b: Boolean) {}

    override fun frameAvailable(image: Image?) {}

    override fun error(arErrorType: ARErrorType?, s: String?) {}

    override fun effectSwitched(s: String?) {}

    private fun hideEffects() {
        if (!isEffectSelected) {
            isHide = true
            handler?.postDelayed({
                if (isHide) {
                    binding.typeOfEffectsRecyclerView.visibility = View.GONE

                    val layoutParams: RelativeLayout.LayoutParams =
                        binding.llFooter.layoutParams as RelativeLayout.LayoutParams
                    layoutParams.removeRule(RelativeLayout.ABOVE)
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

                    liveEffectsAdapter.listOfEffects = arrayListOf()

                    val params: ViewGroup.LayoutParams =
                        binding.typeOfEffectsRecyclerView.getLayoutParams()
                    params.height = 0
                    binding.typeOfEffectsRecyclerView.setLayoutParams(params)

                    val relativeParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                    relativeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    relativeParams.removeRule(RelativeLayout.ABOVE)
                    relativeParams.addRule(RelativeLayout.ALIGN_PARENT_END)

                    binding.otherPart.layoutParams = relativeParams
                }

            }, 5000)
        }
    }

    private fun showEffects() {
        isEffectSelected = false
        if (!binding.typeOfEffectsRecyclerView.isVisible) {
            isHide = false
            val layoutParams: RelativeLayout.LayoutParams =
                binding.llFooter.layoutParams as RelativeLayout.LayoutParams
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            layoutParams.addRule(RelativeLayout.ABOVE, R.id.typeOfEffectsRecyclerView)

            binding.typeOfEffectsRecyclerView.visibility = View.VISIBLE
            liveEffectsAdapter.listOfEffects = newListItems

            val params: ViewGroup.LayoutParams = binding.typeOfEffectsRecyclerView.getLayoutParams()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            binding.typeOfEffectsRecyclerView.setLayoutParams(params)
            binding.typeOfEffectsRecyclerView.requestLayout()


            val relativeParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            relativeParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            relativeParams.addRule(RelativeLayout.ABOVE, binding.typeOfEffectsRecyclerView.id)
            relativeParams.addRule(RelativeLayout.ALIGN_PARENT_END)

            binding.otherPart.layoutParams = relativeParams

            hideEffects()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (liveEventInfo.isCoHost == 1) {
            watchLiveVideoViewModel.removeCoHostFromLive(
                StartGameRequestRequest(
                    liveId = liveEventInfo.id,
                    roomId = liveEventInfo.channelId,
                    userId = loggedInUserCache.getLoggedInUserId()
                )
            )
        }
    }
}