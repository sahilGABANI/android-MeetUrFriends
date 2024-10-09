package com.meetfriend.app.ui.home.viewmodel

import android.content.Context
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.cloudflare.CloudFlareRepository
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.ImageToCloudFlare
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.GetPostRequest
import com.meetfriend.app.api.post.model.PostInformation
import com.meetfriend.app.api.post.model.PostLikeUnlikeRequest
import com.meetfriend.app.api.post.model.PostReportRequest
import com.meetfriend.app.api.post.model.PostShareRequest
import com.meetfriend.app.api.story.StoryRepository
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.cloudFlareImageUploadBaseUrl
import com.meetfriend.app.newbase.extension.getCommonPhotoFileName
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.ui.storywork.models.ResultListResult
import com.meetfriend.app.ui.storywork.models.StoryDetailResponse
import com.meetfriend.app.utils.Constant.FiXED_20_INT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class MainHomeViewModel(
    private val postRepository: PostRepository,
    private val storyRepository: StoryRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loginUserCache: LoggedInUserCache,
) : BasicViewModel() {

    companion object {
        val mainHomeStateSubject: PublishSubject<MainHomeViewState> = PublishSubject.create()
    }

    val mainHomeState: Observable<MainHomeViewState> = mainHomeStateSubject.hide()

    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfPostData: MutableList<PostInformation> = mutableListOf()

    private var listOfStories: MutableList<ResultListResult> = mutableListOf()
    private var pageNumberStory: Int = 1
    private var isLoadMoreStory: Boolean = true
    private var isLoadingStory: Boolean = false
    private var cloudFlareConfig: CloudFlareConfig? = null

    fun getPost() {
        val getPostRequest = GetPostRequest(pageNo, FiXED_20_INT, "")
        postRepository.getPost(getPostRequest)
            .doOnSubscribe {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
            }.doAfterTerminate {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
                isLoading = false
            }.subscribeOnIoAndObserveOnMainThread({ listOfPostInformation ->
                if (pageNo == 1) {
                    listOfPostData.clear()
                    updatePostList(listOfPostInformation)
                } else {
                    if (listOfPostInformation.isNotEmpty()) {
                        updatePostList(listOfPostInformation)
                    } else {
                        isLoadMore = false
                    }
                }
            }, { throwable ->
                mainHomeStateSubject.onNext(
                    MainHomeViewState.ErrorMessage(
                        throwable.localizedMessage ?: "Something wrong. Please try again"
                    )
                )
                Timber.e(throwable)
            }).autoDispose()
    }

    private fun updatePostList(listOfPostInformation: List<PostInformation>) {
        listOfPostData.addAll(listOfPostInformation)
        mainHomeStateSubject.onNext(
            MainHomeViewState.GetPostResponse(
                listOfPostData
            )
        )
    }

    fun loadMorePost() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getPost()
            }
        }
    }

    fun resetPaginationForPost() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getPost()
    }

    fun pullToRefreshStory(perPageNo: Int) {
        pageNumberStory = 1
        isLoadMoreStory = true
        isLoadingStory = false
        listOfStories.clear()
        getAllStory(perPageNo)
    }

    private fun getAllStory(perPageNo: Int) {
        Timber.tag(
            "HomePageStoryView"
        ).i("LoadMoreStories called pageNumberStory: $pageNumberStory & perPageNo: $perPageNo")
        storyRepository.getListOfStories(pageNumberStory, perPageNo)
            .doOnSubscribe {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
            }
            .doAfterTerminate {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.result?.let {
                    Timber.tag("Response").i("LoadMoreStories response?.result: ${response.result}")
                    if (pageNumberStory == 1) {
                        listOfStories.clear()
                        listOfStories = response.result.toMutableList()
                        mainHomeStateSubject.onNext(MainHomeViewState.GetAllStoryInfo(listOfStories))
                        isLoadingStory = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfStories.addAll(it)
                            mainHomeStateSubject.onNext(
                                MainHomeViewState.GetAllStoryInfo(
                                    listOfStories
                                )
                            )
                            isLoadingStory = false
                        } else {
                            pageNumberStory--
                            Timber.tag("HomePageStoryView").i("LoadMoreStories pageNumberStory: $pageNumberStory")
                            isLoadingStory = false
                        }
                    }
                }
            }, { throwable ->
                throwable.printStackTrace()
                throwable.localizedMessage?.let {
                    Timber.e(it)
                }
            }).autoDispose()
    }

    fun loadMoreStory(perPageNo: Int) {
        if (!isLoadingStory) {
            isLoadingStory = true
            if (isLoadMoreStory) {
                pageNumberStory++
                getAllStory(perPageNo)
            }
        }
    }

    fun getUserStory(userId: String) {
        storyRepository.getStories(userId).doOnSubscribe {
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
        }.doAfterSuccess {
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            mainHomeStateSubject.onNext(MainHomeViewState.StoryDetailsState(response))
        }, { throwable ->

            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun deleteStory(userId: String) {
        storyRepository.deleteStory(userId).doOnSubscribe {
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
        }.subscribeOnIoAndObserveOnMainThread({
            it.message?.let { story ->
                mainHomeStateSubject.onNext(MainHomeViewState.SuccessMessage(story))
            }
        }, { throwable ->
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun viewStory(userId: String) {
        storyRepository.viewStory(userId).doOnSubscribe {
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
        }.doAfterSuccess {
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
        }, { throwable ->
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun getConversationId(receiverId: Int) {
        storyRepository.getConversationId(receiverId).doOnSubscribe {
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
        }.doAfterSuccess {
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            mainHomeStateSubject.onNext(MainHomeViewState.GetConversation(it.conversationId ?: 0))
        }, { throwable ->
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun reportStory(userId: String, reason: String) {
        storyRepository.reportStory(userId, reason).doOnSubscribe {
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
        }.doAfterSuccess {
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            mainHomeStateSubject.onNext(MainHomeViewState.StoryReportState(response.message.toString()))
        }, { throwable ->
            mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun postLikeDisLike(postLikeUnlikeRequest: PostLikeUnlikeRequest) {
        postRepository.postLikeUnlike(postLikeUnlikeRequest)
            .doOnSubscribe {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
            }.doAfterSuccess {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    mainHomeStateSubject.onNext(
                        MainHomeViewState.PostLikeUnlikeResponse(
                            response.message.toString(),
                            postLikeUnlikeRequest.postId
                        )
                    )
                }
            }, { throwable ->
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun postShare(postShareRequest: PostShareRequest) {
        postRepository.postShare(postShareRequest)
            .doOnSubscribe {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
            }.doAfterSuccess {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    mainHomeStateSubject.onNext(
                        MainHomeViewState.PostShareResponse(
                            response.message.toString(),
                            postShareRequest.postId
                        )
                    )
                }
            }, { throwable ->
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun reportOrHidePost(postReportRequest: PostReportRequest) {
        postRepository.reportOrHidePost(postReportRequest)
            .doOnSubscribe {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
            }.doAfterSuccess {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    mainHomeStateSubject.onNext(MainHomeViewState.PostReportOrHideMessage(response.message.toString()))
                }
            }, { throwable ->
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deletePost(postId: Int) {
        postRepository.deletePost(postId)
            .doOnSubscribe {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
            }.doAfterSuccess {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                mainHomeStateSubject.onNext(MainHomeViewState.PostDeleteSuccessFully(response.message.toString()))
            }, { throwable ->
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun postViewByUser(postId: String) {
        postRepository.userViewPost(postId = postId)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    mainHomeStateSubject.onNext(MainHomeViewState.SuccessViewMessage(it))
                }
            }, { throwable ->
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
    fun getShareCount(postId: Int) {
        postRepository.getShareCount(postId)
            .doOnSubscribe {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
            }.doAfterSuccess {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                mainHomeStateSubject.onNext(MainHomeViewState.PostShareCount(response.no_of_shared_count))
            }, { throwable ->
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
    fun addStoryImage(addStoryRequest: AddStoryRequest) {
        postRepository.addStory(addStoryRequest)
            .doOnSubscribe {
                mainHomeStateSubject.onNext(MainHomeViewState.StoryLoadingState(true))
            }.doAfterSuccess {
                mainHomeStateSubject.onNext(MainHomeViewState.StoryLoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    mainHomeStateSubject.onNext(MainHomeViewState.AddStoryResponse(response.message.toString()))
                }
            }, { throwable ->
                mainHomeStateSubject.onNext(MainHomeViewState.StoryLoadingState(false))
                throwable.localizedMessage?.let {
                    mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getCloudFlareConfig(isFirstLoad: Boolean) {
        cloudFlareRepository.getCloudFlareConfig()
            .doOnSubscribe {
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(isFirstLoad))
                if (response.status) {
                    cloudFlareConfig = response.data
                    if (cloudFlareConfig != null) {
                        mainHomeStateSubject.onNext(
                            MainHomeViewState.GetCloudFlareConfig(
                                cloudFlareConfig ?: CloudFlareConfig()
                            )
                        )
                    } else {
                        response.message?.let {
                            mainHomeStateSubject.onNext(
                                MainHomeViewState.CloudFlareConfigErrorMessage(
                                    it
                                )
                            )
                        }
                    }
                }
            }, { throwable ->
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mainHomeStateSubject.onNext(
                        MainHomeViewState.CloudFlareConfigErrorMessage(
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
        Timber.tag("StoryOpenEditor").d("uploadImageToCloudFlare: imageFile: $imageFile")
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
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    val result = response.result
                    if (result != null) {
                        val variants = result.variants
                        if (!variants.isNullOrEmpty()) {
                            mainHomeStateSubject.onNext(
                                MainHomeViewState.UploadImageCloudFlareSuccess(
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
                mainHomeStateSubject.onNext(MainHomeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun handleCloudFlareMediaUploadError(errors: List<String>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.firstOrNull()
            if (error != null) {
                mainHomeStateSubject.onNext(MainHomeViewState.ErrorMessage(error.toString()))
            }
        }
    }
}

sealed class MainHomeViewState {
    data class ErrorMessage(val errorMessage: String) : MainHomeViewState()
    data class SuccessMessage(val successMessage: String) : MainHomeViewState()
    data class SuccessViewMessage(val viewMessage: String) : MainHomeViewState()
    data class LoadingState(val isLoading: Boolean) : MainHomeViewState()
    data class GetPostResponse(val postList: List<PostInformation>) : MainHomeViewState()
    data class GetAllStoryInfo(val storyListInfo: List<ResultListResult>) : MainHomeViewState()

    data class PostLikeUnlikeResponse(val message: String, val postId: Int) : MainHomeViewState()
    data class PostShareResponse(val message: String, val postId: Int) : MainHomeViewState()
    data class PostDeleteSuccessFully(val message: String) : MainHomeViewState()
    data class PostReportOrHideMessage(val message: String) : MainHomeViewState()
    data class PostShareCount(val count: Int) : MainHomeViewState()
    data class AddStoryResponse(val message: String) : MainHomeViewState()
    data class StoryLoadingState(val isLoading: Boolean) : MainHomeViewState()
    data class StoryDetailsState(val storyDetailResult: StoryDetailResponse) : MainHomeViewState()
    data class StoryReportState(val successMessage: String) : MainHomeViewState()
    data class GetCloudFlareConfig(val cloudFlareConfig: CloudFlareConfig) : MainHomeViewState()
    data class CloudFlareConfigErrorMessage(val errorMessage: String) : MainHomeViewState()
    data class UploadImageCloudFlareSuccess(val imageUrl: String, val currentRequestIndex: Int) : MainHomeViewState()
    data class GetConversation(val conversationId: Int) : MainHomeViewState()
}
