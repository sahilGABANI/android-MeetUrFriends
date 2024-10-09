package com.meetfriend.app.api.follow.model

import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.responseclasses.video.Links
import java.io.Serializable

data class GetFollowingRequest(

    @field:SerializedName("page")
    val page: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("search")
    val search: String? = null,

    @field:SerializedName("per_page")
    val perPage: Int,

    )

data class GetFollowersRequest(

    @field:SerializedName("page")
    val page: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("search")
    val search: String? = null,

    @field:SerializedName("per_page")
    val perPage: Int,

    )

data class GetUserForLiveInviteRequest(

    @field:SerializedName("page")
    val page: Int,

    @field:SerializedName("per_page")
    val perPage: Int,

    )

data class FollowUnfollowRequest(
    @field:SerializedName("user_id")
    val userId: Int,
)

data class AcceptFollowRequestRequest(
    @field:SerializedName("follow_request_id")
    val followRequestId: Int,
)

sealed class FollowClickStates {
    data class FollowClick(val user: MeetFriendUser) : FollowClickStates()
    data class FollowingClick(val user: MeetFriendUser) : FollowClickStates()
    data class ProfileClick(val user: MeetFriendUser) : FollowClickStates()
    data class CancelClick(val user: MeetFriendUser) : FollowClickStates()
    data class MoreClick(val user: MeetFriendUser) : FollowClickStates()
}

data class FollowResult(
    @SerializedName("current_page") val current_page: Int,
    @SerializedName("data") val data: List<MeetFriendUser>? = null,
    @SerializedName("first_page_url") val first_page_url: String,
    @SerializedName("from") val from: Int,
    @SerializedName("last_page") val last_page: Int,
    @SerializedName("last_page_url") val last_page_url: String,
    @SerializedName("links") val links: List<Links>,
    @SerializedName("next_page_url") val next_page_url: String,
    @SerializedName("path") val path: String,
    @SerializedName("per_page") val per_page: Int,
    @SerializedName("prev_page_url") val prev_page_url: String,
    @SerializedName("to") val to: Int,
    @SerializedName("total") val total: Int
) : Serializable

sealed class FollowRequestClickStates {
    data class AcceptClick(val user: MeetFriendUser) : FollowRequestClickStates()
    data class RejectClick(val user: MeetFriendUser) : FollowRequestClickStates()
    data class ProfileClick(val user: MeetFriendUser) : FollowRequestClickStates()
}