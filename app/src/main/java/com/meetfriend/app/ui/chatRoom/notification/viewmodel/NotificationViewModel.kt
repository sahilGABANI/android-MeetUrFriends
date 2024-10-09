package com.meetfriend.app.ui.chatRoom.notification.viewmodel

import com.meetfriend.app.api.notification.NotificationRepository
import com.meetfriend.app.api.notification.model.AcceptRejectRequestRequest
import com.meetfriend.app.api.notification.model.NotificationItemInfo
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NotificationViewModel(private val notificationRepository: NotificationRepository) :
    BasicViewModel() {

    private val notificationStateSubject: PublishSubject<NotificationViewState> =
        PublishSubject.create()
    val notificationState: Observable<NotificationViewState> = notificationStateSubject.hide()

    companion object {
        const val PER_PAGE = 20
    }
    private var page = 1
    private var isLoading = false
    private var isLoadMore = true

    private var listOfNotification: MutableList<NotificationItemInfo> = mutableListOf()

    private fun getNotification() {
        notificationRepository.getNotification(PER_PAGE, page)
            .doOnSubscribe {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(true))
            }
            .doOnTerminate {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(false))
                isLoading = false
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                isLoading = false
                if (response.status) {
                    if (page == 1) {
                        if (response.result?.data != null) {
                            listOfNotification = response.result.data.toMutableList()
                            notificationStateSubject.onNext(
                                NotificationViewState.NotificationData(
                                    listOfNotification
                                )
                            )
                        }
                    } else {
                        if (response.result?.data != null) {
                            listOfNotification.addAll(response.result.data)
                            notificationStateSubject.onNext(
                                NotificationViewState.NotificationData(
                                    listOfNotification
                                )
                            )
                        } else {
                            isLoadMore = false
                        }
                    }
                } else {
                    notificationStateSubject.onNext(NotificationViewState.ErrorMessage(response.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    notificationStateSubject.onNext(NotificationViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun notificationMarkAllRead() {
        notificationRepository.notificationMarkAllRead()
            .doOnSubscribe {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(true))
            }
            .doAfterTerminate {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                notificationStateSubject.onNext(NotificationViewState.LoadingState(false))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    notificationStateSubject.onNext(NotificationViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun acceptRejectRequest(acceptRejectRequestRequest: AcceptRejectRequestRequest) {
        notificationRepository.acceptRejectRequest(acceptRejectRequestRequest)
            .doOnSubscribe {
                notificationStateSubject.onNext(NotificationViewState.RequestLoadingState(true))
            }
            .doAfterTerminate {
                notificationStateSubject.onNext(NotificationViewState.RequestLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                notificationStateSubject.onNext(NotificationViewState.RequestLoadingState(false))
                if (response.status) {
                    notificationStateSubject.onNext(NotificationViewState.SuccessMessage(response.message.toString()))
                } else {
                    notificationStateSubject.onNext(NotificationViewState.ErrorMessage(response.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    notificationStateSubject.onNext(NotificationViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun loadMoreNotification() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                page += 1
                getNotification()
            }
        }
    }

    fun resetPaginationForNotification() {
        page = 1
        isLoading = false
        isLoadMore = true
        getNotification()
    }

    fun deleteAllNotification() {
        notificationRepository.deleteAllNotification()
            .doOnSubscribe {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(true))
            }
            .doAfterTerminate {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                notificationStateSubject.onNext(NotificationViewState.LoadingState(false))
                notificationStateSubject.onNext(NotificationViewState.DeleteAllData(response.status))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    notificationStateSubject.onNext(NotificationViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteNotification(id: String) {
        notificationRepository.deleteNotification(id)
            .doOnSubscribe {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(true))
            }
            .doAfterTerminate {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                notificationStateSubject.onNext(NotificationViewState.LoadingState(false))
                notificationStateSubject.onNext(NotificationViewState.DeleteData(response.status))
            }, { throwable ->
                throwable.localizedMessage?.let {
                    notificationStateSubject.onNext(NotificationViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class NotificationViewState {
    data class ErrorMessage(val errorMessage: String) : NotificationViewState()
    data class SuccessMessage(val successMessage: String) : NotificationViewState()
    data class LoadingState(val isLoading: Boolean) : NotificationViewState()
    data class RequestLoadingState(val isLoading: Boolean) : NotificationViewState()
    data class NotificationData(val notificationList: List<NotificationItemInfo>) :
        NotificationViewState()

    data class DeleteAllData(val deleteData: Boolean) : NotificationViewState()
    data class DeleteData(val deleteData: Boolean) : NotificationViewState()
}
