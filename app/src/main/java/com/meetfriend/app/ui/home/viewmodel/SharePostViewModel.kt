package com.meetfriend.app.ui.home.viewmodel

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.MentionUserRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class SharePostViewModel(private val postRepository: PostRepository) : BasicViewModel() {

    private val sharePostStateSubject: PublishSubject<SharePostViewState> = PublishSubject.create()
    val sharePostState: Observable<SharePostViewState> = sharePostStateSubject.hide()

    private var selectedTagUserInfo: MutableList<MeetFriendUser> = mutableListOf()


    fun getUserForMention(searchText: String) {
        postRepository.mentionUser(MentionUserRequest(searchText))
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.let {
                    sharePostStateSubject.onNext(SharePostViewState.UserListForMention(it.result))
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
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString@${user.userName}"
            sharePostStateSubject.onNext(
                SharePostViewState.UpdateDescriptionText(
                    descriptionString.plus(
                        " "
                    )
                )
            )
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("@"))?.first ?: return
            Timber.i("lastIndexOfToken %s", lastIndexOfToken)
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString @${user.userName} $remainString"
            sharePostStateSubject.onNext(
                SharePostViewState.UpdateDescriptionText(
                    descriptionString.plus(
                        " "
                    )
                )
            )
        }
    }
}

sealed class SharePostViewState {
    data class ErrorMessage(val errorMessage: String) : SharePostViewState()
    data class SuccessMessage(val successMessage: String) : SharePostViewState()
    data class LoadingState(val isLoading: Boolean) : SharePostViewState()
    data class UserListForMention(val listOfUserForMention: List<MeetFriendUser>?) :
        SharePostViewState()

    data class UpdateDescriptionText(val descriptionString: String) : SharePostViewState()


}