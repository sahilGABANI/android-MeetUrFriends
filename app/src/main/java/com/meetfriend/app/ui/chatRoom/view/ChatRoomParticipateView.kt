package com.meetfriend.app.ui.chatRoom.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.ChatRoomUser
import com.meetfriend.app.api.chat.model.TempChatRoomInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewChatRoomParticipateBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomParticipateView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val chatRoomParticipateItemClickSubject: PublishSubject<TempChatRoomInfo> =
        PublishSubject.create()
    val chatRoomParticipateItemClick: Observable<TempChatRoomInfo> =
        chatRoomParticipateItemClickSubject.hide()

    private lateinit var binding: ViewChatRoomParticipateBinding
    private lateinit var tempMessageInfo: ChatRoomUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_chat_room_participate, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewChatRoomParticipateBinding.bind(view)

        binding.apply {

        }
    }

    fun bind(tempMessageInfo: ChatRoomUser) {
        this.tempMessageInfo = tempMessageInfo

        Glide.with(this)
            .load(tempMessageInfo.senderProfile)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        binding.tvUserName.text = tempMessageInfo.chatUserName
        tempMessageInfo.bio?.let {
            binding.tvDescription.text = it
        }
        tempMessageInfo.role?.let {
            if (it == "0") {
                binding.tvAdmin.isVisible = false
            }
        }
        checkExpiration()

    }

    private fun checkExpiration() {
        val c = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val getCurrentDateTime = sdf.format(c.time)
        val getMyTime = tempMessageInfo.expireDate.toString()

        if (getMyTime.compareTo(getCurrentDateTime) < 0) {
            binding.tvAdmin.isVisible = false
        } else {

        }
    }
}