package com.meetfriend.app.ui.storywork.models

import com.google.gson.annotations.SerializedName

data class StoryDetailResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("media_url") val media_url: String,
    @SerializedName("base_url") val base_url: String,
    @SerializedName("result") val storiesResult: StoryDetailResult
)