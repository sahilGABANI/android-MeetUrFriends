package com.meetfriend.app.ui.chatRoom.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityOtherUserFullProfileBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import javax.inject.Inject

class OtherUserFullProfileActivity : BasicActivity() {

    companion object {
        private const val INTENT_MESSAGE_INFO = "INTENT_MESSAGE_INFO"
        fun getIntent(context: Context, messageInfo: MessageInfo): Intent {
            val intent = Intent(context, OtherUserFullProfileActivity::class.java)
            intent.putExtra(INTENT_MESSAGE_INFO, messageInfo)
            return intent
        }
    }


    private lateinit var binding: ActivityOtherUserFullProfileBinding

    private lateinit var messageInfo: MessageInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtherUserFullProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        listenToViewEvent()
        loadDataFromIntent()

    }

    private fun loadDataFromIntent() {
        intent?.let {
            this.messageInfo = it.getParcelableExtra(INTENT_MESSAGE_INFO)!!
        }

        Glide.with(this)
            .load(messageInfo.fileUrl)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivProfileImage)

        binding.tvUserName.text =
            if (messageInfo.senderId == loggedInUserCache.getLoggedInUserId()) getString(R.string.label_you) else messageInfo.senderName
    }

    private fun listenToViewEvent() {

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }

    }

}