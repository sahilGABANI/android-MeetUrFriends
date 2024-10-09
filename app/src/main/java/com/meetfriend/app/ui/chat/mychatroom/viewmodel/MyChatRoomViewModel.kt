package com.meetfriend.app.ui.chat.mychatroom.viewmodel

import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MyChatRoomViewModel(private val chatRepository: ChatRepository) : BasicViewModel() {
    companion object {
        const val PER_PAGE = 12
    }
    private val myChatRoomStateSubject: PublishSubject<MyChatRoomViewState> =
        PublishSubject.create()
    val myChatRoomState: Observable<MyChatRoomViewState> = myChatRoomStateSubject.hide()

    private val perPage = PER_PAGE
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfChatRoom: MutableList<ChatRoomInfo> = mutableListOf()

    private fun getMyChatRoom() {
        chatRepository.getMyPrivateChatRoom(perPage, pageNo)
            .doOnSubscribe {
                myChatRoomStateSubject.onNext(MyChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                myChatRoomStateSubject.onNext(MyChatRoomViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                isLoading = false
                response?.let {
                    if (response.status) {
                        if (pageNo == 1) {
                            if (response.result?.data != null) {
                                listOfChatRoom = response.result.data.toMutableList()
                                myChatRoomStateSubject.onNext(
                                    MyChatRoomViewState.ListOfMyChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            }
                        } else {
                            if (response.result?.data != null) {
                                listOfChatRoom.addAll(response.result.data)
                                myChatRoomStateSubject.onNext(
                                    MyChatRoomViewState.ListOfMyChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            } else {
                                isLoadMore = false
                            }
                        }
                    } else {
                        response.message?.let {
                            myChatRoomStateSubject.onNext(MyChatRoomViewState.ErrorMessage(response.message))
                        }
                    }
                }
            }, { throwable ->
                myChatRoomStateSubject.onNext(MyChatRoomViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    myChatRoomStateSubject.onNext(MyChatRoomViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun loadMoreMyChatRoom() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getMyChatRoom()
            }
        }
    }

    fun resetPaginationForMyChatRoom() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getMyChatRoom()
    }
}

sealed class MyChatRoomViewState {
    data class ErrorMessage(val errorMessage: String) : MyChatRoomViewState()
    data class SuccessMessage(val successMessage: String) : MyChatRoomViewState()
    data class LoadingState(val isLoading: Boolean) : MyChatRoomViewState()
    data class ListOfMyChatRoom(val listOfChatRoom: List<ChatRoomInfo>) : MyChatRoomViewState()
}
