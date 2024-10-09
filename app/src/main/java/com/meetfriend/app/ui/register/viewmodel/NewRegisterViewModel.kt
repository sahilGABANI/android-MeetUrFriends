package com.meetfriend.app.ui.register.viewmodel

import com.meetfriend.app.api.authentication.AuthenticationRepository
import com.meetfriend.app.api.authentication.model.DeviceIdRequest
import com.meetfriend.app.api.authentication.model.LoginRequest
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.authentication.model.RegisterRequest
import com.meetfriend.app.api.authentication.model.SwitchDeviceAccountRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewRegisterViewModel(private val authenticationRepository: AuthenticationRepository) :
    BasicViewModel() {

    private val registerStateSubject: PublishSubject<RegisterViewState> = PublishSubject.create()
    val registerState: Observable<RegisterViewState> = registerStateSubject.hide()

    fun register(registerRequest: RegisterRequest) {
        authenticationRepository.register(registerRequest)
            .doOnSubscribe {
                registerStateSubject.onNext(RegisterViewState.LoadingState(true))
            }
            .doAfterSuccess {
                registerStateSubject.onNext(RegisterViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                registerStateSubject.onNext(RegisterViewState.LoadingState(false))
                if (response.status) {
                    registerStateSubject.onNext(RegisterViewState.SuccessMessage(response.message.toString()))
                    registerStateSubject.onNext(RegisterViewState.SuccessRegister(response))
                } else {
                    response.message?.let {
                        registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun login(loginRequest: LoginRequest) {
        authenticationRepository.login(loginRequest)
            .doOnSubscribe {
                registerStateSubject.onNext(RegisterViewState.LoadingState(true))
            }
            .doAfterTerminate {
                registerStateSubject.onNext(RegisterViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                registerStateSubject.onNext(RegisterViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        registerStateSubject.onNext(RegisterViewState.SuccessMessage(it))
                    }
                    registerStateSubject.onNext(RegisterViewState.LoginData(response))
                } else {
                    response.message?.let {
                        registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getListOfAccount(deviceID: String) {
        authenticationRepository.getListOfAccount(DeviceIdRequest(deviceID))
            .doOnSubscribe {
                registerStateSubject.onNext(RegisterViewState.LoadingState(true))
            }
            .doAfterTerminate {
                registerStateSubject.onNext(RegisterViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                registerStateSubject.onNext(RegisterViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        registerStateSubject.onNext(RegisterViewState.SuccessMessage(it))
                    }
                    registerStateSubject.onNext(RegisterViewState.GetListOfUser(response))
                } else {
                    response.message?.let {
                        registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun switchAccount(switchDeviceAccountRequest: SwitchDeviceAccountRequest) {
        authenticationRepository.switchAccount(switchDeviceAccountRequest)
            .doOnSubscribe {
                registerStateSubject.onNext(RegisterViewState.LoadingState(true))
            }
            .doAfterTerminate {
                registerStateSubject.onNext(RegisterViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                registerStateSubject.onNext(RegisterViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        registerStateSubject.onNext(RegisterViewState.SuccessMessage(it))
                    }
                    registerStateSubject.onNext(RegisterViewState.SwitchAccount)
                } else {
                    response.message?.let {
                        registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class RegisterViewState {
    data class ErrorMessage(val errorMessage: String) : RegisterViewState()
    data class SuccessMessage(val successMessage: String) : RegisterViewState()
    data class LoadingState(val isLoading: Boolean) : RegisterViewState()
    data class SuccessRegister(val registerResponse: MeetFriendResponse<MeetFriendUser>) :
        RegisterViewState()

    data class LoginData(val loginData: MeetFriendResponse<MeetFriendUser>) : RegisterViewState()
    data class GetListOfUser(val loginData: MeetFriendResponse<List<MeetFriendUser>>) : RegisterViewState()
    object SwitchAccount : RegisterViewState()
}
