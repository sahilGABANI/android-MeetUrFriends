package com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel

import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OneToOneChatRoomViewModel(private val chatRepository: ChatRepository) : BasicViewModel() {
    companion object {
        const val PER_PAGE = 12
    }
    private val oneToOneChatRoomStateSubject: PublishSubject<OneToOneChatRoomViewState> =
        PublishSubject.create()
    val oneToOneChatRoomState: Observable<OneToOneChatRoomViewState> =
        oneToOneChatRoomStateSubject.hide()

    private val perPage = PER_PAGE
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfChatRoom: MutableList<ChatRoomInfo> = mutableListOf()

    private fun getOneToOneChatRoom() {
        chatRepository.getOneToOneChatRoom(perPage, pageNo)
            .doOnSubscribe {
                oneToOneChatRoomStateSubject.onNext(OneToOneChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                oneToOneChatRoomStateSubject.onNext(OneToOneChatRoomViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                isLoading = false
                response?.let {
                    if (response.status) {
                        if (pageNo == 1) {
                            oneToOneChatRoomStateSubject.onNext(
                                OneToOneChatRoomViewState.UnReadMessageState(
                                    response.totalUnreadCount
                                )
                            )
                            if (response.result?.data != null) {
                                listOfChatRoom = response.result.data.toMutableList()
                                oneToOneChatRoomStateSubject.onNext(
                                    OneToOneChatRoomViewState.ListOfOneToOneChatRoom(
                                        listOfChatRoom as ArrayList<ChatRoomInfo>
                                    )
                                )
                            }
                        } else {
                            if (response.result?.data != null) {
                                listOfChatRoom.addAll(response.result.data)
                                oneToOneChatRoomStateSubject.onNext(
                                    OneToOneChatRoomViewState.ListOfOneToOneChatRoom(
                                        listOfChatRoom as ArrayList<ChatRoomInfo>
                                    )
                                )
                            } else {
                                isLoadMore = false
                            }
                        }
                    } else {
                        response.message?.let {
                            oneToOneChatRoomStateSubject.onNext(
                                OneToOneChatRoomViewState.ErrorMessage(
                                    response.message
                                )
                            )
                        }
                    }
                }
            }, { throwable ->
                oneToOneChatRoomStateSubject.onNext(OneToOneChatRoomViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    oneToOneChatRoomStateSubject.onNext(OneToOneChatRoomViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteChatRoom(conversationId: Int) {
        chatRepository.deleteChatRoom(conversationId).doOnSubscribe {
            oneToOneChatRoomStateSubject.onNext(OneToOneChatRoomViewState.DeleteLoadingState(true))
        }.doAfterTerminate {
            oneToOneChatRoomStateSubject.onNext(OneToOneChatRoomViewState.DeleteLoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                oneToOneChatRoomStateSubject.onNext(OneToOneChatRoomViewState.DeleteChatRoom)
            } else {
                oneToOneChatRoomStateSubject.onNext(OneToOneChatRoomViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                oneToOneChatRoomStateSubject.onNext(OneToOneChatRoomViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun loadMoreOneToOneChatRoom() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getOneToOneChatRoom()
            }
        }
    }

    fun resetPaginationForOneToOneChatRoom() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getOneToOneChatRoom()
    }
}

sealed class OneToOneChatRoomViewState {
    data class ErrorMessage(val errorMessage: String) : OneToOneChatRoomViewState()
    data class SuccessMessage(val successMessage: String) : OneToOneChatRoomViewState()
    data class LoadingState(val isLoading: Boolean) : OneToOneChatRoomViewState()
    data class ListOfOneToOneChatRoom(val listOfChatRoom: ArrayList<ChatRoomInfo>) :
        OneToOneChatRoomViewState()

    data class DeleteLoadingState(val isLoading: Boolean) : OneToOneChatRoomViewState()
    data class UnReadMessageState(val count: Int?) : OneToOneChatRoomViewState()
    object DeleteChatRoom : OneToOneChatRoomViewState()
}
