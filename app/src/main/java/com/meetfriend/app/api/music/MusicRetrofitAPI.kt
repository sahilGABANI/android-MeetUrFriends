package com.meetfriend.app.api.music

import com.meetfriend.app.api.music.model.MusicResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicRetrofitAPI {
    @GET("http://44.212.163.52:3000/api/search/songs")
    fun getMusicList(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("query", encoded = true) query: String? = "latestsongs"
    ): Single<MusicResponse>
}