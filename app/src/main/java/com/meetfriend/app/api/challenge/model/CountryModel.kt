package com.meetfriend.app.api.challenge.model

import com.google.gson.annotations.SerializedName

data class CountryModel(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("sortname")
    val sortName: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("phonecode")
    val phoneCode: Int,

    @field:SerializedName("status")
    val status: Int,

    @field:SerializedName("country_id")
    val countryId: Int?,

    @field:SerializedName("state_id")
    val stateId: Int?,

    var isSelected: Boolean = false
)
