package com.meetfriend.app.ui.search.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.search.SearchRepository
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SearchListViewModel(
    private val searchRepository: SearchRepository,
    private val followRepository: FollowRepository
) : BasicViewModel() {

    private val searchUserStateSubject: PublishSubject<SearchUserViewState> =
        PublishSubject.create()
    val searchUserState: Observable<SearchUserViewState> = searchUserStateSubject.hide()

    private var perPage = 12
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfFollowers: ArrayList<MeetFriendUser> = arrayListOf()

    fun loadMoreFollowers(search: String) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                searchUsers(search)
            }
        }
    }

    fun resetPaginationForFollowers(search: String) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        listOfFollowers.clear()
        searchUsers(search)
    }

    fun searchUsers(search: String) {
        searchRepository.searchUsers(search, pageNo, perPage)
            .doOnSubscribe {
                searchUserStateSubject.onNext(SearchUserViewState.LoadingState(true))
            }
            .doAfterSuccess {
                searchUserStateSubject.onNext(SearchUserViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                searchUserStateSubject.onNext(SearchUserViewState.LoadingState(false))

                if (pageNo == 1) {
                    listOfFollowers.clear()
                }
                response.result?.data?.let {
                    listOfFollowers.addAll(it)

                    if (it.size == 0) {
                        isLoadMore = false
                    }
                    searchUserStateSubject.onNext(SearchUserViewState.SearchUserList(listOfFollowers))
                }

            }, { throwable ->
                throwable.localizedMessage?.let {
                    searchUserStateSubject.onNext(SearchUserViewState.LoadingState(false))
                    searchUserStateSubject.onNext(SearchUserViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (it.status) {
                } else {
                    searchUserStateSubject.onNext(SearchUserViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    searchUserStateSubject.onNext(SearchUserViewState.ErrorMessage(it))
                }

            }).autoDispose()
    }

    sealed class SearchUserViewState {
        data class ErrorMessage(val errorMessage: String) : SearchUserViewState()
        data class SuccessMessage(val successMessage: String) : SearchUserViewState()
        data class LoadingState(val isLoading: Boolean) : SearchUserViewState()
        data class SearchUserList(val searchUserList: ArrayList<MeetFriendUser>) :
            SearchUserViewState()
    }
}