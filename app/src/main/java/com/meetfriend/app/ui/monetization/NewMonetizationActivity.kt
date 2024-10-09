package com.meetfriend.app.ui.monetization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.profile.model.ProfileValidationInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityNewMonetizationBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.monetization.viewmodel.MonetizationViewModel
import com.meetfriend.app.ui.monetization.viewmodel.MonetizationViewStates
import com.meetfriend.app.utils.Constant
import javax.inject.Inject

class NewMonetizationActivity : BasicActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, NewMonetizationActivity::class.java)
        }
    }

    lateinit var binding: ActivityNewMonetizationBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MonetizationViewModel>
    lateinit var monetizationViewModel: MonetizationViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewMonetizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        monetizationViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvents()

        monetizationViewModel.getProfileInfo(loggedInUserCache.getLoggedInUserId())
    }

    private fun listenToViewEvents() {
        binding.ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.tvClickHere.throttleClicks().subscribeAndObserveOnMainThread {
        }.autoDispose()

        binding.tvSendRequest.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(HubRequestFormActivity.getIntent(this))
            openInterstitialAds()
            Constant.CLICK_COUNT++
        }.autoDispose()

        RxBus.listen(RxEvent.FinishHubRequest::class.java).subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        monetizationViewModel.monetizationState.subscribeAndObserveOnMainThread {
            when (it) {
                is MonetizationViewStates.ProfileData -> {
                    handleProfileData(it.data)
                }
                is MonetizationViewStates.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun handleProfileData(data: ProfileValidationInfo) {
        updateIcon(data.haveAccount, binding.ivHaveAccount)
        updateIcon(data.havePersonalAccount, binding.ivPersonalAccount)
        updateIcon(data.validAge, binding.ivValidAge)
        updateIcon(data.validFollowers, binding.ivValidFollower)
        updateIcon(data.validViews, binding.ivValidViews)
        updateIcon(data.validContent, binding.ivValidContent)
        updateIcon(data.validViolation, binding.ivValidViolation)
    }

    private fun updateIcon(condition: Boolean?, imageView: ImageView) {
        val iconRes = if (condition == true) {
            R.drawable.ic_monetization_check
        } else {
            R.drawable.ic_monetization_cancel
        }
        imageView.setImageResource(iconRes)
    }
}
