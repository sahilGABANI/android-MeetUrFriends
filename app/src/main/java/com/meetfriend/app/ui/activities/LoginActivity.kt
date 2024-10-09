package com.meetfriend.app.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.notification.model.AdType
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.challenge.ChallengeDetailsActivity
import com.meetfriend.app.ui.chatRoom.profile.ChatRoomProfileActivity
import com.meetfriend.app.ui.home.shortdetails.ShortDetailsActivity
import com.meetfriend.app.ui.livestreaming.videoroom.LiveRoomActivity
import com.meetfriend.app.ui.main.viewmodel.HomeViewModel
import com.meetfriend.app.ui.main.viewmodel.HomeViewState
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utilclasses.UtilsClass
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class LoginActivity : BasicActivity() {
    private lateinit var appUpdateManager: AppUpdateManager

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HomeViewModel>
    private lateinit var homeViewModel: HomeViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        MeetFriendApplication.component.inject(this)
        homeViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        homeViewModel.getAppConfig()
        updateApp()
        firebaseDynamicLink()
    }
    private fun updateApp() {
        appUpdateManager = AppUpdateManagerFactory.create(this@LoginActivity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                appUpdateCompleted()
            }
        }
    }

    private fun listenToViewModel() {
        homeViewModel.homeState.subscribeAndObserveOnMainThread {
            when (it) {
                is HomeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is HomeViewState.AppConfigInfo -> {
                    it.data?.forEach { configInfo ->
                        when (configInfo.type) {
                            AdType.NativeAdId.toString() -> {
                                loggedInUserCache.setNativeAdId(configInfo.value)
                            }
                            AdType.BannerAdId.toString() -> {
                                loggedInUserCache.setBannerAdId(configInfo.value)
                            }
                            AdType.InterstitialAdId.toString() -> {
                                loggedInUserCache.setInterstitialAdId(configInfo.value)
                            }
                            AdType.AppOpenAdId.toString() -> {
                                loggedInUserCache.setAppOpenAdId(configInfo.value)
                            }
                            AdType.InterstitialAdScrollCount.toString() -> {
                                loggedInUserCache.setScrollCountAd(configInfo.value)
                            }
                            AdType.InterstitialAdClickCount.toString() -> {
                                loggedInUserCache.setClickCountAd(configInfo.value)
                            }
                        }
                    }
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun appUpdateCompleted() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Androidly Alert")
        builder.setMessage("We have a message")
        builder.setPositiveButton("OK") { _, _ ->
            appUpdateManager.completeUpdate()
        }

        builder.create()
        try {
            builder.show()
        } catch (e: java.lang.Exception) {
            Timber.e(e)
        }
    }

    private fun firebaseDynamicLink() {
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { linkData ->
                linkData?.link?.let { handleDynamicLink(it) }
            }
            .addOnFailureListener {
                Timber.e(it)
            }
    }
    private fun handleDynamicLink(link: Uri) {
        val linkString = link.toString()
        when {
            linkString.contains("challenge") -> handleChallengeLink(link)
            linkString.contains("images_share") -> handleImageShareLink(linkString)
            linkString.contains("post") -> handlePostLink(link)
            linkString.contains("user") -> handleUserLink(link)
            linkString.contains("live") -> handleLiveLink(link)
            linkString.contains("reels") -> handleReelsLink(link)
            linkString.contains("chat_profile") -> handleChatProfileLink(link)
        }
    }

    private fun handleChallengeLink(link: Uri) {
        val challengeId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        if (challengeId != null) {
            startActivity(ChallengeDetailsActivity.getIntent(this, challengeId))
        }
    }

    private fun handleImageShareLink(linkString: String) {
        val url: String = linkString.toString()
        val imageUrl = url.substringAfter("images_share/")
        if (imageUrl != "0") {
            val intent = Intent(this, FullScreenActivity::class.java)
            intent.putExtra(
                "url",
                imageUrl
            )
            this.startActivity(intent)
        }
    }

    private fun handlePostLink(link: Uri) {
        val postId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        if (postId != null) {
            startActivity(ShortDetailsActivity.getIntent(this, postId))
        }
    }

    private fun handleUserLink(link: Uri) {
        val userId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        if (userId != null) {
            startActivity(MyProfileActivity.getIntentWithData(this, userId))
        }
    }

    private fun handleLiveLink(link: Uri) {
        val liveId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        if (liveId != null) {
            startActivity(LiveRoomActivity.getIntent(this, null, liveId))
        }
    }

    private fun handleReelsLink(link: Uri) {
        val postId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        if (postId != null) {
            startActivity(ShortDetailsActivity.getIntent(this, postId))
        }
    }

    private fun handleChatProfileLink(link: Uri) {
        val userId: Int? = link.pathSegments.lastOrNull()?.toIntOrNull()
        if (userId != null) {
            startActivity(ChatRoomProfileActivity.getIntent(this, userId))
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        try {
            UtilsClass.updateUserStatus(this, true)
        } catch (e: IOException) {
            Timber.e(e, "An error occurred $e")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            UtilsClass.updateUserStatus(this, false)
        } catch (e: IOException) {
            Timber.e(e, "An error occurred $e")
        }
    }
}
