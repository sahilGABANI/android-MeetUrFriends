package com.meetfriend.app.ui.chatRoom.roomview.viewmodel

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomUser
import com.meetfriend.app.api.chat.model.GetChatMessageRequest
import com.meetfriend.app.api.chat.model.GetChatRoomAdminRequest
import com.meetfriend.app.api.chat.model.GetMentionUserRequest
import com.meetfriend.app.api.chat.model.JoinAgoraSDKVoiceCallChannel
import com.meetfriend.app.api.chat.model.JoinChatRoomResponse
import com.meetfriend.app.api.chat.model.JoinRoomRequest
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.RestrictRequest
import com.meetfriend.app.api.chat.model.SendMicAccessRequestSocketRequest
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.api.chat.model.VoiceCallEndSocketRequest
import com.meetfriend.app.api.chat.model.VoiceCallStartSocketRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.socket.SocketService
import com.meetfriend.app.utils.Constant
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ChatRoomMessageViewModel(
    private val chatRepository: ChatRepository,
    private val loggedInUserCache: LoggedInUserCache,
) : BasicViewModel() {

    private val chatRoomMessageStateSubject: PublishSubject<ChatRoomMessageViewState> =
        PublishSubject.create()
    val chatRoomMessageState: Observable<ChatRoomMessageViewState> =
        chatRoomMessageStateSubject.hide()

    private lateinit var chatRoomInfo: ChatRoomInfo

    init {
        observeRoomJoined()
        observeNewMessage()
        observeKickedOutUser()
        observeBannedUser()
        observeVoiceCallStarted()
        observeVoiceCallEnded()
        observeRestrictUser()
    }

    var pageNo = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    private fun getChatRoomMessage(chatMessageRequest: GetChatMessageRequest) {
        chatRepository.getChatRoomMessage(chatMessageRequest, pageNo).doOnSubscribe {
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(true))
        }.doAfterTerminate {
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it != null) {
                val messageInfo = it.result?.data

                if (!messageInfo.isNullOrEmpty()) {
                    chatRoomMessageStateSubject.onNext(
                        ChatRoomMessageViewState.LoadChatMessageList(
                            messageInfo
                        )
                    )
                } else {
                    chatRoomMessageStateSubject.onNext(
                        ChatRoomMessageViewState.LoadChatMessageEmptyList(
                            messageInfo
                        )
                    )

                    isLoadMore = false
                }
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun loadMore(chatMessageRequest: GetChatMessageRequest) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo++
                getChatRoomMessage(chatMessageRequest)
            }
        }
    }

    fun resetPagination(chatMessageRequest: GetChatMessageRequest) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getChatRoomMessage(chatMessageRequest)
    }

    fun joinRoom(joinRoomRequest: JoinRoomRequest) {
        chatRepository.joinRoom(joinRoomRequest).doOnSubscribe {}.doOnError {
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.RoomConnectionFail("Fail to connect room"))
        }.subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.JoinRoomSuccess)
        }, {
            chatRoomMessageStateSubject.onNext(
                ChatRoomMessageViewState.RoomConnectionFail(
                    it.localizedMessage ?: "Fail to connect room"
                )
            )
        }).autoDispose()
    }

    private fun observeRoomJoined() {
        chatRepository.observeRoomJoined().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.JoinRoomResponse(it))
        }, {
            Timber.e(it)
            chatRoomMessageStateSubject.onNext(
                ChatRoomMessageViewState.RoomConnectionFail(
                    it.localizedMessage ?: "Fail to join room"
                )
            )
        }).autoDispose()
    }

    fun sendPrivateMessage(sendPrivateMessageRequest: SendPrivateMessageRequest) {
        chatRepository.sendPrivateMessage(sendPrivateMessageRequest).doOnSubscribe {
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(true))
        }.doAfterTerminate {
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
        }, {
        })
    }

    private fun observeNewMessage() {
        chatRepository.observeNewMessage().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.GetNewSendMessage(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeKickedOutUser() {
        chatRepository.observeKickedOutUser().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.KickedOutData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeBannedUser() {
        chatRepository.observeBannedUser().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.BannedUserData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    override fun onCleared() {
        chatRepository.off(SocketService.EVENT_ROOM_JOIN)
        chatRepository.off(SocketService.EVENT_VOICE_CALL_END_LISTEN)
        chatRepository.off(SocketService.EVENT_VOICE_CALL_START_LISTEN)
        chatRepository.off(SocketService.EVENT_RECEIVE_MICE_REQUESTED)
        chatRepository.off(SocketService.EVENT_REQUEST_ACCEPTED)
        chatRepository.off(SocketService.EVENT_REVOKE_MIC_ACCESS)

        super.onCleared()
    }

    fun loadAgoraToken(tokenRole: Int) {
        val channelName = Constant.AGORA_CHANNEL_NAME_PREFIX.plus(chatRoomInfo.id)
        chatRepository.getAgoraSDKToken(channelName, tokenRole)
            .subscribeOnIoAndObserveOnMainThread({
                chatRoomMessageStateSubject.onNext(
                    ChatRoomMessageViewState.AgoraSDKVoiceCallChannel(
                        JoinAgoraSDKVoiceCallChannel(
                            channelName = channelName,
                            loggedInUserCache.getLoggedInUserId(),
                            tokenRole,
                            it.rtcToken,
                            chatRoomInfo.id
                        )
                    )
                )
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun setCurrentChatInfo(chatRoomInfo: ChatRoomInfo) {
        this.chatRoomInfo = chatRoomInfo
    }

    private fun observeVoiceCallStarted() {
        chatRepository.observeVoiceCallStarted().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.JoinVoiceCallResponse(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeVoiceCallEnded() {
        chatRepository.observeVoiceCallEnded().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.EndVoiceCall)
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun updateVoiceCallStarted(voiceCallStartSocketRequest: VoiceCallStartSocketRequest) {
        chatRepository.startVoiceCall(voiceCallStartSocketRequest)
            .subscribeOnIoAndObserveOnMainThread({
            }, {
            }).autoDispose()
    }

    fun updateVoiceCallEnded() {
        chatRepository.endVoiceCall(VoiceCallEndSocketRequest(chatRoomInfo.id))
            .subscribeOnIoAndObserveOnMainThread({
            }, {
            }).autoDispose()
    }

    fun getChatRoomAdmin(getChatRoomAdminRequest: GetChatRoomAdminRequest) {
        chatRepository.getChatRoomAdmin(getChatRoomAdminRequest).doOnSubscribe {
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(true))
        }.doAfterTerminate {
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.ChatRoomAdminData(it.data))
            } else {
                chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.ErrorMessage(it.message.toString()))
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun getInitialUserForMention(conversationId: Int) {
        chatRepository.getUserForMention(GetMentionUserRequest(conversationId, "")).doOnSubscribe {
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(true))
        }.doAfterTerminate {
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            response?.let {
                response.result?.let {
                    chatRoomMessageStateSubject.onNext(
                        ChatRoomMessageViewState.InitialUserListForMention(
                            it.toMutableList()
                        )
                    )
                }
            }
        }, { throwable ->
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun getUserForMention(conversationId: Int, searchText: String) {
        chatRepository.getUserForMention(GetMentionUserRequest(conversationId, searchText))
            .doOnSubscribe {
                chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(true))
            }.doAfterTerminate {
                chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.UserListForMention(it.result))
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

    fun searchUserClicked(
        initialDescriptionString: String,
        subString: String,
        user: MeetFriendUser,
    ) {
        val remainString = initialDescriptionString.removePrefix(subString)
        if (subString.length == initialDescriptionString.length) {
            val lastIndexOfToken =
                initialDescriptionString.findLastAnyOf(listOf("@"))?.first ?: return
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString@${user.chatUserName}"
            chatRoomMessageStateSubject.onNext(
                ChatRoomMessageViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${user.chatUserName} $remainString"
            chatRoomMessageStateSubject.onNext(
                ChatRoomMessageViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        }
    }

    private fun observeRestrictUser() {
        chatRepository.observeRestrictUser().subscribeOnIoAndObserveOnMainThread({
            chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.RestrictUserData(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun sendMicAccessRequest(micAccessRequestSocketRequest: SendMicAccessRequestSocketRequest) {
        chatRepository.sendMicAccessRequest(micAccessRequestSocketRequest)
            .subscribeOnIoAndObserveOnMainThread({
                chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.SendRequest)
            }, {
                it.message?.let {
                    chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun observeMicAccessRequest() {
        chatRepository.observeMicAccessRequest().subscribeOnIoAndObserveOnMainThread(
            {
                chatRoomMessageStateSubject.onNext(
                    ChatRoomMessageViewState.ReceivedMicAccessRequest(
                        it
                    )
                )
            },
            {
                it.message?.let {
                    chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.ErrorMessage(it))
                }
            }
        ).autoDispose()
    }

    fun acceptMicAccessRequest(sendMicAccessRequestSocketRequest: SendMicAccessRequestSocketRequest) {
        chatRepository.acceptMicAccessRequest(sendMicAccessRequestSocketRequest)
            .subscribeOnIoAndObserveOnMainThread({
                chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.AcceptedRequest)
            }, {
                it.message?.let {
                    chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun observeAcceptMicAccessRequest() {
        chatRepository.observeAcceptMicAccessRequest().subscribeOnIoAndObserveOnMainThread(
            {
                chatRoomMessageStateSubject.onNext(
                    ChatRoomMessageViewState.AcceptedMicAccessRequest(
                        it
                    )
                )
            },
            {
                it.message?.let {
                    chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.ErrorMessage(it))
                }
            }
        ).autoDispose()
    }

    fun revokeMicAccess(revokeMicAccessRequest: SendMicAccessRequestSocketRequest) {
        chatRepository.revokeMicAccess(revokeMicAccessRequest).subscribeOnIoAndObserveOnMainThread({
        }, {
            it.message?.let {
                chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun observeRevokeMicAccess() {
        chatRepository.observeRevokeMicAccess().subscribeOnIoAndObserveOnMainThread(
            {
                chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.RevokeMicAccess(it))
            },
            {
                it.message?.let {
                    chatRoomMessageStateSubject.onNext(ChatRoomMessageViewState.ErrorMessage(it))
                }
            }
        ).autoDispose()
    }
}

sealed class ChatRoomMessageViewState {
    data class ErrorMessage(val errorMessage: String) : ChatRoomMessageViewState()
    data class SuccessMessage(val successMessage: String) : ChatRoomMessageViewState()
    data class LoadingState(val isLoading: Boolean) : ChatRoomMessageViewState()
    data class LoadChatMessageList(val listOfMessage: List<MessageInfo>) :
        ChatRoomMessageViewState()

    data class LoadChatMessageEmptyList(val listOfMessage: List<MessageInfo>?) :
        ChatRoomMessageViewState()

    data class RoomConnectionFail(val errorMessage: String) : ChatRoomMessageViewState()
    data class GetNewSendMessage(val chatMessage: MessageInfo) : ChatRoomMessageViewState()
    data class KickedOutData(val chatMessage: MessageInfo) : ChatRoomMessageViewState()
    data class BannedUserData(val chatMessage: MessageInfo) : ChatRoomMessageViewState()
    data class RestrictUserData(val chatMessage: RestrictRequest) : ChatRoomMessageViewState()
    object EndVoiceCall : ChatRoomMessageViewState()
    data class AgoraSDKVoiceCallChannel(val joinAgoraSDKVoiceCallChannel: JoinAgoraSDKVoiceCallChannel) :
        ChatRoomMessageViewState()

    data class JoinRoomResponse(val joinChatRoomResponse: JoinChatRoomResponse) :
        ChatRoomMessageViewState()

    data class ChatRoomAdminData(val listOfAdmin: List<ChatRoomUser>?) : ChatRoomMessageViewState()
    data class UserListForMention(val listOfUserForMention: List<MeetFriendUser>?) :
        ChatRoomMessageViewState()

    data class UpdateDescriptionText(val descriptionString: String) : ChatRoomMessageViewState()
    data class InitialUserListForMention(val listOfUserForMention: List<MeetFriendUser>) :
        ChatRoomMessageViewState()

    data class RestrictUser(val successMessage: String?) : ChatRoomMessageViewState()
    data class JoinVoiceCallResponse(val joinVoiceCallData: VoiceCallStartSocketRequest) :
        ChatRoomMessageViewState()

    object SendRequest : ChatRoomMessageViewState()
    data class ReceivedMicAccessRequest(val micAccessRequestData: SendMicAccessRequestSocketRequest) :
        ChatRoomMessageViewState()

    object AcceptedRequest : ChatRoomMessageViewState()
    data class AcceptedMicAccessRequest(val micAccessRequestData: SendMicAccessRequestSocketRequest) :
        ChatRoomMessageViewState()

    data class RevokeMicAccess(val revokeMicAccessData: SendMicAccessRequestSocketRequest) :
        ChatRoomMessageViewState()

    object JoinRoomSuccess : ChatRoomMessageViewState()
}
