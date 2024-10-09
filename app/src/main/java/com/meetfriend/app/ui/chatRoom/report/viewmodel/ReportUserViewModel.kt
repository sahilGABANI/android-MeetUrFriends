package com.meetfriend.app.ui.chatRoom.report.viewmodel

import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.ReportUserInfo
import com.meetfriend.app.api.chat.model.ReportUserRequest
import com.meetfriend.app.api.hashtag.HashtagRepository
import com.meetfriend.app.api.hashtag.model.ReportHashtagRequest
import com.meetfriend.app.api.hashtag.model.ReportHashtagResponse
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.ui.hashtag.viewmodel.HashTagsViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReportUserViewModel(
    private val chatRepository: ChatRepository,
    private val hashtagRepository: HashtagRepository
) : BasicViewModel() {

    private val reportUserStateSubject: PublishSubject<ReportUserViewState> =
        PublishSubject.create()
    val reportUserState: Observable<ReportUserViewState> = reportUserStateSubject.hide()

    fun reportUser(reportUserRequest: ReportUserRequest) {
        chatRepository.reportUser(reportUserRequest).doOnSubscribe {
            reportUserStateSubject.onNext(ReportUserViewState.LoadingState(true))
        }.doAfterTerminate {
            reportUserStateSubject.onNext(ReportUserViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                reportUserStateSubject.onNext(ReportUserViewState.SuccessMessage(it.message.toString()))
                reportUserStateSubject.onNext(ReportUserViewState.SuccessMessage(it.message.toString()))
                reportUserStateSubject.onNext(ReportUserViewState.ReportUser(it.result))
            } else {
                reportUserStateSubject.onNext(ReportUserViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                reportUserStateSubject.onNext(ReportUserViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun reportHashtag(reportHashtagRequest: ReportHashtagRequest) {
        hashtagRepository.reportHashtagInfo(reportHashtagRequest)
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    it.data?.let { reportRes ->
                        reportUserStateSubject.onNext(
                            ReportUserViewState.ReportHashtag(
                            reportRes
                        ))
                    }
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
    data class ReportUser(val reportUserInfo: ReportUserInfo?) : ReportUserViewState()
    data class ReportHashtag(val hashtagResponse: ReportHashtagResponse) : ReportUserViewState()

}
