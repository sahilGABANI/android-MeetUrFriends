package com.meetfriend.app.api.chat

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.chat.model.AgoraSDKVoiceCallTokenInfo
import com.meetfriend.app.api.chat.model.AgoraVoiceCallChannelInfo
import com.meetfriend.app.api.chat.model.AgoraVoiceCallChannelInfoRequest
import com.meetfriend.app.api.chat.model.ChatRoom
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomMessage
import com.meetfriend.app.api.chat.model.ChatRoomUser
import com.meetfriend.app.api.chat.model.CreateChatRoomRequest
import com.meetfriend.app.api.chat.model.CreateOneToOneChatRequest
import com.meetfriend.app.api.chat.model.GetChatMessageRequest
import com.meetfriend.app.api.chat.model.GetChatRoomAdminRequest
import com.meetfriend.app.api.chat.model.GetMentionUserRequest
import com.meetfriend.app.api.chat.model.GetMiceAccessRequest
import com.meetfriend.app.api.chat.model.MiceAccessInfo
import com.meetfriend.app.api.chat.model.ReportUserInfo
import com.meetfriend.app.api.chat.model.ReportUserRequest
import com.meetfriend.app.api.chat.model.RestrictUserRequest
import com.meetfriend.app.api.chat.model.UpdateChatRoomRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonStoryResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import com.meetfriend.app.ui.storywork.models.ResultListResult
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatRetrofitAPI {

    @POST("room/create")
    fun createChatRoom(
        @Body createChatRoomRequest: CreateChatRoomRequest
    ): Single<MeetFriendResponseForChat<ChatRoomInfo>>

    @GET("room/private")
    fun getPrivateChatRoom(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Single<MeetFriendResponse<ChatRoom>>

    @GET("room/public")
    fun getPublicChatRoom(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Single<MeetFriendResponse<ChatRoom>>

    @POST("room/conversation")
    fun getChatRoomMessage(
        @Body getChatMessageRequest: GetChatMessageRequest,
        @Query("page") pageNo: Int,
    ): Single<MeetFriendResponse<ChatRoomMessage>>

    @GET("room/get/{id}")
    fun chatRoomInfo(@Path("id") conversationId: Int): Single<MeetFriendResponseForChat<ChatRoomInfo>>

    @POST("room/call")
    fun getAgoraChannelName(
        @Body agoraVoiceCallChannelInfoRequest: AgoraVoiceCallChannelInfoRequest,
    ): Single<MeetFriendResponseForChat<AgoraVoiceCallChannelInfo>>

    @GET("https://meeturfriends.herokuapp.com/rtc/{channel_name}/{token_role}/uid/{uid}/")
    fun getAgoraCallToken(
        @Path("channel_name") channelName: String,
        @Path("token_role") tokenRole: Int,
        @Path("uid") userId: Int,
    ): Single<AgoraSDKVoiceCallTokenInfo>

    @POST("room/create-chat")
    fun createOneToOneChat(
        @Body createOneToOneChatRequest: CreateOneToOneChatRequest,
    ): Single<MeetFriendResponseForChat<ChatRoomInfo>>

    @POST("room/update/{conversation_id}")
    fun updateRoom(
        @Path(
            "conversation_id"
        ) conversationId: Int,
        @Body updateChatRoomRequest: UpdateChatRoomRequest
    ): Single<MeetFriendResponseForChat<ChatRoomInfo>>

    @GET("room/one-to-one")
    fun getOneToOneChatRoom(
        @Query("per_page") perPage:Int,
        @Query("page") page: Int,
    ): Single<MeetFriendResponse<ChatRoom>>

    @GET("room/my-private")
    fun getMyPrivateChatRoom(
        @Query("per_page") perPage:Int,
        @Query("page") page: Int,
    ): Single<MeetFriendResponse<ChatRoom>>

    @DELETE("room/chat/{conversation_id}")
    fun deleteChatRoom(
        @Path("conversation_id") conversationId: Int
    ): Single<MeetFriendCommonResponse>

    @POST("room/public-admin")
    fun getChatRoomAdmin(
        @Body getChatRoomAdminRequest: GetChatRoomAdminRequest
    ): Single<MeetFriendResponseForChat<List<ChatRoomUser>>>

    @POST("room/mention")
    fun getUserForMention(
        @Body getMentionUserRequest: GetMentionUserRequest
    ): Single<MeetFriendResponse<List<MeetFriendUser>>>

    @POST("room/restrict")
    fun restrictUser(
        @Body restrictUserRequest: RestrictUserRequest
    ): Single<MeetFriendCommonResponse>

    @POST("room/have_mice_users")
    fun getMiceAccessInfo(
        @Body getMiceAccessRequest: GetMiceAccessRequest
    ): Single<MeetFriendResponse<List<MiceAccessInfo>>>

    @POST("room/report-user")
    fun reportUser(@Body reportUserRequest: ReportUserRequest): Single<MeetFriendResponse<ReportUserInfo>>

    @POST("story/by_user")
    fun storyByUser(@Query("user_id") storyId: Int): Single<MeetFriendCommonStoryResponse<ResultListResult>>
}
