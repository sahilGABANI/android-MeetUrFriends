package com.meetfriend.app.ui.livestreaming.viewmodel

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.livestreaming.LiveRepository
import com.meetfriend.app.api.livestreaming.model.RejectJoinRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UpdateInviteCoHostStatusViewModel(
    private val liveRepository: LiveRepository,
    private val loggedInUserCache: LoggedInUserCache
) : BasicViewModel() {

    private val updateInviteCoHostStatusStatesSubject: PublishSubject<UpdateInviteCoHostStatus> =
        PublishSubject.create()
    val updateInviteCoHostStatusStates: Observable<UpdateInviteCoHostStatus> =
        updateInviteCoHostStatusStatesSubject.hide()

    val loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser

    fun rejectAsCoHost(channelId: String) {
        liveRepository.rejectCoHosts(RejectJoinRequest(channelId))
            .doOnSubscribe {
                updateInviteCoHostStatusStatesSubject.onNext(
                    UpdateInviteCoHostStatus.LoadingSettingState(
                        true
                    )
                )
            }.doAfterTerminate {
                updateInviteCoHostStatusStatesSubject.onNext(
                    UpdateInviteCoHostStatus.LoadingSettingState(
                        false
                    )
                )
            }.subscribeOnIoAndObserveOnMainThread({
                updateInviteCoHostStatusStatesSubject.onNext(UpdateInviteCoHostStatus.RejectedCoHostRequest)
            }, {
                updateInviteCoHostStatusStatesSubject.onNext(
                    UpdateInviteCoHostStatus.ErrorMessage(
                        it.localizedMessage ?: ""
                    )
                )
            }).autoDispose()
    }

    sealed class UpdateInviteCoHostStatus {
        data class ErrorMessage(val errorMessage: String) : UpdateInviteCoHostStatus()
        data class SuccessMessage(val successMessage: String) : UpdateInviteCoHostStatus()
        data class LoadingSettingState(val isLoading: Boolean) : UpdateInviteCoHostStatus()
        object RejectedCoHostRequest : UpdateInviteCoHostStatus()
    }
}
