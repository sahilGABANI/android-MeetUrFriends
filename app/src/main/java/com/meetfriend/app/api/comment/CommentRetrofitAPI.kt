package com.meetfriend.app.api.comment

import com.meetfriend.app.api.post.model.PostCommentResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface CommentRetrofitAPI {

    @FormUrlEncoded
    @POST("post/comment")
    fun commentPost(@Field("post_id") postId : Int,@Field("content") content : String,@Field("mention_ids") mention_ids : String?): Single<MeetFriendResponse<PostCommentResponse>>

    @FormUrlEncoded
    @POST("post/comment/update")
    fun updateComment(@Field("comment_id") commentId : Int,@Field("content") content : String,@Field("mention_ids") mention_ids : String?): Single<MeetFriendResponse<PostCommentResponse>>

    @FormUrlEncoded
    @POST("post/comment/delete")
    fun deleteComment(@Field("comment_id") commentId : Int): Single<MeetFriendCommonResponse>

    @FormUrlEncoded
    @POST("post/comment/reply")
    fun replyComment(@Field("post_id") postId : Int,@Field("content") content : String,@Field("parent_id") parentId : Int,@Field("mention_ids") mention_ids : String?): Single<MeetFriendResponse<PostCommentResponse>>

}