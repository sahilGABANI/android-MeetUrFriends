package com.meetfriend.app.newbase.network.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.chat.model.ChatRoomUser
import com.meetfriend.app.api.profile.model.ChatRoomUserInfo
import com.meetfriend.app.ui.storywork.models.UserData

@Keep
data class MeetFriendResponse<T>(

    @field:SerializedName("profile_updated_status")
    val profileUpdatedStatus: Boolean? = null,

    @field:SerializedName("access_token")
    val accessToken: String? = null,

    @field:SerializedName("result")
    val result: T? = null,

    @field:SerializedName("data")
    val data: T? = null,

    @field:SerializedName("base_url")
    val baseUrl: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("media_url")
    val mediaUrl: String? = null,

    @field:SerializedName("status")
    val status: Boolean = false,

    @field:SerializedName("api_token")
    val apiToken: String? = null,

    @field:SerializedName("total_unread_count")
    val totalUnreadCount: Int? = null,

    @field:SerializedName("total_current_coins")
    val total_current_coins: Double? = null,

    @field:SerializedName("total_earning")
    val totalEarning: String? = null,

    @field:SerializedName("success")
    val success: Boolean? = null,
)

@Keep
data class MeetFriendResponseForChat<T>(

    @field:SerializedName("data")
    val data: T? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("status")
    val status: Boolean = false,

    @field:SerializedName("users")
    val users: List<ChatRoomUser>? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,

    @field:SerializedName("total_cohost")
    var totalCohost: Int? = null,
)

@Keep
data class MeetFriendResponseForLive<T>(

    @field:SerializedName("data")
    val data: List<T>? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("status")
    val status: Boolean = false,

    @field:SerializedName("users")
    val users: List<ChatRoomUser>? = null,
)

@Keep
data class MeetFriendResponseForGetProfile<T>(

    @field:SerializedName("profile_updated_status")
    val profileUpdatedStatus: Boolean? = null,

    @field:SerializedName("access_token")
    val accessToken: String? = null,

    @field:SerializedName("result")
    val result: T? = null,

    @field:SerializedName("base_url")
    val baseUrl: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("media_url")
    val mediaUrl: String? = null,

    @field:SerializedName("status")
    val status: Boolean = false,

    @field:SerializedName("api_token")
    val apiToken: String? = null,

    @field:SerializedName("data")
    val data: ChatRoomUserInfo? = null,
)

@Keep
data class MeetFriendCommonResponse(
    @field:SerializedName("status")
    val status: Boolean = false,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("result")
    val result: Int? = null,

    @field:SerializedName("total_message_unread_count")
    val totalMessageUnreadCount: Int? = null,

    @field:SerializedName("uid")
    val uid: Int? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("channelName")
    val channelName: String? = null,

    @field:SerializedName("token")
    val token: String? = null,

    @field:SerializedName("follow_for")
    val follow_for: String? = null,

    @field:SerializedName("is_live")
    val isLive: Int? = null,

    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("challenge_id")
    val challengeId: Int? = null,

    @field:SerializedName("hub_request")
    val hubRequest: Boolean? = false,

    @field:SerializedName("place_api_key")
    val placeApiKey: String? = null
)

data class MeetFriendCommonStoryResponse<T>(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("media_url")
    val media_url: String,
    @SerializedName("base_url")
    val base_url: String,
    @SerializedName("result")
    val result: List<T>,
    @SerializedName("user_data")
    val user_data: UserData,
    @SerializedName("total_earning")
    val totalEarning: String ? = null,
    @SerializedName("total_sent")
    val totalSent: String ? = null,
    @SerializedName("total_recieved")
    val totalRecieved: String ? = null,
)


data class MeetFriendCommonChallengeResponse<T>(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("media_url")
    val media_url: String,
    @SerializedName("base_url")
    val base_url: String,
    @SerializedName("result")
    val result: T
)


@Keep
data class MeetFriendCommonResponses(
    @field:SerializedName("status")
    val status: Boolean = false,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("no_of_short_count")
    val noOfShotCount: Int? = null
)

@Keep
data class MeetFriendCommonResponseForStory(
    @field:SerializedName("status")
    val status: Boolean = false,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,
)

data class MeetFriendLogOutResponse<T>(

    @field:SerializedName("access_token")
    val accessToken: String? = null,

    @field:SerializedName("logout_conversation_id")
    val logoutConversationId: List<Int?>? = null,

    @field:SerializedName("registrationToken")
    val registrationToken: List<String?>? = null,

    @field:SerializedName("data")
    val data: T? = null,

    @field:SerializedName("success")
    val success: Boolean? = null,

    @field:SerializedName("conversation_id")
    val conversationId: List<Int?>? = null,

    @field:SerializedName("token_type")
    val tokenType: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("expires_in")
    val expiresIn: Int? = null
)