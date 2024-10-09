package com.meetfriend.app.api.model

import com.meetfriend.app.api.post.model.PostInformation
import com.meetfriend.app.api.post.model.SharedPost

sealed class HomePagePostInfoState {
    data class UserProfileClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class OptionsClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class PostMediaClick(val postInfo: PostInformation, val position: Int) : HomePagePostInfoState()
    data class PostLikeClick(val postInfo: PostInformation) : HomePagePostInfoState()

    data class AddPostLikeClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class RemovePostLikeClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class PostLikeCountClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class CommentClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class ShareClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class RepostClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class TagPeopleClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class TagPeopleFirstUserClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class OtherUserProfileClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class TagRepostPeopleClick(val postInfo: SharedPost?) : HomePagePostInfoState()
    data class TagRepostPeopleFirstUserClick(val postInfo: SharedPost?) : HomePagePostInfoState()
    data class PostMediaVideoClick(val postInfo: PostInformation, val position: Int) : HomePagePostInfoState()
    data class MentionUserNavigationClick(val userId : Int) : HomePagePostInfoState()
    data class MentionHashTagNavigationClick(val hashTagId : Int, val hashTagName : String) : HomePagePostInfoState()
    data class ShowUserNotFoundToast(val isShow :Boolean) : HomePagePostInfoState()
    data class ShowHashtagNotFoundToast(val isShow :Boolean) : HomePagePostInfoState()
    data class GiftClick(val postInfo: PostInformation) : HomePagePostInfoState()
    data class OpenLinkAttachment(val postInfo: PostInformation) : HomePagePostInfoState()
    data class OpenLinkAttachmentMultiple(val uri: String) : HomePagePostInfoState()
}