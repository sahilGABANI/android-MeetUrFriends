package com.meetfriend.app.api.search

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query



interface SearchRetrofitAPI {
    @GET("friend/search")
    fun searchUsers(@Query("search") search: String, @Query("page") page: Int, @Query("per_page") perPage: Int): Single<MeetFriendResponse<MeetFriendResponseForChat<ArrayList<MeetFriendUser>>>>
}