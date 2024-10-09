package com.meetfriend.app.ui.chat.onetoonechatroom.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.message.model.MessageAction
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewTypingMessageBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class OneToOneChatTypingMessageView(context: Context) :
    ConstraintLayoutWithLifecycle(context) {

    private val messageItemClicksSubject: PublishSubject<MessageAction> = PublishSubject.create()
    val messageItemItemClicks: Observable<MessageAction> = messageItemClicksSubject.hide()

    private lateinit var binding: ViewTypingMessageBinding
    private lateinit var messageInfo: MessageInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_typing_message, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewTypingMessageBinding.bind(view)

        binding.apply {
            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.ViewProfile(messageInfo))
            }.autoDispose()
        }
    }

    fun bind(messageInfo: MessageInfo) {
        this.messageInfo = messageInfo

        Glide.with(context)
            .load(messageInfo.senderProfile)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)
    }
}
