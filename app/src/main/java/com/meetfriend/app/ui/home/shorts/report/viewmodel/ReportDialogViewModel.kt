package com.meetfriend.app.ui.home.shorts.report.viewmodel

import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.PostReportRequest
import com.meetfriend.app.api.post.model.ReportLiveRequest
import com.meetfriend.app.api.story.StoryRepository
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReportDialogViewModel(
    private val postRepository: PostRepository,
    private val storyRepository: StoryRepository
) : BasicViewModel() {

    private val reportDialogStateSubject: PublishSubject<ReportDialogViewState> =
        PublishSubject.create()
    val reportDialogState: Observable<ReportDialogViewState> = reportDialogStateSubject.hide()

    fun reportOrHidePost(postReportRequest: PostReportRequest) {
        postRepository.reportOrHidePost(postReportRequest)
            .doOnSubscribe {
                reportDialogStateSubject.onNext(ReportDialogViewState.LoadingState(true))
            }.doAfterSuccess {
                reportDialogStateSubject.onNext(ReportDialogViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    reportDialogStateSubject.onNext(ReportDialogViewState.SuccessMessage(response.message.toString()))
                }
            }, { throwable ->
                reportDialogStateSubject.onNext(ReportDialogViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    reportDialogStateSubject.onNext(ReportDialogViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun reportLiveStreaming(liveId: Int, reason: String) {
        postRepository.reportLiveStreaming(ReportLiveRequest(liveId, reason))
            .doOnSubscribe {
                reportDialogStateSubject.onNext(ReportDialogViewState.LoadingState(true))
            }.doAfterSuccess {
                reportDialogStateSubject.onNext(ReportDialogViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    reportDialogStateSubject.onNext(ReportDialogViewState.SuccessMessage(response.message.toString()))
                }
            }, { throwable ->
                reportDialogStateSubject.onNext(ReportDialogViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    reportDialogStateSubject.onNext(ReportDialogViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun reportStory(userId: String, reason: String) {
        storyRepository.reportStory(userId, reason).doOnSubscribe {
            reportDialogStateSubject.onNext(ReportDialogViewState.LoadingState(true))
        }.doAfterSuccess {
            reportDialogStateSubject.onNext(ReportDialogViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            reportDialogStateSubject.onNext(ReportDialogViewState.SuccessMessage(response.message.toString()))
        }, { throwable ->
            reportDialogStateSubject.onNext(ReportDialogViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                reportDialogStateSubject.onNext(ReportDialogViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
}

sealed class ReportDialogViewState {
    data class ErrorMessage(val errorMessage: String) : ReportDialogViewState()
    data class SuccessMessage(val successMessage: String) : ReportDialogViewState()
    data class LoadingState(val isLoading: Boolean) : ReportDialogViewState()
}
