package com.meetfriend.app.ui.chatRoom.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.TempChatRoomInfo
import com.meetfriend.app.api.chat.model.TempMessageInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewChatRoomAdminBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ChatRoomAdminView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val chatRoomAdminItemClickSubject: PublishSubject<TempChatRoomInfo> =
        PublishSubject.create()
    val chatRoomAdminItemClick: Observable<TempChatRoomInfo> = chatRoomAdminItemClickSubject.hide()

    private lateinit var binding: ViewChatRoomAdminBinding
    private lateinit var tempMessageInfo: TempMessageInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_chat_room_admin, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewChatRoomAdminBinding.bind(view)

        binding.apply {

        }
    }

    fun bind(tempMessageInfo: TempMessageInfo) {
        this.tempMessageInfo = tempMessageInfo

        Glide.with(this)
            .load(tempMessageInfo.profileImage)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        binding.tvUserName.text = tempMessageInfo.userName
        binding.tvDescription.text = tempMessageInfo.message
    }
}