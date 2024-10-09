package com.meetfriend.app.responseclasses.share

import com.google.gson.annotations.SerializedName

data class ShareCountResponse (
    @SerializedName("status") val status : Boolean,
    @SerializedName("message") val message : String,
    @SerializedName("no_of_shared_count") val no_of_shared_count : Int
)