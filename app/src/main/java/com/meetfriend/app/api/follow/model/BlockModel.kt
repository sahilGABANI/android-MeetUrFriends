package com.meetfriend.app.api.follow.model

import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.responseclasses.video.Links

data class BlockResult(
    @field:SerializedName("current_page")
    val current_page: Int,
    @field:SerializedName("data")
    val data: List<BlockInfo>? = null,
    @field:SerializedName("first_page_url")
    val first_page_url: String,
    @field:SerializedName("from")
    val from: Int,
    @field:SerializedName("last_page")
    val last_page: Int,
    @field:SerializedName("last_page_url")
    val last_page_url: String,
    @field:SerializedName("links")
    val links: List<Links>,
    @field:SerializedName("next_page_url")
    val next_page_url: String,
    @field:SerializedName("path")
    val path: String,
    @field:SerializedName("per_page")
    val per_page: Int,
    @field:SerializedName("prev_page_url")
    val prev_page_url: String,
    @field:SerializedName("to")
    val to: Int,
    @field:SerializedName("total")
    val total: Int
)

data class BlockInfo(
    val accepted_user: MeetFriendUser,
    val block_status: String,
    val friend_id: Int,
    val is_blocked_by_me: Int,
    val friend_status: String,
    val id: Int,
    val request_status: String,
    val user_id: Int,
    val isadded: Int,
    var isSelected: Boolean = false
)
