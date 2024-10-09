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
import com.meetfriend.app.databinding.ViewOneToOneChatReceiverStoryReplayBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.FileUtils.formatTo
import com.meetfriend.app.utils.FileUtils.toDate
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class OneToOneChatReceiverStoryReplayView(context: Context, canSendMessage: Int) :
    ConstraintLayoutWithLifecycle(context) {

    private val messageItemClicksSubject: PublishSubject<MessageAction> = PublishSubject.create()
    val messageItemItemClicks: Observable<MessageAction> = messageItemClicksSubject.hide()

    private lateinit var binding: ViewOneToOneChatReceiverStoryReplayBinding
    private lateinit var messageInfo: MessageInfo
    private var canSendMessage: Int = 0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
        this.canSendMessage = canSendMessage
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_one_to_one_chat_receiver_story_replay, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewOneToOneChatReceiverStoryReplayBinding.bind(view)

        MeetFriendApplication.component.inject(this)

        binding.apply {
            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.ViewProfile(messageInfo))
            }.autoDispose()
            tvUserName.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.ViewProfile(messageInfo))
            }.autoDispose()
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

    fun bind(messageInfo: MessageInfo, isLastMessageFromSender: Boolean) {
        this.messageInfo = messageInfo
        binding.swipeRevelRight.isVisible = messageInfo.messageType != MessageType.Initial
        if (canSendMessage == 0) binding.swipeRevelRight.setLockDrag(true)

        binding.IvStoryImage.throttleClicks().subscribeAndObserveOnMainThread {
            messageItemClicksSubject.onNext(MessageAction.StoryView(messageInfo))
        }

        if (messageInfo.storyId == 0 || messageInfo.storyId == null) {
            binding.TvReplayText.visibility = View.VISIBLE
            binding.IvStoryImage.visibility = View.GONE
            binding.TvUnavailableText.visibility = View.VISIBLE
        } else {
            binding.TvUnavailableText.visibility = View.GONE
            binding.IvStoryImage.visibility = View.VISIBLE
            binding.TvReplayText.visibility = View.VISIBLE
            Glide.with(context).load(messageInfo.fileUrl)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .error(R.drawable.ic_empty_profile_placeholder).into(binding.IvStoryImage)
        }

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

        binding.ivUserProfileImage.visibility = if (isLastMessageFromSender) View.VISIBLE else View.INVISIBLE

        Glide.with(context)
            .load(messageInfo.senderProfile)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        binding.tvUserName.text = messageInfo.senderName
        binding.tvMessage.text = messageInfo.message
        val time = messageInfo.createdAt?.toDate()?.formatTo("dd MMM yyyy,HH:mm a")
        binding.tvTime.text = time
    }
}
