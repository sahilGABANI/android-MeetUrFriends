package com.meetfriend.app.ui.myprofile.viewmodel

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.CreateOneToOneChatRequest
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.messages.MessagesRepository
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.GetHashTagPostRequest
import com.meetfriend.app.api.post.model.PostInformation
import com.meetfriend.app.api.post.model.PostLikeUnlikeRequest
import com.meetfriend.app.api.post.model.PostShareRequest
import com.meetfriend.app.api.userprofile.UserProfileRepository
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.responseclasses.UpdatePhotoResponse
import com.meetfriend.app.responseclasses.photos.Data
import com.meetfriend.app.responseclasses.photos.UserPhotosResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MultipartBody

class UserProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val loggedInUserCache: LoggedInUserCache,
    private val postRepository: PostRepository,
    private val messagesRepository: MessagesRepository,
    private val followRepository: FollowRepository
) : BasicViewModel() {

    private val userProfileStateSubject: PublishSubject<UserProfileViewState> =
        PublishSubject.create()
    val userProfileState: Observable<UserProfileViewState> = userProfileStateSubject.hide()

    fun userProfile(id: Int) {
        userProfileRepository.viewProfile(id).doOnSubscribe {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(true))
        }.doAfterTerminate {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
            userProfileStateSubject.onNext(UserProfileViewState.UserProfileData(response))
        }, { throwable ->
            userProfileStateSubject.onNext(
                UserProfileViewState.ErrorMessage(
                    throwable.localizedMessage ?: "Something wrong. Please try again"
                )
            )
        }).autoDispose()
    }

    fun editUserProfile(
        firstName: String? = null,
        lastName: String? = null,
        userName: String?= null,
        education: String?= null,
        gender: String?= null,
        city: String?= null,
        hobbies: String?= null,
        work: String?= null,
        dob: String?= null,
        bio: String?= null,
        relationship: String?= null,
        dob_string: String?= null,
        isPrivate: Int?= null
    ) {
        userProfileRepository.editUserProfile(
            firstName,
            lastName,
            userName,
            education,
            gender,
            city,
            hobbies,
            work,
            dob,
            bio,
            relationship,
            dob_string,
            isPrivate
        ).doOnSubscribe {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(true))
        }.doAfterTerminate {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
            userProfileStateSubject.onNext(UserProfileViewState.EditUserProfile(response.message.toString()))
        }, { throwable ->
            userProfileStateSubject.onNext(
                UserProfileViewState.ErrorMessage(
                    throwable.localizedMessage ?: "Something wrong. Please try again"
                )
            )
        }).autoDispose()
    }

    fun uploadProfileImage(requestBody: MultipartBody) {
        userProfileRepository.uploadProfileImage(requestBody).doOnSubscribe {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(true))
        }.doAfterTerminate {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
            userProfileStateSubject.onNext(UserProfileViewState.UploadUserProfileResponse(response))
        }, { throwable ->
            userProfileStateSubject.onNext(
                UserProfileViewState.ErrorMessage(
                    throwable.localizedMessage ?: "Something wrong. Please try again"
                )
            )
        }).autoDispose()
    }

    private var listOfLikePostData: MutableList<Data> = mutableListOf()
    private val FOR_USER_LIKE_PER_PAGE = 12
    private var pageNoUserLike = 1
    private var isLoadingForUserLike = false
    private var isLoadMoreUserLike = true

    fun userPhotosVideos(id: Int) {
        userProfileRepository.userPhotosVideos(id, pageNoUserLike, FOR_USER_LIKE_PER_PAGE)
            .doOnSubscribe {
                userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(true))
            }.doAfterTerminate {
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            if (response.status) {
                userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
                isLoadingForUserLike = false
                if (pageNoUserLike == 1) {
                    listOfLikePostData.clear()
                    updateUserLikePostList((response.result.data))
                } else {
                    if (response.result.data.isNotEmpty()) {
                        updateUserLikePostList(response.result.data)
                    } else {
                        isLoadMoreUserLike = false
                    }
                }
            }
        }, { throwable ->
            userProfileStateSubject.onNext(
                UserProfileViewState.ErrorMessage(
                    throwable.localizedMessage ?: "Something wrong. Please try again"
                )
            )
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
        }).autoDispose()
    }

    private fun updateUserLikePostList(listOfPostInformation: ArrayList<Data>) {
        listOfLikePostData.addAll(listOfPostInformation)
        userProfileStateSubject.onNext(
            UserProfileViewState.GetUserLikeInformation(
                listOfLikePostData
            )
        )
    }

    fun loadMoreUserLikePost(userId: Int) {
        if (!isLoadingForUserLike) {
            isLoadingForUserLike = true
            if (isLoadMoreUserLike) {
                pageNoUserLike++
                userPhotosVideos(userId)
            }
        }
    }

    fun resetPaginationForUserLikePost(userId: Int) {
        pageNoUserLike = 1
        isLoadingForUserLike = false
        isLoadMoreUserLike = true
        userPhotosVideos(userId)
    }

    private var listOfUserShortsData: MutableList<Data> = mutableListOf()
    private val FOR_USER_SHORTS_PER_PAGE = 12
    private var pageNoUserShorts = 1
    private var isLoadingForUserShorts = false
    private var isLoadMoreUserShorts = true
    fun userVideos(id: Int) {
        userProfileRepository.userVideos(id, pageNoUserShorts, FOR_USER_SHORTS_PER_PAGE)
            .doOnSubscribe {
                userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(true))
            }.doAfterTerminate {
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
            if (response.status) {
                isLoadingForUserShorts = false
                if (pageNoUserShorts == 1) {
                    listOfUserShortsData.clear()
                    updateVideoPostList((response.result.data))
                } else {
                    if (response.result.data.isNotEmpty()) {
                        updateVideoPostList(response.result.data)
                    } else {
                        isLoadMoreUserShorts = false
                    }
                }
            }
        }, { throwable ->
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
            userProfileStateSubject.onNext(
                UserProfileViewState.ErrorMessage(
                    throwable.localizedMessage ?: "Something wrong. Please try again"
                )
            )
        }).autoDispose()
    }

    private fun updateVideoPostList(listOfPostInformation: ArrayList<Data>) {
        listOfUserShortsData.addAll(listOfPostInformation)
        userProfileStateSubject.onNext(
            UserProfileViewState.GetShortsInformation(
                listOfUserShortsData
            )
        )
    }

    fun loadMoreUserShort(userId: Int) {
        if (!isLoadingForUserShorts) {
            isLoadingForUserShorts = true
            if (isLoadMoreUserShorts) {
                pageNoUserShorts++
                userVideos(userId)
            }
        }
    }

    fun resetPaginationForUserShorts(userId: Int) {
        pageNoUserShorts = 1
        isLoadingForUserShorts = false
        isLoadMoreUserShorts = true
        userVideos(userId)
    }

    private var pageNoH = 1
    private var isLoadingH = false
    private var isLoadMoreH = true

    private var listOfPostDataH: MutableList<PostInformation> = mutableListOf()

    fun getHashTagPost(hashTag: Int, searchString: String? = null) {
        val getHashTagPostRequest = GetHashTagPostRequest(
            pageNoH,
            PER_PAGE,
            hashTag,
            searchString
        )
        postRepository.hashTagsPosts(getHashTagPostRequest).doOnSubscribe {
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(true))
        }.doAfterTerminate {
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
            isLoadingH = false
        }.subscribeOnIoAndObserveOnMainThread({ it ->
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
            if (it.status) {
                if (it.result?.listOfPosts != null) {
                    if (pageNoH == 1) {
                        listOfPostDataH.clear()
                        updatePostListH(it.result.listOfPosts.listOfPosts ?: listOf())
                    } else {
                        if (it.result.listOfPosts.listOfPosts?.isNotEmpty() == true) {
                            updatePostListH(it.result.listOfPosts.listOfPosts)
                        } else {
                            isLoadMoreH = false
                        }
                    }
                } else {
                    userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
                    isLoadMoreH = false
                    updatePostListH(listOf())
                }
            }
        }, { throwable ->
            isLoadMore = false
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
            userProfileStateSubject.onNext(
                UserProfileViewState.ErrorMessage(
                    throwable.message ?: "Something wrong. Please try again"
                )
            )
        }).autoDispose()
    }

    private fun updatePostListH(listOfPostInformation: List<PostInformation>) {
        listOfPostDataH.addAll(listOfPostInformation)
        userProfileStateSubject.onNext(
            UserProfileViewState.GetPostResponse(
                listOfPostDataH
            )
        )
    }

    fun loadMorePostH(hashTag: Int, search: String? = null) {
        if (!isLoadingH) {
            isLoadingH = true
            if (isLoadMoreH) {
                pageNoH++
                getHashTagPost(hashTag, search)
            }
        }
    }

    fun resetPaginationForHashTagPost(hashTag: Int, search: String? = null) {
        pageNoH = 1
        isLoadingH = false
        isLoadMoreH = true
        getHashTagPost(hashTag, search)
    }


    private val PER_PAGE = 15
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfPostData: MutableList<PostInformation> = mutableListOf()

    fun getPost(userId: Int) {
        userProfileRepository.userPosts(userId, pageNo, PER_PAGE).doOnSubscribe {
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(true))
        }.doAfterTerminate {
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({ it ->
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
            if (it.status) {
                if (it.result != null) {
                    if (pageNo == 1) {
                        listOfPostData.clear()
                        updatePostList(it.result.listOfPosts ?: listOf())
                    } else {
                        if (it.result.listOfPosts?.isNotEmpty() == true) {
                            updatePostList(it.result.listOfPosts)
                        } else {
                            isLoadMore = false
                        }
                    }
                } else {
                    userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
                    isLoadMore = false
                    updatePostList(listOf())
                }
            }
        }, { throwable ->
            isLoadMore = false
            userProfileStateSubject.onNext(UserProfileViewState.PostLoadingState(false))
            userProfileStateSubject.onNext(
                UserProfileViewState.ErrorMessage(
                    throwable.message ?: "Something wrong. Please try again"
                )
            )
        }).autoDispose()
    }

    private fun updatePostList(listOfPostInformation: List<PostInformation>) {
        listOfPostData.addAll(listOfPostInformation)
        userProfileStateSubject.onNext(
            UserProfileViewState.GetPostResponse(
                listOfPostData
            )
        )
    }

    fun loadMorePost(userId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo++
                getPost(userId)
            }
        }
    }

    fun resetPaginationForUserPost(userId: Int) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getPost(userId)
    }

    fun createOneToOneChat(oneToOneChatRequest: CreateOneToOneChatRequest) {
        messagesRepository.createChatRoom(oneToOneChatRequest).doOnSubscribe {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(true))
        }.doAfterTerminate {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.data != null) {
                userProfileStateSubject.onNext(UserProfileViewState.OneToOneChatData(it.data))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                userProfileStateSubject.onNext(UserProfileViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                } else {
                    userProfileStateSubject.onNext(UserProfileViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    userProfileStateSubject.onNext(UserProfileViewState.ErrorMessage(it))
                }

            }).autoDispose()
    }

    fun blockUnBlockPeople(friendId: Int, blockStatus: String) {
        userProfileRepository.blockUnBlockPeople(friendId, blockStatus)
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    val successMessage =
                        if (blockStatus == "block") "User blocked successfully" else "User unblocked successfully"
                    userProfileStateSubject.onNext(
                        UserProfileViewState.BlockSuccessMessage(
                            successMessage
                        )
                    )

                } else {
                    userProfileStateSubject.onNext(UserProfileViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    userProfileStateSubject.onNext(UserProfileViewState.ErrorMessage(it))
                }

            }).autoDispose()
    }

    fun deletePost(postId: Int) {
        postRepository.deletePost(postId).doOnSubscribe {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(true))
        }.doAfterSuccess {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            userProfileStateSubject.onNext(UserProfileViewState.PostDeleteSuccessFully(response.message.toString()))
        }, { throwable ->
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                userProfileStateSubject.onNext(UserProfileViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun postShare(postShareRequest: PostShareRequest) {
        postRepository.postShare(postShareRequest).doOnSubscribe {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(true))
        }.doAfterSuccess {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            if (response.status) {
                userProfileStateSubject.onNext(UserProfileViewState.PostShareResponse(response.message.toString()))
            }
        }, { throwable ->
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                userProfileStateSubject.onNext(UserProfileViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun postLikeDisLike(postLikeUnlikeRequest :PostLikeUnlikeRequest) {
        postRepository.postLikeUnlike(postLikeUnlikeRequest).doOnSubscribe {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(true))
        }.doAfterSuccess {
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            if (response.status) {
                userProfileStateSubject.onNext(UserProfileViewState.PostLikeUnlikeResponse(response.message.toString()))
            }
        }, { throwable ->
            userProfileStateSubject.onNext(UserProfileViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                userProfileStateSubject.onNext(UserProfileViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

}

sealed class UserProfileViewState {
    data class ErrorMessage(val errorMessage: String) : UserProfileViewState()
    data class SuccessMessage(val successMessage: String) : UserProfileViewState()
    data class LoadingState(val isLoading: Boolean) : UserProfileViewState()
    data class UserProfileData(val userProfile: MeetFriendUser) : UserProfileViewState()
    data class EditUserProfile(val message: String) : UserProfileViewState()
    data class UploadUserProfileResponse(val updatePhotoResponse: UpdatePhotoResponse) :
        UserProfileViewState()

    data class UserVideos(val updatePhotoResponse: UserPhotosResponse) : UserProfileViewState()
    data class GetPostResponse(val userPhotosResponse: List<PostInformation>) :
        UserProfileViewState()

    data class GetShortsInformation(val userShortsResponse: List<Data>) : UserProfileViewState()
    data class GetUserLikeInformation(val userShortsResponse: List<Data>) : UserProfileViewState()
    data class OneToOneChatData(val chatRoomInfo: ChatRoomInfo) : UserProfileViewState()
    data class PostDeleteSuccessFully(val message: String) : UserProfileViewState()
    data class PostShareResponse(val message: String) : UserProfileViewState()
    data class PostLikeUnlikeResponse(val message: String) : UserProfileViewState()
    data class ReportUserSuccess(val successMessage: String) : UserProfileViewState()

    data class PostLoadingState(val isLoading: Boolean) : UserProfileViewState()
    data class BlockSuccessMessage(val successMessage: String) : UserProfileViewState()


}