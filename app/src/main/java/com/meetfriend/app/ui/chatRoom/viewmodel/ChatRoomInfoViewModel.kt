package com.meetfriend.app.ui.chatRoom.viewmodel

import android.content.Context
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.*
import com.meetfriend.app.api.cloudflare.CloudFlareRepository
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.ImageToCloudFlare
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.cloudFlareImageUploadBaseUrl
import com.meetfriend.app.newbase.extension.getCommonPhotoFileName
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChatRoomInfoViewModel(
    private val chatRepository: ChatRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loggedInUserCache: LoggedInUserCache,
) : BasicViewModel() {

    private val chatRoomInfoStateSubject: PublishSubject<ChatRoomInfoViewState> =
        PublishSubject.create()
    val chatRoomState: Observable<ChatRoomInfoViewState> = chatRoomInfoStateSubject.hide()

    fun getChatRoomInfo(conversationId: Int) {
        chatRepository.getChatRoomInfo(conversationId)
            .doOnSubscribe {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.LoadingState(true))
            }
            .doAfterTerminate {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                if (it != null) {
                    chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ChatRoomDetails(it.data))
                    chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ChatRoomUserInfo(it.users))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun updateRoom(conversationId: Int, updateChatRoomRequest: UpdateChatRoomRequest) {
        chatRepository.updateRoom(conversationId, updateChatRoomRequest)
            .doOnSubscribe {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.LoadingState(true))

            }.doAfterTerminate {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.LoadingState(false))

            }.subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    it.data?.let { chatRoomInfo ->
                        chatRoomInfoStateSubject.onNext(
                            ChatRoomInfoViewState.UpdatedChatRoomInfo(
                                chatRoomInfo
                            )
                        )
                    }
                } else {
                    chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ErrorMessage(it))
                }

            }).autoDispose()
    }

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.CloudFlareLoadingState(true))
            }
            .doAfterTerminate {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.CloudFlareLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        chatRoomInfoStateSubject.onNext(
                            ChatRoomInfoViewState.GetCloudFlareConfig(
                                cloudFlareConfig
                            )
                        )
                    } else {
                        response.message?.let {
                            chatRoomInfoStateSubject.onNext(
                                ChatRoomInfoViewState.CloudFlareConfigErrorMessage(
                                    it
                                )
                            )
                        }
                    }
                }
            }, { throwable ->
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    chatRoomInfoStateSubject.onNext(
                        ChatRoomInfoViewState.CloudFlareConfigErrorMessage(
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
            getCommonPhotoFileName(loggedInUserCache.getLoggedInUserId())
        val imageCopyFile = File(imageTempPathDir + File.separator + fileName + ".jpg")
        val finalImageFile = imageFile.copyTo(imageCopyFile)

        val apiUrl = cloudFlareImageUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(ImageToCloudFlare(apiUrl, authToken, filePart))
            .doOnSubscribe {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.CloudFlareLoadingState(true))
            }
            .doAfterTerminate {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.CloudFlareLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            chatRoomInfoStateSubject.onNext(
                                ChatRoomInfoViewState.UploadImageCloudFlareSuccess(
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
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun deleteChatRoom(conversationId: Int) {
        chatRepository.deleteChatRoom(conversationId).doOnSubscribe {
            chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.DeleteLoadingState(true))
        }.doAfterTerminate {
            chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.DeleteLoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.DeleteChatRoom)
            } else {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun getMiceAccessInfo(getMiceAccessRequest: GetMiceAccessRequest) {
        chatRepository.getMiceAccessInfo(getMiceAccessRequest).doOnSubscribe {
            chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.DeleteLoadingState(true))
        }.doAfterTerminate {
            chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.DeleteLoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.MiceAccess(it.result))
            } else {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                chatRoomInfoStateSubject.onNext(ChatRoomInfoViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
}

sealed class ChatRoomInfoViewState {
    data class ErrorMessage(val errorMessage: String) : ChatRoomInfoViewState()
    data class SuccessMessage(val successMessage: String) : ChatRoomInfoViewState()
    data class LoadingState(val isLoading: Boolean) : ChatRoomInfoViewState()

    data class ChatRoomDetails(val chatRoomInfo: ChatRoomInfo?) : ChatRoomInfoViewState()
    data class ChatRoomUserInfo(val chatRoomUserInfo: List<ChatRoomUser>?) : ChatRoomInfoViewState()
    data class UpdatedChatRoomInfo(val chatRoomInfo: ChatRoomInfo) : ChatRoomInfoViewState()

    data class CloudFlareLoadingState(val isLoading: Boolean) : ChatRoomInfoViewState()
    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : ChatRoomInfoViewState()
    data class CloudFlareConfigErrorMessage(val errorMessage: String) : ChatRoomInfoViewState()
    data class UploadImageCloudFlareSuccess(val imageUrl: String) : ChatRoomInfoViewState()

    data class DeleteLoadingState(val isLoading: Boolean) : ChatRoomInfoViewState()
    data class MiceAccess(val chatRoomUserInfo: List<MiceAccessInfo>?) : ChatRoomInfoViewState()
    object DeleteChatRoom : ChatRoomInfoViewState()
}