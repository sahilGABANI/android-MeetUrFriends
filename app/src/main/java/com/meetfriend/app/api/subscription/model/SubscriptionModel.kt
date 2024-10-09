package com.meetfriend.app.api.subscription.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


enum class SubscriptionOption{
    ChatRoom,
    Admin
}

@Parcelize
data class TempPlanInfo(
    val coinImage:Int?,
    val roomType: String,
    val amount: String,
    val duration: String,
    var isSelected:Boolean,
    val month: Int,
) : Parcelable

data class SubscriptionRequest(

    @field:SerializedName("months")
    val month: Int,

    @field:SerializedName("transaction_id")
    val transactionId: String,

    @field:SerializedName("android_payload")
    val androidPayload: String,
)

data class AdminSubscriptionRequest(

    @field:SerializedName("months")
    val month: Int,

    @field:SerializedName("transaction_id")
    val transactionId: String,

    )