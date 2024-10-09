package com.meetfriend.app.ui.redeem.history

import com.meetfriend.app.api.redeem.RedeemRepository
import com.meetfriend.app.api.redeem.model.RedeemHistoryInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class RedeemHistoryViewModel(private val redeemRepository: RedeemRepository) : BasicViewModel() {

    private val redeemHistoryStateSubject: PublishSubject<RedeemHistoryViewState> =
        PublishSubject.create()
    val redeemHistoryState: Observable<RedeemHistoryViewState> = redeemHistoryStateSubject.hide()

    private val perPage = 15
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfHistory: MutableList<RedeemHistoryInfo> = mutableListOf()

    private fun redeemHistory() {
        redeemRepository.redeemHistory(pageNo,perPage).doOnSubscribe {
            redeemHistoryStateSubject.onNext(RedeemHistoryViewState.LoadingState(true))
        }.doAfterTerminate {
            isLoading = false
            redeemHistoryStateSubject.onNext(RedeemHistoryViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            isLoading = false
            if (it.status) {
                redeemHistoryStateSubject.onNext(RedeemHistoryViewState.EarningAmount(it.totalEarning))
                if (pageNo == 1) {
                    if (it.result?.data != null) {
                        listOfHistory = it.result.data.toMutableList()
                        redeemHistoryStateSubject.onNext(RedeemHistoryViewState.HistoryData(listOfHistory))

                    }
                } else {
                    if (it.result?.data != null) {
                        listOfHistory.addAll(it.result.data)
                        redeemHistoryStateSubject.onNext(RedeemHistoryViewState.HistoryData(listOfHistory))

                    } else {
                        isLoadMore = false
                    }
                }
            } else {
                redeemHistoryStateSubject.onNext(RedeemHistoryViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                redeemHistoryStateSubject.onNext(RedeemHistoryViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    private fun redeemMonetizationHistory() {
        redeemRepository.redeemMonetizationHistory(perPage,pageNo).doOnSubscribe {
            redeemHistoryStateSubject.onNext(RedeemHistoryViewState.LoadingState(true))
        }.doAfterTerminate {
            isLoading = false
            redeemHistoryStateSubject.onNext(RedeemHistoryViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            isLoading = false
            if (it.status) {
                redeemHistoryStateSubject.onNext(RedeemHistoryViewState.EarningAmount(it.totalEarning))
                if (pageNo == 1) {
                    if (it.result?.data != null) {
                        listOfHistory = it.result.data.toMutableList()
                        redeemHistoryStateSubject.onNext(RedeemHistoryViewState.HistoryData(listOfHistory))

                    }
                } else {
                    if (it.result?.data != null) {
                        listOfHistory.addAll(it.result.data)
                        redeemHistoryStateSubject.onNext(RedeemHistoryViewState.HistoryData(listOfHistory))

                    } else {
                        isLoadMore = false
                    }
                }
            } else {
                redeemHistoryStateSubject.onNext(RedeemHistoryViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                redeemHistoryStateSubject.onNext(RedeemHistoryViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun loadMoreHistory() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                redeemHistory()
            }
        }
    }

    fun resetPaginationForHistory() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        redeemHistory()
    }

    fun loadMoreMHistory() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                redeemMonetizationHistory()
            }
        }
    }

    fun resetPaginationForMHistory() {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        redeemMonetizationHistory()
    }
}

sealed class RedeemHistoryViewState {
    data class LoadingState(val isLoading: Boolean) : RedeemHistoryViewState()
    data class ErrorMessage(val errorMessage: String) : RedeemHistoryViewState()
    data class HistoryData(val data: List<RedeemHistoryInfo>?) : RedeemHistoryViewState()
    data class VerifyOTPSuccess(val successMessage: String) : RedeemHistoryViewState()
    data class EarningAmount(val amount: String?) : RedeemHistoryViewState()
}