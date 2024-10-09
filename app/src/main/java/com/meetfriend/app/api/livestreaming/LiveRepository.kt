package com.meetfriend.app.api.livestreaming

import com.meetfriend.app.api.livestreaming.model.CheckCoHostFollowRequest
import com.meetfriend.app.api.livestreaming.model.CoHostFollowInfo
import com.meetfriend.app.api.livestreaming.model.CoHostListInfo
import com.meetfriend.app.api.livestreaming.model.CoHostRequests
import com.meetfriend.app.api.livestreaming.model.CoHostSocketEvent
import com.meetfriend.app.api.livestreaming.model.CoinCentsInfo
import com.meetfriend.app.api.livestreaming.model.CreateLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.EndLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.GameResultInfo
import com.meetfriend.app.api.livestreaming.model.JoinLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.JoinLiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LeaveLiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.api.livestreaming.model.LiveEventKickUser
import com.meetfriend.app.api.livestreaming.model.LiveEventSendOrReadComment
import com.meetfriend.app.api.livestreaming.model.LiveEventWatchingCount
import com.meetfriend.app.api.livestreaming.model.LiveEventWatchingUserRequest
import com.meetfriend.app.api.livestreaming.model.LiveJoinResponse
import com.meetfriend.app.api.livestreaming.model.LiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LiveSummaryInfo
import com.meetfriend.app.api.livestreaming.model.ReceiveGiftInfo
import com.meetfriend.app.api.livestreaming.model.RejectJoinRequest
import com.meetfriend.app.api.livestreaming.model.SendGiftInGameRequest
import com.meetfriend.app.api.livestreaming.model.SendHeartSocketEvent
import com.meetfriend.app.api.livestreaming.model.StartGameRequestRequest
import com.meetfriend.app.api.livestreaming.model.TopGifterInfo
import com.meetfriend.app.api.livestreaming.model.UpdateLiveViewerCountRequest
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForLive
import com.meetfriend.app.socket.SocketDataManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class LiveRepository(
    private val liveRetrofitAPI: LiveRetrofitAPI,
    private val socketDataManager: SocketDataManager,
) {
    private val meetFriendResponseConverter: MeetFriendResponseConverter = MeetFriendResponseConverter()

    fun createLiveEvent(request: CreateLiveEventRequest): Single<MeetFriendResponseForChat<LiveEventInfo>> {
        return liveRetrofitAPI.createLiveEvent(request)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun joinLiveEvent(request: JoinLiveEventRequest): Single<MeetFriendResponseForChat<LiveEventInfo>> {
        return liveRetrofitAPI.joinLiveEvent(request)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun endLiveEvent(request: EndLiveEventRequest): Single<MeetFriendCommonResponse> {
        return liveRetrofitAPI.endLiveEvent(request)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getAllActiveLiveEvent(): Single<MeetFriendResponseForLive<LiveEventInfo>> {
        return liveRetrofitAPI.getAllActiveLiveEvent().flatMap {
            meetFriendResponseConverter.convertToSingleForLive(it)
        }
    }

    fun inviteCoHosts(coHostRequest: CoHostRequests): Single<MeetFriendCommonResponse> {
        return liveRetrofitAPI.inviteOrRejectCoHosts(coHostRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun rejectCoHosts(request: RejectJoinRequest): Single<MeetFriendCommonResponse> {
        return liveRetrofitAPI.rejectCoHosts(request)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun liveJoinUsers(
        request: LiveEventWatchingUserRequest
    ): Single<MeetFriendResponseForChat<List<LiveJoinResponse>>> {
        return liveRetrofitAPI.liveJoinUser(request)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun liveRoom(request: LiveRoomRequest): Completable {
        return socketDataManager.createNewLiveRoom(request)
    }

    fun observeNewLiveRoom(): Observable<LiveRoomRequest> {
        return socketDataManager.addedNewRoom()
    }

    fun joinLive(request: JoinLiveRoomRequest): Completable {
        return socketDataManager.joinLive(request)
    }
    fun attendeeJoinLive(request: JoinLiveRoomRequest): Completable {
        return socketDataManager.attendeeJoinLive(request)
    }

    fun observeLiveWatchingCounter(): Observable<LiveEventWatchingCount> {
        return socketDataManager.observeLiveWatchingCounter()
    }

    fun leaveLiveRoom(request: LeaveLiveRoomRequest): Completable {
        return socketDataManager.leaveLiveRoom(request)
    }

    fun observeLiveEnd(): Observable<LiveSummaryInfo> {
        return socketDataManager.observeLiveEnd()
    }

    fun liveCommentReceive(): Observable<LiveEventSendOrReadComment> {
        return socketDataManager.liveCommentReceive()
    }

    fun sendComment(liveEventSendOrReadComment: LiveEventSendOrReadComment): Completable {
        return socketDataManager.liveComments(liveEventSendOrReadComment)
    }

    fun sendHeart(request: SendHeartSocketEvent): Completable {
        return socketDataManager.sendHeart(request)
    }

    fun observeLiveHeart(): Observable<SendHeartSocketEvent> {
        return socketDataManager.observeLiveHeart()
    }

    fun joinAsCoHost(request: CoHostSocketEvent): Completable {
        return socketDataManager.joinAsCoHost(request)
    }

    fun observeLiveAsCoHost(): Observable<CoHostListInfo> {
        return socketDataManager.observeLiveAsCoHost()
    }

    fun updateLiveViewerCount(updateLiveViewerCountRequest: UpdateLiveViewerCountRequest): Completable {
        return socketDataManager.updateLiveViewerCount(updateLiveViewerCountRequest)
    }

    fun kickOutUserFromLive(liveEventKickUser: LiveEventKickUser): Completable {
        return socketDataManager.kickUserFromLive(liveEventKickUser)
    }

    fun observeKickedOutUserFromLive(): Observable<LiveEventKickUser> {
        return socketDataManager.observeKickedUser()
    }

    fun observeRestrictUserFromLive(): Observable<LiveEventKickUser> {
        return socketDataManager.observeRestrictUser()
    }

    fun sendGiftInGame(request: SendGiftInGameRequest): Completable {
        return socketDataManager.sendGiftInGame(request)
    }

    fun observeReceiveGiftInGame(): Observable<ReceiveGiftInfo> {
        return socketDataManager.observeReceiveGiftInGame()
    }
    fun observeTopGifterInGame(): Observable<TopGifterInfo> {
        return socketDataManager.observeTopGifterInGame()
    }

    fun startGameInLive(request: StartGameRequestRequest): Completable {
        return socketDataManager.startGameInLive(request)
    }

    fun observeGameStarted(): Observable<StartGameRequestRequest> {
        return socketDataManager.observeGameStarted()
    }
    fun endGameInLive(request: StartGameRequestRequest): Completable {
        return socketDataManager.endGameInLive(request)
    }

    fun observeGameEnded(): Observable<GameResultInfo> {
        return socketDataManager.observeGameEnded()
    }

    fun wantToPlayLiveGame(request: StartGameRequestRequest): Completable {
        return socketDataManager.wantToPlayLiveGame(request)
    }

    fun observeWantToPlayLiveGame(): Observable<StartGameRequestRequest> {
        return socketDataManager.observeWantToPlayLiveGame()
    }

    fun removeCoHostFromLive(request: StartGameRequestRequest): Completable {
        return socketDataManager.removeCoHostFromLive(request)
    }

    fun observeRemoveCoHost(): Observable<Unit> {
        return socketDataManager.observeRemoveCoHost()
    }

    fun checkCoHostFollow(userId: Int): Single<MeetFriendResponse<CoHostFollowInfo>> {
        return liveRetrofitAPI.checkCoHostFollow(CheckCoHostFollowRequest(userId))
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getCoinInCents(): Single<MeetFriendResponse<CoinCentsInfo>> {
        return liveRetrofitAPI.getCoinInCents()
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun off(name: String) {
        socketDataManager.off(name)
    }
}
