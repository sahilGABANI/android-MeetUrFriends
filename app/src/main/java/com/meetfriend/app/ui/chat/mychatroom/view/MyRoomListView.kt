package com.meetfriend.app.ui.chat.mychatroom.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomListItemActionState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewMyRoomListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant.FiXED_9_INT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates
import kotlin.random.Random

class MyRoomListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val chatRoomItemClickSubject: PublishSubject<ChatRoomListItemActionState> =
        PublishSubject.create()
    val chatRoomItemClick: Observable<ChatRoomListItemActionState> = chatRoomItemClickSubject.hide()

    private lateinit var binding: ViewMyRoomListBinding
    private lateinit var chatRoomInfo: ChatRoomInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_my_room_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewMyRoomListBinding.bind(view)

        MeetFriendApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        binding.apply {
            binding.rlChatRoomDataContainer.throttleClicks().subscribeAndObserveOnMainThread {
                chatRoomItemClickSubject.onNext(
                    ChatRoomListItemActionState.ContainerClick(
                        chatRoomInfo
                    )
                )
            }.autoDispose()
        }
    }

    fun bind(chatRoomInfo: ChatRoomInfo) {
        this.chatRoomInfo = chatRoomInfo

        val r = Random
        val listOfBackground = arrayListOf(
            R.drawable.bg_chat_room_brown,
            R.drawable.bg_chat_room_pink,
            R.drawable.bg_chat_room_yellow,
            R.drawable.bg_chat_room_green,
            R.drawable.bg_chat_room_grey,
            R.drawable.bg_chat_room_light_pink,
            R.drawable.bg_chat_room_list_view_purple,
            R.drawable.bg_chat_room_orange,
            R.drawable.bg_chat_room_sky,
        )
        val i1 = r.nextInt(FiXED_9_INT - 0) + 0

        Glide.with(this)
            .load(chatRoomInfo.filePath)
            .error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivProfileImage)

        binding.tvChatRoomName.text = chatRoomInfo.roomName

        binding.tvUserCount.text = chatRoomInfo.noOfJoinCount.toString()

        binding.rlChatRoomDataContainer.background =
            ContextCompat.getDrawable(context, listOfBackground[i1])
    }
}
