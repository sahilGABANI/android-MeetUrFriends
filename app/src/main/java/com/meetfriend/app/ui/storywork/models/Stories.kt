package com.meetfriend.app.ui.storywork.models

import com.google.gson.annotations.SerializedName

data class Stories(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("content") val content: String,
    @SerializedName("file_name") val file_name: String,
    @SerializedName("file_path") val file_path: String? = null,
    @SerializedName("type") val type: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("status") val status: Int,
    @SerializedName("created_at") val created_at: String,
    @SerializedName("updated_at") val updated_at: String
)
