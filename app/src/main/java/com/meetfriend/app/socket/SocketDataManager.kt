package com.meetfriend.app.socket

import com.meetfriend.app.api.chat.model.GetChatRoomAdminRequest
import com.meetfriend.app.api.chat.model.JoinChatRoomResponse
import com.meetfriend.app.api.chat.model.JoinRoomRequest
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.RestrictRequest
import com.meetfriend.app.api.chat.model.SeenMsgRequest
import com.meetfriend.app.api.chat.model.SendJoinChatRoomRequestRequest
import com.meetfriend.app.api.chat.model.SendMicAccessRequestSocketRequest
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.api.chat.model.TypingRequest
import com.meetfriend.app.api.chat.model.VoiceCallEndSocketRequest
import com.meetfriend.app.api.chat.model.VoiceCallStartSocketRequest
import com.meetfriend.app.api.gift.model.AcceptRejectGiftRequest
import com.meetfriend.app.api.livestreaming.model.CoHostListInfo
import com.meetfriend.app.api.livestreaming.model.CoHostSocketEvent
import com.meetfriend.app.api.livestreaming.model.GameResultInfo
import com.meetfriend.app.api.livestreaming.model.JoinLiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LeaveLiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LiveEventKickUser
import com.meetfriend.app.api.livestreaming.model.LiveEventSendOrReadComment
import com.meetfriend.app.api.livestreaming.model.LiveEventWatchingCount
import com.meetfriend.app.api.livestreaming.model.LiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LiveSummaryInfo
import com.meetfriend.app.api.livestreaming.model.ReceiveGiftInfo
import com.meetfriend.app.api.livestreaming.model.SendGiftInGameRequest
import com.meetfriend.app.api.livestreaming.model.SendHeartSocketEvent
import com.meetfriend.app.api.livestreaming.model.StartGameRequestRequest
import com.meetfriend.app.api.livestreaming.model.TopGifterInfo
import com.meetfriend.app.api.livestreaming.model.UpdateLiveViewerCountRequest
import io.reactivex.Completable
import io.reactivex.Observable
import org.json.JSONObject
import timber.log.Timber

class SocketDataManager(private val appSocket: SocketService) : Connectible {

    companion object {
        const val TAG: String = "SocketTag"
    }

    private val connectionEmitter by lazy {
        Observable.create<Unit> { emitter ->
            appSocket.on(SocketService.EVENT_CONNECT) {

                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_CONNECT}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Socket connected $it")
                emitter.onNext(Unit)
            }
        }.share()
    }

    private val connectionErrorEmitter by lazy {
        Observable.create<Unit> { emitter ->
            appSocket.on(SocketService.EVENT_CONNECT_ERROR) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_CONNECT_ERROR}")
                Timber.tag(SocketService.SOCKET_TAG).e("ON Error $it")
                emitter.onNext(Unit)
            }
        }.share()
    }

    private val disconnectEmitter by lazy {
        Observable.create<Unit> { emitter ->
            appSocket.on(SocketService.EVENT_DISCONNECT) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_DISCONNECT}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Socket disconnected $it")
                emitter.onNext(Unit)
            }
        }.share()
    }

    private val roomJoinEmitter by lazy {
        Observable.create<JoinChatRoomResponse> { emitter ->
            appSocket.on(SocketService.EVENT_ROOM_JOIN) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_ROOM_JOIN}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(JoinChatRoomResponse::class.java))
            }
        }.share()
    }

    private val messageEmitter by lazy {
        Observable.create<MessageInfo> { emitter ->
            appSocket.on(SocketService.EVENT_PRIVATE_MESSAGE) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_PRIVATE_MESSAGE}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(MessageInfo::class.java))
            }
        }.share()
    }

    private val joinRoomRequestEmitter by lazy {
        Observable.create<SendJoinChatRoomRequestRequest> { emitter ->
            appSocket.on(SocketService.EVENT_RECEIVE_JOIN_ROOM_REQUEST) {
                Timber.tag(
                    SocketService.SOCKET_TAG
                ).i("ON Event Name : ${SocketService.EVENT_RECEIVE_JOIN_ROOM_REQUEST}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(SendJoinChatRoomRequestRequest::class.java))
            }
        }.share()
    }

    private val kickedOutEmitter by lazy {
        Observable.create<MessageInfo> { emitter ->
            appSocket.on(SocketService.EVENT_KICKED_OUT) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_KICKED_OUT}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(MessageInfo::class.java))
            }
        }.share()
    }

    private val bannedUserEmitter by lazy {
        Observable.create<MessageInfo> { emitter ->
            appSocket.on(SocketService.EVENT_BANNED_USER) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_BANNED_USER}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(MessageInfo::class.java))
            }
        }.share()
    }

    private val voiceCallStartedEmitter by lazy {
        Observable.create<VoiceCallStartSocketRequest> { emitter ->
            appSocket.on(SocketService.EVENT_VOICE_CALL_START_LISTEN) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_VOICE_CALL_START_LISTEN}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(VoiceCallStartSocketRequest::class.java))
            }
        }.share()
    }

    private val voiceCallEndedEmitter by lazy {
        Observable.create<VoiceCallEndSocketRequest> { emitter ->
            appSocket.on(SocketService.EVENT_VOICE_CALL_END_LISTEN) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_VOICE_CALL_END_LISTEN}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(VoiceCallEndSocketRequest::class.java))
            }
        }.share()
    }

    private val restrictRequestEmitter by lazy {
        Observable.create<RestrictRequest> { emitter ->
            appSocket.on(SocketService.EVENT_RESTRICT_USER_LISTEN) {
                Timber.tag(SocketService.SOCKET_TAG)
                    .i("ON Event Name : ${SocketService.EVENT_RESTRICT_USER_LISTEN}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(RestrictRequest::class.java))
            }
        }
    }

    private val miceAccessRequestEmitter by lazy {
        Observable.create<SendMicAccessRequestSocketRequest> { emitter ->
            appSocket.on(SocketService.EVENT_RECEIVE_MICE_REQUESTED) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_RECEIVE_MICE_REQUESTED}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(SendMicAccessRequestSocketRequest::class.java))
            }
        }.share()
    }

    private val acceptedRequestEmitter by lazy {
        Observable.create<SendMicAccessRequestSocketRequest> { emitter ->
            appSocket.on(SocketService.EVENT_REQUEST_ACCEPTED) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_RECEIVE_MICE_REQUESTED}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(SendMicAccessRequestSocketRequest::class.java))
            }
        }.share()
    }

    private val revokeMicAccessEmitter by lazy {
        Observable.create<SendMicAccessRequestSocketRequest> { emitter ->
            appSocket.on(SocketService.EVENT_REVOKE_MIC_ACCESS) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_REVOKE_MIC_ACCESS}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(SendMicAccessRequestSocketRequest::class.java))
            }
        }.share()
    }

    private val addedNewRoomEmitter by lazy {
        Observable.create<LiveRoomRequest> { emitter ->
            appSocket.on(SocketService.EVENT_ADDED_NEW_ROOM) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_ADDED_NEW_ROOM}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveRoomRequest::class.java))
            }
        }.share()
    }

    private val liveWatchingCountEmitter by lazy {
        Observable.create<LiveEventWatchingCount> { emitter ->
            appSocket.on(SocketService.EVENT_UPDATE_LIVE_COUNTER) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_UPDATE_LIVE_COUNTER}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveEventWatchingCount::class.java))
            }
        }.share()
    }

    private val liveEndEmitter by lazy {
        Observable.create<LiveSummaryInfo> { emitter ->
            appSocket.on(SocketService.EVENT_LIVE_END) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_LIVE_END}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveSummaryInfo::class.java))
            }
        }.share()
    }

    private val kickedUserEmitter by lazy {
        Observable.create<LiveEventKickUser> { emitter ->
            appSocket.on(SocketService.EVENT_KICKED_USER) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_KICKED_USER}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveEventKickUser::class.java))
            }
        }.share()
    }

    private val restrictUserEmitter by lazy {
        Observable.create<LiveEventKickUser> { emitter ->
            appSocket.on(SocketService.EVENT_RESTRICT_USER) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_RESTRICT_USER}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveEventKickUser::class.java))
            }
        }.share()
    }
    private val sendCommentEmitter by lazy {
        Observable.create<LiveEventSendOrReadComment> { emitter ->
            appSocket.on(SocketService.EVENT_LIVE_NEW_MESSAGE) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_LIVE_NEW_MESSAGE}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveEventSendOrReadComment::class.java))
            }
        }.share()
    }

    private val liveHeartEmitter by lazy {
        Observable.create<SendHeartSocketEvent> { emitter ->
            appSocket.on(SocketService.EVENT_LIVE_HEART) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_LIVE_HEART}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(SendHeartSocketEvent::class.java))
            }
        }.share()
    }

    private val updateLiveCoHost by lazy {
        Observable.create<CoHostListInfo> { emitter ->
            appSocket.on(SocketService.EVENT_LIVE_CO_HOST) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_LIVE_CO_HOST}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(CoHostListInfo::class.java))
            }
        }.share()
    }

    private val videoCallEndedEmitter by lazy {
        Observable.create<VoiceCallEndSocketRequest> { emitter ->
            appSocket.on(SocketService.EVENT_VIDEO_CALL_END_LISTEN) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_VIDEO_CALL_END_LISTEN}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(VoiceCallEndSocketRequest::class.java))
            }
        }.share()
    }

    private val giftsRequestEmitter by lazy {
        Observable.create<MessageInfo> { emitter ->
            appSocket.on(SocketService.EVENT_ACCEPT_REJECT_GIFT_REQUEST) {
                Timber.tag(
                    SocketService.SOCKET_TAG
                ).i("ON Event Name : ${SocketService.EVENT_ACCEPT_REJECT_GIFT_REQUEST}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(MessageInfo::class.java))
            }
        }.share()
    }

    private val receiveGiftInGameEmitter by lazy {
        Observable.create<ReceiveGiftInfo> { emitter ->
            appSocket.on(SocketService.EVENT_RECEIVE_GIFT) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_RECEIVE_GIFT}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(ReceiveGiftInfo::class.java))
            }
        }.share()
    }

    private val topGifterInGameEmitter by lazy {
        Observable.create<TopGifterInfo> { emitter ->
            appSocket.on(SocketService.EVENT_TOP_GIFTER) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_TOP_GIFTER}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(TopGifterInfo::class.java))
            }
        }.share()
    }

    private val gameStartedEmitter by lazy {
        Observable.create<StartGameRequestRequest> { emitter ->
            appSocket.on(SocketService.EVENT_GAME_STARTED) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_GAME_STARTED}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(StartGameRequestRequest::class.java))
            }
        }.share()
    }

    private val gameEndedEmitter by lazy {
        Observable.create<GameResultInfo> { emitter ->
            appSocket.on(SocketService.EVENT_GAME_ENDED) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_GAME_ENDED}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(GameResultInfo::class.java))
            }
        }.share()
    }

    private val wantToPlayGameEmitter by lazy {
        Observable.create<StartGameRequestRequest> { emitter ->
            appSocket.on(SocketService.EVENT_WANT_TO_PLAY_GAME) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_WANT_TO_PLAY_GAME}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(StartGameRequestRequest::class.java))
            }
        }.share()
    }

    private val removeCoHostFromLiveEmitter by lazy {
        Observable.create<Unit> { emitter ->
            appSocket.on(SocketService.EVENT_REMOVE_CO_HOST_FROM_LIVE) {
                Timber.tag(
                    SocketService.SOCKET_TAG
                ).i("ON Event Name : ${SocketService.EVENT_REMOVE_CO_HOST_FROM_LIVE}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(Unit::class.java))
            }
        }.share()
    }

    private val typingEmitter by lazy {
        Observable.create<TypingRequest> { emitter ->
            appSocket.on(SocketService.EVENT_TYPING) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_TYPING}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(TypingRequest::class.java))
            }
        }.share()
    }

    private val stopTypingEmitter by lazy {
        Observable.create<TypingRequest> { emitter ->
            appSocket.on(SocketService.EVENT_STOP_TYPING) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_STOP_TYPING}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(TypingRequest::class.java))
            }
        }.share()
    }

    private val msgSeenEmitter by lazy {
        Observable.create<Unit> { emitter ->
            appSocket.on(SocketService.EVENT_MSG_SEEN) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_MSG_SEEN}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(Unit::class.java))
            }
        }.share()
    }

    private fun String.toJsonObject() = JSONObject(this)

    private fun <T> Array<Any>.getResponse(clazz: Class<T>): T {
        return appSocket.getGson().fromJson(this[0].toString(), clazz)
    }

    override val isConnected: Boolean get() = appSocket.isConnected
    override fun connect() = appSocket.connect()
    override fun connectionEmitter(): Observable<Unit> = connectionEmitter
    override fun connectionError(): Observable<Unit> = connectionErrorEmitter
    override fun disconnect() = appSocket.disconnect()
    override fun disconnectEmitter(): Observable<Unit> = disconnectEmitter
    override fun roomJoined(): Observable<JoinChatRoomResponse> = roomJoinEmitter
    override fun privateMessage(): Observable<MessageInfo> = messageEmitter
    override fun receiveJoinRoomRequest(): Observable<SendJoinChatRoomRequestRequest> = joinRoomRequestEmitter
    override fun kickedOutUser(): Observable<MessageInfo> = kickedOutEmitter
    override fun bannedUser(): Observable<MessageInfo> = bannedUserEmitter
    override fun voiceCallStarted(): Observable<VoiceCallStartSocketRequest> = voiceCallStartedEmitter
    override fun voiceCallEnded(): Observable<VoiceCallEndSocketRequest> = voiceCallEndedEmitter
    override fun restrictUser(): Observable<RestrictRequest> = restrictRequestEmitter
    override fun miceAccessRequest(): Observable<SendMicAccessRequestSocketRequest> = miceAccessRequestEmitter
    override fun acceptedRequest(): Observable<SendMicAccessRequestSocketRequest> = acceptedRequestEmitter
    override fun micAccessRevoked(): Observable<SendMicAccessRequestSocketRequest> = revokeMicAccessEmitter
    override fun addedNewRoom(): Observable<LiveRoomRequest> = addedNewRoomEmitter
    override fun observeLiveWatchingCounter(): Observable<LiveEventWatchingCount> = liveWatchingCountEmitter
    override fun observeLiveEnd(): Observable<LiveSummaryInfo> = liveEndEmitter
    override fun observeKickedUser(): Observable<LiveEventKickUser> = kickedUserEmitter
    override fun observeRestrictUser(): Observable<LiveEventKickUser> = restrictUserEmitter
    override fun liveCommentReceive(): Observable<LiveEventSendOrReadComment> = sendCommentEmitter
    override fun videoCallEnded(): Observable<VoiceCallEndSocketRequest> = videoCallEndedEmitter
    override fun observeGiftRequest(): Observable<MessageInfo> = giftsRequestEmitter
    override fun observeReceiveGiftInGame(): Observable<ReceiveGiftInfo> = receiveGiftInGameEmitter
    override fun observeTopGifterInGame(): Observable<TopGifterInfo> = topGifterInGameEmitter
    override fun observeGameStarted(): Observable<StartGameRequestRequest> = gameStartedEmitter
    override fun observeGameEnded(): Observable<GameResultInfo> = gameEndedEmitter
    override fun observeWantToPlayLiveGame(): Observable<StartGameRequestRequest> = wantToPlayGameEmitter
    override fun observeRemoveCoHost(): Observable<Unit> = removeCoHostFromLiveEmitter
    override fun observeStartTyping(): Observable<TypingRequest> = typingEmitter
    override fun observeStopTyping(): Observable<TypingRequest> = stopTypingEmitter
    override fun observeMsgSeen(): Observable<Unit> = msgSeenEmitter

    override fun joinRoom(joinRoomRequest: JoinRoomRequest): Completable {
        return appSocket.request(SocketService.EVENT_ROOM, appSocket.getGson().toJson(joinRoomRequest).toJsonObject())
    }

    override fun sendPrivateMessage(sendPrivateMessageRequest: SendPrivateMessageRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_SEND_PRIVATE_MESSAGE,
            appSocket.getGson().toJson(sendPrivateMessageRequest).toJsonObject(),
        )
    }

    override fun sendJoinRoomRequest(sendJoinChatRoomRequest: SendJoinChatRoomRequestRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_SEND_JOIN_ROOM_REQUEST,
            appSocket.getGson().toJson(sendJoinChatRoomRequest).toJsonObject(),
        )
    }

    override fun off(name: String) {
        appSocket.off(name)
    }

    override fun voiceCallStart(voiceCallStartSocketRequest: VoiceCallStartSocketRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_VOICE_CALL_START,
            appSocket.getGson().toJson(voiceCallStartSocketRequest).toJsonObject()
        )
    }

    override fun voiceCallEnd(voiceCallEndSocketRequest: VoiceCallEndSocketRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_VOICE_CALL_END,
            appSocket.getGson().toJson(voiceCallEndSocketRequest).toJsonObject()
        )
    }

    override fun sendMiceAccessRequest(
        sendMicAccessRequestSocketRequest: SendMicAccessRequestSocketRequest
    ): Completable {
        return appSocket.request(
            SocketService.EVENT_MICE_REQUEST,
            appSocket.getGson().toJson(sendMicAccessRequestSocketRequest).toJsonObject()
        )
    }

    override fun acceptMicRequest(sendMicAccessRequestSocketRequest: SendMicAccessRequestSocketRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_REQUEST_ACCEPT,
            appSocket.getGson().toJson(sendMicAccessRequestSocketRequest).toJsonObject()
        )
    }

    override fun revokeMicAccess(revokeMicAccessRequest: SendMicAccessRequestSocketRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_REVOKE_MIC_ACCESS,
            appSocket.getGson().toJson(revokeMicAccessRequest).toJsonObject()
        )
    }

    override fun createNewLiveRoom(liveRoomRequest: LiveRoomRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_CREATE_LIVE_ROOM,
            appSocket.getGson().toJson(liveRoomRequest).toJsonObject()
        )
    }

    override fun joinLive(joinLiveRoomRequest: JoinLiveRoomRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_JOIN_LIVE_ROOM,
            appSocket.getGson().toJson(joinLiveRoomRequest).toJsonObject()
        )
    }

    override fun attendeeJoinLive(joinLiveRoomRequest: JoinLiveRoomRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_ATTENDEE_JOIN_LIVE_ROOM,
            appSocket.getGson().toJson(joinLiveRoomRequest).toJsonObject()
        )
    }

    override fun leaveLiveRoom(joinLiveRoomRequest: LeaveLiveRoomRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_LEAVE_LIVE_ROOM,
            appSocket.getGson().toJson(joinLiveRoomRequest).toJsonObject()
        )
    }

    override fun kickUserFromLive(liveEventKickUser: LiveEventKickUser): Completable {
        return appSocket.request(
            SocketService.EVENT_KICK_USER,
            appSocket.getGson().toJson(liveEventKickUser).toJsonObject()
        )
    }

    override fun liveComments(liveEventSendOrReadComment: LiveEventSendOrReadComment): Completable {
        return appSocket.request(
            SocketService.EVENT_LIVE_COMMENT,
            appSocket.getGson().toJson(liveEventSendOrReadComment).toJsonObject()
        )
    }

    override fun sendHeart(request: SendHeartSocketEvent): Completable {
        return appSocket.request(SocketService.EVENT_SEND_HEART, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun observeLiveHeart(): Observable<SendHeartSocketEvent> = liveHeartEmitter

    override fun joinAsCoHost(request: CoHostSocketEvent): Completable {
        return appSocket.request(
            SocketService.EVENT_JOIN_ROOM_AS_CO_HOST,
            appSocket.getGson().toJson(request).toJsonObject()
        )
    }

    override fun observeLiveAsCoHost(): Observable<CoHostListInfo> = updateLiveCoHost

    override fun updateLiveViewerCount(updateLiveViewerCountRequest: UpdateLiveViewerCountRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_UPDATE_VIEWER_COUNTER,
            appSocket.getGson().toJson(updateLiveViewerCountRequest).toJsonObject()
        )
    }

    override fun leaveOneToOneRoom(leaveRoomRequest: GetChatRoomAdminRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_LEAVE_ONE_TO_ONE_CHAT,
            appSocket.getGson().toJson(leaveRoomRequest).toJsonObject()
        )
    }

    override fun videoCallEnd(voiceCallEndSocketRequest: VoiceCallEndSocketRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_VIDEO_CALL_END,
            appSocket.getGson().toJson(voiceCallEndSocketRequest).toJsonObject()
        )
    }

    override fun acceptRejectGiftRequest(acceptRejectGiftRequest: AcceptRejectGiftRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_ACCEPT_REJECT_GIFT_REQUEST,
            appSocket.getGson().toJson(acceptRejectGiftRequest).toJsonObject()
        )
    }

    override fun sendGiftInGame(request: SendGiftInGameRequest): Completable {
        return appSocket.request(SocketService.EVENT_SEND_GIFT, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun startGameInLive(request: StartGameRequestRequest): Completable {
        return appSocket.request(SocketService.EVENT_START_GAME, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun endGameInLive(request: StartGameRequestRequest): Completable {
        return appSocket.request(SocketService.EVENT_END_GAME, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun wantToPlayLiveGame(request: StartGameRequestRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_WANT_TO_PLAY_GAME,
            appSocket.getGson().toJson(request).toJsonObject()
        )
    }

    override fun removeCoHostFromLive(request: StartGameRequestRequest): Completable {
        return appSocket.request(
            SocketService.EVENT_REMOVE_CO_HOST_FROM_LIVE,
            appSocket.getGson().toJson(request).toJsonObject()
        )
    }

    override fun startTyping(request: TypingRequest): Completable {
        return appSocket.request(SocketService.EVENT_TYPING, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun stopTyping(request: TypingRequest): Completable {
        return appSocket.request(SocketService.EVENT_STOP_TYPING, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun seenMsg(request: SeenMsgRequest): Completable {
        return appSocket.request(SocketService.EVENT_MSG_SEEN, appSocket.getGson().toJson(request).toJsonObject())
    }
}
