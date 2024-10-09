package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewChatRoomMessageBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ChatRoomMessageView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val messageItemClicksSubject: PublishSubject<MessageInfo> = PublishSubject.create()
    val messageItemItemClicks: Observable<MessageInfo> = messageItemClicksSubject.hide()

    private lateinit var binding: ViewChatRoomMessageBinding
    private lateinit var messageInfo: MessageInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_chat_room_message, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewChatRoomMessageBinding.bind(view)

        binding.apply {
            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(messageInfo)
            }.autoDispose()
            tvUserName.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(messageInfo)
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

        binding.tvUserName.text = messageInfo.senderName
        binding.tvMessage.text = messageInfo.message
        binding.tvAge.visibility = GONE
        binding.tvGender.visibility = GONE

    }
}