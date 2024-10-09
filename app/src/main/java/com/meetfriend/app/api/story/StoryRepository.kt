package com.meetfriend.app.api.story

import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponseForStory
import com.meetfriend.app.newbase.network.model.MeetFriendCommonStoryResponse
import com.meetfriend.app.ui.storywork.models.*
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody

class StoryRepository(private val storyRetrofitAPI: StoryRetrofitAPI) {

    private val meetFriendResponseConverter: MeetFriendResponseConverter =
        MeetFriendResponseConverter()

    fun addStory(
        multipartTypedOutput: Array<MultipartBody.Part?>?, type: RequestBody
    ): Single<MeetFriendCommonResponse> {
        return storyRetrofitAPI.addStory(multipartTypedOutput, type)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getStories(userId :String) :Single<StoryDetailResponse> {
        return storyRetrofitAPI.getStories(userId)
    }

    fun getListOfStories(pageNo: Int, perPageNo: Int): Single<MeetFriendCommonStoryResponse<ResultListResult>> {
        return storyRetrofitAPI.getStoriesList(pageNo, perPageNo)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForStory(it) }
    }

    fun viewStory(storyId: String) :Single<MeetFriendCommonResponse> {
        return storyRetrofitAPI.viewStory(storyId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getConversationId(receiverId: Int): Single<MeetFriendCommonResponseForStory> {
        return storyRetrofitAPI.getConversationId(receiverId)
            .flatMap { meetFriendResponseConverter.convertCommonResponsesStory(it) }
    }

    fun deleteStory(storyId: String) :Single<MeetFriendCommonResponse> {
        return storyRetrofitAPI.deleteStory(storyId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun reportStory(storyId: String,reason :String) :Single<MeetFriendCommonResponse> {
        return storyRetrofitAPI.reportStory(storyId,reason)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

}