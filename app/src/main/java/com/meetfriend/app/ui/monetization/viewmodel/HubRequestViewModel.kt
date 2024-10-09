package com.meetfriend.app.ui.monetization.viewmodel

import com.meetfriend.app.api.monetization.MonetizationRepository
import com.meetfriend.app.api.monetization.model.HubRequestInfo
import com.meetfriend.app.api.monetization.model.SendHubRequestRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HubRequestViewModel(
    private val monetizationRepository: MonetizationRepository
) : BasicViewModel() {

    private val hubRequestStateSubject: PublishSubject<HubRequestViewStates> = PublishSubject.create()
    val hubRequestState: Observable<HubRequestViewStates> = hubRequestStateSubject.hide()

    fun createHubRequest(request: SendHubRequestRequest) {
        monetizationRepository.createHubRequest(request)
            .doOnSubscribe {
                hubRequestStateSubject.onNext(HubRequestViewStates.LoadingState(true))
            }
            .doAfterSuccess {
                hubRequestStateSubject.onNext(HubRequestViewStates.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                hubRequestStateSubject.onNext(HubRequestViewStates.LoadingState(false))
                response?.let {
                    if (response.status) {
                        response.result?.let {
                            hubRequestStateSubject.onNext(
                                HubRequestViewStates.RequestData(
                                    response.result
                                )
                            )
                        }
                        hubRequestStateSubject.onNext(HubRequestViewStates.SuccessMessage(it.message.toString()))
                    }
                }

            }, { throwable ->
                hubRequestStateSubject.onNext(HubRequestViewStates.LoadingState(false))
                throwable.localizedMessage?.let {
                    hubRequestStateSubject.onNext(HubRequestViewStates.ErrorMessage(it))
                }
            }).autoDispose()
    }

}

sealed class HubRequestViewStates {
    data class ErrorMessage(val errorMessage: String) : HubRequestViewStates()
    data class SuccessMessage(val successMessage: String) : HubRequestViewStates()
    data class LoadingState(val isLoading: Boolean) : HubRequestViewStates()
    data class RequestData(val data: HubRequestInfo) : HubRequestViewStates()
}