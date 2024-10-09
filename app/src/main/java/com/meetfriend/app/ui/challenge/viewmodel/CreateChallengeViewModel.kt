package com.meetfriend.app.ui.challenge.viewmodel

import com.meetfriend.app.api.challenge.ChallengeRepository
import com.meetfriend.app.api.challenge.model.CountryModel
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.post.model.HashTagsResponse
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.RequestBody
import timber.log.Timber

class CreateChallengeViewModel(
    private val challengeRepository: ChallengeRepository,
    private val postRepository: PostRepository
) : BasicViewModel() {

    private val addNewChallengeStateSubjects: PublishSubject<AddNewChallengeViewState> = PublishSubject.create()
    val addNewChallengeState: Observable<AddNewChallengeViewState> = addNewChallengeStateSubjects.hide()
    private var selectedHashTagInfo: MutableList<HashTagsResponse> = mutableListOf()

    fun getHashTagList(searchText: String) {
        postRepository.getHashTagList(searchText)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.result.let {
                    addNewChallengeStateSubjects.onNext(
                        AddNewChallengeViewState.HashTagListForMention(
                            it?.data ?: arrayListOf()
                        )
                    )
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

    fun searchHashTagClicked(
        initialDescriptionString: String,
        subString: String,
        user: HashTagsResponse,
    ) {
        if (user !in selectedHashTagInfo) {
            selectedHashTagInfo.add(user)
        }
        val remainString = initialDescriptionString.removePrefix(subString)

        if (subString.length == initialDescriptionString.length) {
            val lastIndexOfToken =
                initialDescriptionString.findLastAnyOf(listOf("#"))?.first ?: return
            val tempSubString = initialDescriptionString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString${user.tagName}"
            addNewChallengeStateSubjects.onNext(
                AddNewChallengeViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        } else {
            val lastIndexOfToken = subString.findLastAnyOf(listOf("#"))?.first ?: return
            val tempSubString = subString.substring(0, lastIndexOfToken)
            val descriptionString = "$tempSubString ${user.tagName} $remainString"
            addNewChallengeStateSubjects.onNext(
                AddNewChallengeViewState.UpdateDescriptionText(
                    descriptionString.plus(" ")
                )
            )
        }
    }

    fun createPost(createChallengeRequest: RequestBody) {
        challengeRepository.createChatRoom(createChallengeRequest)
            .doOnSubscribe {
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(false))
                if (response.status) {
                    addNewChallengeStateSubjects.onNext(
                        AddNewChallengeViewState.CreatePostSuccessMessage(response.challengeId)
                    )
                }
            }, { throwable ->
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getAllCountryList() {
        challengeRepository.getAllCountryList()
            .doOnSubscribe {
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(false))
                response?.result?.let {
                    addNewChallengeStateSubjects.onNext(
                        AddNewChallengeViewState.AllCountryListResponse(
                            it
                        )
                    )
                }
            }, { throwable ->
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getAllStates(countryId: String) {
        challengeRepository.getAllStates(countryId)
            .doOnSubscribe {
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(false))
                response?.result?.let {
                    addNewChallengeStateSubjects.onNext(
                        AddNewChallengeViewState.AllStateListResponse(
                            it
                        )
                    )
                }
            }, { throwable ->
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getAllCities(stateId: String) {
        challengeRepository.getAllCities(stateId)
            .doOnSubscribe {
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(false))
                response?.result?.let {
                    addNewChallengeStateSubjects.onNext(
                        AddNewChallengeViewState.AllCityListResponse(
                            it
                        )
                    )
                }
            }, { throwable ->
                addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    addNewChallengeStateSubjects.onNext(AddNewChallengeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class AddNewChallengeViewState {
    data class ErrorMessage(val errorMessage: String) : AddNewChallengeViewState()
    data class SuccessMessage(val successMessage: String) : AddNewChallengeViewState()
    data class LoadingState(val isLoading: Boolean) : AddNewChallengeViewState()
    data class AllCountryListResponse(val countryList: ArrayList<CountryModel>) :
        AddNewChallengeViewState()

    data class AllStateListResponse(val stateList: ArrayList<CountryModel>) :
        AddNewChallengeViewState()

    data class AllCityListResponse(val cityList: ArrayList<CountryModel>) :
        AddNewChallengeViewState()

    data class CreatePostSuccessMessage(val challengeId: Int?) : AddNewChallengeViewState()
    data class HashTagListForMention(val listOfUserForMention: List<HashTagsResponse>?) : AddNewChallengeViewState()
    data class UpdateDescriptionText(val descriptionString: String) : AddNewChallengeViewState()
}
