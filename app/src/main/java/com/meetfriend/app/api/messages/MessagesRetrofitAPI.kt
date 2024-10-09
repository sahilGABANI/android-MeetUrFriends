package com.meetfriend.app.api.messages

import com.meetfriend.app.api.chat.model.ChatRoom
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.CreateChatRoomRequest
import com.meetfriend.app.api.chat.model.CreateOneToOneChatRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import io.reactivex.Single
import retrofit2.http.*

interface MessagesRetrofitAPI {

    @POST("room/create-normal-chat")
    fun createChatRoom(@Body createChatRoomRequest: CreateOneToOneChatRequest): Single<MeetFriendResponseForChat<ChatRoomInfo>>

    @GET("room/normal-one-to-one")
    fun getOneToOneChatRoom(
        @Query("per_page") perPage:Int,
        @Query("page") page: Int,
        @Query("search") search: String?,
    ): Single<MeetFriendResponse<ChatRoom>>


    @DELETE("room/normal-chat/{conversation_id}")
    fun deleteChatRoom(
        @Path("conversation_id") conversationId: Int
    ):Single<MeetFriendCommonResponse>
}