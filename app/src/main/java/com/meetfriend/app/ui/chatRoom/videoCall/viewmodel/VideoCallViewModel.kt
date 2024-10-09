package com.meetfriend.app.ui.chatRoom.videoCall.viewmodel

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.JoinAgoraSDKVoiceCallChannel
import com.meetfriend.app.api.chat.model.JoinRoomRequest
import com.meetfriend.app.api.chat.model.VoiceCallEndSocketRequest
import com.meetfriend.app.api.notification.NotificationRepository
import com.meetfriend.app.api.notification.model.VoipCallRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.socket.SocketService
import com.meetfriend.app.utils.Constant
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import kotlin.properties.Delegates

class VideoCallViewModel(
    private val chatRepository: ChatRepository,
    private val loggedInUserCache: LoggedInUserCache,
    private val notificationRepository: NotificationRepository,
) : BasicViewModel() {

    private val videoCallStateSubject: PublishSubject<VideoCallViewState> = PublishSubject.create()
    val videoCallState: Observable<VideoCallViewState> = videoCallStateSubject.hide()

    private var chatRoomId by Delegates.notNull<Int>()

    init {
        observeVideoCallEnded()
    }

    fun loadAgoraToken(tokenRole: Int) {
        val channelName = Constant.AGORA_CHANNEL_NAME_PREFIX.plus(chatRoomId)
        chatRepository.getAgoraSDKToken(channelName, tokenRole)
            .subscribeOnIoAndObserveOnMainThread({
                videoCallStateSubject.onNext(
                    VideoCallViewState.AgoraSDKVoiceCallChannel(
                        JoinAgoraSDKVoiceCallChannel(
                            channelName = channelName,
                            loggedInUserCache.getLoggedInUserId(),
                            tokenRole,
                            it.rtcToken,
                            chatRoomId
                        )
                    )
                )
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun setCurrentChatInfo(chatRoomId: Int) {
        this.chatRoomId = chatRoomId
    }

    fun updateVideoCallEnded() {
        chatRepository.endVideoCall(VoiceCallEndSocketRequest(chatRoomId))
            .subscribeOnIoAndObserveOnMainThread({
            }, {
            }).autoDispose()
    }

    private fun observeVideoCallEnded() {
        chatRepository.observeVideoCallEnded().subscribeOnIoAndObserveOnMainThread({
            videoCallStateSubject.onNext(VideoCallViewState.EndVideoCall)
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun joinRoom() {
        chatRepository.joinRoom(JoinRoomRequest(chatRoomId))
            .subscribeOnIoAndObserveOnMainThread({
            }, {
            }).autoDispose()
    }

    override fun onCleared() {
        chatRepository.off(SocketService.EVENT_VIDEO_CALL_END_LISTEN)
        super.onCleared()
    }

    fun missedVoipCall(voipCallRequest: VoipCallRequest) {
        notificationRepository.startVoipCall(voipCallRequest).subscribeOnIoAndObserveOnMainThread({
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun startVoipCall(voipCallRequest: VoipCallRequest) {
        notificationRepository.startVoipCall(voipCallRequest).subscribeOnIoAndObserveOnMainThread({
            videoCallStateSubject.onNext(VideoCallViewState.ErrorMessage(it.message.toString()))
        }, {
            Timber.e(it)
        }).autoDispose()
    }
}

sealed class VideoCallViewState {
    data class ErrorMessage(val errorMessage: String) : VideoCallViewState()
    data class AgoraSDKVoiceCallChannel(val joinAgoraSDKVoiceCallChannel: JoinAgoraSDKVoiceCallChannel) :
        VideoCallViewState()

    object EndVideoCall : VideoCallViewState()
}
