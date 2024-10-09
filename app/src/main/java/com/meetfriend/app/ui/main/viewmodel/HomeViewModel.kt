package com.meetfriend.app.ui.main.viewmodel

import android.content.Context
import androidx.lifecycle.Observer
import com.meetfriend.app.api.authentication.AuthenticationRepository
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.SwitchDeviceAccountRequest
import com.meetfriend.app.api.cloudflare.CloudFlareRepository
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.monetization.MonetizationRepository
import com.meetfriend.app.api.monetization.model.ExchangeRateResponse
import com.meetfriend.app.api.notification.NotificationRepository
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.newbase.BasicViewModel
import com.meetfriend.app.newbase.extension.cloudFlareGetVideoUploadBaseUrl
import com.meetfriend.app.newbase.extension.cloudFlareVideoUploadBaseUrl
import com.meetfriend.app.newbase.extension.getCommonVideoFileName
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.utils.Constant.FIX_5000
import com.skydoves.viewmodel.lifecycle.viewModelLifecycleOwner
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.concurrent.TimeUnit

class HomeViewModel(
    private val notificationRepository: NotificationRepository,
    private val monetizationRepository: MonetizationRepository,
    private val cloudFlareRepository: CloudFlareRepository,
    private val loginUserCache: LoggedInUserCache,
    private val postRepository: PostRepository,
    private val authenticationRepository: AuthenticationRepository
) : BasicViewModel() {

    private val homeStateSubject: PublishSubject<HomeViewState> = PublishSubject.create()
    val homeState: Observable<HomeViewState> = homeStateSubject.hide()

    private var cloudFlareConfig: CloudFlareConfig? = null
    private var videoUid: String = ""
    private var thumbnail: String = ""
    fun getNotificationCount() {
        notificationRepository.getNotificationCount().subscribeOnIoAndObserveOnMainThread({
            homeStateSubject.onNext(HomeViewState.GetNotificationCount(it))
        }, { throwable ->
            throwable.localizedMessage?.let {
                homeStateSubject.onNext(HomeViewState.ErrorMessage(it))
            }
        })
    }

    fun getExchangeRate() {
        monetizationRepository.getExchangeRate()
            .subscribeOnIoAndObserveOnMainThread({
                homeStateSubject.onNext(
                    HomeViewState.ExchangeRate(it)
                )
            }, {
            }).autoDispose()
    }

    fun getAppConfig() {
        notificationRepository.getAppConfig().subscribeOnIoAndObserveOnMainThread({
            homeStateSubject.onNext(HomeViewState.AppConfigInfo(it.result))
        }, { throwable ->
            throwable.localizedMessage?.let {
                homeStateSubject.onNext(HomeViewState.ErrorMessage(it))
            }
        })
    }

    fun addStoryVideo(addStoryRequest: AddStoryRequest) {
        postRepository.addStory(addStoryRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.status) {
                    homeStateSubject.onNext(HomeViewState.AddStoryResponse(response.message.toString()))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    homeStateSubject.onNext(HomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun uploadVideoToCloudFlare(
        context: Context,
        cloudFlareConfig: CloudFlareConfig,
        videoFile: File
    ) {
        val videoTempPathDir = context.getExternalFilesDir("MeetFriendsVideos")?.path
        val fileName =
            getCommonVideoFileName(loginUserCache.getLoggedInUserId())
        val videoCopyFile = File(videoTempPathDir + File.separator + fileName + ".mp4")
        val finalImageFile = videoFile.copyTo(videoCopyFile)
        this.cloudFlareConfig = cloudFlareConfig
        val apiUrl = cloudFlareVideoUploadBaseUrl.format(cloudFlareConfig.accountId)
        val authToken = "Bearer ".plus(cloudFlareConfig.apiToken)
        cloudFlareRepository.uploadVideoUsingTus(apiUrl, authToken, finalImageFile)
            .doOnSubscribe {
                homeStateSubject.onNext(HomeViewState.StoryLoadingState(true))
            }
            .doAfterTerminate {
                homeStateSubject.onNext(HomeViewState.StoryLoadingState(true))
            }
            .subscribeOn(Schedulers.io())
            .flatMap {
                cloudFlareRepository.getUploadVideoDetails(it, authToken)
            }
            .observeOn(AndroidSchedulers.mainThread()) // Observe on main thread if needed
            .subscribe({
                videoUid = it.uid.toString()
                thumbnail = it.thumbnail.toString()
                getVideoStatusCheckAPI()
            }, { throwable ->
                homeStateSubject.onNext(HomeViewState.StoryLoadingState(false))
                throwable.localizedMessage?.let {
                    homeStateSubject.onNext(HomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private fun getVideoStatusCheckAPI() {
        if (cloudFlareConfig != null) {
            val apiUrl = cloudFlareGetVideoUploadBaseUrl.format(cloudFlareConfig?.accountId, videoUid)
            val authToken = "Bearer ".plus(cloudFlareConfig?.apiToken)
            cloudFlareRepository.getUploadVideoStatus(apiUrl, authToken).observeOn(AndroidSchedulers.mainThread())
                .subscribeOnIoAndObserveOnMainThread({
                    if (it.result?.status?.state != "ready" && it.result?.status?.state != "error") {
                        Observable.timer(FIX_5000.toLong(), TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                            getVideoStatusCheckAPI()
                        }
                    } else if (it.result.status.state == "error") {
                        it.result.status.errorReasonText?.let {
                            homeStateSubject.onNext(HomeViewState.CloudFlareErrorMessage(it))
                        }
                        homeStateSubject.onNext(HomeViewState.StoryLoadingState(false))
                    } else {
                        homeStateSubject.onNext(HomeViewState.StoryLoadingState(false))
                        homeStateSubject.onNext(
                            HomeViewState.UploadVideoCloudFlareSuccess(
                                videoUid,
                                thumbnail
                            )
                        )
                    }
                }, {
                    homeStateSubject.onNext(HomeViewState.StoryLoadingState(false))
                })
        }
    }

    fun switchAccount(switchDeviceAccountRequest: SwitchDeviceAccountRequest) {
        authenticationRepository.switchAccount(switchDeviceAccountRequest)
            .doOnSubscribe {
                homeStateSubject.onNext(HomeViewState.LoadingState(true))
            }
            .doAfterTerminate {
                homeStateSubject.onNext(HomeViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                homeStateSubject.onNext(HomeViewState.LoadingState(false))
                if (response.status) {
                    response.message?.let {
                        homeStateSubject.onNext(HomeViewState.SuccessMessage(it))
                    }
                    homeStateSubject.onNext(HomeViewState.SwitchAccount)
                } else {
                    response.message?.let {
                        homeStateSubject.onNext(HomeViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    homeStateSubject.onNext(HomeViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getLiveDataInfo() {
        cloudFlareRepository.getLiveData().observe(
            viewModelLifecycleOwner,
            Observer {
                homeStateSubject.onNext(HomeViewState.ProgressDisplay(it))
            }
        )
    }
}

sealed class HomeViewState {
    data class ErrorMessage(val errorMessage: String) : HomeViewState()
    data class CloudFlareErrorMessage(val errorMessage: String) : HomeViewState()
    data class SuccessMessage(val successMessage: String) : HomeViewState()
    data class LoadingState(val isLoading: Boolean) : HomeViewState()
    data class StoryLoadingState(val isLoading: Boolean) : HomeViewState()
    data class GetNotificationCount(val response: MeetFriendCommonResponse) : HomeViewState()
    data class ExchangeRate(val data: ExchangeRateResponse?) : HomeViewState()
    data class AppConfigInfo(val data: List<com.meetfriend.app.api.notification.model.AppConfigInfo>?) :
        HomeViewState()

    data class AddStoryResponse(val message: String) : HomeViewState()
    data class UploadVideoCloudFlareSuccess(val videoId: String, val thumbnail: String) :
        HomeViewState()

    data class ProgressDisplay(val progressInfo: Double) : HomeViewState()
    object SwitchAccount : HomeViewState()
}
