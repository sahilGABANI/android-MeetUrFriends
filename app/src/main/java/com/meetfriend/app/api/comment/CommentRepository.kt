package com.meetfriend.app.api.comment

import com.meetfriend.app.api.post.model.PostCommentResponse
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import io.reactivex.Single

class CommentRepository(private val commentRetrofitAPI: CommentRetrofitAPI) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun commentPost(postId :Int,content :String,mentionId:String?) : Single<PostCommentResponse> {
        return commentRetrofitAPI.commentPost(postId,content,mentionId)
            .flatMap { meetFriendResponseConverter.convertToSingle(it) }
    }

    fun updateComment(postId :Int,content :String,mentionId:String?):Single<PostCommentResponse> {
        return commentRetrofitAPI.updateComment(postId,content,mentionId)
            .flatMap { meetFriendResponseConverter.convertToSingle(it) }
    }

    fun deleteComment(commentId: Int) :Single<MeetFriendCommonResponse> {
        return commentRetrofitAPI.deleteComment(commentId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun commentReply(postId :Int,content :String,parentId :Int,mentionId:String?): Single<PostCommentResponse> {
        return commentRetrofitAPI.replyComment(postId, content, parentId,mentionId)
            .flatMap { meetFriendResponseConverter.convertToSingle(it) }
    }

}