package com.meetfriend.app.newbase.extension

import android.view.View
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

fun View.throttleClicks(): Observable<Unit> {
    return clicks().throttleFirst(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
}

fun <T> Observable<T>.subscribeAndObserveOnMainThread(onNext: (t: T) -> Unit): Disposable {
    return observeOn(AndroidSchedulers.mainThread())
        .subscribe(onNext)
}

fun <T> Observable<T>.subscribeOnIoAndObserveOnMainThread(
    onNext: (t: T) -> Unit,
    onError: (Throwable) -> Unit
): Disposable {
    return subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onNext, onError)
}

fun <T> Single<T>.subscribeOnIoAndObserveOnMainThread(
    onNext: (t: T) -> Unit,
    onError: (Throwable) -> Unit
): Disposable {
    return subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onNext, onError)
}

fun Completable.subscribeOnIoAndObserveOnMainThread(
    onComplete: () -> Unit,
    onError: (Throwable) -> Unit
): Disposable {
    return subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onComplete, onError)
}


fun <T> Observable<T>.throttleClicks(): Observable<T> {
    return this.throttleFirst(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
}

fun <T> SingleEmitter<T>.onSafeSuccess(t: T) {
    if (!isDisposed) onSuccess(t!!)
}

fun <T> SingleEmitter<T>.onSafeError(throwable: Throwable) {
    if (!isDisposed) onError(throwable)
}

fun <T> ObservableEmitter<T>.onSafeNext(t: T) {
    if (!isDisposed) onNext(t!!)
}