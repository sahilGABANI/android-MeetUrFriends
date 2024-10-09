package com.meetfriend.app.ui.chatRoom.viewmodel

import android.content.Context
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.cloudflare.CloudFlareRepository
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.ImageToCloudFlare
import com.meetfriend.app.api.profile.ProfileRepository
import com.meetfriend.app.api.profile.model.ChangeProfileRequest
import com.meetfriend.app.api.profile.model.CreateChatRoomUserRequest
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

class ChatRoomCreateUserViewModel(
    private val profileRepository: ProfileRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loginUserCache: LoggedInUserCache,
) : BasicViewModel() {

    private val userCreateStateSubject: PublishSubject<ChatRoomCreateUserViewState> =
        PublishSubject.create()
    val userCreateState: Observable<ChatRoomCreateUserViewState> = userCreateStateSubject.hide()

    val per_page = 20

    fun createChatRoomUser(createChatRoomUserRequest: CreateChatRoomUserRequest) {
        profileRepository.createChatRoomUser(createChatRoomUserRequest)
            .doOnSubscribe {
                userCreateStateSubject.onNext(ChatRoomCreateUserViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                userCreateStateSubject.onNext(ChatRoomCreateUserViewState.LoadingState(false))
                userCreateStateSubject.onNext(ChatRoomCreateUserViewState.CreateChatUser(response.status))
                if (response.status) {
                    response.message?.let {
                        userCreateStateSubject.onNext(
                            ChatRoomCreateUserViewState.ChatRoomSuccessMessage(
                                it
                            )
                        )
                    }
                } else {
                    userCreateStateSubject.onNext(ChatRoomCreateUserViewState.LoadingState(false))
                    response.message?.let {
                        userCreateStateSubject.onNext(ChatRoomCreateUserViewState.ErrorMessage(it))
                        userCreateStateSubject.onNext(
                            ChatRoomCreateUserViewState.EditUserNameErrorMessage(
                                it
                            )
                        )
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    userCreateStateSubject.onNext(ChatRoomCreateUserViewState.LoadingState(false))
                    userCreateStateSubject.onNext(ChatRoomCreateUserViewState.ErrorMessage(it))
                    userCreateStateSubject.onNext(
                        ChatRoomCreateUserViewState.EditUserNameErrorMessage(
                            it
                        )
                    )
                }
            }).autoDispose()
    }

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                userCreateStateSubject.onNext(
                    ChatRoomCreateUserViewState.CloudFlareLoadingState(
                        true
                    )
                )
            }
            .doAfterTerminate {
                userCreateStateSubject.onNext(
                    ChatRoomCreateUserViewState.CloudFlareLoadingState(
                        false
                    )
                )
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        userCreateStateSubject.onNext(
                            ChatRoomCreateUserViewState.GetCloudFlareConfig(
                                cloudFlareConfig
                            )
                        )
                    } else {
                        response.message?.let {
                            userCreateStateSubject.onNext(
                                ChatRoomCreateUserViewState.CloudFlareConfigErrorMessage(
                                    it
                                )
                            )
                        }
                    }
                }
            }, { throwable ->
                userCreateStateSubject.onNext(ChatRoomCreateUserViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    userCreateStateSubject.onNext(
                        ChatRoomCreateUserViewState.CloudFlareConfigErrorMessage(
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
            "file", finalImageFile.name, finalImageFile.asRequestBody("image/*".toMediaTypeOrNull())
        )
        cloudFlareRepository.uploadImageToCloudFlare(ImageToCloudFlare(apiUrl, authToken, filePart))
            .doOnSubscribe {
                userCreateStateSubject.onNext(
                    ChatRoomCreateUserViewState.CloudFlareLoadingState(
                        true
                    )
                )
            }
            .doAfterTerminate {
                userCreateStateSubject.onNext(
                    ChatRoomCreateUserViewState.CloudFlareLoadingState(
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
                            userCreateStateSubject.onNext(
                                ChatRoomCreateUserViewState.UploadImageCloudFlareSuccess(
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
                userCreateStateSubject.onNext(ChatRoomCreateUserViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    userCreateStateSubject.onNext(ChatRoomCreateUserViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                userCreateStateSubject.onNext(ChatRoomCreateUserViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun editProfile(changeProfileRequest: ChangeProfileRequest) {
        profileRepository.changeProfile(changeProfileRequest)
            .doOnSubscribe {
                userCreateStateSubject.onNext(ChatRoomCreateUserViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                userCreateStateSubject.onNext(ChatRoomCreateUserViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        userCreateStateSubject.onNext(
                            ChatRoomCreateUserViewState.EditProfileSuccessMessage(
                                it
                            )
                        )
                    }
                } else {
                    response.message?.let {
                        userCreateStateSubject.onNext(ChatRoomCreateUserViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                userCreateStateSubject.onNext(ChatRoomCreateUserViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    userCreateStateSubject.onNext(ChatRoomCreateUserViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


}

sealed class ChatRoomCreateUserViewState {
    data class ErrorMessage(val errorMessage: String) : ChatRoomCreateUserViewState()
    data class SuccessMessage(val successMessage: String) : ChatRoomCreateUserViewState()
    data class EditProfileSuccessMessage(val successMessage: String) : ChatRoomCreateUserViewState()
    data class ChatRoomSuccessMessage(val successMessage: String) : ChatRoomCreateUserViewState()
    data class LoadingState(val isLoading: Boolean) : ChatRoomCreateUserViewState()
    data class CloudFlareConfigErrorMessage(val errorMessage: String) :
        ChatRoomCreateUserViewState()

    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) :
        ChatRoomCreateUserViewState()

    data class UploadImageCloudFlareSuccess(val imageUrl: String) : ChatRoomCreateUserViewState()
    data class CloudFlareLoadingState(val isLoading: Boolean) : ChatRoomCreateUserViewState()
    data class CreateChatUser(val isUserCreated: Boolean) : ChatRoomCreateUserViewState()
    data class EditUserNameErrorMessage(val errorMessage: String) : ChatRoomCreateUserViewState()
}