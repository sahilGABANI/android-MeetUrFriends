package com.meetfriend.app.ui.hashtag.viewmodel

import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.hashtag.HashtagRepository
import com.meetfriend.app.api.hashtag.model.HashtagResponse
import com.meetfriend.app.api.hashtag.model.ReportHashtagRequest
import com.meetfriend.app.api.hashtag.model.ReportHashtagResponse
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HashTagViewModel(private val hashtagRepository: HashtagRepository, private val followRepository: FollowRepository) : BasicViewModel() {

    private val hashTagsStateSubject: PublishSubject<HashTagsViewState> = PublishSubject.create()
    val hashTagsState: Observable<HashTagsViewState> = hashTagsStateSubject.hide()

    fun getHashtagDetails(hashTagId: Int) {
        hashtagRepository.getHashtagDetails(hashTagId).doOnSubscribe {
            hashTagsStateSubject.onNext(HashTagsViewState.LoadingState(true))
        }.doAfterTerminate {
            hashTagsStateSubject.onNext(HashTagsViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            hashTagsStateSubject.onNext(HashTagsViewState.LoadingState(false))
            if (it.status) {
                hashTagsStateSubject.onNext(HashTagsViewState.HashtagResponses(it.data))
            } else {
                hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun followUnfollow(userId: Int, hashtagId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    getHashtagDetails(hashtagId)
                } else {
                    hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }



    private var perPageH = 15
    private var pageNoH = 1
    private var isLoadingH = false
    private var isLoadMoreH = true

    private var listOfFollowingH: ArrayList<HashtagResponse> = arrayListOf()

    fun getHashtagUsers(search: String? = null) {
        hashtagRepository.getHashtagList(
            page = pageNoH,
            perPage = perPageH,
            search = search
        ).doOnSubscribe {
            hashTagsStateSubject.onNext(HashTagsViewState.LoadingState(true))
        }.doAfterTerminate {
            hashTagsStateSubject.onNext(HashTagsViewState.LoadingState(false))
            isLoadingH = false
        }.subscribeOnIoAndObserveOnMainThread({
            hashTagsStateSubject.onNext(HashTagsViewState.LoadingState(false))
            if (it.status) {
                if (pageNoH == 1) {
                    if (it.result?.listOfPosts?.size ?: 0 > 0) {
                        listOfFollowingH = it.result?.listOfPosts ?: arrayListOf()
                        hashTagsStateSubject.onNext(
                            HashTagsViewState.HashtagList(
                                listOfFollowingH
                            )
                        )
                    } else {
                        listOfFollowingH.clear()
                        hashTagsStateSubject.onNext(HashTagsViewState.EmptyState)
                    }
                } else {
                    if (it.result != null) {
                        it.result.listOfPosts?.let { it1 -> listOfFollowingH.addAll(it1) }
                        hashTagsStateSubject.onNext(
                            HashTagsViewState.HashtagList(
                                listOfFollowingH
                            )
                        )
                    } else {
                        isLoadMoreH = false
                    }
                }

            } else {
                hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it))
            }

        }).autoDispose()
    }

    fun loadMoreFollowing(search: String? = null) {
        if (!isLoadingH) {
            isLoadingH = true
            if (isLoadMoreH) {
                pageNoH += 1
                getHashtagUsers(search)
            }
        }
    }

    fun resetPaginationForFollowing(search: String? = null) {
        pageNoH = 1
        isLoadingH = false
        isLoadMoreH = true
        getHashtagUsers(search)
    }


    fun blockHashtag(reportHashtagRequest: ReportHashtagRequest) {
        hashtagRepository.blockHashtagInfo(reportHashtagRequest)
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    it.data?.let { reportRes ->
                        hashTagsStateSubject.onNext(
                            HashTagsViewState.BlockHashtag(
                                reportRes
                            )
                        )
                    }
                } else {
                    hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun reportHashtag(reportHashtagRequest: ReportHashtagRequest) {
        hashtagRepository.reportHashtagInfo(reportHashtagRequest)
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    it.data?.let { reportRes ->
                        hashTagsStateSubject.onNext(
                            HashTagsViewState.ReportHashtag(
                                reportRes
                            )
                        )
                    }
                } else {
                    hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    hashTagsStateSubject.onNext(HashTagsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class HashTagsViewState {
    data class ErrorMessage(val errorMessage: String) : HashTagsViewState()
    data class SuccessMessage(val successMessage: String) : HashTagsViewState()
    data class LoadingState(val isLoading: Boolean) : HashTagsViewState()
    data class HashtagResponses(val hashtagResponse: HashtagResponse?) : HashTagsViewState()
    data class HashtagList(val hashtagResponse: ArrayList<HashtagResponse>) : HashTagsViewState()
    data class ReportHashtag(val hashtagResponse: ReportHashtagResponse) : HashTagsViewState()
    data class BlockHashtag(val hashtagResponse: ReportHashtagResponse) : HashTagsViewState()
    object EmptyState : HashTagsViewState()

}