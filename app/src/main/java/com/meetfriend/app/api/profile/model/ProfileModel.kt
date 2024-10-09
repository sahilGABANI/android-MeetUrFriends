package com.meetfriend.app.api.profile.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class ChangeProfileRequest(

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("profile_pic")
    val profilePic: String? = null,
)

data class CreateChatRoomUserRequest(

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("chat_user_name")
    val chatUserName: String? = null,
)

@Parcelize
data class ChatRoomUserInfo(

    @field:SerializedName("chat_user_name")
    val chatUserName: String? = null,

    @field:SerializedName("is_online")
    val isOnline: Int? = null,
):Parcelable

data class LinksItem(

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("label")
    val label: String? = null,

    @field:SerializedName("url")
    val url: Any? = null,
)

data class GetProfileInfoRequest(

    @field:SerializedName("user_id")
    val userId: Int,
)

data class CheckUserRequest(

    @field:SerializedName("id")
    val id: String,
)

data class ChatRoomUserProfileInfo(

    @field:SerializedName("per_page")
    val perPage: String? = null,

    @field:SerializedName("data")
    val data: List<ProfileItemInfo>? = null,

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
data class ProfileItemInfo(

    @field:SerializedName("file_path")
    val filePath: String? = null,

    @field:SerializedName("extension")
    val extension: String? = null,

    @field:SerializedName("size")
    val size: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("file_name")
    val fileName: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,
) : Parcelable


enum class SelectedOption {
    Camera,
    Gallery,
    VideoCamera,
    VideoGallery
}
@Parcelize
data class UserOnlineStatusResponse(

    @field:SerializedName("is_online_user_id")
    val isOnlineUserId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("is_online")
    val isOnline: Boolean? = null,
) : Parcelable

data class SendFeedbackRequest(

    @field:SerializedName("rating")
    val rating: Float? = null,

    @field:SerializedName("message")
    val message: String? = null,
)

data class FeedbackInfo(

    @field:SerializedName("rating")
    val rating: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,
)

data class SendHelpRequest(

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("phone")
    val phone: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("media")
    val media: String? = null,
)

data class HelpFormInfo(

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("phone")
    val phone: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("media")
    val media: String? = null,
    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,
)

@Parcelize
data class ProfileValidationInfo(

    @field:SerializedName("have_account")
    val haveAccount: Boolean? = false,

    @field:SerializedName("have_personal_account")
    val havePersonalAccount: Boolean? = false,

    @field:SerializedName("valid_age")
    val validAge: Boolean? = false,

    @field:SerializedName("have_least_3000_followers")
    val validFollowers: Boolean? = false,

    @field:SerializedName("have_least_10000_video_views_in_the")
    val validViews: Boolean? = false,

    @field:SerializedName("creates_should_have_original_content")
    val validContent: Boolean? = false,

    @field:SerializedName("does_not_have_a_violations")
    val validViolation: Boolean? = false,

) : Parcelable

sealed class ProfileMoreActionState{
    object DeleteClick : ProfileMoreActionState()
    object  ShareClick : ProfileMoreActionState()
    object ReportClick : ProfileMoreActionState()

}