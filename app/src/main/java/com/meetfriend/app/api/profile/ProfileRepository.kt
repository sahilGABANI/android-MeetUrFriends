package com.meetfriend.app.api.profile

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.ChatUser
import com.meetfriend.app.api.authentication.model.DeviceIdRequest
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.api.profile.model.ChangeProfileRequest
import com.meetfriend.app.api.profile.model.ChatRoomUserProfileInfo
import com.meetfriend.app.api.profile.model.CheckUserRequest
import com.meetfriend.app.api.profile.model.CreateChatRoomUserRequest
import com.meetfriend.app.api.profile.model.FeedbackInfo
import com.meetfriend.app.api.profile.model.GetProfileInfoRequest
import com.meetfriend.app.api.profile.model.HelpFormInfo
import com.meetfriend.app.api.profile.model.ProfileValidationInfo
import com.meetfriend.app.api.profile.model.SendFeedbackRequest
import com.meetfriend.app.api.profile.model.SendHelpRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForGetProfile
import contractorssmart.app.utilsclasses.PreferenceHandler
import io.reactivex.Single

class ProfileRepository(
    private val profileRetrofitAPI: ProfileRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache
) {
    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun deleteAccount(deleteAccountRequest: FollowUnfollowRequest): Single<MeetFriendCommonResponse> {
        return profileRetrofitAPI.deleteAccount(deleteAccountRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun createChatRoomUser(createChatRoomUserRequest: CreateChatRoomUserRequest): Single<MeetFriendCommonResponse> {
        return profileRetrofitAPI.createChatRoomUser(createChatRoomUserRequest)
            .doAfterSuccess {
                loggedInUserCache.setIsChatUserCreated(true)
            }.flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun changeProfile(changeProfileRequest: ChangeProfileRequest): Single<MeetFriendCommonResponse> {
        return profileRetrofitAPI.changeProfile(changeProfileRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getChatRoomUserProfile(perPage: Int): Single<MeetFriendResponseForGetProfile<ChatRoomUserProfileInfo>> {
        return profileRetrofitAPI.getChatRoomUserProfile(perPage)
            .doAfterSuccess {
                if (it.result != null) {
                    val name = it.data?.chatUserName ?: ""
                    if (it.result.data?.size ?: 0 > 0) {
                        val profileImage = it.result.data?.get(0)?.filePath ?: ""
                        loggedInUserCache.setChatUser(ChatUser(name, profileImage))
                    }
                }
            }
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForGetProfile(it) }
    }

    fun getOtherUserProfile(
        userId: Int,
        perPage: Int
    ): Single<MeetFriendResponseForGetProfile<ChatRoomUserProfileInfo>> {
        return profileRetrofitAPI.getOtherUserProfile(userId, perPage)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForGetProfile(it) }
    }

    fun deleteProfileImage(id: Int): Single<MeetFriendCommonResponse> {
        return profileRetrofitAPI.deleteProfileImage(id)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun sendFeedback(sendFeedbackRequest: SendFeedbackRequest): Single<MeetFriendResponseForChat<FeedbackInfo>> {
        return profileRetrofitAPI.sendFeedback(sendFeedbackRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun sendHelp(sendHelpRequest: SendHelpRequest): Single<MeetFriendResponseForChat<HelpFormInfo>> {
        return profileRetrofitAPI.sendHelp(sendHelpRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun getProfileInfo(userId: Int): Single<MeetFriendResponse<ProfileValidationInfo>> {
        return profileRetrofitAPI.getProfileInfo(GetProfileInfoRequest(userId))
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun checkUser(userId: String): Single<MeetFriendCommonResponse> {
        return profileRetrofitAPI.checkUser(CheckUserRequest(userId))
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun logOutAll(userId: String): Single<MeetFriendCommonResponse> {
        return profileRetrofitAPI.logOutAll(DeviceIdRequest(userId))
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun logOut(deviceId: String): Single<MeetFriendResponse<MeetFriendUser>> {
        return profileRetrofitAPI.logOut(deviceId).doAfterSuccess { meetFriendResponse ->
            loggedInUserCache.setLoggedInUserToken(meetFriendResponse.accessToken)
            loggedInUserCache.setLoggedInUser(meetFriendResponse.data)
            PreferenceHandler.writeString(
                MeetFriendApplication.context,
                PreferenceHandler.AUTHORIZATION_TOKEN,
                meetFriendResponse.accessToken ?: ""
            )
        }.flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForLogOut(it) }
    }
}
