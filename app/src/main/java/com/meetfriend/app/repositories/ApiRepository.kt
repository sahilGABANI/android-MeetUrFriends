package com.meetfriend.app.repositories

import com.meetfriend.app.network.WebService
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.responseclasses.CommonResponseClass
import com.meetfriend.app.responseclasses.DeviceDataResponse
import com.meetfriend.app.responseclasses.RegisterResponseClass
import com.meetfriend.app.responseclasses.settings.SecurityManagementResult
import io.reactivex.Single

class ApiRepository(private val webService: WebService) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()
    fun deviceToken(data: Map<String, Any>): Single<MeetFriendResponse<DeviceDataResponse>> {
        return webService.getDeviceToken(data).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun forgotPassword(
        data: HashMap<String, Any>
    ): Single<MeetFriendResponse<CommonResponseClass>> {
        return webService.forgotPassword(data).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun register(
        data: HashMap<String, Any>
    ): Single<MeetFriendResponse<RegisterResponseClass>> {
        return webService.register(data).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun resetPassword(
        data: HashMap<String, Any>
    ): Single<MeetFriendResponse<CommonResponseClass>> {
        return webService.resetPassword(data).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun fetchSecurity(data: Map<String, Any>): Single<MeetFriendResponse<SecurityManagementResult>> {
        return webService.securityManagement(data)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun deleteSecurity(
        data: HashMap<String, Any>
    ): Single<MeetFriendResponse<CommonResponseClass>> {
        return webService.deleteSecurity(data).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }
}
