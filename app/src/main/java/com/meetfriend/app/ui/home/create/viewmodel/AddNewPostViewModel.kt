package com.meetfriend.app.ui.home.create.viewmodel

import android.content.Context
import androidx.lifecycle.Observer
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.cloudflare.CloudFlareRepository
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.ImageToCloudFlare
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.CreatePostRequest
import com.meetfriend.app.api.post.model.EditShortRequest
import com.meetfriend.app.api.post.model.HashTagsResponse
import com.meetfriend.app.api.post.model.MentionUserRequest
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.cloudFlareGetVideoUploadBaseUrl
import com.meetfriend.app.newbase.extension.cloudFlareImageUploadBaseUrl
import com.meetfriend.app.newbase.extension.cloudFlareVideoUploadBaseUrl
import com.meetfriend.app.newbase.extension.getCommonPhotoFileName
import com.meetfriend.app.newbase.extension.getCommonVideoFileName
import com.meetfriend.app.newbase.extension.getSelectedHashTags
import com.meetfriend.app.newbase.extension.getSelectedTagUserIds
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.skydoves.viewmodel.lifecycle.viewModelLifecycleOwner
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class AddNewPostViewModel(
    private val cloudFlareRepository: CloudFlareRepository,
    private val postRepository: PostRepository,
    private val loginUserCache: LoggedInUserCache,
) : BasicViewModel() {

    private val addNewPostStateSubjects: PublishSubject<AddNewPostViewState> =
        PublishSubject.create()
    val addNewPostState: Observable<AddNewPostViewState> = addNewPostStateSubjects.hide()

    private var selectedTagUserInfo: MutableList<MeetFriendUser> = mutableListOf()
    private var selectedHashTagInfo: MutableList<HashTagsResponse> = mutableListOf()

    private var videoUid: String? = null
    private var cloudFlareConfig: CloudFlareConfig? = null
    companion object {
        const val DELAY = 5000L
    }

    fun getCloudFlareConfig(isFirstLoad: Boolean) {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(isFirstLoad))
                if (response.status) {
                    cloudFlareConfig = response.data
                    val cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        addNewPostStateSubjects.onNext(
                            AddNewPostViewState.GetCloudFlareConfig(
                                cloudFlareConfig
                            )
                        )
                    } else {
                        response.message?.let {
                            addNewPostStateSubjects.onNext(
                                AddNewPostViewState.CloudFlareConfigErrorMessage(
                                    it
                                )
                            )
                        }
                    }
                }
            }, { throwable ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(
                        AddNewPostViewState.CloudFlareConfigErrorMessage(
                            it
                        )
                    )
                }
            }).autoDispose()
    }

    fun uploadImageToCloudFlare(
        context: Context,
        cloudFlareConfig: CloudFlareConfig,
        imageFile: File,
        totalRequests: Int? = 1,
        currentRequestIndex: Int? = 0
    ) {
        val imageTempPathDir = context.getExternalFilesDir("OutgoerImages")?.path
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
        cloudFlareRepository.uploadImageToCloudFlare(
            ImageToCloudFlare(
                apiUrl,
                authToken,
                filePart,
                finalImageFile.name,
                totalRequests,
                currentRequestIndex
            )
        )
            .doOnSubscribe {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            addNewPostStateSubjects.onNext(
                                AddNewPostViewState.UploadImageCloudFlareSuccess(
                                    variants.first(),
                                    currentRequestIndex ?: 0
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
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addStory(addStoryRequest: AddStoryRequest) {
        postRepository.addStory(addStoryRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.StoryUploadSuccess(response.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getLiveDataInfo() {
        cloudFlareRepository.getLiveData().observe(
            viewModelLifecycleOwner,
            Observer {
                addNewPostStateSubjects.onNext(AddNewPostViewState.ProgressDisplay(it))
            }
        )
    }
    fun uploadVideoToCloudFlare(
        context: Context,
        cloudFlareConfig: CloudFlareConfig,
        videoFile: File,
        currentRequestIndex: Int? = null
    ) {
        val videoTempPathDir = context.getExternalFilesDir("MeetFriendsVideos")?.path
        val fileName =
            getCommonVideoFileName(loginUserCache.getLoggedInUserId())
        val videoCopyFile = File(videoTempPathDir + File.separator + fileName + ".mp4")
        val finalImageFile = videoFile.copyTo(videoCopyFile)
        val apiUrl = cloudFlareVideoUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        cloudFlareRepository.uploadVideoUsingTus(apiUrl, authToken, finalImageFile)
            .subscribeOn(Schedulers.io())
            .flatMap {
                cloudFlareRepository.getUploadVideoDetails(it, authToken)
            }
            .observeOn(AndroidSchedulers.mainThread()) // Observe on main thread if needed
            .subscribe({
                videoUid = it.uid
                addNewPostStateSubjects.onNext(
                    AddNewPostViewState.UploadVideoCloudFlareSuccess(
                        it.uid.toString(),
                        it.thumbnail.toString(),
                        currentRequestIndex
                    )
                )
            }, { throwable ->
                Timber.e(throwable)
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(error.toString()))
            }
        }
    }

    fun createPost(request: CreatePostRequest, postType: String) {
        request.mentionIds = request.content?.let {
            getSelectedTagUserIds(
                selectedTagUserInfo,
                it
            )
        }

        request.tagNames = request.content?.let {
            getSelectedHashTags(
                it
            )
        }

        postRepository.createPost(request)
            .doOnSubscribe {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                if (response.status) {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.CreatePostType(postType, response.postId))

                    if (request.typeOfMedia == "shorts") {
                        getVideoStatusCheckAPI()
                    } else {
                        addNewPostStateSubjects.onNext(AddNewPostViewState.CreatePostSuccessMessage)
                    }
                }
            }, { throwable ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(
                        AddNewPostViewState.CloudFlareConfigErrorMessage(
                            it
                        )
                    )
                }
            }).autoDispose()
    }

    fun getUserForMention(searchText: String) {
        postRepository.mentionUser(MentionUserRequest(searchText))
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.UserListForMention(it.result))
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

    fun getHashTagList(searchText: String) {
        postRepository.getHashTagList(searchText)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.result.let {
                    addNewPostStateSubjects.onNext(
                        AddNewPostViewState.HashTagListForMention(
                            it?.data ?: arrayListOf()
                        )
                    )
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
        if (user !in selectedTagUserInfo) {
            selectedTagUserInfo.add(user)
        }
        val remainString = initialDescriptionString.removePrefix(subString)
        if (subString.length == initialDescriptionString.length) {
            val lastIndexOfToken =
                initialDescriptionString.findLastAnyOf(listOf("@"))?.first ?: return
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString@${user.userName}"
            addNewPostStateSubjects.onNext(
                AddNewPostViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${user.userName} $remainString"
            addNewPostStateSubjects.onNext(
                AddNewPostViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        }
    }

    fun searchHashTagClicked(
        initialDescriptionString: String,
        subString: String,
        user: HashTagsResponse,
    ) {
        if (user !in selectedHashTagInfo) {
            selectedHashTagInfo.add(user)
        }
        val remainString = initialDescriptionString.removePrefix(subString)

        if (subString.length == initialDescriptionString.length) {
            val lastIndexOfToken =
                initialDescriptionString.findLastAnyOf(listOf("#"))?.first ?: return
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString${user.tagName}"
            addNewPostStateSubjects.onNext(
                AddNewPostViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("#"))?.first ?: return
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString ${user.tagName} $remainString"
            addNewPostStateSubjects.onNext(
                AddNewPostViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        }
    }

    fun editShort(request: EditShortRequest) {
        request.mentionIds = request.content?.let {
            getSelectedTagUserIds(
                selectedTagUserInfo,
                it
            )
        }

        request.tagNames = request.content?.let {
            getSelectedHashTags(
                it
            )
        }
        postRepository.editShort(request)
            .doOnSubscribe {
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                if (response.status) {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.EditShortSuccessMessage)
                }
            }, { throwable ->
                addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun getVideoStatusCheckAPI() {
        if (cloudFlareConfig != null) {
            val apiUrl = cloudFlareGetVideoUploadBaseUrl.format(cloudFlareConfig?.accountId, videoUid)
            val authToken = "Bearer ".plus(cloudFlareConfig?.apiToken)
            cloudFlareRepository.getUploadVideoStatus(apiUrl, authToken).observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
                } // Observe on main thread if needed
                .subscribeOnIoAndObserveOnMainThread({
                    addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(true))
                    if (it.result?.status?.state != "ready") {
                        Observable.timer(DELAY, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                            getVideoStatusCheckAPI()
                        }
                    } else {
                        addNewPostStateSubjects.onNext(AddNewPostViewState.LoadingState(false))
                        addNewPostStateSubjects.onNext(AddNewPostViewState.CreateShortSuccessMessage)
                    }
                }, {
                    Timber.tag("UploadingPostReelsService").e(it)
                })
        }
    }
}

sealed class AddNewPostViewState {
    data class ProgressDisplay(val progressInfo: Double) : AddNewPostViewState()
    data class ErrorMessage(val errorMessage: String) : AddNewPostViewState()
    data class SuccessMessage(val successMessage: String) : AddNewPostViewState()
    data class LoadingState(val isLoading: Boolean) : AddNewPostViewState()

    data class CloudFlareConfigErrorMessage(val errorMessage: String) : AddNewPostViewState()
    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : AddNewPostViewState()

    data class UploadImageCloudFlareSuccess(val imageUrl: String, val currentRequestIndex: Int) : AddNewPostViewState()
    data class UploadVideoCloudFlareSuccess(val videoId: String, val thumbnail: String, val currentRequestIndex: Int?) :
        AddNewPostViewState()

    data class CreatePostType(val postType: String, val postId: Int?) : AddNewPostViewState()

    object CreatePostSuccessMessage : AddNewPostViewState()
    object CreateShortSuccessMessage : AddNewPostViewState()
    data class UserListForMention(val listOfUserForMention: List<MeetFriendUser>?) :
        AddNewPostViewState()

    data class HashTagListForMention(val listOfUserForMention: List<HashTagsResponse>?) :
        AddNewPostViewState()

    data class UpdateDescriptionText(val descriptionString: String) : AddNewPostViewState()

    object EditShortSuccessMessage : AddNewPostViewState()
    data class StoryUploadSuccess(val successMessage: String) : AddNewPostViewState()
}
