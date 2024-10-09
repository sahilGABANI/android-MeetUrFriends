package com.meetfriend.app.api.livestreaming.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.gift.model.GiftsItemInfo
import com.meetfriend.app.api.gift.model.User
import com.meetfriend.app.utils.Constant.FiXED_3600_INT
import com.meetfriend.app.utils.Constant.FiXED_60_INT
import kotlinx.parcelize.Parcelize

data class Time(
    val hours: Int,
    val min: Int,
    val second: Int,
)

fun Int.secondToTime(): Time {
    val h = (this / FiXED_3600_INT)
    val m = (this / FiXED_60_INT % FiXED_60_INT)
    val s = (this % FiXED_60_INT)
    return Time(h, m, s)
}

enum class LiveStreamNoOfCoHost(val type: String) {
    FirstCoHost("firstCoHost"),
    SecondCoHost("secondCoHost"),
    ThirdCoHost("thirdCoHost"),
    FourthCoHost("fourthCoHost")
}

data class CreateLiveEventRequest(
    @field:SerializedName("event_name")
    val eventName: String? = null,

    @field:SerializedName("is_lock")
    val isLock: Int? = null,

    @field:SerializedName("password")
    val password: String? = null,

    @field:SerializedName("invite_ids")
    val inviteIds: String? = null,

    @field:SerializedName("is_allow_play_game")
    val isAllowPlayGame: Int? = 0,
)

data class JoinLiveEventRequest(
    @field:SerializedName("live_id")
    val liveId: Int? = null,

    @field:SerializedName("role_type")
    val roleType: String = "RoleAttendee",
)

data class EndLiveEventRequest(
    @field:SerializedName("live_id")
    val liveId: Int? = null,
)

data class LiveEventWatchingUserRequest(
    @field:SerializedName("live_id")
    val liveId: Int,
)

@Parcelize
data class LiveEventInfo(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("event_name")
    val eventName: String? = null,

    @field:SerializedName("channel_id")
    var channelId: String,

    @field:SerializedName("is_lock")
    val isLock: Int? = null,

    @field:SerializedName("start_date")
    val startDate: String? = null,

    @field:SerializedName("expire_date")
    val expireDate: String? = null,

    @field:SerializedName("token")
    var token: String,

    @field:SerializedName("event_image")
    val eventImage: String? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("chat_user_name")
    val chatUserName: String? = null,

    @field:SerializedName("firstName")
    val firstName: String? = null,

    @field:SerializedName("lastName")
    val lastName: String? = null,

    @field:SerializedName("userName")
    var userName: String? = null,

    @field:SerializedName("live_views")
    val liveViews: Int? = null,

    @field:SerializedName("live_liked_counter")
    val liveLikedCounter: Int? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("profile_photo")
    var profilePhoto: String? = null,

    @field:SerializedName("userjoin")
    val userJoin: Int? = null,

    @field:SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null,

    @field:SerializedName("is_co_host")
    var isCoHost: Int? = null,

    @field:SerializedName("host_status")
    var hostStatus: Int? = null,

    @field:SerializedName("followers")
    val followers: Int? = null,

    // [{"key":"role_type","value":"RolePublisher","description":"RoleAttendee, RolePublisher","type":"text"}]
    @field:SerializedName("role_type")
    var roleType: String? = null,

    @field:SerializedName("is_private")
    var isPrivate: Int? = null,

    @field:SerializedName("no_of_followers")
    var noOfFollowers: Int? = null,

    @field:SerializedName("is_requested")
    var isRequested: Int? = null,

    @field:SerializedName("follow_status")
    var followStatus: Int? = null,

    @field:SerializedName("following_status")
    var followingStatus: Int? = null,

    @field:SerializedName("is_restrict")
    var isRestrict: Int? = null,

    @field:SerializedName("is_kicked")
    var isKicked: Int? = null,

    @field:SerializedName("is_allow_play_game")
    var isAllowPlayGame: Int? = 0,

    @field:SerializedName("restrict_by")
    var restrictBy: Int? = null,

    @field:SerializedName("is_verified")
    var isVerified: Int? = null,

    @field:SerializedName("is_co_host_notification")
    val isCoHostNotification: Int? = null,

    @field:SerializedName("host_status_notification")
    val hostStatusNotification: Int? = null,

    var reopenscreen: Boolean = false
) : Parcelable

@Keep
data class LiveEventWatchingCount(
    @field:SerializedName("roomId")
    val roomId: String? = null,

    @field:SerializedName("viewerCounter")
    val viewerCounter: Int? = null,
)

data class LiveRoomRequest(
    @field:SerializedName("roomId")
    val roomId: String?,

    @field:SerializedName("roomTag")
    val roomTag: String?,

    @field:SerializedName("userId")
    val userId: Int?,

    @field:SerializedName("profile_photo")
    val profilePhoto: String?,

    @field:SerializedName("liveId")
    val liveId: Int?,

    @field:SerializedName("is_allow_play_game")
    val isAllowPlayGame: Int? = null,

    @field:SerializedName("is_verified")
    val isVerified: Int?,
)

data class JoinLiveRoomRequest(
    @field:SerializedName("roomId")
    val roomId: String?,

    @field:SerializedName("userId")
    val userId: Int?,

    @field:SerializedName("liveId")
    val liveId: Int? = null,

    @field:SerializedName("is_allow_play_game")
    val isAllowPlayGame: Int? = null
)

data class LeaveLiveRoomRequest(
    @field:SerializedName("userId")
    val userId: String?,

    @field:SerializedName("live_id")
    val liveId: String?,

    @field:SerializedName("roomId")
    val roomId: String?,
)

@Parcelize
data class LiveSummaryInfo(
    @field:SerializedName("roomId")
    val roomId: String?,

    @field:SerializedName("start_time")
    val startTime: String?,

    @field:SerializedName("end_time")
    val endTime: String?,

    @field:SerializedName("total_time")
    val totalTime: String?,

    @field:SerializedName("total_gifts")
    val totalGifts: String?,

    @field:SerializedName("total_coins")
    val totalCoins: String?,

    @field:SerializedName("total_likes")
    val totalLikes: Int?,

    @field:SerializedName("total_comments")
    val totalComments: Int?,

    @field:SerializedName("total_viewers")
    val totalViewers: Int?,

    @field:SerializedName("total_time_of_this_month")
    val totalTimeOfThisMonth: String?,

    @Expose(serialize = false, deserialize = false)
    var centsValue: String? = null,

) : Parcelable

data class LiveEventVerifyRequest(
    @field:SerializedName("channel_id")
    val channelId: String? = null,

    @field:SerializedName("password")
    val password: String? = null,
)

@Parcelize
data class LiveEventSendOrReadComment(
    @field:SerializedName("roomId")
    val roomId: String?,

    @field:SerializedName("live_id")
    val liveId: Int?,

    @field:SerializedName("user_id")
    val userId: Int?,

    @field:SerializedName("id")
    val id: String,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("profile_url")
    val profileUrl: String? = null,

    @field:SerializedName("comment")
    val comment: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("to_userId")
    val toUserId: String? = null,

    @field:SerializedName("is_verified")
    val isVerified: Int? = null,
) : Parcelable

data class CoHostRequests(
    @field:SerializedName("live_id")
    val channelId: Int? = null,
    // i.e 50,55
    @field:SerializedName("invite_ids")
    val inviteIds: String? = null,
)

data class RejectJoinRequest(
    @field:SerializedName("id")
    val id: String? = null,
)

data class CheckCoHostFollowRequest(

    @field:SerializedName("user_id")
    val userId: Int,
)

data class CoHostFollowInfo(

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("is_follow")
    val isFollow: Int? = null,
)

data class LiveJoinResponse(
    @field:SerializedName("request_status")
    val requestStatus: Int? = null,

    @field:SerializedName("profileUrl")
    val profileUrl: String? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("role")
    val role: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("report_by")
    val reportBy: String? = null,

    @field:SerializedName("reason")
    val reason: String? = null,
)

data class LiveEventKickUser(

    @field:SerializedName("userId")
    val userId: Int?,

    @field:SerializedName("action_type")
    val actionType: String?,

    @field:SerializedName("seconds")
    val seconds: Int?,

    @field:SerializedName("roomId")
    val roomId: Int?,

    @field:SerializedName("restrictBy")
    val restrictBy: Int? = null,

)

data class SendHeartSocketEvent(
    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("roomId")
    val channelId: String? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,

    @field:SerializedName("is_allow_play_game")
    val isAllowPlayGame: Int? = 0,

    @field:SerializedName("heartCounter")
    val heartCounter: Int? = 0,
)

data class CoHostSocketEvent(
    @field:SerializedName("userId")
    val userId: Int? = null,

    @field:SerializedName("roomId")
    val roomId: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("profile_photo")
    val profile: String? = null,

    @field:SerializedName("live_id")
    val live_id: String? = null,

    @field:SerializedName("liveId")
    val liveId: String? = null,

    @field:SerializedName("is_allow_play_game")
    val isAllowPlayGame: Int? = 0,

    @field:SerializedName("is_verified")
    val isVerified: Int? = null,
)

data class CoHostListInfo(
    @field:SerializedName("roomId")
    val roomId: String? = null,

    @field:SerializedName("is_allow_play_game")
    val isAllowPlayGame: Int? = 0,

    @field:SerializedName("hosts")
    val hosts: List<CoHostInfo>? = null,
)

data class CoHostInfo(
    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("profile_photo")
    var profilephoto: String? = null,

    @field:SerializedName("is_host")
    var isHost: Boolean? = false,

    @field:SerializedName("is_verified")
    val isVerified: Int? = null
)

data class UpdateLiveViewerCountRequest(

    @field:SerializedName("roomId")
    val roomId: String? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,

    @field:SerializedName("userId")
    val userId: Int? = null,

    @field:SerializedName("is_allow_play_game")
    val isAllowPlayGame: Int? = null,

)

data class SendGiftInGameRequest(
    @field:SerializedName("giftId")
    val giftId: String? = null,

    @field:SerializedName("senderId")
    val senderId: String? = null,

    @field:SerializedName("receiverId")
    val receiverId: String? = null,

    @field:SerializedName("liveId")
    val liveId: String? = null,

    @field:SerializedName("roomId")
    val roomId: String? = null,

    @field:SerializedName("quantity")
    val quantity: Int? = 1,

    @field:SerializedName("is_allow_play_game")
    val isAllowPlayGame: Int? = 1,

    @field:SerializedName("is_combo")
    val isCombo: Int? = 0,
)

data class StartGameRequestRequest(

    @field:SerializedName("roomId")
    val roomId: String? = null,

    @field:SerializedName("liveId")
    val liveId: Int? = null,

    @field:SerializedName("userId")
    val userId: Int? = null,

    @field:SerializedName("is_verified")
    val isVerified: Int? = null

)

@Parcelize
data class GameResultInfo(
    @field:SerializedName("roomId")
    val roomId: String? = null,

    @field:SerializedName("liveId")
    val liveId: Int? = null,

    @field:SerializedName("userId")
    val userId: Int? = null,

    @field:SerializedName("game_result")
    val gameResult: ArrayList<GameResult>? = null,
) : Parcelable

@Parcelize
data class GameResult(
    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("total_gifts")
    val totalGifts: Int? = null,

    @field:SerializedName("total_coins")
    val totalCoins: Int? = null,

    @field:SerializedName("likes")
    val likes: Int? = null,

    @field:SerializedName("result")
    val result: String? = null,

    @field:SerializedName("total_game_duration")
    val totalGameDuration: String? = null,

    @field:SerializedName("total_win_count")
    val totalWinCount: Int? = null,

    @Expose(serialize = false, deserialize = false)
    var centsValue: String? = null,
) : Parcelable

data class ReceiveGiftInfo(

    @field:SerializedName("gift")
    val gift: GiftsItemInfo? = null,

    @field:SerializedName("sender")
    val sender: User? = null,

    @field:SerializedName("receiver")
    val receiver: User? = null,

    @field:SerializedName("quantity")
    val quantity: Int? = 0,

    @field:SerializedName("is_combo")
    val isCombo: Int? = 0,

)

data class TopGifterInfo(

    @field:SerializedName("topgifter")
    var topgifter: List<TopGifter>? = null,

    @field:SerializedName("progress")
    var progress: List<ProgressInfo>? = null,

    @field:SerializedName("game_started")
    var gameStarted: Int? = null,

    @field:SerializedName("play_start_time")
    var playStartTime: String,

)

data class ProgressInfo(

    @field:SerializedName("user_id")
    var userId: Int? = null,

    @field:SerializedName("total_gifts")
    var totalGifts: Int? = null,

    @field:SerializedName("total_coins")
    var totalCoins: Int? = null,

    @field:SerializedName("total_win_count")
    var totalWinCount: Int? = null,

)

@Parcelize
data class TopGifter(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("firstName")
    val firstName: String? = null,

    @field:SerializedName("lastName")
    val lastName: String? = null,

    @field:SerializedName("profile_photo")
    val profilePhoto: String? = null,

    @field:SerializedName("userName")
    val userName: String? = null,

    @field:SerializedName("media_url")
    val mediaUrl: String? = null,

    @field:SerializedName("from_id")
    var fromId: Int? = null,

    @field:SerializedName("total_gift")
    var totalGift: Int? = null,

    @field:SerializedName("coins")
    var coins: Int? = null,

    @field:SerializedName("to_id")
    var toId: Int? = null,

    @field:SerializedName("is_verified")
    var isVerified: Int? = null,

    @Expose(serialize = false, deserialize = false)
    var index: Int = 0,

) : Parcelable

data class CoinCentsInfo(

    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("uuid")
    var uuid: String? = null,

    @field:SerializedName("type")
    var type: String? = null,

    @field:SerializedName("name")
    var name: String? = null,
    @field:SerializedName("value")
    var value: String? = null,

    @field:SerializedName("data")
    var data: String? = null,
)

enum class LivePopupType {
    JoinLive,
    JoinVideoCall,
    JoinAsCoHost
}

sealed class LiveUserOption {
    object ViewProfile : LiveUserOption()
    object KickOut : LiveUserOption()
    object Restrict : LiveUserOption()
}

const val ROLE_PUBLISHER = "RolePublisher"
const val ROLE_ATTENDEE = "RoleAttendee"
