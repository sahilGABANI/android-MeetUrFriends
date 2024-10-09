package com.meetfriend.app.ui.challenge.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.challenge.ChallengeRepository
import com.meetfriend.app.api.challenge.model.*
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.MentionUserRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ChallengeCommentViewModel(
    private val challengeRepository: ChallengeRepository,
    private val postRepository: PostRepository
) :
    BasicViewModel() {

    private val challengeCommentStateSubject: PublishSubject<ChallengeCommentViewState> =
        PublishSubject.create()
    val challengeCommentState: Observable<ChallengeCommentViewState> =
        challengeCommentStateSubject.hide()

    private val perPageForChallengeComment = 10
    private var pageNoForChallengeComment = 1
    private var isLoadingForChallengeComment = false
    private var isLoadMoreForChallengeComment = true
    private var listOfChallengeComment: MutableList<ChallengeComment> = mutableListOf()

    private fun getChallengeCommentList(challengeId: Int) {
        challengeRepository.getChallengeCommentList(
            page = pageNoForChallengeComment,
            perPage = perPageForChallengeComment,
            challengeId = challengeId
        ).doOnSubscribe {
            challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(true))
        }.doAfterSuccess {
            challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
            isLoadingForChallengeComment = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoadingForChallengeComment = false
            response?.let {
                if (response.status) {
                    if (pageNoForChallengeComment == 1) {
                        if (response.result.data != null && response.result.data.isNotEmpty()) {
                            listOfChallengeComment = response.result.data.toMutableList()
                            challengeCommentStateSubject.onNext(
                                ChallengeCommentViewState.ListOfChallengeComment(
                                    listOfChallengeComment
                                )
                            )
                        } else {
                            challengeCommentStateSubject.onNext(
                                ChallengeCommentViewState.EmptyState(
                                    "no Challenge for You"
                                )
                            )
                        }
                    } else {
                        if (response.result.data != null) {
                            listOfChallengeComment.addAll(response.result.data)
                            challengeCommentStateSubject.onNext(
                                ChallengeCommentViewState.ListOfChallengeComment(
                                    listOfChallengeComment
                                )
                            )
                        } else {
                            isLoadMoreForChallengeComment = false
                        }
                    }
                } else {
                    response.message.let {
                        challengeCommentStateSubject.onNext(
                            ChallengeCommentViewState.ErrorMessage(
                                response.message
                            )
                        )
                    }
                }
            }

        }, { throwable ->
            challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
            }
        }).autoDispose()

    }

    fun loadMoreChallengeComment(challengeId: Int) {
        if (!isLoadingForChallengeComment) {
            isLoadingForChallengeComment = true
            if (isLoadMoreForChallengeComment) {
                pageNoForChallengeComment += 1
                getChallengeCommentList(challengeId)
            }
        }
    }

    fun resetPaginationChallengeComment(challengeId: Int) {
        pageNoForChallengeComment = 1
        isLoadingForChallengeComment = false
        isLoadMoreForChallengeComment = true
        getChallengeCommentList(challengeId)
    }

    fun challengeCommentLikeUnLike(challengeCountRequest: ChallengeCommentRequest) {
        challengeRepository.challengeCommentLikeUnLike(createChallengeRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun deleteChallengeComment(challengeCountRequest: ChallengeCommentRequest) {
        challengeRepository.deleteChallengeComment(createChallengeRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                challengeCommentStateSubject.onNext(
                    ChallengeCommentViewState.SuccessDeleteMessage(
                        response.message ?: "",
                        challengeCountRequest
                    )
                )
            }, { throwable ->
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun updateChallengeComment(challengeUpdateCommentRequest: ChallengeUpdateCommentRequest) {
        var userList: ArrayList<Int> = arrayListOf()
        selectedTagUserInfo.forEach {
            userList.add(it.id)
        }
        challengeUpdateCommentRequest.mentionIds = userList.joinToString(",")
        challengeRepository.updateChallengeComment(challengeUpdateCommentRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.result?.let {
                    challengeCommentStateSubject.onNext(
                        ChallengeCommentViewState.UpdateCommentMessage(
                            response.result
                        )
                    )
                }
            }, { throwable ->
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun addChallengeComment(challengeCountRequest: AddChallengeCommentRequest) {
        var userList: ArrayList<Int> = arrayListOf()
        selectedTagUserInfo.forEach {
            userList.add(it.id)
        }
        challengeCountRequest.mentionIds = userList.joinToString(",")
        challengeRepository.addChallengeComment(createChallengeRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (challengeCountRequest.parentId != null)
                    challengeCommentStateSubject.onNext(
                        ChallengeCommentViewState.AddChallengeCommentReplay(
                            response.result
                        )
                    )
                else
                    challengeCommentStateSubject.onNext(
                        ChallengeCommentViewState.AddChallengeComment(
                            response.result
                        )
                    )
            }, { throwable ->
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun getUserForMention(searchText: String) {
        postRepository.mentionUser(MentionUserRequest(searchText))
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.let {
                    challengeCommentStateSubject.onNext(
                        ChallengeCommentViewState.UserListForMention(
                            it.result
                        )
                    )
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

    private var selectedTagUserInfo: MutableList<MeetFriendUser> = mutableListOf()
    fun searchUserClicked(
        initialDescriptionString: String,
        subString: String,
        user: MeetFriendUser,
    ) {
        if (user !in selectedTagUserInfo) {
            selectedTagUserInfo.add(user)
        }
        val remainString = initialDescriptionString.removePrefix(subString)
        if (subString.length == initialDescriptionString.length) {
            val lastIndexOfToken =
                initialDescriptionString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString@${user.userName}"
            challengeCommentStateSubject.onNext(
                ChallengeCommentViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${user.userName} $remainString"
            challengeCommentStateSubject.onNext(
                ChallengeCommentViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        }
    }

    private val perPageForChallengePostComment = 10
    private var pageNoForChallengePostComment = 1
    private var isLoadingForChallengePostComment = false
    private var isLoadMoreForChallengePostComment = true
    private var listOfChallengePostComment: MutableList<ChallengeComment> = mutableListOf()

    private fun getChallengePostCommentList(challengeId: Int) {
        challengeRepository.getChallengePostCommentList(
            page = pageNoForChallengePostComment,
            perPage = perPageForChallengePostComment,
            challengeId = challengeId
        ).doOnSubscribe {
            challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(true))
        }.doAfterSuccess {
            challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
            isLoadingForChallengePostComment = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoadingForChallengePostComment = false
            response?.let {
                if (response.status) {
                    if (pageNoForChallengePostComment == 1) {
                        if (response.result.data != null && response.result.data.isNotEmpty()) {
                            listOfChallengePostComment = response.result.data.toMutableList()
                            challengeCommentStateSubject.onNext(
                                ChallengeCommentViewState.ListOfChallengeComment(
                                    listOfChallengePostComment
                                )
                            )
                        } else {
                            challengeCommentStateSubject.onNext(
                                ChallengeCommentViewState.EmptyState(
                                    "no Challenge for You"
                                )
                            )
                        }
                    } else {
                        if (response.result.data != null) {
                            listOfChallengePostComment.addAll(response.result.data)
                            challengeCommentStateSubject.onNext(
                                ChallengeCommentViewState.ListOfChallengeComment(
                                    listOfChallengePostComment
                                )
                            )
                        } else {
                            isLoadMoreForChallengePostComment = false
                        }
                    }
                } else {
                    response.message.let {
                        challengeCommentStateSubject.onNext(
                            ChallengeCommentViewState.ErrorMessage(
                                response.message
                            )
                        )
                    }
                }
            }

        }, { throwable ->
            challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun loadMoreChallengePostComment(challengeId: Int) {
        if (!isLoadingForChallengePostComment) {
            isLoadingForChallengePostComment = true
            if (isLoadMoreForChallengePostComment) {
                pageNoForChallengePostComment += 1
                getChallengePostCommentList(challengeId)
            }
        }
    }

    fun resetPaginationChallengePostComment(challengeId: Int) {
        pageNoForChallengePostComment = 1
        isLoadingForChallengePostComment = false
        isLoadMoreForChallengePostComment = true
        getChallengePostCommentList(challengeId)
    }


    fun updateChallengePostComment(challengeUpdateCommentRequest: ChallengeUpdateCommentRequest) {
        challengeRepository.updateChallengePostComment(challengeUpdateCommentRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.result.let {
                    challengeCommentStateSubject.onNext(
                        ChallengeCommentViewState.UpdateCommentMessage(
                            it
                        )
                    )
                }

            }, { throwable ->
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun addChallengePostComment(challengeCountRequest: AddChallengePostCommentRequest) {
        var userList: ArrayList<String> = arrayListOf()
        selectedTagUserInfo.forEach {
            userList.add(it.id.toString())
        }
        challengeCountRequest.mentionIds = userList.joinToString(",")

        challengeRepository.addChallengePostComment(createChallengeRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (challengeCountRequest.parentId != null)
                    challengeCommentStateSubject.onNext(
                        ChallengeCommentViewState.AddChallengeCommentReplay(
                            response.result
                        )
                    )
                else
                    challengeCommentStateSubject.onNext(
                        ChallengeCommentViewState.AddChallengeComment(
                            response.result
                        )
                    )

            }, { throwable ->
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun challengePostCommentLikeUnLike(challengeCountRequest: ChallengePostCommentRequest) {
        challengeRepository.challengePostCommentLikeUnLike(createChallengeRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }

    fun deleteChallengePostComment(challengeCountRequest: ChallengePostDeleteCommentRequest) {
        challengeRepository.deleteChallengePostComment(createChallengeRequest = challengeCountRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                challengeCommentStateSubject.onNext(
                    ChallengeCommentViewState.SuccessDeletePostMessage(
                        response.message ?: "",
                        challengeCountRequest
                    )
                )
            }, { throwable ->
                challengeCommentStateSubject.onNext(ChallengeCommentViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    challengeCommentStateSubject.onNext(ChallengeCommentViewState.ErrorMessage(it))
                }
            }).autoDispose()

    }
}

sealed class ChallengeCommentViewState {
    data class ErrorMessage(val errorMessage: String) : ChallengeCommentViewState()
    data class SuccessMessage(val successMessage: String) : ChallengeCommentViewState()
    data class LoadingState(val isLoading: Boolean) : ChallengeCommentViewState()
    data class EmptyState(val message: String) : ChallengeCommentViewState()
    data class SuccessDeleteMessage(
        val successMessage: String,
        val challengeCountRequest: ChallengeCommentRequest
    ) : ChallengeCommentViewState()

    data class SuccessDeletePostMessage(
        val successMessage: String,
        val challengeCountRequest: ChallengePostDeleteCommentRequest
    ) : ChallengeCommentViewState()

    data class UpdateCommentMessage(val challengeComment: ChallengeComment) :
        ChallengeCommentViewState()

    data class ListOfChallengeComment(val listOfChallengeComment: List<ChallengeComment>) :
        ChallengeCommentViewState()

    data class AddChallengeComment(val listOfChallengeComment: ChallengeComment) :
        ChallengeCommentViewState()

    data class AddChallengeCommentReplay(val listOfChallengeComment: ChallengeComment) :
        ChallengeCommentViewState()

    data class UserListForMention(val listOfUserForMention: List<MeetFriendUser>?) :
        ChallengeCommentViewState()

    data class UpdateDescriptionText(val descriptionString: String) : ChallengeCommentViewState()

}