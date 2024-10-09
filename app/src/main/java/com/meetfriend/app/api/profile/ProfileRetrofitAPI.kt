package com.meetfriend.app.api.profile

import com.meetfriend.app.api.authentication.model.DeviceIdRequest
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.profile.model.ChangeProfileRequest
import com.meetfriend.app.api.profile.model.ChatRoomUserProfileInfo
import com.meetfriend.app.api.profile.model.CheckUserRequest
import com.meetfriend.app.api.profile.model.CreateChatRoomUserRequest
import com.meetfriend.app.api.profile.model.FeedbackInfo
import com.meetfriend.app.api.profile.model.GetProfileInfoRequest
import com.meetfriend.app.api.profile.model.HelpFormInfo
import com.meetfriend.app.api.profile.model.ProfileValidationInfo
import com.meetfriend.app.api.profile.model.SendFeedbackRequest
import com.meetfriend.app.api.profile.model.SendHelpRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForGetProfile
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileRetrofitAPI {

    @POST("room/chat-name")
    fun createChatRoomUser(@Body createChatUserNameRequest: CreateChatRoomUserRequest): Single<MeetFriendCommonResponse>

    @POST("room/change/profile")
    fun changeProfile(@Body changeProfileRequest: ChangeProfileRequest): Single<MeetFriendCommonResponse>

    @GET("room/user/profile")
    fun getChatRoomUserProfile(
        @Query("per_page") perPage: Int
    ): Single<MeetFriendResponseForGetProfile<ChatRoomUserProfileInfo>>

    @GET("room/user/profile/{userId}")
    fun getOtherUserProfile(
        @Path("userId") userId: Int,
        @Query("per_page") perPage: Int,
    ): Single<MeetFriendResponseForGetProfile<ChatRoomUserProfileInfo>>

    @DELETE("room/profile/{id}")
    fun deleteProfileImage(@Path("id") id: Int): Single<MeetFriendCommonResponse>

    @POST("feedback")
    fun sendFeedback(@Body sendFeedbackRequest: SendFeedbackRequest): Single<MeetFriendResponseForChat<FeedbackInfo>>

    @POST("help")
    fun sendHelp(@Body sendHelpRequest: SendHelpRequest): Single<MeetFriendResponseForChat<HelpFormInfo>>

    @POST("user/delete-account")
    fun deleteAccount(@Body deleteAccountRequest: FollowUnfollowRequest): Single<MeetFriendCommonResponse>

    @POST("profile-info")
    fun getProfileInfo(
        @Body getProfileInfoRequest: GetProfileInfoRequest
    ): Single<MeetFriendResponse<ProfileValidationInfo>>

    @POST("auth/check-user")
    fun checkUser(@Body checkUserRequest: CheckUserRequest): Single<MeetFriendCommonResponse>

    @POST("auth/logout-all")
    fun logOutAll(@Body checkUserRequest: DeviceIdRequest): Single<MeetFriendCommonResponse>

    @GET("auth/logout")
    fun logOut(@Query("device_id") deviceId: String): Single<MeetFriendResponse<MeetFriendUser>>
}
