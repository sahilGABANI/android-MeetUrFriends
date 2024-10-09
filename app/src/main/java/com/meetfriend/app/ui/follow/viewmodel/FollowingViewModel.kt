package com.meetfriend.app.ui.follow.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.follow.model.GetFollowingRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FollowingViewModel(private val followRepository: FollowRepository) : BasicViewModel() {

    private val followingStateSubjects: PublishSubject<FollowingState> = PublishSubject.create()
    val followingStates: Observable<FollowingState> = followingStateSubjects.hide()

    private var perPage = 15
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfFollowing: MutableList<MeetFriendUser> = mutableListOf()

    fun getFollowing(userId: Int, search: String? = null) {
        followRepository.getFollowing(
            GetFollowingRequest(
                page = pageNo,
                userId = userId,
                search = search,
                perPage = perPage
            )
        ).doOnSubscribe {
            followingStateSubjects.onNext(FollowingState.LoadingState(true))
        }.doAfterTerminate {
            followingStateSubjects.onNext(FollowingState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (pageNo == 1) {
                    if (!it.result?.data.isNullOrEmpty()) {
                        listOfFollowing = it.result?.data?.toMutableList() ?: mutableListOf()
                        followingStateSubjects.onNext(
                            FollowingState.FollowingData(
                                listOfFollowing
                            )
                        )
                    } else {
                        followingStateSubjects.onNext(FollowingState.EmptyState)
                    }
                } else {
                    if (it.result?.data != null) {
                        listOfFollowing.addAll(it.result.data)
                        followingStateSubjects.onNext(
                            FollowingState.FollowingData(
                                listOfFollowing
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }

            } else {
                followingStateSubjects.onNext(FollowingState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                followingStateSubjects.onNext(FollowingState.ErrorMessage(it))
            }

        }).autoDispose()
    }

     fun loadMoreFollowing(userId: Int, search: String? = null) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getFollowing(userId,search)
            }
        }
    }

    fun resetPaginationForFollowing(userId: Int, search: String? = null) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getFollowing(userId,search)
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                } else {
                    followingStateSubjects.onNext(FollowingState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    followingStateSubjects.onNext(FollowingState.ErrorMessage(it))
                }

            }).autoDispose()
    }


    private var perPageH = 15
    private var pageNoH = 1
    private var isLoadingH = false
    private var isLoadMoreH = true

    private var listOfFollowingH: MutableList<MeetFriendUser> = mutableListOf()

    fun getHashtagUsers(search: String? = null) {
        followRepository.getHashtagUsers(
            page = pageNoH,
            perPage = perPageH,
            search = search
        ).doOnSubscribe {
            followingStateSubjects.onNext(FollowingState.LoadingState(true))
        }.doAfterTerminate {
            followingStateSubjects.onNext(FollowingState.LoadingState(false))
            isLoadingH = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (pageNoH == 1) {
                    if (!it.result?.data.isNullOrEmpty()) {
                        listOfFollowingH = it.result?.data?.toMutableList() ?: mutableListOf()
                        followingStateSubjects.onNext(
                            FollowingState.FollowingData(
                                listOfFollowingH
                            )
                        )
                    } else {
                        followingStateSubjects.onNext(FollowingState.EmptyState)
                    }
                } else {
                    if (it.result?.data != null) {
                        listOfFollowingH.addAll(it.result.data)
                        followingStateSubjects.onNext(
                            FollowingState.FollowingData(
                                listOfFollowingH
                            )
                        )
                    } else {
                        isLoadMoreH = false
                    }
                }

            } else {
                followingStateSubjects.onNext(FollowingState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                followingStateSubjects.onNext(FollowingState.ErrorMessage(it))
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

}

sealed class FollowingState {
    data class ErrorMessage(val errorMessage: String) : FollowingState()
    data class SuccessMessage(val successMessage: String) : FollowingState()
    data class LoadingState(val isLoading: Boolean) : FollowingState()
    data class FollowingData(val followingData: List<MeetFriendUser>?) : FollowingState()
    object EmptyState : FollowingState()
}