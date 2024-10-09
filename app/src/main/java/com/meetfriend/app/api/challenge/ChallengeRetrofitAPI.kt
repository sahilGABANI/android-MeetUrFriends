package com.meetfriend.app.api.challenge

import com.meetfriend.app.api.challenge.model.AddChallengeCommentRequest
import com.meetfriend.app.api.challenge.model.AddChallengePostCommentRequest
import com.meetfriend.app.api.challenge.model.ChallengeComment
import com.meetfriend.app.api.challenge.model.ChallengeCommentInfo
import com.meetfriend.app.api.challenge.model.ChallengeCommentRequest
import com.meetfriend.app.api.challenge.model.ChallengeCountRequest
import com.meetfriend.app.api.challenge.model.ChallengeInfo
import com.meetfriend.app.api.challenge.model.ChallengeItem
import com.meetfriend.app.api.challenge.model.ChallengeLikeUserInfo
import com.meetfriend.app.api.challenge.model.ChallengePostCommentRequest
import com.meetfriend.app.api.challenge.model.ChallengePostDeleteCommentRequest
import com.meetfriend.app.api.challenge.model.ChallengePostLikeRequest
import com.meetfriend.app.api.challenge.model.ChallengePostViewByUserRequest
import com.meetfriend.app.api.challenge.model.ChallengeReactions
import com.meetfriend.app.api.challenge.model.ChallengeUpdateCommentRequest
import com.meetfriend.app.api.challenge.model.CountryModel
import com.meetfriend.app.api.challenge.model.DeleteChallengePostRequest
import com.meetfriend.app.api.challenge.model.ReportChallengePostRequest
import com.meetfriend.app.api.challenge.model.ReportChallengeRequest
import com.meetfriend.app.newbase.network.model.MeetFriendCommonChallengeResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ChallengeRetrofitAPI {

    @FormUrlEncoded
    @POST("hashtag/challenges")
    fun hashTagsChallenge(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("search") search: String? = null,
        @Field("hashtag_id") hashtagId: Int
    ): Single<MeetFriendResponse<ChallengeInfo>>

    @GET("challenge/index")
    fun getChallengeList(
        @Query(
            "page"
        ) page: Int,
        @Query(
            "per_page"
        ) perPage: Int,
        @Query("is_my_challenge") isMyChallenge: Int,
        @Query("status") status: Int?
    ): Single<MeetFriendCommonChallengeResponse<ChallengeInfo>>

    @POST("challenge/create")
    fun createChatRoom(@Body createChallengeRequest: RequestBody): Single<MeetFriendCommonResponse>

    @POST("challenge/view")
    fun challengeView(
        @Body shortsCountRequest: ChallengeCountRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeItem>>

    @POST("challenge/like-unlike")
    fun challengeLikeUnLike(
        @Body shortsCountRequest: ChallengeCountRequest
    ): Single<MeetFriendCommonResponse>

    @GET("challenge/comment")
    fun getChallengeCommentList(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("challenge_id") challengeId: Int
    ): Single<MeetFriendCommonChallengeResponse<ChallengeCommentInfo>>

    @POST("challenge/comment/update")
    fun updateChallengeComment(
        @Body challengeCommentLikeRequest: ChallengeUpdateCommentRequest
    ): Single<MeetFriendResponse<ChallengeComment>>

    @POST("challenge/comment/like-unlike")
    fun challengeCommentLikeUnLike(
        @Body challengeCommentLikeRequest: ChallengeCommentRequest
    ): Single<MeetFriendCommonResponse>

    @POST("challenge/comment/delete")
    fun deleteChallengeComment(
        @Body challengeCommentLikeRequest: ChallengeCommentRequest
    ): Single<MeetFriendCommonResponse>

    @POST("challenge/comment")
    fun addChallengeComment(
        @Body challengeCommentLikeRequest: AddChallengeCommentRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeComment>>

    @GET("common/country")
    fun getAllCountries(): Single<MeetFriendResponse<ArrayList<CountryModel>>>

    @GET("common/state")
    fun getAllStates(@Query("country_id") countryId: String): Single<MeetFriendResponse<ArrayList<CountryModel>>>

    @GET("common/city")
    fun getAllCities(@Query("state_id") stateId: String): Single<MeetFriendResponse<ArrayList<CountryModel>>>

    @POST("challenge/view-by-user")
    fun challengeViewByUser(@Query("challenge_id") challengeId: String): Single<MeetFriendCommonResponse>

    @POST("challenge/report")
    fun reportChallenge(@Body reportChallengeRequest: ReportChallengeRequest): Single<MeetFriendResponse<ChallengeInfo>>

    @POST("challenge/delete")
    fun deleteChallenge(@Body challengeCountRequest: ChallengeCountRequest): Single<MeetFriendCommonResponse>

    @POST("challenge/post/delete")
    fun deleteChallengePost(@Body challengeCountRequest: DeleteChallengePostRequest): Single<MeetFriendCommonResponse>

    @POST("challenge/post/create")
    fun uploadChallengeAcceptFile(@Body file: RequestBody): Single<MeetFriendCommonResponse>

    @POST("challenge/accepted-rejected")
    fun challengeAcceptRejectPost(@Body challengeReactions: ChallengeReactions): Single<MeetFriendCommonResponse>

    @POST("challenge/view")
    fun challengeDetail(
        @Body challengeReactions: ChallengeCountRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeItem>>

    @POST("challenge/post/like-unlike")
    fun challengePostLikeUnlike(
        @Body challengePostLike: ChallengePostLikeRequest
    ): Single<MeetFriendCommonResponse>

    @POST("challenge/post/comment")
    fun addChallengePostComment(
        @Body challengeCommentLikeRequest: AddChallengePostCommentRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeComment>>

    @POST("challenge/post/comment/update")
    fun updateChallengePostComment(
        @Body challengeUpdateCommentRequest: ChallengeUpdateCommentRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeComment>>

    @GET("challenge/post/comment")
    fun getChallengePostCommentList(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("challenge_post_id") challengePostId: Int
    ): Single<MeetFriendCommonChallengeResponse<ChallengeCommentInfo>>

    @POST("challenge/post/comment/delete")
    fun deleteChallengePostComment(
        @Body challengeCountRequest: ChallengePostDeleteCommentRequest
    ): Single<MeetFriendCommonResponse>

    @POST("challenge/post/comment/like-unlike")
    fun challengePostCommentLikeUnLike(
        @Body challengeCommentLikeRequest: ChallengePostCommentRequest
    ): Single<MeetFriendCommonResponse>

    @POST("challenge/post/report")
    fun reportChallengePost(
        @Body reportChallengeRequest: ReportChallengePostRequest
    ): Single<MeetFriendResponse<ChallengeInfo>>

    @POST("challenge/post/view-by-user")
    fun challengePostViewByUser(
        @Body reportChallengeRequest: ChallengePostViewByUserRequest
    ): Single<MeetFriendCommonResponse>

    @GET("challenge/liked-users-listing")
    fun challengeLikedUserList(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("challenge_id") challengeId: Int,
    ): Single<MeetFriendResponse<ChallengeLikeUserInfo>>
}
