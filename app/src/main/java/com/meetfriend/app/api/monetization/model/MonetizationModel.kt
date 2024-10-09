package com.meetfriend.app.api.monetization.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

data class HubRequestInfo(

    @field:SerializedName("user_id")
    var userId: Int,

    @field:SerializedName("first_name")
    var firstName: String? = null,

    @field:SerializedName("last_name")
    var lastName: String? = null,

    @field:SerializedName("date_of_birth")
    var dateOfBirth: String? = null,

    @field:SerializedName("address")
    var address: String? = null,

    @field:SerializedName("email")
    var email: String? = null,

    @field:SerializedName("phone_no")
    var phoneNo: String? = null,

    @field:SerializedName("social_link")
    var socialLink: String? = null,

    @field:SerializedName("social_type")
    var socialType: String? = null,

    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("update_at")
    var updatedAt: String? = null,

    @field:SerializedName("created_at")
    var createdAt: String? = null,

    )

@Parcelize
data class SendHubRequestRequest(

    @field:SerializedName("first_name")
    var firstName: String? = null,

    @field:SerializedName("last_name")
    var lastName: String? = null,

    @field:SerializedName("date_of_birth")
    var dateOfBirth: String? = null,

    @field:SerializedName("address")
    var address: String? = null,

    @field:SerializedName("email")
    var email: String? = null,

    @field:SerializedName("phone_no")
    var phoneNo: String? = null,

    @field:SerializedName("social")
    var social: @RawValue Any? = null,

    @field:SerializedName("story_status")
    var storyStatus: Int? = 0,

    @field:SerializedName("post_status")
    var postStatus: Int? = 0,

    @field:SerializedName("short_status")
    var shortStatus: Int? = 0,

    @field:SerializedName("challenge_status")
    var challengeStatus: Int? = 0,

    @field:SerializedName("share_status")
    var shareStatus: Int? = 0,

    @field:SerializedName("live_status")
    var liveStatus: Int? = 0,

    @field:SerializedName("timezone")
    var timezone: String? = null,

    @field:SerializedName("country_code")
    var countryCode: String? = null,

    ) : Parcelable

enum class SocialType {
    @SerializedName("facebook")
    facebook,

    @SerializedName("instagram")
    instagram,

    @SerializedName("twitter")
    twitter,

    @SerializedName("tiktok")
    tiktok
}

data class EarningAmountInfo(
    @field:SerializedName("total_usd")
    var totalUsd: String? = null,

    @field:SerializedName("total_earning")
    var totalEarning: String? = null,

    @field:SerializedName("total_category_coins")
    var totalCategoryCoins: ArrayList<TargetInfo>? = null,

    @field:SerializedName("user")
    var user: MeetFriendUser? = null,

    @field:SerializedName("requset_status")
    var requsetStatus: String? = null,

    @field:SerializedName("accept_date")
    var acceptDate: String? = null,

    )

data class TargetInfo(
    @field:SerializedName("date")
    var date: String? = null,

    @field:SerializedName("target")
    var target: ArrayList<AmountData>? = null,
)
data class AmountData(
    @field:SerializedName("total_target")
    var totalTarget: Int? = null,

    @field:SerializedName("total_usd")
    var totalUsd: String? = null,

    @field:SerializedName("category_type")
    var categoryType: String? = null,

    @field:SerializedName("total_amount")
    var totalAmount: Int? = null,

    @field:SerializedName("total_object")
    var totalObject: String? = null,
)

data class EarningListRequest(
    @field:SerializedName("from_date")
    var fromDate: String? = null,

    @field:SerializedName("to_date")
    var toDate: String? = null,

    @field:SerializedName("timezone")
    var timezone: String? = null,
)

data class SendCoinRequest(
    @field:SerializedName("coins")
    var coins: String? = null,

    @field:SerializedName("to_id")
    var toId: String? = null,
)

@Parcelize
data class SocialLinkInfo(
    @field:SerializedName("social_type")
    var socialType: String? = null,

    @field:SerializedName("social_link")
    var socialLink: String? = null,
) :Parcelable

data class ExchangeRateResponse(
    val rates: Map<String, Double>
)
