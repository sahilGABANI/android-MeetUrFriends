package com.meetfriend.app.ui.chatRoom.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.chat.joinchatroom.JoinChatRoomFragment
import com.meetfriend.app.ui.chat.mychatroom.MyChatRoomFragment
import com.meetfriend.app.ui.chat.onetoonechatroom.OneToOneChatRoomFragment
import com.meetfriend.app.ui.chat.profile.ChatRoomProfileFragment

class ChatRoomTabAdapter(
    fragmentActivity: FragmentActivity,
) : FragmentStateAdapter(fragmentActivity) {
    companion object {
        const val JOIN_ROOM_POSITION = 0
        const val MY_CHAT_ROOM_POSITION = 1
        const val ONE_TO_ONE_CHAT_ROOM_POSITION = 3
        const val CHAT_ROOM_PROFILE_POSITION = 4
        const val ITEM_COUNT = 5
    }

    override fun getItemCount() = ITEM_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            JOIN_ROOM_POSITION -> {
                JoinChatRoomFragment.newInstance()
            }

            MY_CHAT_ROOM_POSITION -> {
                MyChatRoomFragment.newInstance()
            }

            ONE_TO_ONE_CHAT_ROOM_POSITION -> {
                OneToOneChatRoomFragment.newInstance()
            }

            CHAT_ROOM_PROFILE_POSITION -> {
                ChatRoomProfileFragment.newInstance(0)
            }

            else -> {
                JoinChatRoomFragment.newInstance()
            }
        }
    }
}