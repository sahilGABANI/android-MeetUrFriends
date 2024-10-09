package com.meetfriend.app.ui.follow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.tabs.TabLayoutMediator
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityProfileFollowingBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.follow.view.ProfileFollowingAdapter
import com.meetfriend.app.utils.FileUtils
import javax.inject.Inject

class ProfileFollowingActivity : BasicActivity() {

    companion object {
        const val INTENT_USER_ID = "INTENT_USER_ID"
        private const val INTENT_FOLLOWING = "INTENT_FOLLOWING"
        private const val INTENT_USER_NAME = "INTENT_USER_NAME"

        fun getIntent(
            context: Context,
            userId: Int,
            isFollowing: Boolean,
            userName: String
        ): Intent {
            val intent = Intent(context, ProfileFollowingActivity::class.java)
            intent.putExtra(INTENT_USER_ID, userId)
            intent.putExtra(INTENT_FOLLOWING, isFollowing)
            intent.putExtra(INTENT_USER_NAME, userName)
            return intent
        }
    }

    private lateinit var binding: ActivityProfileFollowingBinding
    private lateinit var profileFollowingAdapter: ProfileFollowingAdapter
    private var userId: Int = -1
    private var isFollowing: Boolean = false
    private var userName: String = ""

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileFollowingBinding.inflate(layoutInflater)
        MeetFriendApplication.component.inject(this@ProfileFollowingActivity)
        setContentView(binding.root)

        FileUtils.loadBannerAd(this,binding.adView,loggedInUserCache.getBannerAdId())
        loadDataFromIntent()
        listenToViewEvent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            userId = it.getIntExtra(INTENT_USER_ID, -1)
            if (userId == -1) {
              onBackPressedDispatcher.onBackPressed()
            }
            isFollowing = it.getBooleanExtra(INTENT_FOLLOWING, true)
            userName = it.getStringExtra(INTENT_USER_NAME).toString()
            binding.tvUserName.text = userName
        }
    }

    private fun listenToViewEvent() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        profileFollowingAdapter = ProfileFollowingAdapter(this, userId)
        binding.viewPagerFollowing.isUserInputEnabled = true
        binding.viewPagerFollowing.offscreenPageLimit = 1
        binding.viewPagerFollowing.adapter = profileFollowingAdapter

        if (!isFollowing) {
            binding.viewPagerFollowing.currentItem = 1
        }

        TabLayoutMediator(binding.tabLayoutFollowing, binding.viewPagerFollowing) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.following)
                }
                1 -> {
                    tab.text = getString(R.string.followers)
                }
                2 -> {
                    tab.text = getString(R.string.suggested)
                }
            }
        }.attach()
    }
}