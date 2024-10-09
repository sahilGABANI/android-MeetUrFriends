package com.meetfriend.app.ui.livestreaming.watchlive.viewmodel

import android.os.Handler
import android.os.Looper
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.gift.GiftsRepository
import com.meetfriend.app.api.gift.model.MyEarningInfo
import com.meetfriend.app.api.livestreaming.LiveRepository
import com.meetfriend.app.api.livestreaming.model.CoHostFollowInfo
import com.meetfriend.app.api.livestreaming.model.CoHostListInfo
import com.meetfriend.app.api.livestreaming.model.CoHostSocketEvent
import com.meetfriend.app.api.livestreaming.model.CoinCentsInfo
import com.meetfriend.app.api.livestreaming.model.EndLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.GameResultInfo
import com.meetfriend.app.api.livestreaming.model.JoinLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.JoinLiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LeaveLiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.api.livestreaming.model.LiveEventKickUser
import com.meetfriend.app.api.livestreaming.model.LiveEventSendOrReadComment
import com.meetfriend.app.api.livestreaming.model.LiveSummaryInfo
import com.meetfriend.app.api.livestreaming.model.ROLE_ATTENDEE
import com.meetfriend.app.api.livestreaming.model.ROLE_PUBLISHER
import com.meetfriend.app.api.livestreaming.model.ReceiveGiftInfo
import com.meetfriend.app.api.livestreaming.model.SendGiftInGameRequest
import com.meetfriend.app.api.livestreaming.model.SendHeartSocketEvent
import com.meetfriend.app.api.livestreaming.model.StartGameRequestRequest
import com.meetfriend.app.api.livestreaming.model.UpdateLiveViewerCountRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import com.meetfriend.app.socket.SocketService
import com.meetfriend.app.ui.livestreaming.viewmodel.currentTime
import com.meetfriend.app.utils.Constant.FiXED_3000_MILLISECOND
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class WatchLiveVideoViewModel(
    private val liveRepository: LiveRepository,
    private val loggedInUserCache: LoggedInUserCache,
    private val followRepository: FollowRepository,
    private val giftsRepository: GiftsRepository
) : BasicViewModel() {

    private val watchLiveVideoListStatesSubject: PublishSubject<WatchLiveVideoListState> =
        PublishSubject.create()
    val watchLiveVideoListStates: Observable<WatchLiveVideoListState> =
        watchLiveVideoListStatesSubject.hide()

    private var channelId: String? = null
    private var liveId = -1
    private var loggedInUserId = loggedInUserCache.getLoggedInUserId() ?: -1

    private var liveEventInfo: LiveEventInfo? = null

    private var listOfLiveEventSendOrReadComment: MutableList<LiveEventSendOrReadComment> =
        mutableListOf()

    init {
        observeLiveEventEnd()
        observeLiveWatchingCount()
        loggedInUserCache.invitedAsCoHost.subscribeAndObserveOnMainThread {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.InviteCoHostNotification(
                    it
                )
            )
        }
        observeOtherLiveComment()
        observeLiveHeart()
        observeLiveAsCoHost()
        observeKickedOutUserFromLive()
        observeRestrictUserFromLive()
        observeReceiveGiftInGame()
        observeTopGifterInGame()
        observeGameStarted()
        observeGameEnded()
        observeWantToPlayLiveGame()
        observeRemoveCoHost()
    }

    private fun observeLiveHeart() {
        liveRepository.observeLiveHeart().subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LiveHeart(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeLiveAsCoHost() {
        liveRepository.observeLiveAsCoHost().subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.JoinCoHost(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun joinAsCoHost(userId: Int) {
        val request = CoHostSocketEvent(
            userId = userId,
            roomId = channelId,
            name = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName,
            profile = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto,
            live_id = liveId.toString(),
            liveId = liveId.toString(),
            liveEventInfo?.isAllowPlayGame,
            isVerified = loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified
        )

        liveRepository.joinAsCoHost(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("joinAsCoHost")
                watchLiveVideoListStatesSubject.onNext(
                    WatchLiveVideoListState.SuccessJoinCoHostMessage("")
                )
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun sendHeart() {
        val request = SendHeartSocketEvent(
            id = currentTime.toString(),
            channelId = channelId,
            liveId = liveId,
            userId = liveEventInfo?.userId,
            isAllowPlayGame = liveEventInfo?.isAllowPlayGame
        )
        liveRepository.sendHeart(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("sendHeart")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun sendComment(comment: String, toUserId: String?) {
        val request = LiveEventSendOrReadComment(
            roomId = channelId,
            liveId = liveId,
            userId = loggedInUserId,
            id = currentTime.toString(),
            name = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
                ?: loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName.plus(" ")
                    .plus(loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName),
            username = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
                ?: loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName.plus(" ")
                    .plus(loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName),
            profileUrl = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto ?: "",
            comment = comment,
            toUserId = if (toUserId == "null") {
                if (liveEventInfo?.isCoHost == 1) {
                    loggedInUserCache.getLoggedInUserId()
                        .toString()
                } else {
                    liveEventInfo?.userId.toString()
                }
            } else {
                toUserId
            },
            isVerified = loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified
        )

        liveRepository.sendComment(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("sendComment")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun updateLiveEventInfo(liveEventInfo: LiveEventInfo) {
        this.liveEventInfo = liveEventInfo
        this.channelId = liveEventInfo.channelId
        this.liveId = liveEventInfo.id
    }

    fun joinLiveEvent(
        isCoHost: Boolean,
        isFromNotification: Boolean = false,
        type: String? = null,
        liveEventInfo: LiveEventInfo,
    ) {
        val coHostType = if (isCoHost) {
            ROLE_PUBLISHER
        } else {
            ROLE_ATTENDEE
        }
        liveRepository.joinLiveEvent(
            JoinLiveEventRequest(
                liveId,
                roleType = if (!type.isNullOrEmpty()) {
                    type
                } else {
                    coHostType
                }
            )
        ).doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->

            if (isCoHost) {
                joinAsCoHost(loggedInUserCache.getLoggedInUserId())
            } else {
                joinLiveRoom(liveEventInfo.isAllowPlayGame ?: 0)

                Handler(Looper.getMainLooper()).postDelayed({
                    attendeeJoinLive(liveEventInfo.isAllowPlayGame ?: 0)
                }, FiXED_3000_MILLISECOND)
            }
            if (!isFromNotification || isCoHost) {
                if (response.data != null) {
                    liveEventInfo.token = response.data.token
                    watchLiveVideoListStatesSubject.onNext(
                        WatchLiveVideoListState.JoinEventTokenInfo(
                            liveEventInfo,
                            isCoHost,
                            response
                        )
                    )
                }
            }
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.ErrorMessage(
                    it.localizedMessage ?: "Please try again"
                )
            )
        }).autoDispose()
    }

    fun joinLiveRoom(isAllowPlayGame: Int) {
        val request = JoinLiveRoomRequest(
            roomId = channelId,
            userId = loggedInUserId,
            liveId = liveId,
            isAllowPlayGame = isAllowPlayGame
        )
        liveRepository.joinLive(request).subscribeOnIoAndObserveOnMainThread(
            {
            },
            {
                Timber.e(it)
            }
        ).autoDispose()
    }

    fun attendeeJoinLive(isAllowPlayGame: Int) {
        val request = JoinLiveRoomRequest(
            roomId = channelId,
            userId = loggedInUserId,
            liveId = liveId,
            isAllowPlayGame = isAllowPlayGame
        )
        liveRepository.attendeeJoinLive(request).subscribeOnIoAndObserveOnMainThread(
            {
            },
            {
                Timber.e(it)
            }
        ).autoDispose()
    }

    private fun observeLiveWatchingCount() {
        liveRepository.observeLiveWatchingCounter().subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.LiveWatchingCount(
                    it.viewerCounter ?: 0
                )
            )
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeLiveEventEnd() {
        Timber.tag("Listened").e("it.toString()")
        liveRepository.observeLiveEnd().subscribeOnIoAndObserveOnMainThread({
            Timber.tag("Listen").e(it.toString())
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LiveEventEnd(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeOtherLiveComment() {
        liveRepository.liveCommentReceive().subscribeOnIoAndObserveOnMainThread({
            Timber.tag("LIVE").e(it.toString())
            updateComment(it)
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun updateComment(liveEventSendOrReadComment: LiveEventSendOrReadComment) {
        listOfLiveEventSendOrReadComment.add(liveEventSendOrReadComment)
        watchLiveVideoListStatesSubject.onNext(
            WatchLiveVideoListState.UpdateComment(
                liveEventSendOrReadComment
            )
        )
    }

    override fun onCleared() {
        liveRepository.off(SocketService.EVENT_KICKED_USER)
        liveRepository.off(SocketService.EVENT_RESTRICT_USER)
        liveRepository.off(SocketService.EVENT_LIVE_CO_HOST)
        liveRepository.off(SocketService.EVENT_RECEIVE_GIFT)
        liveRepository.off(SocketService.EVENT_TOP_GIFTER)
        liveRepository.off(SocketService.EVENT_GAME_STARTED)
        liveRepository.off(SocketService.EVENT_GAME_ENDED)
        liveRepository.off(SocketService.EVENT_REMOVE_CO_HOST_FROM_LIVE)
        super.onCleared()
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.FollowSuccess(it.message.toString()))
                } else {
                    watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun updateLiveViewerCount(updateLiveViewerCountRequest: UpdateLiveViewerCountRequest) {
        liveRepository.updateLiveViewerCount(updateLiveViewerCountRequest)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("decrease count")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun observeKickedOutUserFromLive() {
        liveRepository.observeKickedOutUserFromLive().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.KickedOutUser(it))
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeRestrictUserFromLive() {
        liveRepository.observeRestrictUserFromLive().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.RestrictUser(it))
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun sendGiftInGame(request: SendGiftInGameRequest) {
        liveRepository.sendGiftInGame(request).doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeReceiveGiftInGame() {
        liveRepository.observeReceiveGiftInGame().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ReceiveGift(it))
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeTopGifterInGame() {
        liveRepository.observeTopGifterInGame().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.TopGifterInfo(it))
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeGameStarted() {
        liveRepository.observeGameStarted().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.GameStarted(it))
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeGameEnded() {
        liveRepository.observeGameEnded().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.GameEnded(it))
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun endGameInLive(request: StartGameRequestRequest) {
        liveRepository.endGameInLive(request).doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            Timber.i("endGameInLive")
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeWantToPlayLiveGame() {
        liveRepository.observeWantToPlayLiveGame().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.PlayGameRequestInfo(it))
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun startGameInLive(request: StartGameRequestRequest) {
        liveRepository.startGameInLive(request).doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            Timber.i("startGameInLive")
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeRemoveCoHost() {
        liveRepository.observeRemoveCoHost().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.RemovedCohostInfo(it))
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun kickOutUserFromLive(liveEventKickUser: LiveEventKickUser) {
        liveRepository.kickOutUserFromLive(liveEventKickUser).doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            Timber.i("kickOutUserFromLive")
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun endLiveEvent() {
        if (channelId.isNullOrEmpty()) {
            loggedInUserCache.setEventChannelId("")
        } else {
            liveRepository.endLiveEvent(EndLiveEventRequest(liveId))
                .doAfterTerminate {
                    loggedInUserCache.removeEventChannelId(channelId ?: "")
                    // sendLiveEndEvent()
                }
                .subscribeOnIoAndObserveOnMainThread({
                }, { throwable ->
                    Timber.e(throwable)
                })
        }
    }

    fun sendLiveEndEvent() {
        val request = LeaveLiveRoomRequest(
            userId = loggedInUserCache.getLoggedInUserId().toString(),
            roomId = channelId.toString(),
            liveId = liveId.toString()
        )

        liveRepository.leaveLiveRoom(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("liveRoomDisconnect")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun sendGiftInLive(toId: String, postId: String, coins: Double, giftId: Int, quantity: Int) {
        giftsRepository.sendGiftInLive(toId, postId, coins, giftId, quantity).doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.SendTulipGiftSuccess)
            } else {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun getMyEarningInfo() {
        giftsRepository.getMyEarning().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.MyEarningData(it.result))
            } else {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun checkFollowCoHost(userId: Int) {
        liveRepository.checkCoHostFollow(userId).doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.CoHostFollowData(it.result))
            } else {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it))
            }
        })
    }

    fun getCoinInCents() {
        liveRepository.getCoinInCents().doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.CoinCentsData(it.result))
            } else {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.ErrorMessage(it))
            }
        })
    }

    fun removeCoHostFromLive(request: StartGameRequestRequest) {
        liveRepository.removeCoHostFromLive(request).doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            Timber.i("removeCoHostFromLive")
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.SuccessMessage(
                    it.message ?: ""
                )
            )
        })
    }
}

sealed class WatchLiveVideoListState {
    data class ErrorMessage(val errorMessage: String) : WatchLiveVideoListState()
    data class SuccessMessage(val successMessage: String) : WatchLiveVideoListState()
    data class SuccessJoinCoHostMessage(val successMessage: String) : WatchLiveVideoListState()
    data class LoadingState(val isLoading: Boolean) : WatchLiveVideoListState()
    data class LiveWatchingCount(val liveWatchingCount: Int) : WatchLiveVideoListState()
    data class UpdateComment(val sendOrReadComment: LiveEventSendOrReadComment) :
        WatchLiveVideoListState()

    data class InviteCoHostNotification(val liveEventInfo: LiveEventInfo) :
        WatchLiveVideoListState()

    data class JoinEventTokenInfo(
        val liveEventInfo: LiveEventInfo,
        val isCoHost: Boolean,
        val response: MeetFriendResponseForChat<LiveEventInfo>
    ) :
        WatchLiveVideoListState()

    data class LiveEventEnd(val endGameResponse: LiveSummaryInfo) : WatchLiveVideoListState()
    data class KickUserComment(val liveEventKickUser: LiveEventKickUser) : WatchLiveVideoListState()
    data class LiveHeart(val sendHeartSocketEvent: SendHeartSocketEvent) : WatchLiveVideoListState()
    data class JoinCoHost(val coHostSocketEvent: CoHostListInfo) : WatchLiveVideoListState()
    data class KickedOutUser(val liveEventKickUser: LiveEventKickUser) : WatchLiveVideoListState()
    data class ReceiveGift(val receiveGiftInfo: ReceiveGiftInfo) : WatchLiveVideoListState()
    data class TopGifterInfo(val liveEventKickUser: com.meetfriend.app.api.livestreaming.model.TopGifterInfo) :
        WatchLiveVideoListState()

    data class GameStarted(val startGameInfo: StartGameRequestRequest) :
        WatchLiveVideoListState()

    data class GameEnded(val endGameInfo: GameResultInfo) : WatchLiveVideoListState()
    data class PlayGameRequestInfo(val liveEventKickUser: StartGameRequestRequest) : WatchLiveVideoListState()
    data class RemovedCohostInfo(val liveEventKickUser: Unit) : WatchLiveVideoListState()

    data class RestrictUser(val liveEventKickUser: LiveEventKickUser) : WatchLiveVideoListState()
    data object SendTulipGiftSuccess : WatchLiveVideoListState()
    data class MyEarningData(val myEarningInfo: MyEarningInfo?) : WatchLiveVideoListState()
    data class CoHostFollowData(val coHostFollowInfo: CoHostFollowInfo?) : WatchLiveVideoListState()

    data class FollowSuccess(val message: String) : WatchLiveVideoListState()
    data class CoinCentsData(val coinCentsInfo: CoinCentsInfo?) : WatchLiveVideoListState()
}
