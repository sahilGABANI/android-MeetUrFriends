package com.meetfriend.app.api.subscription

import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.subscription.model.AdminSubscriptionRequest
import com.meetfriend.app.api.subscription.model.SubscriptionRequest
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import io.reactivex.Single

class SubscriptionRepository(private val subscriptionRetrofitAPI: SubscriptionRetrofitAPI) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter = MeetFriendResponseConverter()

    fun updateSubscription(
        conversationId: Int,
        subscriptionRequest: SubscriptionRequest
    ): Single<MeetFriendResponseForChat<ChatRoomInfo>> {
        return subscriptionRetrofitAPI.updateSubscription(conversationId, subscriptionRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun updateAdminSubscription(
        conversationId: Int,
        adminSubscriptionRequest: AdminSubscriptionRequest
    ): Single<MeetFriendResponseForChat<ChatRoomInfo>> {
        return subscriptionRetrofitAPI.updateAdminSubscription(conversationId, adminSubscriptionRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }
}
