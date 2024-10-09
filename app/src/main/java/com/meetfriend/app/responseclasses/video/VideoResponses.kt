package com.meetfriend.app.responseclasses.video

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.hashtag.model.HashtagInfo
import com.meetfriend.app.api.post.model.MentionUser
import com.meetfriend.app.api.post.model.PostLikesInformation
import com.meetfriend.app.api.post.model.TaggedUser
import com.meetfriend.app.responseclasses.posts.PostMedia
import kotlinx.parcelize.Parcelize
import java.io.Serializable

data class Result (
    @SerializedName("current_page") val current_page : Int,
    @SerializedName("data") val videoData : List<DataVideo>,
    @SerializedName("first_page_url") val first_page_url : String,
    @SerializedName("from") val from : Int,
    @SerializedName("last_page") val last_page : Int,
    @SerializedName("last_page_url") val last_page_url : String,
    @SerializedName("links") val links : List<Links>,
    @SerializedName("next_page_url") val next_page_url : String,
    @SerializedName("path") val path : String,
    @SerializedName("per_page") val per_page : Int,
    @SerializedName("prev_page_url") val prev_page_url : String,
    @SerializedName("to") val to : Int,
    @SerializedName("total") val total : Int
): Serializable

data class Links (
    @SerializedName("url") val url : String,
    @SerializedName("label") val label : String,
    @SerializedName("active") val active : Boolean
): Serializable

@Parcelize
data class DataVideo (
    @SerializedName("id") val id : Int,
    @SerializedName("user_id") val user_id : Int,
    @SerializedName("posts_id") val posts_id : Int,
    @SerializedName("file_name") val file_name : String,
    @SerializedName("file_path") val file_path : String,
    @SerializedName("extension") val extension : String,
    @SerializedName("original_video_url") val originalVideoUrl : String,
    @SerializedName("size") val size : Int,
    @SerializedName("created_at") val createdAt : String,
    @SerializedName("post") var post : Post,
    @SerializedName("post_hashtags") val post_hashtags: ArrayList<HashtagInfo>? = null,
    @SerializedName("ads") var ads : Boolean,
    @SerializedName("thumbnail") val thumbnail : String? = null,
    @SerializedName("follow_back") var followBack : Int,
    @SerializedName("liveId") val liveId : Int,
    @SerializedName("width") val width : Int? = 0,
    @SerializedName("height") val height : Int? = 0,
    @field:SerializedName("music_title") var music_title: String? = null,
    @field:SerializedName("artists") var artists: String? = null,
) : Parcelable

@Parcelize
data class Post (

    @SerializedName("id") val id : Int,
    @SerializedName("user_id") val user_id : Int,
    @SerializedName("content") val content : String? = null,
    @SerializedName("location") val location : String? = null,
    @SerializedName("is_shared") val is_shared : Int,
    @SerializedName("short_views") val shortViews : Int,
    @SerializedName("repost_count") val repostCount : Int,
    @SerializedName("shared_post_id") val shared_post_id : Int,
    @SerializedName("shared_user_id") val shared_user_id : Int,
    @SerializedName("type") val type : String,
    @SerializedName("privacy") val privacy : Int,
    @SerializedName("created_at") val created_at : String,
    @SerializedName("post_likes_count") var post_likes_count : Int,
    @SerializedName("is_liked_count") var is_liked_count : Int,
    @SerializedName("no_of_shared_count") var no_of_shared_count : Int,
    @SerializedName("total_gift_count") var total_gift_count : Int,
    @SerializedName("post_likes") val post_likes : List<PostLikesInformation> = arrayListOf(),
    @SerializedName("user") val user : User,
    @SerializedName("post_media") val post_media : ArrayList<PostMedia> = arrayListOf(),
    @SerializedName("post_comments") var post_comments : MutableList<Post_comments>,
    @SerializedName("shared_post") val shared_post : Post? = null,
    @SerializedName("follow_back") var followBack : Int,
    @SerializedName("tagged_users") val tagged_users_list: ArrayList<TaggedUser>? = null,
    @SerializedName("mention_users") val mention_users: ArrayList<MentionUser>? = null,
    @SerializedName("post_hashtags") val post_hashtags: ArrayList<HashtagInfo>? = null,
    @SerializedName("shared_mention_users") val shared_mention_users: ArrayList<MentionUser>? = null,
    @field:SerializedName("platform") var platform: String? = null,

    @SerializedName("web_link") val web_link: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName("file_path") val file_path: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("original_video_url") val original_video_url: String? = null,
    @SerializedName("width") val width : Int? = 0,
    @SerializedName("height") val height : Int? = 0,
    @SerializedName("rotation_angle") val rotationAngle: Float? = null,
    @SerializedName("post") val post: Post? = null,


    @field:SerializedName("music_title") var music_title: String? = null,
    @field:SerializedName("artists") var artists: String? = null,
): Parcelable

data class User (

    @SerializedName("id") val id : Int,
    @SerializedName("firstName") val firstName : String,
    @SerializedName("lastName") val lastName : String,
    @SerializedName("profile_photo") val profile_photo : String,
    @SerializedName("cover_photo") val cover_photo : String? = null,
    @SerializedName("gender") val gender : String,
    @SerializedName("userName") val userName : String?=null,
    @SerializedName("is_verified") val isVerified : Int?=null
): Serializable

data class  Post_comments(

    @SerializedName("id") val id : Int,
    @SerializedName("post_id") val post_id : Int? = null,
    @SerializedName("user_id") val user_id : Int? = null,
    @SerializedName("parent_id") val parent_id : String,
    @SerializedName("content") var content : String? = null,
    @SerializedName("replied_to_user_id") val replied_to_user_id : String,
    @SerializedName("created_at") val created_at : String? = null,
    @SerializedName("child_comments") val child_comments : ArrayList<Child_comments> = arrayListOf(),
    @SerializedName("user") val user : User ?= null,
    @SerializedName("mention_comments") val mention_comments : ArrayList<MentionUser>? = null

): Serializable

data class Child_comments (

    @SerializedName("id") val id : Int,
    @SerializedName("post_id") val post_id : Int? = null,
    @SerializedName("user_id") val user_id : Int? = null,
    @SerializedName("parent_id") val parent_id : Int? = null,
    @SerializedName("content") var content : String,
    @SerializedName("replied_to_user_id") val replied_to_user_id : String,
    @SerializedName("created_at") val created_at : String,
    @SerializedName("user") val user : User,
    @SerializedName("replied_to_user") val replied_to_user : String,
    @SerializedName("sub_mention_comments") val mention_comments : ArrayList<MentionUser>? = null

): Serializable



data class ShortPositionData(val buttonX: Float, val buttonY: Float, val buttonWidth: Int, val buttonHeight: Int)

