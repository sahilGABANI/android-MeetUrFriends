package com.meetfriend.app.ui.chatRoom.viewmodel

import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.SendJoinChatRoomRequestRequest
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.notification.NotificationRepository
import com.meetfriend.app.api.notification.model.AcceptRejectRequestRequest
import com.meetfriend.app.api.profile.ProfileRepository
import com.meetfriend.app.api.profile.model.ChatRoomUserProfileInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForGetProfile
import com.meetfriend.app.utils.Constant.FiXED_12_INT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ChatRoomViewModel(
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val notificationRepository: NotificationRepository,

) : BasicViewModel() {

    private val chatRoomStateSubject: PublishSubject<ChatRoomViewState> = PublishSubject.create()
    val chatRoomState: Observable<ChatRoomViewState> = chatRoomStateSubject.hide()

    init {
        observeKickedOutUser()
        observeNewMessage()
        observeBannedUser()
    }
    companion object {
        const val PER_PAGE = 12
    }


    private val perPage = PER_PAGE
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfChatRoom: MutableList<ChatRoomInfo> = mutableListOf()

    fun deleteAccount(deleteAccountRequest: FollowUnfollowRequest) {
        profileRepository.deleteAccount(deleteAccountRequest)
            .doOnSubscribe {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (response.status) {
                        response.message?.let {
                            chatRoomStateSubject.onNext(ChatRoomViewState.DeleteUserSuccessMessage(response.message))
                        }
                    } else {
                        response.message?.let {
                            chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(response.message))
                        }
                    }
                }
            }, { throwable ->
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun getPrivateChatRoom() {
        chatRepository.getPrivateChatRoom(pageNo, perPage)
            .doOnSubscribe {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                isLoading = false
                response?.let {
                    if (response.status) {
                        if (pageNo == 1) {
                            if (response.result?.data != null) {
                                listOfChatRoom = response.result.data.toMutableList()
                                chatRoomStateSubject.onNext(
                                    ChatRoomViewState.ListOfChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            }
                        } else {
                            if (response.result?.data != null) {
                                listOfChatRoom.addAll(response.result.data)
                                chatRoomStateSubject.onNext(
                                    ChatRoomViewState.ListOfChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            } else {
                                isLoadMore = false
                            }
                        }
                    } else {
                        response.message?.let {
                            chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(response.message))
                        }
                    }
                }
            }, { throwable ->
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun loadMorePrivateChatList() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getPrivateChatRoom()
            }
        }
    }

    fun resetPaginationForPrivateChatRoom() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getPrivateChatRoom()
    }

    private fun getPublicChatRoom() {
        chatRepository.getPublicChatRoom(pageNo, perPage)
            .doOnSubscribe {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                isLoading = false
                response?.let {
                    if (response.status) {
                        if (pageNo == 1) {
                            if (response.result?.data != null) {
                                listOfChatRoom = response.result.data.toMutableList()
                                chatRoomStateSubject.onNext(
                                    ChatRoomViewState.ListOfChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            }
                        } else {
                            if (response.result?.data != null) {
                                listOfChatRoom.addAll(response.result.data)
                                chatRoomStateSubject.onNext(
                                    ChatRoomViewState.ListOfChatRoom(
                                        listOfChatRoom
                                    )
                                )
                            } else {
                                isLoadMore = false
                            }
                        }
                    } else {
                        response.message?.let {
                            chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(response.message))
                        }
                    }
                }
            }, { throwable ->
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun loadMorePublicChatList() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getPublicChatRoom()
            }
        }
    }

    fun resetPaginationForPublicChatRoom() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getPublicChatRoom()
    }

    fun getChatRoomUser() {
        profileRepository.getChatRoomUserProfile(perPage)
            .doOnSubscribe {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(false))
                if (response.status) {
                    response.let {
                        chatRoomStateSubject.onNext(ChatRoomViewState.GetProfileData(it))
                    }
                } else {
                    response.message?.let {
                        chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun sendJoinChatRoomRequest(sendJoinChatRoomRequest: SendJoinChatRoomRequestRequest) {
        chatRepository.sendJoinRoomRequest(sendJoinChatRoomRequest)
            .doOnSubscribe {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(true))
            }.doAfterTerminate {
                chatRoomStateSubject.onNext(ChatRoomViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
            }, { throwable ->
                throwable.localizedMessage?.let {
                    chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
                }
            })
    }

    fun getNotificationCount() {
        notificationRepository.getNotificationCount()
            .doOnSubscribe {
            }
            .doAfterSuccess {
            }
            .subscribeOnIoAndObserveOnMainThread({
                if (it.result != null) {
                    chatRoomStateSubject.onNext(ChatRoomViewState.GetNotificationCount(it.result))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
                }
            })
    }

    fun acceptRejectRequest(acceptRejectRequestRequest: AcceptRejectRequestRequest) {
        notificationRepository.acceptRejectRequest(acceptRejectRequestRequest)
            .doOnSubscribe {
                chatRoomStateSubject.onNext(ChatRoomViewState.RequestLoadingState(true))
            }
            .doAfterSuccess {
                chatRoomStateSubject.onNext(ChatRoomViewState.RequestLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    chatRoomStateSubject.onNext(ChatRoomViewState.AcceptRejectSuccess(response.message.toString()))
                } else {
                    chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(response.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun observeJoinRoomRequest() {
        chatRepository.observeJoinRoomRequest().subscribeOnIoAndObserveOnMainThread({
            chatRoomStateSubject.onNext(ChatRoomViewState.ReceivedRequestData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeNewMessage() {
        chatRepository.observeNewMessage().subscribeOnIoAndObserveOnMainThread({
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeKickedOutUser() {
        chatRepository.observeKickedOutUser().subscribeOnIoAndObserveOnMainThread({
            chatRoomStateSubject.onNext(ChatRoomViewState.KickedOutData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeBannedUser() {
        chatRepository.observeBannedUser().subscribeOnIoAndObserveOnMainThread({
            chatRoomStateSubject.onNext(ChatRoomViewState.BannedUserData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun checkUser(userId: String) {
        profileRepository.checkUser(userId).subscribeOnIoAndObserveOnMainThread({
            chatRoomStateSubject.onNext(ChatRoomViewState.CheckUserData(it))
        }, { throwable ->
            throwable.localizedMessage?.let {
                chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun logOutAll(deviceId: String) {
        profileRepository.logOutAll(deviceId).subscribeOnIoAndObserveOnMainThread({
            chatRoomStateSubject.onNext(ChatRoomViewState.LogOutAll)
        }, { throwable ->
            throwable.localizedMessage?.let {
                chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun logOut(deviceId: String) {
        profileRepository.logOut(deviceId).subscribeOnIoAndObserveOnMainThread({
            Timber.tag("logOut").i("accessToken :${it.accessToken}")
            if (it.accessToken.isNullOrEmpty()) {
                chatRoomStateSubject.onNext(ChatRoomViewState.LogOut)
            } else {
                chatRoomStateSubject.onNext(ChatRoomViewState.LogInOtherAccount)
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                chatRoomStateSubject.onNext(ChatRoomViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
}

sealed class ChatRoomViewState {
    data class ErrorMessage(val errorMessage: String) : ChatRoomViewState()
    data class SuccessMessage(val successMessage: String) : ChatRoomViewState()
    data class DeleteUserSuccessMessage(val successMessage: String) : ChatRoomViewState()
    data class LoadingState(val isLoading: Boolean) : ChatRoomViewState()
    data class ListOfChatRoom(val listOfChatRoom: List<ChatRoomInfo>) : ChatRoomViewState()

    data class GetProfileData(val profileData: MeetFriendResponseForGetProfile<ChatRoomUserProfileInfo>) :
        ChatRoomViewState()

    data class ReceivedRequestData(val requestData: SendJoinChatRoomRequestRequest) :
        ChatRoomViewState()

    data class GetNotificationCount(val notificationCount: Int) : ChatRoomViewState()
    data class KickedOutData(val kickedOutData: MessageInfo) : ChatRoomViewState()
    data class BannedUserData(val bannedUserData: MessageInfo) : ChatRoomViewState()
    data class RequestLoadingState(val isLoading: Boolean) : ChatRoomViewState()
    data class AcceptRejectSuccess(val successMessage: String) : ChatRoomViewState()
    data class CheckUserData(val data: MeetFriendCommonResponse) : ChatRoomViewState()
    object LogOutAll : ChatRoomViewState()
    object LogOut : ChatRoomViewState()
    object LogInOtherAccount : ChatRoomViewState()
}
