package com.meetfriend.app.api.chat.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.profile.model.LinksItem
import kotlinx.parcelize.Parcelize


data class ChatRoomMessage(

    @field:SerializedName("per_page")
    val perPage: String? = null,

    @field:SerializedName("data")
    val data: List<MessageInfo>? = null,

    @field:SerializedName("last_page")
    val lastPage: Int? = null,

    @field:SerializedName("next_page_url")
    val nextPageUrl: Any? = null,

    @field:SerializedName("prev_page_url")
    val prevPageUrl: Any? = null,

    @field:SerializedName("first_page_url")
    val firstPageUrl: String? = null,

    @field:SerializedName("path")
    val path: String? = null,

    @field:SerializedName("total")
    val total: Int? = null,

    @field:SerializedName("last_page_url")
    val lastPageUrl: String? = null,

    @field:SerializedName("from")
    val from: Int? = null,

    @field:SerializedName("links")
    val links: List<LinksItem?>? = null,

    @field:SerializedName("to")
    val to: Int? = null,

    @field:SerializedName("current_page")
    val currentPage: Int? = null,

    @field:SerializedName("isOnline")
    val isOnline: Int? = null,
)

@Parcelize
data class MessageInfo(

    @field:SerializedName("sender_profile")
    val senderProfile: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("message_type")
    val messageType: MessageType? = null,

    @field:SerializedName("sender_name")
    val senderName: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("message")
    var message: String? = null,

    @field:SerializedName("sender_id")
    val senderId: Int? = null,

    @field:SerializedName("request_to_user")
    val requestToUser: Int? = null,

    @field:SerializedName("age")
    val age: String? = null,

    @field:SerializedName("gender")
    val gender: String? = null,

    @field:SerializedName("is_online")
    val isOnline: Int? = null,

    @field:SerializedName("reply_id")
    val replyId: Int? = null,

    @field:SerializedName("reply_name")
    val replyName: String? = null,

    @field:SerializedName("reply_message")
    val replyMessage: String? = null,

    @field:SerializedName("is_save")
    var isSave: Boolean = false,

    @field:SerializedName("file_url")
    var fileUrl: String? = null,

    @field:SerializedName("video_url")
    var videoUrl: String? = null,

    @field:SerializedName("story_id")
    var storyId: Int? = null,

    @field:SerializedName("request_coins")
    var requestCoins: Int? = null,

    @field:SerializedName("request_status")
    var requestStatus: Int? = null,

    @field:SerializedName("gift_id")
    var giftId: Int? = null,

    @field:SerializedName("is_seen")
    var isSeen: Int? = null,

    @field:SerializedName("timestemp")
    var timestemp: Double? = null,

    ) : Parcelable

data class GetChatMessageRequest(

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("per_page")
    val perPage: Int? = null,

    @field:SerializedName("room_type")
    val roomType: Int? = null,
)

data class SendPrivateMessageRequest(

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("receiver_id")
    val receiverId: Int? = null,

    @field:SerializedName("sender_id")
    val senderId: Int? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("message_type")
    val messageType: String? = null,

    @field:SerializedName("sender_name")
    val senderName: String? = null,

    @field:SerializedName("conversation_name")
    val conversationName: String? = null,

    @field:SerializedName("sender_profile")
    val senderProfile: String? = null,

    @field:SerializedName("age")
    val age: String? = null,

    @field:SerializedName("gender")
    val gender: String? = null,

    @field:SerializedName("kickout_userid")
    val kickoutUserId: Int? = null,

    @field:SerializedName("seconds")
    val seconds: Int? = null,

    @field:SerializedName("reply_id")
    val replyId: Int? = null,

    @field:SerializedName("reply_name")
    val replyName: String? = null,

    @field:SerializedName("reply_message")
    val replyMessage: String? = null,

    @field:SerializedName("file_url")
    var fileUrl: String? = null,

    @field:SerializedName("video_url")
    var videoUrl: String? = null,

    @field:SerializedName("room_type")
    var roomType: Int? = null,

    @field:SerializedName("request_coins")
    var requestCoins: Int? = null,

    @field:SerializedName("request_status")
    var requestStatus: Int? = null,

    @field:SerializedName("gift_id")
    var giftId: Int? = null,

    @field:SerializedName("story_id")
    var storyId: Int? = null,

    @field:SerializedName("is_verified")
    var isVerified: Int? = null,
    )

enum class MessageType {
    @SerializedName("initial")
    Initial,

    @SerializedName("kickout")
    Kickout,

    @SerializedName("slap")
    Slap,

    @SerializedName("ban")
    Ban,

    @SerializedName("joinF")
    JoinF,

    @SerializedName("text")
    Text,

    @SerializedName("image")
    Image,

    @SerializedName("video")
    Video,

    @SerializedName("join")
    Join,

    @SerializedName("left")
    Left,

    @SerializedName("send_gift")
    SendGift,

    @SerializedName("request_coins")
    RequestCoins,

    @SerializedName("reply")
    Reply,

    @SerializedName("reply_story")
    ReplyStory,

    @SerializedName("typing")
    Typing,

    @SerializedName("date")
    Date,
}

data class AgoraVoiceCallChannelInfoRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int,
)

data class AgoraVoiceCallChannelInfo(
    @field:SerializedName("token")
    val token: String? = null,

    @field:SerializedName("channel_name")
    val channelName: String,
)

data class AgoraSDKVoiceCallTokenInfo(
    @field:SerializedName("rtcToken")
    val rtcToken: String,
)

data class JoinAgoraSDKVoiceCallChannel(
    val channelName: String,
    val userId: Int,
    val rolType: Int,
    val token: String,
    val conversationId: Int,
)

data class VoiceCallStartSocketRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int,
    @field:SerializedName("agoraToken")
    val agoraToken: String,
    @field:SerializedName("agoraRoomName")
    val agoraRoomName: String,
    @field:SerializedName("user_id")
    val userId: Int,
)

data class VoiceCallEndSocketRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int,
)

data class RestrictRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("sender_id")
    val senderId: Int,
)

data class MentionUserInfo(

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("chat_user_name")
    val chatUserName: String? = null,

    @field:SerializedName("userName")
    val userName: String? = null,
)

data class GetMentionUserRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("search")
    val search: String? = null,
)

data class RestrictUserRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int?,

    @field:SerializedName("user_id")
    val userId: Int?,
)

@Parcelize
data class SendMicAccessRequestSocketRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("sender_id")
    val senderId: Int? = null,

    @field:SerializedName("sender_name")
    val senderName: String? = null,

    @field:SerializedName("conversation_name")
    val conversationName: String? = null,

    @field:SerializedName("sender_profile")
    val senderProfile: String? = null,
) : Parcelable

data class MiceAccessInfo(
    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("chat_user_name")
    val chatUserName: String? = null,

    @field:SerializedName("sender_profile")
    val senderProfile: String? = null,
)