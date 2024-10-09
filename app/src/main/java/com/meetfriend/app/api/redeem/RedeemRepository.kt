package com.meetfriend.app.api.redeem

import com.meetfriend.app.api.redeem.model.RedeemHistoryData
import com.meetfriend.app.api.redeem.model.RedeemRequestRequest
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single

class RedeemRepository(private val redeemRetrofitAPI: RedeemRetrofitAPI) {
    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun sendRedeemRequest(request: RedeemRequestRequest): Single<MeetFriendCommonResponse> {
        return redeemRetrofitAPI.sendRedeemRequest(request)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun verifyRedeemOTP(request: RedeemRequestRequest): Single<MeetFriendCommonResponse> {
        return redeemRetrofitAPI.verifyRedeemOTP(request)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun redeemHistory(page:Int, perPage:Int): Single<MeetFriendResponse<RedeemHistoryData>> {
        return redeemRetrofitAPI.redeemHistory(page,perPage)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun redeemMonetizationHistory(perPage:Int,page:Int): Single<MeetFriendResponse<RedeemHistoryData>> {
        return redeemRetrofitAPI.redeemMonetizationHistory(perPage, page)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

}