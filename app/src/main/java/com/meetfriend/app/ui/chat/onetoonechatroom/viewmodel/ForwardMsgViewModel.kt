package com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel

import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.messages.MessagesRepository
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.utils.Constant.FiXED_12_INT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ForwardMsgViewModel(
    private val chatRepository: ChatRepository,
    private val messagesRepository: MessagesRepository
) : BasicViewModel() {

    private val forwardMsgStateSubject: PublishSubject<ForwardMsgViewState> =
        PublishSubject.create()
    val forwardMsgState: Observable<ForwardMsgViewState> = forwardMsgStateSubject.hide()

    private val perPage = FiXED_12_INT
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var fpageNo = 1
    private var fisLoading = false
    private var fisLoadMore = true

    private var listOfChatRoom: MutableList<ChatRoomInfo> = mutableListOf()

    private fun getForwardPeopleList() {
        chatRepository.getOneToOneChatRoom(perPage, pageNo)
            .doOnSubscribe {
                forwardMsgStateSubject.onNext(ForwardMsgViewState.LoadingState(true))
            }
            .doAfterSuccess {
                forwardMsgStateSubject.onNext(ForwardMsgViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                isLoading = false
                response?.let {
                    if (response.status) {
                        if (pageNo == 1) {
                            if (response.result?.data != null) {
                                listOfChatRoom = response.result.data.toMutableList()
                                forwardMsgStateSubject.onNext(
                                    ForwardMsgViewState.ListOfOneToOneChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            }
                        } else {
                            if (response.result?.data != null) {
                                listOfChatRoom.addAll(response.result.data)
                                forwardMsgStateSubject.onNext(
                                    ForwardMsgViewState.ListOfOneToOneChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            } else {
                                isLoadMore = false
                            }
                        }
                    } else {
                        response.message?.let {
                            forwardMsgStateSubject.onNext(ForwardMsgViewState.ErrorMessage(response.message))
                        }
                    }
                }
            }, { throwable ->
                forwardMsgStateSubject.onNext(ForwardMsgViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    forwardMsgStateSubject.onNext(ForwardMsgViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun loadMoreOneToOneChatRoom() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getForwardPeopleList()
            }
        }
    }

    fun resetPaginationForOneToOneChatRoom() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getForwardPeopleList()
    }

    private fun getForwardNormalPeopleList() {
        messagesRepository.getOneToOneChatRoom(perPage, fpageNo, null)
            .doOnSubscribe {
                forwardMsgStateSubject.onNext(ForwardMsgViewState.LoadingState(true))
            }
            .doAfterSuccess {
                forwardMsgStateSubject.onNext(ForwardMsgViewState.LoadingState(false))
                fisLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                fisLoading = false
                response?.let {
                    if (response.status) {
                        if (pageNo == 1) {
                            if (response.result?.data != null) {
                                listOfChatRoom = response.result.data.toMutableList()
                                forwardMsgStateSubject.onNext(
                                    ForwardMsgViewState.ListOfOneToOneChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            }
                        } else {
                            if (response.result?.data != null) {
                                listOfChatRoom.addAll(response.result.data)
                                forwardMsgStateSubject.onNext(
                                    ForwardMsgViewState.ListOfOneToOneChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            } else {
                                fisLoadMore = false
                            }
                        }
                    } else {
                        response.message?.let {
                            forwardMsgStateSubject.onNext(ForwardMsgViewState.ErrorMessage(response.message))
                        }
                    }
                }
            }, { throwable ->
                forwardMsgStateSubject.onNext(ForwardMsgViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    forwardMsgStateSubject.onNext(ForwardMsgViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun loadNormalMoreOneToOneChatRoom() {
        if (!fisLoading) {
            fisLoading = true
            if (fisLoadMore) {
                fpageNo += 1
                getForwardNormalPeopleList()
            }
        }
    }

    fun resetPaginationForOneToOneNormalChatRoom() {
        fpageNo = 1
        fisLoading = false
        fisLoadMore = true
        getForwardNormalPeopleList()
    }
}

sealed class ForwardMsgViewState {
    data class ErrorMessage(val errorMessage: String) : ForwardMsgViewState()
    data class SuccessMessage(val successMessage: String) : ForwardMsgViewState()
    data class LoadingState(val isLoading: Boolean) : ForwardMsgViewState()
    data class ListOfOneToOneChatRoom(val listOfChatRoom: List<ChatRoomInfo>) :
        ForwardMsgViewState()
}
