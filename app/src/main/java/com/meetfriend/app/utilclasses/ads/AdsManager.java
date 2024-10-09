package com.meetfriend.app.utilclasses.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.rewarded.RewardedAd;

public class AdsManager {

    public interface AdsCallback {
        void nativeResponse(NativeAd nativeAd);

        void adsOnLoaded();

    }

    public interface InterstitialAdsCallback {
        void InterstitialResponse(InterstitialAd nativeAd);

        void adsOnLoaded();
    } public interface RewardedAdsCallback {
        void InterstitialResponse(RewardedAd nativeAd);

        void adsOnLoaded();
    }

    private AdsManager() {
    }

    public NativeAd mNativeAds;
    private static AdsManager adsManager;

    public static AdsManager getAdsManager() {
        if (adsManager == null) adsManager = new AdsManager();
        return adsManager;
    }

    public void openNativeAds(Context context, String id ,AdsCallback adsCallback) {
        AdLoader adLoader = new AdLoader.Builder(context, id)

                .forNativeAd(nativeAd -> {
                    if (((Activity) context).isDestroyed()) {
                        nativeAd.destroy();
                        return;
                    }
                    if (adsCallback != null) {
                        try {
                            if (mNativeAds != null) mNativeAds.destroy();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        mNativeAds = nativeAd;
                        adsCallback.nativeResponse(nativeAd);
                        if (nativeAd != null) {
                        }
                    }

                })

                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        if (adsCallback != null) {
                            adsCallback.nativeResponse(null);
                        }
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        adsCallback.adsOnLoaded();
                    }
                }).withNativeAdOptions(new NativeAdOptions.Builder().build()).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public void openInterstitialAds(Activity context, String id ,InterstitialAdsCallback adsCallback) {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, id, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        adsCallback.InterstitialResponse(interstitialAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        adsCallback.InterstitialResponse(null);

                    }
                });
    }


}
