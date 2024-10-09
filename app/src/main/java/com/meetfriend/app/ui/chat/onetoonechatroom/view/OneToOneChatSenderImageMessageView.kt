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
import com.meetfriend.app.databinding.ViewChatSenderImageMessageBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class OneToOneChatSenderImageMessageView(context: Context, canSendMessage: Int, profileImage: String?) :
    ConstraintLayoutWithLifecycle(context) {

    private val messageItemClicksSubject: PublishSubject<MessageAction> = PublishSubject.create()
    val messageItemItemClicks: Observable<MessageAction> = messageItemClicksSubject.hide()

    private lateinit var binding: ViewChatSenderImageMessageBinding
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
        val view = View.inflate(context, R.layout.view_chat_sender_image_message, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewChatSenderImageMessageBinding.bind(view)

        MeetFriendApplication.component.inject(this)

        binding.apply {
            delete.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.Delete(messageInfo))
                binding.swipeRevelRight.close(true)
            }
            ivMessage.throttleClicks().subscribeAndObserveOnMainThread {
                if (messageInfo.messageType == MessageType.Image) {
                    messageItemClicksSubject.onNext(MessageAction.ViewPhoto(messageInfo))
                } else {
                    messageItemClicksSubject.onNext(MessageAction.ViewVideo(messageInfo))
                }
            }.autoDispose()
        }
    }

    fun bind(messageInfo: MessageInfo) {
        this.messageInfo = messageInfo

        if (canSendMessage == 0) binding.swipeRevelRight.setLockDrag(true)

        if (messageInfo.senderId != loggedInUserCache.getLoggedInUserId()) {
            binding.swipeRevelRight.setLockDrag(true)
        } else {
            binding.swipeRevelRight.setLockDrag(false)
        }

        binding.ivPlay.isVisible = messageInfo.messageType == MessageType.Video

        binding.ivSave.isVisible = messageInfo.isSave
        binding.rlMessageContainer.visibility = View.VISIBLE

        Glide.with(context)
            .load(messageInfo.fileUrl)
            .placeholder(R.drawable.ic_placer_holder_image_new)
            .error(R.drawable.ic_placer_holder_image_new)
            .into(binding.ivMessage)

        binding.ivUserProfileImage.isVisible = messageInfo.isSeen == 1

        Glide.with(context)
            .load(profileImage)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)
    }
}
