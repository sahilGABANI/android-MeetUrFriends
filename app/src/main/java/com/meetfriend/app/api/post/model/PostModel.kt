package com.meetfriend.app.api.post.model

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.challenge.model.ChallengeComment
import com.meetfriend.app.api.challenge.model.ChallengeItem
import com.meetfriend.app.api.challenge.model.ChallengePostInfo
import com.meetfriend.app.api.challenge.model.ChildCommentItem
import com.meetfriend.app.api.hashtag.model.HashtagInfo
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.responseclasses.video.Child_comments
import com.meetfriend.app.responseclasses.video.DataVideo
import com.meetfriend.app.responseclasses.video.Post_comments
import kotlinx.parcelize.Parcelize

data class CreatePostRequest(

    @field:SerializedName("content") var content: String? = null,
    @field:SerializedName("platform") var platform: String? = null,

    @field:SerializedName("location") var location: String? = null,

    @field:SerializedName("tagged_user_id") var taggedUserId: String? = null,

    @field:SerializedName("tag_names") var tagNames: String? = null,

    @field:SerializedName("media") var media: List<MediaRequest>? = null,

    @field:SerializedName("video_id") var videoId: List<VideoMediaRequest>? = null,

    @field:SerializedName("thumbnail") var thumbnail: List<String>? = null,

    @field:SerializedName("type_of_media") var typeOfMedia: String? = null,

    @field:SerializedName("privacy") var privacy: Int? = null,

    @field:SerializedName("width") var width: Int? = null,

    @field:SerializedName("height") var height: Int? = null,

    @field:SerializedName("allowed_users") var allowedUser: List<Int>? = null,

    @field:SerializedName("mention_ids") var mentionIds: String? = null,
    @field:SerializedName("country_code") var countryCode: String? = null,
    @field:SerializedName("position") var position: String? = null,
    @field:SerializedName("rotation_angle") var rotationAngle: Double? = null,
    @field:SerializedName("web_link") var webLink: String? = null,
    @field:SerializedName("music_title") var musicTitle: String? = null,
    @field:SerializedName("artists") var artists: String? = null,
)
data class MediaRequest(

    @field:SerializedName("image")
    var image: String? = null,

    @field:SerializedName("width")
    var width: Int? = 0,

    @field:SerializedName("position")
    var position: String? = null,

    @field:SerializedName("rotation_angle")
    var rotationAngle: Float? = 0f,

    @field:SerializedName("web_link")
    var webLink: String? = "",

    @field:SerializedName("height")
    var height: Int? = 0
)

data class VideoMediaRequest(

    @field:SerializedName("uid")
    var uid: String? = null,

    @field:SerializedName("music_title")
    var musicTitle: String? = null,

    @field:SerializedName("artists")
    var artists: String? = null,

    @field:SerializedName("position")
    var position: String? = null,

    @field:SerializedName("rotation_angle")
    var rotationAngle: Float? = 0f,

    @field:SerializedName("web_link")
    var webLink: String? = "",

    @field:SerializedName("width")
    var width: Int? = 0,

    @field:SerializedName("height")
    var height: Int? = 0
)
data class MediaSize(
    @field:SerializedName("width")
    var width: Int? = 0,

    @field:SerializedName("height")
    var height: Int? = 0
)

data class EditShortRequest(

    @field:SerializedName("post_id") var postId: Int,

    @field:SerializedName("content") var content: String? = null,

    @field:SerializedName("tagged_user_id") var taggedUserId: String? = null,
    @field:SerializedName("tag_names") var tagNames: String? = null,

    @field:SerializedName("mention_ids") var mentionIds: String? = null,

    @field:SerializedName("location") var location: String? = null,

    @field:SerializedName("privacy") var privacy: Int? = null,

)

data class ViewPostRequest(
    @field:SerializedName("post_id") var postId: String,
    @field:SerializedName("type") var type: String,
)

data class ShareData(
    val privacy: String? = null,
    val about: String? = null,
    val mentionUserId: String? = null
)
data class LaunchActivityData(
    val context: Context,
    val postType: String,
    val imagePathList: ArrayList<String>? = null,
    val videoPath: String? = null,
    val shortsInfo: com.meetfriend.app.responseclasses.video.Post? = null,
    val tagName: String? = null,
    val videoUri: Uri? = null,
    val linkAttachmentDetails: LinkAttachmentDetails? = null,
    val listOfMultipleMedia: ArrayList<MultipleImageDetails>? = null,
    val musicResponse: MusicInfo? = null
)

sealed class ShortsPageState {
    data class UserProfileClick(val dataVideo: DataVideo) : ShortsPageState()
    data class FollowClick(val dataVideo: DataVideo) : ShortsPageState()
    data class AddReelLikeClick(val dataVideo: DataVideo) : ShortsPageState()
    data class RemoveReelLikeClick(val dataVideo: DataVideo) : ShortsPageState()
    data class CommentClick(val dataVideo: DataVideo) : ShortsPageState()
    data class ShareClick(val dataVideo: DataVideo) : ShortsPageState()
    data class DownloadClick(val dataVideo: DataVideo) : ShortsPageState()
    data class MoreClick(val dataVideo: DataVideo) : ShortsPageState()
    data class MentionUserClick(val mentionUser: String, val dataVideo: DataVideo) : ShortsPageState()
    data class HashtagClick(val mentionUser: String, val dataVideo: DataVideo) : ShortsPageState()
    data class GiftClick(val dataVideo: DataVideo) : ShortsPageState()
    data class OpenLinkAttachment(val dataVideo: DataVideo) : ShortsPageState()
    object OpenLoginPopup : ShortsPageState()
}

sealed class ChallengePageState {
    data class UserProfileClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class FollowClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class ReelLikeClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class AddReelLikeClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class RemoveReelLikeClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class CommentClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class ShareClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class JoinChallengeClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class UserChallengeReplyProfileClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class UserChallengeWinnerProfileClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class MoreClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class GiftClick(val dataVideo: ChallengeItem) : ChallengePageState()
    data class HashtagClick(val mentionUser: String, val dataVideo: ChallengeItem) : ChallengePageState()
    data class OpenLinkAttachment(val dataVideo: ChallengeItem) : ChallengePageState()
}

sealed class ChallengePostPageState {
    data class UserProfileClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
    data class FollowClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
    data class AddReelLikeClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
    data class RemoveReelLikeClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
    data class CommentClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
    data class ShareClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
    data class JoinChallengeClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
    data class UserChallengeReplyProfileClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
    data class MoreClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
    data class GiftClick(val dataVideo: ChallengePostInfo) : ChallengePostPageState()
}

sealed class ShortsCommentState {
    data class DeleteClick(val postComments: Post_comments) : ShortsCommentState()
    data class ReplyClick(val postComments: Post_comments) : ShortsCommentState()
    data class EditClick(val postComments: Post_comments) : ShortsCommentState()
    data class ReplyDeleteClick(val childComment: Child_comments) : ShortsCommentState()
    data class ReplyReplyClick(val childComment: Child_comments) : ShortsCommentState()
    data class ReplyEditClick(val childComment: Child_comments) : ShortsCommentState()
    data class MentionUserClick(val mentionText: String, val postComments: Post_comments) : ShortsCommentState()
    data class ReplyMentionUserClick(val mentionText: String, val childComment: Child_comments) : ShortsCommentState()
}

sealed class ChallengeCommentState {
    data class UserProfileClick(val userId: Int) : ChallengeCommentState()
    data class AddCommentLikeClick(val dataVideo: ChallengeComment) : ChallengeCommentState()
    data class RemoveCommentLikeClick(val dataVideo: ChallengeComment) : ChallengeCommentState()
    data class DeleteClick(val postComments: ChallengeComment) : ChallengeCommentState()
    data class ReplyClick(val postComments: ChallengeComment) : ChallengeCommentState()
    data class EditClick(val postComments: ChallengeComment) : ChallengeCommentState()
    data class ReplyDeleteClick(
        val postComments: ChallengeComment,
        val childCommentItem: ChildCommentItem
    ) : ChallengeCommentState()
    data class ReplyReplyClick(val childComment: ChallengeComment) : ChallengeCommentState()
    data class ReplyEditClick(val childComment: ChildCommentItem) : ChallengeCommentState()
    data class AddReplyCommentLikeClick(val childComment: ChildCommentItem) : ChallengeCommentState()
}

sealed class ChallengeCommentReplyState {
    data class UserProfileClick(val userId: Int) : ChallengeCommentReplyState()
    data class AddCommentLikeClick(val dataVideo: ChildCommentItem) : ChallengeCommentReplyState()
    data class AddReplyCommentLikeClick(val dataVideo: ChildCommentItem) : ChallengeCommentReplyState()
    data class RemoveCommentLikeClick(val dataVideo: ChildCommentItem) : ChallengeCommentReplyState()
    data class DeleteClick(val postComments: ChildCommentItem) : ChallengeCommentReplyState()
    data class ReplyClick(val postComments: ChildCommentItem) : ChallengeCommentReplyState()
    data class EditClick(val postComments: ChildCommentItem) : ChallengeCommentReplyState()
    data class ReplyDeleteClick(val childComment: ChildCommentItem) : ChallengeCommentReplyState()
    data class ReplyReplyClick(val childComment: ChildCommentItem) : ChallengeCommentReplyState()
    data class ReplyEditClick(val childComment: ChildCommentItem) : ChallengeCommentReplyState()
}

@Parcelize
data class User(
    @field:SerializedName("userName") val userName: String? = null,

    @field:SerializedName("firstName") val firstName: String? = null,

    @field:SerializedName("lastName") val lastName: String? = null,

    @field:SerializedName("profile_photo") val profilePhoto: String? = null,

    @field:SerializedName("gender") val gender: String? = null,

    @field:SerializedName("id") val id: Int,

    @field:SerializedName("is_verified") val isVerified: Int,

    @field:SerializedName("cover_photo") val coverPhoto: String? = null
) : Parcelable

@Parcelize
data class PostCommentsItem(

    @field:SerializedName("child_comments") val childComments: List<ChildComments>? = null,

    @field:SerializedName("post_id") val postId: Int? = null,

    @field:SerializedName("user_id") val userId: Int? = null,

    @field:SerializedName("parent_id") val parentId: Int? = null,

    @field:SerializedName("replied_to_user_id") val repliedToUserId: Int? = null,

    @field:SerializedName("created_at") val createdAt: String? = null,

    @field:SerializedName("id") val id: Int,

    @field:SerializedName("user") val user: User? = null,

    @field:SerializedName("content") val content: String? = null
) : Parcelable

@Parcelize
data class PostInformation(

    @field:SerializedName("privacy") val privacy: Int? = null,

    @field:SerializedName("created_at") val createdAt: String? = null,

    @field:SerializedName("post_comments") val postComments: List<PostCommentsItem>? = null,

    @field:SerializedName("type") val type: String? = null,

    @field:SerializedName("content") var content: String? = null,

    @field:SerializedName("share_count") val shareCount: Int? = null,

    @field:SerializedName("post_likes_count") var postLikesCount: Int? = null,

    @field:SerializedName("user_id") val userId: Int? = null,

    @field:SerializedName("shared_post") val sharedPost: SharedPost? = null,

    @field:SerializedName("shared_post_id") val sharedPostId: Int? = null,

    @field:SerializedName("is_liked_count") var isLikedCount: Int? = null,

    @field:SerializedName("post_media") val postMedia: List<PostMediaInformation>? = null,

    @field:SerializedName("is_shared") val isShared: Int? = null,

    @field:SerializedName("follow_back") val followBack: Int? = null,

    @field:SerializedName("no_of_shared_count") val noOfSharedCount: Int? = null,

    @field:SerializedName("total_gift_count") var totalGiftCount: Int? = null,

    @field:SerializedName("short_views") var shortViews: Int? = null,

    @field:SerializedName("repost_count") var repostCount: Int? = null,

    @field:SerializedName("id") val id: Int,

    @field:SerializedName("user") val user: User,

    @field:SerializedName("tagged_users") val taggedUsersList: ArrayList<TaggedUser>? = null,

    @field:SerializedName("location") val location: String? = null,

    @field:SerializedName("mention_users") val mentionUsers: ArrayList<MentionUser>? = null,
    @field:SerializedName("post_hashtags") val postHashtags: ArrayList<HashtagInfo>? = null,

    @field:SerializedName("post_likes") val postLikes: List<PostLikesInformation>,

    @SerializedName("web_link") val webLink: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName("rotation_angle") val rotationAngle: Float? = null,
    @field:SerializedName("platform") var platform: String? = null,

) : Parcelable

@Parcelize
data class TaggedUser(

    @field:SerializedName("id") val id: Int,

    @field:SerializedName("post_id") val postId: Int? = null,

    @field:SerializedName("tagged_user_id") val taggedUserId: Int? = null,

    @field:SerializedName("user") val user: User,

) : Parcelable

@Parcelize
data class PostMediaInformation(

    @field:SerializedName("file_path") val filePath: String? = null,

    @field:SerializedName("extension") val extension: String? = null,

    @field:SerializedName("size") val size: String? = null,

    @field:SerializedName("width") val width: String? = null,

    @field:SerializedName("height") val height: String? = null,

    @field:SerializedName("file_name") val fileName: String? = null,

    @field:SerializedName("created_at") val createdAt: String? = null,

    @field:SerializedName("posts_id") val postsId: Int? = null,

    @field:SerializedName("id") val id: Int,

    @field:SerializedName("thumbnail") val thumbnail: String? = null,

    @SerializedName("web_link") val webLink: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName("rotation_angle") val rotationAngle: Float? = null,
    @field:SerializedName("music_title") var musicTitle: String? = null,
    @field:SerializedName("artists") var artists: String? = null,

) : Parcelable {
    fun getIsFilePathImage(): Boolean {
        return extension != ".m3u8"
    }
}

@Parcelize
data class PostLikesInformation(

    @field:SerializedName("post_id") val postId: Int? = null,

    @field:SerializedName("user_id") val userId: Int? = null,

    @field:SerializedName("id") val id: Int,

    @field:SerializedName("user") val user: User? = null
) : Parcelable

data class PostHashTagResponses<T>(

    @field:SerializedName("data") val listOfPosts: T? = null,

    @field:SerializedName("current_page") val currentPage: String? = null
)

data class PostResponses(

    @field:SerializedName("data") val listOfPosts: List<PostInformation>? = null,

    @field:SerializedName("current_page") val currentPage: String? = null
)

data class GetHashTagPostRequest(
    var page: Int,
    var perPage: Int,
    var hashTagId: Int,
    var searchString: String? = null
)

data class GetPostRequest(
    var page: Int,
    var perPage: Int,
    var searchString: String? = null
)
data class PostLikeUnlikeRequest(
    var postId: Int,
    var postStatus: String
)

data class PostShareRequest(
    var postId: Int,
    var privacy: String,
    var content: String,
    var mentionUser: String?
)

data class PostReportRequest(
    var postId: Int,
    var content: String,
    var type: String,
)

data class GetShortsRequest(
    var page: Int,
    var perPage: Int,
    var searchString: String,
    var isFollowing: Int
)

@Parcelize
data class SharedPost(

    @field:SerializedName("privacy") val privacy: Int? = null,

    @field:SerializedName("created_at") val createdAt: String? = null,

    @field:SerializedName("type") val type: String? = null,

    @field:SerializedName("content") val content: String? = null,

    @field:SerializedName("user_id") val userId: Int? = null,

    @field:SerializedName("post_media") val postMedia: List<PostMediaInformation>? = null,

    @field:SerializedName("tagged_users") val taggedUsersList: ArrayList<TaggedUser>? = null,

    @field:SerializedName("is_shared") val isShared: Int? = null,

    @field:SerializedName("id") val id: Int,

    @field:SerializedName("user") val user: User? = null,

    @field:SerializedName("location") val location: String? = null,

    @field:SerializedName("is_verified") val isVerified: Int,

    @field:SerializedName("shared_mention_users") val sharedMentionUsers: ArrayList<MentionUser>? = null,
) : Parcelable

data class PostCommentRequest(
    @field:SerializedName("post_id") val postId: Int? = null,

    @field:SerializedName("content") val content: String? = null,

    @field:SerializedName("type") val type: String? = null,
)

data class PostCommentResponse(

    @field:SerializedName("post_id") val postId: Int? = null,

    @field:SerializedName("user_id") val userId: Int? = null,

    @field:SerializedName("parent_id") val parentId: Int? = null,

    @field:SerializedName("created_at") val createdAt: String? = null,

    @field:SerializedName("id") val id: Int,

    @field:SerializedName("user") val user: User,

    @field:SerializedName("content") val content: String? = null,

    @field:SerializedName("mention_comments") val mentionComments: ArrayList<MentionUser>? = null,
)

data class PostIdRequest(
    @field:SerializedName("post_id") val postId: Int? = null,
)

data class WaterMarkVideoRequest(
    @field:SerializedName("file_name") val fileName: String? = null,

    @field:SerializedName("posts_id") val postsId: Int? = null,

    @field:SerializedName("url") val url: String? = null,
)

data class ReportLiveRequest(
    @field:SerializedName("live_id") val liveId: Int,

    @field:SerializedName("reason") val reason: String? = null,
)

data class MentionUserRequest(

    @field:SerializedName("search") val search: String? = null,

)

data class VideoResponse(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("data") val videoData: List<DataVideo>,
    @SerializedName("first_page_url") val firstPageUrl: String,
    @SerializedName("from") val from: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("last_page_url") val lastPageUrl: String,
    @SerializedName("links") val links: List<Links>,
    @SerializedName("next_page_url") val nextPageUrl: String,
    @SerializedName("path") val path: String,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("prev_page_url") val prevPageUrl: String,
    @SerializedName("to") val to: Int,
    @SerializedName("total") val total: Int
)

data class Links(
    @SerializedName("url") val url: String,
    @SerializedName("label") val label: String,
    @SerializedName("active") val active: Boolean
)

data class DataVideo(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("posts_id") val postsId: Int,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_path") val filePath: String,
    @SerializedName("extension") val extension: String,
    @SerializedName("size") val size: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("post") val post: Post,
    @SerializedName("ads") var ads: Boolean,
    @SerializedName("thumbnail") val thumbnail: String? = null,
)

data class Post(

    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("location") val location: String,
    @SerializedName("is_shared") val isShared: Int,
    @SerializedName("shared_post_id") val sharedPostId: Int,
    @SerializedName("shared_user_id") val sharedUserId: Int,
    @SerializedName("type") val type: String,
    @SerializedName("privacy") val privacy: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("post_likes_count") var postLikesCount: Int,
    @SerializedName("is_liked_count") val isLikedCount: Int,
    @SerializedName("no_of_shared_count") var noOfSharedCount: Int,
    @SerializedName("post_likes") val postLikes: List<PostLikes>,
    @SerializedName("user") val user: User,
    @SerializedName("post_comments") val postComments: MutableList<PostComments>,
    @SerializedName("shared_post") val sharedPost: String
)

data class PostLikes(

    @SerializedName("id") val id: Int,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user") val user: User
)

data class PostComments(

    @SerializedName("id") val id: Int,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("parent_id") val parentId: String,
    @SerializedName("content") val content: String,
    @SerializedName("replied_to_user_id") val repliedToUserId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("child_comments") val childComments: ArrayList<ChildComments>,
    @SerializedName("user") val user: User
)

@Parcelize
data class ChildComments(

    @SerializedName("id") val id: Int,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("parent_id") val parentId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("replied_to_user_id") val repliedToUserId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("user") val user: User,
    @SerializedName("replied_to_user") val repliedToUser: String
) : Parcelable

@Parcelize
data class MentionUser(

    @field:SerializedName("id") val id: Int,

    @field:SerializedName("post_id") val postId: Int? = null,

    @field:SerializedName("mention_user_id") val mentionUserId: Long? = null,

    @field:SerializedName("user") val user: User,

) : Parcelable

@Parcelize
data class LinkAttachmentDetails(
    var finalX: Float? = 0.0f,
    var finalY: Float? = 0.0f,
    var lastHeight: Float? = 0.0f,
    var lastWidth: Float? = 0.0f,
    var lastRotation: Float? = 0.0f,
    val attachUrl: String? = null
) : Parcelable

@Parcelize
data class MultipleImageDetails(
    var finalX: Float? = 0.0f,
    var finalY: Float? = 0.0f,
    var lastHeight: Float? = 0.0f,
    var lastWidth: Float? = 0.0f,
    var lastRotation: Float? = 0.0f,
    var attachUrl: String? = null,
    var mainImagePath: String? = null,
    var editedImagePath: String? = null,
    var mainVideoPath: String? = null,
    var editedVideoPath: String? = null,
    var isVideo: Boolean? = false,
    var isSelected: Boolean? = false,
    var musicInfo: MusicInfo? = null,
    var isMusicVideo: Boolean? = false,
    var musicVideoPath: String? = null,
    var width: Int? = 0,
    var height: Int? = 0
) : Parcelable

@Parcelize
data class UploadMediaRequest(
    var imageUrl: String? = null,
    var thumbnail: String? = null,
    var videoId: String? = null,
    var isVideo: Boolean? = false,
) : Parcelable

@Parcelize
data class FontDetails(
    var fontId: Int? = null,
    var fontName: String? = null,
) : Parcelable

data class PositionData(val x: Float, val y: Float, val width: Int, val height: Int)

data class SetUpLinkViewData(
    val platform: String?,
    val buttonX: Float,
    val buttonY: Float,
    val buttonWidth: Int,
    val buttonHeight: Int
)

data class ResultWebData(val logoUrl: String, val title: String, val url: String)

data class ResultGoogleSearchData(val title: String, val url: String)

sealed class LinkAttachmentState {
    data class WebAddClick(val goggleWebData: ResultWebData) : LinkAttachmentState()
    data class GoogleAddClick(val googleSearchData: ResultGoogleSearchData) : LinkAttachmentState()
}

sealed class ShortsDetailPageState {
    data class UserProfileClick(val dataVideo: com.meetfriend.app.responseclasses.video.Post) : ShortsDetailPageState()
    data class FollowClick(val dataVideo: com.meetfriend.app.responseclasses.video.Post) : ShortsDetailPageState()
    data class AddReelLikeClick(val dataVideo: com.meetfriend.app.responseclasses.video.Post) : ShortsDetailPageState()
    data class RemoveReelLikeClick(
        val dataVideo: com.meetfriend.app.responseclasses.video.Post
    ) : ShortsDetailPageState()
    data class CommentClick(val dataVideo: com.meetfriend.app.responseclasses.video.Post) : ShortsDetailPageState()
    data class ShareClick(val dataVideo: com.meetfriend.app.responseclasses.video.Post) : ShortsDetailPageState()
    data class DownloadClick(val dataVideo: com.meetfriend.app.responseclasses.video.Post) : ShortsDetailPageState()
    data class MoreClick(val dataVideo: com.meetfriend.app.responseclasses.video.Post) : ShortsDetailPageState()
    data class MentionUserClick(
        val mentionUser: String,
        val dataVideo: com.meetfriend.app.responseclasses.video.Post
    ) : ShortsDetailPageState()
    data class HashtagClick(
        val mentionUser: String,
        val dataVideo: com.meetfriend.app.responseclasses.video.Post
    ) : ShortsDetailPageState()
    data class GiftClick(val dataVideo: com.meetfriend.app.responseclasses.video.Post) : ShortsDetailPageState()
    data class OpenLinkAttachment(
        val dataVideo: com.meetfriend.app.responseclasses.video.Post
    ) : ShortsDetailPageState()
}
