package com.meetfriend.app.ui.home.shorts.comment.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.comment.CommentRepository
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.MentionUserRequest
import com.meetfriend.app.api.post.model.PostCommentResponse
import com.meetfriend.app.api.post.model.ViewPostRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.getSelectedTagUserIds
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.responseclasses.video.Post
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ViewPostViewModel(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val followRepository: FollowRepository
) : BasicViewModel() {

    private val viewPostStateSubjects: PublishSubject<ViewPostViewState> = PublishSubject.create()
    val viewPostState: Observable<ViewPostViewState> = viewPostStateSubjects.hide()

    private var selectedTagUserInfo: MutableList<MeetFriendUser> = mutableListOf()

    fun viewPost(postId: String) {
        postRepository.viewPost(ViewPostRequest(postId, "posts"))
            .doOnSubscribe {
                viewPostStateSubjects.onNext(ViewPostViewState.LoadingState(true))
            }
            .doAfterSuccess {
                viewPostStateSubjects.onNext(ViewPostViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                viewPostStateSubjects.onNext(ViewPostViewState.LoadingState(false))
                if (response.status) {
                    viewPostStateSubjects.onNext(ViewPostViewState.PostData(response.result!!))
                }
            }, { throwable ->
                viewPostStateSubjects.onNext(ViewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    viewPostStateSubjects.onNext(ViewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun commentPost(postId: Int, content: String) {
        commentRepository.commentPost(
            postId,
            content,
            getSelectedTagUserIds(selectedTagUserInfo, content)
        )
            .subscribeOnIoAndObserveOnMainThread({ response ->
                viewPostStateSubjects.onNext(ViewPostViewState.CommentResponse(response))
            }, { throwable ->
                viewPostStateSubjects.onNext(ViewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    viewPostStateSubjects.onNext(ViewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
        selectedTagUserInfo.clear()
    }

    fun updateComment(postId: Int, content: String) {
        commentRepository.updateComment(
            postId,
            content,
            getSelectedTagUserIds(selectedTagUserInfo, content)
        )
            .subscribeOnIoAndObserveOnMainThread({ response ->
                viewPostStateSubjects.onNext(ViewPostViewState.UpdateCommentResponse(response))
            }, { throwable ->
                viewPostStateSubjects.onNext(ViewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    viewPostStateSubjects.onNext(ViewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
        selectedTagUserInfo.clear()
    }

    fun deleteComment(commentId: Int) {
        commentRepository.deleteComment(commentId)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                viewPostStateSubjects.onNext(ViewPostViewState.DeleteCommentMessage(response.message.toString()))
            }, { throwable ->
                viewPostStateSubjects.onNext(ViewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    viewPostStateSubjects.onNext(ViewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun commentReply(postId: Int, content: String, parentId: Int) {
        commentRepository.commentReply(
            postId,
            content,
            parentId,
            getSelectedTagUserIds(selectedTagUserInfo, content)
        )
            .subscribeOnIoAndObserveOnMainThread({ response ->
                viewPostStateSubjects.onNext(ViewPostViewState.CommentReplyResponse(response))
            }, { throwable ->
                viewPostStateSubjects.onNext(ViewPostViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    viewPostStateSubjects.onNext(ViewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
        selectedTagUserInfo.clear()
    }

    fun followUnfollow(userId: Int) {
        followRepository.followUnfollowUser(FollowUnfollowRequest(userId))
            .subscribeOnIoAndObserveOnMainThread({
                if (!it.status) {
                    viewPostStateSubjects.onNext(ViewPostViewState.ErrorMessage(it.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    viewPostStateSubjects.onNext(ViewPostViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getUserForMention(searchText: String) {
        postRepository.mentionUser(MentionUserRequest(searchText))
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.let {
                    viewPostStateSubjects.onNext(ViewPostViewState.UserListForMention(it.result))
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

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
            viewPostStateSubjects.onNext(
                ViewPostViewState.UpdateDescriptionText(
                    descriptionString.plus(
                        " "
                    )
                )
            )
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${user.userName} $remainString"
            viewPostStateSubjects.onNext(
                ViewPostViewState.UpdateDescriptionText(
                    descriptionString.plus(
                        " "
                    )
                )
            )
        }
    }
}

sealed class ViewPostViewState {
    data class ErrorMessage(val errorMessage: String) : ViewPostViewState()
    data class SuccessMessage(val successMessage: String) : ViewPostViewState()
    data class LoadingState(val isLoading: Boolean) : ViewPostViewState()
    data class PostData(val postData: Post) : ViewPostViewState()
    data class CommentResponse(val commentResponse: PostCommentResponse) : ViewPostViewState()
    data class UpdateCommentResponse(val commentResponse: PostCommentResponse) : ViewPostViewState()
    data class CommentReplyResponse(val commentResponse: PostCommentResponse) : ViewPostViewState()
    data class DeleteCommentMessage(val deleteCommentMessage: String) : ViewPostViewState()
    data class UserListForMention(val listOfUserForMention: List<MeetFriendUser>?) :
        ViewPostViewState()
    data class UpdateDescriptionText(val descriptionString: String) : ViewPostViewState()
}
