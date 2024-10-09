package com.meetfriend.app.api.hashtag

import com.meetfriend.app.api.hashtag.model.GetCountryHashtagRequest
import com.meetfriend.app.api.hashtag.model.HashtagResponse
import com.meetfriend.app.api.hashtag.model.ReportHashtagRequest
import com.meetfriend.app.api.hashtag.model.ReportHashtagResponse
import com.meetfriend.app.api.post.model.PostHashTagResponses
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface HashtagRetrofitAPI {

    @GET("hashtag/get/{tag_id}")
    fun getHashtagDetails(@Path("tag_id") tagId: Int): Single<MeetFriendResponse<HashtagResponse>>

    @POST("hashtag/get")
    fun getHashtagList(@Query("page") page: Int, @Query("per_page") perPage: Int, @Query("search") search: String?): Single<MeetFriendResponse<PostHashTagResponses<ArrayList<HashtagResponse>>>>

    @POST("hashtag/get/country_code/")
    fun getCountryHashtagList(@Body getCountryHashtagRequest : GetCountryHashtagRequest, @Query("page") page: Int, @Query("per_page") perPage: Int,): Single<MeetFriendResponse<PostHashTagResponses<ArrayList<HashtagResponse>>>>

    @POST("hashtag/report")
    fun reportHashtagInfo(@Body reportHashtagRequest: ReportHashtagRequest): Single<MeetFriendResponse<ReportHashtagResponse>>

    @POST("hashtag/block")
    fun blockHashtagInfo(@Body reportHashtagRequest: ReportHashtagRequest): Single<MeetFriendResponse<ReportHashtagResponse>>
}