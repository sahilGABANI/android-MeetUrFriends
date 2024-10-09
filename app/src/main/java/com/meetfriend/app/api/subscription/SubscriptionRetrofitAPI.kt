package com.meetfriend.app.api.subscription

import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.subscription.model.AdminSubscriptionRequest
import com.meetfriend.app.api.subscription.model.SubscriptionRequest
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface SubscriptionRetrofitAPI {

    @POST("room/update-subscription/{conversation_id}")
    fun updateSubscription(
        @Path("conversation_id") conversationId: Int,
        @Body subscriptionRequest: SubscriptionRequest,
    ): Single<MeetFriendResponseForChat<ChatRoomInfo>>

    @POST("room/update-admin-subscription/{conversation_id}")
    fun updateAdminSubscription(
        @Path("conversation_id") conversationId: Int,
        @Body adminSubscriptionRequest: AdminSubscriptionRequest,
    ): Single<MeetFriendResponseForChat<ChatRoomInfo>>
}
