package com.meetfriend.app.ui.chat.onetoonechatroom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityViewVideoMessageBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import javax.inject.Inject

class ViewVideoMessageActivity : BasicActivity() {

    companion object {
        private const val INTENT_MESSAGE_INFO = "INTENT_MESSAGE_INFO"

        fun getIntent(context: Context, messageInfo: MessageInfo): Intent {
            val intent = Intent(context, ViewVideoMessageActivity::class.java)
            intent.putExtra(INTENT_MESSAGE_INFO, messageInfo)
            return intent
        }
    }

    lateinit var binding: ActivityViewVideoMessageBinding

    lateinit var messageInfo: MessageInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityViewVideoMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this@ViewVideoMessageActivity)

        loadDataFromIntent()
        playVideo()
        listenToViewEvent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            this.messageInfo = it.getParcelableExtra(INTENT_MESSAGE_INFO)!!
        }
        binding.tvUserName.text =
            if (messageInfo.senderId == loggedInUserCache.getLoggedInUserId()) {
                getString(R.string.label_you)
            } else {
                messageInfo.senderName
            }
    }

    private fun playVideo() {
        val uri: Uri = Uri.parse(messageInfo.videoUrl)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(mediaController)
        binding.videoView.setVideoURI(uri)
        binding.videoView.requestFocus()
        binding.videoView.start()

        binding.videoView.setOnPreparedListener {
            hideLoading()
        }
    }

    private fun listenToViewEvent() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()
    }
}
