package com.meetfriend.app.ui.storywork.models

import com.google.gson.annotations.SerializedName

data class SingleStory(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("content") val content: String? = null,
    @SerializedName("file_name") val file_name: String? = null,
    @SerializedName("file_path") val file_path: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("created_at") val created_at: String? = null,
    @SerializedName("updated_at") val updated_at: String? = null,
    @SerializedName("no_of_views_count") val no_of_views_count: Int? = null,
    @SerializedName("is_viewed_count") val is_viewed_count: Int? = null,
    @SerializedName("user") val user: User?= null,
    @SerializedName("listViewed") val listViewed: Boolean? = null
)