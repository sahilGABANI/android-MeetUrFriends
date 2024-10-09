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

class ChallengePostCommentViewModel(
    private val challengeRepository: ChallengeRepository,
    private val postRepository: PostRepository
) :
    BasicViewModel() {

    private val challengePostCommentStateSubject: PublishSubject<ChallengePostCommentViewState> =
        PublishSubject.create()


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
            challengePostCommentStateSubject.onNext(ChallengePostCommentViewState.LoadingState(true))
        }.doAfterSuccess {
            challengePostCommentStateSubject.onNext(ChallengePostCommentViewState.LoadingState(false))
            isLoadingForChallengePostComment = false
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoadingForChallengePostComment = false
            response?.let {
                if (response.status) {
                    if (pageNoForChallengePostComment == 1) {
                        if (response.result.data != null && response.result.data.isNotEmpty()) {
                            listOfChallengePostComment = response.result.data.toMutableList()
                            challengePostCommentStateSubject.onNext(
                                ChallengePostCommentViewState.ListOfChallengeComment(
                                    listOfChallengePostComment
                                )
                            )
                        } else {
                            challengePostCommentStateSubject.onNext(
                                ChallengePostCommentViewState.EmptyState(
                                    "no Challenge for You"
                                )
                            )
                        }
                    } else {
                        if (response.result.data != null) {
                            listOfChallengePostComment.addAll(response.result.data)
                            challengePostCommentStateSubject.onNext(
                                ChallengePostCommentViewState.ListOfChallengeComment(
                                    listOfChallengePostComment
                                )
                            )
                        } else {
                            isLoadMoreForChallengePostComment = false
                        }
                    }
                } else {
                    response.message.let {
                        challengePostCommentStateSubject.onNext(
                            ChallengePostCommentViewState.ErrorMessage(
                                response.message
                            )
                        )
                    }
                }
            }

        }, { throwable ->
            challengePostCommentStateSubject.onNext(ChallengePostCommentViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                challengePostCommentStateSubject.onNext(
                    ChallengePostCommentViewState.ErrorMessage(
                        it
                    )
                )
            }
        }).autoDispose()

    }

    fun getUserForMention(searchText: String) {
        postRepository.mentionUser(MentionUserRequest(searchText))
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.let {
                    challengePostCommentStateSubject.onNext(
                        ChallengePostCommentViewState.UserListForMention(
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
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString@${user.userName}"
            challengePostCommentStateSubject.onNext(
                ChallengePostCommentViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${user.userName} $remainString"
            challengePostCommentStateSubject.onNext(
                ChallengePostCommentViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        }
    }

}


sealed class ChallengePostCommentViewState {

    data class ErrorMessage(val errorMessage: String) : ChallengePostCommentViewState()
    data class SuccessMessage(val successMessage: String) : ChallengePostCommentViewState()
    data class LoadingState(val isLoading: Boolean) : ChallengePostCommentViewState()
    data class EmptyState(val message: String) : ChallengePostCommentViewState()
    data class ListOfChallengeComment(val listOfChallengeComment: List<ChallengeComment>) :
        ChallengePostCommentViewState()
    data class UserListForMention(val listOfUserForMention: List<MeetFriendUser>?) :
        ChallengePostCommentViewState()
    data class UpdateDescriptionText(val descriptionString: String) :
        ChallengePostCommentViewState()

}