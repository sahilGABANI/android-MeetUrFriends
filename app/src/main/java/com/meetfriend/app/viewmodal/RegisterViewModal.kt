package com.meetfriend.app.viewmodal

import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.repositories.ApiRepository
import com.meetfriend.app.responseclasses.CommonResponseClass
import com.meetfriend.app.responseclasses.DeviceDataResponse
import com.meetfriend.app.responseclasses.RegisterResponseClass
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class RegisterViewModal(
    private val apiRepository: ApiRepository
) : BasicViewModel() {

    private val forgotPasswordSubject: PublishSubject<ForgotPasswordViewState> = PublishSubject.create()
    val forgotPasswordState: Observable<ForgotPasswordViewState> = forgotPasswordSubject.hide()

    fun forgotPassword(requestModel: HashMap<String, Any>) {
        apiRepository.forgotPassword(requestModel)
            .doOnSubscribe {
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(true))
            }
            .doAfterSuccess {
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(false))
                if (response.status) {
                    forgotPasswordSubject.onNext(ForgotPasswordViewState.ForgotPassword(response))
                } else {
                    response.message?.let {
                        forgotPasswordSubject.onNext(ForgotPasswordViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage.let {
                    forgotPasswordSubject.onNext(ForgotPasswordViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
    fun register(requestModel: HashMap<String, Any>) {
        apiRepository.register(requestModel)
            .doOnSubscribe {
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(true))
            }
            .doAfterTerminate {
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(false))
                if (response.status) {
                    forgotPasswordSubject.onNext(ForgotPasswordViewState.Register(response))
                } else {
                    response.message?.let {
                        forgotPasswordSubject.onNext(ForgotPasswordViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage.let {
                    forgotPasswordSubject.onNext(ForgotPasswordViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deviceToken(requestModel: HashMap<String, Any>) {
        apiRepository.deviceToken(requestModel)
            .doOnSubscribe {
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(true))
            }
            .doAfterSuccess {
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(false))
                if (response.status) {
                    forgotPasswordSubject.onNext(ForgotPasswordViewState.DeviceToken(response))
                } else {
                    response.message?.let {
                        forgotPasswordSubject.onNext(ForgotPasswordViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage.let {
                    forgotPasswordSubject.onNext(ForgotPasswordViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun resetPassword(requestModel: HashMap<String, Any>) {
        apiRepository.resetPassword(requestModel)
            .doOnSubscribe {
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(true))
            }
            .doAfterSuccess {
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                forgotPasswordSubject.onNext(ForgotPasswordViewState.LoadingState(false))
                if (response.status) {
                    forgotPasswordSubject.onNext(ForgotPasswordViewState.ResetPassword(response))
                } else {
                    response.message?.let {
                        forgotPasswordSubject.onNext(ForgotPasswordViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage.let {
                    forgotPasswordSubject.onNext(ForgotPasswordViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class ForgotPasswordViewState {
    data class ErrorMessage(val errorMessage: String) : ForgotPasswordViewState()
    data class SuccessMessage(val successMessage: String) : ForgotPasswordViewState()
    data class LoadingState(val isLoading: Boolean) : ForgotPasswordViewState()
    data class DeviceToken(val deviceDataData: MeetFriendResponse<DeviceDataResponse>) : ForgotPasswordViewState()
    data class Register(val registerResponse: MeetFriendResponse<RegisterResponseClass>) : ForgotPasswordViewState()
    data class ForgotPassword(val commonResponse: MeetFriendResponse<CommonResponseClass>) : ForgotPasswordViewState()
    data class ResetPassword(val resetPassword: MeetFriendResponse<CommonResponseClass>) : ForgotPasswordViewState()
}
