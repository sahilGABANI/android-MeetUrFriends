package com.meetfriend.app.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.follow.model.FollowUnfollowRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.AdUnifiedBinding
import com.meetfriend.app.databinding.FragmentSettingsBinding
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewState
import com.meetfriend.app.ui.fragments.LearnMoreFragment
import com.meetfriend.app.ui.fragments.PrivacyPolicyFragment
import com.meetfriend.app.ui.fragments.TermConditionFragment
import com.meetfriend.app.ui.fragments.settings.SecurityManagmentFragment
import com.meetfriend.app.utilclasses.LocalSharedPreferences
import com.meetfriend.app.utilclasses.UtilsClass
import com.meetfriend.app.utils.FileUtils
import contractorssmart.app.utilsclasses.PreferenceHandler
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class SettingActivity : AppCompatActivity() {

    lateinit var binding: FragmentSettingsBinding

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatRoomViewModel>
    private lateinit var moreViewModel: ChatRoomViewModel
    private lateinit var localSharedPreferences: LocalSharedPreferences

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var currentNativeAd: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        MeetFriendApplication.component.inject(this)
        setContentView(binding.root)
        moreViewModel = getViewModelFromFactory(viewModelFactory)
        localSharedPreferences = LocalSharedPreferences()
        listenToViewModel()
        initializeMobileAdsSdk()

        binding.apply {
            backAppCompatImageView.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            layoutScrutyMngmnt.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.settingContainer, SecurityManagmentFragment())
                    .addToBackStack("SecurityFrag")
                    .commit()
            }
            layoutPrvcyPolicy.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.settingContainer, PrivacyPolicyFragment())
                    .addToBackStack("PolicyFrag")
                    .commit()
            }
            layoutTermCondition.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.settingContainer, TermConditionFragment())
                    .addToBackStack("TermsFrag")
                    .commit()
            }
            layoutLearMore.setOnClickListener {
                supportFragmentManager.beginTransaction().replace(R.id.settingContainer, LearnMoreFragment())
                    .addToBackStack("LearnFrag")
                    .commit()
            }
            layoutDeleteAccount.setOnClickListener {
                deleteAccount()
            }
        }
    }

    private fun deleteAccount() {
        val builder =
            AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage("Are you sure, you want to delete account?")
        builder.setPositiveButton(
            "Yes"
        ) { dialog, _ ->
            moreViewModel.deleteAccount(FollowUnfollowRequest(loggedInUserCache.getLoggedInUserId()))
            dialog.dismiss()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun listenToViewModel() {
        moreViewModel.chatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatRoomViewState.DeleteUserSuccessMessage -> {
                    UtilsClass.updateUserStatus(this, false)
                    PreferenceHandler.writeString(
                        this,
                        PreferenceHandler.SHOW_SUGGESTION,
                        ""
                    )
                    clearFirebaseToken()
                    loggedInUserCache.clearUserPreferences()
                    localSharedPreferences.removeAllData()
                    val editor = FileUtils.loginUserTokenSharedPreference.edit()
                    editor.clear()
                    editor.apply()

                    val intent = Intent(this, WelcomeActivity::class.java)
                    startActivity(intent)
                    this.finish()
                }
                else -> {
                }
            }
        }
    }

    private fun clearFirebaseToken() {
        FirebaseMessaging.getInstance().apply {
            deleteToken()
        }
    }

    private fun refreshAd() {
        val builder = AdLoader.Builder(this, loggedInUserCache.getNativeAdId())

        builder.forNativeAd { nativeAd ->
            var activityDestroyed = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                activityDestroyed = isDestroyed
            }
            if (activityDestroyed || isFinishing || isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd
            val unifiedAdBinding = AdUnifiedBinding.inflate(layoutInflater)
            FileUtils.loadNativeAd(nativeAd, unifiedAdBinding)
            binding.adFrame.removeAllViews()
            binding.adFrame.addView(unifiedAdBinding.root)
        }

        val videoOptions = VideoOptions.Builder().setStartMuted(false).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        builder.withNativeAdOptions(adOptions)

        val adLoader =
            builder
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) = Unit
                    }
                )
                .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        MobileAds.initialize(this) {
            refreshAd()
        }
    }

    override fun onDestroy() {
        currentNativeAd?.destroy()
        super.onDestroy()
    }
}
