package com.meetfriend.app.api.authentication

import com.meetfriend.app.api.authentication.model.DeviceIdRequest
import com.meetfriend.app.api.authentication.model.LoginRequest
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.authentication.model.RegisterRequest
import com.meetfriend.app.api.authentication.model.SwitchDeviceAccountRequest
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthenticationRetrofitAPI {

    @POST("auth/login")
    fun login(@Body loginRequest: LoginRequest): Single<MeetFriendResponse<MeetFriendUser>>

    @POST("auth/register")
    fun registerUser(@Body registerRequest: RegisterRequest): Single<MeetFriendResponse<MeetFriendUser>>

    @POST("get-accounts")
    fun getListOfAccount(@Body deviceIdRequest: DeviceIdRequest): Single<MeetFriendResponse<List<MeetFriendUser>>>

    @POST("user/switch-account")
    fun switchAccount(@Body request: SwitchDeviceAccountRequest): Single<MeetFriendResponse<MeetFriendUser>>
}
