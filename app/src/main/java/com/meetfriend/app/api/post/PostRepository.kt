package com.meetfriend.app.api.post

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.livestreaming.model.LiveJoinResponse
import com.meetfriend.app.api.post.model.CreatePostRequest
import com.meetfriend.app.api.post.model.EditShortRequest
import com.meetfriend.app.api.post.model.GetHashTagPostRequest
import com.meetfriend.app.api.post.model.GetPostRequest
import com.meetfriend.app.api.post.model.GetShortsRequest
import com.meetfriend.app.api.post.model.HashTagResponse
import com.meetfriend.app.api.post.model.MentionUserRequest
import com.meetfriend.app.api.post.model.PostHashTagResponses
import com.meetfriend.app.api.post.model.PostIdRequest
import com.meetfriend.app.api.post.model.PostInformation
import com.meetfriend.app.api.post.model.PostLikeUnlikeRequest
import com.meetfriend.app.api.post.model.PostReportRequest
import com.meetfriend.app.api.post.model.PostResponses
import com.meetfriend.app.api.post.model.PostShareRequest
import com.meetfriend.app.api.post.model.ReportLiveRequest
import com.meetfriend.app.api.post.model.ShortsCountRequest
import com.meetfriend.app.api.post.model.VideoResponse
import com.meetfriend.app.api.post.model.ViewPostRequest
import com.meetfriend.app.api.post.model.WaterMarkVideoRequest
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponses
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.responseclasses.share.ShareCountResponse
import com.meetfriend.app.responseclasses.share.ShareFileResponse
import com.meetfriend.app.responseclasses.video.DataVideo
import com.meetfriend.app.responseclasses.video.Post
import io.reactivex.Single

class PostRepository(private val postRetrofitAPI: PostRetrofitAPI) {
    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun hashTagsPosts(
        getHashTagPostRequest: GetHashTagPostRequest,
    ): Single<MeetFriendResponse<PostHashTagResponses<PostResponses>>> {
        return postRetrofitAPI.hashTagsPosts(
            getHashTagPostRequest.page,
            getHashTagPostRequest.perPage,
            getHashTagPostRequest.searchString,
            getHashTagPostRequest.hashTagId
        )
    }

    fun hashTagsShorts(
        getHashTagPostRequest: GetHashTagPostRequest,
    ): Single<MeetFriendResponse<VideoResponse>> {
        return postRetrofitAPI.hashTagsShorts(
            getHashTagPostRequest.page,
            getHashTagPostRequest.perPage,
            getHashTagPostRequest.searchString,
            getHashTagPostRequest.hashTagId
        )
    }

    fun createPost(createPostRequest: CreatePostRequest): Single<MeetFriendCommonResponse> {
        return postRetrofitAPI.createPost(createPostRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun viewPost(viewPostRequest: ViewPostRequest): Single<MeetFriendResponse<Post>> {
        return postRetrofitAPI.viewPost(viewPostRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getPost(getPostRequest: GetPostRequest): Single<List<PostInformation>> {
        return postRetrofitAPI.getPosts(
            getPostRequest.page,
            getPostRequest.perPage,
            getPostRequest.searchString ?: ""
        )
            .flatMap { meetFriendResponseConverter.convertToSingle(it) }
            .map {
                it.listOfPosts ?: listOf()
            }
    }

    fun postLikeUnlike(
        postLikeUnlikeRequest: PostLikeUnlikeRequest
    ): Single<MeetFriendCommonResponse> {
        return postRetrofitAPI.postLikeUnlike(
            postLikeUnlikeRequest.postId,
            postLikeUnlikeRequest.postStatus
        ).flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun postShare(
        postShareRequest: PostShareRequest
    ): Single<MeetFriendCommonResponse> {
        return postRetrofitAPI.postShare(
            postShareRequest.postId,
            postShareRequest.privacy,
            postShareRequest.content,
            postShareRequest.mentionUser
        ).flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun reportOrHidePost(
        postReportRequest: PostReportRequest
    ): Single<MeetFriendCommonResponse> {
        return postRetrofitAPI.reportOrHidePost(
            postReportRequest.postId,
            postReportRequest.content,
            postReportRequest.type
        ).flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun deletePost(postId: Int): Single<MeetFriendCommonResponse> {
        return postRetrofitAPI.deletePost(postId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun userViewPost(postId: String): Single<MeetFriendCommonResponse> {
        return postRetrofitAPI.postViewByUser(postId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getShareCount(postId: Int): Single<ShareCountResponse> {
        return postRetrofitAPI.getShareCount(PostIdRequest(postId))
            .flatMap { meetFriendResponseConverter.convertToSingle(it) }
    }

    fun getWaterMarkVideo(waterMarkVideoRequest: WaterMarkVideoRequest): Single<ShareFileResponse> {
        return postRetrofitAPI.getWaterMarkVideo(
            WaterMarkVideoRequest(
                waterMarkVideoRequest.fileName,
                waterMarkVideoRequest.postsId,
                waterMarkVideoRequest.url
            )
        )
    }

    fun getVideos(
        getShortsRequest: GetShortsRequest,
    ): Single<List<DataVideo>> {
        return postRetrofitAPI.getVideos(
            getShortsRequest.page,
            getShortsRequest.perPage,
            getShortsRequest.searchString,
            getShortsRequest.isFollowing
        ).flatMap { meetFriendResponseConverter.convertToSingle(it) }
            .map { it.videoData }
    }

    fun updateShortsCount(shortsCountRequest: ShortsCountRequest): Single<MeetFriendCommonResponses> {
        return postRetrofitAPI.updateShortsCount(
            shortsCountRequest
        ).flatMap { meetFriendResponseConverter.convertCommonResponses(it) }
    }

    fun addStory(
        addStoryRequest: AddStoryRequest
    ): Single<MeetFriendCommonResponse> {
        return postRetrofitAPI.addStory(
            addStoryRequest
        ).flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun reportLiveStreaming(reportLiveRequest: ReportLiveRequest): Single<MeetFriendResponse<LiveJoinResponse>> {
        return postRetrofitAPI.reportLiveStreaming(reportLiveRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun mentionUser(mentionUserRequest: MentionUserRequest): Single<MeetFriendResponse<List<MeetFriendUser>>> {
        return postRetrofitAPI.mentionUser(mentionUserRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getHashTagList(search: String): Single<MeetFriendResponse<HashTagResponse>> {
        return postRetrofitAPI.getHashTagList(search)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun editShort(editShortRequest: EditShortRequest): Single<MeetFriendCommonResponse> {
        return postRetrofitAPI.editShort(
            editShortRequest
        ).flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }
}
