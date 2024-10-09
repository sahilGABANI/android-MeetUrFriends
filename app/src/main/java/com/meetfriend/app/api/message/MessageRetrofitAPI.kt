package com.meetfriend.app.api.message

import com.meetfriend.app.api.message.model.EditMessageRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST

interface MessageRetrofitAPI {

    @POST("room/chat-feature")
    fun deleteMessage(
        @Body editMessageRequest: EditMessageRequest
    ): Single<MeetFriendCommonResponse>

    @POST("room/chat-feature")
    fun editMessage(
        @Body editMessageRequest: EditMessageRequest
    ): Single<MeetFriendCommonResponse>
}