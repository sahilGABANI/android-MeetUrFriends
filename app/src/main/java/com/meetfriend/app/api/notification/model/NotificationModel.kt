package com.meetfriend.app.api.notification.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.chat.model.SendJoinChatRoomRequestRequest
import com.meetfriend.app.api.profile.model.LinksItem
import kotlinx.parcelize.Parcelize

data class NotificationInfo(

    @field:SerializedName("per_page")
    val perPage: String? = null,

    @field:SerializedName("data")
    val data: List<NotificationItemInfo>? = null,

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
data class NotificationItemInfo(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("from_user")
    val fromUser: NotificationFromUserInfo? = null,

    @field:SerializedName("to_user")
    val toUser: Int? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("type_id")
    val typeId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("is_read")
    val isRead: Int? = null,

    @field:SerializedName("is_repeat")
    val isRepeat: Int? = null,

) : Parcelable

@Parcelize
data class NotificationFromUserInfo(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("firstName")
    val firstName: String? = null,

    @field:SerializedName("lastName")
    val lastName: String? = null,

    @field:SerializedName("profile_photo")
    val profilePhoto: String? = null,

    @field:SerializedName("cover_photo")
    val coverPhoto: String? = null,

    @field:SerializedName("gender")
    val gender: String? = null,

) : Parcelable

data class AcceptRejectRequestRequest(

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("from_uid")
    val fromUid: Int? = null,
)

data class VoipCallRequest(
    @field:SerializedName("friend_id")
    val friendId: Int,

    @field:SerializedName("call_type")
    val callType: String,

    @field:SerializedName("type")
    val type: String,

    @field:SerializedName("channelName")
    val channelName: String,

    @field:SerializedName("token")
    val token: String,
)

data class AppConfigInfo(

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("value")
    val value: String? = null,
)

enum class AdType {
    @SerializedName("NativeAdId")
    NativeAdId,

    @SerializedName("InterstitialAdId")
    InterstitialAdId,

    @SerializedName("BannerAdId")
    BannerAdId,

    @SerializedName("AppOpenAdId")
    AppOpenAdId,

    @SerializedName("RewardedAdId")
    RewardedAdId,

    @SerializedName("InterstitialAdScrollCount")
    InterstitialAdScrollCount,

    @SerializedName("InterstitialAdClickCount")
    InterstitialAdClickCount,
}

sealed class NotificationActionState {
    data class ProfileImageClick(val notificationItemInfo: NotificationItemInfo) :
        NotificationActionState()

    data class AcceptClick(val notificationItemInfo: NotificationItemInfo) :
        NotificationActionState()

    data class RejectClick(val notificationItemInfo: NotificationItemInfo) :
        NotificationActionState()

    data class AcceptFromPopup(val receivedRequestData: SendJoinChatRoomRequestRequest) :
        NotificationActionState()

    data class RejectFromPopup(val receivedRequestData: SendJoinChatRoomRequestRequest) :
        NotificationActionState()

    data class ContainerClick(val notificationItemInfo: NotificationItemInfo) :
        NotificationActionState()

    data class DeleteClick(val notificationItemInfo: NotificationItemInfo) :
        NotificationActionState()
}
