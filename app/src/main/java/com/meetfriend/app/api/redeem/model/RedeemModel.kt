package com.meetfriend.app.api.redeem.model

import com.google.gson.annotations.SerializedName

data class RedeemRequestRequest(

    @field:SerializedName("amount")
    val amount: String? = null,

    @field:SerializedName("paypal_email")
    val paypalEmail: String? = null,

    @field:SerializedName("otp")
    val otp: String? = null,

    @field:SerializedName("is_monetization")
    val isMonetization: Int? = null,

    @field:SerializedName("service_charge")
    val serviceCharge: Int? = null,

    )

data class RedeemHistoryData(
    @field:SerializedName("data")
    val data: List<RedeemHistoryInfo>? = null,
)

data class RedeemHistoryInfo(

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("user_id")
    val userId: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("amount")
    val amount: String? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("accept_date")
    val acceptDate: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    )