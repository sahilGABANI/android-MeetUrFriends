package com.meetfriend.app.newbase

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import timber.log.Timber
import javax.inject.Inject

class ActivityManager {
    private lateinit var appOpenAdManager: AppOpenAdManager

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private  var  adsShowOnce :Boolean = false


    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ActivityManager? = null

        fun getInstance(): ActivityManager =
            instance ?: synchronized(this) {
                instance ?: ActivityManager().also { instance = it }
            }


    }

    private var lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                adsShowOnce = false
            }

            Lifecycle.Event.ON_START -> {
                if (foregroundActivity?.localClassName == "ui.deepar.DeeparEffetcsActivity") {
                    adsShowOnce = true
                } else {
                    foregroundActivity?.let { appOpenAdManager.showAdIfAvailable(it) }
                }
            }

            Lifecycle.Event.ON_DESTROY -> {
                adsShowOnce = false
            }

            else -> {}
        }
    }


    var application: Application? = null
        private set

    var foregroundActivity: Activity? = null
        private set

    fun init(application: Application) {
        this.application = application
        MeetFriendApplication.component.inject(this)
        appOpenAdManager = AppOpenAdManager()
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
        this.application?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                foregroundActivity = activity
                foregroundActivity?.let {
                    if (activity.localClassName == "ui.activities.WelcomeActivity") {
                        appOpenAdManager.showAd(it)
                    } else if (activity.localClassName == "ui.main.MainHomeActivity" && !adsShowOnce) {
                        appOpenAdManager.showAdIfAvailable(it)
                    } else {

                    }
                }

            }

            override fun onActivityStarted(activity: Activity) {
                foregroundActivity = activity
            }

            override fun onActivityResumed(activity: Activity) {
                foregroundActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityDestroyed(activity: Activity) {
                if (foregroundActivity != null && foregroundActivity == activity) {
                    foregroundActivity = null
                    Timber.e("onActivityDestroyed")
                }
            }
        })
    }


    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        /** Request an ad. */
        fun loadAd(context: Context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context, loggedInUserCache.getAppOpenAdId(), request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {

                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        appOpenAd = ad
                        isLoadingAd = false
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                        isLoadingAd = false;
                    }
                })
        }

        /** Check if ad exists and can be shown. */
        private fun isAdAvailable(): Boolean {
            return appOpenAd != null
        }

        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener? = null) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                return
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                onShowAdCompleteListener?.onShowAdComplete()
                loadAd(activity)
                return
            }

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    // Called when full screen content is dismissed.
                    // Set the reference to null so isAdAvailable() returns false.
                    appOpenAd = null
                    isShowingAd = false
                    adsShowOnce  = true

                    onShowAdCompleteListener?.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when fullscreen content failed to show.
                    // Set the reference to null so isAdAvailable() returns false.
                    appOpenAd = null
                    isShowingAd = false

                    onShowAdCompleteListener?.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                }
            }
            isShowingAd = true
            appOpenAd?.show(activity)
        }

        fun showAd(activity: Activity) {
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                activity, loggedInUserCache.getAppOpenAdId(), request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {

                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        ad.show(activity)
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

                            override fun onAdDismissedFullScreenContent() {
                                // Called when full screen content is dismissed.
                                // Set the reference to null so isAdAvailable() returns false.
                                appOpenAd = null
                                isShowingAd = false
                                adsShowOnce  = true
                                loadAd(activity)
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                // Called when fullscreen content failed to show.
                                // Set the reference to null so isAdAvailable() returns false.
                                appOpenAd = null
                                isShowingAd = false
                                loadAd(activity)
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                            }
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                    }
                })
        }
    }

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

}
