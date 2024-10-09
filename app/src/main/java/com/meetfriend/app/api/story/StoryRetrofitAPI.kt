package com.meetfriend.app.api.story

import com.meetfriend.app.newbase.network.model.*
import com.meetfriend.app.ui.storywork.models.*
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface StoryRetrofitAPI {

    @Multipart
    @POST("api/story/create")
    fun addStory(
        @Part multipartTypedOutput: Array<MultipartBody.Part?>?,
        @Part("type") type: RequestBody
    ): Single<MeetFriendCommonResponse>

    @POST("story/getStoryDetail")
    fun getStories(@Query("user_id") id: String): Single<StoryDetailResponse>
    @GET("story/index")
    fun getStoriesList(
        @Query("page") pageNo: Int,
        @Query("per_page") perPageNo: Int
    ): Single<MeetFriendCommonStoryResponse<ResultListResult>>

    @POST("story/viewStory")
    fun viewStory(@Query("story_id") storyId: String): Single<MeetFriendCommonResponse>

    @POST("room/get-conversation-id")
    fun getConversationId(@Query("receiver_id") receiverId: Int): Single<MeetFriendCommonResponseForStory>

    @POST("story/deleteStory")
    fun deleteStory(@Query("id") id: String): Single<MeetFriendCommonResponse>

    @POST("story/report")
    fun reportStory(
        @Query("story_id") id: String,
        @Query("reason") reason: String
    ): Single<MeetFriendCommonResponse>
}