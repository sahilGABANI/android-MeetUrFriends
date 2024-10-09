package com.meetfriend.app.api.hashtag.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import kotlinx.parcelize.Parcelize

data class HashtagResponse(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user")
    val user: MeetFriendUser? = null,

    @field:SerializedName("follow_status")
    val followStatus: Int? = null,

    @field:SerializedName("total_post_challenges")
    val totalPostChallenges: Int? = null,

    @field:SerializedName("following_status")
    val followingStatus: Int? = null,

    @field:SerializedName("is_block")
    val isBlock: Int? = null,

    @field:SerializedName("total_shorts")
    val totalShorts: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("total_posts")
    val totalPosts: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("total_stories")
    val totalStories: Int? = null,

    @field:SerializedName("is_private")
    val isPrivate: Int? = null,

    @field:SerializedName("like_count")
    val likeCount: Int? = null,

    @field:SerializedName("tag_name")
    val tagName: String? = null,

    @field:SerializedName("total_challenges")
    val totalChallenges: Int? = null,

    @field:SerializedName("is_requested")
    val isRequested: Int? = null,
)

@Parcelize
data class HashtagInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("hashtag_id")
    val hashtagId: Int,

    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("tag_name")
    val tagName: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null

): Parcelable

@Parcelize
data class ReportHashtagRequest(
    @field:SerializedName("hashtag_id")
    val hashtagId: Int,

    @field:SerializedName("reason")
    val reason: String? = null,
):

    Parcelable@Parcelize
data class GetCountryHashtagRequest(
    @field:SerializedName("country_code")
    val countryCode: String,
): Parcelable


@Parcelize
data class ReportHashtagResponse(
    @field:SerializedName("hashtag_id")
    val hashtagId: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("reason")
    val reason: String? = null,

    @field:SerializedName("created_at")
    val createdAAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("id")
    val id: Int,
): Parcelable