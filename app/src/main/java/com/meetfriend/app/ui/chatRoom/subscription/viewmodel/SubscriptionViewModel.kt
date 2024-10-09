package com.meetfriend.app.ui.chatRoom.subscription.viewmodel

import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.subscription.SubscriptionRepository
import com.meetfriend.app.api.subscription.model.AdminSubscriptionRequest
import com.meetfriend.app.api.subscription.model.SubscriptionRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SubscriptionViewModel(private val subscriptionRepository: SubscriptionRepository) :
    BasicViewModel() {

    private val subscriptionStateSubject: PublishSubject<SubscriptionViewState> =
        PublishSubject.create()
    val subscriptionState: Observable<SubscriptionViewState> = subscriptionStateSubject.hide()

    fun updateSubscription(conversationId: Int, subscriptionRequest: SubscriptionRequest) {
        subscriptionRepository.updateSubscription(conversationId, subscriptionRequest)
            .doOnSubscribe {
                subscriptionStateSubject.onNext(SubscriptionViewState.LoadingState(true))
            }.doAfterTerminate {
                subscriptionStateSubject.onNext(SubscriptionViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({
                // subscriptionStateSubject.onNext(SubscriptionViewState.SubscriptionSuccess)
                if (it.status) {
                    if (it.data != null) {
                        subscriptionStateSubject.onNext(SubscriptionViewState.SubscriptionData(it.data))
                    }
                } else {
                    subscriptionStateSubject.onNext(SubscriptionViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    subscriptionStateSubject.onNext(SubscriptionViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun updateAdminSubscription(
        conversationId: Int,
        adminSubscriptionRequest: AdminSubscriptionRequest
    ) {
        subscriptionRepository.updateAdminSubscription(conversationId, adminSubscriptionRequest)
            .doOnSubscribe {
                subscriptionStateSubject.onNext(SubscriptionViewState.LoadingState(true))
            }.doAfterTerminate {
                subscriptionStateSubject.onNext(SubscriptionViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    if (it.data != null) {
                        subscriptionStateSubject.onNext(
                            SubscriptionViewState.AdminSubscriptionData(
                                it.data
                            )
                        )
                    }
                } else {
                    subscriptionStateSubject.onNext(SubscriptionViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    subscriptionStateSubject.onNext(SubscriptionViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class SubscriptionViewState {
    data class ErrorMessage(val errorMessage: String) : SubscriptionViewState()
    data class SuccessMessage(val successMessage: String) : SubscriptionViewState()
    data class LoadingState(val isLoading: Boolean) : SubscriptionViewState()
    object SubscriptionSuccess : SubscriptionViewState()
    data class SubscriptionData(val chatRoomInfo: ChatRoomInfo) : SubscriptionViewState()
    data class AdminSubscriptionData(val chatRoomInfo: ChatRoomInfo) : SubscriptionViewState()
}
