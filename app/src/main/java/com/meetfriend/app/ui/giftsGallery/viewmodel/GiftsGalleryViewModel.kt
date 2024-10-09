package com.meetfriend.app.ui.giftsGallery.viewmodel

import com.meetfriend.app.api.gift.GiftsRepository
import com.meetfriend.app.api.gift.model.GiftsItemInfo
import com.meetfriend.app.api.gift.model.MyEarningInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GiftsGalleryViewModel(
    private val giftsRepository: GiftsRepository,
) : BasicViewModel() {

    private val giftsGalleryStateSubject: PublishSubject<GiftsGalleryViewState> =
        PublishSubject.create()
    val giftsGalleryState: Observable<GiftsGalleryViewState> = giftsGalleryStateSubject.hide()

    private var pageNo = 1
    private var perPage = 20
    private var isLoading = false
    private var isLoadMore = true

    private var listOfGifts: MutableList<GiftsItemInfo> = mutableListOf()

    fun getGifts() {
        giftsRepository.getGifts(
            page = pageNo, perPage = perPage
        ).doOnSubscribe {
            giftsGalleryStateSubject.onNext(GiftsGalleryViewState.LoadingState(true))
        }.doAfterTerminate {
            giftsGalleryStateSubject.onNext(GiftsGalleryViewState.LoadingState(false))
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (pageNo == 1) {
                    if (!it.result?.data.isNullOrEmpty()) {
                        listOfGifts = it.result?.data?.toMutableList() ?: mutableListOf()
                        giftsGalleryStateSubject.onNext(
                            GiftsGalleryViewState.GiftsData(
                                listOfGifts
                            )
                        )
                    } else {
                        giftsGalleryStateSubject.onNext(GiftsGalleryViewState.EmptyState)
                    }
                } else {
                    if (it.result?.data != null) {
                        listOfGifts.addAll(it.result.data)
                        giftsGalleryStateSubject.onNext(
                            GiftsGalleryViewState.GiftsData(
                                listOfGifts
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }

            } else {
                giftsGalleryStateSubject.onNext(GiftsGalleryViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                giftsGalleryStateSubject.onNext(GiftsGalleryViewState.ErrorMessage(it))
            }

        }).autoDispose()
    }

    fun loadMoreGifts() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getGifts()
            }
        }
    }

    fun resetPaginationForGifts() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getGifts()
    }

    fun getMyEarningInfo() {
        giftsRepository.getMyEarning().doOnSubscribe {
            giftsGalleryStateSubject.onNext(GiftsGalleryViewState.LoadingState(true))
        }.doAfterTerminate {
            giftsGalleryStateSubject.onNext(GiftsGalleryViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftsGalleryStateSubject.onNext(GiftsGalleryViewState.MyEarningData(it.result))
            } else {
                giftsGalleryStateSubject.onNext(GiftsGalleryViewState.ErrorMessage(it.message.toString()))

            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                giftsGalleryStateSubject.onNext(GiftsGalleryViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun sendGiftPost(toId: String, postId: String, coins: Double, giftId: Int) {
        giftsRepository.sendGiftPost(toId, postId, coins, giftId).doOnSubscribe {
            giftsGalleryStateSubject.onNext(GiftsGalleryViewState.LoadingState(true))
        }.doAfterTerminate {
            giftsGalleryStateSubject.onNext(GiftsGalleryViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftsGalleryStateSubject.onNext(GiftsGalleryViewState.SuccessMessage(it.message.toString()))
            } else {
                giftsGalleryStateSubject.onNext(GiftsGalleryViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                giftsGalleryStateSubject.onNext(GiftsGalleryViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
}

sealed class GiftsGalleryViewState {
    data class ErrorMessage(val errorMessage: String) : GiftsGalleryViewState()
    data class SuccessMessage(val successMessage: String) : GiftsGalleryViewState()
    data class LoadingState(val isLoading: Boolean) : GiftsGalleryViewState()
    data class GiftsData(val listOfGifts: List<GiftsItemInfo>) : GiftsGalleryViewState()
    data class MyEarningData(val myEarningInfo: MyEarningInfo?) : GiftsGalleryViewState()
    object EmptyState : GiftsGalleryViewState()

}