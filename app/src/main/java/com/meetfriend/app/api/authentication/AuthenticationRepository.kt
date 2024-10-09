package com.meetfriend.app.api.authentication

import com.meetfriend.app.api.authentication.model.DeviceIdRequest
import com.meetfriend.app.api.authentication.model.LoginRequest
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.authentication.model.RegisterRequest
import com.meetfriend.app.api.authentication.model.SwitchDeviceAccountRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.socket.SocketDataManager
import contractorssmart.app.utilsclasses.PreferenceHandler
import io.reactivex.Single

class AuthenticationRepository(
    private val authenticationRetrofitAPI: AuthenticationRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache,
    private val socketDataManager: SocketDataManager
) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun login(loginRequest: LoginRequest): Single<MeetFriendResponse<MeetFriendUser>> {
        return authenticationRetrofitAPI.login(loginRequest)
            .doAfterSuccess { meetFriendResponse ->
                loggedInUserCache.setLoggedInUserToken(meetFriendResponse.accessToken)
                loggedInUserCache.setLoggedInUser(meetFriendResponse.result)
                loggedInUserCache.setProfileUpdatedStatus(meetFriendResponse.profileUpdatedStatus)
                loggedInUserCache.setUserFirstTimeLogin(true)

                PreferenceHandler.writeString(
                    MeetFriendApplication.context,
                    PreferenceHandler.AUTHORIZATION_TOKEN,
                    meetFriendResponse.accessToken ?: ""
                )

                socketDataManager.connect()
            }.flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun register(registerRequest: RegisterRequest): Single<MeetFriendResponse<MeetFriendUser>> {
        return authenticationRetrofitAPI.registerUser(registerRequest).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getListOfAccount(deviceIdRequest: DeviceIdRequest): Single<MeetFriendResponse<List<MeetFriendUser>>> {
        return authenticationRetrofitAPI.getListOfAccount(deviceIdRequest).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun switchAccount(loginRequest: SwitchDeviceAccountRequest): Single<MeetFriendResponse<MeetFriendUser>> {
        return authenticationRetrofitAPI.switchAccount(loginRequest)
            .doAfterSuccess { meetFriendResponse ->
                loggedInUserCache.setLoggedInUserToken(meetFriendResponse.accessToken)
                loggedInUserCache.setLoggedInUser(meetFriendResponse.result)
                loggedInUserCache.setProfileUpdatedStatus(meetFriendResponse.profileUpdatedStatus)
                loggedInUserCache.setUserFirstTimeLogin(true)

                PreferenceHandler.writeString(
                    MeetFriendApplication.context,
                    PreferenceHandler.AUTHORIZATION_TOKEN,
                    meetFriendResponse.accessToken ?: ""
                )

                socketDataManager.connect()
            }.flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }
}
