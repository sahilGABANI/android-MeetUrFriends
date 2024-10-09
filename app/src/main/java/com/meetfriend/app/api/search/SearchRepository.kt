package com.meetfriend.app.api.search

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import io.reactivex.Single

class SearchRepository(private val searchRetrofitAPI: SearchRetrofitAPI) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter = MeetFriendResponseConverter()

    fun searchUsers(
        search: String,
        page: Int,
        perPage: Int
    ): Single<MeetFriendResponse<MeetFriendResponseForChat<ArrayList<MeetFriendUser>>>> {
        return searchRetrofitAPI.searchUsers(search, page, perPage)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }
}