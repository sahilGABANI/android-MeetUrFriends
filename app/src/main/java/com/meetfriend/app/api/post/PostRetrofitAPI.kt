package com.meetfriend.app.api.post

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.livestreaming.model.LiveJoinResponse
import com.meetfriend.app.api.post.model.CreatePostRequest
import com.meetfriend.app.api.post.model.EditShortRequest
import com.meetfriend.app.api.post.model.HashTagResponse
import com.meetfriend.app.api.post.model.MentionUserRequest
import com.meetfriend.app.api.post.model.PostHashTagResponses
import com.meetfriend.app.api.post.model.PostIdRequest
import com.meetfriend.app.api.post.model.PostResponses
import com.meetfriend.app.api.post.model.ReportLiveRequest
import com.meetfriend.app.api.post.model.ShortsCountRequest
import com.meetfriend.app.api.post.model.VideoResponse
import com.meetfriend.app.api.post.model.ViewPostRequest
import com.meetfriend.app.api.post.model.WaterMarkVideoRequest
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponses
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.responseclasses.share.ShareCountResponse
import com.meetfriend.app.responseclasses.share.ShareFileResponse
import com.meetfriend.app.responseclasses.video.Post
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PostRetrofitAPI {

    @FormUrlEncoded
    @POST("hashtag/posts")
    fun hashTagsPosts(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("search") search: String? = null,
        @Field("hashtag_id") hashtagId: Int
    ): Single<MeetFriendResponse<PostHashTagResponses<PostResponses>>>

    @FormUrlEncoded
    @POST("hashtag/shorts")
    fun hashTagsShorts(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("search") search: String? = null,
        @Field("hashtag_id") hashtagId: Int
    ): Single<MeetFriendResponse<VideoResponse>>

    @POST("post/create_cloude_v4")
    fun createPost(@Body createPostRequest: CreatePostRequest): Single<MeetFriendCommonResponse>

    @POST("post/view")
    fun viewPost(@Body viewPostRequest: ViewPostRequest): Single<MeetFriendResponse<Post>>

    @GET("home/posts")
    fun getPosts(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("search") search: String
    ): Single<MeetFriendResponse<PostResponses>>

    @FormUrlEncoded
    @POST("post/like-dislike")
    fun postLikeUnlike(
        @Field("post_id") postId: Int,
        @Field("post_status") postStatus: String
    ): Single<MeetFriendCommonResponse>

    @FormUrlEncoded
    @POST("post/share")
    fun postShare(
        @Field("post_id") postId: Int,
        @Field("privacy") privacy: String,
        @Field("content") content: String,
        @Field("mention_ids") mentionIds: String?
    ): Single<MeetFriendCommonResponse>

    @FormUrlEncoded
    @POST("post/report-or-hide")
    fun reportOrHidePost(
        @Field("post_id") postId: Int,
        @Field("content") content: String,
        @Field("type") type: String
    ): Single<MeetFriendCommonResponse>

    @FormUrlEncoded
    @POST("post/delete")
    fun deletePost(@Field("post_id") postId: Int): Single<MeetFriendCommonResponse>

    @POST("post/view-by-user")
    fun postViewByUser(@Query("post_id") postId: String): Single<MeetFriendCommonResponse>

    @POST("api/post/share-count")
    fun getShareCount(@Body request: PostIdRequest): Single<MeetFriendResponse<ShareCountResponse>>

    @POST("post/share-video")
    fun getWaterMarkVideo(@Body request: WaterMarkVideoRequest): Single<ShareFileResponse>

    @GET("home/postVideos")
    fun getVideos(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("search") search: String,
        @Query("is_following") isFollowing: Int
    ): Single<MeetFriendResponse<VideoResponse>>

    @POST("post/short-count")
    fun updateShortsCount(
        @Body shortsCountRequest: ShortsCountRequest
    ): Single<MeetFriendCommonResponses>

    @POST("story/create")
    fun addStory(
        @Body addStoryRequest: AddStoryRequest
    ): Single<MeetFriendCommonResponse>

    @POST("live/live-report")
    fun reportLiveStreaming(@Body reportLiveRequest: ReportLiveRequest): Single<MeetFriendResponse<LiveJoinResponse>>

    @POST("user/mention-follow")
    fun mentionUser(@Body mentionUserRequest: MentionUserRequest): Single<MeetFriendResponse<List<MeetFriendUser>>>

    @POST("hashtag/get")
    fun getHashTagList(@Query("search") search: String): Single<MeetFriendResponse<HashTagResponse>>

    @POST("post/edit_short")
    fun editShort(@Body editShortRequest: EditShortRequest): Single<MeetFriendCommonResponse>
}
