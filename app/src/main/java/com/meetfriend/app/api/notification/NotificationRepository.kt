package com.meetfriend.app.api.notification

import com.meetfriend.app.api.notification.model.AcceptRejectRequestRequest
import com.meetfriend.app.api.notification.model.AppConfigInfo
import com.meetfriend.app.api.notification.model.NotificationInfo
import com.meetfriend.app.api.notification.model.VoipCallRequest
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForGetProfile
import io.reactivex.Single

class NotificationRepository(private val notificationRetrofitAPI: NotificationRetrofitAPI) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun getNotification(
        perPage: Int,
        page: Int
    ): Single<MeetFriendResponseForGetProfile<NotificationInfo>> {
        return notificationRetrofitAPI.getNotification(perPage, page)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForGetProfile(it) }
    }

    fun getNotificationCount(): Single<MeetFriendCommonResponse> {
        return notificationRetrofitAPI.getNotificationCount()
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun notificationMarkAllRead(): Single<MeetFriendCommonResponse> {
        return notificationRetrofitAPI.notificationMarkAllRead()
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun acceptRejectRequest(acceptRejectRequestRequest: AcceptRejectRequestRequest): Single<MeetFriendCommonResponse> {
        return notificationRetrofitAPI.acceptRejectRequest(acceptRejectRequestRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun startVoipCall(voipCallRequest: VoipCallRequest): Single<MeetFriendCommonResponse> {
        return notificationRetrofitAPI.startVoipCall(voipCallRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun deleteAllNotification(): Single<MeetFriendCommonResponse> {
        return notificationRetrofitAPI.deleteAllNotification()
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun deleteNotification(id: String): Single<MeetFriendCommonResponse> {
        return notificationRetrofitAPI.deleteNotification(id)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getAppConfig(): Single<MeetFriendResponse<List<AppConfigInfo>>> {
        return notificationRetrofitAPI.getAppConfig()
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }
}
