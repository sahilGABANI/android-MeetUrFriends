package com.meetfriend.app.ui.livestreaming.watchlive.viewmodel

import com.meetfriend.app.api.livestreaming.LiveRepository
import com.meetfriend.app.api.livestreaming.model.LiveEventWatchingUserRequest
import com.meetfriend.app.api.livestreaming.model.LiveJoinResponse
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LiveWatchingUserViewModel(
    private val liveRepository: LiveRepository
) : BasicViewModel() {

    private val liveStreamingStatesSubject: PublishSubject<LiveStreamingViewState> =
        PublishSubject.create()
    val liveStreamingStates: Observable<LiveStreamingViewState> = liveStreamingStatesSubject.hide()

    fun liveJoinUserEvent(liveId: Int) {
        liveRepository.liveJoinUsers(LiveEventWatchingUserRequest(liveId))
            .subscribeOnIoAndObserveOnMainThread({
                it.data?.let { joinUser ->
                    liveStreamingStatesSubject.onNext(LiveStreamingViewState.LiveJoinUser(joinUser))
                }
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    sealed class LiveStreamingViewState {
        data class ErrorMessage(val errorMessage: String) : LiveStreamingViewState()
        data class SuccessMessage(val successMessage: String) : LiveStreamingViewState()
        data class LoadingState(val isLoading: Boolean) : LiveStreamingViewState()

        data class LiveJoinUser(val listUserJoin: List<LiveJoinResponse>) : LiveStreamingViewState()
    }
}
