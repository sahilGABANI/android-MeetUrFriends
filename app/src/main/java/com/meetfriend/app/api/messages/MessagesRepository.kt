package com.meetfriend.app.api.messages

import com.meetfriend.app.api.chat.model.*
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import com.meetfriend.app.socket.SocketDataManager
import io.reactivex.Completable
import io.reactivex.Single

class MessagesRepository(
    private val messagesRetrofitAPI: MessagesRetrofitAPI,
    private val socketDataManager: SocketDataManager,
) {
    private val meetFriendResponseConverter: MeetFriendResponseConverter = MeetFriendResponseConverter()


    fun createChatRoom(createChatRoomRequest: CreateOneToOneChatRequest): Single<MeetFriendResponseForChat<ChatRoomInfo>> {
        return messagesRetrofitAPI.createChatRoom(createChatRoomRequest)
            .flatMap {
                meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(
                    it
                )
            }
    }

    fun joinRoom(joinRoomRequest: JoinRoomRequest): Completable {
        return socketDataManager.joinRoom(joinRoomRequest)
    }

    fun sendPrivateMessage(sendPrivateMessageRequest: SendPrivateMessageRequest): Completable {
        return socketDataManager.sendPrivateMessage(sendPrivateMessageRequest)
    }

    fun off(name: String) {
        socketDataManager.off(name)
    }

    fun getOneToOneChatRoom(perPage: Int, page: Int,search:String?): Single<MeetFriendResponse<ChatRoom>> {
        return messagesRetrofitAPI.getOneToOneChatRoom(perPage, page,search)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }


    fun deleteConversation(conversationId: Int): Single<MeetFriendCommonResponse> {
        return messagesRetrofitAPI.deleteChatRoom(conversationId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

}