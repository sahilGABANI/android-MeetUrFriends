package com.meetfriend.app.api.monetization

import com.meetfriend.app.api.monetization.model.*
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MonetizationRetrofitAPI {

    @POST("hub/requests")
    fun createHubRequest(@Body request: SendHubRequestRequest): Single<MeetFriendResponse<HubRequestInfo>>

    @POST("earning/list")
    fun getEarningList(@Body request: EarningListRequest): Single<MeetFriendResponse<EarningAmountInfo>>

    @GET("https://open.er-api.com/v6/latest/USD")
    fun getExchangeRate(): Single<ExchangeRateResponse>

    @POST("redeem/request")
    fun sendRedeemReq():Single<MeetFriendCommonResponse>
}