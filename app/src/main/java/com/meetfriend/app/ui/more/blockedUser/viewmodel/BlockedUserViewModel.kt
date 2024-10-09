package com.meetfriend.app.ui.more.blockedUser.viewmodel

import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.BlockInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BlockedUserViewModel(private val followRepository: FollowRepository) : BasicViewModel() {

    private val blockUserStateSubject: PublishSubject<BlockUserViewState> =
        PublishSubject.create()
    val blockUserState: Observable<BlockUserViewState> = blockUserStateSubject.hide()

    private var listOfBlockUser: MutableList<BlockInfo> = mutableListOf()


    private var pageNo = 1
    private var perPage = 15
    private var isLoading = false
    private var isLoadMore = true

    fun getBlockedUser() {
        followRepository.blockedUserList(perPage, pageNo).doOnSubscribe {
            blockUserStateSubject.onNext(BlockUserViewState.LoadingState(true))
        }.doAfterTerminate {
            blockUserStateSubject.onNext(BlockUserViewState.LoadingState(false))

        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (pageNo == 1) {
                    if (!it.result?.data.isNullOrEmpty()) {
                        listOfBlockUser = it.result?.data?.toMutableList() ?: mutableListOf()
                        blockUserStateSubject.onNext(
                            BlockUserViewState.BlockedUserData(
                                listOfBlockUser
                            )
                        )
                    } else {
                        blockUserStateSubject.onNext(BlockUserViewState.EmptyState)
                    }
                } else {
                    if (it.result?.data != null) {
                        listOfBlockUser.addAll(it.result.data)
                        blockUserStateSubject.onNext(
                            BlockUserViewState.BlockedUserData(
                                listOfBlockUser
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }

            } else {
                blockUserStateSubject.onNext(BlockUserViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                blockUserStateSubject.onNext(BlockUserViewState.ErrorMessage(it))
            }
        })
    }

    fun loadMoreBlockUser() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getBlockedUser()
            }
        }
    }

    fun resetPaginationForBlockUser() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getBlockedUser()
    }

    fun blockUnBlockUser(friendId: Int, blockStatus: String) {
        followRepository.blockUnBlockUser(friendId, blockStatus).doOnSubscribe {
            blockUserStateSubject.onNext(BlockUserViewState.LoadingState(true))
        }.doAfterTerminate {
            blockUserStateSubject.onNext(BlockUserViewState.LoadingState(false))

        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                blockUserStateSubject.onNext(BlockUserViewState.SuccessMessage(it.message.toString()))

            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                blockUserStateSubject.onNext(BlockUserViewState.ErrorMessage(it))
            }
        })
    }

}

sealed class BlockUserViewState {
    data class ErrorMessage(val errorMessage: String) : BlockUserViewState()
    data class SuccessMessage(val successMessage: String) : BlockUserViewState()
    data class LoadingState(val isLoading: Boolean) : BlockUserViewState()
    data class BlockedUserData(val blockedUserData: List<BlockInfo>) : BlockUserViewState()
    object EmptyState : BlockUserViewState()
}