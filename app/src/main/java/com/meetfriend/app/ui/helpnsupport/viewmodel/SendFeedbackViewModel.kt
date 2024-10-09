package com.meetfriend.app.ui.helpnsupport.viewmodel

import com.meetfriend.app.api.profile.ProfileRepository
import com.meetfriend.app.api.profile.model.SendFeedbackRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SendFeedbackViewModel(private val profileRepository: ProfileRepository) : BasicViewModel() {

    private val feedbackStateSubject: PublishSubject<FeedbackViewState> = PublishSubject.create()
    val feedbackState: Observable<FeedbackViewState> = feedbackStateSubject.hide()

    fun sendFeedback(rating: Float, message: String) {
        profileRepository.sendFeedback(SendFeedbackRequest(rating, message))
            .doOnSubscribe {
                feedbackStateSubject.onNext(FeedbackViewState.LoadingState(true))
            }.doAfterSuccess {
                feedbackStateSubject.onNext(FeedbackViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                feedbackStateSubject.onNext(FeedbackViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        feedbackStateSubject.onNext(FeedbackViewState.SuccessMessage(it))
                    }
                } else {
                    response.message?.let {
                        feedbackStateSubject.onNext(FeedbackViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    feedbackStateSubject.onNext(FeedbackViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

}

sealed class FeedbackViewState {
    data class ErrorMessage(val errorMessage: String) : FeedbackViewState()
    data class SuccessMessage(val successMessage: String) : FeedbackViewState()
    data class LoadingState(val isLoading: Boolean) : FeedbackViewState()
}