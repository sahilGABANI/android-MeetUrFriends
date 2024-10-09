package com.meetfriend.app.ui.chatRoom.profile.viewmodel

import android.content.Context
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.CreateOneToOneChatRequest
import com.meetfriend.app.api.cloudflare.CloudFlareRepository
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.ImageToCloudFlare
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.profile.ProfileRepository
import com.meetfriend.app.api.profile.model.ChangeProfileRequest
import com.meetfriend.app.api.profile.model.ChatRoomUserProfileInfo
import com.meetfriend.app.api.profile.model.CreateChatRoomUserRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.cloudFlareImageUploadBaseUrl
import com.meetfriend.app.newbase.extension.getCommonPhotoFileName
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForGetProfile
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loginUserCache: LoggedInUserCache,
    private val chatRepository: ChatRepository,
    private val followRepository: FollowRepository

) : BasicViewModel() {

    private val profileStateSubject: PublishSubject<ProfileViewState> = PublishSubject.create()
    val profileState: Observable<ProfileViewState> = profileStateSubject.hide()

    companion object {
        const val perPage = 20
    }

    fun createChatRoomUser(createChatRoomUserRequest: CreateChatRoomUserRequest) {
        profileRepository.createChatRoomUser(createChatRoomUserRequest)
            .doOnSubscribe {
                profileStateSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                profileStateSubject.onNext(ProfileViewState.LoadingState(false))
                profileStateSubject.onNext(ProfileViewState.CreateChatUser(response.status))
                if (response.status) {
                    response.message?.let {
                        profileStateSubject.onNext(ProfileViewState.ChatRoomSuccessMessage(it))
                    }
                } else {
                    response.message?.let {
                        profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                        profileStateSubject.onNext(ProfileViewState.EditUserNameErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                    profileStateSubject.onNext(ProfileViewState.EditUserNameErrorMessage(it))
                }
            }).autoDispose()
    }

    fun editProfile(changeProfileRequest: ChangeProfileRequest) {
        profileRepository.changeProfile(changeProfileRequest)
            .doOnSubscribe {
                profileStateSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                profileStateSubject.onNext(ProfileViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        profileStateSubject.onNext(ProfileViewState.EditProfileSuccessMessage(it))
                    }
                } else {
                    response.message?.let {
                        profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getChatRoomUser() {
        profileRepository.getChatRoomUserProfile(perPage)
            .doOnSubscribe {
                profileStateSubject.onNext(ProfileViewState.GetChatRoomUserLoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                profileStateSubject.onNext(ProfileViewState.GetChatRoomUserLoadingState(false))
                if (response.status) {
                    response.let {
                        profileStateSubject.onNext(ProfileViewState.GetProfileData(it))
                    }
                    response.message?.let {
                        profileStateSubject.onNext(ProfileViewState.SuccessMessage(it))
                    }
                } else {
                    response.message?.let {
                        profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                profileStateSubject.onNext(ProfileViewState.CloudFlareLoadingState(true))
            }
            .doAfterTerminate {
                profileStateSubject.onNext(ProfileViewState.CloudFlareLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        profileStateSubject.onNext(
                            ProfileViewState.GetCloudFlareConfig(
                                cloudFlareConfig
                            )
                        )
                    } else {
                        response.message?.let {
                            profileStateSubject.onNext(
                                ProfileViewState.CloudFlareConfigErrorMessage(
                                    it
                                )
                            )
                        }
                    }
                }
            }, { throwable ->
                profileStateSubject.onNext(ProfileViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    profileStateSubject.onNext(ProfileViewState.CloudFlareConfigErrorMessage(it))
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
                profileStateSubject.onNext(ProfileViewState.CloudFlareLoadingState(true))
            }
            .doAfterTerminate {
                profileStateSubject.onNext(ProfileViewState.CloudFlareLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            profileStateSubject.onNext(
                                ProfileViewState.UploadImageCloudFlareSuccess(
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
                profileStateSubject.onNext(ProfileViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteProfileImage(id: Int) {
        profileRepository.deleteProfileImage(id)
            .doOnSubscribe {
                profileStateSubject.onNext(ProfileViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                profileStateSubject.onNext(ProfileViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        profileStateSubject.onNext(ProfileViewState.DeleteProfileSuccessMessage(it))
                    }
                } else {
                    response.message?.let {
                        profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                profileStateSubject.onNext(ProfileViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun getOtherUserProfile(userId: Int) {
        profileRepository.getOtherUserProfile(userId, perPage)
            .doOnSubscribe {
                profileStateSubject.onNext(ProfileViewState.LoadingState(true))
            }.doAfterTerminate {
                profileStateSubject.onNext(ProfileViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread(
                { response ->
                    profileStateSubject.onNext(ProfileViewState.LoadingState(false))
                    if (response.status) {
                        response.let {
                            profileStateSubject.onNext(ProfileViewState.GetProfileData(it))
                        }
                    }
                },
                { throwable ->
                    throwable.localizedMessage?.let {
                        profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                    }
                }
            ).autoDispose()
    }

    fun createOneToOneChat(oneToOneChatRequest: CreateOneToOneChatRequest) {
        chatRepository.createOneToOneChat(oneToOneChatRequest).doOnSubscribe {
            profileStateSubject.onNext(ProfileViewState.LoadingState(true))
        }.doAfterTerminate {
            profileStateSubject.onNext(ProfileViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread(
            {
                if (it.data != null) {
                    profileStateSubject.onNext(ProfileViewState.OneToOneChatData(it.data))
                }
            },
            { throwable ->
                throwable.localizedMessage?.let {
                    profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }

        ).autoDispose()
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (!it.status) {
                    profileStateSubject.onNext(ProfileViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    profileStateSubject.onNext(ProfileViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class ProfileViewState {
    data class ErrorMessage(val errorMessage: String) : ProfileViewState()
    data class SuccessMessage(val successMessage: String) : ProfileViewState()
    data class EditProfileSuccessMessage(val successMessage: String) : ProfileViewState()
    data class DeleteProfileSuccessMessage(val successMessage: String) : ProfileViewState()
    data class ChatRoomSuccessMessage(val successMessage: String) : ProfileViewState()
    data class LoadingState(val isLoading: Boolean) : ProfileViewState()

    data class CloudFlareConfigErrorMessage(val errorMessage: String) : ProfileViewState()
    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : ProfileViewState()
    data class UploadImageCloudFlareSuccess(val imageUrl: String) : ProfileViewState()
    data class CloudFlareLoadingState(val isLoading: Boolean) : ProfileViewState()

    data class GetProfileData(val profileData: MeetFriendResponseForGetProfile<ChatRoomUserProfileInfo>) :
        ProfileViewState()

    data class CreateChatUser(val isUserCreated: Boolean) : ProfileViewState()
    data class GetChatRoomUserLoadingState(val isLoading: Boolean) : ProfileViewState()
    data class EditUserNameErrorMessage(val errorMessage: String) : ProfileViewState()
    data class OneToOneChatData(val chatRoomInfo: ChatRoomInfo) : ProfileViewState()
}
