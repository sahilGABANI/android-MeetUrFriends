package com.meetfriend.app.ui.monetization.earnings.viewmodel

import com.meetfriend.app.api.gift.GiftsRepository
import com.meetfriend.app.api.monetization.MonetizationRepository
import com.meetfriend.app.api.monetization.model.EarningAmountInfo
import com.meetfriend.app.api.monetization.model.EarningListRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EarningsViewModel(
    private val monetizationRepository: MonetizationRepository,
    private val giftsRepository: GiftsRepository
) : BasicViewModel() {

    private val earningsStateSubject: PublishSubject<EarningsViewStates> = PublishSubject.create()
    val earningsState: Observable<EarningsViewStates> = earningsStateSubject.hide()

    fun getEarningList(request: EarningListRequest) {

        monetizationRepository.getEarningList(request)
            .doOnSubscribe {
                earningsStateSubject.onNext(EarningsViewStates.LoadingState(true))
            }
            .doAfterTerminate {
                earningsStateSubject.onNext(EarningsViewStates.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (response.status) {
                        response.result?.let {
                            earningsStateSubject.onNext(
                                EarningsViewStates.EarningData(
                                    response.result
                                )
                            )
                        }
                    }
                }

            }, { throwable ->
                earningsStateSubject.onNext(EarningsViewStates.LoadingState(false))
                throwable.localizedMessage?.let {
                    earningsStateSubject.onNext(EarningsViewStates.ErrorMessage(it))
                }
            }).autoDispose()
    }

}

sealed class EarningsViewStates {
    data class ErrorMessage(val errorMessage: String) : EarningsViewStates()
    data class SuccessMessage(val successMessage: String) : EarningsViewStates()
    data class LoadingState(val isLoading: Boolean) : EarningsViewStates()
    data class EarningData(val data: EarningAmountInfo) : EarningsViewStates()
    data class ClaimedCoin(val successMessage: String) : EarningsViewStates()

}