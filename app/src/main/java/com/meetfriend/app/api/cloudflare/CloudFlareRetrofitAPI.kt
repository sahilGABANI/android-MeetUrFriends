package com.meetfriend.app.api.cloudflare

import com.meetfriend.app.api.cloudflare.model.*
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.*

interface CloudFlareRetrofitAPI {
    @GET("room/api-token")
    fun getCloudFlareConfig(): Single<MeetFriendResponseForChat<CloudFlareConfig>>

    @Multipart
    @POST
    fun uploadImageToCloudFlare(
        @Url url: String?,
        @Header("Authorization") authToken: String?,
        @Part file: MultipartBody.Part,
    ): Single<UploadImageCloudFlareResponse>

    @Multipart
    @POST
    fun uploadVideoToCloudFlare(
        @Url url: String?,
        @Header("Authorization") authToken: String?,
        @Part file: MultipartBody.Part,
    ): Single<UploadVideoCloudFlareResponse>

    @GET
    fun getUploadVideoDetails(
        @Url url: String?,
        @Header("Authorization") authToken: String?,
    ): Single<CloudFlareVideoDetails>

    @GET
    fun getUploadVideoStatus(
        @Url url: String?,
        @Header("Authorization") authToken: String?,
    ): Single<CloudFlareVideoStatus>
}