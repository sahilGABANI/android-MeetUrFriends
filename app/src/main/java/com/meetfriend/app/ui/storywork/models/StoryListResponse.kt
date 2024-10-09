package com.meetfriend.app.ui.storywork.models

import com.google.gson.annotations.SerializedName

data class StoryListResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("media_url") val media_url: String,
    @SerializedName("base_url") val base_url: String,
    @SerializedName("result") val result: List<ResultListResult>,
    @SerializedName("user_data") val user_data: UserData
)