package com.meetfriend.app.ui.chatRoom.videoCall
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.JoinAgoraSDKVoiceCallChannel
import com.meetfriend.app.api.notification.model.VoipCallRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityOneToOneVideoCallBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.videoCall.viewmodel.VideoCallViewModel
import com.meetfriend.app.ui.chatRoom.videoCall.viewmodel.VideoCallViewState
import com.meetfriend.app.utils.Constant
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.RtcEngineConfig
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtc.video.VideoCanvas
import timber.log.Timber
import javax.inject.Inject


class OneToOneVideoCallActivity : BasicActivity() {

    companion object {
        private const val INTENT_CHAT_ROOM_INFO = "INTENT_CHAT_ROOM_INFO"
        private const val INTENT_IS_CALLER = "INTENT_IS_CALLER"


        fun getIntent(context: Context, chatRoomInfo: ChatRoomInfo?, isCaller: Boolean): Intent {
            val intent = Intent(context, OneToOneVideoCallActivity::class.java)
            intent.putExtra(INTENT_CHAT_ROOM_INFO, chatRoomInfo)
            intent.putExtra(INTENT_IS_CALLER, isCaller)
            return intent
        }
    }

    lateinit var binding: ActivityOneToOneVideoCallBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VideoCallViewModel>
    private lateinit var videoCallViewModel: VideoCallViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var agoraEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private var isJoined = false
    private lateinit var chatRoomInfo: ChatRoomInfo
    lateinit var builder: NotificationCompat.Builder
    var chatRoomId = 0
    private var isCaller: Boolean = false
    private var isRemoteUserJoined: Boolean = false
    private var isFrom: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RxBus.publish(RxEvent.CloseLiveAndStartVideoCall)

        binding = ActivityOneToOneVideoCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissionGranted(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        MeetFriendApplication.component.inject(this)
        videoCallViewModel = getViewModelFromFactory(viewModelFactory)

        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey("INTENT_CHAT_ROOM_ID")) {
                chatRoomId = extras.getInt("INTENT_CHAT_ROOM_ID")
                val action = extras.getString("action")
                this.isFrom = extras.getInt("isFrom")

                if (isFrom == 1) {
                    agoraEngine?.leaveChannel()
                    if (remoteSurfaceView != null) remoteSurfaceView?.visibility = View.GONE
                } else {
                    remoteSurfaceView?.visibility = View.VISIBLE
                }

                videoCallViewModel.setCurrentChatInfo(chatRoomId)
                videoCallViewModel.joinRoom()
                val manager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (action != null) {
                    manager.cancel(action.toInt())
                }

            }

        }

        intent?.let {
            if (it.hasExtra(INTENT_CHAT_ROOM_INFO)) {
                it.getParcelableExtra<ChatRoomInfo>(INTENT_CHAT_ROOM_INFO)?.let { item ->
                    this.chatRoomInfo = item
                    videoCallViewModel.setCurrentChatInfo(chatRoomInfo.id)
                }
                this.isCaller = it.getBooleanExtra(INTENT_IS_CALLER, false)
            }
        }

        if (isFrom != 1) {
            setupVideoSDKEngine()
        }

        videoCallViewModel.loadAgoraToken(Constants.CLIENT_ROLE_BROADCASTER)

        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewEvent() {

        binding.ivCall.throttleClicks().subscribeAndObserveOnMainThread {
            if (isCaller && !isRemoteUserJoined) {
                val friendId =
                    if (chatRoomInfo.receiverId == loggedInUserCache.getLoggedInUserId()) chatRoomInfo.userId else chatRoomInfo.receiverId
                videoCallViewModel.missedVoipCall(
                    VoipCallRequest(
                        friendId,
                        Constant.CALL_TYPE_VIDEO,
                        Constant.CALL_TYPE_MISSED,
                        Constant.AGORA_CHANNEL_NAME_PREFIX.plus(chatRoomInfo.id),
                        Constant.TOKEN
                    )
                )
            }
            videoCallViewModel.updateVideoCallEnded()
            leaveChannel()
        }.autoDispose()

        binding.ivCameraOption.throttleClicks().subscribeAndObserveOnMainThread {
            agoraEngine?.switchCamera()
        }.autoDispose()

        binding.ivMic.throttleClicks().subscribeAndObserveOnMainThread {
            binding.ivMicMute.isVisible = true
            binding.ivMic.isVisible = false
            agoraEngine?.muteLocalAudioStream(true)

        }.autoDispose()

        binding.ivMicMute.throttleClicks().subscribeAndObserveOnMainThread {
            binding.ivMicMute.isVisible = false
            binding.ivMic.isVisible = true

            agoraEngine?.muteLocalAudioStream(false)

        }.autoDispose()

        binding.ivVideo.throttleClicks().subscribeAndObserveOnMainThread {

        }.autoDispose()
    }

    private fun listenToViewModel() {
        videoCallViewModel.videoCallState.subscribeAndObserveOnMainThread {
            when (it) {
                is VideoCallViewState.AgoraSDKVoiceCallChannel -> {
                    if (isCaller) {
                        val friendId =
                            if (chatRoomInfo.receiverId == loggedInUserCache.getLoggedInUserId()) chatRoomInfo.userId else chatRoomInfo.receiverId
                        videoCallViewModel.startVoipCall(
                            VoipCallRequest(
                                friendId,
                                Constant.CALL_TYPE_VIDEO,
                                Constant.CALL_TYPE_CALLING,
                                Constant.AGORA_CHANNEL_NAME_PREFIX.plus(chatRoomInfo.id),
                                Constant.TOKEN
                            )
                        )
                    }
                    joinChannel(it.joinAgoraSDKVoiceCallChannel)
                }
                is VideoCallViewState.EndVideoCall -> {
                    finish()
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun checkPermissionGranted(context: Context) {
        XXPermissions.with(context)
            .permission(Permission.RECORD_AUDIO)
            .permission(Permission.CAMERA)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {
                        setupVideoSDKEngine()
                    } else {
                        checkPermissionGranted(this@OneToOneVideoCallActivity)
                    }
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast("Authorization is permanently denied, please manually grant permissions")
                    XXPermissions.startPermissionActivity(
                        this@OneToOneVideoCallActivity,
                        permissions
                    )
                }
            })
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = getString(R.string.new_agora_app_id)
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine?.enableVideo()

        } catch (e: Exception) {
            Timber.e(e.toString())
        }


    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Timber.i("Remote user joined $uid")

            // Set the remote video view
            agoraEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread { remoteSurfaceView?.visibility = View.GONE }
            finish()
        }
    }


    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(this)
        if (binding.flRemoteVideoContainer.childCount > 0) {
            binding.flRemoteVideoContainer.removeAllViews()
        }
        // Add to the local container
        binding.flRemoteVideoContainer.addView(
            surfaceView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        agoraEngine?.setupRemoteVideo(
            VideoCanvas(
                surfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                uid
            )
        )
        binding.tvRinging.visibility = View.GONE
    }

    private fun setupLocalVideo() {
        val surfaceView = RtcEngine.CreateRendererView(this)
        surfaceView?.setZOrderMediaOverlay(true)
        binding.flLocalVideoContainer.addView(surfaceView,FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT))
        agoraEngine?.setupLocalVideo(
            VideoCanvas(
                surfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )

    }

    private fun joinChannel(joinAgoraSDKVideoCallChannel: JoinAgoraSDKVoiceCallChannel) {
        val options = ChannelMediaOptions()
        agoraEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        agoraEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        setupLocalVideo()
        localSurfaceView?.visibility = View.VISIBLE
        agoraEngine?.startPreview()
        agoraEngine?.joinChannel(
            joinAgoraSDKVideoCallChannel.token,
            joinAgoraSDKVideoCallChannel.channelName,
            options.toString(),
            joinAgoraSDKVideoCallChannel.userId
        )
    }

    private fun leaveChannel() {
        if (!isJoined) {
        } else {
            agoraEngine?.leaveChannel()
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView?.visibility = View.GONE
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView?.visibility = View.GONE
            isJoined = false
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine?.stopPreview()
        agoraEngine?.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    override fun onResume() {
        super.onResume()

        RxBus.listen(RxEvent.DeclineCall::class.java).subscribeAndObserveOnMainThread {
            leaveChannel()
        }.autoDispose()

        RxBus.listen(RxEvent.CloseLive::class.java).subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        if (camera == 0 && audio == 0) {
            setupVideoSDKEngine()
        }
    }

}