package com.meetfriend.app.ui.login.viewmodel

import com.meetfriend.app.api.authentication.AuthenticationRepository
import com.meetfriend.app.api.authentication.model.LoginRequest
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LoginViewModel(
    private val authenticationRepository: AuthenticationRepository
) : BasicViewModel() {

    private val loginStateSubject: PublishSubject<LoginViewState> = PublishSubject.create()
    val loginState: Observable<LoginViewState> = loginStateSubject.hide()

    fun login(loginRequest: LoginRequest) {
        authenticationRepository.login(loginRequest)
            .doOnSubscribe {
                loginStateSubject.onNext(LoginViewState.LoadingState(true))
            }
            .doAfterTerminate {
                loginStateSubject.onNext(LoginViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                loginStateSubject.onNext(LoginViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        loginStateSubject.onNext(LoginViewState.SuccessMessage(it))
                    }
                    loginStateSubject.onNext(LoginViewState.LoginData(response))
                } else {
                    response.message?.let {
                        loginStateSubject.onNext(LoginViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    loginStateSubject.onNext(LoginViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class LoginViewState {
    data class ErrorMessage(val errorMessage: String) : LoginViewState()
    data class SuccessMessage(val successMessage: String) : LoginViewState()
    data class LoadingState(val isLoading: Boolean) : LoginViewState()
    data class LoginData(val loginData: MeetFriendResponse<MeetFriendUser>) : LoginViewState()
}
