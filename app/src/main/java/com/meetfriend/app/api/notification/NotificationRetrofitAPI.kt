package com.meetfriend.app.api.notification

import com.meetfriend.app.api.notification.model.AcceptRejectRequestRequest
import com.meetfriend.app.api.notification.model.AppConfigInfo
import com.meetfriend.app.api.notification.model.NotificationInfo
import com.meetfriend.app.api.notification.model.VoipCallRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForGetProfile
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationRetrofitAPI {

    @GET("notifications/index")
    fun getNotification(
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
    ): Single<MeetFriendResponseForGetProfile<NotificationInfo>>

    @GET("notifications/count-unread")
    fun getNotificationCount(): Single<MeetFriendCommonResponse>

    @GET("notifications/mark-all-read")
    fun notificationMarkAllRead(): Single<MeetFriendCommonResponse>

    @POST("room/accept-reject-request")
    fun acceptRejectRequest(
        @Body acceptRejectRequestRequest: AcceptRejectRequestRequest
    ): Single<MeetFriendCommonResponse>

    @POST("room/voip_call")
    fun startVoipCall(@Body voipCallRequest: VoipCallRequest): Single<MeetFriendCommonResponse>

    @POST("notifications/delete-all")
    fun deleteAllNotification(): Single<MeetFriendCommonResponse>

    @FormUrlEncoded
    @POST("notifications/delete")
    fun deleteNotification(
        @Field("id") id: String
    ): Single<MeetFriendCommonResponse>

    @GET("general-settings/app-config")
    fun getAppConfig(): Single<MeetFriendResponse<List<AppConfigInfo>>>
}
