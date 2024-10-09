package com.meetfriend.app.api.hashtag

import com.meetfriend.app.api.hashtag.model.GetCountryHashtagRequest
import com.meetfriend.app.api.hashtag.model.HashtagResponse
import com.meetfriend.app.api.hashtag.model.ReportHashtagRequest
import com.meetfriend.app.api.hashtag.model.ReportHashtagResponse
import com.meetfriend.app.api.post.model.PostHashTagResponses
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single

class HashtagRepository(private val hashtagRetrofitAPI: HashtagRetrofitAPI) {
    private val meetFriendResponseConverter: MeetFriendResponseConverter = MeetFriendResponseConverter()

    fun getHashtagDetails(hashTagId: Int): Single<MeetFriendResponse<HashtagResponse>> {
        return hashtagRetrofitAPI.getHashtagDetails(hashTagId).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getHashtagList(page: Int, perPage: Int, search: String? = null): Single<MeetFriendResponse<PostHashTagResponses<ArrayList<HashtagResponse>>>> {
        return hashtagRetrofitAPI.getHashtagList(page, perPage, search).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getCountryHashtagList(countryCode:String ,page: Int, perPage: Int): Single<MeetFriendResponse<PostHashTagResponses<ArrayList<HashtagResponse>>>> {
        return hashtagRetrofitAPI.getCountryHashtagList(GetCountryHashtagRequest(countryCode) ,page, perPage).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun reportHashtagInfo(reportHashtagRequest: ReportHashtagRequest): Single<MeetFriendResponse<ReportHashtagResponse>> {
        return hashtagRetrofitAPI.reportHashtagInfo(reportHashtagRequest).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun blockHashtagInfo(reportHashtagRequest: ReportHashtagRequest): Single<MeetFriendResponse<ReportHashtagResponse>> {
        return hashtagRetrofitAPI.blockHashtagInfo(reportHashtagRequest).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }
}