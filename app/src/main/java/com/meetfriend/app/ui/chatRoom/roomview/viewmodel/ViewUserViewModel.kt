package com.meetfriend.app.ui.chatRoom.roomview.viewmodel

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.RestrictRequest
import com.meetfriend.app.api.chat.model.RestrictUserRequest
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ViewUserViewModel(
    private val chatRepository: ChatRepository,
    private val loggedInUserCache: LoggedInUserCache,
) : BasicViewModel() {

    private val chatRoomMessageStateSubject: PublishSubject<ViewUserViewState> =
        PublishSubject.create()
    val chatRoomMessageState: Observable<ViewUserViewState> = chatRoomMessageStateSubject.hide()

    init {
        observeNewMessage()
        observeKickedOutUser()
        observeBannedUser()
        observeRestrictUser()
    }

    fun sendPrivateMessage(sendPrivateMessageRequest: SendPrivateMessageRequest) {
        chatRepository.sendPrivateMessage(sendPrivateMessageRequest).doOnSubscribe {
            chatRoomMessageStateSubject.onNext(ViewUserViewState.LoadingState(true))
        }.doAfterTerminate {
            chatRoomMessageStateSubject.onNext(ViewUserViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({

        }, {

        })
    }

    private fun observeNewMessage() {
        chatRepository.observeNewMessage().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ViewUserViewState.GetNewSendMessage(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeKickedOutUser() {
        chatRepository.observeKickedOutUser().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ViewUserViewState.KickedOutData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeBannedUser() {
        chatRepository.observeBannedUser().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ViewUserViewState.BannedUserData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun restrictUser(restrictUserRequest: RestrictUserRequest) {
        chatRepository.restrictUser(restrictUserRequest).doOnSubscribe {
            chatRoomMessageStateSubject.onNext(ViewUserViewState.LoadingState(true))
        }.doAfterTerminate {
            chatRoomMessageStateSubject.onNext(ViewUserViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ViewUserViewState.RestrictUser(it.message))
        }, {

        })
    }

    private fun observeRestrictUser() {
        chatRepository.observeRestrictUser().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ViewUserViewState.RestrictUserData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

}

sealed class ViewUserViewState {
    data class ErrorMessage(val errorMessage: String) : ViewUserViewState()
    data class SuccessMessage(val successMessage: String) : ViewUserViewState()
    data class LoadingState(val isLoading: Boolean) : ViewUserViewState()
    data class GetNewSendMessage(val chatMessage: MessageInfo) : ViewUserViewState()
    data class KickedOutData(val chatMessage: MessageInfo) : ViewUserViewState()
    data class BannedUserData(val chatMessage: MessageInfo) : ViewUserViewState()
    data class RestrictUserData(val chatMessage: RestrictRequest) : ViewUserViewState()
    data class RestrictUser(val successMessage: String?) : ViewUserViewState()

}