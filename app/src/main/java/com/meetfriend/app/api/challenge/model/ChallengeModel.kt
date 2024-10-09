package com.meetfriend.app.api.challenge.model

import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.hashtag.model.HashtagInfo

data class ChallengeLikeUserInfo(
    @field:SerializedName("current_page")
    var currentPage: Int,

    @field:SerializedName("data")
    var data: ArrayList<ChallengeUserModel> = arrayListOf(),

    @field:SerializedName("first_page_url")
    var firstPageUrl: String? = null,

    @field:SerializedName("last_page_url")
    var lastPageUrl: String? = null,
)

data class ChallengeUserModel(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("challenge_id")
    val challengeId: Int? = null,

    @field:SerializedName("challenge_post_id")
    val challengePostId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("user")
    val user: User? = null

)
data class ChallengeInfo(

    @field:SerializedName("per_page")
    val perPage: String? = null,

    @field:SerializedName("data")
    val data: List<ChallengeItem>? = null,

    @field:SerializedName("last_page")
    val lastPage: Int? = null,

    @field:SerializedName("next_page_url")
    val nextPageUrl: String? = null,

    @field:SerializedName("prev_page_url")
    val prevPageUrl: Any? = null,

    @field:SerializedName("first_page_url")
    val firstPageUrl: String? = null,

    @field:SerializedName("path")
    val path: String? = null,

    @field:SerializedName("total")
    val total: Int? = null,

    @field:SerializedName("last_page_url")
    val lastPageUrl: String? = null,

    @field:SerializedName("from")
    val from: Int? = null,

    @field:SerializedName("links")
    val links: List<LinksItem?>? = null,

    @field:SerializedName("to")
    val to: Int? = null,

    @field:SerializedName("current_page")
    val currentPage: Int? = null
)

data class CountryData(

    @field:SerializedName("sortname")
    val sortname: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("phonecode")
    val phonecode: Int? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("status")
    val status: Int? = null
)

data class CityData(

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("state_id")
    val stateId: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null
)

data class ChallengeCountryItem(

    @field:SerializedName("country_data")
    val countryData: CountryData? = null,

    @field:SerializedName("challenge_id")
    val challengeId: Int? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("country_id")
    val countryId: Int? = null
)

data class ChallengeCityItem(

    @field:SerializedName("challenge_id")
    val challengeId: Int? = null,

    @field:SerializedName("city_data")
    val cityData: CityData? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("city_id")
    val cityId: Int? = null
)

data class LinksItem(

    @field:SerializedName("active")
    val active: Boolean? = null,

    @field:SerializedName("label")
    val label: String? = null,

    @field:SerializedName("url")
    val url: Any? = null
)

data class User(

    @field:SerializedName("firstName")
    val firstName: String? = null,

    @field:SerializedName("lastName")
    val lastName: String? = null,

    @field:SerializedName("profile_photo")
    val profilePhoto: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("userName")
    val userName: String? = null,

    @field:SerializedName("is_verified")
    val isVerified: Int? = null,
)

data class ChallengePostInfo(

    @field:SerializedName("file_path")
    val filePath: String? = null,

    @field:SerializedName("no_of_likes_count")
    var noOfLikesCount: Int? = null,

    @field:SerializedName("is_follow")
    val isFollow: Int? = null,

    @field:SerializedName("challenge_id")
    val challengeId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("is_liked_count")
    var isLikedCount: Int? = null,

    @field:SerializedName("description")
    val description: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("no_of_comment_count")
    val noOfCommentCount: Int? = null,

    @field:SerializedName("user")
    val user: User? = null,

    @field:SerializedName("no_of_views_count")
    val noOfViewsCount: Int? = null
) {
    fun getIsFilePathImage(): Boolean {
        val filePath = filePath
        val wordList = filePath?.split(".")
        return wordList?.last()?.equals("png") == true || wordList?.last()
            ?.equals("jpeg") == true || wordList?.last()?.equals("jpg") == true
    }

    fun getPostTotal(): Int {
        return noOfLikesCount?.plus(noOfViewsCount ?: 0) ?: 0
    }
}

data class ChallengeStateItem(

    @field:SerializedName("state_data")
    val stateData: StateData? = null,

    @field:SerializedName("challenge_id")
    val challengeId: Int? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("state_id")
    val stateId: Int? = null
)

data class StateData(

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("country_id")
    val countryId: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null
)

data class ChallengeItem(

    @field:SerializedName("by_admin")
    val byAdmin: Int? = null,

    @field:SerializedName("country")
    val country: String? = null,

    @field:SerializedName("file_path")
    val filePath: String? = null,

    @field:SerializedName("time_to")
    val timeTo: String? = null,

    @field:SerializedName("is_follow")
    val isFollow: Int? = null,

    @field:SerializedName("follow_back")
    var followBack: Int? = null,

    @field:SerializedName("city")
    val city: String? = null,

    @field:SerializedName("timezone")
    val timezone: String? = null,

    @field:SerializedName("challenge_country")
    val challengeCountry: List<ChallengeCountryItem?>? = null,

    @field:SerializedName("challenge_state")
    val challengeState: List<ChallengeStateItem?>? = null,

    @field:SerializedName("privacy")
    val privacy: Int? = null,

    @field:SerializedName("description")
    val description: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("date_to")
    val dateTo: String? = null,

    @field:SerializedName("challenge_city")
    val challengeCity: List<ChallengeCityItem?>? = null,

    @field:SerializedName("title")
    val title: String? = null,

    @field:SerializedName("created_by")
    val createdBy: Int? = null,

    @field:SerializedName("is_liked_count")
    var isLikeCount: Int? = null,

    @field:SerializedName("no_of_likes_count")
    var noOfLikesCount: Int? = null,

    @field:SerializedName("no_of_views_count")
    val noOfViewsCount: Int? = null,

    @field:SerializedName("no_of_comment_count")
    val noOfCommentCount: Int? = null,

    @field:SerializedName("time_from")
    val timeFrom: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("state")
    val state: String? = null,

    @field:SerializedName("challenge_reactions")
    val challengeReactions: ChallengeReactions? = null,

    @field:SerializedName("user")
    val user: User? = null,

    @field:SerializedName("winner_user")
    val winnerUser: User? = null,

    @field:SerializedName("date_from")
    val dateFrom: String? = null,

    @field:SerializedName("status")
    var status: Int? = null,

    @field:SerializedName("challenge_post_user")
    val challengePostUser: ArrayList<User>? = null,

    @field:SerializedName("challenge_posts_sorted")
    val challengePostsSorted: ArrayList<User>? = null,

    @field:SerializedName("challenge_posts")
    val challengePost: ArrayList<ChallengePostInfo>? = null,

    @field:SerializedName("challenge_post_count")
    val challengePostCount: Int? = null,

    @SerializedName("challenge_hashtags")
    val postHashtags: ArrayList<HashtagInfo>? = null,

    var userCity: String? = null,
    var userState: String? = null,
    var userCountry: String? = null,

    @SerializedName("web_link")
    val webLink: String? = null,

    @SerializedName("position")
    val position: String? = null,

    @SerializedName("rotation_angle")
    val rotationAngle: Float? = null,

    @field:SerializedName("music_title") var musicTitle: String? = null,

    @field:SerializedName("artists") var artists: String? = null,
    @SerializedName("width") val width: Int? = 0,
    @SerializedName("height") val height: Int? = 0,
    @field:SerializedName("platform") var platform: String? = null,
) {
    fun getIsFilePathImage(): Boolean {
        val filePath = filePath
        val wordList = filePath?.split(".")
        return wordList?.last()?.equals("png") == true || wordList?.last()
            ?.equals("jpeg") == true || wordList?.last()?.equals("jpg") == true
    }
}

data class ChallengeReactions(
    @field:SerializedName("challenge_id")
    val challengeId: Int? = null,

    @field:SerializedName("userId")
    val userId: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null
)

data class ChallengeCountRequest(
    @SerializedName("challenge_id")
    val challengeId: Int
)

data class DeleteChallengePostRequest(
    @SerializedName("challenge_post_id")
    val challengePostId: Int?
)

data class ChallengePostDeleteCommentRequest(
    @SerializedName("comment_id")
    val commentId: Int,

    @SerializedName("parent_id")
    val parentId: Int? = null,
)

data class ChallengePostLikeRequest(
    @SerializedName("challenge_id")
    val challengeId: Int,

    @SerializedName("challenge_post_id")
    val challengePostId: Int,

    @SerializedName("status")
    val status: Int,
)

data class ChallengeUpdateCommentRequest(
    @SerializedName("comment_id")
    val commentId: Int,

    @SerializedName("content")
    val content: String,

    @SerializedName("mention_ids")
    var mentionIds: String? = null,

    @SerializedName("parent_id")
    val parentId: Int? = null,
)

data class ChallengeCommentRequest(
    @SerializedName("comment_id")
    val commentId: Int,

    @SerializedName("parent_id")
    val parentId: Int? = null,
)

data class ChallengePostCommentRequest(
    @SerializedName("comment_id")
    val commentId: Int,

    @SerializedName("challenge_post_id")
    val challengePostId: Int?
)

data class AddChallengeCommentRequest(
    @SerializedName("challenge_id")
    val challengeId: Int,

    @SerializedName("parent_id")
    val parentId: Int? = null,

    @SerializedName("content")
    val content: String,

    @SerializedName("mention_ids")
    var mentionIds: String? = null
)

data class AddChallengePostCommentRequest(
    @SerializedName("challenge_id")
    val challengeId: Int,

    @SerializedName("challenge_post_id")
    val challengePostId: Int? = null,

    @SerializedName("content")
    val content: String,

    @SerializedName("mention_ids")
    var mentionIds: String? = null,

    @SerializedName("parent_id")
    val parentId: Int? = null
)

data class ChallengePostViewByUserRequest(
    @SerializedName("challenge_id")
    val challengeId: Int,
    @SerializedName("challenge_post_id")
    val challengePostId: Int

)

data class ReportChallengeRequest(
    @SerializedName("challenge_id")
    val challengeId: Int,

    @SerializedName("reason")
    val reason: String,
)

data class ReportChallengePostRequest(
    @SerializedName("challenge_id")
    val challengeId: Int,

    @SerializedName("reason")
    val reason: String,

    @SerializedName("challenge_post_id")
    val challengePostId: Int? = null,
)

data class ChallengeComment(

    @field:SerializedName("challenge_id")
    val challengeId: Int,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("uuid")
    val uuid: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: Any? = null,

    @field:SerializedName("content")
    var content: String? = null,

    @field:SerializedName("no_of_likes_count")
    var noOfLikesCount: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("parent_id")
    val parentId: Int? = null,

    @field:SerializedName("replied_to_user_id")
    val repliedToUserId: Any? = null,

    @field:SerializedName("is_liked_count")
    var isLikedCount: Int? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user")
    val user: User? = null,
    @field:SerializedName("child_comments")
    var childComments: List<ChildCommentItem>? = null,

    @field:SerializedName("mention_comments")
    var mentionComments: ArrayList<MentionUserInfo>? = null,

)

data class ChildCommentItem(

    @field:SerializedName("challenge_id")
    val challengeId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("parent_id")
    val parentId: Int? = null,

    @field:SerializedName("replied_to_user_id")
    val repliedToUserId: Any? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user")
    val user: User? = null,

    @field:SerializedName("content")
    var content: String? = null,

    @field:SerializedName("no_of_likes_count")
    var noOfLikesCount: Int? = null,

    @field:SerializedName("is_liked_count")
    var isLikedCount: Int? = null,

    @field:SerializedName("sub_mention_comments")
    var mentionComments: ArrayList<MentionUserInfo>? = null,
)

data class MentionUserInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("comment_id")
    val commentId: Int? = null,

    @field:SerializedName("mention_user_id")
    val mentionUserId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user")
    val user: User? = null,
)

data class ChallengeCommentInfo(

    @field:SerializedName("per_page")
    val perPage: String? = null,

    @field:SerializedName("data")
    val data: List<ChallengeComment>? = null,

    @field:SerializedName("last_page")
    val lastPage: Int? = null,

    @field:SerializedName("next_page_url")
    val nextPageUrl: Any? = null,

    @field:SerializedName("prev_page_url")
    val prevPageUrl: Any? = null,

    @field:SerializedName("first_page_url")
    val firstPageUrl: String? = null,

    @field:SerializedName("path")
    val path: String? = null,

    @field:SerializedName("total")
    val total: Int? = null,

    @field:SerializedName("last_page_url")
    val lastPageUrl: String? = null,

    @field:SerializedName("from")
    val from: Int? = null,

    @field:SerializedName("links")
    val links: List<LinksItem?>? = null,

    @field:SerializedName("to")
    val to: Int? = null,

    @field:SerializedName("current_page")
    val currentPage: Int? = null
)

enum class ChallengeType {
    AllChallenge,
    MyChallenge,
    LiveChallenge,
    CompletedChallenge
}
