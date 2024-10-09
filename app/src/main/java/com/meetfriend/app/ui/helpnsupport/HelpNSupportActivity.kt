package com.meetfriend.app.ui.helpnsupport

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.meetfriend.app.databinding.ActivityHelpNsupportBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks

class HelpNSupportActivity : BasicActivity() {

    private lateinit var binding: ActivityHelpNsupportBinding

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, HelpNSupportActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHelpNsupportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MobileAds.initialize(this) {}

        val mAdView = AdView(this@HelpNSupportActivity)
        mAdView.setAdSize(AdSize.BANNER)
        mAdView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
        binding.adView.addView(mAdView)
        val adRequest: AdRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        initUI()
    }

    private fun initUI() {
        binding.moniimage.throttleClicks().subscribeAndObserveOnMainThread {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("mailto:")
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("contact@meeturfriends.com"))
            startActivity(intent)
        }.autoDispose()

        binding.helpForm.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(HelpFormActivity.getIntent(this@HelpNSupportActivity))
        }.autoDispose()

        binding.ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
    }
}
