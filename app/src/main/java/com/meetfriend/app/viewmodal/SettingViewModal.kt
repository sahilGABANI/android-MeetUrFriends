package com.meetfriend.app.viewmodal

import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.repositories.ApiRepository
import com.meetfriend.app.responseclasses.CommonResponseClass
import com.meetfriend.app.responseclasses.settings.SecurityManagementResult
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SettingViewModal(
    private val apiRepository: ApiRepository
) : BasicViewModel() {
    private val settingStateSubject: PublishSubject<SettingViewState> = PublishSubject.create()
    val settingState: Observable<SettingViewState> = settingStateSubject.hide()
    fun fetchSecurity(requestModel: HashMap<String, Any>) {
        apiRepository.fetchSecurity(requestModel)
            .doOnSubscribe {
                settingStateSubject.onNext(SettingViewState.LoadingState(true))
            }
            .doAfterSuccess {
                settingStateSubject.onNext(SettingViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->

                settingStateSubject.onNext(SettingViewState.LoadingState(false))
                if (response.status) {
                    settingStateSubject.onNext(SettingViewState.SecurityData(response))
                } else {
                    response.message?.let {
                        settingStateSubject.onNext(SettingViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    settingStateSubject.onNext(SettingViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    // Delete security method
    fun deleteSecurity(requestModel: HashMap<String, Any>) {
        apiRepository.deleteSecurity(requestModel)
            .doOnSubscribe {
                settingStateSubject.onNext(SettingViewState.LoadingState(true))
            }
            .doAfterSuccess {
                settingStateSubject.onNext(SettingViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                settingStateSubject.onNext(SettingViewState.LoadingState(false))
                if (response.status) {
                    settingStateSubject.onNext(SettingViewState.DeleteSecurityData(response))
                } else {
                    response.message?.let {
                        settingStateSubject.onNext(SettingViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage.let {
                    settingStateSubject.onNext(SettingViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class SettingViewState {
    data class ErrorMessage(val errorMessage: String) : SettingViewState()
    data class SuccessMessage(val successMessage: String) : SettingViewState()
    data class LoadingState(val isLoading: Boolean) : SettingViewState()
    data class SecurityData(val securityData: MeetFriendResponse<SecurityManagementResult>) : SettingViewState()
    data class DeleteSecurityData(val deleteResponse: MeetFriendResponse<CommonResponseClass>) : SettingViewState()
}
