package com.meetfriend.app.api.cloudflare.model

import com.google.gson.annotations.SerializedName

data class CloudFlareVideoDetails(

    @field:SerializedName("result")
    val result: CloudFlareVideoInfo
)

data class CloudFlareVideoInfo(
    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("thumbnail")
    val thumbnail: String? = null,

    @field:SerializedName("status")
    val status: Status? = null
)

data class Status(
    @field:SerializedName("state")
    val state: String? = null,

    @field:SerializedName("errorReasonCode")
    val errorReasonCode: String? = null,

    @field:SerializedName("errorReasonText")
    val errorReasonText: String? = null,
)

data class CloudFlareVideoStatus(

    @field:SerializedName("result")
    val result: Result? = null,

    @field:SerializedName("success")
    val success: Boolean? = null,

    @field:SerializedName("messages")
    val messages: List<Any?>? = null,

    @field:SerializedName("errors")
    val errors: List<Any?>? = null
)

data class Playback(

    @field:SerializedName("dash")
    val dash: String? = null,

    @field:SerializedName("hls")
    val hls: String? = null
)

data class Result(

    @field:SerializedName("preview")
    val preview: String? = null,

    @field:SerializedName("requireSignedURLs")
    val requireSignedURLs: Boolean? = null,

    @field:SerializedName("thumbnailTimestampPct")
    val thumbnailTimestampPct: Int? = null,

    @field:SerializedName("readyToStreamAt")
    val readyToStreamAt: Any? = null,

    @field:SerializedName("readyToStream")
    val readyToStream: Boolean? = null,

    @field:SerializedName("duration")
    val duration: Double? = null,

    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("allowedOrigins")
    val allowedOrigins: List<Any?>? = null,

    @field:SerializedName("maxSizeBytes")
    val maxSizeBytes: Any? = null,

    @field:SerializedName("uploaded")
    val uploaded: String? = null,

    @field:SerializedName("modified")
    val modified: String? = null,

    @field:SerializedName("playback")
    val playback: Playback? = null,

    @field:SerializedName("publicDetails")
    val publicDetails: PublicDetails? = null,

    @field:SerializedName("uploadExpiry")
    val uploadExpiry: String? = null,

    @field:SerializedName("creator")
    val creator: Any? = null,

    @field:SerializedName("thumbnail")
    val thumbnail: String? = null,

    @field:SerializedName("watermark")
    val watermark: Any? = null,

    @field:SerializedName("created")
    val created: String? = null,

    @field:SerializedName("input")
    val input: Input? = null,

    @field:SerializedName("clippedFrom")
    val clippedFrom: Any? = null,

    @field:SerializedName("size")
    val size: Int? = null,

    @field:SerializedName("meta")
    val meta: Meta? = null,

    @field:SerializedName("scheduledDeletion")
    val scheduledDeletion: Any? = null,

    @field:SerializedName("maxDurationSeconds")
    val maxDurationSeconds: Any? = null,

    @field:SerializedName("status")
    val status: Status? = null
)

data class Meta(

    @field:SerializedName("filename")
    val filename: String? = null
)

data class PublicDetails(

    @field:SerializedName("share_link")
    val shareLink: String? = null,

    @field:SerializedName("channel_link")
    val channelLink: String? = null,

    @field:SerializedName("logo")
    val logo: String? = null,

    @field:SerializedName("title")
    val title: String? = null
)

data class Input(

    @field:SerializedName("width")
    val width: Int? = null,

    @field:SerializedName("height")
    val height: Int? = null
)