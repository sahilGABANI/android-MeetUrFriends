package com.meetfriend.app.ui.home.shortdetails.viewmodel

import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.PostLikeUnlikeRequest
import com.meetfriend.app.api.post.model.PostShareRequest
import com.meetfriend.app.api.post.model.ShortsCountRequest
import com.meetfriend.app.api.post.model.ViewPostRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.responseclasses.video.Post
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ShortDetailsViewModel(
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository
) : BasicViewModel() {

    private val shortDetailsStateSubjects: PublishSubject<ShortDetailsViewState> =
        PublishSubject.create()
    val shortDetailsState: Observable<ShortDetailsViewState> = shortDetailsStateSubjects.hide()

    fun viewPost(postId: String) {
        postRepository.viewPost(ViewPostRequest(postId,"shorts"))
            .doOnSubscribe {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(true))
            }
            .doAfterSuccess {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
                if (response.status) {
                    shortDetailsStateSubjects.onNext(ShortDetailsViewState.PostData(response.result!!))
                }
            }, { throwable ->
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    shortDetailsStateSubjects.onNext(ShortDetailsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun updateShortsCount(shortsCountRequest: ShortsCountRequest) {
        postRepository.updateShortsCount(shortsCountRequest).doOnSubscribe {
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(true))
        }.doAfterSuccess {
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            response.noOfShotCount?.let {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.GetShortsCount(it))
            }
        }, { throwable ->
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                } else {
                    shortDetailsStateSubjects.onNext(ShortDetailsViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    shortDetailsStateSubjects.onNext(ShortDetailsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun shortsLikeDisLike(postLikeUnlikeRequest: PostLikeUnlikeRequest) {
        postRepository.postLikeUnlike(postLikeUnlikeRequest).doOnSubscribe {
        }.doAfterSuccess {
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.ShortsLikeUnlikeResponse(response.message.toString()))
        }, { throwable ->
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun postShare(postShareRequest: PostShareRequest) {
        postRepository.postShare(postShareRequest).doOnSubscribe {
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(true))
        }.doAfterSuccess {
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            if (response.status) {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.PostShareResponse(response.message.toString()))
            }
        }, { throwable ->
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun deletePost(postId: Int) {
        postRepository.deletePost(postId).doOnSubscribe {
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(true))
        }.doAfterSuccess {
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.PostDeleteSuccessFully(response.message.toString()))
        }, { throwable ->
            shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
    fun postViewByUser(postId: String) {
        postRepository.userViewPost(postId = postId)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    shortDetailsStateSubjects.onNext(ShortDetailsViewState.SuccessViewMessage(it))
                }
            }, { throwable ->
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    shortDetailsStateSubjects.onNext(ShortDetailsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
    fun postLikeDisLike(postLikeUnlikeRequest: PostLikeUnlikeRequest) {
        postRepository.postLikeUnlike(postLikeUnlikeRequest)
            .doOnSubscribe {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(true))
            }.doAfterSuccess {
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                }
            }, { throwable ->
                shortDetailsStateSubjects.onNext(ShortDetailsViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    shortDetailsStateSubjects.onNext(ShortDetailsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class ShortDetailsViewState {
    data class ErrorMessage(val errorMessage: String) : ShortDetailsViewState()
    data class SuccessMessage(val successMessage: String) : ShortDetailsViewState()
    data class LoadingState(val isLoading: Boolean) : ShortDetailsViewState()
    data class PostData(val postData: Post) : ShortDetailsViewState()
    data class GetShortsCount(val noOfShortsCount: Int) : ShortDetailsViewState()
    data class ShortsLikeUnlikeResponse(val message: String) : ShortDetailsViewState()
    data class PostShareResponse(val message: String) : ShortDetailsViewState()
    data class SuccessViewMessage(val viewMessage: String) : ShortDetailsViewState()

    data class PostDeleteSuccessFully(val message: String) : ShortDetailsViewState()

}