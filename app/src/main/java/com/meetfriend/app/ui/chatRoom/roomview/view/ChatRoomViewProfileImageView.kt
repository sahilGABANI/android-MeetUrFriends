package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.TempMessageInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewChatRoomViewProfileImagesBinding

class ChatRoomViewProfileImageView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: ViewChatRoomViewProfileImagesBinding
    private lateinit var chatRoomInfo: TempMessageInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_chat_room_view_profile_images, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewChatRoomViewProfileImagesBinding.bind(view)

        binding.apply {

        }
    }

    fun bind(chatRoomInfo: TempMessageInfo) {
        this.chatRoomInfo = chatRoomInfo

        Glide.with(context)
            .load(chatRoomInfo.profileImage)
            .into(binding.ivProfileImage)


    }
}