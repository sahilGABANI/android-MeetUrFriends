package com.meetfriend.app.newbase.network


import com.meetfriend.app.newbase.extension.onSafeError
import com.meetfriend.app.newbase.extension.onSafeSuccess
import com.meetfriend.app.newbase.network.model.*
import io.reactivex.Single

class MeetFriendResponseConverter {
    fun <T> convert(meetFriendResponse: MeetFriendResponse<T>?): Single<T> {
        return convertToSingle(meetFriendResponse)
    }

    fun <T> convertToSingle(meetFriendResponse: MeetFriendResponse<T>?): Single<T> {
        return Single.create { emitter ->
            when {
                meetFriendResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendResponse.status -> {
                    emitter.onSafeError(Exception(meetFriendResponse.message))
                }
                meetFriendResponse.status -> {
                    emitter.onSafeSuccess(meetFriendResponse.result)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleWithFullResponse(meetFriendResponse: MeetFriendResponse<T>?): Single<MeetFriendResponse<T>> {
        return Single.create { emitter ->
            when {
                meetFriendResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendResponse.status -> {
                    emitter.onSafeError(Exception(meetFriendResponse.message))
                }
                meetFriendResponse.status -> {
                    emitter.onSafeSuccess(meetFriendResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleWithFullResponseForLogOut(meetFriendResponse: MeetFriendResponse<T>?): Single<MeetFriendResponse<T>> {
        return Single.create { emitter ->
            when {
                meetFriendResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendResponse.success!! -> {
                    emitter.onSafeError(Exception(meetFriendResponse.message))
                }
                meetFriendResponse.success -> {
                    emitter.onSafeSuccess(meetFriendResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun convertCommonResponse(meetFriendCommonResponse: MeetFriendCommonResponse): Single<MeetFriendCommonResponse> {
        return Single.create { emitter ->
            when {
                meetFriendCommonResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendCommonResponse.status -> {
                    emitter.onSafeError(Exception(meetFriendCommonResponse.message))
                }
                meetFriendCommonResponse.status -> {
                    emitter.onSafeSuccess(meetFriendCommonResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun convertCommonResponses(meetFriendCommonResponses: MeetFriendCommonResponses): Single<MeetFriendCommonResponses> {
        return Single.create { emitter ->
            when {
                meetFriendCommonResponses == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendCommonResponses.status -> {
                    emitter.onSafeError(Exception(meetFriendCommonResponses.message))
                }
                meetFriendCommonResponses.status -> {
                    emitter.onSafeSuccess(meetFriendCommonResponses)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun convertCommonResponsesStory(meetFriendCommonResponseForStory: MeetFriendCommonResponseForStory): Single<MeetFriendCommonResponseForStory> {
        return Single.create { emitter ->
            when {
                meetFriendCommonResponseForStory == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendCommonResponseForStory.status -> {
                    emitter.onSafeError(Exception(meetFriendCommonResponseForStory.message))
                }
                meetFriendCommonResponseForStory.status -> {
                    emitter.onSafeSuccess(meetFriendCommonResponseForStory)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleWithFullResponseForCreateChatRoom(meetFriendResponse: MeetFriendResponseForChat<T>?): Single<MeetFriendResponseForChat<T>> {
        return Single.create { emitter ->
            when {
                meetFriendResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendResponse.status -> {
                    emitter.onSafeError(Exception(meetFriendResponse.message))
                }
                meetFriendResponse.status -> {
                    emitter.onSafeSuccess(meetFriendResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleWithResponseForCreateChatRoom(meetFriendResponse: MeetFriendResponseForChat<T>?): Single<T> {
        return Single.create { emitter ->
            when {
                meetFriendResponse?.data == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendResponse.status -> {
                    emitter.onSafeError(Exception(meetFriendResponse.message))
                }
                meetFriendResponse.status -> {
                    emitter.onSafeSuccess(meetFriendResponse.data)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleWithFullResponseForGetProfile(meetFriendResponse: MeetFriendResponseForGetProfile<T>?): Single<MeetFriendResponseForGetProfile<T>> {
        return Single.create { emitter ->
            when {
                meetFriendResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendResponse.status -> {
                    emitter.onSafeError(Exception(meetFriendResponse.message))
                }
                meetFriendResponse.status -> {
                    emitter.onSafeSuccess(meetFriendResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleWithFullResponseForStory(meetFriendResponse: MeetFriendCommonStoryResponse<T>?): Single<MeetFriendCommonStoryResponse<T>> {
        return Single.create { emitter ->
            when {
                meetFriendResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendResponse.status -> {
                    emitter.onSafeError(Exception(meetFriendResponse.message))
                }
                meetFriendResponse.status -> {
                    emitter.onSafeSuccess(meetFriendResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleForLive(meetFriendResponse: MeetFriendResponseForLive<T>?): Single<MeetFriendResponseForLive<T>> {
        return Single.create { emitter ->
            when {
                meetFriendResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendResponse.status -> {
                    emitter.onSafeError(Exception(meetFriendResponse.message))
                }
                meetFriendResponse.status -> {
                    emitter.onSafeSuccess(meetFriendResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }


    fun <T> convertToSingleForChallenge(meetFriendResponse: MeetFriendCommonChallengeResponse<T>?): Single<MeetFriendCommonChallengeResponse<T>> {
        return Single.create { emitter ->
            when {
                meetFriendResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !meetFriendResponse.status -> {
                    emitter.onSafeError(Exception(meetFriendResponse.message))
                }
                meetFriendResponse.status -> {
                    emitter.onSafeSuccess(meetFriendResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }
}