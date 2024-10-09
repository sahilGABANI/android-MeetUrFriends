package com.meetfriend.app.api.follow

import com.meetfriend.app.api.follow.model.*
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import io.reactivex.Single

class FollowRepository(private val followRetrofitAPI: FollowRetrofitAPI) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun getHashtagUsers(page: Int, perPage: Int, search: String? = null): Single<MeetFriendResponse<FollowResult>> {
        return followRetrofitAPI.getHashtagUsers(page, perPage, search)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getFollowing(getFollowingRequest: GetFollowingRequest): Single<MeetFriendResponse<FollowResult>> {
        return followRetrofitAPI.getFollowing(getFollowingRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getFollowers(getFollowersRequest: GetFollowersRequest): Single<MeetFriendResponse<FollowResult>> {
        return followRetrofitAPI.getFollowers(getFollowersRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun followUnfollowUser(followUnfollowRequest: FollowUnfollowRequest): Single<MeetFriendCommonResponse> {
        return followRetrofitAPI.followUnfollowUser(followUnfollowRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getSuggestedUsers(search: String,page: Int, perPage: Int): Single<MeetFriendResponse<FollowResult>> {
        return followRetrofitAPI.getSuggestedUsers(search,page, perPage)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun cancelFriendRequest(friendId: Int): Single<MeetFriendCommonResponse> {
        return followRetrofitAPI.cancelFriendRequest(friendId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getFollowRequests(page: Int, perPage: Int): Single<MeetFriendResponse<FollowResult>> {
        return followRetrofitAPI.getFollowRequests(page, perPage)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun acceptFollowRequest(acceptFollowRequestRequest: AcceptFollowRequestRequest): Single<MeetFriendCommonResponse> {
        return followRetrofitAPI.acceptFollowRequest(acceptFollowRequestRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun rejectFollowRequest(acceptFollowRequestRequest: AcceptFollowRequestRequest): Single<MeetFriendCommonResponse> {
        return followRetrofitAPI.rejectFollowRequest(acceptFollowRequestRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }


    fun getUserForLiveInvite(getUserForLiveInviteRequest: GetUserForLiveInviteRequest): Single<MeetFriendResponse<FollowResult>> {
        return followRetrofitAPI.getUserForLiveInvite(getUserForLiveInviteRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun blockedUserList(perPage: Int, page: Int): Single<MeetFriendResponse<BlockResult>> {
        return followRetrofitAPI.blockedUserList(perPage, page)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun blockUnBlockUser(friendId: Int, blockStatus: String): Single<MeetFriendCommonResponse> {
        return followRetrofitAPI.blockUnBlockUser(friendId, blockStatus)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }


}