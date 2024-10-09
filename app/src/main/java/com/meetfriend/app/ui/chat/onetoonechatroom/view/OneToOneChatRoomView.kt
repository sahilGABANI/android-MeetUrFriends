package com.meetfriend.app.ui.chat.onetoonechatroom.view

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomListItemActionState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewOneToOneChatRoomListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.FileUtils.formatTo
import com.meetfriend.app.utils.FileUtils.toDate
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.*
import javax.inject.Inject
import kotlin.properties.Delegates

@RequiresApi(Build.VERSION_CODES.N)
class OneToOneChatRoomView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val chatRoomItemClickSubject: PublishSubject<ChatRoomListItemActionState> =
        PublishSubject.create()
    val chatRoomItemClick: Observable<ChatRoomListItemActionState> = chatRoomItemClickSubject.hide()

    private lateinit var binding: ViewOneToOneChatRoomListBinding
    private lateinit var chatRoomInfo: ChatRoomInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()

    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_one_to_one_chat_room_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewOneToOneChatRoomListBinding.bind(view)

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

            delete.throttleClicks().subscribeAndObserveOnMainThread {
                chatRoomItemClickSubject.onNext(ChatRoomListItemActionState.DeleteClick(chatRoomInfo))
                binding.swipeRevelRight.close(true)
            }
        }
    }


    fun bind(chatRoomInfo: ChatRoomInfo) {
        this.chatRoomInfo = chatRoomInfo

        val profileImage =
            if (chatRoomInfo.userId.equals(loggedInUserCache.getLoggedInUserId())) {
                chatRoomInfo.receiver?.profilePhoto
            } else {
                chatRoomInfo.user?.profilePhoto
            }

        Glide.with(this).load(profileImage).error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder).into(binding.ivProfileImage)

        if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
            binding.tvChatRoomName.text = chatRoomInfo.user?.firstName

            if (chatRoomInfo.user?.isVerified == 1) {
                binding.ivAccountVerified.visibility = View.VISIBLE
            } else {
                binding.ivAccountVerified.visibility = View.GONE
            }
        } else {
            binding.tvChatRoomName.text = chatRoomInfo.receiver?.firstName
            if (chatRoomInfo.receiver?.isVerified == 1) {
                binding.ivAccountVerified.visibility = View.VISIBLE
            } else {
                binding.ivAccountVerified.visibility = View.GONE
            }
        }
        binding.llUserCount.visibility = View.GONE
        binding.tvUserCount.text = chatRoomInfo.noOfJoinCount.toString()
        binding.tvLastMessage.text =
            if (chatRoomInfo.lastMessage.isNullOrEmpty()) chatRoomInfo.messageType else chatRoomInfo.lastMessage

        if (chatRoomInfo.unreadCount!! > 0) {
            binding.tvUnReadMessageCount.visibility = View.VISIBLE

            if (chatRoomInfo.unreadCount < 10) {
                binding.tvUnReadMessageCount.text = chatRoomInfo.unreadCount.toString()
            } else {
                binding.tvUnReadMessageCount.text = "9+"
            }
        } else
            binding.tvUnReadMessageCount.visibility = View.GONE

        lastMessageTime()

        binding.cvContainer.visibility = View.VISIBLE

    }

    private fun lastMessageTime() {
        val lastMessageDate = chatRoomInfo.lastDatetime?.toDate()?.formatTo("MM-dd-yyyy")
        val lastMessageTime = chatRoomInfo.lastDatetime?.toDate()?.formatTo("hh:mm a")
        val currentDate: String = DateFormat.format("MM-dd-yyyy", Date().time) as String
        val lastMessageMonthName = chatRoomInfo.lastDatetime?.toDate()?.formatTo("MMM")
        val dateLastMessage = chatRoomInfo.lastDatetime?.toDate()?.formatTo("dd")?.toInt()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val yesterdayDate = cal.time.formatTo("MM-dd-yyyy")
        if (currentDate == lastMessageDate) {
            binding.tvLastMessageTime.text = lastMessageTime
        } else {
            if (lastMessageDate == yesterdayDate) {
                binding.tvLastMessageTime.text =
                    " ${resources.getString(R.string.label_yesterday)} $lastMessageTime"
            } else {
                binding.tvLastMessageTime.text = "$lastMessageMonthName $dateLastMessage"
            }
        }
    }
}