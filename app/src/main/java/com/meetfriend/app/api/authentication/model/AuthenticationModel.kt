package com.meetfriend.app.api.authentication.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class LoginResponse(

    @field:SerializedName("profile_updated_status")
    val profileUpdatedStatus: Boolean? = null,

    @field:SerializedName("access_token")
    val accessToken: String? = null,

    @field:SerializedName("api_token")
    val apiToken: String? = null,

    @field:SerializedName("result")
    val result: MeetFriendUser? = null,

    @field:SerializedName("base_url")
    val baseUrl: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("media_url")
    val mediaUrl: String? = null,

    @field:SerializedName("status")
    val status: Boolean? = null,
)

data class LoggedInUser(
    val loggedInUser: MeetFriendUser,
    val loggedInUserToken: String?,
)

data class ChatUser(
    val chatUserName: String,
    val chatUserProfile: String
)

data class MeetFriendUser(

    @field:SerializedName("lastName")
    var lastName: String? = null,

    @field:SerializedName("google_id")
    var googleId: String? = null,

    @field:SerializedName("education")
    var education: String? = null,

    @field:SerializedName("cover_photo")
    var coverPhoto: String? = null,

    @field:SerializedName("gender")
    var gender: String? = null,

    @field:SerializedName("profile_photo")
    var profilePhoto: String? = null,

    @field:SerializedName("city")
    var city: String? = null,

    @field:SerializedName("work")
    var work: String? = null,

    @field:SerializedName("bio")
    var bio: String? = null,

    @field:SerializedName("userName")
    var userName: String? = null,

    @field:SerializedName("firstName")
    var firstName: String? = null,

    @field:SerializedName("hobbies")
    var hobbies: String? = null,

    @field:SerializedName("dob")
    var dob: String? = null,

    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("relationship")
    var relationship: String? = null,

    @field:SerializedName("user_customer_count")
    val userCustomerCount: Int? = null,

    @field:SerializedName("email_or_phone")
    var emailOrPhone: String? = null,

    @field:SerializedName("age")
    val age: String? = null,

    @field:SerializedName("follow_status")
    var followStatus: Int? = null,

    @field:SerializedName("following_status")
    var followingStatus: Int? = null,

    @field:SerializedName("follow_back")
    var followBack: Int? = null,

    @field:SerializedName("user_follow_back")
    var userFollowBack: Int? = null,

    var isSelected: Boolean = false,

    @Expose(serialize = false, deserialize = false)
    var isInvited: Boolean = false,

    @Expose(serialize = false, deserialize = false)
    var isAlreadyInvited: Boolean = false,

    @field:SerializedName("isFollow")
    val isFollow: String? = null,

    @field:SerializedName("no_of_followers")
    var noOfFollowers: Int? = null,

    @field:SerializedName("no_of_followings")
    val noOfFollowings: Int? = null,

    @field:SerializedName("post_like")
    val postLike: Int? = null,

    @field:SerializedName("follower_id")
    var followerId: Int? = null,

    @field:SerializedName("is_private")
    var isPrivate: Int? = null,

    @field:SerializedName("is_requested")
    var isRequested: Int? = null,

    @field:SerializedName("myFriend")
    var myFriend: MyFriend? = null,

    @field:SerializedName("userBlockedYou")
    var userBlockedYou: Int? = null,

    @field:SerializedName("chat_user_name")
    val chatUserName: String? = null,

    @field:SerializedName("user")
    val user: MeetFriendUser? = null,

    @field:SerializedName("total_post_challenges")
    val totalPostChallenges: Int? = null,

    @field:SerializedName("is_block")
    val isBlock: Int? = null,

    @field:SerializedName("total_shorts")
    val totalShorts: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("total_posts")
    val totalPosts: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("total_stories")
    val totalStories: Int? = null,

    @field:SerializedName("like_count")
    val likeCount: Int? = null,

    @field:SerializedName("tag_name")
    val tagName: String? = null,

    @field:SerializedName("is_verified")
    val isVerified: Int? = null,

    @field:SerializedName("total_challenges")
    val totalChallenges: Int? = null,
)

data class MyFriend(

    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("user_id")
    var userId: Int? = null,

    @field:SerializedName("friend_id")
    var friendId: Int? = null,

    @field:SerializedName("request_status")
    val requestStatus: String? = null,

    @field:SerializedName("block_status")
    val blockStatus: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("device_token")
    val deviceToken: String? = null,
)

data class LoginRequest(

    @field:SerializedName("email_or_phone")
    val emailOrPhone: String? = null,

    @field:SerializedName("device_type")
    val deviceType: String? = null,

    @field:SerializedName("device_token")
    val deviceToken: String? = null,

    @field:SerializedName("device_model")
    val deviceModel: String? = null,

    @field:SerializedName("voip_device_token")
    val voipDeviceToken: String? = null,

    @field:SerializedName("password")
    val password: String? = null,

    @field:SerializedName("remember")
    val remember: String? = null,

    @field:SerializedName("device_location")
    val deviceLocation: String? = null,

    @field:SerializedName("device_id")
    val deviceId: String? = null,

    @field:SerializedName("google_id")
    val googleId: String? = null,

    @field:SerializedName("login_type")
    val loginType: String? = null,
    @field:SerializedName("firstName")
    val firstName: String? = null,

    @field:SerializedName("lastName")
    val lastName: String? = null,

    @field:SerializedName("userName")
    val userName: String? = null,

)

data class RegisterRequest(

    @field:SerializedName("firstName")
    val firstName: String? = null,

    @field:SerializedName("lastName")
    val lastName: String? = null,

    @field:SerializedName("email_or_phone")
    val emailOrPhone: Any? = null,

    @field:SerializedName("country_code")
    val countryCode: Any? = null,

    @field:SerializedName("dob")
    val dob: Any? = null,

    @field:SerializedName("dob_string")
    val dobString: Any? = null,

    @field:SerializedName("gender")
    val gender: Any? = null,

    @field:SerializedName("device_type")
    val deviceType: String? = null,

    @field:SerializedName("device_token")
    val deviceToken: String? = null,

    @field:SerializedName("device_model")
    val deviceModel: String? = null,

    @field:SerializedName("voip_device_token")
    val voipDeviceToken: String? = null,

    @field:SerializedName("password")
    val password: String? = null,

    @field:SerializedName("confirmPassword")
    val confirmPassword: String? = null,

    @field:SerializedName("device_id")
    val deviceId: Any? = null,

    @field:SerializedName("device_location")
    val deviceLocation: Any? = null,

)

data class DeviceIdRequest(
    @field:SerializedName("device_id")
    val deviceId: String? = null,
)

data class SwitchDeviceAccountRequest(
    @field:SerializedName("device_id")
    val deviceId: String,

    @field:SerializedName("user_id")
    val userId: Int,
)
