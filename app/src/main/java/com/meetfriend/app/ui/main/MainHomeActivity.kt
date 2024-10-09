package com.meetfriend.app.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.icu.util.Currency
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.SwitchDeviceAccountRequest
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.StoryUploadData
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.api.notification.model.AdType
import com.meetfriend.app.api.notification.model.AppConfigInfo
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.api.post.model.LinkAttachmentDetails
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.connectivity.base.ConnectivityProvider
import com.meetfriend.app.databinding.ActivityMainHomeBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.startActivityWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.ui.activities.FullScreenActivity
import com.meetfriend.app.ui.activities.WelcomeActivity
import com.meetfriend.app.ui.camerakit.SnapkitActivity
import com.meetfriend.app.ui.challenge.ChallengeDetailsActivity
import com.meetfriend.app.ui.challenge.ChallengeDetailsActivity.Companion.INTENT_CHALLENGE_ID
import com.meetfriend.app.ui.challenge.CreateChallengeActivity
import com.meetfriend.app.ui.chat.onetoonechatroom.ViewOneToOneChatRoomActivity
import com.meetfriend.app.ui.chatRoom.notification.ChatRoomNotificationActivity
import com.meetfriend.app.ui.chatRoom.profile.ChatRoomProfileActivity
import com.meetfriend.app.ui.follow.request.FollowRequestActivity
import com.meetfriend.app.ui.home.ProgressDialogFragment
import com.meetfriend.app.ui.home.create.AddNewPostActivity
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.shortdetails.ShortDetailsActivity
import com.meetfriend.app.ui.home.shortdetails.ShortDetailsActivity.Companion.INTENT_IS_COMMENT
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewModel.Companion.mainHomeStateSubject
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewState
import com.meetfriend.app.ui.livestreaming.videoroom.LiveRoomActivity
import com.meetfriend.app.ui.main.view.MainHomeTabAdapter
import com.meetfriend.app.ui.main.viewmodel.HomeViewModel
import com.meetfriend.app.ui.main.viewmodel.HomeViewState
import com.meetfriend.app.ui.messages.MessageListActivity
import com.meetfriend.app.ui.messages.create.CreateMessageActivity
import com.meetfriend.app.ui.mygifts.GiftsTransactionActivity
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.ui.search.SearchListActivity
import com.meetfriend.app.ui.trends.TrendsFlowActivity
import com.meetfriend.app.utils.Constant
import contractorssmart.app.utilsclasses.PreferenceHandler
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainHomeActivity : BasicActivity(), ConnectivityProvider.ConnectivityStateListener {

    private lateinit var binding: ActivityMainHomeBinding
    private lateinit var mainHomeTabAdapter: MainHomeTabAdapter
    private val cameraPermissionRequestCodeForCreatePost: Int = CAMERA_PERMISSION_REQUEST_CODE
    private val cameraPermissionRequestCodeForCreateShorts: Int = CAMERA_PERMISSION_REQUEST_CODE_FOR_CREATE_SHORTS
    private val cameraPermissionRequestCodeForCreateChallenge: Int = CAMERA_PERMISSION_REQUEST_CODE_FOR_CREATE_CHALLENGE
    private var isProgressStart: Boolean = true

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        private const val LIVE_EVENT_INFO = "LIVE_EVENT_INFO"
        private const val STORY_VIDEO_FILE_PATH = "STORY_VIDEO_FILE_PATH"
        private const val CLOUD_FLARE_CONFIG = "CLOUD_FLARE_CONFIG"
        private const val VIDEO_DURATION = "VIDEO_DURATION"
        private const val EXTRA_INITIAL_TAB_INDEX = "EXTRA_INITIAL_TAB_INDEX"
        private const val POST_ID = "POST_ID"
        const val INTENT_IS_SWITCH_ACCOUNT = "INTENT_SWITCH_ACCOUNT"
        const val USER_ID = "USER_ID"
        const val LINK_ATTACHMENT_DETAILS = "LINK_ATTACHMENT_DETAILS"
        const val VIDEO_HEIGHT = "VIDEO_HEIGHT"
        const val VIDEO_WIDTH = "VIDEO_WIDTH"
        const val MUSIC_INFO = "MUSIC_INFO"
        private const val CAMERA_PERMISSION_REQUEST_CODE: Int = 10002
        private const val CAMERA_PERMISSION_REQUEST_CODE_FOR_CREATE_SHORTS: Int = 10003
        private const val CAMERA_PERMISSION_REQUEST_CODE_FOR_CREATE_CHALLENGE: Int = 10004
        private const val REFRESH_FOR_YOU_FRAGMENT: Int = 1000
        private const val INTERVAL_FETCH_LOCATION: Int = 6000
        private const val FASTEST_INTERVAL_FETCH_LOCATION: Int = 4000
        private const val START_RESOLUTION_FOR_RESULT: Int = 2001
        private const val MAX_COUNT = 10

        private const val HOME_POSITION = 0
        private const val SHORTS_POSITION = 1
        private const val TRENDS_POSITION = 2
        private const val CHALLENGE_POSITION = 3
        private const val MESSAGE_POSITION = 4
        private const val MORE_POSITION = 5
        private const val REQUEST_CODE = 1002

        fun getIntent(context: Context, isLogin: Boolean? = true): Intent {
            val intent = Intent(context, MainHomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("IS_LOGIN", isLogin)
            return intent
        }

        fun getIntentFrom(context: Context, initialTab: Int): Intent {
            val intent = Intent(context, MainHomeActivity::class.java)
            intent.putExtra(EXTRA_INITIAL_TAB_INDEX, initialTab)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }

        fun launchFromPostNotification(
            context: Context,
            postId: Int,
            isComment: Boolean? = false,
            isLogin: Boolean,
            toUserId: Int
        ): Intent {
            val intent = Intent(context, ShortDetailsActivity::class.java).apply {
                putExtra(POST_ID, postId)
                putExtra(INTENT_IS_COMMENT, isComment)
                putExtra(INTENT_IS_SWITCH_ACCOUNT, !isLogin)
                putExtra(USER_ID, toUserId)
            }
            return intent
        }

        fun launchProfileFromFollowNotification(
            context: Context,
            userId: Int,
            isLogin: Boolean,
            toUserId: Int
        ): Intent {
            val intent = Intent(context, MyProfileActivity::class.java)
            intent.putExtra(MyProfileActivity.USER_ID, userId)
            intent.putExtra(INTENT_IS_SWITCH_ACCOUNT, !isLogin)
            intent.putExtra(USER_ID, toUserId)
            return intent
        }

        fun launchFromFollowNotification(
            context: Context,
            isLogin: Boolean,
            toUserId: Int
        ): Intent {
            val intent = Intent(context, FollowRequestActivity::class.java)
            intent.putExtra(INTENT_IS_SWITCH_ACCOUNT, !isLogin)
            intent.putExtra(USER_ID, toUserId)
            return intent
        }

        fun launchFromMessageNotification(
            context: Context,
            chatRoomInfo: ChatRoomInfo,
            isLogin: Boolean,
            toUserId: Int
        ): Intent {
            val intent = Intent(context, ViewOneToOneChatRoomActivity::class.java)
            intent.putExtra(ViewOneToOneChatRoomActivity.INTENT_CHAT_ROOM_INFO, chatRoomInfo)
            intent.putExtra(ViewOneToOneChatRoomActivity.INTENT_VIDEO_CALL, true)
            intent.putExtra(ViewOneToOneChatRoomActivity.INTENT_CAN_SEND_MESSAGE, true)
            intent.putExtra(INTENT_IS_SWITCH_ACCOUNT, !isLogin)
            intent.putExtra(USER_ID, toUserId)
            return intent
        }

        fun launchFromChatRequestNotification(
            context: Context,
            isLogin: Boolean,
            toUserId: Int
        ): Intent {
            val intent = Intent(context, ChatRoomNotificationActivity::class.java)
            intent.putExtra(INTENT_IS_SWITCH_ACCOUNT, !isLogin)
            intent.putExtra(USER_ID, toUserId)
            return intent
        }

        fun launchFromLiveCoHostNotification(
            context: Context,
            liveId: Int,
            isJoin: Boolean,
            isLogin: Boolean,
            toUserId: Int
        ): Intent {
            val intent = Intent(context, LiveRoomActivity::class.java)
            intent.putExtra(LiveRoomActivity.LIVE_ID, liveId)
            intent.putExtra(LiveRoomActivity.INTENT_IS_JOIN, isJoin)
            intent.putExtra(INTENT_IS_SWITCH_ACCOUNT, !isLogin)
            intent.putExtra(USER_ID, toUserId)
            return intent
        }

        fun launchGiftNotification(
            context: Context,
            isLogin: Boolean,
            toUserId: Int
        ): Intent {
            val intent = Intent(context, GiftsTransactionActivity::class.java)
            intent.putExtra(INTENT_IS_SWITCH_ACCOUNT, !isLogin)
            intent.putExtra(USER_ID, toUserId)
            return intent
        }

        fun launchFromChallengeNotification(
            context: Context,
            postId: Int,
            isLogin: Boolean,
            toUserId: Int
        ): Intent {
            val intent = Intent(context, ChallengeDetailsActivity::class.java)
            intent.putExtra(INTENT_CHALLENGE_ID, postId)
            intent.putExtra(INTENT_IS_SWITCH_ACCOUNT, !isLogin)
            intent.putExtra(USER_ID, toUserId)
            return intent
        }

        fun ConnectivityProvider.NetworkState.hasInternet(): Boolean {
            return (this as? ConnectivityProvider.NetworkState.ConnectedState)?.hasInternet == true
        }

        fun getIntentFromStoryUpload(
            context: Context,
            uploadData: StoryUploadData
        ): Intent {
            return Intent(context, MainHomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(CLOUD_FLARE_CONFIG, uploadData.cloudFlareConfig)
                putExtra(STORY_VIDEO_FILE_PATH, uploadData.videoPath)
                putExtra(VIDEO_DURATION, uploadData.videoDuration)
                putExtra(LINK_ATTACHMENT_DETAILS, uploadData.linkAttachmentDetails)
                putExtra(MUSIC_INFO, uploadData.musicResponse)
                putExtra(VIDEO_HEIGHT, uploadData.videoHeight)
                putExtra(VIDEO_WIDTH, uploadData.videoWidth)
            }
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HomeViewModel>
    private lateinit var homeViewModel: HomeViewModel

    private var snackbar: Snackbar? = null
    private lateinit var progressDialogFragment: ProgressDialogFragment
    private var videoDuration: String = ""
    private var getLocation: Boolean = true
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var isLogin = false
    private var linkAttachmentDetails: LinkAttachmentDetails? = null
    private var musicResponse: MusicInfo? = null
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainHomeBinding.inflate(layoutInflater)
        val view = binding.root
        MeetFriendApplication.component.inject(this)
        homeViewModel = getViewModelFromFactory(viewModelFactory)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@MainHomeActivity)
        setContentView(view)
        isLogin = loggedInUserCache.getLoginUserToken() != null
        notificationPermission()
        listenToViewModel()
        initUI()
        checkInternet()
        if (!loggedInUserCache.getHomeLocationPermissionAsked()) {
            locationPermission()
        }
        homeViewModel.getAppConfig()
        intent?.let {
            if (!it.getBooleanExtra("IS_LOGIN", true)) {
                binding.bottomTab.selectTab(binding.bottomTab.getTabAt(1))
                io.reactivex.Observable
                    .timer(REFRESH_FOR_YOU_FRAGMENT.toLong(), TimeUnit.MILLISECONDS)
                    .subscribeAndObserveOnMainThread {
                        RxBus.publish(RxEvent.RefreshForYouPlayFragment)
                    }.autoDispose()
            }
        }
        val deviceId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        RxBus.listen(RxEvent.SwitchAccount::class.java).subscribeAndObserveOnMainThread {
            homeViewModel.switchAccount(SwitchDeviceAccountRequest(deviceId, it.userId))
        }.autoDispose()
    }

    private fun checkInternet() {
        isConnectedToInternet()
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                snackbar!!.dismiss()
            }

            override fun onLost(network: Network) {
                snackbar!!.show()
            }
        })

        if (isConnectedToInternet()) {
            snackbar!!.dismiss()
        } else {
            snackbar!!.show()
        }
    }

    private fun getLocation() {
        XXPermissions.with(this@MainHomeActivity)
            .permission(Permission.ACCESS_COARSE_LOCATION)
            .permission(Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermissionCallback {
                override fun onGranted(
                    permissions: List<String>,
                    all: Boolean
                ) {
                    if (all) {
                        try {
                            val locationRequest = LocationRequest.create().apply {
                                interval = INTERVAL_FETCH_LOCATION.toLong()
                                fastestInterval = FASTEST_INTERVAL_FETCH_LOCATION.toLong()
                                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                            }
                            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
                            builder.setAlwaysShow(true)
                            val task: Task<LocationSettingsResponse> =
                                LocationServices
                                    .getSettingsClient(this@MainHomeActivity)
                                    .checkLocationSettings(builder.build())
                            task.addOnCompleteListener { task ->
                                try {
                                    task.getResult(ApiException::class.java)!!
                                    // All location settings are satisfied. The client can initialize location
                                    // requests here.
                                    if (ContextCompat.checkSelfPermission(
                                            this@MainHomeActivity, Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        fusedLocationProviderClient?.requestLocationUpdates(
                                            locationRequest,
                                            object : LocationCallback() {
                                                override fun onLocationResult(locationResult: LocationResult) {
                                                    getAddress(locationResult)
                                                }
                                            },
                                            Looper.getMainLooper()
                                        )
                                    }
                                } catch (exception: ApiException) {
                                    when (exception.statusCode) {
                                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                                            try {
                                                val resolvable = exception as ResolvableApiException
                                                resolvable.startResolutionForResult(
                                                    this@MainHomeActivity,
                                                    START_RESOLUTION_FOR_RESULT
                                                )
                                            } catch (e: IntentSender.SendIntentException) {
                                                // Ignore the error.
                                            } catch (e: ClassCastException) {
                                                // Ignore, should be an impossible error.
                                            }

                                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onDenied(
                    permissions: List<String>,
                    never: Boolean
                ) {
                    if (never) {
                        showToast("Location permission is permanently denied, please manually grant permissions")
                        XXPermissions.startPermissionActivity(this@MainHomeActivity, permissions)
                    } else {
                        showToast(getString(R.string.msg_permission_denied))
                    }
                }
            })
    }

    private fun getAddress(locationResult: LocationResult) {
        try {
            if (getLocation) {
                val geo = Geocoder(this@MainHomeActivity, Locale.getDefault())
                var addresses: MutableList<Address> = arrayListOf()
                val lat = locationResult.lastLocation?.latitude
                val long = locationResult.lastLocation?.longitude
                if (lat != null && long != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geo.getFromLocation(lat, long, 1) { addressesList ->
                            addresses = addressesList
                        }
                    } else {
                        addresses = geo.getFromLocation(
                            lat,
                            long,
                            1,
                        ) as MutableList<Address>
                    }
                    if (addresses.isNotEmpty()) {
                        PreferenceHandler.writeString(
                            this@MainHomeActivity,
                            "countryCode",
                            addresses[0].countryCode
                        )
                        homeViewModel.getExchangeRate()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isConnectedToInternet(): Boolean {
        val connectivityManager = applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val activeNetwork = network?.let { connectivityManager.getNetworkCapabilities(it) }
        return activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ||
            activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
    }

    private fun listenToViewModel() {
        homeViewModel.homeState.subscribeAndObserveOnMainThread {
            when (it) {
                is HomeViewState.ErrorMessage -> handleErrorMessage(it.errorMessage)
                is HomeViewState.CloudFlareErrorMessage -> handleCloudFlareError(it.errorMessage)
                is HomeViewState.StoryLoadingState -> showLoading(it.isLoading)
                is HomeViewState.GetNotificationCount -> setNotificationCount(it.response)
                is HomeViewState.ExchangeRate -> handleExchangeRate(it)
                is HomeViewState.AppConfigInfo -> setUpAppConfigInfo(it.data)
                is HomeViewState.UploadVideoCloudFlareSuccess -> uploadVideoCloudFlarePerform(it)
                is HomeViewState.AddStoryResponse -> handleAddStoryResponse(it)
                is HomeViewState.SwitchAccount -> initUI()
                else -> {}
            }
        }.autoDispose()
    }

    private fun handleErrorMessage(errorMessage: String) {
        showToast(errorMessage)
    }

    private fun handleCloudFlareError(errorMessage: String) {
        showToast(errorMessage)
        hideLoading()
    }

    private fun handleExchangeRate(it: HomeViewState.ExchangeRate) {
        val currencyCode: String
        var data = PreferenceHandler.readString(this, "countryCode", "")
        if (data == "") {
            data = Locale.getDefault().toString()
            currencyCode = Currency.getInstance(Locale(data)).currencyCode ?: ""
        } else {
            currencyCode = Currency.getInstance(Locale("", data)).currencyCode ?: ""
        }
        val exchangeRate = it.data?.rates?.get(currencyCode) ?: 1.0
        Constant.EXCHANGE_RATE = exchangeRate.toString()
    }

    private fun handleAddStoryResponse(it: HomeViewState.AddStoryResponse) {
        hideLoading()
        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        mainHomeStateSubject.onNext(MainHomeViewState.AddStoryResponse(it.message))
        if (::progressDialogFragment.isInitialized) {
            progressDialogFragment.progressState.subscribeAndObserveOnMainThread {
                progressDialogFragment.dismiss()
            }.autoDispose()
        }
    }

    private fun uploadVideoCloudFlarePerform(homeViewState: HomeViewState.UploadVideoCloudFlareSuccess) {
        if (isProgressStart) {
            if (linkAttachmentDetails != null && !linkAttachmentDetails?.attachUrl.isNullOrEmpty()) {
                val listOfPositionArray = arrayListOf<Float>()
                listOfPositionArray.add(linkAttachmentDetails?.finalX ?: 0.0F)
                listOfPositionArray.add(linkAttachmentDetails?.finalY ?: 0.0F)
                listOfPositionArray.add(linkAttachmentDetails?.lastWidth ?: 0.0F)
                listOfPositionArray.add(linkAttachmentDetails?.lastHeight ?: 0.0F)

                if (musicResponse != null) {
                    val addStoryRequest = AddStoryRequest(
                        type = "video",
                        image = null,
                        uid = homeViewState.videoId,
                        position = listOfPositionArray.toString(),
                        rotation = linkAttachmentDetails?.lastRotation,
                        webUrl = linkAttachmentDetails?.attachUrl,
                        height = videoHeight,
                        width = videoWidth,
                        musicTitle = musicResponse?.name,
                        artists = getArtistsNames(),
                        duration = videoDuration
                    )
                    homeViewModel.addStoryVideo(addStoryRequest)
                } else {
                    val addStoryRequest = AddStoryRequest(
                        type = "video",
                        image = null,
                        uid = homeViewState.videoId,
                        position = listOfPositionArray.toString(),
                        rotation = linkAttachmentDetails?.lastRotation,
                        webUrl = linkAttachmentDetails?.attachUrl,
                        height = videoHeight,
                        width = videoWidth,
                        musicTitle = null,
                        artists = null,
                        duration = videoDuration
                    )
                    homeViewModel.addStoryVideo(addStoryRequest)
                }
            } else {
                if (musicResponse != null) {
                    val addStoryRequest = AddStoryRequest(
                        type = "video",
                        image = null,
                        uid = homeViewState.videoId,
                        position = null,
                        rotation = null,
                        webUrl = null,
                        height = videoHeight,
                        width = videoWidth,
                        musicTitle = musicResponse?.name,
                        artists = getArtistsNames(),
                        duration = videoDuration
                    )
                    homeViewModel.addStoryVideo(addStoryRequest)
                } else {
                    val addStoryRequest = AddStoryRequest(
                        type = "video",
                        image = null,
                        uid = homeViewState.videoId,
                        position = null,
                        rotation = null,
                        webUrl = null,
                        height = videoHeight,
                        width = videoWidth,
                        musicTitle = null,
                        artists = null,
                        duration = videoDuration
                    )
                    homeViewModel.addStoryVideo(addStoryRequest)
                }
            }
        }
    }

    private fun setUpAppConfigInfo(it: List<AppConfigInfo>?) {
        it?.forEach { configInfo ->
            when (configInfo.type) {
                AdType.NativeAdId.toString() -> {
                    loggedInUserCache.setNativeAdId(configInfo.value)
                }

                AdType.BannerAdId.toString() -> {
                    loggedInUserCache.setBannerAdId(configInfo.value)
                }

                AdType.InterstitialAdId.toString() -> {
                    loggedInUserCache.setInterstitialAdId(configInfo.value)
                }

                AdType.AppOpenAdId.toString() -> {
                    loggedInUserCache.setAppOpenAdId(configInfo.value)
                }

                AdType.InterstitialAdScrollCount.toString() -> {
                    loggedInUserCache.setScrollCountAd(configInfo.value)
                }

                AdType.InterstitialAdClickCount.toString() -> {
                    loggedInUserCache.setClickCountAd(configInfo.value)
                }
            }
        }
    }

    private fun setNotificationCount(response: MeetFriendCommonResponse) {
        loggedInUserCache.setPlaceApiKey(response.placeApiKey)
        val notificationCount: Int? = response.result
        val messageUnreadCount: Int? = response.totalMessageUnreadCount
        if (notificationCount!! > 0) {
            binding.countAppCompatTextView.visibility = View.VISIBLE

            if (notificationCount < MAX_COUNT) {
                binding.countAppCompatTextView.text = notificationCount.toString()
            } else {
                binding.countAppCompatTextView.text = "9+"
            }
        } else {
            binding.countAppCompatTextView.visibility = View.GONE
        }

        if (messageUnreadCount!! > 0) {
            binding.messageCountAppCompatTextView.visibility = View.VISIBLE

            if (messageUnreadCount < MAX_COUNT) {
                binding.messageCountAppCompatTextView.text = messageUnreadCount.toString()
            } else {
                binding.messageCountAppCompatTextView.text = "9+"
            }
        } else {
            binding.messageCountAppCompatTextView.visibility = View.GONE
        }
        if (response.isLive != null) binding.tvLiveBadge.isVisible = response.isLive > 0
    }

    private fun getArtistsNames(): String? {
        return musicResponse?.artists?.all?.joinToString(separator = ", ") { it?.name ?: "" }
    }

    override fun onStart() {
        super.onStart()
        firebaseDynamicLink()
    }

    private fun firebaseDynamicLink() {
        Firebase.dynamicLinks.getDynamicLink(intent).addOnSuccessListener(this) { linkData ->
            linkData?.link?.let { link ->
                when {
                    link.toString().contains("challenge") -> handleChallengeLink(link)
                    link.toString().contains("images_share") -> handleImageShareLink(link)
                    link.toString().contains("post") -> handlePostLink(link)
                    link.toString().contains("user") -> handleUserLink(link)
                    link.toString().contains("live") -> handleLiveLink(link)
                    link.toString().contains("reels") -> handleReelsLink(link)
                    link.toString().contains("chat_profile") -> handleChatProfileLink(link)
                }
            }
        }.addOnFailureListener {
            // Handle failure
        }
    }

    private fun handleChallengeLink(link: Uri) {
        val challengeId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        challengeId?.let {
            startActivity(ChallengeDetailsActivity.getIntent(this, it))
        }
    }

    private fun handleImageShareLink(link: Uri) {
        val url: String = link.toString()
        val imageUrl = url.substringAfter("images_share/")
        val intent = Intent(this, FullScreenActivity::class.java)
        intent.putExtra("url", imageUrl)
        this.startActivity(intent)
    }

    private fun handlePostLink(link: Uri) {
        val postId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        postId?.let {
            startActivity(ShortDetailsActivity.getIntent(this, it))
        }
    }

    private fun handleUserLink(link: Uri) {
        val userId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        userId?.let {
            startActivity(MyProfileActivity.getIntentWithData(this, it))
        }
    }

    private fun handleLiveLink(link: Uri) {
        val liveId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        liveId?.let {
            startActivity(LiveRoomActivity.getIntent(this, null, it))
        }
    }

    private fun handleReelsLink(link: Uri) {
        val postId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        postId?.let {
            startActivity(ShortDetailsActivity.getIntent(this, it))
        }
    }

    private fun handleChatProfileLink(link: Uri) {
        val userId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        userId?.let {
            startActivity(ChatRoomProfileActivity.getIntent(this, it))
        }
    }

    private fun initUI() {
        handleIntentExtras()
        setupMainHomeTabAdapter()
        setupSnackbar()
        setupViewPager()
        setupBottomNavigation()
        setupClickListeners()
        setupRxBus()
        binding.menu.isVisible = loggedInUserCache.getLoginUserToken() != null
    }

    private fun handleIntentExtras() {
        if (intent.hasExtra(STORY_VIDEO_FILE_PATH)) {
            handleStoryVideo()
        }
        handleLiveEvent()
    }

    private fun handleStoryVideo() {
        val cloudFlareConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(CLOUD_FLARE_CONFIG, CloudFlareConfig::class.java)
        } else {
            intent?.getParcelableExtra(CLOUD_FLARE_CONFIG)
        }

        videoDuration = intent?.getStringExtra(VIDEO_DURATION).toString()
        val storyVideo = File(intent?.getStringExtra(STORY_VIDEO_FILE_PATH).toString())
        linkAttachmentDetails = intent.getParcelableExtra(LINK_ATTACHMENT_DETAILS)
        musicResponse = intent.getParcelableExtra(MUSIC_INFO)
        videoHeight = intent.getIntExtra(VIDEO_HEIGHT, 0)
        videoWidth = intent.getIntExtra(VIDEO_WIDTH, 0)

        if (cloudFlareConfig != null && isProgressStart) {
            uploadVideoToCloudFlare(cloudFlareConfig, storyVideo)
        }
    }

    private fun uploadVideoToCloudFlare(cloudFlareConfig: CloudFlareConfig, storyVideo: File) {
        homeViewModel.uploadVideoToCloudFlare(this@MainHomeActivity, cloudFlareConfig, storyVideo)
        progressDialogFragment = ProgressDialogFragment.newInstance("story")
        progressDialogFragment.progressState.subscribeAndObserveOnMainThread {
            progressDialogFragment.dismiss()
            showLoading()
        }.autoDispose()
        progressDialogFragment.progressCancelState.subscribeAndObserveOnMainThread {
            isProgressStart = it
            progressDialogFragment.dismiss()
        }.autoDispose()
        progressDialogFragment.show(supportFragmentManager, ProgressDialogFragment::class.java.name)
    }

    private fun handleLiveEvent() {
        intent?.getParcelableExtra<LiveEventInfo>(LIVE_EVENT_INFO)?.let {
            startActivity(
                if (it.isLock == 1) {
                    LiveRoomActivity.getIntent(this)
                } else {
                    LiveRoomActivity.getIntent(this, it)
                }
            )
        }
    }

    private fun setupMainHomeTabAdapter() {
        mainHomeTabAdapter = MainHomeTabAdapter(this)
    }

    private fun setupSnackbar() {
        val view = findViewById<View>(android.R.id.content)
        snackbar = Snackbar.make(view, "No internet connection", Snackbar.LENGTH_INDEFINITE)
    }

    private fun setupViewPager() {
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = MORE_POSITION.toInt()
        binding.viewPager.adapter = mainHomeTabAdapter

        val initialTabIndex = intent.getIntExtra(EXTRA_INITIAL_TAB_INDEX, 0)
        if (loggedInUserCache.getLoginUserToken() != null) {
            binding.viewPager.setCurrentItem(initialTabIndex, false)
            handleTabSelection(initialTabIndex)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                openInterstitialAds()
                Constant.CLICK_COUNT++
                val position = tab.position
                handleTabSelection(position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Called when a tab exits the selected state
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Called when a tab that is already selected is chosen again
            }
        })
    }

    private fun setupClickListeners() {
        binding.createAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            displayPopupMenu()
        }.autoDispose()

        setupIndividualClickListeners()
    }

    private fun setupIndividualClickListeners() {
        binding.liveAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.createLinearLayout.visibility = View.GONE
            startActivity(LiveRoomActivity.getIntent(this))
        }.autoDispose()

        // Repeat for other image views...
        binding.trendsAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.createLinearLayout.visibility = View.GONE
            startActivity(TrendsFlowActivity.getIntent(this@MainHomeActivity))
        }.autoDispose()

        binding.messageAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.createLinearLayout.visibility = View.GONE
            startActivity(MessageListActivity.getIntent(this@MainHomeActivity))
        }.autoDispose()

        binding.notificationRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            binding.createLinearLayout.visibility = View.GONE
            startActivityWithDefaultAnimation(ChatRoomNotificationActivity.getIntent(this))
        }.autoDispose()

        binding.searchAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(SearchListActivity.getIntent(this@MainHomeActivity))
        }.autoDispose()

        binding.videoPostAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.createPostLinearLayout.visibility = View.GONE
            startActivity(AddNewPostActivity.launchActivity(this, "POST_TYPE_POST_VIDEO"))
        }.autoDispose()

        binding.post.throttleClicks().subscribeAndObserveOnMainThread {
            openCreatePost()
        }.autoDispose()

        binding.shorts.throttleClicks().subscribeAndObserveOnMainThread {
            openCreateShorts()
        }.autoDispose()

        binding.challenges.throttleClicks().subscribeAndObserveOnMainThread {
            openCreateChallenge()
        }.autoDispose()
    }

    private fun setupRxBus() {
        RxBus.listen(RxEvent.MessageUnreadCount::class.java).subscribeAndObserveOnMainThread {
            if (isLogin) homeViewModel.getNotificationCount()
        }.autoDispose()
    }

    @SuppressLint("MissingInflatedId")
    private fun displayPopupMenu() {
        val layoutInflater = layoutInflater
        val popupView: View = layoutInflater.inflate(R.layout.popup_home_create, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val post: LinearLayout = popupView.findViewById(R.id.llPost)
        val shorts: LinearLayout = popupView.findViewById(R.id.llShorts)
        val challenge: LinearLayout = popupView.findViewById(R.id.llChalleng)

        popupWindow.isOutsideTouchable = true

        post.setOnClickListener {
            openCreatePost()
            popupWindow.dismiss()
        }

        shorts.setOnClickListener {
            openCreateShorts()
            popupWindow.dismiss()
        }

        challenge.setOnClickListener {
            openCreateChallenge()
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(binding.createAppCompatImageView)
    }

    private fun openLoginPopup() {
        val loginBottomSheet = WelcomeActivity.newInstance()
        loginBottomSheet.show(supportFragmentManager, WelcomeActivity::class.java.name)
    }

    private fun handleTabSelection(tabPosition: Int) {
        when (tabPosition) {
            HOME_POSITION -> handleHomeTabSelection()
            SHORTS_POSITION -> handleVideoTabSelection()
            TRENDS_POSITION -> handleTabSelectionWithLoginCheck(TRENDS_POSITION)
            CHALLENGE_POSITION -> handleTabSelectionWithLoginCheck(CHALLENGE_POSITION)
            MESSAGE_POSITION -> handleNumberFourTabSelection()
            MORE_POSITION -> handleNumberFiveTabSelection()
        }
    }

    private fun handleHomeTabSelection() {
        if (isLogin) {
            showHomeUI()
            RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(true))
            binding.viewPager.setCurrentItem(HOME_POSITION, false)
        } else {
            openLoginPopup()
            binding.bottomTab.getTabAt(1)?.select()
        }
    }

    private fun handleVideoTabSelection() {
        binding.viewPager.setCurrentItem(SHORTS_POSITION, false)
        binding.toolbar.visibility = View.GONE
        binding.bottomTab.getTabAt(SHORTS_POSITION)?.select()
    }

    private fun handleTabSelectionWithLoginCheck(position: Int) {
        if (isLogin) {
            binding.viewPager.setCurrentItem(position, false)
            binding.toolbar.visibility = View.GONE
        } else {
            openLoginPopup()
            binding.bottomTab.getTabAt(SHORTS_POSITION)?.select()
        }
    }

    private fun handleNumberFourTabSelection() {
        if (isLogin) {
            binding.viewPager.setCurrentItem(MESSAGE_POSITION, false)
            binding.toolbar.visibility = View.VISIBLE
            configureMessageUI()
        } else {
            openLoginPopup()
            binding.bottomTab.getTabAt(SHORTS_POSITION)?.select()
        }
    }

    private fun handleNumberFiveTabSelection() {
        if (isLogin) {
            binding.toolbar.visibility = View.VISIBLE
            binding.flLive.isVisible = false
            binding.createAppCompatImageView.isVisible = false
            binding.createMessageAppCompatImageView.isVisible = false
            binding.notificationRelativeLayout.isVisible = true
            binding.viewPager.setCurrentItem(MORE_POSITION, false)
        } else {
            openLoginPopup()
            binding.bottomTab.getTabAt(SHORTS_POSITION)?.select()
        }
    }

    private fun showHomeUI() {
        binding.toolbar.visibility = View.VISIBLE
        binding.flLive.isVisible = true
        binding.createAppCompatImageView.isVisible = true
        binding.notificationRelativeLayout.isVisible = true
        binding.createMessageAppCompatImageView.isVisible = false
    }

    private fun configureMessageUI() {
        binding.flLive.isVisible = false
        binding.createAppCompatImageView.isVisible = false
        binding.notificationRelativeLayout.isVisible = false
        binding.createMessageAppCompatImageView.isVisible = true
        binding.createMessageAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(CreateMessageActivity.getIntentWithDataS(this@MainHomeActivity, "CreateChat", ""))
        }
    }

    private fun openCreatePost() {
        val cameraPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            )
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            )
        }

        val permissionsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        }

        val missingPermissions = permissionsToCheck.filter {
            ContextCompat.checkSelfPermission(this@MainHomeActivity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this@MainHomeActivity,
                cameraPermissions,
                cameraPermissionRequestCodeForCreatePost
            )
        } else {
            val intent = AddNewPostInfoActivity.launchActivity(
                LaunchActivityData(this@MainHomeActivity,
                postType = AddNewPostInfoActivity.CREATE_POST,
                imagePathList = arrayListOf()
                )
            )
            startActivity(intent)
        }
    }

    private fun openCreateShorts() {
        binding.createLinearLayout.visibility = View.GONE
        val cameraPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            )
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            )
        }

        val permissionsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        }

        val missingPermissions = permissionsToCheck.filter {
            ContextCompat.checkSelfPermission(this@MainHomeActivity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this@MainHomeActivity,
                cameraPermissions,
                cameraPermissionRequestCodeForCreateShorts
            )
        } else {
            val intent = SnapkitActivity.getIntent(this@MainHomeActivity, true)
            startActivity(intent)
        }
    }

    private fun openCreateChallenge() {
        val cameraPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            )
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            )
        }

        val permissionsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        }

        val missingPermissions = permissionsToCheck.filter {
            ContextCompat.checkSelfPermission(this@MainHomeActivity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this@MainHomeActivity,
                cameraPermissions,
                cameraPermissionRequestCodeForCreateChallenge
            )
        } else {
            startActivity(CreateChallengeActivity.getIntent(this@MainHomeActivity))
        }
    }

    override fun onResume() {
        super.onResume()
        getLocation = true
        if (isLogin) homeViewModel.getNotificationCount()
    }

    override fun onPause() {
        super.onPause()
        getLocation = false
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.POST_NOTIFICATIONS)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    // Permission granted; you can add any necessary logic here
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    // Handle denied permissions here, if necessary
                }
            })
    }

    private fun notificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_DENIED
        ) {
            checkPermissionGranted(this)
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        val hasInternet = state.hasInternet()
        if (snackbar != null) {
            if (hasInternet) {
                snackbar!!.dismiss()
            } else {
                snackbar!!.show()
            }
        }
    }

    private fun locationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            getLocation()
            loggedInUserCache.setHomeLocationPermissionAsked(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(SnapkitActivity.getIntent(this, isStory = true))
            } else {
                showToast("Permission denied!")
            }
        } else if (requestCode == cameraPermissionRequestCodeForCreatePost) {
            val intent = AddNewPostInfoActivity.launchActivity(
                LaunchActivityData(this@MainHomeActivity,
                postType = AddNewPostInfoActivity.CREATE_POST,
                imagePathList = arrayListOf()
                )
            )
            startActivity(intent)
        } else if (requestCode == cameraPermissionRequestCodeForCreateShorts) {
            val intent = SnapkitActivity.getIntent(this@MainHomeActivity, true)
            startActivity(intent)
        } else if (requestCode == cameraPermissionRequestCodeForCreateChallenge) {
            startActivity(CreateChallengeActivity.getIntent(this@MainHomeActivity))
        }
    }
}
