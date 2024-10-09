package com.meetfriend.app.api.redeem

import com.meetfriend.app.api.redeem.model.RedeemHistoryData
import com.meetfriend.app.api.redeem.model.RedeemRequestRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RedeemRetrofitAPI {

    @POST("redeem/check")
    fun sendRedeemRequest(@Body request: RedeemRequestRequest): Single<MeetFriendCommonResponse>

    @POST("redeem/verify")
    fun verifyRedeemOTP(@Body request: RedeemRequestRequest): Single<MeetFriendCommonResponse>

    @GET("redeem/history")
    fun redeemHistory(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Single<MeetFriendResponse<RedeemHistoryData>>

    @GET("redeem/monetization-history")
    fun redeemMonetizationHistory(
        @Query("per_page") perPage: Int,
        @Query("page") page: Int
    ): Single<MeetFriendResponse<RedeemHistoryData>>
}