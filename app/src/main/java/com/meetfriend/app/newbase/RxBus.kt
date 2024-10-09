package com.meetfriend.app.newbase

import android.net.Uri
import com.meetfriend.app.api.challenge.model.ChallengeType
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.api.livestreaming.model.LiveSummaryInfo
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.api.post.model.LinkAttachmentDetails
import com.meetfriend.app.responseclasses.video.Post
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object RxBus {

    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    // Listen should return an Observable and not the publisher
    // Using ofType we filter only events that match that class type
    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}

class RxEvent {
    data class UpdateChatRoom(
        val chatRoomInfo: ChatRoomInfo,
        val chatName: String,
        val startVC: Boolean,
        val fromProfile: Boolean
    )

    object DeleteChatRoom
    object UpdatedAdminSubscription
    object DeleteProfileImage
    object CreateChatRoom
    object SubscriptionSuccessFull
    data class CommentUpdate(val data: Post?)
    data class PostCommentUpdate(val data: Post?)
    data class PlayVideo(val isPlay: Boolean, val count: Int)
    data class PlayHashtagVideo(val isPlay: Boolean, val count: Int)
    data class PlayChallengeVideo(val isPlay: Boolean, val count: Int, val type: ChallengeType)
    object RefreshFoYouFragment
    object RefreshFoYouPlayFragment
    object RefreshForYouPlayFragment
    object RefreshPlayerView
    object RefreshFollowingPlayFragment
    object RefreshChallengeFragment
    object RefreshHashtagVideoFragment
    data class RefreshAllChallengeFragment(val type: ChallengeType)
    object DeclineCall
    data class UnreadMessageBadge(val count: Int?)
    data class ShowProgress(val isShow: Boolean)
    data class SearchTextUser(val search: String)
    data class SearchTextHashtags(val search: String)
    data class SearchCountryHashtags(val search: String)
    data class SearchTextPost(val search: String)
    data class SearchTextShorts(val search: String)
    data class SearchTextChallenges(val search: String)
    object MessageUnreadCount
    object ShortsEditedSuccessfully
    data class UpdateCanChat(val canSend: Boolean)
    data class UpdateLiveCoHost(val liveEventInfo: LiveEventInfo)
    data class DisplayCoHostRequestToHost(val liveEventInfo: LiveEventInfo)
    object RemoveCoHost
    data class DisplayLiveSummary(val liveSummaryInfo: LiveSummaryInfo)
    object CloseLive
    object CloseLiveAndStartVideoCall
    object StoryWatched
    object FinishHubRequest
    object PauseForYouShorts
    object PlayForYouShorts
    data class CreatePost(val postType: String, val filePath: String, val inputImage: Uri)

    data class RefreshGiftSummaryFragment(val startDate: String, val endDate: String)
    data class TotalEarning(val amount: String)

    data class ShowRedeemInfo(val status: String, val amount: String)

    data class ItemClick(val itemId: Int)

    // User Profile
    data class UserProfileLikeUnlike(val postId: Int, val postStatus: String)
    data class UserProfileShortDelete(val deleteShortId: Int)

    data class GoogleSignInResult(val result: com.google.android.gms.auth.api.signin.GoogleSignInResult?)
    data class StartVideo(val checkImage: Boolean)
    data class RefreshHomePagePostPlayVideo(val isVisible: Boolean)

    data class SwitchAccount(val userId: Int)

    data class CreateChallenge(val filePath: String, val linkAttachmentDetails: LinkAttachmentDetails? = null)
    data class UpdateProgressBar(val musicInfo: MusicInfo, val process: Int)

    companion object {
        fun cutCall(): DeclineCall {
            return DeclineCall
        }

        fun updateUnreadCount(): MessageUnreadCount {
            return MessageUnreadCount
        }

        fun coHostRequestForAttendee(liveEventInfo: LiveEventInfo): UpdateLiveCoHost {
            return UpdateLiveCoHost(liveEventInfo)
        }

        fun coHostRequestToHost(liveEventInfo: LiveEventInfo): DisplayCoHostRequestToHost {
            return DisplayCoHostRequestToHost(liveEventInfo)
        }

        fun redeemInfo(status: String, amount: String): ShowRedeemInfo {
            return ShowRedeemInfo(status, amount)
        }

        fun switchUserAccount(userId: Int): SwitchAccount {
            return SwitchAccount(userId)
        }
    }
}
