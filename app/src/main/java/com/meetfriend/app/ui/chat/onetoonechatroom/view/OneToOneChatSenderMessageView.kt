package com.meetfriend.app.ui.chat.onetoonechatroom.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.MessageType
import com.meetfriend.app.api.message.model.MessageAction
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewOneToOneChatSenderMessageBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class OneToOneChatSenderMessageView(context: Context, canSendMessage: Int, profileImage: String?) :
    ConstraintLayoutWithLifecycle(context) {

    private val messageItemClicksSubject: PublishSubject<MessageAction> = PublishSubject.create()
    val messageItemItemClicks: Observable<MessageAction> = messageItemClicksSubject.hide()

    private lateinit var binding: ViewOneToOneChatSenderMessageBinding
    private lateinit var messageInfo: MessageInfo
    private var canSendMessage: Int = 0
    private var profileImage: String? = null

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
        this.canSendMessage = canSendMessage
        this.profileImage = profileImage
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_one_to_one_chat_sender_message, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewOneToOneChatSenderMessageBinding.bind(view)

        MeetFriendApplication.component.inject(this)

        binding.apply {
            delete.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.Delete(messageInfo))
                binding.swipeRevelRight.close(true)
            }
            tvEdit.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.Edit(messageInfo))
                binding.swipeRevelRight.close(true)
            }
            tvMore.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.More(messageInfo))
                binding.swipeRevelRight.close(true)
            }
        }
    }

    fun bind(messageInfo: MessageInfo) {
        this.messageInfo = messageInfo
        binding.swipeRevelRight.isVisible = messageInfo.messageType != MessageType.Initial
        if (canSendMessage == 0) binding.swipeRevelRight.setLockDrag(true)
        if (messageInfo.senderId == loggedInUserCache.getLoggedInUserId()) {
            binding.delete.isVisible = true
            binding.tvEdit.isVisible = true
            binding.tvMore.isVisible = true
        } else {
            binding.delete.isVisible = false
            binding.tvEdit.isVisible = false
            binding.tvMore.isVisible = true
        }
        binding.ivSave.isVisible = messageInfo.isSave
        binding.rlMessageContainer.visibility = View.VISIBLE

        binding.tvMessage.text = messageInfo.message

        binding.ivUserProfileImage.isVisible = messageInfo.isSeen == 1

        Glide.with(context)
            .load(profileImage)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)
    }
}
