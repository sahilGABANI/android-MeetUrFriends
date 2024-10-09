package com.meetfriend.app.ui.giftsGallery.viewmodel

import com.meetfriend.app.api.gift.GiftsRepository
import com.meetfriend.app.api.gift.model.GiftsItemInfo
import com.meetfriend.app.api.gift.model.MyEarningInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GiftGalleryBottomSheetViewModel(private val giftsRepository: GiftsRepository) :
    BasicViewModel() {

    private val giftsGalleryBottomSheetStateSubject: PublishSubject<GiftsGalleryBottomSheetViewState> =
        PublishSubject.create()
    val giftsGalleryBottomSheetState: Observable<GiftsGalleryBottomSheetViewState> =
        giftsGalleryBottomSheetStateSubject.hide()

    private var pageNo = 1
    private var perPage = 16
    private var isLoading = false
    private var isLoadMore = true

    private var listOfGifts: MutableList<GiftsItemInfo> = mutableListOf()

    fun getGifts() {
        giftsRepository.getGifts(
            page = pageNo, perPage = perPage
        ).doOnSubscribe {
            giftsGalleryBottomSheetStateSubject.onNext(
                GiftsGalleryBottomSheetViewState.LoadingState(
                    true
                )
            )
        }.doAfterTerminate {
            giftsGalleryBottomSheetStateSubject.onNext(
                GiftsGalleryBottomSheetViewState.LoadingState(
                    false
                )
            )
            isLoading = false
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (pageNo == 1) {
                    if (!it.result?.data.isNullOrEmpty()) {
                        listOfGifts = it.result?.data?.toMutableList() ?: mutableListOf()
                        giftsGalleryBottomSheetStateSubject.onNext(
                            GiftsGalleryBottomSheetViewState.GiftsData(
                                listOfGifts
                            )
                        )
                    } else {
                        giftsGalleryBottomSheetStateSubject.onNext(GiftsGalleryBottomSheetViewState.EmptyState)
                    }
                } else {
                    if (!it.result?.data.isNullOrEmpty()) {
                        it.result?.data?.let { it1 -> listOfGifts.addAll(it1) }
                        giftsGalleryBottomSheetStateSubject.onNext(
                            GiftsGalleryBottomSheetViewState.GiftsData(
                                listOfGifts
                            )
                        )
                    } else {
                        isLoadMore = false
                    }
                }

            } else {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.ErrorMessage(
                        it.message.toString()
                    )
                )
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.ErrorMessage(
                        it
                    )
                )
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

    fun sendGiftPost(toId: String, postId: String, coins: Double, giftId: Int) {
        giftsRepository.sendGiftPost(toId, postId, coins, giftId).doOnSubscribe {
            giftsGalleryBottomSheetStateSubject.onNext(
                GiftsGalleryBottomSheetViewState.LoadingState(
                    true
                )
            )
        }.doAfterTerminate {
            giftsGalleryBottomSheetStateSubject.onNext(
                GiftsGalleryBottomSheetViewState.LoadingState(
                    false
                )
            )
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.SuccessMessage(
                        it.message.toString()
                    )
                )
            } else {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.ErrorMessage(
                        it.message.toString()
                    )
                )
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.ErrorMessage(
                        it
                    )
                )
            }
        }).autoDispose()
    }

    fun sendGiftInLive(toId: String, postId: String, coins: Double, giftId: Int, quantity: Int) {
        giftsRepository.sendGiftInLive(toId, postId, coins, giftId, quantity).doOnSubscribe {
            giftsGalleryBottomSheetStateSubject.onNext(
                GiftsGalleryBottomSheetViewState.LoadingState(
                    true
                )
            )
        }.doAfterTerminate {
            giftsGalleryBottomSheetStateSubject.onNext(
                GiftsGalleryBottomSheetViewState.LoadingState(
                    false
                )
            )
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.SuccessMessage(
                        it.message.toString()
                    )
                )
            } else {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.ErrorMessage(
                        it.message.toString()
                    )
                )
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.ErrorMessage(
                        it
                    )
                )
            }
        }).autoDispose()
    }

    fun getMyEarningInfo() {
        giftsRepository.getMyEarning().doOnSubscribe {
            giftsGalleryBottomSheetStateSubject.onNext(
                GiftsGalleryBottomSheetViewState.LoadingState(
                    true
                )
            )
        }.doAfterTerminate {
            giftsGalleryBottomSheetStateSubject.onNext(
                GiftsGalleryBottomSheetViewState.LoadingState(
                    false
                )
            )
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.MyEarningData(
                        it.result
                    )
                )
            } else {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.ErrorMessage(
                        it.message.toString()
                    )
                )

            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                giftsGalleryBottomSheetStateSubject.onNext(
                    GiftsGalleryBottomSheetViewState.ErrorMessage(
                        it
                    )
                )
            }
        }).autoDispose()
    }
}

sealed class GiftsGalleryBottomSheetViewState {
    data class ErrorMessage(val errorMessage: String) : GiftsGalleryBottomSheetViewState()
    data class SuccessMessage(val successMessage: String) : GiftsGalleryBottomSheetViewState()
    data class LoadingState(val isLoading: Boolean) : GiftsGalleryBottomSheetViewState()
    data class GiftsData(val listOfGifts: List<GiftsItemInfo>) : GiftsGalleryBottomSheetViewState()
    data class MyEarningData(val myEarningInfo: MyEarningInfo?) : GiftsGalleryBottomSheetViewState()
    object EmptyState : GiftsGalleryBottomSheetViewState()

}