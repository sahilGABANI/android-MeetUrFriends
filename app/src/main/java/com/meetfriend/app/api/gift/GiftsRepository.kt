package com.meetfriend.app.api.gift

import com.meetfriend.app.api.gift.model.*
import com.meetfriend.app.api.monetization.model.EarningListRequest
import com.meetfriend.app.api.monetization.model.SendCoinRequest
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonStoryResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single

class GiftsRepository(private val giftsRetrofitAPI: GiftsRetrofitAPI) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun getGifts(page: Int, perPage: Int): Single<MeetFriendResponse<GiftsResponse>> {
        return giftsRetrofitAPI.getGifts(page, perPage)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getMyEarning(): Single<MeetFriendResponse<MyEarningInfo>> {
        return giftsRetrofitAPI.getMyEarning()
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun sendGiftPost(
        toId: String,
        postId: String,
        coins: Double,
        giftId: Int,
    ): Single<MeetFriendCommonResponse> {
        return giftsRetrofitAPI.sendGiftPost(toId, postId, coins, giftId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getCoinPlans(): Single<MeetFriendResponse<List<CoinPlanInfo>>> {
        return giftsRetrofitAPI.getCoinPlans()
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun coinPurchase(
        coins: String,
        transactionId: String,
        planName: String,
        price: String,
    ): Single<MeetFriendCommonResponse> {
        return giftsRetrofitAPI.coinPurchase(coins, transactionId, planName, price)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getSendGiftTransaction(request: EarningListRequest): Single<MeetFriendCommonStoryResponse<GiftTransaction>> {
        return giftsRetrofitAPI.getSendGiftTransaction(request)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForStory(it) }
    }

    fun getReceivedGiftTransaction(request: EarningListRequest): Single<MeetFriendCommonStoryResponse<GiftTransaction>> {
        return giftsRetrofitAPI.getReceivedGiftTransaction(request)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForStory(it) }
    }

    fun SendCoins(request: SendCoinRequest): Single<MeetFriendCommonResponse> {
        return giftsRetrofitAPI.sendCoins(request)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun sendGiftInLive(
        toId: String,
        postId: String,
        coins: Double,
        giftId: Int,
        quantity:Int
    ): Single<MeetFriendCommonResponse> {
        return giftsRetrofitAPI.sendGiftInLive(toId, postId, coins, giftId,quantity)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun giftWeeklySummary(request: EarningListRequest): Single<MeetFriendCommonStoryResponse<GiftWeeklyInfo>> {
        return giftsRetrofitAPI.giftWeeklySummary(request)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForStory(it) }
    }

}