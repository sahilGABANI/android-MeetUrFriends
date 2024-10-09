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
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonChallengeResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single
import okhttp3.RequestBody
class ChallengeRepository(
    private val challengeRetrofitAPI: ChallengeRetrofitAPI,
) {
    private val meetFriendResponseConverter: MeetFriendResponseConverter = MeetFriendResponseConverter()

    fun hashTagsChallenge(
        page: Int,
        perPage: Int,
        hashTagId: Int,
        search: String? = null
    ): Single<MeetFriendResponse<ChallengeInfo>> {
        return challengeRetrofitAPI.hashTagsChallenge(page, perPage, search, hashTagId).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun challengeLikedUserList(
        page: Int,
        perPage: Int,
        challengeId: Int
    ): Single<MeetFriendResponse<ChallengeLikeUserInfo>> {
        return challengeRetrofitAPI.challengeLikedUserList(page, perPage, challengeId).flatMap {
            meetFriendResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getChallengeList(
        page: Int,
        perPage: Int,
        isMyChallenge: Int,
        status: Int?
    ): Single<MeetFriendCommonChallengeResponse<ChallengeInfo>> {
        return challengeRetrofitAPI.getChallengeList(page, perPage, isMyChallenge, status).flatMap {
            meetFriendResponseConverter.convertToSingleForChallenge(it)
        }
    }

    fun challengeViewByUser(challengeId: String): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.challengeViewByUser(challengeId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun createChatRoom(createChallengeRequest: RequestBody): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.createChatRoom(createChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun challengeView(
        createChallengeRequest: ChallengeCountRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeItem>> {
        return challengeRetrofitAPI.challengeView(createChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleForChallenge(it) }
    }
    fun challengeLikeUnLike(createChallengeRequest: ChallengeCountRequest): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.challengeLikeUnLike(createChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getChallengeCommentList(
        page: Int,
        perPage: Int,
        challengeId: Int
    ): Single<MeetFriendCommonChallengeResponse<ChallengeCommentInfo>> {
        return challengeRetrofitAPI.getChallengeCommentList(page, perPage, challengeId).flatMap {
            meetFriendResponseConverter.convertToSingleForChallenge(it)
        }
    }

    fun challengeCommentLikeUnLike(createChallengeRequest: ChallengeCommentRequest): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.challengeCommentLikeUnLike(createChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun updateChallengeComment(
        challengeCommentLikeRequest: ChallengeUpdateCommentRequest
    ): Single<MeetFriendResponse<ChallengeComment>> {
        return challengeRetrofitAPI.updateChallengeComment(challengeCommentLikeRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun deleteChallengeComment(createChallengeRequest: ChallengeCommentRequest): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.deleteChallengeComment(createChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun addChallengeComment(
        createChallengeRequest: AddChallengeCommentRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeComment>> {
        return challengeRetrofitAPI.addChallengeComment(createChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleForChallenge(it) }
    }

    fun getAllCountryList(): Single<MeetFriendResponse<ArrayList<CountryModel>>> {
        return challengeRetrofitAPI.getAllCountries()
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getAllStates(countryId: String): Single<MeetFriendResponse<ArrayList<CountryModel>>> {
        return challengeRetrofitAPI.getAllStates(countryId)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getAllCities(stateId: String): Single<MeetFriendResponse<ArrayList<CountryModel>>> {
        return challengeRetrofitAPI.getAllCities(stateId)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun reportChallenge(reportChallengeRequest: ReportChallengeRequest): Single<MeetFriendResponse<ChallengeInfo>> {
        return challengeRetrofitAPI.reportChallenge(reportChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun deleteChallenge(challengeCountRequest: ChallengeCountRequest): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.deleteChallenge(challengeCountRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }
    fun deleteChallengePost(challengeCountRequest: DeleteChallengePostRequest): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.deleteChallengePost(challengeCountRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun challengeAcceptRejectPost(challengeReactions: ChallengeReactions): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.challengeAcceptRejectPost(challengeReactions)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun uploadChallengeAcceptFile(file: RequestBody): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.uploadChallengeAcceptFile(file)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }
    fun challengeDetail(
        challengeReactions: ChallengeCountRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeItem>> {
        return challengeRetrofitAPI.challengeDetail(challengeReactions)
            .flatMap { meetFriendResponseConverter.convertToSingleForChallenge(it) }
    }

    fun challengePostLikeUnlike(challengePostLikeRequest: ChallengePostLikeRequest): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.challengePostLikeUnlike(challengePostLikeRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun addChallengePostComment(
        createChallengeRequest: AddChallengePostCommentRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeComment>> {
        return challengeRetrofitAPI.addChallengePostComment(createChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleForChallenge(it) }
    }

    fun updateChallengePostComment(
        challengeUpdateCommentRequest: ChallengeUpdateCommentRequest
    ): Single<MeetFriendCommonChallengeResponse<ChallengeComment>> {
        return challengeRetrofitAPI.updateChallengePostComment(challengeUpdateCommentRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleForChallenge(it) }
    }

    fun getChallengePostCommentList(
        page: Int,
        perPage: Int,
        challengeId: Int
    ): Single<MeetFriendCommonChallengeResponse<ChallengeCommentInfo>> {
        return challengeRetrofitAPI.getChallengePostCommentList(page, perPage, challengeId).flatMap {
            meetFriendResponseConverter.convertToSingleForChallenge(it)
        }
    }

    fun deleteChallengePostComment(
        createChallengeRequest: ChallengePostDeleteCommentRequest
    ): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.deleteChallengePostComment(createChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun challengePostCommentLikeUnLike(
        createChallengeRequest: ChallengePostCommentRequest
    ): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.challengePostCommentLikeUnLike(createChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun reportChallengePost(
        reportChallengeRequest: ReportChallengePostRequest
    ): Single<MeetFriendResponse<ChallengeInfo>> {
        return challengeRetrofitAPI.reportChallengePost(reportChallengeRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun challengePostViewByUser(
        challengePostViewByUserRequest: ChallengePostViewByUserRequest
    ): Single<MeetFriendCommonResponse> {
        return challengeRetrofitAPI.challengePostViewByUser(challengePostViewByUserRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }
}
