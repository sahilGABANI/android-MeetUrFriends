package com.meetfriend.app.newbase

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utilclasses.ads.AdsManager
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.UiUtils
import com.mixpanel.android.mpmetrics.MixpanelAPI
import contractorssmart.app.utilsclasses.PreferenceHandler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import javax.inject.Inject

abstract class BasicFragment : Fragment() {

    private val compositeDisposable = CompositeDisposable()
    var mp: MixpanelAPI? = null
    lateinit var baseActivity: Activity

    @Inject
    lateinit var loggedInUserCacheBasic: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mp = MixpanelAPI.getInstance(requireContext(), "9baf1cfb430c0de219529759f0b22395", true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        context.let {
            baseActivity = (context as Activity)
        }
    }

    fun Disposable.autoDispose() {
        compositeDisposable.add(this)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    @CallSuper
    open fun onBackPressed(): Boolean {
        Timber.i("[%s] onBackPressed", javaClass.simpleName)
        if (isAdded) {
            val view = baseActivity.currentFocus
            if (view != null && view is EditText) {
                UiUtils.hideKeyboard(baseActivity.window)
                return false
            }
        }
        return false
    }

    open fun showLoading(show: Boolean?) {
        if (show!!) showLoading() else hideLoading()
    }

    open fun showLoading() {
        CallProgressWheel.showLoadingDialog(requireActivity())
    }

    open fun hideLoading() {
        CallProgressWheel.dismissLoadingDialog()
    }

    fun getUserId(): String {
        return PreferenceHandler.readString(
            requireActivity(),
            PreferenceHandler.USER_ID,
            ""
        )
    }

    fun openInterstitialAds() {
        if (Constant.CLICK_COUNT % loggedInUserCacheBasic.getClickCountAd().toInt() == 0) {
            AdsManager.getAdsManager().openInterstitialAds(
                requireActivity(),
                loggedInUserCacheBasic.getInterstitialAdId(),
                object : AdsManager.InterstitialAdsCallback {
                    override fun InterstitialResponse(nativeAd: InterstitialAd?) {
                        nativeAd?.show(requireActivity())
                    }

                    override fun adsOnLoaded() {
                        return
                    }
                }
            )
        }
    }
}
