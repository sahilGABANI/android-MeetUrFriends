package com.meetfriend.app.api.places

import com.meetfriend.app.api.places.model.PlaceSearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface PlaceSearch {
    @GET("api/place/textsearch/json")
    fun getPlacesLists(@Query("key") key: String, @Query("query") type: String, @Query("location") location: String, @Query("radius") radius: Int): Call<PlaceSearchResponse>
}