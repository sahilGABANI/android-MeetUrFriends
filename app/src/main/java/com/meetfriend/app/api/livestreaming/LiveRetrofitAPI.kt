package com.meetfriend.app.api.livestreaming

import com.meetfriend.app.api.livestreaming.model.CheckCoHostFollowRequest
import com.meetfriend.app.api.livestreaming.model.CoHostFollowInfo
import com.meetfriend.app.api.livestreaming.model.CoHostRequests
import com.meetfriend.app.api.livestreaming.model.CoinCentsInfo
import com.meetfriend.app.api.livestreaming.model.CreateLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.EndLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.JoinLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.api.livestreaming.model.LiveEventWatchingUserRequest
import com.meetfriend.app.api.livestreaming.model.LiveJoinResponse
import com.meetfriend.app.api.livestreaming.model.RejectJoinRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForLive
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LiveRetrofitAPI {

    @POST("live/create")
    fun createLiveEvent(@Body request: CreateLiveEventRequest): Single<MeetFriendResponseForChat<LiveEventInfo>>

    @POST("live/join")
    fun joinLiveEvent(@Body request: JoinLiveEventRequest): Single<MeetFriendResponseForChat<LiveEventInfo>>

    @POST("live/end")
    fun endLiveEvent(@Body request: EndLiveEventRequest): Single<MeetFriendCommonResponse>

    @GET("live/active")
    fun getAllActiveLiveEvent(): Single<MeetFriendResponseForLive<LiveEventInfo>>

    @POST("live/publisher")
    fun inviteOrRejectCoHosts(@Body request: CoHostRequests): Single<MeetFriendCommonResponse>

    @POST("live/reject")
    fun rejectCoHosts(@Body request: RejectJoinRequest): Single<MeetFriendCommonResponse>

    @POST("live/join_users")
    fun liveJoinUser(
        @Body request: LiveEventWatchingUserRequest
    ): Single<MeetFriendResponseForChat<List<LiveJoinResponse>>>

    @POST("user/follow-check")
    fun checkCoHostFollow(@Body request: CheckCoHostFollowRequest): Single<MeetFriendResponse<CoHostFollowInfo>>

    @GET("coins/get-coin-cents")
    fun getCoinInCents(): Single<MeetFriendResponse<CoinCentsInfo>>
}
