package com.meetfriend.app.responseclasses.share

import com.google.gson.annotations.SerializedName

data class ShareFileResponse (
    @SerializedName("status") val status : Boolean,
    @SerializedName("message") val message : String,
    @SerializedName("watermark_media_url") val watermark_media_url : String
)