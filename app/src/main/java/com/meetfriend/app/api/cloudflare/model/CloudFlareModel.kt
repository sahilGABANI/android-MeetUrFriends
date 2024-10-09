package com.meetfriend.app.api.cloudflare.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.api.post.model.LinkAttachmentDetails
import kotlinx.parcelize.Parcelize
import okhttp3.MultipartBody

@Keep
@Parcelize
data class CloudFlareConfig(
    @field:SerializedName("account_id")
    val accountId: String? = null,

    @field:SerializedName("api_token")
    val apiToken: String? = null,

    @field:SerializedName("image_upload_url")
    val imageUploadUrl: String? = null,

    @field:SerializedName("video_upload_url")
    val videoUploadUrl: String? = null,

    @field:SerializedName("video_key")
    val videoKey: String? = null,
) : Parcelable

data class StoryUploadData(
    val cloudFlareConfig: CloudFlareConfig,
    val videoPath: String,
    val videoDuration: String,
    val linkAttachmentDetails: LinkAttachmentDetails? = null,
    val musicResponse: MusicInfo? = null,
    val videoHeight: Int? = null,
    val videoWidth: Int? = null
)

@Keep
data class UploadImageCloudFlareResponse(
    @field:SerializedName("result")
    val result: UploadImageCloudFlareResult? = null,

    @field:SerializedName("success")
    val success: Boolean,

    @field:SerializedName("errors")
    val errors: List<String>? = null,
)

@Keep
data class UploadImageCloudFlareResult(
    @field:SerializedName("variants")
    val variants: List<String>? = null,
)

@Keep
data class UploadVideoCloudFlareResponse(
    @field:SerializedName("result")
    val result: UploadVideoCloudFlareResult? = null,

    @field:SerializedName("success")
    val success: Boolean,

    @field:SerializedName("errors")
    val errors: List<String>? = null,
)

@Keep
data class UploadVideoCloudFlareResult(
    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("preview")
    val preview: String? = null,

    @field:SerializedName("thumbnail")
    val thumbnail: String? = null,

)

data class ImageToCloudFlare(
    val apiUrl: String,
    val authToken: String,
    val filePart: MultipartBody.Part,
    val fileName: String? = null,
    val totalRequests: Int? = 0,
    val currentRequestIndex: Int? = 0
)
