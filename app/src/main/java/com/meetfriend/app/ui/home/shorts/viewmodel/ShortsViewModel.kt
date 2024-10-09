package com.meetfriend.app.ui.home.shorts.viewmodel

import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.GetHashTagPostRequest
import com.meetfriend.app.api.post.model.GetShortsRequest
import com.meetfriend.app.api.post.model.PostLikeUnlikeRequest
import com.meetfriend.app.api.post.model.ShortsCountRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.responseclasses.video.DataVideo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ShortsViewModel(
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository
) : BasicViewModel() {

    private val shortsStateSubject: PublishSubject<ShortsViewState> = PublishSubject.create()
    val shortsState: Observable<ShortsViewState> = shortsStateSubject.hide()

    private var pageNoH = 1
    private var isLoadingH = false
    private var isLoadMoreH = true
    companion object {
        const val PER_PAGE = 10
    }

    private var listOfPostDataH: MutableList<DataVideo> = mutableListOf()

    fun getHashTagPost(hashTag: Int, searchString: String? = null) {
        val getHashTagPostRequest = GetHashTagPostRequest(
            pageNoH,
            PER_PAGE,
            hashTag,
            searchString
        )
        postRepository.hashTagsShorts(getHashTagPostRequest).doOnSubscribe {
            shortsStateSubject.onNext(ShortsViewState.LoadingState(true))
        }.doAfterTerminate {
            shortsStateSubject.onNext(ShortsViewState.LoadingState(false))
            isLoadingH = false
        }.subscribeOnIoAndObserveOnMainThread({ result ->
            shortsStateSubject.onNext(ShortsViewState.LoadingState(false))
            if (result.status) {
                if (result.result != null) {
                    if (pageNoH == 1) {
                        listOfPostDataH.clear()
                        updatePostListH(result.result.videoData)
                    } else {
                        if (result.result.videoData?.isNotEmpty() == true) {
                            updatePostListH(result.result.videoData)
                        } else {
                            isLoadMoreH = false
                        }
                    }
                } else {
                    shortsStateSubject.onNext(ShortsViewState.LoadingState(false))
                    isLoadMoreH = false
                    updatePostListH(listOf())
                }
            }
        }, { throwable ->
            isLoadMoreH = false
            shortsStateSubject.onNext(ShortsViewState.LoadingState(false))
            shortsStateSubject.onNext(
                ShortsViewState.ErrorMessage(
                    throwable.message ?: "Something went wrong. Please try again"
                )
            )
        }).autoDispose()
    }

    private fun updatePostListH(listOfPostInformation: List<DataVideo>) {
        listOfPostDataH.addAll(listOfPostInformation)

        shortsStateSubject.onNext(
            ShortsViewState.ShortsResponse(
                listOfPostDataH
            )
        )
    }

    fun loadMorePostH(hashTag: Int, searchString: String? = null) {
        if (!isLoadingH) {
            isLoadingH = true
            if (isLoadMoreH) {
                pageNoH++
                getHashTagPost(hashTag, searchString)
            }
        }
    }

    fun resetPaginationForHashTagPost(hashTag: Int, searchString: String? = null) {
        pageNoH = 1
        isLoadingH = false
        isLoadMoreH = true
        getHashTagPost(hashTag, searchString)
    }

    private val perPage = PER_PAGE
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfShorts: MutableList<DataVideo> = mutableListOf()
    fun getShorts(isFollowing: Int) {
        val getShortsRequest = GetShortsRequest(pageNo, perPage,"", isFollowing)
        postRepository.getVideos(getShortsRequest).doOnSubscribe {
            shortsStateSubject.onNext(ShortsViewState.LoadingState(true))
        }.doAfterTerminate {
            shortsStateSubject.onNext(ShortsViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({ listOfShortsInformation ->
            if (pageNo == 1) {
                listOfShorts.clear()
            } else {
                if (listOfShortsInformation.isEmpty()) {
                    isLoadMore = false
                }
            }
            updateShortsList(listOfShortsInformation)
        }, { throwable ->
            shortsStateSubject.onNext(
                ShortsViewState.ErrorMessage(
                    throwable.localizedMessage ?: "Something wrong. Please try again"
                )
            )
        }).autoDispose()
    }

    private fun updateShortsList(listOfShortsInformation: List<DataVideo>) {
        listOfShorts.addAll(listOfShortsInformation)
        shortsStateSubject.onNext(
            ShortsViewState.ShortsResponse(
                listOfShorts
            )
        )
    }

    fun loadMoreShorts(isFollowing: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getShorts(isFollowing)
            }
        }
    }

    fun resetPaginationForShorts(isFollowing: Int) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getShorts(isFollowing)
    }

    fun shortsLikeDisLike(postLikeUnlikeRequest: PostLikeUnlikeRequest) {
        postRepository.postLikeUnlike(postLikeUnlikeRequest).doOnSubscribe {
            shortsStateSubject.onNext(ShortsViewState.LoadingState(true))
        }.doAfterSuccess {
            shortsStateSubject.onNext(ShortsViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            shortsStateSubject.onNext(
                ShortsViewState.ShortsLikeUnlikeResponse(
                    response.message.toString(),
                    postLikeUnlikeRequest.postId
                )
            )
        }, { throwable ->
            shortsStateSubject.onNext(ShortsViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                shortsStateSubject.onNext(ShortsViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun updateShortsCount(shortsCountRequest: ShortsCountRequest) {
        postRepository.updateShortsCount(shortsCountRequest).doOnSubscribe {
            shortsStateSubject.onNext(ShortsViewState.LoadingState(true))
        }.doAfterSuccess {
            shortsStateSubject.onNext(ShortsViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            response.noOfShotCount?.let {
                shortsStateSubject.onNext(ShortsViewState.GetShortsCount(it))
            }
        }, { throwable ->
            shortsStateSubject.onNext(ShortsViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                shortsStateSubject.onNext(ShortsViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (!it.status) {
                    shortsStateSubject.onNext(ShortsViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    shortsStateSubject.onNext(ShortsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteShort(postId: Int) {
        postRepository.deletePost(postId)
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    shortsStateSubject.onNext(ShortsViewState.SuccessMessage("Short deleted successfully"))
                } else {
                    shortsStateSubject.onNext(ShortsViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    shortsStateSubject.onNext(ShortsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class ShortsViewState {
    data class ErrorMessage(val errorMessage: String) : ShortsViewState()
    data class SuccessMessage(val successMessage: String) : ShortsViewState()
    data class LoadingState(val isLoading: Boolean) : ShortsViewState()
    data class ShortsResponse(val shortsList: List<DataVideo>) : ShortsViewState()
    data class ShortsLikeUnlikeResponse(val message: String, val postId: Int) : ShortsViewState()
    data class GetShortsCount(val noOfShortsCount: Int) : ShortsViewState()
}
