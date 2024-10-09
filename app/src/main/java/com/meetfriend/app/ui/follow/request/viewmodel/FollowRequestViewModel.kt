package com.meetfriend.app.ui.follow.request.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.AcceptFollowRequestRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FollowRequestViewModel(private val followRepository: FollowRepository) : BasicViewModel() {

    private val followRequestStateSubjects: PublishSubject<FollowRequestState> =
        PublishSubject.create()
    val followRequestStates: Observable<FollowRequestState> = followRequestStateSubjects.hide()

    private var listOfFollowRequest: MutableList<MeetFriendUser> = mutableListOf()

    private var perPage = 12
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private fun getFollowRequest() {
        followRepository.getFollowRequests(pageNo, perPage).doOnSubscribe {
            followRequestStateSubjects.onNext(FollowRequestState.LoadingState(true))
        }.doAfterTerminate {
            followRequestStateSubjects.onNext(FollowRequestState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (pageNo == 1) {
                    if (it.result?.data != null) {
                        listOfFollowRequest = it.result.data.toMutableList()
                        followRequestStateSubjects.onNext(
                            FollowRequestState.FollowRequestsData(
                                listOfFollowRequest
                            )
                        )
                    } else {
                        followRequestStateSubjects.onNext(FollowRequestState.EmptyState)
                    }
                } else {
                    if (it.result?.data != null) {
                        listOfFollowRequest.addAll(it.result.data)
                        followRequestStateSubjects.onNext(
                            FollowRequestState.FollowRequestsData(
                                listOfFollowRequest
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }
            } else {
                followRequestStateSubjects.onNext(FollowRequestState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                followRequestStateSubjects.onNext(FollowRequestState.ErrorMessage(it))
            }

        }).autoDispose()
    }

    fun loadMoreFollowRequest() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getFollowRequest()
            }
        }
    }

    fun resetPaginationForFollowRequest() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getFollowRequest()
    }

    fun acceptFollowRequest(followRequestId: Int) {
        followRepository.acceptFollowRequest(AcceptFollowRequestRequest(followRequestId))
            .doOnSubscribe {
            }.doAfterTerminate {
            }.subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    followRequestStateSubjects.onNext(FollowRequestState.SuccessMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    followRequestStateSubjects.onNext(FollowRequestState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun rejectFollowRequest(followRequestId: Int) {
        followRepository.rejectFollowRequest(AcceptFollowRequestRequest(followRequestId))
            .doOnSubscribe {
            }.doAfterTerminate {
            }.subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                    followRequestStateSubjects.onNext(FollowRequestState.SuccessMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    followRequestStateSubjects.onNext(FollowRequestState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class FollowRequestState {
    data class ErrorMessage(val errorMessage: String) : FollowRequestState()
    data class SuccessMessage(val successMessage: String) : FollowRequestState()
    data class LoadingState(val isLoading: Boolean) : FollowRequestState()
    data class FollowRequestsData(val requestData: MutableList<MeetFriendUser>?) :
        FollowRequestState()

    object EmptyState : FollowRequestState()
}