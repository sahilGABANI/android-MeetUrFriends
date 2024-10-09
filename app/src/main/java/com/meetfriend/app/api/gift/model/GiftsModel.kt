package com.meetfriend.app.api.gift.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.responseclasses.video.Links
import kotlinx.parcelize.Parcelize

@Parcelize
data class GiftsItemInfo(

    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("coins")
    var coins: Double? = null,

    @field:SerializedName("file_path")
    var file_path: String? = null,

    var isSend: Boolean = false,

    @field:SerializedName("quantity")
    var quantity: Int? = 1,

    @field:SerializedName("is_combo")
    var isCombo: Int? = 0,

    ) : Parcelable

data class MyEarningInfo(

    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("total_purchased_coins")
    var totalPurchasedCoins: Double? = null,

    @field:SerializedName("redeem_coins_limit")
    val redeemCoinsLimit: String? = null,

    @field:SerializedName("total_gift_recieved_coins")
    val totalGiftRecievedCoins: Double? = null,

    @field:SerializedName("total_gift_recieved_unpaid_coins")
    val totalGiftRecievedUnpaidCoins: Double? = null,

    @field:SerializedName("total_gift_send_coins")
    val totalGiftSendCoins: Double? = null,

    @field:SerializedName("total_current_coins")
    val totalCurrentCoins: Double? = null,

    @field:SerializedName("user")
    val user: User? = null,

    )

data class GiftsResponse(
    @SerializedName("current_page") val current_page: Int,
    @SerializedName("data") val data: List<GiftsItemInfo>? = null,
    @SerializedName("first_page_url") val first_page_url: String,
    @SerializedName("from") val from: Int,
    @SerializedName("last_page") val last_page: Int,
    @SerializedName("last_page_url") val last_page_url: String,
    @SerializedName("links") val links: List<Links>,
    @SerializedName("next_page_url") val next_page_url: String? = null,
    @SerializedName("path") val path: String,
    @SerializedName("per_page") val per_page: Int,
    @SerializedName("prev_page_url") val prev_page_url: String? = null,
    @SerializedName("to") val to: Int,
    @SerializedName("total") val total: Int
)

@Parcelize
data class User(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("firstName")
    val firstName: String? = null,

    @field:SerializedName("lastName")
    val lastName: String? = null,

    @field:SerializedName("profile_photo")
    val profilePhoto: String? = null,

    @field:SerializedName("gender")
    var gender: String? = null,

    @field:SerializedName("userName")
    val userName: String? = null,

    @field:SerializedName("media_url")
    val mediaUrl: String? = null,

    @field:SerializedName("is_verified")
    val isVerified: Int? = null,

    ) : Parcelable

data class GiftTransactionInfo(
    @field:SerializedName("payment_status")
    var paymentStatus: Int? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("from_id")
    val fromId: Int? = null,

    @field:SerializedName("to_id")
    val toId: Int? = null,

    @field:SerializedName("coins")
    val coins: Double? = null,

    @field:SerializedName("coins_type")
    val coinsType: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("gift_id")
    val giftId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("gift_gallery")
    val giftGallery: GiftsItemInfo? = null,

    @field:SerializedName("from_user")
    val fromUser: User? = null
)

data class AcceptRejectGiftRequest(

    @field:SerializedName("msg_type")
    val msgType: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("request_status")
    val request_status: Int,

    @field:SerializedName("conversation_id")
    val conversation_id: Int,
)

data class GiftTransaction(
    @field:SerializedName("date")
    var date: String? = null,

    @field:SerializedName("data")
    var data: ArrayList<GiftTransactionInfo>? = null,

    )

data class GiftWeeklyInfo(
    @field:SerializedName("total_sent")
    var totalSent: Double? = null,

    @field:SerializedName("total_recieved")
    var totalRecieved: Double? = null,

    @field:SerializedName("start_date")
    var startDate: String? = null,

    @field:SerializedName("end_date")
    var endDate: String? = null,

    @field:SerializedName("total")
    var total: String? = null,

    @field:SerializedName("previous_coin_balance")
    var previousCoinBalance: Double? = null,

    @field:SerializedName("coin_balance")
    var coinBalance: Double? = null,

    )

sealed class GiftItemClickStates {
    data class GiftItemClick(val data: GiftsItemInfo) : GiftItemClickStates()
    data class SendGiftClick(val data: GiftsItemInfo) : GiftItemClickStates()
    data class SendGiftInChatClick(val data: GiftsItemInfo) : GiftItemClickStates()
    data class RequestGiftClick(val data: GiftsItemInfo) : GiftItemClickStates()
    data class SendGiftInGameClick(val data: GiftsItemInfo) : GiftItemClickStates()

    data class GiftItemComboClick(val data: GiftsItemInfo) : GiftItemClickStates()
    object  ComboSent : GiftItemClickStates()


}

enum class TransactionType {
    Received,
    Sent,
    Weekly
}
