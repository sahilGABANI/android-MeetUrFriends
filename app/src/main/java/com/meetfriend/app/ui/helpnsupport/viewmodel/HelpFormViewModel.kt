package com.meetfriend.app.ui.helpnsupport.viewmodel

import android.content.Context
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.cloudflare.CloudFlareRepository
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.ImageToCloudFlare
import com.meetfriend.app.api.profile.ProfileRepository
import com.meetfriend.app.api.profile.model.SendHelpRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class HelpFormViewModel(
    private val profileRepository: ProfileRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loginUserCache: LoggedInUserCache,

    ) : BasicViewModel() {

    private val helpFormStateSubject: PublishSubject<HelpFormViewState> = PublishSubject.create()
    val helpFormState: Observable<HelpFormViewState> = helpFormStateSubject.hide()

    fun getCloudFlareConfig() {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                helpFormStateSubject.onNext(HelpFormViewState.LoadingState(true))
            }
            .doAfterTerminate {
                helpFormStateSubject.onNext(HelpFormViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        helpFormStateSubject.onNext(
                            HelpFormViewState.GetCloudFlareConfig(
                                cloudFlareConfig
                            )
                        )
                    } else {
                        response.message?.let {
                            helpFormStateSubject.onNext(
                                HelpFormViewState.CloudFlareConfigErrorMessage(
                                    it
                                )
                            )
                        }
                    }
                }
            }, { throwable ->
                helpFormStateSubject.onNext(HelpFormViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    helpFormStateSubject.onNext(
                        HelpFormViewState.CloudFlareConfigErrorMessage(
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
                helpFormStateSubject.onNext(
                    HelpFormViewState.CloudFlareLoadingState(
                        true
                    )
                )
            }
            .doAfterTerminate {
                helpFormStateSubject.onNext(
                    HelpFormViewState.CloudFlareLoadingState(
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
                            helpFormStateSubject.onNext(
                                HelpFormViewState.UploadImageCloudFlareSuccess(
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
                helpFormStateSubject.onNext(HelpFormViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    helpFormStateSubject.onNext(HelpFormViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                helpFormStateSubject.onNext(HelpFormViewState.ErrorMessage(error.toString()))
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
                helpFormStateSubject.onNext(
                    HelpFormViewState.CloudFlareLoadingState(
                        true
                    )
                )
            }
            .subscribeOn(Schedulers.io())
            .flatMap {
                cloudFlareRepository.getUploadVideoDetails(it, authToken)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterSuccess {
                helpFormStateSubject.onNext(
                    HelpFormViewState.CloudFlareLoadingState(
                        false
                    )
                )
            }
            .subscribe({
                helpFormStateSubject.onNext(
                    HelpFormViewState.UploadVideoCloudFlareSuccess(
                        it.uid.toString(),
                        it.thumbnail.toString()
                    )
                )
            }, { throwable ->
                Timber.e(throwable)
                helpFormStateSubject.onNext(HelpFormViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    helpFormStateSubject.onNext(HelpFormViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun sendFeedback(sendHelpRequest: SendHelpRequest) {
        profileRepository.sendHelp(sendHelpRequest)
            .doOnSubscribe {
                helpFormStateSubject.onNext(HelpFormViewState.LoadingState(true))
            }.doAfterSuccess {
                helpFormStateSubject.onNext(HelpFormViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                helpFormStateSubject.onNext(HelpFormViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        helpFormStateSubject.onNext(HelpFormViewState.SuccessMessage(it))
                    }
                } else {
                    response.message?.let {
                        helpFormStateSubject.onNext(HelpFormViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    helpFormStateSubject.onNext(HelpFormViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class HelpFormViewState {
    data class ErrorMessage(val errorMessage: String) : HelpFormViewState()
    data class SuccessMessage(val successMessage: String) : HelpFormViewState()
    data class LoadingState(val isLoading: Boolean) : HelpFormViewState()
    data class CloudFlareConfigErrorMessage(val errorMessage: String) : HelpFormViewState()
    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) :
        HelpFormViewState()

    data class UploadImageCloudFlareSuccess(val imageUrl: String) : HelpFormViewState()
    data class CloudFlareLoadingState(val isLoading: Boolean) : HelpFormViewState()
    data class UploadVideoCloudFlareSuccess(val videoId: String, val thumbnail: String) :
        HelpFormViewState()
}