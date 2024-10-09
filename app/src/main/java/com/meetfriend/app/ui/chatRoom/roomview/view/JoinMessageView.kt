package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewJoinMessageBinding

class JoinMessageView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: ViewJoinMessageBinding
    private lateinit var messageInfo: MessageInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_join_message, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewJoinMessageBinding.bind(view)

        binding.apply {

        }
    }

    fun bind(messageInfo: MessageInfo) {
        this.messageInfo = messageInfo


    }
}