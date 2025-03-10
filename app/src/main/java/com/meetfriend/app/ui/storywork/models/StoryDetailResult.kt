package com.meetfriend.app.ui.storywork.models

import com.google.gson.annotations.SerializedName

data class StoryDetailResult(
    @SerializedName("id") val id: Int,
    @SerializedName("uuid") val uuid: String,
    @SerializedName("country_code") val country_code: String,
    @SerializedName("email_or_phone") val email_or_phone: String,
    @SerializedName("password_show") val password_show: String,
    @SerializedName("google_id") val google_id: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("city") val city: String,
    @SerializedName("education") val education: String,
    @SerializedName("work") val work: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("dob") val dob: String,
    @SerializedName("dob_string") val dob_string: String,
    @SerializedName("relationship") val relationship: String,
    @SerializedName("hobbies") val hobbies: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("profile_photo") val profile_photo: String,
    @SerializedName("cover_photo") val cover_photo: String,
    @SerializedName("email_verified_at") val email_verified_at: String,
    @SerializedName("status") val status: Int,
    @SerializedName("api_token") val api_token: String,
    @SerializedName("device_type") val device_type: String,
    @SerializedName("device_token") val device_token: String,
    @SerializedName("device_id") val device_id: String,
    @SerializedName("voip_device_token") val voip_device_token: String,
    @SerializedName("current_coins") val current_coins: Double,
    @SerializedName("created_at") val created_at: String,
    @SerializedName("lastActivity_at") val lastActivity_at: String,
    @SerializedName("is_qualified") val is_qualified: Int,
    @SerializedName("paid_amount") val paid_amount: Int,
    @SerializedName("updated_at") val updated_at: String,
    @SerializedName("deleted_at") val deleted_at: String,
    @SerializedName("stories") val stories: List<Stories>,
    @SerializedName("userName") val userName: String
)