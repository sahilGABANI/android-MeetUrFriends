package com.meetfriend.app.ui.messages.viewmodel

import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.messages.MessagesRepository
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MessageChatRoomViewModel(private val chatRepository: MessagesRepository) : BasicViewModel() {

    private val messageChatRoomStateSubject: PublishSubject<MessagesChatRoomViewState> =
        PublishSubject.create()
    val messageChatRoomState: Observable<MessagesChatRoomViewState> =
        messageChatRoomStateSubject.hide()

    private val PER_PAGE = 12
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfChatRoom: MutableList<ChatRoomInfo> = mutableListOf()

    private fun getOneToOneChatRoom(search: String?) {
        chatRepository.getOneToOneChatRoom(PER_PAGE, pageNo, search)
            .doOnSubscribe {
                if (search.isNullOrEmpty())
                    messageChatRoomStateSubject.onNext(MessagesChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                messageChatRoomStateSubject.onNext(MessagesChatRoomViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                isLoading = false
                val isSearch = !search.isNullOrEmpty()
                response?.let {
                    if (response.status) {
                        if (pageNo == 1) {
                            if (response.result?.data != null) {

                                listOfChatRoom = response.result.data.toMutableList()
                                messageChatRoomStateSubject.onNext(
                                    MessagesChatRoomViewState.ListOfOneToOneChatRoom(
                                        listOfChatRoom, isSearch
                                    )
                                )
                            }
                        } else {
                            if (response.result?.data != null) {
                                listOfChatRoom.addAll(response.result.data)
                                messageChatRoomStateSubject.onNext(
                                    MessagesChatRoomViewState.ListOfOneToOneChatRoom(
                                        listOfChatRoom, isSearch
                                    )
                                )
                            } else {
                                isLoadMore = false
                            }
                        }
                    } else {
                        response.message?.let {
                            messageChatRoomStateSubject.onNext(
                                MessagesChatRoomViewState.ErrorMessage(
                                    response.message
                                )
                            )
                        }
                    }
                }

            }, { throwable ->
                messageChatRoomStateSubject.onNext(MessagesChatRoomViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    messageChatRoomStateSubject.onNext(MessagesChatRoomViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun loadMoreOneToOneChatRoom(search: String?) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getOneToOneChatRoom(search)
            }
        }
    }

    fun resetPaginationForOneToOneChatRoom(search: String?) {
        listOfChatRoom.clear()
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getOneToOneChatRoom(search)
    }


    fun deleteChatRoom(conversationId: Int) {
        chatRepository.deleteConversation(conversationId).doOnSubscribe {
            messageChatRoomStateSubject.onNext(MessagesChatRoomViewState.DeleteLoadingState(true))
        }.doAfterTerminate {
            messageChatRoomStateSubject.onNext(MessagesChatRoomViewState.DeleteLoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                messageChatRoomStateSubject.onNext(MessagesChatRoomViewState.DeleteChatRoom)
            } else {
                messageChatRoomStateSubject.onNext(MessagesChatRoomViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                messageChatRoomStateSubject.onNext(MessagesChatRoomViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
}


sealed class MessagesChatRoomViewState {
    data class ErrorMessage(val errorMessage: String) : MessagesChatRoomViewState()
    data class SuccessMessage(val successMessage: String) : MessagesChatRoomViewState()
    data class LoadingState(val isLoading: Boolean) : MessagesChatRoomViewState()
    data class ListOfOneToOneChatRoom(
        val listOfChatRoom: List<ChatRoomInfo>,
        val isSearch: Boolean
    ) : MessagesChatRoomViewState()

    data class DeleteLoadingState(val isLoading: Boolean) : MessagesChatRoomViewState()
    object DeleteChatRoom : MessagesChatRoomViewState()
}