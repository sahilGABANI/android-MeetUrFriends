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

interface Connectible {
    val isConnected: Boolean
    fun connect()
    fun connectionEmitter(): Observable<Unit>
    fun connectionError(): Observable<Unit>
    fun disconnect()
    fun disconnectEmitter(): Observable<Unit>
    fun roomJoined(): Observable<JoinChatRoomResponse>
    fun privateMessage(): Observable<MessageInfo>
    fun receiveJoinRoomRequest(): Observable<SendJoinChatRoomRequestRequest>
    fun kickedOutUser(): Observable<MessageInfo>
    fun bannedUser(): Observable<MessageInfo>

    fun voiceCallStarted(): Observable<VoiceCallStartSocketRequest>
    fun voiceCallEnded(): Observable<VoiceCallEndSocketRequest>
    fun restrictUser(): Observable<RestrictRequest>
    fun miceAccessRequest(): Observable<SendMicAccessRequestSocketRequest>
    fun acceptedRequest(): Observable<SendMicAccessRequestSocketRequest>
    fun micAccessRevoked(): Observable<SendMicAccessRequestSocketRequest>

    fun joinRoom(joinRoomRequest: JoinRoomRequest): Completable
    fun sendPrivateMessage(sendPrivateMessageRequest: SendPrivateMessageRequest): Completable
    fun sendJoinRoomRequest(sendJoinChatRoomRequest: SendJoinChatRoomRequestRequest): Completable
    fun off(name: String)

    fun voiceCallStart(voiceCallStartSocketRequest: VoiceCallStartSocketRequest): Completable
    fun voiceCallEnd(voiceCallEndSocketRequest: VoiceCallEndSocketRequest): Completable
    fun sendMiceAccessRequest(sendMicAccessRequestSocketRequest: SendMicAccessRequestSocketRequest): Completable
    fun acceptMicRequest(sendMicAccessRequestSocketRequest: SendMicAccessRequestSocketRequest): Completable
    fun revokeMicAccess(revokeMicAccessRequest: SendMicAccessRequestSocketRequest): Completable

    fun createNewLiveRoom(liveRoomRequest: LiveRoomRequest): Completable
    fun addedNewRoom(): Observable<LiveRoomRequest>

    fun joinLive(joinLiveRoomRequest: JoinLiveRoomRequest): Completable
    fun attendeeJoinLive(joinLiveRoomRequest: JoinLiveRoomRequest): Completable
    fun observeLiveWatchingCounter(): Observable<LiveEventWatchingCount>

    fun leaveLiveRoom(joinLiveRoomRequest: LeaveLiveRoomRequest): Completable
    fun observeLiveEnd(): Observable<LiveSummaryInfo>

    fun kickUserFromLive(liveEventKickUser: LiveEventKickUser): Completable
    fun observeKickedUser(): Observable<LiveEventKickUser>
    fun observeRestrictUser(): Observable<LiveEventKickUser>

    fun liveComments(liveEventSendOrReadComment: LiveEventSendOrReadComment): Completable
    fun liveCommentReceive(): Observable<LiveEventSendOrReadComment>

    fun sendHeart(request: SendHeartSocketEvent): Completable
    fun observeLiveHeart(): Observable<SendHeartSocketEvent>

    fun joinAsCoHost(request: CoHostSocketEvent): Completable
    fun observeLiveAsCoHost(): Observable<CoHostListInfo>

    fun updateLiveViewerCount(updateLiveViewerCountRequest: UpdateLiveViewerCountRequest): Completable

    fun leaveOneToOneRoom(leaveRoomRequest: GetChatRoomAdminRequest): Completable

    fun videoCallEnded(): Observable<VoiceCallEndSocketRequest>
    fun videoCallEnd(voiceCallEndSocketRequest: VoiceCallEndSocketRequest): Completable
    fun acceptRejectGiftRequest(acceptRejectGiftRequest: AcceptRejectGiftRequest): Completable

    fun observeGiftRequest(): Observable<MessageInfo>

    fun sendGiftInGame(request: SendGiftInGameRequest): Completable

    fun observeReceiveGiftInGame(): Observable<ReceiveGiftInfo>
    fun observeTopGifterInGame(): Observable<TopGifterInfo>

    fun startGameInLive(request: StartGameRequestRequest): Completable
    fun endGameInLive(request: StartGameRequestRequest): Completable

    fun observeGameStarted(): Observable<StartGameRequestRequest>
    fun observeGameEnded(): Observable<GameResultInfo>

    fun wantToPlayLiveGame(request: StartGameRequestRequest): Completable

    fun observeWantToPlayLiveGame(): Observable<StartGameRequestRequest>

    fun removeCoHostFromLive(request: StartGameRequestRequest): Completable

    fun observeRemoveCoHost(): Observable<Unit>

    fun startTyping(request: TypingRequest): Completable

    fun observeStartTyping(): Observable<TypingRequest>
    fun stopTyping(request: TypingRequest): Completable

    fun observeStopTyping(): Observable<TypingRequest>

    fun seenMsg(request: SeenMsgRequest): Completable
    fun observeMsgSeen(): Observable<Unit>
}
