package com.meetfriend.app.api.post.model

import com.google.gson.annotations.SerializedName

data class ShortsCountRequest(
    @SerializedName("post_id")
    val postId: Int
)