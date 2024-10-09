package com.meetfriend.app.api.chat.model

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.profile.model.LinksItem
import kotlinx.parcelize.Parcelize

data class TempChatRoomInfo(
    val profileImage: Int,
    val chatName: String,
    val userCount: Int,

    )

data class TempMessageInfo(
    val profileImage: Int,
    val userName: String,
    val message: String,

    )

@Parcelize
data class ChatRoomIsJoined(

    @field:SerializedName("role")
    val role: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("is_banned")
    val isBanned: Int? = null,

    @field:SerializedName("is_restrict")
    val isRestrict: Int? = null,


    ) : Parcelable


@Parcelize
data class ChatRoomInfo(

    @field:SerializedName("isjoined")
    val isjoined: ChatRoomIsJoined? = null,

    @field:SerializedName("is_join")
    val isJoin: Boolean? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("last_message")
    val lastMessage: String? = null,

    @field:SerializedName("message_type")
    val messageType: String? = null,

    @field:SerializedName("last_datetime")
    val lastDatetime: String? = null,

    @field:SerializedName("room_name")
    var roomName: String? = null,

    @field:SerializedName("expire_date")
    val expireDate: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("room_descriprion")
    val roomDescriprion: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("receiver_id")
    val receiverId: Int,

    @field:SerializedName("payment_method")
    val paymentMethod: String? = null,

    @field:SerializedName("room_type")
    val roomType: Int? = null,

    @field:SerializedName("user")
    val user: ChatRoomUser? = null,

    @field:SerializedName("latest_join")
    val latestJoin: ArrayList<String>? = null,

    @field:SerializedName("file_path")
    val filePath: String? = null,

    @field:SerializedName("no_of_join_count")
    val noOfJoinCount: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("is_paid")
    val isPaid: Int? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("is_request")
    val isRequest: Boolean? = null,

    @field:SerializedName("is_admin")
    var isAdmin: Boolean = false,

    @field:SerializedName("is_kickout")
    val isKickout: Boolean? = null,

    @field:SerializedName("iskickouted")
    val isKickouted: KickoutedInfo? = null,

    @field:SerializedName("call_roomName")
    val callRoomName: String? = null,

    @field:SerializedName("is_expired")
    val is_expired: Boolean? = null,


    @field:SerializedName("transaction_id")
    val transactionId: String? = null,

    @field:SerializedName("receiver")
    val receiver: ChatRoomUser? = null,

    @field:SerializedName("is_restrict")
    val isRestrict: Int? = null,

    @field:SerializedName("unread_count")
    val unreadCount: Int? = null,

    @field:SerializedName("is_chat")
    val isChat: Int? = null,

     @field:SerializedName("is_verified")
    val isVerified: Int? = null,

    @Expose(serialize = false, deserialize = false)
    var isSelected: Boolean = false,

    ) : Parcelable

data class CreateChatRoomRequest(

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("room_name")
    val roomName: String? = null,

    @field:SerializedName("room_descriprion")
    val roomDescriprion: String? = null,

    @field:SerializedName("room_pic")
    val roomPic: String? = null,

    @field:SerializedName("payment_method")
    val paymentMethod: String? = null,

    @field:SerializedName("room_type")
    val roomType: Int? = null,

    )

data class ChatRoom(

    @field:SerializedName("per_page")
    val perPage: String? = null,

    @field:SerializedName("data")
    val data: List<ChatRoomInfo>? = null,

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
)

@Parcelize
data class ChatRoomUser(

    @field:SerializedName("id")
    val id: Int = 0,

    @field:SerializedName("firstName")
    val firstName: String? = null,

    @field:SerializedName("lastName")
    val lastName: String? = null,

    @field:SerializedName("profile_photo")
    val profilePhoto: String? = null,

    @field:SerializedName("sender_profile")
    val senderProfile: String? = null,

    @field:SerializedName("role")
    val role: String? = null,

    @field:SerializedName("chat_user_name")
    val chatUserName: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("bio")
    val bio: String? = null,

    @field:SerializedName("kickout_expire")
    val kickoutExpire: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("is_banned")
    val isBanned: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("expire_date")
    val expireDate: String? = null,

    @field:SerializedName("transaction_id")
    val transactionId: String? = null,

    @field:SerializedName("is_online")
    val isOnline: Int? = null,

    @field:SerializedName("last_seen")
    val lastSeen: String? = null,

    @field:SerializedName("is_verified")
    val isVerified: Int? = null,

) : Parcelable {
    fun getUserName(): String {
        return "${firstName ?: ""} ${lastName ?: ""}"
    }
}

data class JoinRoomRequest(

    @field:SerializedName("conversationId")
    val conversationId: Int = 0,

    )

@Parcelize
data class SendJoinChatRoomRequestRequest(

    @field:SerializedName("receiver_id")
    val receiverId: Int = 0,

    @field:SerializedName("sender_id")
    val senderId: Int = 0,

    @field:SerializedName("conversation_id")
    val conversationId: Int = 0,

    @field:SerializedName("sender_name")
    val senderName: String? = null,

    @field:SerializedName("conversation_name")
    val conversationName: String? = null,

    @field:SerializedName("sender_profile")
    val senderProfile: String? = null,

    ) : Parcelable

@Parcelize
data class KickoutedInfo(

    @field:SerializedName("role")
    val role: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("kickout_expire")
    val kickoutExpire: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("is_banned")
    val isBanned: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null,
) : Parcelable

sealed class ChatRoomListItemActionState {
    data class ContainerClick(val chatRoomInfo: ChatRoomInfo) : ChatRoomListItemActionState()
    data class JoinClick(val chatRoomInfo: ChatRoomInfo) : ChatRoomListItemActionState()
    data class DeleteClick(val chatRoomInfo: ChatRoomInfo) : ChatRoomListItemActionState()

}

data class CreateOneToOneChatRequest(

    @field:SerializedName("receiver_id")
    val receiverId: Int,
)

data class JoinChatRoomResponse(

    @field:SerializedName("message")
    val message: String,

    @field:SerializedName("conversationId")
    val conversationId: Int,

    @field:SerializedName("call_token")
    val callToken: String? = null,

    @field:SerializedName("call_user_id")
    val callUserId: Int? = null,
)

data class UpdateChatRoomRequest(
    @field:SerializedName("room_name")
    val roomName: String,

    @field:SerializedName("room_descriprion")
    val roomDescription: String,

    @field:SerializedName("room_pic")
    val roomPic: String? = null,
)

data class GetChatRoomAdminRequest(

    @field:SerializedName("conversation_id")
    val conversationId: Int,

    )

data class GetMiceAccessRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int
)

data class ReportUserRequest(

    @field:SerializedName("report_for")
    val reportFor: Int?,

    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("reason")
    val reason: String

)

data class ReportUserInfo(

    @field:SerializedName("reason")
    val reason: String? = null,

    @field:SerializedName("report_by")
    val reportBy: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("report_for")
    val reportFor: Int? = null
)

data class TypingRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,
)

data class SeenMsgRequest(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("conversationId")
    val conversationId: Int? = null,

    @field:SerializedName("receiverId")
    val receiverId: Int? = null,
)
