package com.meetfriend.app.ui.myprofile.report.viewmodel

import com.meetfriend.app.api.userprofile.UserProfileRepository
import com.meetfriend.app.api.userprofile.model.ReportUserRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReportMainUserViewModel(private val userProfileRepository: UserProfileRepository) :
    BasicViewModel() {

    private val reportUserStateSubject: PublishSubject<ReportUserViewState> =
        PublishSubject.create()
    val reportUserState: Observable<ReportUserViewState> = reportUserStateSubject.hide()

    fun reportUser(reportFor: Int, reason: String) {
        userProfileRepository.reportUser(ReportUserRequest(reportFor, reason))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    reportUserStateSubject.onNext(ReportUserViewState.ReportUserSuccess(it.message.toString()))
                } else {
                    reportUserStateSubject.onNext(ReportUserViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reportUserStateSubject.onNext(ReportUserViewState.ErrorMessage(it))
                }

            }).autoDispose()
    }
}

sealed class ReportUserViewState {
    data class ErrorMessage(val errorMessage: String) : ReportUserViewState()
    data class SuccessMessage(val successMessage: String) : ReportUserViewState()
    data class LoadingState(val isLoading: Boolean) : ReportUserViewState()
    data class ReportUserSuccess(val successMessage: String) : ReportUserViewState()

}