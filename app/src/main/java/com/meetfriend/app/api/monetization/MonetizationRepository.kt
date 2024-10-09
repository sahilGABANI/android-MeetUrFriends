package com.meetfriend.app.api.monetization

import com.meetfriend.app.api.monetization.model.*
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single

class MonetizationRepository(private val monetizationRetrofitAPI: MonetizationRetrofitAPI,
) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun createHubRequest(request: SendHubRequestRequest): Single<MeetFriendResponse<HubRequestInfo>> {
        return monetizationRetrofitAPI.createHubRequest(request).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getEarningList(request: EarningListRequest): Single<MeetFriendResponse<EarningAmountInfo>> {
        return monetizationRetrofitAPI.getEarningList(request).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getExchangeRate(): Single<ExchangeRateResponse> {
        return monetizationRetrofitAPI.getExchangeRate()
    }

}