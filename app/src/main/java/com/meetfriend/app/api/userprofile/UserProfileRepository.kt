package com.meetfriend.app.api.userprofile

import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.post.model.PostResponses
import com.meetfriend.app.api.userprofile.model.ReportUser
import com.meetfriend.app.api.userprofile.model.ReportUserRequest
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.responseclasses.UpdatePhotoResponse
import com.meetfriend.app.responseclasses.photos.UserPhotosResponse
import io.reactivex.Single
import okhttp3.MultipartBody

class UserProfileRepository(private val userProfileRetrofitAPI: UserProfileRetrofitAPI) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun viewProfile(id: Int): Single<MeetFriendUser> {
        return userProfileRetrofitAPI.viewProfile(id)
            .flatMap {
                meetFriendResponseConverter.convertToSingle(it)
            }
    }

    fun editUserProfile(
        firstName: String?, lastName: String?,userName: String?, education: String?, gender: String?, city: String?, hobbies: String?, work: String?, dob: String?, bio: String?, relationship: String?, dob_string: String?,isPrivate:Int?
    ): Single<  MeetFriendResponse<MeetFriendUser>> {
        return userProfileRetrofitAPI.editProfileInfo(firstName, lastName,userName, education, gender, city, hobbies, work, dob, bio, relationship, dob_string,isPrivate)
            .flatMap {
                meetFriendResponseConverter.convertToSingleWithFullResponse(it)
            }
    }

    fun uploadProfileImage(requestBody: MultipartBody) : Single<UpdatePhotoResponse> {
        return userProfileRetrofitAPI.uploadProfilePicture(requestBody)
    }

    fun userPhotosVideos(id : Int,page:Int,perPage: Int) :Single<UserPhotosResponse> {
        return userProfileRetrofitAPI.userPhotosVideos(id,page, perPage)
    }
    fun userVideos(id : Int,page:Int,perPage: Int) :Single<UserPhotosResponse> {
        return userProfileRetrofitAPI.userVideos(id,page, perPage)
    }

    fun userPosts(id : Int,page:Int,perPage: Int) :Single<MeetFriendResponse<PostResponses>> {
        return userProfileRetrofitAPI.userPosts(id,page, perPage)
    }
    fun blockUnBlockPeople(friendId :Int,blockStatus :String) :Single<MeetFriendCommonResponse> {
        return userProfileRetrofitAPI.blockUnBlockPeople(friendId,blockStatus)
            .flatMap {
                meetFriendResponseConverter.convertCommonResponse(it)
            }
    }

    fun reportUser(reportUserRequest: ReportUserRequest) :Single<MeetFriendResponse<ReportUser>> {
        return userProfileRetrofitAPI.reportUser(reportUserRequest)
            .flatMap {
                meetFriendResponseConverter.convertToSingleWithFullResponse(it)
            }
    }
}