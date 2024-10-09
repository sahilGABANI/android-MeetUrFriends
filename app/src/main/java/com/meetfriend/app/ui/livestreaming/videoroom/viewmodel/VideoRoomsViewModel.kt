package com.meetfriend.app.ui.livestreaming.videoroom.viewmodel

import com.meetfriend.app.api.livestreaming.LiveRepository
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class VideoRoomsViewModel(
    private val liveRepository: LiveRepository
) : BasicViewModel() {

    private val videoRoomsStateSubject: PublishSubject<VideoRoomsViewState> =
        PublishSubject.create()
    val videoRoomsState: Observable<VideoRoomsViewState> = videoRoomsStateSubject.hide()

    init {
        observeLiveEventEnd()
    }

    fun getAllActiveLiveEvent() {
        liveRepository.getAllActiveLiveEvent().doOnSubscribe {
            videoRoomsStateSubject.onNext(VideoRoomsViewState.LoadingState(true))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            if (response.data.isNullOrEmpty()) {
                videoRoomsStateSubject.onNext((VideoRoomsViewState.EmptyLiveEvent))
            } else {
                videoRoomsStateSubject.onNext(VideoRoomsViewState.LoadAllActiveEventList(response.data))
            }
        }, { throwable ->
            Timber.e(throwable)
            throwable.localizedMessage?.let {
                videoRoomsStateSubject.onNext((VideoRoomsViewState.ErrorMessage(it)))
            }
        }).autoDispose()
    }

    private fun observeLiveEventEnd() {
        liveRepository.observeLiveEnd().subscribeOnIoAndObserveOnMainThread({
            Timber.e(it.toString())
            videoRoomsStateSubject.onNext(VideoRoomsViewState.LiveEventEnd)
        }, {
            Timber.e(it)
        }).autoDispose()
    }
}

sealed class VideoRoomsViewState {
    data class ErrorMessage(val errorMessage: String) : VideoRoomsViewState()
    data class SuccessMessage(val successMessage: String) : VideoRoomsViewState()
    data class LoadingState(val isLoading: Boolean) : VideoRoomsViewState()

    data class LoadAllActiveEventList(val allActiveEventInfo: List<LiveEventInfo>) :
        VideoRoomsViewState()

    object LiveEventEnd : VideoRoomsViewState()
    object EmptyLiveEvent : VideoRoomsViewState()
}
