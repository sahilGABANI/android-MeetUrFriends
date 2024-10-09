package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.MessageType
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewInitialMessageBinding
import com.meetfriend.app.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.*


class InitialMessageView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: ViewInitialMessageBinding
    private lateinit var messageInfo: MessageInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_initial_message, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewInitialMessageBinding.bind(view)

        binding.apply {

        }
    }

    fun bind(messageInfo: MessageInfo) {
        this.messageInfo = messageInfo

        when (messageInfo.messageType) {
            MessageType.Initial -> {
                binding.ivMessageType.visibility = GONE
                binding.llMessageContainer.visibility = GONE
            }

            MessageType.Ban -> {
                Glide.with(this)
                    .load(R.drawable.ic_ban_user)
                    .placeholder(R.drawable.ic_ban_user)
                    .error(R.drawable.ic_ban_user)
                    .into(binding.ivMessageType)
            }
            MessageType.Kickout -> {
                Glide.with(this)
                    .load(R.drawable.ic_kick_out_message)
                    .placeholder(R.drawable.ic_kick_out_message)
                    .error(R.drawable.ic_kick_out_message)
                    .into(binding.ivMessageType)

            }
            MessageType.Slap -> {
                Glide.with(this)
                    .load(R.drawable.ic_slap_message)
                    .placeholder(R.drawable.ic_slap_message)
                    .error(R.drawable.ic_slap_message)
                    .into(binding.ivMessageType)
            }
            MessageType.Join -> {
                Glide.with(this)
                    .load(R.drawable.ic_join_room)
                    .placeholder(R.drawable.ic_join_room)
                    .error(R.drawable.ic_join_room)
                    .into(binding.ivMessageType)
            }
            MessageType.Left -> {
                Glide.with(this)
                    .load(R.drawable.ic_left_room)
                    .placeholder(R.drawable.ic_left_room)
                    .error(R.drawable.ic_left_room)
                    .into(binding.ivMessageType)
            }
            else -> {}
        }
        binding.cvMessage.isVisible = messageInfo.messageType != MessageType.Date
        binding.tvDate.isVisible = messageInfo.messageType == MessageType.Date

        binding.tvDate.text = FileUtils.msgDateTime(convertTimestampToDate(messageInfo.timestemp?.toLong() ?: 0L))
        binding.tvMessage.text = messageInfo.message

    }

    private fun convertTimestampToDate(timestamp: Long): String {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = Date(timestamp)
            return dateFormat.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}