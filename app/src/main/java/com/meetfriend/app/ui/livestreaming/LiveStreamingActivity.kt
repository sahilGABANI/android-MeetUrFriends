package com.meetfriend.app.ui.livestreaming

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
import android.view.View.OnFocusChangeListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.jakewharton.rxbinding3.widget.editorActions
import com.makeramen.roundedimageview.RoundedImageView
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.chat.model.MessageType
import com.meetfriend.app.api.effect.model.CenterZoomLayoutManager
import com.meetfriend.app.api.livestreaming.model.*
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityLiveSreamingBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.socket.SocketDataManager
import com.meetfriend.app.ui.chatRoom.roomview.KickOutDialogFragment
import com.meetfriend.app.ui.chatRoom.roomview.ViewUserBottomSheet
import com.meetfriend.app.ui.home.shorts.report.ReportDialogFragment
import com.meetfriend.app.ui.livestreaming.game.GameResultBottomSheet
import com.meetfriend.app.ui.livestreaming.game.GameTopContributorBottomSheet
import com.meetfriend.app.ui.livestreaming.inviteuser.InviteUserInLiveStreamingBottomSheet
import com.meetfriend.app.ui.livestreaming.settings.StartLiveStreamingSettingsBottomSheet
import com.meetfriend.app.ui.livestreaming.videoroom.LiveRoomActivity
import com.meetfriend.app.ui.livestreaming.view.LiveEffectsAdapter
import com.meetfriend.app.ui.livestreaming.view.LiveStreamingCommentAdapter
import com.meetfriend.app.ui.livestreaming.viewmodel.LiveStreamingViewModel
import com.meetfriend.app.ui.livestreaming.viewmodel.LiveStreamingViewState
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.LIVE
import com.meetfriend.app.utils.DeeparDetails
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
import timber.log.Timber
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.properties.Delegates


class LiveStreamingActivity : BasicActivity(), AREventListener {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, LiveStreamingActivity::class.java)
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LiveStreamingViewModel>
    private lateinit var liveStreamUserViewModel: LiveStreamingViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUser: MeetFriendUser? = null
    private var loggedInUserId by Delegates.notNull<Int>()

    @Inject
    lateinit var socketDataManager: SocketDataManager

    private var countTimerDisposable: Disposable? = null

    private lateinit var binding: ActivityLiveSreamingBinding
    private lateinit var liveEventCommentAdapter: LiveStreamingCommentAdapter

    private var engine: RtcEngine? = null
    private var myUid = 0
    private var joined = false
    private var handler: Handler? = null
    private var firstCoHost: Boolean = false
    private var secondCoHost: Boolean = false
    private var thirdCoHost: Boolean = false
    private var fourthCoHost: Boolean = false

    private var firstCoHostUid = 0 // It is always main host UID
    private var secondCoHostUid = 0
    private var thirdCoHostUid = 0
    private var fourthCoHostUid = 0

    private var isEndingStream = false
    private var channelId: String? = null
    private var liveId = -1
    private var isAllowPlayGame = 0
    private var isGameStarted = false
    private var gameCount = 0

    private var liveStreamNoOfCoHostHashMap = HashMap<Int, String>()
    private val listOfLiveEventSendOrReadComment: ArrayList<LiveEventSendOrReadComment> =
        arrayListOf()

    private var user1ContributorList: List<TopGifter>? = null
    private var user2ContributorList: List<TopGifter>? = null
    private var liveSummaryInfo: LiveSummaryInfo? = null
    private var centsValue: String? = null
    private var listOfCoHostInfo: CoHostListInfo? = null
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
    private var isHide = true
    private var wasPlayGameVisible = false
    private var wasfiltersVisible = false
    private var isEffectSelected = false
    private var previousHeight: Int = 0

    private val getThumbModel: HeartsView.Model by lazy {
        val bitmap = AppCompatResources.getDrawable(
            this@LiveStreamingActivity,
            R.drawable.ic_fill_heart
        )?.toBitmap()

        HeartsView.Model(
            0,
            bitmap!!
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deepAR = DeepAR(this)
        deepAR!!.setLicenseKey("24498eaaaa0063ce289681785b8275f1c9ffebdfe0e20f089be95b3c5234d9447d7aa983f37b9adb")
        deepAR!!.initialize(this, this)

        MeetFriendApplication.component.inject(this)
        liveStreamUserViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityLiveSreamingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDeepar()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        handler = Handler(Looper.getMainLooper())

        loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser
        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        updateUserInfo()

        listenToViewModel()
        listenToViewEvent()
        mp?.timeEvent(Constant.SCREEN_TIME)
        hideEffects(10)


    }

    private fun initDeepar() {
        callInProgress = false

        val llm = CenterZoomLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        liveEffectsAdapter = LiveEffectsAdapter(this@LiveStreamingActivity).apply {
            effectClicks.subscribeAndObserveOnMainThread { response ->
                isEffectSelected = true
                MeetFriendApplication.assetManager?.let { assetManager ->
                    try {
                        if (response.effectName == DeeparDetails.NONE) {
                            val pathNone: String? = null
                            deepAR?.switchEffect(response.type, pathNone)
                        } else {
                            val stream: InputStream = assetManager.open(response.effectFileName)
                            deepAR?.switchEffect(response.type, stream)
                        }
                        hideEffects(5000)
                        liveEffectsAdapter.listOfEffects?.indexOf(response)?.let { it1 ->
                            binding.typeOfEffectsRecyclerView.smoothScrollToPosition(it1)
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
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
                }
            })
        }


        liveEffectsAdapter.listOfEffects = DeeparDetails.getMasks()

        binding.maskFlipAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.maskFlipAppCompatImageView.isSelected = true
            binding.filtersFlipAppCompatImageView.isSelected = false
            binding.effectsFlipAppCompatImageView.isSelected = false
            binding.backgroundAppCompatImageView.isSelected = false
            liveEffectsAdapter.listOfEffects = DeeparDetails.getMasks()
            showEffects()
        }

        binding.filtersFlipAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.maskFlipAppCompatImageView.isSelected = false
            binding.filtersFlipAppCompatImageView.isSelected = true
            binding.effectsFlipAppCompatImageView.isSelected = false
            binding.backgroundAppCompatImageView.isSelected = false
            liveEffectsAdapter.listOfEffects = DeeparDetails.getFilters()
            showEffects()
        }

        binding.effectsFlipAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.maskFlipAppCompatImageView.isSelected = false
            binding.filtersFlipAppCompatImageView.isSelected = false
            binding.effectsFlipAppCompatImageView.isSelected = true
            binding.backgroundAppCompatImageView.isSelected = false
            liveEffectsAdapter.listOfEffects = DeeparDetails.getEffects()
            showEffects()
        }

        binding.backgroundAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.maskFlipAppCompatImageView.isSelected = false
            binding.filtersFlipAppCompatImageView.isSelected = false
            binding.effectsFlipAppCompatImageView.isSelected = false
            binding.backgroundAppCompatImageView.isSelected = true
            liveEffectsAdapter.listOfEffects = DeeparDetails.getBackground()
            showEffects()
        }

    }

    private fun updateUserInfo() {

        Glide.with(this)
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto ?: "")
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.userProfileImageView)

        binding.nameTextView.text =
            loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName.plus(" ")
                .plus(loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName)

        binding.userNameTextView.text = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName

        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        val followersCount = SpannableStringBuilder()
            .append(
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.noOfFollowers?.prettyCount()
                    .toString()
            )
            .append(" ")
            .append(resources.getString(R.string.followers))

        binding.followersCountAppCompatTextView.text = followersCount

    }

    private fun manageEndGameWinCount(liveEventKickUser: GameResultInfo) {
        liveEventKickUser.gameResult?.forEach {
            if (loggedInUserId == it.userId) {
                binding.tvUser1WinCount.text = it.totalWinCount.toString()
            } else {
                binding.tvUser2WinCount.text = it.totalWinCount.toString()
            }
        }
    }

    private fun listenToViewModel() {
        liveStreamUserViewModel.liveStreamingViewState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is LiveStreamingViewState.ErrorMessage -> {
                    showToast(state.errorMessage)
                }

                is LiveStreamingViewState.LeaveLiveRoom -> {
                    isEndingStream = true

                    val props = JSONObject()
                    props.put(Constant.LIVE_ID, liveId)
                    props.put(Constant.ROLE, "MainPublisher")
                    props.put(Constant.START_TIME, liveSummaryInfo?.startTime)
                    props.put(Constant.END_TIME, liveSummaryInfo?.endTime)

                    mp?.track("End Live Detail")
                    onBackPressed()
                }

                is LiveStreamingViewState.LiveWatchingCount -> {
                    val liveWatchingCount =
                        if (state.liveWatchingCount < 0) 0 else state.liveWatchingCount
                    binding.tagTextView.text =
                        liveWatchingCount.prettyCount().toString()
                }

                is LiveStreamingViewState.UpdateComment -> {
                    if (isAllowPlayGame == 1) {

                        if (state.liveEventSendOrReadComment.userId == loggedInUserCache.getLoggedInUserId() || state.liveEventSendOrReadComment.toUserId == loggedInUserCache.getLoggedInUserId()
                                .toString()
                        ) {
                            displayComment(state.liveEventSendOrReadComment)
                        }

                    } else {
                        displayComment(state.liveEventSendOrReadComment)
                    }
                }

                is LiveStreamingViewState.LiveHeart -> {
                    binding.likeTextView.text = state.sendHeartSocketEvent.heartCounter.toString()
                    binding.likeImageView.isSelected = !binding.likeImageView.isSelected
                    binding.heartsView.emitHeart(getThumbModel)
                }

                is LiveStreamingViewState.JoinCoHost -> {
                    listOfCoHostInfo = state.coHostSocketEvent
                    setCoHostName()
                    if (state.coHostSocketEvent.hosts?.size!! > 1) {
                        binding.tvSelfUserName.text = state.coHostSocketEvent.hosts[0].name
                        Glide.with(this)
                            .load(state.coHostSocketEvent.hosts[0].profilephoto)
                            .error(R.drawable.ic_empty_profile_placeholder)
                            .placeholder(R.drawable.ic_empty_profile_placeholder)
                            .into(binding.ivSelfProfileImage)

                        binding.tvOppositeUserName.text = state.coHostSocketEvent.hosts[1].name
                        Glide.with(this)
                            .load(state.coHostSocketEvent.hosts[1].profilephoto)
                            .error(R.drawable.ic_empty_profile_placeholder)
                            .placeholder(R.drawable.ic_empty_profile_placeholder)
                            .into(binding.ivOppositeUserImage)
                    }


                }

                is LiveStreamingViewState.GameStarted -> {
                    if (state.startGameInfo.liveId == liveId) {
                        isGameStarted = true
                        binding.tvPlayGame.isVisible = false
                        manageStartGameAnimation()
                        binding.settingsAppCompatImageView.isVisible = false
                    }

                }

                is LiveStreamingViewState.GameEnded -> {
                    if (state.endGameInfo.liveId == liveId) {
                        manageEndGame(state.endGameInfo)
                        manageEndGameWinCount(state.endGameInfo)
                        binding.settingsAppCompatImageView.isVisible = true
                    }
                }

                is LiveStreamingViewState.TopGifterInfo -> {

                    if (state.liveEventKickUser.gameStarted == 1) {
                        if (!binding.winProgressIndicator.isVisible) {
                            binding.winProgressIndicator.isVisible = true
                            binding.llUser1WinContainer.isVisible = true
                            binding.llUser2WinContainer.isVisible = true
                            binding.tvGiftCountUser1.isVisible = true
                            binding.tvGiftCountUser2.isVisible =true
                        }
                        isGameStarted = true
                    } else {
                        isGameStarted = false
                        binding.winProgressIndicator.isVisible = false
                        binding.tvGiftCountUser1.isVisible = false
                        binding.tvGiftCountUser2.isVisible =false
                        binding.tvGameTimer.isVisible = false

                        if (gameCount > 0) {
                            if (binding.coHostSecondVideo.isVisible) {
                                binding.tvPlayGame.isVisible = true
                                binding.tvPlayGame.text = getString(R.string.label_play_again)
                                binding.llUser1ContributorImage.isVisible = false
                                binding.flUser2ContributorImage.isVisible = false
                            }
                        }
                    }

                    if (state.liveEventKickUser.progress.isNullOrEmpty()) {
                        binding.tvGiftCountUser1.text = resources.getString(R.string._0)
                        binding.tvGiftCountUser2.text = resources.getString(R.string._0)
                    } else {
                        if (state.liveEventKickUser.progress?.first()?.userId == loggedInUserCache.getLoggedInUserId()) {
                            binding.tvGiftCountUser1.text =
                                state.liveEventKickUser.progress?.first()?.totalCoins.toString()
                            binding.tvUser1WinCount.text =
                                state.liveEventKickUser.progress?.first()?.totalWinCount.toString()

                        } else {
                            binding.tvGiftCountUser2.text =
                                state.liveEventKickUser.progress?.first()?.totalCoins.toString()
                            binding.tvUser2WinCount.text =
                                state.liveEventKickUser.progress?.first()?.totalWinCount.toString()
                        }
                    }
                    manageProgressOfGame()
                    user1ContributorList =
                        state.liveEventKickUser.topgifter?.filter { it.toId == loggedInUserId }
                    user2ContributorList =
                        state.liveEventKickUser.topgifter?.filter { it.toId != loggedInUserId }
                    user1ContributorList?.forEach {
                        it.index = user1ContributorList?.indexOf(it) ?: 0
                    }
                    user2ContributorList?.forEach {
                        it.index = user2ContributorList?.indexOf(it) ?: 0
                    }

                    if (isGameStarted) {
                        manageUser1ContributorImage()
                        manageUser2ContributorImage()
                    }
                }

                is LiveStreamingViewState.ReceiveGift -> {

                    if (state.receiveGiftInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                        displayReceiveGiftInfo(state.receiveGiftInfo, true)
                    } else {
                        displayReceiveGiftInfo(state.receiveGiftInfo, false)
                    }
                }

                is LiveStreamingViewState.PlayGameRequestInfo -> {

                }

                is LiveStreamingViewState.LiveSummary -> {
                    liveSummaryInfo = state.liveSummaryInfo

                    val props = JSONObject()
                    props.put(
                        Constant.TOTAL_TIME_OF_THIS_MONTH,
                        liveSummaryInfo?.totalTimeOfThisMonth
                    )

                    mp?.track(Constant.TOTAL_HOURS_USER_LIVE_IN_THIS_MONTH, props)
                }

                is LiveStreamingViewState.CoinCentsData -> {
                    centsValue = state.coinCentsInfo?.value

                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun setCoHostName() {
        handler?.postDelayed({
            listOfCoHostInfo?.hosts?.forEach {

                liveStreamNoOfCoHostHashMap.forEach { (key, value) ->

                    if (value == LiveStreamNoOfCoHost.SecondCoHost.type) {
                        if (key == it.userId) {
                            binding.tvSecondCoHostName.text = it.name
                            binding.ivSecondVerified.isVisible = it.isVerified == 1
                        }
                    }
                    if (value == LiveStreamNoOfCoHost.ThirdCoHost.type) {
                        if (key == it.userId) {
                            binding.tvThirdCoHostName.text = it.name
                            binding.ivThirdVerified.isVisible = it.isVerified == 1
                        }
                    }
                    if (value == LiveStreamNoOfCoHost.FourthCoHost.type) {
                        if (key == it.userId) {
                            binding.tvFourthCoHostName.text = it.name
                            binding.ivFourthVerified.isVisible = it.isVerified == 1
                        }
                    }
                }
            }
        }, 2000)
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

    private fun manageUser1ContributorImage() {
        when (user1ContributorList?.size) {
            0 -> {
                binding.llUser1ContributorImage.isVisible = false
            }

            1 -> {
                binding.llUser1ContributorImage.isVisible = true
                setProfileImage(
                    binding.ivTopGifter1User1Image,
                    user1ContributorList!![0].profilePhoto
                )
                binding.ivTopGifter2User1Image.isVisible = false
                binding.ivTopGifter3user1Image.isVisible = false

            }

            2 -> {
                binding.llUser1ContributorImage.isVisible = true
                binding.ivTopGifter2User1Image.isVisible = true
                binding.ivTopGifter3user1Image.isVisible = true
                setProfileImage(
                    binding.ivTopGifter1User1Image,
                    user1ContributorList!![0].profilePhoto
                )
                setProfileImage(
                    binding.ivTopGifter2User1Image,
                    user1ContributorList!![1].profilePhoto
                )
                binding.ivTopGifter3user1Image.isVisible = false

            }

            else -> {
                binding.llUser1ContributorImage.isVisible = true
                binding.ivTopGifter2User1Image.isVisible = true
                binding.ivTopGifter3user1Image.isVisible = true
                setProfileImage(
                    binding.ivTopGifter1User1Image,
                    user1ContributorList!![0].profilePhoto
                )
                setProfileImage(
                    binding.ivTopGifter2User1Image,
                    user1ContributorList!![1].profilePhoto
                )
                setProfileImage(
                    binding.ivTopGifter3user1Image,
                    user1ContributorList!![2].profilePhoto
                )
            }
        }
    }

    private fun manageUser2ContributorImage() {
        when (user2ContributorList?.size) {
            0 -> {
                binding.flUser2ContributorImage.isVisible = false
            }

            1 -> {
                binding.flUser2ContributorImage.isVisible = true
                setProfileImage(
                    binding.ivTopGifter1user2Image,
                    user2ContributorList!![0].profilePhoto
                )
                binding.ivTopGifter2user2Image.isVisible = false
                binding.ivTopGifter3user2Image.isVisible = false

            }

            2 -> {
                binding.flUser2ContributorImage.isVisible = true
                binding.ivTopGifter2user2Image.isVisible = true
                binding.ivTopGifter3user2Image.isVisible = true

                setProfileImage(
                    binding.ivTopGifter1user2Image,
                    user2ContributorList!![0].profilePhoto
                )
                setProfileImage(
                    binding.ivTopGifter2user2Image,
                    user2ContributorList!![1].profilePhoto
                )
                binding.ivTopGifter3user2Image.isVisible = false

            }

            else -> {
                binding.flUser2ContributorImage.isVisible = true
                binding.ivTopGifter2user2Image.isVisible = true
                binding.ivTopGifter3user2Image.isVisible = true

                setProfileImage(
                    binding.ivTopGifter1user2Image,
                    user2ContributorList!![0].profilePhoto
                )
                setProfileImage(
                    binding.ivTopGifter2user2Image,
                    user2ContributorList!![1].profilePhoto
                )
                setProfileImage(
                    binding.ivTopGifter3user2Image,
                    user2ContributorList!![2].profilePhoto
                )
            }
        }
    }

    private fun displayComment(liveEventSendOrReadComment: LiveEventSendOrReadComment) {

        if (liveEventSendOrReadComment.liveId == liveId) {
            listOfLiveEventSendOrReadComment.add(liveEventSendOrReadComment)
            liveEventCommentAdapter.listOfDataItems =
                listOfLiveEventSendOrReadComment
            liveEventCommentAdapter.notifyDataSetChanged()

            binding.commentRecyclerView.scrollToPosition(
                listOfLiveEventSendOrReadComment.size - 1
            )
        }
    }

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
            .plus(if (receiveGiftInfo.quantity!! > 0) receiveGiftInfo.quantity.toString() else 1)
        handler?.postDelayed({ container.visibility = View.GONE }, 4000)

    }


    @SuppressLint("RestrictedApi")
    private fun listenToViewEvent() {
        liveStreamUserViewModel.getCoinInCents()

        binding.rlHeader.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                previousHeight = binding.rlHeader.height
                binding.rlHeader.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        binding.rlHeader.viewTreeObserver.addOnGlobalLayoutListener {

            val view: View = findViewById(R.id.rlHeader)
            val rootViewHeight = view.rootView.height
            val location = IntArray(2)
            binding.llFooter.getLocationOnScreen(location)
            val height = (location[1] + binding.llFooter.measuredHeight) as Int
            var deff = rootViewHeight - height
            if (deff > 500) {
                binding.settingAppCompatImageView.isVisible = true
                if (!isGameStarted && !binding.tvPlayGame.isVisible) {
                    binding.settingsAppCompatImageView.isVisible = true
                }
                binding.cameraFlipAppCompatImageView.isVisible = true
                binding.flMuteUnMute.isVisible = true
                binding.maskFlipAppCompatImageView.isVisible = true
                binding.filtersFlipAppCompatImageView.isVisible = true
                binding.effectsFlipAppCompatImageView.isVisible = true
                binding.backgroundAppCompatImageView.isVisible = true
                if (wasfiltersVisible) {
                    showEffects()
                }
            }
        }

        RxBus.listen(RxEvent.DisplayCoHostRequestToHost::class.java)
            .subscribeAndObserveOnMainThread {
                openUpdateCoHostStatusDialog(it.liveEventInfo)
            }.autoDispose()

        updateTimerText(0.secondToTime())

        binding.tagTextView.throttleClicks().subscribeAndObserveOnMainThread {
//            val bottomSheetFragment = LiveWatchBottomSheetFragment(liveId)
//            bottomSheetFragment.show(
//                supportFragmentManager,
//                LiveWatchBottomSheetFragment::class.java.name
//            )
        }

        liveEventCommentAdapter = LiveStreamingCommentAdapter(this).apply {
            commentClicks.subscribeAndObserveOnMainThread {
                if (it.userId != loggedInUserCache.getLoggedInUserId()) {
                    if (it.type != MessageType.JoinF.toString()) {
                        if (it.userId != firstCoHostUid && it.userId != secondCoHostUid && it.userId != thirdCoHostUid && it.userId != fourthCoHostUid) {
                            openLiveUserBottomSheet(it.userId, it.liveId)
                        } else {
                            startActivity(
                                MyProfileActivity.getIntentWithData(
                                    this@LiveStreamingActivity,
                                    it.userId
                                )
                            )
                        }
                    }
                }
            }
        }

        binding.commentRecyclerView.apply {
            val linearLayoutManager =
                LinearLayoutManager(this@LiveStreamingActivity, RecyclerView.VERTICAL, false)
            linearLayoutManager.stackFromEnd = true
            layoutManager = linearLayoutManager
            adapter = liveEventCommentAdapter
        }

        binding.cameraFlipAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {

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

        }.autoDispose()

        binding.commentEditTextView.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEND }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this)
                if (binding.commentEditTextView.text.toString().isNotEmpty()) {
                    liveStreamUserViewModel.sendComment(binding.commentEditTextView.text.toString())
                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "live")
                    props.put(Constant.CONTENT_ID, liveId)

                    mp?.track(Constant.COMMENT_CONTENT, props)
                }
                binding.commentEditTextView.setText("")
            }.autoDispose()

        binding.settingsAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val bottomSheetFragment =
                InviteUserInLiveStreamingBottomSheet.newInstance(isAllowPlayGame)
            bottomSheetFragment.inviteUpdated.subscribeAndObserveOnMainThread {
                liveStreamUserViewModel.inviteCoHost(liveId, it)
                bottomSheetFragment.dismissBottomSheet()
            }.autoDispose()
            bottomSheetFragment.show(
                supportFragmentManager,
                InviteUserInLiveStreamingBottomSheet::class.java.name
            )
        }.autoDispose()

        binding.sendImageView.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            if (binding.commentEditTextView.text.toString().isNotEmpty()) {
                liveStreamUserViewModel.sendComment(binding.commentEditTextView.text.toString())
                val props = JSONObject()
                props.put(Constant.CONTENT_TYPE, "live")
                props.put(Constant.CONTENT_ID, liveId)

                mp?.track(Constant.COMMENT_CONTENT, props)
            }
            binding.commentEditTextView.setText("")
            binding.settingAppCompatImageView.isVisible = true
            binding.settingsAppCompatImageView.isVisible = true
            binding.cameraFlipAppCompatImageView.isVisible = true
            binding.flMuteUnMute.isVisible = true

            if (secondCoHost || thirdCoHost || fourthCoHost) {
                val layoutParams: RelativeLayout.LayoutParams =
                    binding.llFooter.layoutParams as RelativeLayout.LayoutParams
                layoutParams.setMargins(0, 0, 0, 0)
                layoutParams.addRule(RelativeLayout.BELOW, R.id.clLiveContainer)
                binding.ivCommentShadow.isVisible = true
            }
        }.autoDispose()

        binding.exitAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.likeFloatingActionButton.throttleClicks().subscribeAndObserveOnMainThread {
            liveStreamUserViewModel.sendHeart(isAllowPlayGame)
        }.autoDispose()

        binding.commentEditTextView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.commentEditTextView.requestFocus()) {
                binding.settingAppCompatImageView.isVisible = false
                binding.settingsAppCompatImageView.isVisible = false
                binding.cameraFlipAppCompatImageView.isVisible = false
                binding.flMuteUnMute.isVisible = false

                wasPlayGameVisible = binding.tvPlayGame.isVisible
                wasfiltersVisible = binding.typeOfEffectsRecyclerView.isVisible
                hideEffects(10)
                val location = IntArray(2)
                binding.llFooter.getLocationOnScreen(location)
            }
        }.autoDispose()

        binding.commentEditTextView.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.settingAppCompatImageView.isVisible = false
                binding.settingsAppCompatImageView.isVisible = false
                binding.cameraFlipAppCompatImageView.isVisible = false
                binding.flMuteUnMute.isVisible = false
            } else {
                binding.settingAppCompatImageView.isVisible = true
                if (!isGameStarted && !binding.tvPlayGame.isVisible) {
                    binding.settingsAppCompatImageView.isVisible = true
                }
                binding.cameraFlipAppCompatImageView.isVisible = true
                binding.flMuteUnMute.isVisible = true
            }
        }


        KeyboardVisibilityEvent.setEventListener(
            this
        ) {
            if (it) {
                binding.settingAppCompatImageView.isVisible = false
                binding.settingsAppCompatImageView.isVisible = false
                binding.cameraFlipAppCompatImageView.isVisible = false
                binding.flMuteUnMute.isVisible = false

                binding.maskFlipAppCompatImageView.isVisible = false
                binding.filtersFlipAppCompatImageView.isVisible = false
                binding.effectsFlipAppCompatImageView.isVisible = false
                binding.backgroundAppCompatImageView.isVisible = false


            } else {
                binding.settingAppCompatImageView.isVisible = true
                if (!isGameStarted && !binding.tvPlayGame.isVisible) {
                    binding.settingsAppCompatImageView.isVisible = true
                }
                binding.cameraFlipAppCompatImageView.isVisible = true
                binding.flMuteUnMute.isVisible = true

                binding.maskFlipAppCompatImageView.isVisible = true
                binding.filtersFlipAppCompatImageView.isVisible = true
                binding.effectsFlipAppCompatImageView.isVisible = true
                binding.backgroundAppCompatImageView.isVisible = true
                if (wasfiltersVisible) {
                    showEffects()
                }
            }
        }

        engine = RtcEngine.create(this, getString(R.string.agora_app_id), iRtcEngineEventHandler)
        openLiveStreamCreateEventSettingBottomSheet()

        binding.settingAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            openMoreOptionBottomSheet()
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

        binding.cvSecondMore.throttleClicks().subscribeAndObserveOnMainThread {
            liveStreamUserViewModel.removeCoHostFromLive(
                StartGameRequestRequest(
                    roomId = channelId,
                    liveId = liveId,
                    userId = secondCoHostUid
                )
            )
            // openLiveUserBottomSheet(secondCoHostUid, liveId)

        }.autoDispose()

        binding.cvThirddMore.throttleClicks().subscribeAndObserveOnMainThread {
            //  openLiveUserBottomSheet(thirdCoHostUid, liveId)
            liveStreamUserViewModel.removeCoHostFromLive(
                StartGameRequestRequest(
                    roomId = channelId,
                    userId = thirdCoHostUid
                )
            )
        }.autoDispose()

        binding.cvFourMore.throttleClicks().subscribeAndObserveOnMainThread {
            // openLiveUserBottomSheet(fourthCoHostUid, liveId)
            liveStreamUserViewModel.removeCoHostFromLive(
                StartGameRequestRequest(
                    roomId = channelId,
                    userId = fourthCoHostUid
                )
            )
        }.autoDispose()

        binding.tvPlayGame.throttleClicks().subscribeAndObserveOnMainThread {

            liveStreamUserViewModel.wantToPlayLiveGame(
                StartGameRequestRequest(
                    userId = secondCoHostUid,
                    liveId = liveId,
                    isVerified = loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified
                )
            )
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
    }

    private fun openLiveUserBottomSheet(userId: Int?, liveId: Int?) {
        val bottomSheet = LiveUserOptionBottomSheet()
        bottomSheet.liveUserClicks.subscribeAndObserveOnMainThread { state ->
            when (state) {
                LiveUserOption.KickOut -> {
                    openKickOutDialog(userId, liveId)
                }

                LiveUserOption.Restrict -> {
                    liveStreamUserViewModel.kickOutUserFromLive(
                        LiveEventKickUser(
                            userId = userId,
                            actionType = Constant.MESSAGE_TYPE_RESTRICT,
                            seconds = 0,
                            roomId = liveId,
                            restrictBy = loggedInUserId
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
            liveStreamUserViewModel.kickOutUserFromLive(
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

    private fun openMoreOptionBottomSheet() {
        val bottomSheet = LiveStreamingMoreOptionBottomSheet.newInstance(
            isFromShorts = false,
            isFromLive = true,
            true
        )
        bottomSheet.optionClick.subscribeAndObserveOnMainThread {
            when (it) {
                resources.getString(R.string.label_share) -> {
                    ShareHelper.shareDeepLink(
                        this,
                        3,
                        liveId,
                        "", true
                    ) {
                        ShareHelper.shareText(this, it)

                        val props = JSONObject()
                        props.put(Constant.CONTENT_TYPE, "live")
                        props.put(Constant.CONTENT_ID, liveId)

                        mp?.track(Constant.SHARE_CONTENT, props)
                    }
                }

                resources.getString(R.string.report) -> {
                    val reportDialog = ReportDialogFragment.newInstance(liveId, true, LIVE)
                    reportDialog.optionClick.subscribeAndObserveOnMainThread {
                        val props = JSONObject()
                        props.put(Constant.CONTENT_TYPE, "live")
                        props.put(Constant.CONTENT_ID, liveId)

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

    private fun joinChannel(channelId: String, accessToken: String) {

        // Create render view by RtcEngine
        val surfaceView = RtcEngine.CreateRendererView(this)
        if (binding.coHostFirstVideo.childCount > 0) {
            binding.coHostFirstVideo.removeAllViews()
        }
        // Add to the local container
        binding.coHostFirstVideo.addView(
            surfaceView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        binding.coHostFirstVideo.addView(
            binding.firstCoHostRelativeLayout,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        updateUIForSingleHost()
        binding.firstCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
        firstCoHost = true
        // Setup local video to render your local camera preview
        engine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        // Set audio route to microPhone
        engine?.setDefaultAudioRoutetoSpeakerphone(true)
        engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        /**In the demo, the default is to enter as the anchor. */
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
        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
        option.autoSubscribeVideo = true
        val res = engine?.joinChannel(
            accessToken,
            channelId,
            option.toString(),
            loggedInUserCache.getLoggedInUserId()
        )
        if (res != 0) {
            // Usually happens with invalid parameters
            // Error code description can be found at:
            // en: https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html
            res?.let { RtcEngine.getErrorDescription(abs(res)) }?.let { showLongToast(it) }
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
            liveEventCommentAdapter.listOfDataItems = listOfLiveEventSendOrReadComment
            liveEventCommentAdapter.notifyDataSetChanged()

        }
    }

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
            firstCoHostUid = uid
            joined = true
            renderer!!.isCallInProgress = true
            welcomeMessage()
        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)

            if (!firstCoHost) {
                liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.FirstCoHost.type
                firstCoHost = true
                handler?.post {
                    if (binding.coHostFirstVideo.childCount > 0) {
                        binding.coHostFirstVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@LiveStreamingActivity)
                    surfaceView.setZOrderMediaOverlay(true)
                    // Add to the remote container
                    binding.coHostFirstVideo.addView(
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
                            uid
                        )
                    )
                    engine?.setupLocalVideo(
                        VideoCanvas(
                            surfaceView,
                            VideoCanvas.RENDER_MODE_HIDDEN,
                            0
                        )
                    )
                    binding.coHostFirstVideo.visibility = View.VISIBLE
                    binding.coHostFirstVideo.addView(
                        binding.firstCoHostRelativeLayout,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    binding.firstCoHostRelativeLayout.setVerticalGravity(Gravity.TOP)
                    updateUIForSingleHost()
                    if (isAllowPlayGame == 1) {
                        binding.tvPlayGame.isVisible = false
                    }

                }
                return
            }
            if (!secondCoHost) {
                liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.SecondCoHost.type
                secondCoHost = true
                secondCoHostUid = uid
                handler?.post {
                    if (binding.coHostSecondVideo.childCount > 0) {
                        binding.coHostSecondVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@LiveStreamingActivity)
                    surfaceView.setZOrderMediaOverlay(true)
                    // Add to the remote container
                    binding.coHostSecondVideo.addView(
                        surfaceView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )

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
                        if (isAllowPlayGame == 1) {
                            binding.tvPlayGame.isVisible =
                                binding.coHostThirdVideo.visibility != View.VISIBLE
                        }
                    }
                    binding.settingsAppCompatImageView.isVisible = true

                }
                return
            }
            if (!thirdCoHost) {
                liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.ThirdCoHost.type
                thirdCoHost = true
                thirdCoHostUid = uid
                handler?.post {
                    if (binding.coHostThirdVideo.childCount > 0) {
                        binding.coHostThirdVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@LiveStreamingActivity)
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
                            uid
                        )
                    )
                    updateUIForMultipleHost()

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
                        if (isAllowPlayGame == 1) {
                            binding.tvPlayGame.isVisible = false
                        }
                    }
                    binding.settingsAppCompatImageView.isVisible = true
                }
                return
            }
            if (!fourthCoHost) {
                liveStreamNoOfCoHostHashMap[uid] = LiveStreamNoOfCoHost.FourthCoHost.type
                fourthCoHost = true
                fourthCoHostUid = uid
                handler?.post {
                    if (binding.coHostFourVideo.childCount > 0) {
                        binding.coHostFourVideo.removeAllViews()
                    }
                    // Create render view by RtcEngine
                    val surfaceView = RtcEngine.CreateRendererView(this@LiveStreamingActivity)
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
                    engine?.setupRemoteVideo(
                        VideoCanvas(
                            surfaceView,
                            VideoCanvas.RENDER_MODE_HIDDEN,
                            uid
                        )
                    )
                    updateUIForMultipleHost()

                    if (!binding.llSecondAndFourthCoHost.isVisible) {
                        binding.llSecondAndFourthCoHost.isVisible = true
                    }
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
                        if (isAllowPlayGame == 1) {
                            binding.tvPlayGame.isVisible = false
                        }
                    }

                }
                return
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                binding.settingsAppCompatImageView.isVisible = true
            }
            if (isAllowPlayGame == 1) {
                handler?.postDelayed({
                    if (isGameStarted) {
                        liveStreamUserViewModel.endGameInLive(
                            StartGameRequestRequest(
                                channelId,
                                liveId,
                                uid
                            )
                        )
                    }
                }, 30000)
            }


            handler?.post {
                liveStreamNoOfCoHostHashMap.forEach { (key, value) ->
                    if (key == uid) {
                        if (value == LiveStreamNoOfCoHost.FirstCoHost.type) {
                            firstCoHost = false
                            handler?.post {
                                if (binding.coHostFirstVideo.childCount > 0) {
                                    binding.coHostFirstVideo.removeAllViews()
                                }
                                binding.coHostFirstVideo.visibility = View.GONE

                                if (isAllowPlayGame == 1) {
                                    binding.tvPlayGame.isVisible = false
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

                                if (isAllowPlayGame == 1) {
                                    binding.tvPlayGame.isVisible = false
                                }
                            }
                        }
                        if (value == LiveStreamNoOfCoHost.ThirdCoHost.type) {
                            thirdCoHost = false
                            handler?.post {
                                if (binding.coHostThirdVideo.childCount > 0) {
                                    binding.coHostThirdVideo.removeAllViews()
                                }
                                binding.coHostThirdVideo.visibility = View.GONE
                            }

                            if (isAllowPlayGame == 1) {
                                binding.tvPlayGame.isVisible = true
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
                                if (isAllowPlayGame == 1) {
                                    binding.tvPlayGame.isVisible = false
                                }
                            }
                        }

                    }
                    handler?.postDelayed({
                        if (!binding.coHostThirdVideo.isVisible && !binding.coHostFourVideo.isVisible && !binding.coHostSecondVideo.isVisible) {
                            updateUIForSingleHost()
                        }
                    }, 1000)
                }
                engine?.setupRemoteVideo(VideoCanvas(null, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            }
        }

        override fun onClientRoleChanged(oldRole: Int, newRole: Int) {
        }
    }

    private fun openLiveStreamCreateEventSettingBottomSheet() {
        val bottomSheet = StartLiveStreamingSettingsBottomSheet().apply {
            liveNowSuccess.subscribeAndObserveOnMainThread {
                channelId = it.channelId
                liveId = it.id
                isAllowPlayGame = it.isAllowPlayGame ?: 0

                val props = JSONObject()

                if (isAllowPlayGame.equals(1))
                    props.put("content_type", "Live with game")
                else
                    props.put("content_type", "Normal Live")


                mp?.track("Create Content", props)


                liveStreamUserViewModel.updateChannelId(channelId, liveId, isAllowPlayGame)

                joinChannel(it.channelId, it.token)
                setupCamera()
                setup()
                dismissBottomSheet()

            }
            closeIconClick.subscribeAndObserveOnMainThread {
              onBackPressedDispatcher.onBackPressed()
            }
        }
        bottomSheet.show(
            supportFragmentManager,
            StartLiveStreamingSettingsBottomSheet::class.java.name
        )
    }

    private fun startTimer() {
        var countTime = 300
        countTimerDisposable?.dispose()
        countTimerDisposable =
            Observable.interval(1, TimeUnit.SECONDS).subscribeAndObserveOnMainThread {
                countTime--
                updateTimerText(countTime.secondToTime())

                if (countTime == 0) {
                    if (isGameStarted) {
                        liveStreamUserViewModel.endGameInLive(
                            StartGameRequestRequest(
                                channelId,
                                liveId,
                                loggedInUserCache.getLoggedInUserId()
                            )
                        )
                    }
                }

                if (countTime < 0) {
                    countTimerDisposable?.dispose()
                    binding.tvGameTimer.isVisible = false
                    binding.winProgressIndicator.isVisible = false

                }
            }


    }

    override fun onBackPressed() {
        if (isEndingStream) {
            super.onBackPressed()
        } else {
            streamEndingConfirmation()
        }
    }

    private fun updateTimerText(secondToTime: Time) {
        val minString = secondToTime.min.toString().padStart(2, '0')
        val secondString = secondToTime.second.toString().padStart(2, '0')
        binding.tvGameTimer.text =
            resources.getString(R.string.label_vs).plus(" ")
                .plus(minString.plus(":").plus(secondString))
    }

    private fun streamEndingConfirmation() {

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.label_live_stream))
            .setMessage(getString(R.string.msg_are_you_sure_do_want_end_this_event))
            .setNegativeButton(getString(R.string.label_no)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.label_yes)) { dialog, _ ->
                dialog.dismiss()
                liveStreamUserViewModel.endLiveEvent()
            }.show()
    }

    //leaveChannel and Destroy the RtcEngine instance
    override fun onDestroy() {
        if (isAllowPlayGame == 1) {
            if (isGameStarted) {

                liveStreamUserViewModel.endGameInLive(
                    StartGameRequestRequest(
                        channelId,
                        liveId,
                        loggedInUserCache.getLoggedInUserId()
                    )
                )
            }

        }
        liveStreamUserViewModel.sendLiveEndEvent()
        engine?.leaveChannel()
        handler?.post { RtcEngine.destroy() }
        countTimerDisposable?.dispose()
        engine = null
        liveSummaryInfo?.let {
            it.centsValue = centsValue
            RxBus.publish(RxEvent.DisplayLiveSummary(it))
        }
        super.onDestroy()

        deepAR!!.release()

        val props = JSONObject()
        props.put(Constant.SCREEN_TYPE, "live")

        mp?.track(Constant.SCREEN_TIME, props)
        RtcEngine.destroy()
    }

    override fun onResume() {
        super.onResume()

        if (liveId != -1) {
            if (socketDataManager.isConnected) {
                liveStreamUserViewModel.joinLiveRoom(isAllowPlayGame)
            } else {
                handler?.postDelayed({
                    liveStreamUserViewModel.joinLiveRoom(isAllowPlayGame)
                }, 3000)
            }
        }

        RxBus.listen(RxEvent.CloseLive::class.java).subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        RxBus.listen(RxEvent.CloseLiveAndStartVideoCall::class.java)
            .subscribeAndObserveOnMainThread {
                finish()
            }.autoDispose()
        if (surfaceView != null) {
            setupCamera()
            surfaceView!!.onResume()
        }

    }

    private fun manageStartGameAnimation() {
        gameCount += 1
        binding.tvPlayGame.isVisible = false
        binding.tvGameTimer.isVisible = true
        binding.llGameUser1Info.isVisible = true
        binding.ivGameStarted.isVisible = true
        binding.winProgressIndicator.isVisible = true
        binding.llUser1WinContainer.isVisible = true
        binding.llUser2WinContainer.isVisible = true
        binding.tvGiftCountUser1.isVisible = true
        binding.tvGiftCountUser2.isVisible = true
        startTimer()

        binding.tvSelfUserName.text =
            loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName.toString()
        Glide.with(this)
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto)
            .error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivSelfProfileImage)


        val anim: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        binding.llGameUser1Info.isVisible = true
        binding.llGameUser1Info.startAnimation(anim)

        binding.llGameUser2Info.isVisible = true
        binding.llGameUser2Info.startAnimation(anim)

        binding.ivGameStarted.isVisible = true
        binding.ivGameStarted.startAnimation(anim)

        val handler = Handler()
        handler.postDelayed({ binding.llGameUser2Info.visibility = View.GONE }, 5000)
        handler.postDelayed({ binding.llGameUser1Info.visibility = View.GONE }, 5000)
        handler.postDelayed({ binding.ivGameStarted.visibility = View.GONE }, 5000)

    }

    private fun updateUIForSingleHost() {
        runOnUiThread {
            val layoutParams: RelativeLayout.LayoutParams =
                binding.llFooter.layoutParams as RelativeLayout.LayoutParams
            layoutParams.removeRule(RelativeLayout.BELOW)

            val layoutParamss: RelativeLayout.LayoutParams =
                binding.clLiveContainer.layoutParams as RelativeLayout.LayoutParams
            layoutParamss.removeRule(RelativeLayout.BELOW)

            binding.llMainLiveStreaming.background = resources.getDrawable(R.color.color_transparent)

            binding.clLiveContainer.layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
            binding.clLiveContainer.requestLayout()
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
    }

    private fun updateUIForMultipleHost() {

        runOnUiThread {
            binding.clLiveContainer.layoutParams.height =
                resources.getDimensionPixelSize(R.dimen._300sdp)
            binding.clLiveContainer.requestLayout()

            val layoutParams: RelativeLayout.LayoutParams =
                binding.llFooter.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.BELOW, R.id.clLiveContainer)
            binding.ivCommentShadow.isVisible = true

            val layoutParamss: RelativeLayout.LayoutParams =
                binding.clLiveContainer.layoutParams as RelativeLayout.LayoutParams
            layoutParamss.addRule(RelativeLayout.BELOW, R.id.winProgressIndicator)

            binding.llMainLiveStreaming.background = resources.getDrawable(R.color.color_FF141A26)

            val layoutParams2: RelativeLayout.LayoutParams =
                binding.llUser1ReceiveGiftInfo.layoutParams as RelativeLayout.LayoutParams
            layoutParams2.removeRule(RelativeLayout.CENTER_VERTICAL)
            layoutParams2.addRule(RelativeLayout.ABOVE, R.id.cvFirstMore)

            val layoutParams3: RelativeLayout.LayoutParams =
                binding.llUser2ReceiveGiftInfo.layoutParams as RelativeLayout.LayoutParams
            layoutParams3.removeRule(RelativeLayout.CENTER_VERTICAL)
            layoutParams3.addRule(RelativeLayout.ABOVE, R.id.llSecondCoHostData)
        }

    }

    private fun manageEndGame(endGameInfo: GameResultInfo) {

        isGameStarted = false
        binding.tvGameTimer.isVisible = false
        binding.winProgressIndicator.isVisible = false
        binding.settingsAppCompatImageView.isVisible = true
        binding.llUser1ContributorImage.isVisible = false
        binding.flUser2ContributorImage.isVisible = false
        binding.tvGiftCountUser1.isVisible = false
        binding.tvGiftCountUser2.isVisible =false


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

        val handler = Handler()
        handler.postDelayed({
            binding.llUser1GameResult.isVisible = false
            binding.llUser2GameResult.isVisible = false
            binding.llDrawGameResult.isVisible = false
            binding.llUser1WinContainer.isVisible = false
            binding.llUser2WinContainer.isVisible = false
            binding.winProgressIndicator.progress = 50

            val data = endGameInfo.gameResult?.filter { it.userId == loggedInUserId }
            data?.forEach {
                it.centsValue = centsValue
            }
            val bottomSheet = data?.let { GameResultBottomSheet.newInstance(it.first()) }
            handler.postDelayed({ bottomSheet?.dismiss() }, 10000)
            bottomSheet?.show(supportFragmentManager, GameResultBottomSheet::class.java.name)

            if (binding.coHostSecondVideo.isVisible) {
                binding.tvPlayGame.text = getString(R.string.label_play_again)
            }

        }, 5000)
    }

    private fun openUpdateCoHostStatusDialog(data: LiveEventInfo) {
        if (!isGameStarted) {
            val dialog = LiveStreamAcceptRejectDialog(data, isFrom = LivePopupType.JoinLive)
            dialog.apply {
                isCancelable = false
                inviteCoHostStatus.subscribeAndObserveOnMainThread {

                    if (it) {
                        dismissDialog()
                        finish()
                        startActivity(
                            LiveRoomActivity.getIntent(
                                this@LiveStreamingActivity,
                                liveId = data.id,
                                isJoin = false
                            )
                        )

                    } else {
                        dismissDialog()
                    }

                }
            }
            dialog.show(supportFragmentManager, LiveStreamAcceptRejectDialog::class.java.name)
        }

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

        } else if (count1 == 0.0 && count2 == 0.0) {
            binding.winProgressIndicator.progress = 50
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
        grantResults: IntArray,
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
        val cameraPreset = CameraResolutionPreset.P1920x1080
        val width: Int
        val height: Int
        val orientation = getScreenOrientation()
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            width = cameraPreset.width
            height = cameraPreset.height
        } else {
            width = cameraPreset.height
            height = cameraPreset.width
        }
        val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(width, height))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(this)
        ) { image ->
            try {
                val yBuffer = image.planes[0].buffer
                val uBuffer = image.planes[1].buffer
                val vBuffer = image.planes[2].buffer
                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()
                val imageBufferSize = ySize + uSize + vSize
                if (allocatedBufferSize < imageBufferSize) {
                    initializeBuffers(imageBufferSize)
                }
                val byteData = ByteArray(imageBufferSize)
                val imgWidth = image.width
                val yStride = image.planes[0].rowStride
                val uStride = image.planes[1].rowStride
                val vStride = image.planes[2].rowStride
                var outputOffset = 0
                if (imgWidth == yStride) {
                    yBuffer[byteData, outputOffset, ySize]
                    outputOffset += ySize
                } else {
                    var inputOffset = 0
                    while (inputOffset < ySize) {
                        yBuffer.position(inputOffset)
                        yBuffer[byteData, outputOffset, Math.min(yBuffer.remaining(), imgWidth)]
                        outputOffset += imgWidth
                        inputOffset += yStride
                    }
                }
                //U and V are swapped
                if (imgWidth == vStride) {
                    vBuffer[byteData, outputOffset, vSize]
                    outputOffset += vSize
                } else {
                    var inputOffset = 0
                    while (inputOffset < vSize) {
                        vBuffer.position(inputOffset)
                        vBuffer[byteData, outputOffset, Math.min(vBuffer.remaining(), imgWidth)]
                        outputOffset += imgWidth
                        inputOffset += vStride
                    }
                }
                if (imgWidth == uStride) {
                    uBuffer[byteData, outputOffset, uSize]
                    outputOffset += uSize
                } else {
                    var inputOffset = 0
                    while (inputOffset < uSize) {
                        uBuffer.position(inputOffset)
                        uBuffer[byteData, outputOffset, Math.min(uBuffer.remaining(), imgWidth)]
                        outputOffset += imgWidth
                        inputOffset += uStride
                    }
                }
                buffers!![currentBuffer]!!.put(byteData)
                buffers!![currentBuffer]!!.position(0)
                if (deepAR != null) {
                    deepAR!!.receiveFrame(
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

            } catch (e: Exception) {
                // Handle the exception here
                e.printStackTrace()
                // You might want to log the exception or take appropriate action
            }
        }

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle((this as LifecycleOwner), cameraSelector, imageAnalysis)
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
        val pathNone: String? = null
        deepAR?.switchEffect("mask", pathNone)
    }

    override fun faceVisibilityChanged(b: Boolean) {}

    override fun imageVisibilityChanged(s: String?, b: Boolean) {}

    override fun frameAvailable(image: Image?) {}

    override fun error(arErrorType: ARErrorType?, s: String?) {}

    override fun effectSwitched(s: String?) {}

    private fun hideEffects(time: Long) {
        if (!isEffectSelected) {
            isHide = true
            handler?.postDelayed({
                if (isHide) {
                    val layoutParams: RelativeLayout.LayoutParams =
                        binding.llFooter.layoutParams as RelativeLayout.LayoutParams
                    layoutParams.removeRule(RelativeLayout.ABOVE)
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)


                    val layoutParams1: RelativeLayout.LayoutParams =
                        binding.otherPart.layoutParams as RelativeLayout.LayoutParams
                    layoutParams1.removeRule(RelativeLayout.ABOVE)
                    layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

                    binding.typeOfEffectsRecyclerView.visibility = View.GONE
                }
            }, time)
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


            val layoutParams1: RelativeLayout.LayoutParams =
                binding.otherPart.layoutParams as RelativeLayout.LayoutParams
            layoutParams1.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            layoutParams1.addRule(RelativeLayout.ABOVE, R.id.typeOfEffectsRecyclerView)


            binding.typeOfEffectsRecyclerView.visibility = View.VISIBLE

        }
        hideEffects(5000)
    }

}