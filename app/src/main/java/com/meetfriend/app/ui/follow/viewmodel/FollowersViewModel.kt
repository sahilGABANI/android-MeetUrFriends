package com.meetfriend.app.ui.follow.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.follow.model.GetFollowersRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FollowersViewModel(private val followRepository: FollowRepository) : BasicViewModel() {
    private val followerStateSubjects: PublishSubject<FollowersState> = PublishSubject.create()
    val followerStates: Observable<FollowersState> = followerStateSubjects.hide()

    private var perPage = 15
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfFollowers: MutableList<MeetFriendUser> = mutableListOf()

    fun getFollowers(userId: Int , search :String? =null) {
        followRepository.getFollowers(
            GetFollowersRequest(
                page = pageNo,
                userId = userId,
                search= search,
                perPage = perPage
            )
        ).doOnSubscribe {
            followerStateSubjects.onNext(FollowersState.LoadingState(true))
        }.doAfterTerminate {
            followerStateSubjects.onNext(FollowersState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            followerStateSubjects.onNext(FollowersState.LoadingState(false))
            if (it.status) {
                if (pageNo == 1) {
                    if (!it.result?.data.isNullOrEmpty()) {
                        listOfFollowers = it.result?.data?.toMutableList() ?: mutableListOf()
                        followerStateSubjects.onNext(
                            FollowersState.FollowersData(
                                listOfFollowers
                            )
                        )
                    } else {
                        followerStateSubjects.onNext(FollowersState.EmptyState)
                    }
                } else {
                    if (it.result?.data != null) {
                        listOfFollowers.addAll(it.result.data)
                        followerStateSubjects.onNext(
                            FollowersState.FollowersData(
                                listOfFollowers
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }
            } else {
                followerStateSubjects.onNext(FollowersState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                followerStateSubjects.onNext(FollowersState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun loadMoreFollowers(userId: Int, search :String) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getFollowers(userId,search)
            }
        }
    }

    fun resetPaginationForFollowers(userId: Int, search :String? =null) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getFollowers(userId,search)
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                } else {
                    followerStateSubjects.onNext(FollowersState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    followerStateSubjects.onNext(FollowersState.ErrorMessage(it))
                }

            }).autoDispose()
    }
}

sealed class FollowersState {
    data class ErrorMessage(val errorMessage: String) : FollowersState()
    data class SuccessMessage(val successMessage: String) : FollowersState()
    data class LoadingState(val isLoading: Boolean) : FollowersState()
    data class FollowersData(val followingData: List<MeetFriendUser>?) : FollowersState()
    object EmptyState : FollowersState()
}