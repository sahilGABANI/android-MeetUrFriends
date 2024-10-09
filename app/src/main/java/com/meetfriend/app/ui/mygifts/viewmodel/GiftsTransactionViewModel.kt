package com.meetfriend.app.ui.mygifts.viewmodel

import com.meetfriend.app.api.gift.GiftsRepository
import com.meetfriend.app.api.gift.model.GiftTransaction
import com.meetfriend.app.api.gift.model.MyEarningInfo
import com.meetfriend.app.api.livestreaming.LiveRepository
import com.meetfriend.app.api.livestreaming.model.CoinCentsInfo
import com.meetfriend.app.api.monetization.model.EarningListRequest
import com.meetfriend.app.api.monetization.model.SendCoinRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GiftsTransactionViewModel(private val giftsRepository: GiftsRepository,
private val liveRepository:LiveRepository) : BasicViewModel() {

    private val giftTransactionStateSubject: PublishSubject<GiftTransactionViewState> =
        PublishSubject.create()
    val giftTransactionState: Observable<GiftTransactionViewState> =
        giftTransactionStateSubject.hide()

    fun getSendGiftTransaction(request: EarningListRequest) {
        giftsRepository.getSendGiftTransaction(request).doOnSubscribe {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(true))
        }.doAfterTerminate {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftTransactionStateSubject.onNext(
                    GiftTransactionViewState.CoinsInfo(
                        it.totalSent ?: "0.0"
                    )
                )
                if (!it.result.isNullOrEmpty())
                    giftTransactionStateSubject.onNext(
                        GiftTransactionViewState.GiftTransactionData(
                            it.result as ArrayList
                        )
                    )
                else
                    giftTransactionStateSubject.onNext(GiftTransactionViewState.EmptyState)

            } else {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun getReceivedGiftTransaction(request: EarningListRequest) {
        giftsRepository.getReceivedGiftTransaction(request).doOnSubscribe {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(true))
        }.doAfterTerminate {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftTransactionStateSubject.onNext(
                    GiftTransactionViewState.CoinsInfo(
                        it.totalRecieved ?: "0.0"
                    )
                )
                if (!it.result.isNullOrEmpty())
                    giftTransactionStateSubject.onNext(
                        GiftTransactionViewState.GiftTransactionData(
                            it.result as ArrayList
                        )
                    )
                else
                    giftTransactionStateSubject.onNext(GiftTransactionViewState.EmptyState)

            } else {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun sendCoins(request: SendCoinRequest) {
        giftsRepository.SendCoins(request).doOnSubscribe {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(true))
        }.doAfterTerminate {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftTransactionStateSubject.onNext(
                    GiftTransactionViewState.SuccessMessage(
                        it.message ?:""
                    )
                )
            } else {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }


    fun getMyEarningInfo() {
        giftsRepository.getMyEarning().doOnSubscribe {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(true))
        }.doAfterTerminate {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.MyEarningData(it.result))
            } else {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun getCoinInCents() {
        liveRepository.getCoinInCents().doOnSubscribe {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(true))
        }.doAfterTerminate {
            giftTransactionStateSubject.onNext(GiftTransactionViewState.LoadingState(false))

        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.CoinCentsData(it.result))
            } else {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                giftTransactionStateSubject.onNext(GiftTransactionViewState.ErrorMessage(it))
            }
        })
    }
}

sealed class GiftTransactionViewState {
    data class ErrorMessage(val errorMessage: String) : GiftTransactionViewState()
    data class SuccessMessage(val successMessage: String) : GiftTransactionViewState()
    data class LoadingState(val isLoading: Boolean) : GiftTransactionViewState()
    data class MyEarningData(val myEarningInfo: MyEarningInfo?) : GiftTransactionViewState()
    data class GiftTransactionData(val data: ArrayList<GiftTransaction>) :
        GiftTransactionViewState()

    data class CoinsInfo(val coin : String) : GiftTransactionViewState()

    object EmptyState : GiftTransactionViewState()
    data class CoinCentsData(val coinCentsInfo: CoinCentsInfo?) : GiftTransactionViewState()


}