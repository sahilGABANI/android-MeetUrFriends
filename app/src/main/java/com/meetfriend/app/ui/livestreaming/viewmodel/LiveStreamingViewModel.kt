package com.meetfriend.app.ui.livestreaming.viewmodel

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.livestreaming.LiveRepository
import com.meetfriend.app.api.livestreaming.model.CoHostListInfo
import com.meetfriend.app.api.livestreaming.model.CoHostRequests
import com.meetfriend.app.api.livestreaming.model.CoinCentsInfo
import com.meetfriend.app.api.livestreaming.model.EndLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.GameResultInfo
import com.meetfriend.app.api.livestreaming.model.JoinLiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LeaveLiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LiveEventKickUser
import com.meetfriend.app.api.livestreaming.model.LiveEventSendOrReadComment
import com.meetfriend.app.api.livestreaming.model.LiveRoomRequest
import com.meetfriend.app.api.livestreaming.model.LiveSummaryInfo
import com.meetfriend.app.api.livestreaming.model.ReceiveGiftInfo
import com.meetfriend.app.api.livestreaming.model.SendHeartSocketEvent
import com.meetfriend.app.api.livestreaming.model.StartGameRequestRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.socket.SocketService
import com.meetfriend.app.utils.Constant.FiXED_1000000_INT
import com.meetfriend.app.utils.Constant.FiXED_3_INT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

val currentTime: Long
    get() = System.nanoTime() / FiXED_1000000_INT

class LiveStreamingViewModel(
    private val liveRepository: LiveRepository,
    private val loggedInUserCache: LoggedInUserCache,
) : BasicViewModel() {
    private val liveStreamingStateSubject: PublishSubject<LiveStreamingViewState> =
        PublishSubject.create()
    val liveStreamingViewState: Observable<LiveStreamingViewState> =
        liveStreamingStateSubject.hide()

    private var channelId: String? = null
    private var liveId = -1
    private var isAllowPlayGame = -1
    private var roomTag: String? = null
    private var loggedInUserId = loggedInUserCache.getLoggedInUserId() ?: -1
    private var selectedCoHostUserMap: MutableMap<Int, MeetFriendUser> = mutableMapOf()

    init {
        observeNewLiveRoom()
        observeLiveWatchingCount()
        observeOtherLiveComment()
        observeLiveHeart()
        observeKickedOutUserFromLive()
        observeLiveAsCoHost()
        observeReceiveGiftInGame()
        observeTopGifterInGame()
        observeGameStarted()
        observeGameEnded()
        observeWantToPlayLiveGame()
        observeLiveEventEnd()
    }

    fun updateChannelId(channelId: String?, liveId: Int, isAllowPlayGame: Int) {
        this.channelId = channelId
        this.liveId = liveId
        this.isAllowPlayGame = isAllowPlayGame
        loggedInUserCache.setEventChannelId(channelId ?: "")
        liveRoom()
    }

    private fun liveRoom() {
        val request = LiveRoomRequest(
            roomId = channelId,
            roomTag = roomTag,
            userId = loggedInUserId,
            profilePhoto = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto,
            liveId = liveId,
            isAllowPlayGame = isAllowPlayGame,
            isVerified = loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified
        )
        liveRepository.liveRoom(request).subscribeOnIoAndObserveOnMainThread({
            Timber.i("liveRoom")
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeNewLiveRoom() {
        liveRepository.observeNewLiveRoom().subscribeOnIoAndObserveOnMainThread({
            joinLiveRoom(isAllowPlayGame)
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun endLiveEvent() {
        if (channelId.isNullOrEmpty()) {
            loggedInUserCache.setEventChannelId("")
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LeaveLiveRoom)
        } else {
            liveRepository.endLiveEvent(EndLiveEventRequest(liveId))
                .doAfterTerminate {
                    loggedInUserCache.removeEventChannelId(channelId ?: "")
                    liveStreamingStateSubject.onNext(LiveStreamingViewState.LeaveLiveRoom)
                }
                .subscribeOnIoAndObserveOnMainThread({
                }, { throwable ->
                    Timber.e(throwable)
                })
        }
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
            }
        ).autoDispose()
    }

    private fun observeLiveWatchingCount() {
        liveRepository.observeLiveWatchingCounter().subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.LiveWatchingCount(
                    it.viewerCounter ?: 0
                )
            )
        }, {
        }).autoDispose()
    }

    fun sendLiveEndEvent() {
        val request = LeaveLiveRoomRequest(
            userId = loggedInUserCache.getLoggedInUserId().toString(),
            roomId = channelId.toString(),
            liveId = liveId.toString()
        )

        liveRepository.leaveLiveRoom(request)
            .subscribeOnIoAndObserveOnMainThread({
            }, {
            }).autoDispose()
    }

    override fun onCleared() {
        liveRepository.off(SocketService.EVENT_ADDED_NEW_ROOM)
        liveRepository.off(SocketService.EVENT_UPDATE_LIVE_COUNTER)
        liveRepository.off(SocketService.EVENT_KICKED_USER)
        liveRepository.off(SocketService.EVENT_RECEIVE_GIFT)
        liveRepository.off(SocketService.EVENT_TOP_GIFTER)
        liveRepository.off(SocketService.EVENT_GAME_STARTED)
        liveRepository.off(SocketService.EVENT_GAME_ENDED)
        liveRepository.off(SocketService.EVENT_WANT_TO_PLAY_GAME)

        super.onCleared()
    }

    private fun observeOtherLiveComment() {
        liveRepository.liveCommentReceive().subscribeOnIoAndObserveOnMainThread({
            Timber.tag("LIVE").e(it.toString())
            liveStreamingStateSubject.onNext(LiveStreamingViewState.UpdateComment(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun sendComment(comment: String) {
        val request = LiveEventSendOrReadComment(
            roomId = channelId,
            liveId = liveId,
            userId = loggedInUserId,
            // Additional Params
            id = currentTime.toString(),
            name = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
                ?: loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName.plus(" ")
                    .plus(loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName),
            username = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
                ?: loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName.plus(" ")
                    .plus(loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName),
            profileUrl = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto ?: "",
            comment = comment,
            toUserId = loggedInUserCache.getLoggedInUserId().toString(),
            isVerified = loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified
        )

        liveRepository.sendComment(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("sendComment")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun observeLiveHeart() {
        liveRepository.observeLiveHeart().subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LiveHeart(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun sendHeart(isAllowPlayGame: Int) {
        val request = SendHeartSocketEvent(
            id = currentTime.toString(),
            channelId = channelId,
            liveId = liveId,
            userId = loggedInUserId,
            isAllowPlayGame = isAllowPlayGame
        )
        liveRepository.sendHeart(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("sendHeart")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun observeLiveAsCoHost() {
        liveRepository.observeLiveAsCoHost().subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(LiveStreamingViewState.JoinCoHost(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun inviteCoHost(liveId: Int, inviteUserMap: Map<Int, MeetFriendUser>) {
        val coHostUserList = inviteUserMap.filter { it.value.isInvited }.values.toList()
            .filter { !it.isAlreadyInvited }.take(FiXED_3_INT)
        selectedCoHostUserMap.putAll(coHostUserList.map { it.id to it })
        liveRepository.inviteCoHosts(
            CoHostRequests(
                channelId = liveId,
                inviteIds = inviteUserMap.map { it.value.id }.joinToString(",")
            )
        ).doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun kickOutUserFromLive(liveEventKickUser: LiveEventKickUser) {
        liveRepository.kickOutUserFromLive(liveEventKickUser).doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            Timber.i("kickOutUserFromLive")
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeKickedOutUserFromLive() {
        liveRepository.observeKickedOutUserFromLive().doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(LiveStreamingViewState.KickedOutUser(it))
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeReceiveGiftInGame() {
        liveRepository.observeReceiveGiftInGame().doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(LiveStreamingViewState.ReceiveGift(it))
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeTopGifterInGame() {
        liveRepository.observeTopGifterInGame().doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(LiveStreamingViewState.TopGifterInfo(it))
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun endGameInLive(request: StartGameRequestRequest) {
        liveRepository.endGameInLive(request).doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            Timber.i("endGameInLive")
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeGameStarted() {
        liveRepository.observeGameStarted().doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(LiveStreamingViewState.GameStarted(it))
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeGameEnded() {
        liveRepository.observeGameEnded().doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(LiveStreamingViewState.GameEnded(it))
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun wantToPlayLiveGame(request: StartGameRequestRequest) {
        liveRepository.wantToPlayLiveGame(request).doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            Timber.i("wantToPlayLiveGame")
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun observeWantToPlayLiveGame() {
        liveRepository.observeWantToPlayLiveGame().doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            liveStreamingStateSubject.onNext(LiveStreamingViewState.PlayGameRequestInfo(it))
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    fun removeCoHostFromLive(request: StartGameRequestRequest) {
        liveRepository.removeCoHostFromLive(request).doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            Timber.i("removeCoHostFromLive")
        }, {
            liveStreamingStateSubject.onNext(
                LiveStreamingViewState.SuccessMessage(
                    it.message ?: ""
                )
            )
        }).autoDispose()
    }

    private fun observeLiveEventEnd() {
        Timber.tag("Listened").e("it.toString()")
        liveRepository.observeLiveEnd().subscribeOnIoAndObserveOnMainThread({
            Timber.tag("Listen").e(it.toString())
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LiveSummary(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun getCoinInCents() {
        liveRepository.getCoinInCents().doOnSubscribe {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamingStateSubject.onNext(LiveStreamingViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                liveStreamingStateSubject.onNext(LiveStreamingViewState.CoinCentsData(it.result))
            } else {
                liveStreamingStateSubject.onNext(LiveStreamingViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                liveStreamingStateSubject.onNext(LiveStreamingViewState.ErrorMessage(it))
            }
        })
    }
}

sealed class LiveStreamingViewState {
    data class ErrorMessage(val errorMessage: String) : LiveStreamingViewState()
    data class SuccessMessage(val successMessage: String) : LiveStreamingViewState()
    data class LoadingState(val isLoading: Boolean) : LiveStreamingViewState()
    data object LeaveLiveRoom : LiveStreamingViewState()
    data class LiveWatchingCount(val liveWatchingCount: Int) : LiveStreamingViewState()
    data class UpdateComment(val liveEventSendOrReadComment: LiveEventSendOrReadComment) :
        LiveStreamingViewState()
    data class LiveHeart(val sendHeartSocketEvent: SendHeartSocketEvent) : LiveStreamingViewState()
    data class JoinCoHost(val coHostSocketEvent: CoHostListInfo) : LiveStreamingViewState()
    data class KickedOutUser(val liveEventKickUser: LiveEventKickUser) : LiveStreamingViewState()
    data class GameStarted(val startGameInfo: StartGameRequestRequest) : LiveStreamingViewState()
    data class GameEnded(val endGameInfo: GameResultInfo) : LiveStreamingViewState()
    data class ReceiveGift(val receiveGiftInfo: ReceiveGiftInfo) : LiveStreamingViewState()
    data class TopGifterInfo(val liveEventKickUser: com.meetfriend.app.api.livestreaming.model.TopGifterInfo) :
        LiveStreamingViewState()

    data class PlayGameRequestInfo(val liveEventKickUser: StartGameRequestRequest) : LiveStreamingViewState()

    data class LiveSummary(val liveSummaryInfo: LiveSummaryInfo) : LiveStreamingViewState()
    data class CoinCentsData(val coinCentsInfo: CoinCentsInfo?) : LiveStreamingViewState()
}
