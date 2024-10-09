package com.meetfriend.app.newbase

import android.os.Bundle
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.ui.bottomsheet.auth.ForgotPassBottomSheet
import com.meetfriend.app.ui.bottomsheet.auth.LoginBottomSheet
import com.meetfriend.app.ui.bottomsheet.auth.RegisterBottomSheet
import com.meetfriend.app.utilclasses.ads.AdsManager
import com.meetfriend.app.utils.Constant
import com.mixpanel.android.mpmetrics.MixpanelAPI
import contractorssmart.app.utilsclasses.PreferenceHandler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    var disposable: Disposable? = null
    var mp: MixpanelAPI? = null
    private val compositeDisposable = CompositeDisposable()

    @Inject
    lateinit var loggedInUserCacheBase: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mp = MixpanelAPI.getInstance(requireContext(), "9baf1cfb430c0de219529759f0b22395", true)

        MeetFriendApplication.component.inject(this)

    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    fun Disposable.autoDispose() {
        compositeDisposable.add(this)
    }

    fun getUserId(): String {
        return PreferenceHandler.readString(requireActivity(), PreferenceHandler.USER_ID, "")
    }

    fun openLoginBottomSheet() {
        val loginBottomSheet = LoginBottomSheet.newInstance()
        loginBottomSheet.show(parentFragmentManager, LoginBottomSheet::class.java.name)
    }

    fun openRegisterBottomSheet() {
        val registerBottomSheet = RegisterBottomSheet.newInstance()
        registerBottomSheet.show(parentFragmentManager, RegisterBottomSheet::class.java.name)
    }

    fun openForgotPasswordBottomSheet() {
        val forgotPassBottomSheet = ForgotPassBottomSheet.newInstance()
        forgotPassBottomSheet.show(parentFragmentManager, ForgotPassBottomSheet::class.java.name)
    }

    fun openInterstitialAds() {
        if (Constant.CLICK_COUNT % loggedInUserCacheBase.getClickCountAd().toInt() == 0) {
            AdsManager.getAdsManager().openInterstitialAds(requireActivity(),
                loggedInUserCacheBase.getInterstitialAdId(),
                object : AdsManager.InterstitialAdsCallback {
                    override fun InterstitialResponse(nativeAd: InterstitialAd?) {

                        if (nativeAd != null) {
                            nativeAd.show(requireActivity())
                        }
                    }

                    override fun adsOnLoaded() {

                    }

                })
        }
    }
}