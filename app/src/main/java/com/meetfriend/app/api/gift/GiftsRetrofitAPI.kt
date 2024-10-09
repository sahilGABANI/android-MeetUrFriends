package com.meetfriend.app.api.gift

import com.meetfriend.app.api.gift.model.*
import com.meetfriend.app.api.monetization.model.EarningListRequest
import com.meetfriend.app.api.monetization.model.SendCoinRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonStoryResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single
import retrofit2.http.*

interface GiftsRetrofitAPI {

    @GET("gift/index")
    fun getGifts(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Single<MeetFriendResponse<GiftsResponse>>

    @GET("coins/myearning")
    fun getMyEarning(): Single<MeetFriendResponse<MyEarningInfo>>

    @FormUrlEncoded
    @POST("coins/gift")
    fun sendGiftPost(
        @Field("to_id") toId: String,
        @Field("post_id") postId: String,
        @Field("coins") coins: Double,
        @Field("gift_id") giftId: Int,
    ): Single<MeetFriendCommonResponse>

    @GET("common/coins-plan")
    fun getCoinPlans(): Single<MeetFriendResponse<List<CoinPlanInfo>>>

    @FormUrlEncoded
    @POST("coins/purchase")
    fun coinPurchase(
        @Field("coins") coins: String,
        @Field("transaction_id") transactionId: String,
        @Field("plan_name") planName: String,
        @Field("price") price: String,
    ): Single<MeetFriendCommonResponse>

    @POST("coins/gift-send-transaction")
    fun getSendGiftTransaction(@Body request: EarningListRequest): Single<MeetFriendCommonStoryResponse<GiftTransaction>>

    @POST("coins/gift-recieved-transaction")
    fun getReceivedGiftTransaction(@Body request: EarningListRequest): Single<MeetFriendCommonStoryResponse<GiftTransaction>>

    @POST("coins/send-coin")
    fun sendCoins(@Body request: SendCoinRequest): Single<MeetFriendCommonResponse>

    @FormUrlEncoded
    @POST("coins/game-gift")
    fun sendGiftInLive(
        @Field("to_id") toId: String,
        @Field("post_id") postId: String,
        @Field("coins") coins: Double,
        @Field("gift_id") giftId: Int,
        @Field("quantity") quantity: Int,
    ): Single<MeetFriendCommonResponse>

    @POST("coins/week-transaction-history")
    fun giftWeeklySummary(
        @Body request: EarningListRequest
    ): Single<MeetFriendCommonStoryResponse<GiftWeeklyInfo>>
}
