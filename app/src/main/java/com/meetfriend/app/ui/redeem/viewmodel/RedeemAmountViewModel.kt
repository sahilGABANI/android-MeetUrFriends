package com.meetfriend.app.ui.redeem.viewmodel

import com.meetfriend.app.api.redeem.RedeemRepository
import com.meetfriend.app.api.redeem.model.RedeemRequestRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class RedeemAmountViewModel(private val redeemRepository: RedeemRepository) : BasicViewModel() {

    private val redeemAmountStateSubject: PublishSubject<RedeemAmountViewState> =
        PublishSubject.create()
    val redeemAmountState: Observable<RedeemAmountViewState> = redeemAmountStateSubject.hide()

    fun sendRedeemRequest(request: RedeemRequestRequest) {
        redeemRepository.sendRedeemRequest(request).doOnSubscribe {
            redeemAmountStateSubject.onNext(RedeemAmountViewState.LoadingState(true))
        }.doAfterTerminate {
            redeemAmountStateSubject.onNext(RedeemAmountViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                redeemAmountStateSubject.onNext(RedeemAmountViewState.RequestSuccess(it.message.toString()))
            } else {
                redeemAmountStateSubject.onNext(RedeemAmountViewState.ErrorMessage(it.message.toString()))
            }

        }, { throwable ->
            throwable.localizedMessage?.let {
                redeemAmountStateSubject.onNext(RedeemAmountViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun verifyRedeemOTP(request: RedeemRequestRequest) {
        redeemRepository.verifyRedeemOTP(request).doOnSubscribe {
            redeemAmountStateSubject.onNext(RedeemAmountViewState.LoadingState(true))
        }.doAfterTerminate {
            redeemAmountStateSubject.onNext(RedeemAmountViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it.status) {
                redeemAmountStateSubject.onNext(RedeemAmountViewState.VerifyOTPSuccess(it.message.toString()))
            } else {
                redeemAmountStateSubject.onNext(RedeemAmountViewState.ErrorMessage(it.message.toString()))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                redeemAmountStateSubject.onNext(RedeemAmountViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

}

sealed class RedeemAmountViewState {
    data class LoadingState(val isLoading: Boolean) : RedeemAmountViewState()
    data class ErrorMessage(val errorMessage: String) : RedeemAmountViewState()
    data class RequestSuccess(val successMessage: String) : RedeemAmountViewState()
    data class VerifyOTPSuccess(val successMessage: String) : RedeemAmountViewState()
}