package com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel

import android.content.Context
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.GetChatMessageRequest
import com.meetfriend.app.api.chat.model.GetChatRoomAdminRequest
import com.meetfriend.app.api.chat.model.JoinChatRoomResponse
import com.meetfriend.app.api.chat.model.JoinRoomRequest
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.SeenMsgRequest
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.api.chat.model.TypingRequest
import com.meetfriend.app.api.cloudflare.CloudFlareRepository
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.ImageToCloudFlare
import com.meetfriend.app.api.gift.GiftsRepository
import com.meetfriend.app.api.gift.model.AcceptRejectGiftRequest
import com.meetfriend.app.api.gift.model.MyEarningInfo
import com.meetfriend.app.api.message.MessageRepository
import com.meetfriend.app.api.message.model.EditMessageRequest
import com.meetfriend.app.api.notification.NotificationRepository
import com.meetfriend.app.api.notification.model.VoipCallRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.cloudFlareImageUploadBaseUrl
import com.meetfriend.app.newbase.extension.cloudFlareVideoUploadBaseUrl
import com.meetfriend.app.newbase.extension.getCommonPhotoFileName
import com.meetfriend.app.newbase.extension.getCommonVideoFileName
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.socket.SocketService
import com.meetfriend.app.ui.storywork.models.ResultListResult
import com.meetfriend.app.utils.Constant
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class ViewOneToOneChatViewModel(
    private val chatRepository: ChatRepository,
    private val notificationRepository: NotificationRepository,
    private val messageRepository: MessageRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loginUserCache: LoggedInUserCache,
    private val giftRepository: GiftsRepository
) : BasicViewModel() {

    private val viewOneToOneChatStateSubject: PublishSubject<ViewOneToOneChatViewState> =
        PublishSubject.create()
    val viewOneToOneChatState: Observable<ViewOneToOneChatViewState> =
        viewOneToOneChatStateSubject.hide()

    init {
        observeRoomJoined()
        observeNewMessage()
        observeGiftRequest()
        observeTyping()
        observeStopTyping()
        observeMsgSeen()
    }

    var pageNo = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    private fun getChatRoomMessage(chatMessageRequest: GetChatMessageRequest) {
        chatRepository.getChatRoomMessage(chatMessageRequest, pageNo).doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterTerminate {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it != null) {
                val messageInfo = it.result?.data

                if (!messageInfo.isNullOrEmpty()) {
                    viewOneToOneChatStateSubject.onNext(
                        ViewOneToOneChatViewState.LoadChatMessageList(
                            messageInfo
                        )
                    )
                } else {
                    viewOneToOneChatStateSubject.onNext(
                        ViewOneToOneChatViewState.LoadChatMessageEmptyList(
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
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.RoomConnectionFail("Fail to connect room"))
        }.subscribeOnIoAndObserveOnMainThread({}, {
            viewOneToOneChatStateSubject.onNext(
                ViewOneToOneChatViewState.RoomConnectionFail(
                    it.localizedMessage ?: "Fail to connect room"
                )
            )
        }).autoDispose()
    }

    private fun observeRoomJoined() {
        chatRepository.observeRoomJoined().subscribeOnIoAndObserveOnMainThread({
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.JoinRoomResponse(it))
        }, {
            Timber.e(it)
            viewOneToOneChatStateSubject.onNext(
                ViewOneToOneChatViewState.RoomConnectionFail(
                    it.localizedMessage ?: "Fail to join room"
                )
            )
        }).autoDispose()
    }

    fun sendPrivateMessage(sendPrivateMessageRequest: SendPrivateMessageRequest) {
        chatRepository.sendPrivateMessage(sendPrivateMessageRequest).doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterTerminate {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
        }, {
        })
    }

    private fun observeNewMessage() {
        chatRepository.observeNewMessage().subscribeOnIoAndObserveOnMainThread({
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.GetNewSendMessage(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    override fun onCleared() {
        chatRepository.off(SocketService.EVENT_ROOM_JOIN)
        chatRepository.off(SocketService.EVENT_ACCEPT_REJECT_GIFT_REQUEST)
        chatRepository.off(SocketService.EVENT_TYPING)
        chatRepository.off(SocketService.EVENT_STOP_TYPING)
        chatRepository.off(SocketService.EVENT_MSG_SEEN)
        super.onCleared()
    }

    fun startVoipCall(voipCallRequest: VoipCallRequest) {
        notificationRepository.startVoipCall(voipCallRequest).subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.VoipCallData(it))
            } else {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(it.message.toString()))
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun updateMessage(editMessageRequest: EditMessageRequest) {
        messageRepository.deleteMessage(editMessageRequest).doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterTerminate {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                when (editMessageRequest.msgType) {
                    Constant.MESSAGE_TYPE_DELETE -> {
                        viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.MessageDeleted)
                    }
                    Constant.MESSAGE_TYPE_EDITED -> {
                        viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.MessageEdited)
                    }
                    Constant.MESSAGE_TYPE_SAVE -> {
                        viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.MessageSaved)
                    }
                    Constant.MESSAGE_TYPE_FORWARD -> {
                        viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.MessageForwarded)
                    }
                }
            } else {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(it.message.toString()))
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
            }
            .doAfterTerminate {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        viewOneToOneChatStateSubject.onNext(
                            ViewOneToOneChatViewState.GetCloudFlareConfig(
                                cloudFlareConfig
                            )
                        )
                    } else {
                        response.message?.let {
                            viewOneToOneChatStateSubject.onNext(
                                ViewOneToOneChatViewState.CloudFlareConfigErrorMessage(
                                    it
                                )
                            )
                        }
                    }
                }
            }, { throwable ->
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    viewOneToOneChatStateSubject.onNext(
                        ViewOneToOneChatViewState.CloudFlareConfigErrorMessage(
                            it
                        )
                    )
                }
            }).autoDispose()
    }

    fun uploadImageToCloudFlare(
        context: Context,
        cloudFlareConfig: CloudFlareConfig,
        imageFile: File
    ) {
        val imageTempPathDir = context.getExternalFilesDir("MeetFriendImages")?.path
        val fileName =
            getCommonPhotoFileName(loginUserCache.getLoggedInUserId())
        val imageCopyFile = File(imageTempPathDir + File.separator + fileName + ".jpg")
        val finalImageFile = imageFile.copyTo(imageCopyFile)

        val apiUrl = cloudFlareImageUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        val filePart = MultipartBody.Part.createFormData(
            "file",
            finalImageFile.name,
            finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(ImageToCloudFlare(apiUrl, authToken, filePart))
            .doOnSubscribe {
                viewOneToOneChatStateSubject.onNext(
                    ViewOneToOneChatViewState.CloudFlareLoadingState(
                        true
                    )
                )
            }
            .doAfterTerminate {
                viewOneToOneChatStateSubject.onNext(
                    ViewOneToOneChatViewState.CloudFlareLoadingState(
                        false
                    )
                )
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            viewOneToOneChatStateSubject.onNext(
                                ViewOneToOneChatViewState.UploadImageCloudFlareSuccess(
                                    variants.first()
                                )
                            )
                        } else {
                            handleCloudFlareMediaUploadError(response.errors)
                        }
                    } else {
                        handleCloudFlareMediaUploadError(response.errors)
                    }
                } else {
                    handleCloudFlareMediaUploadError(response.errors)
                }
            }, { throwable ->
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun uploadVideoToCloudFlare(
        context: Context,
        cloudFlareConfig: CloudFlareConfig,
        videoFile: File
    ) {
        val videoTempPathDir = context.getExternalFilesDir("MeetFriendVideos")?.path
        val fileName =
            getCommonVideoFileName(loginUserCache.getLoggedInUserId())
        val videoCopyFile = File(videoTempPathDir + File.separator + fileName + ".mp4")
        val finalImageFile = videoFile.copyTo(videoCopyFile)

        val apiUrl = cloudFlareVideoUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        cloudFlareRepository.uploadVideoUsingTus(apiUrl, authToken, finalImageFile)
            .doOnSubscribe {
                viewOneToOneChatStateSubject.onNext(
                    ViewOneToOneChatViewState.CloudFlareLoadingState(
                        true
                    )
                )
            }
            .subscribeOn(Schedulers.io())
            .flatMap {
                cloudFlareRepository.getUploadVideoDetails(it, authToken)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate {
                viewOneToOneChatStateSubject.onNext(
                    ViewOneToOneChatViewState.CloudFlareLoadingState(
                        false
                    )
                )
            }
            .subscribe({
                viewOneToOneChatStateSubject.onNext(
                    ViewOneToOneChatViewState.UploadVideoCloudFlareSuccess(
                        it.uid.toString(),
                        it.thumbnail.toString()
                    )
                )
            }, { throwable ->
                Timber.e(throwable)
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun leaveOneToOneRoom(conversationId: Int) {
        chatRepository.leaveOneToOneRoom(GetChatRoomAdminRequest(conversationId)).doOnSubscribe {}
            .doOnError {
                viewOneToOneChatStateSubject.onNext(
                    ViewOneToOneChatViewState.RoomConnectionFail("Fail to connect room")
                )
            }.subscribeOnIoAndObserveOnMainThread({}, {
                viewOneToOneChatStateSubject.onNext(
                    ViewOneToOneChatViewState.RoomConnectionFail(
                        it.localizedMessage ?: "Fail to connect room"
                    )
                )
            }).autoDispose()
    }

    fun storyByUSer(userId: Int) {
        chatRepository.storyByUser(userId).doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterSuccess {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            response.result.let {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.StoryResponseData(response.result))
            }
        }, { throwable ->
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun acceptRejectGiftRequest(acceptRejectGiftRequest: AcceptRejectGiftRequest) {
        chatRepository.acceptRejectGiftRequest(acceptRejectGiftRequest).doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterTerminate {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
        }, {
        })
    }

    private fun observeGiftRequest() {
        chatRepository.observeGiftRequest().subscribeOnIoAndObserveOnMainThread({
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.GiftRequestActionInfo(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun getMyEarningInfo() {
        giftRepository.getMyEarning().doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterTerminate {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.MyEarningData(it.result))
            } else {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun sendGift(toId: String, coins: Double, giftId: Int) {
        giftRepository.sendGiftPost(toId, "", coins, giftId).doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterTerminate {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.SendGiftSuccess)
            } else {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun startTyping(request: TypingRequest) {
        chatRepository.startTyping(request).doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterTerminate {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
        }, {
        })
    }

    fun stopTyping(request: TypingRequest) {
        chatRepository.stopTyping(request).doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterTerminate {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
        }, {
        })
    }

    private fun observeTyping() {
        chatRepository.observeTyping().subscribeOnIoAndObserveOnMainThread({
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.TypingInfo(true))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeStopTyping() {
        chatRepository.observeStopTyping().subscribeOnIoAndObserveOnMainThread({
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.TypingInfo(false))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun seenMsg(request: SeenMsgRequest) {
        chatRepository.seenMsg(request).doOnSubscribe {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(true))
        }.doAfterTerminate {
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
        }, {
        })
    }

    private fun observeMsgSeen() {
        chatRepository.observeMsgSeen().subscribeOnIoAndObserveOnMainThread({
            viewOneToOneChatStateSubject.onNext(ViewOneToOneChatViewState.MessageSeen)
        }, {
            Timber.e(it)
        }).autoDispose()
    }
}

sealed class ViewOneToOneChatViewState {
    data class ErrorMessage(val errorMessage: String) : ViewOneToOneChatViewState()
    data class SuccessMessage(val successMessage: String) : ViewOneToOneChatViewState()
    data class LoadingState(val isLoading: Boolean) : ViewOneToOneChatViewState()
    data class LoadChatMessageList(val listOfMessage: List<MessageInfo>) :
        ViewOneToOneChatViewState()

    data class LoadChatMessageEmptyList(val listOfMessage: List<MessageInfo>?) :
        ViewOneToOneChatViewState()

    data class RoomConnectionFail(val errorMessage: String) : ViewOneToOneChatViewState()
    data class GetNewSendMessage(val chatMessage: MessageInfo) : ViewOneToOneChatViewState()
    data class JoinRoomResponse(val joinChatRoomResponse: JoinChatRoomResponse) :
        ViewOneToOneChatViewState()

    data class VoipCallData(val voipCallData: MeetFriendCommonResponse) :
        ViewOneToOneChatViewState()

    object MessageDeleted : ViewOneToOneChatViewState()
    object MessageEdited : ViewOneToOneChatViewState()
    object MessageSaved : ViewOneToOneChatViewState()
    object MessageForwarded : ViewOneToOneChatViewState()

    data class CloudFlareConfigErrorMessage(val errorMessage: String) : ViewOneToOneChatViewState()
    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) :
        ViewOneToOneChatViewState()

    data class UploadImageCloudFlareSuccess(val imageUrl: String) : ViewOneToOneChatViewState()
    data class CloudFlareLoadingState(val isLoading: Boolean) : ViewOneToOneChatViewState()
    data class UploadVideoCloudFlareSuccess(val videoId: String, val thumbnail: String) :
        ViewOneToOneChatViewState()

    data class GiftRequestActionInfo(val messageInfo: MessageInfo) :
        ViewOneToOneChatViewState()

    object SendGiftSuccess : ViewOneToOneChatViewState()
    data class MyEarningData(val myEarningInfo: MyEarningInfo?) : ViewOneToOneChatViewState()

    data class TypingInfo(val isStart: Boolean) : ViewOneToOneChatViewState()
    object MessageSeen : ViewOneToOneChatViewState()

    data class StoryResponseData(val storyList: List<ResultListResult>) : ViewOneToOneChatViewState()
}
