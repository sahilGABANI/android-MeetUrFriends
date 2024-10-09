package com.meetfriend.app.api.gift.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CoinPlanInfo (
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("identifier")
    var identifier: String? = null,

    @field:SerializedName("plan_name")
    var planName: String? = null,

    @field:SerializedName("coins")
    var coins: String? = null,

    @field:SerializedName("amount")
    var amount: String? = null,

    @field:SerializedName("discount")
    var discount: Double? = null,

    @field:SerializedName("discount_coins")
    var discountCoins: Double? = null,

    @field:SerializedName("coins_with_discount")
    var coinsWithDiscount: Double? = null,

    @Expose(serialize = false, deserialize = false)
    var isSelected: Boolean = false,
)
