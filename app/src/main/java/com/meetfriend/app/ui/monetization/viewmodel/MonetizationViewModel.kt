package com.meetfriend.app.ui.monetization.viewmodel

import com.meetfriend.app.api.profile.ProfileRepository
import com.meetfriend.app.api.profile.model.ProfileValidationInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MonetizationViewModel(
    private val profileRepository: ProfileRepository,
) : BasicViewModel() {

    private val monetizationStateSubject: PublishSubject<MonetizationViewStates> =
        PublishSubject.create()
    val monetizationState: Observable<MonetizationViewStates> = monetizationStateSubject.hide()

    fun getProfileInfo(userId: Int) {
        profileRepository.getProfileInfo(userId)
            .doOnSubscribe {
                monetizationStateSubject.onNext(MonetizationViewStates.LoadingState(true))
            }
            .doAfterSuccess {
                monetizationStateSubject.onNext(MonetizationViewStates.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (response.status) {
                        response.result?.let {
                            monetizationStateSubject.onNext(
                                MonetizationViewStates.ProfileData(
                                    response.result
                                )
                            )
                        }
                    }
                }

            }, { throwable ->
                monetizationStateSubject.onNext(MonetizationViewStates.LoadingState(false))
                throwable.localizedMessage?.let {
                    monetizationStateSubject.onNext(MonetizationViewStates.ErrorMessage(it))
                }
            }).autoDispose()
    }

}

sealed class MonetizationViewStates {
    data class ErrorMessage(val errorMessage: String) : MonetizationViewStates()
    data class SuccessMessage(val successMessage: String) : MonetizationViewStates()
    data class LoadingState(val isLoading: Boolean) : MonetizationViewStates()
    data class ProfileData(val data: ProfileValidationInfo) : MonetizationViewStates()
}