package com.meetfriend.app.ui.trends.viewmodel

import com.meetfriend.app.api.hashtag.HashtagRepository
import com.meetfriend.app.api.hashtag.model.HashtagResponse
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CountryHashTagViewModel(
    private val hashtagRepository: HashtagRepository,
) : BasicViewModel() {

    private val hashTagsStateSubject: PublishSubject<CountryHashTagsViewState> =
        PublishSubject.create()
    val hashTagsState: Observable<CountryHashTagsViewState> = hashTagsStateSubject.hide()

    private var perPage = 15
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfCountryHashTag: ArrayList<HashtagResponse> = arrayListOf()

    private fun getCountryHashtag(countryCode: String) {
        hashtagRepository.getCountryHashtagList(
            countryCode,
            page = pageNo,
            perPage = perPage,
        ).doOnSubscribe {
            hashTagsStateSubject.onNext(CountryHashTagsViewState.LoadingState(true))
        }.doAfterTerminate {
            hashTagsStateSubject.onNext(CountryHashTagsViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (pageNo == 1) {
                    if (it.result?.listOfPosts?.size ?: 0 > 0) {
                        listOfCountryHashTag = it.result?.listOfPosts ?: arrayListOf()
                        hashTagsStateSubject.onNext(
                            CountryHashTagsViewState.HashtagList(
                                listOfCountryHashTag
                            )
                        )
                    } else {
                        listOfCountryHashTag.clear()
                        hashTagsStateSubject.onNext(CountryHashTagsViewState.EmptyState)
                    }
                } else {
                    if (it.result != null) {
                        it.result.listOfPosts?.let { it1 -> listOfCountryHashTag.addAll(it1) }
                        hashTagsStateSubject.onNext(
                            CountryHashTagsViewState.HashtagList(
                                listOfCountryHashTag
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }

            } else {
                hashTagsStateSubject.onNext(CountryHashTagsViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                hashTagsStateSubject.onNext(CountryHashTagsViewState.ErrorMessage(it))
            }

        }).autoDispose()
    }

    fun loadMoreCountryHashtag(countryCode: String) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getCountryHashtag(countryCode)
            }
        }
    }

    fun resetPaginationForCountryHashtag(countryCode: String) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getCountryHashtag(countryCode)
    }
}

sealed class CountryHashTagsViewState {
    data class ErrorMessage(val errorMessage: String) : CountryHashTagsViewState()
    data class SuccessMessage(val successMessage: String) : CountryHashTagsViewState()
    data class LoadingState(val isLoading: Boolean) : CountryHashTagsViewState()
    data class HashtagList(val hashtagResponse: ArrayList<HashtagResponse>) :
        CountryHashTagsViewState()

    object EmptyState : CountryHashTagsViewState()

}