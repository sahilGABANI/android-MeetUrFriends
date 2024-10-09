package com.meetfriend.app.api.message

import com.meetfriend.app.api.message.model.EditMessageRequest
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.socket.SocketDataManager
import io.reactivex.Single


class MessageRepository(
    private val messageRetrofitAPI: MessageRetrofitAPI,
    private val socketDataManager: SocketDataManager,
) {
    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun deleteMessage(editMessageRequest: EditMessageRequest): Single<MeetFriendCommonResponse> {
        return messageRetrofitAPI.deleteMessage(editMessageRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }
}