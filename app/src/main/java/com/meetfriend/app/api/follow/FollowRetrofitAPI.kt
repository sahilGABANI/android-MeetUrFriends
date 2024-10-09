package com.meetfriend.app.api.follow

import com.meetfriend.app.api.follow.model.*
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single
import retrofit2.http.*

interface FollowRetrofitAPI {

    @POST("user/following")
    fun getFollowing(
        @Body getFollowingRequest: GetFollowingRequest
    ): Single<MeetFriendResponse<FollowResult>>


    @POST("hashtag/get/users")
    fun getHashtagUsers(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("search") search: String? = null
    ): Single<MeetFriendResponse<FollowResult>>

    @POST("user/followers")
    fun getFollowers(
        @Body getFollowersRequest: GetFollowersRequest
    ): Single<MeetFriendResponse<FollowResult>>

    @POST("user/follow")
    fun followUnfollowUser(
        @Body followUnfollowRequest: FollowUnfollowRequest
    ): Single<MeetFriendCommonResponse>

    @GET("home/suggestion")
    fun getSuggestedUsers(
        @Query("search") search: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Single<MeetFriendResponse<FollowResult>>

    @FormUrlEncoded
    @POST("home/cancel-add-friend")
    fun cancelFriendRequest(
        @Field("friend_id") friendId: Int
    ): Single<MeetFriendCommonResponse>

    @GET("user/follow-request")
    fun getFollowRequests(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Single<MeetFriendResponse<FollowResult>>

    @POST("user/accept-request")
    fun acceptFollowRequest(
        @Body acceptFollowRequestRequest: AcceptFollowRequestRequest
    ): Single<MeetFriendCommonResponse>

    @POST("user/reject-request")
    fun rejectFollowRequest(
        @Body acceptFollowRequestRequest: AcceptFollowRequestRequest
    ): Single<MeetFriendCommonResponse>

    @POST("user/follow-each-other")
    fun getUserForLiveInvite(
        @Body getUserForLiveInviteRequest: GetUserForLiveInviteRequest
    ): Single<MeetFriendResponse<FollowResult>>

    @GET("friend/block-list")
    fun blockedUserList(
       @Query("per_page") perPage:Int,@Query("page") page: Int
    ): Single<MeetFriendResponse<BlockResult>>

    @FormUrlEncoded
    @POST("friend/block-unblock-friend")
    fun blockUnBlockUser(
        @Field ("friend_id") friendId: Int,
        @Field ("block_status") blockStatus: String,
    ): Single<MeetFriendCommonResponse>
}