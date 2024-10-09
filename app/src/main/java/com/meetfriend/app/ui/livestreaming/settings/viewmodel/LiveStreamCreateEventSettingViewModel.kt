package com.meetfriend.app.ui.livestreaming.settings.viewmodel

import com.meetfriend.app.api.livestreaming.LiveRepository
import com.meetfriend.app.api.livestreaming.model.CreateLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LiveStreamCreateEventSettingViewModel(
    private val liveRepository: LiveRepository
) : BasicViewModel() {

    private val liveStreamCreateEventSettingStatesSubject: PublishSubject<LiveStreamCreateEventSettingState> =
        PublishSubject.create()
    val liveStreamCreateEventSettingStates: Observable<LiveStreamCreateEventSettingState> =
        liveStreamCreateEventSettingStatesSubject.hide()

    fun createLiveEvent(request: CreateLiveEventRequest) {
        liveRepository.createLiveEvent(request).doOnSubscribe {
            liveStreamCreateEventSettingStatesSubject.onNext(
                LiveStreamCreateEventSettingState.LoadingSettingState(
                    true
                )
            )
        }.doAfterTerminate {
            liveStreamCreateEventSettingStatesSubject.onNext(
                LiveStreamCreateEventSettingState.LoadingSettingState(
                    false
                )
            )
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.data != null) {
                liveStreamCreateEventSettingStatesSubject.onNext(
                    LiveStreamCreateEventSettingState.LoadCreateEventInfo(
                        it.data
                    )
                )
            }
        }, { throwable ->
            Timber.e(throwable)
            throwable.localizedMessage?.let {
                liveStreamCreateEventSettingStatesSubject.onNext(
                    LiveStreamCreateEventSettingState.ErrorMessage(
                        it
                    )
                )
            }
        }).autoDispose()
    }
}

sealed class LiveStreamCreateEventSettingState {
    data class ErrorMessage(val errorMessage: String) : LiveStreamCreateEventSettingState()
    data class SuccessMessage(val successMessage: String) : LiveStreamCreateEventSettingState()
    data class LoadingSettingState(val isLoading: Boolean) : LiveStreamCreateEventSettingState()
    data class LoadCreateEventInfo(val liveEventInfo: LiveEventInfo) :
        LiveStreamCreateEventSettingState()
}
