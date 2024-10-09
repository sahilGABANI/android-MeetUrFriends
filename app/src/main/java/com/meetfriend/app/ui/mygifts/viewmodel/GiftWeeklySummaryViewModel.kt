package com.meetfriend.app.ui.mygifts.viewmodel

import com.meetfriend.app.api.gift.GiftsRepository
import com.meetfriend.app.api.gift.model.GiftWeeklyInfo
import com.meetfriend.app.api.monetization.model.EarningListRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GiftWeeklySummaryViewModel(private val giftsRepository: GiftsRepository) : BasicViewModel() {


    private val giftSummaryStateSubject: PublishSubject<GiftWeeklySummaryViewState> =
        PublishSubject.create()
    val giftSummaryState: Observable<GiftWeeklySummaryViewState> = giftSummaryStateSubject.hide()

    fun giftWeeklySummary(request: EarningListRequest) {
        giftsRepository.giftWeeklySummary(request).doOnSubscribe {
            giftSummaryStateSubject.onNext(GiftWeeklySummaryViewState.LoadingState(true))
        }.doAfterTerminate {
            giftSummaryStateSubject.onNext(GiftWeeklySummaryViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftSummaryStateSubject.onNext(GiftWeeklySummaryViewState.TotalEarningAmount(it.totalEarning))

                if (!it.result.isNullOrEmpty())
                    giftSummaryStateSubject.onNext(
                        GiftWeeklySummaryViewState.GiftTransactionData(
                            it.result as ArrayList
                        )
                    )
                else
                    giftSummaryStateSubject.onNext(GiftWeeklySummaryViewState.EmptyState)

            } else {
                giftSummaryStateSubject.onNext(GiftWeeklySummaryViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                giftSummaryStateSubject.onNext(GiftWeeklySummaryViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
}

sealed class GiftWeeklySummaryViewState {
    data class ErrorMessage(val errorMessage: String) : GiftWeeklySummaryViewState()
    data class SuccessMessage(val successMessage: String) : GiftWeeklySummaryViewState()
    data class LoadingState(val isLoading: Boolean) : GiftWeeklySummaryViewState()
    data class GiftTransactionData(val data: ArrayList<GiftWeeklyInfo>) : GiftWeeklySummaryViewState()
    data class TotalEarningAmount(val amount: String?) : GiftWeeklySummaryViewState()

    object EmptyState : GiftWeeklySummaryViewState()
}