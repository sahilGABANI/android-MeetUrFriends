package com.meetfriend.app.ui.monetization

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.view.isVisible
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.monetization.model.SendHubRequestRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivitySelectTaskBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.monetization.viewmodel.HubRequestViewModel
import com.meetfriend.app.ui.monetization.viewmodel.HubRequestViewStates
import java.time.ZonedDateTime
import javax.inject.Inject

class SelectTaskActivity : BasicActivity() {

    companion object {
        private const val INTENT_REQUEST_INFO = "INTENT_REQUEST_INFO"

        fun getIntent(context: Context, request: SendHubRequestRequest): Intent {
            val intent = Intent(context, SelectTaskActivity::class.java)
            intent.putExtra(INTENT_REQUEST_INFO, request)
            return intent
        }
    }

    private lateinit var binding: ActivitySelectTaskBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HubRequestViewModel>
    lateinit var hubRequestViewModel: HubRequestViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var requestData: SendHubRequestRequest? = null
    var hashMapOfStatus: HashMap<String, Int> = HashMap<String, Int>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        hubRequestViewModel = getViewModelFromFactory(viewModelFactory)

        intent?.let {
            requestData = it.getParcelableExtra(INTENT_REQUEST_INFO)
        }

        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        val timeZone = if (Build.VERSION.SDK_INT >= 26) {
            ZonedDateTime.now().zone.toString()
        } else {
            java.util.TimeZone.getDefault().id
        }
        hashMapOfStatus["storyStatus"] = 0
        hashMapOfStatus["postStatus"] = 0
        hashMapOfStatus["shortStatus"] = 0
        hashMapOfStatus["challengeStatus"] = 0
        hashMapOfStatus["shareStatus"] = 0
        hashMapOfStatus["liveStatus"] = 0

        binding.tvSendRequest.throttleClicks().subscribeAndObserveOnMainThread {

            val filteredMap = hashMapOfStatus.filter { (key, value) -> value == 1 }
            if (filteredMap.size >= 3) {
                hubRequestViewModel.createHubRequest(
                    SendHubRequestRequest(
                        requestData?.firstName,
                        requestData?.lastName,
                        requestData?.dateOfBirth,
                        requestData?.address,
                        requestData?.email,
                        requestData?.phoneNo,
                        requestData?.social,
                        hashMapOfStatus["storyStatus"],
                        hashMapOfStatus["postStatus"],
                        hashMapOfStatus["shortStatus"],
                        hashMapOfStatus["challengeStatus"],
                        hashMapOfStatus["shareStatus"],
                        hashMapOfStatus["liveStatus"],
                        timeZone,
                    requestData?.countryCode)
                )
            } else {
                showToast("Please select minimum 3 tasks for monetizaion")
            }


        }.autoDispose()

        binding.switchStory.setOnCheckedChangeListener { _, b ->
            if (b) hashMapOfStatus["storyStatus"] = 1 else hashMapOfStatus["storyStatus"] = 0
        }
        binding.switchPost.setOnCheckedChangeListener { _, b ->
            if (b) hashMapOfStatus["postStatus"] = 1 else hashMapOfStatus["postStatus"] = 0
        }
        binding.switchShorts.setOnCheckedChangeListener { _, b ->
            if (b) hashMapOfStatus["shortStatus"] = 1 else hashMapOfStatus["shortStatus"] = 0
        }
        binding.switchChallenges.setOnCheckedChangeListener { _, b ->
            if (b) hashMapOfStatus["challengeStatus"] = 1 else hashMapOfStatus["challengeStatus"] =
                0
        }
        binding.switchShare.setOnCheckedChangeListener { _, b ->
            if (b) hashMapOfStatus["shareStatus"] = 1 else hashMapOfStatus["shareStatus"] = 0
        }
        binding.switchLive.setOnCheckedChangeListener { _, b ->
            if (b) hashMapOfStatus["liveStatus"] = 1 else hashMapOfStatus["liveStatus"] = 0
        }

        binding.ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        hubRequestViewModel.hubRequestState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is HubRequestViewStates.ErrorMessage -> {
                    showToast(state.errorMessage)
                }
                is HubRequestViewStates.LoadingState -> {
                    binding.tvSendRequest.isVisible = !state.isLoading
                   if(state.isLoading) showLoading() else hideLoading()
                }
                is HubRequestViewStates.RequestData -> {

                }
                is HubRequestViewStates.SuccessMessage -> {
                    loggedInUserCache.setIsHubRequestSent(true)
                    RxBus.publish(RxEvent.FinishHubRequest)
                    finish()

                }
            }
        }.autoDispose()
    }
}