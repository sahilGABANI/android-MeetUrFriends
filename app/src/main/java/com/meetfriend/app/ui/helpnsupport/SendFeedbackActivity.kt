package com.meetfriend.app.ui.helpnsupport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivitySendFeedbackBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.helpnsupport.viewmodel.FeedbackViewState
import com.meetfriend.app.ui.helpnsupport.viewmodel.SendFeedbackViewModel
import javax.inject.Inject

class SendFeedbackActivity : BasicActivity() {

    private lateinit var binding: ActivitySendFeedbackBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SendFeedbackViewModel>
    private lateinit var sendFeedbackViewModel: SendFeedbackViewModel

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, SendFeedbackActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        sendFeedbackViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        initUI()
    }

    private fun initUI() {
        binding.ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        binding.saveAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.edtDescription.text.isNullOrEmpty()) {
                showToast(getString(R.string.label_please_enter_your_feedback))
            } else {
                sendFeedbackViewModel.sendFeedback(
                    binding.ratingBar.rating,
                    binding.edtDescription.text.toString()
                )
            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        sendFeedbackViewModel.feedbackState.subscribeAndObserveOnMainThread {
            when (it) {

                is FeedbackViewState.LoadingState -> {
                    binding.progressBar.visibility = if (it.isLoading) View.VISIBLE else View.GONE
                    binding.saveAppCompatTextView.visibility =
                        if (it.isLoading) View.GONE else View.VISIBLE
                }
                is FeedbackViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                    finish()
                }
                is FeedbackViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
            }
        }.autoDispose()
    }

}