package com.meetfriend.app.api.userprofile

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.post.model.PostResponses
import com.meetfriend.app.api.userprofile.model.ReportUser
import com.meetfriend.app.api.userprofile.model.ReportUserRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.responseclasses.UpdatePhotoResponse
import com.meetfriend.app.responseclasses.photos.UserPhotosResponse
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.*

interface UserProfileRetrofitAPI {

    @FormUrlEncoded
    @POST("auth/view-profile")
    fun viewProfile(
        @Field("user_id") userId : Int
    ): Single<MeetFriendResponse<MeetFriendUser>>

    @FormUrlEncoded
    @POST("auth/edit-profile")
    fun editProfileInfo(
        @Field("firstName") firstName : String?,
        @Field("lastName") lastName : String?,
        @Field("userName") userName : String?,
        @Field("education") education : String?,
        @Field("gender") gender : String?,
        @Field("city") city : String?,
        @Field("hobbies") hobbies : String?,
        @Field("work") work : String?,
        @Field("dob") dob : String?,
        @Field("bio") bio : String?,
        @Field("relationship") relationship : String?,
        @Field("dob_string") dob_string : String?,
        @Field("is_private") isPrivate : Int?
    ): Single<MeetFriendResponse<MeetFriendUser>>

    @POST("auth/upload-profile-photo")
    fun uploadProfilePicture(@Body file: RequestBody): Single<UpdatePhotoResponse>

    @FormUrlEncoded
    @POST("user/like_photo_videos")
    fun userPhotosVideos(
        @Field("user_id") userId : Int,
        @Field("page") page: Int,
        @Field("per_page") perPage: Int
    ): Single<UserPhotosResponse>

    @FormUrlEncoded
    @POST("user/videos")
    fun userVideos(
        @Field("user_id") userId : Int,
        @Field("page") page: Int,
        @Field("per_page") perPage: Int
    ): Single<UserPhotosResponse>

    @FormUrlEncoded
    @POST("user/posts")
    fun userPosts(
        @Field("user_id") userId : Int,
        @Field("page") page: Int,
        @Field("per_page") perPage: Int
    ):  Single<MeetFriendResponse<PostResponses>>

    @FormUrlEncoded
    @POST("friend/block-unblock-friend")
    fun blockUnBlockPeople(
        @Field("friend_id") friendId :Int ,@Field("block_status") blockStatus : String
    ): Single<MeetFriendCommonResponse>

    @POST("user/report-user")
    fun reportUser(@Body reportUserRequest: ReportUserRequest): Single<MeetFriendResponse<ReportUser>>
}