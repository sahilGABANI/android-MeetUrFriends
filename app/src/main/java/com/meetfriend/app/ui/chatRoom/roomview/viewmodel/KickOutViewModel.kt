package com.meetfriend.app.ui.chatRoom.roomview.viewmodel

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class KickOutViewModel(
    private val chatRepository: ChatRepository,
    private val loggedInUserCache: LoggedInUserCache,
) : BasicViewModel() {

    private val chatRoomMessageStateSubject: PublishSubject<KickOutViewState> =
        PublishSubject.create()
    val chatRoomMessageState: Observable<KickOutViewState> = chatRoomMessageStateSubject.hide()

    init {
        observeNewMessage()
        observeKickedOutUser()
    }

    fun sendPrivateMessage(sendPrivateMessageRequest: SendPrivateMessageRequest) {
        chatRepository.sendPrivateMessage(sendPrivateMessageRequest).doOnSubscribe {
            chatRoomMessageStateSubject.onNext(KickOutViewState.LoadingState(true))
        }.doAfterTerminate {
            chatRoomMessageStateSubject.onNext(KickOutViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({

        }, {

        })
    }

    private fun observeNewMessage() {
        chatRepository.observeNewMessage().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(KickOutViewState.GetNewSendMessage(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeKickedOutUser() {
        chatRepository.observeKickedOutUser().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(KickOutViewState.KickedOutData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }


}

sealed class KickOutViewState {
    data class ErrorMessage(val errorMessage: String) : KickOutViewState()
    data class SuccessMessage(val successMessage: String) : KickOutViewState()
    data class LoadingState(val isLoading: Boolean) : KickOutViewState()
    data class GetNewSendMessage(val chatMessage: MessageInfo) : KickOutViewState()
    data class KickedOutData(val chatMessage: MessageInfo) : KickOutViewState()

}