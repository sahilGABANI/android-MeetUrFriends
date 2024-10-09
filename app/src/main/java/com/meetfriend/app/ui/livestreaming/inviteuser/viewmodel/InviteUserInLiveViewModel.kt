package com.meetfriend.app.ui.livestreaming.inviteuser.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.follow.model.GetUserForLiveInviteRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.utils.Constant
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class InviteUserInLiveViewModel(private val followRepository: FollowRepository) : BasicViewModel() {
    private val followerStateSubjects: PublishSubject<InviteUserInLiveState> =
        PublishSubject.create()
    val followerStates: Observable<InviteUserInLiveState> = followerStateSubjects.hide()

    private var perPage = Constant.FiXED_12_INT
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfFollowers: MutableList<MeetFriendUser> = mutableListOf()

    fun getUserForLiveInvite() {
        followRepository.getUserForLiveInvite(
            GetUserForLiveInviteRequest(
                page = pageNo,
                perPage = perPage
            )
        ).doOnSubscribe {
            followerStateSubjects.onNext(InviteUserInLiveState.LoadingState(true))
        }.doAfterTerminate {
            followerStateSubjects.onNext(InviteUserInLiveState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (pageNo == 1) {
                    if (it.result?.data != null) {
                        listOfFollowers = it.result.data.toMutableList()
                        followerStateSubjects.onNext(
                            InviteUserInLiveState.FollowersData(
                                listOfFollowers
                            )
                        )
                    } else {
                        followerStateSubjects.onNext(InviteUserInLiveState.EmptyState)
                    }
                } else {
                    if (!it.result?.data.isNullOrEmpty()) {
                        it.result?.data?.let { it1 -> listOfFollowers.addAll(it1) }
                        followerStateSubjects.onNext(
                            InviteUserInLiveState.FollowersData(
                                listOfFollowers
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }
            } else {
                followerStateSubjects.onNext(InviteUserInLiveState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                followerStateSubjects.onNext(InviteUserInLiveState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun loadMoreInviteUser() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getUserForLiveInvite()
            }
        }
    }

    fun resetPaginationForInviteUser() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getUserForLiveInvite()
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (!it.status) {
                    followerStateSubjects.onNext(InviteUserInLiveState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    followerStateSubjects.onNext(InviteUserInLiveState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class InviteUserInLiveState {
    data class ErrorMessage(val errorMessage: String) : InviteUserInLiveState()
    data class SuccessMessage(val successMessage: String) : InviteUserInLiveState()
    data class LoadingState(val isLoading: Boolean) : InviteUserInLiveState()
    data class FollowersData(val followingData: List<MeetFriendUser>?) : InviteUserInLiveState()
    object EmptyState : InviteUserInLiveState()
}
