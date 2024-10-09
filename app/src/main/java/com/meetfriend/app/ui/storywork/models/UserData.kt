package com.meetfriend.app.ui.storywork.models

import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("content") val content: String,
    @SerializedName("file_name") val file_name: String,
    @SerializedName("file_path") val file_path: String,
    @SerializedName("status") val status: Int,
    @SerializedName("type") val type: String,
    @SerializedName("created_at") val created_at: String,
    @SerializedName("updated_at") val updated_at: String,
    @SerializedName("no_of_views_count") val no_of_views_count: Int,
    @SerializedName("is_viewed_count") val is_viewed_count: Int,
    @SerializedName("user") val user: User?= null,
    @SerializedName("listViewed") val listViewed: Boolean
)
