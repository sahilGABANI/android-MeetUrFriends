package com.meetfriend.app.network

import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.responseclasses.CommonResponseClass
import com.meetfriend.app.responseclasses.DeviceDataResponse
import com.meetfriend.app.responseclasses.RegisterResponseClass
import com.meetfriend.app.responseclasses.settings.SecurityManagementResult
import io.reactivex.Single
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap

/**
 *  All web services are declared here
 */
@JvmSuppressWildcards
interface WebService {

    @FormUrlEncoded
    @POST("auth/register")
    fun register(
        @FieldMap data: HashMap<String, @JvmSuppressWildcards Any>
    ): Single<MeetFriendResponse<RegisterResponseClass>>

    @FormUrlEncoded
    @POST("auth/forgot-password")
    fun forgotPassword(
        @FieldMap data: HashMap<String, @JvmSuppressWildcards Any>
    ): Single<MeetFriendResponse<CommonResponseClass>>

    @POST("auth/device-data")
    fun getDeviceToken(@QueryMap data: Map<String, Any>): Single<MeetFriendResponse<DeviceDataResponse>>

    @FormUrlEncoded
    @POST("auth/reset-password")
    fun resetPassword(
        @FieldMap data: HashMap<String, @JvmSuppressWildcards Any>
    ): Single<MeetFriendResponse<CommonResponseClass>>

    @GET("security-management/index")
    fun securityManagement(@QueryMap data: Map<String, Any>): Single<MeetFriendResponse<SecurityManagementResult>>

    @FormUrlEncoded
    @POST("security-management/delete")
    fun deleteSecurity(
        @FieldMap data: HashMap<String, @JvmSuppressWildcards Any>
    ): Single<MeetFriendResponse<CommonResponseClass>>
}
