package com.meetfriend.app.ui.follow.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SuggestedUserViewModel(private val followRepository: FollowRepository) : BasicViewModel() {
    private val suggestedUserStateSubjects: PublishSubject<SuggestedUserViewState> =
        PublishSubject.create()
    val suggestedUserStates: Observable<SuggestedUserViewState> = suggestedUserStateSubjects.hide()
    private var perPage = 15
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfSuggestedUser: MutableList<MeetFriendUser> = mutableListOf()

    private fun getSuggestedUsers(search:String) {
        followRepository.getSuggestedUsers(search,pageNo, perPage).doOnSubscribe {
            suggestedUserStateSubjects.onNext(SuggestedUserViewState.LoadingState(true))
        }.doAfterTerminate {
            suggestedUserStateSubjects.onNext(SuggestedUserViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                suggestedUserStateSubjects.onNext(SuggestedUserViewState.SuggestedUserData(it.result?.data))

                if (pageNo == 1) {
                    if (it.result?.data != null) {
                        listOfSuggestedUser = it.result.data.toMutableList()
                        suggestedUserStateSubjects.onNext(
                            SuggestedUserViewState.SuggestedUserData(
                                listOfSuggestedUser
                            )
                        )
                    } else {
                        suggestedUserStateSubjects.onNext(SuggestedUserViewState.EmptyState)
                    }
                } else {
                    if (it.result?.data != null) {
                        listOfSuggestedUser.addAll(it.result.data)
                        suggestedUserStateSubjects.onNext(
                            SuggestedUserViewState.SuggestedUserData(
                                listOfSuggestedUser
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }
            } else {
                suggestedUserStateSubjects.onNext(SuggestedUserViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                suggestedUserStateSubjects.onNext(SuggestedUserViewState.ErrorMessage(it))
            }

        }).autoDispose()
    }


    fun loadMoreSuggestedUser(search: String) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getSuggestedUsers(search)
            }
        }
    }

    fun resetPaginationForSuggestedUser(search: String) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getSuggestedUsers(search)
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                } else {
                    suggestedUserStateSubjects.onNext(SuggestedUserViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    suggestedUserStateSubjects.onNext(SuggestedUserViewState.ErrorMessage(it))
                }

            }).autoDispose()
    }

    fun cancelFriendRequest(friendId: Int) {
        followRepository.cancelFriendRequest(friendId)
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                } else {
                    suggestedUserStateSubjects.onNext(SuggestedUserViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    suggestedUserStateSubjects.onNext(SuggestedUserViewState.ErrorMessage(it))
                }

            }).autoDispose()
    }

}

sealed class SuggestedUserViewState {
    data class LoadingState(val isLoading: Boolean) : SuggestedUserViewState()
    data class SuccessMessage(val successMessage: String) : SuggestedUserViewState()
    data class ErrorMessage(val errorMessage: String) : SuggestedUserViewState()
    data class SuggestedUserData(val suggestedUserData: List<MeetFriendUser>?) :
        SuggestedUserViewState()

    object EmptyState : SuggestedUserViewState()
}