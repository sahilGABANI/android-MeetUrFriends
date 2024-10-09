package com.meetfriend.app.ui.chatRoom.create.viewmodel

import android.content.Context
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.CreateChatRoomRequest
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

class CreateChatRoomViewModel(
    private val chatRepository: ChatRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loginUserCache: LoggedInUserCache,
) : BasicViewModel() {

    private val createChatRoomStateSubject: PublishSubject<CreateChatRoomViewState> = PublishSubject.create()
    val createChatRoomState: Observable<CreateChatRoomViewState> = createChatRoomStateSubject.hide()

    fun createChatRoom(createChatRoomRequest: CreateChatRoomRequest) {
        chatRepository.createChatRoom(createChatRoomRequest)
            .doOnSubscribe {
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        createChatRoomStateSubject.onNext(CreateChatRoomViewState.SuccessMessage(it))
                    }
                    response.data?.let {
                        createChatRoomStateSubject.onNext(CreateChatRoomViewState.CreateRoomSuccess(it))
                    }
                } else {
                    response.message?.let {
                        createChatRoomStateSubject.onNext(CreateChatRoomViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    createChatRoomStateSubject.onNext(CreateChatRoomViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.LoadingState(false))
                if (response.status) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        createChatRoomStateSubject.onNext(
                            CreateChatRoomViewState.GetCloudFlareConfig(
                                cloudFlareConfig
                            )
                        )
                    } else {
                        response.message?.let {
                            createChatRoomStateSubject.onNext(
                                CreateChatRoomViewState.CloudFlareConfigErrorMessage(
                                    it
                                )
                            )
                        }
                    }
                }
            }, { throwable ->
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    createChatRoomStateSubject.onNext(CreateChatRoomViewState.CloudFlareConfigErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadImageToCloudFlare(
        context: Context,
        cloudFlareConfig: CloudFlareConfig,
        imageFile: File,
    ) {
        val imageTempPathDir = context.getExternalFilesDir("MeetFriendImages")?.path
        val fileName =
            getCommonPhotoFileName(loginUserCache.getLoggedInUserId())
        val imageCopyFile = File(imageTempPathDir + File.separator + fileName + ".jpg")
        val finalImageFile = imageFile.copyTo(imageCopyFile)

        val apiUrl = cloudFlareImageUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        val filePart = MultipartBody.Part.createFormData(
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(ImageToCloudFlare(apiUrl, authToken, filePart))
            .doOnSubscribe {
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.LoadingState(true))
            }
            .doAfterSuccess {
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            createChatRoomStateSubject.onNext(
                                CreateChatRoomViewState.UploadImageCloudFlareSuccess(
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
                throwable.localizedMessage?.let {
                    createChatRoomStateSubject.onNext(CreateChatRoomViewState.ErrorMessage(it))
                }

            }).autoDispose()

    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                createChatRoomStateSubject.onNext(CreateChatRoomViewState.ErrorMessage(error.toString()))
            }
        }
    }
}

sealed class CreateChatRoomViewState {
    data class ErrorMessage(val errorMessage: String) : CreateChatRoomViewState()
    data class SuccessMessage(val successMessage: String) : CreateChatRoomViewState()
    data class LoadingState(val isLoading: Boolean) : CreateChatRoomViewState()
    data class CreateRoomSuccess(val chatRoomInfo: ChatRoomInfo) : CreateChatRoomViewState()
    data class CloudFlareConfigErrorMessage(val errorMessage: String) : CreateChatRoomViewState()
    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : CreateChatRoomViewState()
    data class UploadImageCloudFlareSuccess(val imageUrl: String) : CreateChatRoomViewState()
}