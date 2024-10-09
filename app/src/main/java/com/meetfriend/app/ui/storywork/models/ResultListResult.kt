package com.meetfriend.app.ui.storywork.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResultListResult(
    @SerializedName("id")
    val id: Int,
    @field:SerializedName("conversation_id")
    var conversationId: Int? = null,
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName")
    val lastName: String? = null,
    @SerializedName("email_or_phone")
    val email_or_phone: String,
    @SerializedName("profile_photo")
    val profile_photo: String? = null,
    @SerializedName("stories")
    val stories: ArrayList<StoriesResponse> = arrayListOf(),
    @SerializedName("userName")
    val userName: String? = null,
    var isSelected: Boolean = false
): Parcelable

@Parcelize
data class StoriesResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("content") val content: String? = null,
    @SerializedName("file_name") val file_name: String? = null,
    @SerializedName("file_path") val file_path: String? = null,
    @SerializedName("video_url") val video_url: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("created_at") val created_at: String? = null,
    @SerializedName("updated_at") val updated_at: String? = null,
    @SerializedName("no_of_views_count") val no_of_views_count: Int? = null,
    @SerializedName("is_viewed_count") val is_viewed_count: Int? = null,
    @SerializedName("user") val user: User?= null,
    @SerializedName("listViewed") val listViewed: Boolean? = null,
    @SerializedName("web_link") val web_link: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName("rotation_angle") val rotationAngle: Float? = null,
    @field:SerializedName("music_title") var music_title: String? = null,
    @field:SerializedName("artists") var artists: String? = null,
    var isSelected: Boolean = false
): Parcelable

@Parcelize
data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("profile_photo") val profile_photo: String,
    @SerializedName("is_verified") val isVerified: Int
):Parcelable

sealed class HomePageStoryInfoState {
    data class StoryResponseData(val storyListResponse: ResultListResult) : HomePageStoryInfoState()
    data class AddStoryResponseInfo(val storyInfo: String) : HomePageStoryInfoState()
    data class UserProfileClick(val storyListResponse: ResultListResult) : HomePageStoryInfoState()
    object LoadMoreStories: HomePageStoryInfoState()
}