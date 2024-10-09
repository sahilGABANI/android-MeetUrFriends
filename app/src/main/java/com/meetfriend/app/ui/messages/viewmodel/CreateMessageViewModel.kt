package com.meetfriend.app.ui.messages.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.CreateOneToOneChatRequest
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.GetUserForLiveInviteRequest
import com.meetfriend.app.api.messages.MessagesRepository
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.MentionUserRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class CreateMessageViewModel(
    private val followRepository: FollowRepository,
    private val messagesRepository: MessagesRepository,
    private val postRepository: PostRepository
) : BasicViewModel() {

    private val createChatStateSubjects: PublishSubject<CreateMessageState> =
        PublishSubject.create()
    val createChatState: Observable<CreateMessageState> = createChatStateSubjects.hide()

    private var perPage = 12
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfFollowers: MutableList<MeetFriendUser> = mutableListOf()

    fun getUserForLiveInvite() {
        followRepository.getUserForLiveInvite(
            GetUserForLiveInviteRequest(
                page = pageNo,
                perPage = perPage
            )
        ).doOnSubscribe {
            createChatStateSubjects.onNext(CreateMessageState.LoadingState(true))
        }.doAfterTerminate {
            createChatStateSubjects.onNext(CreateMessageState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (pageNo == 1) {
                    if (it.result?.data != null) {
                        listOfFollowers = it.result.data.toMutableList()
                        createChatStateSubjects.onNext(
                            CreateMessageState.FollowersData(
                                listOfFollowers
                            )
                        )
                    } else {
                        createChatStateSubjects.onNext(CreateMessageState.EmptyState)
                    }
                } else {
                    if (it.result?.data != null && it.result.data.isNotEmpty()) {
                        listOfFollowers.addAll(it.result.data)
                        createChatStateSubjects.onNext(
                            CreateMessageState.FollowersData(
                                listOfFollowers
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }
            } else {
                createChatStateSubjects.onNext(CreateMessageState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                createChatStateSubjects.onNext(CreateMessageState.ErrorMessage(it))
            }

        }).autoDispose()
    }

    fun loadMoreInviteUser() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getUserForLiveInvite()
            }
        }
    }

    fun resetPaginationForInviteUser() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getUserForLiveInvite()
    }

    fun createOneToOneChat(oneToOneChatRequest: CreateOneToOneChatRequest) {
        messagesRepository.createChatRoom(oneToOneChatRequest).doOnSubscribe {
            createChatStateSubjects.onNext(CreateMessageState.LoadingState(true))
        }.doAfterTerminate {
            createChatStateSubjects.onNext(CreateMessageState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.data != null) {
                createChatStateSubjects.onNext(CreateMessageState.OneToOneChatData(it.data))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                createChatStateSubjects.onNext(CreateMessageState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun getUserForNormalChat(searchText: String) {
        postRepository.mentionUser(MentionUserRequest(searchText))
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.let {
                    createChatStateSubjects.onNext(CreateMessageState.UserListForNormalChat(it.result))
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

}

sealed class CreateMessageState {
    data class ErrorMessage(val errorMessage: String) : CreateMessageState()
    data class SuccessMessage(val successMessage: String) : CreateMessageState()
    data class LoadingState(val isLoading: Boolean) : CreateMessageState()
    data class FollowersData(val followingData: List<MeetFriendUser>?) : CreateMessageState()
    data class UserListForNormalChat(val listOfUserForMention: List<MeetFriendUser>?) :
        CreateMessageState()

    data class OneToOneChatData(val chatRoomInfo: ChatRoomInfo) : CreateMessageState()
    object EmptyState : CreateMessageState()

}