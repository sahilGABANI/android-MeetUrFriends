package com.meetfriend.app.api.post.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class HashTagResponse(

    @field:SerializedName("current_page") var currentPage: Int?,

    @field:SerializedName("data") var data: ArrayList<HashTagsResponse>?,

) : Parcelable

@Parcelize
data class HashTagsResponse(

    @field:SerializedName("tag_name") var tagName: String?,

    @field:SerializedName("total_posts") var totalPosts: Int?,

    @field:SerializedName("total_post_challenges") var totalPostChallenges: Int?,

    @field:SerializedName("id") var id: Int,

    @field:SerializedName("total_challenges") var totalChallenges: Int?,

    @field:SerializedName("total_stories") var totalStories: Int? = null,

    @field:SerializedName("created_at") var createdAt: String? = null,

    @field:SerializedName("user_id") var userId: Int? = null,

    @field:SerializedName("updated_at") var updatedAt: String? = null,

    @field:SerializedName("total_shorts") var totalShorts: Int?

) : Parcelable
