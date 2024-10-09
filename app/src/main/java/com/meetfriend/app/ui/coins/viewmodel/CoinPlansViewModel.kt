package com.meetfriend.app.ui.coins.viewmodel

import com.meetfriend.app.api.gift.GiftsRepository
import com.meetfriend.app.api.gift.model.CoinPlanInfo
import com.meetfriend.app.api.gift.model.MyEarningInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CoinPlansViewModel(private val giftsRepository: GiftsRepository) : BasicViewModel() {

    private val coinPlansStateSubject: PublishSubject<CoinPlansViewState> =
        PublishSubject.create()
    val coinPlansState: Observable<CoinPlansViewState> = coinPlansStateSubject.hide()

    fun getCoinPlans() {
        giftsRepository.getCoinPlans().doOnSubscribe {
            coinPlansStateSubject.onNext(CoinPlansViewState.LoadingState(true))
        }.doAfterTerminate {
            coinPlansStateSubject.onNext(CoinPlansViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                if (!it.result.isNullOrEmpty())
                    coinPlansStateSubject.onNext(CoinPlansViewState.CoinPlanData(it.result))
                else
                    coinPlansStateSubject.onNext(CoinPlansViewState.EmptyState)

            } else {
                coinPlansStateSubject.onNext(CoinPlansViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                coinPlansStateSubject.onNext(CoinPlansViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun getMyEarningInfo() {
        giftsRepository.getMyEarning().doOnSubscribe {
            coinPlansStateSubject.onNext(CoinPlansViewState.LoadingState(true))
        }.doAfterTerminate {
            coinPlansStateSubject.onNext(CoinPlansViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                coinPlansStateSubject.onNext(CoinPlansViewState.MyEarningData(it.result))
            } else {
                coinPlansStateSubject.onNext(CoinPlansViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                coinPlansStateSubject.onNext(CoinPlansViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun coinPurchase(coins: String, transactionId: String, planName: String, price: String) {
        giftsRepository.coinPurchase(coins, transactionId, planName, price).doOnSubscribe {
            coinPlansStateSubject.onNext(CoinPlansViewState.LoadingState(true))
        }.doAfterTerminate {
            coinPlansStateSubject.onNext(CoinPlansViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                coinPlansStateSubject.onNext(CoinPlansViewState.PurchaseSuccessMessage(it.message.toString()))
            } else {
                coinPlansStateSubject.onNext(CoinPlansViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                coinPlansStateSubject.onNext(CoinPlansViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }


}

sealed class CoinPlansViewState {
    data class ErrorMessage(val errorMessage: String) : CoinPlansViewState()
    data class PurchaseSuccessMessage(val successMessage: String) : CoinPlansViewState()
    data class LoadingState(val isLoading: Boolean) : CoinPlansViewState()
    data class MyEarningData(val myEarningInfo: MyEarningInfo?) : CoinPlansViewState()
    data class CoinPlanData(val coinPlanData: List<CoinPlanInfo>) : CoinPlansViewState()
    object EmptyState : CoinPlansViewState()

}