package com.meetfriend.app.ui.chat.onetoonechatroom.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewForwardPeopleListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class ForwardPeopleListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val forwardPeopleClickSubject: PublishSubject<ChatRoomInfo> = PublishSubject.create()
    val forwardPeopleClick: Observable<ChatRoomInfo> = forwardPeopleClickSubject.hide()

    private lateinit var binding: ViewForwardPeopleListBinding
    private lateinit var chatRoomInfo: ChatRoomInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_forward_people_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewForwardPeopleListBinding.bind(view)

        MeetFriendApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        binding.apply {
            binding.rlChatRoomDataContainer.throttleClicks().subscribeAndObserveOnMainThread {
                forwardPeopleClickSubject.onNext(chatRoomInfo)
            }.autoDispose()
        }
    }

    fun bind(chatRoomInfo: ChatRoomInfo) {
        this.chatRoomInfo = chatRoomInfo

        val profileImage =
            if (chatRoomInfo.userId == loggedInUserCache.getLoggedInUserId()) {
                chatRoomInfo.receiver?.profilePhoto
            } else {
                chatRoomInfo.user?.profilePhoto
            }
        Glide.with(this)
            .load(profileImage)
            .error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivProfileImage)

        if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
            binding.tvChatRoomName.text = chatRoomInfo.user?.firstName
        } else {
            binding.tvChatRoomName.text = chatRoomInfo.receiver?.firstName
        }

        binding.ivSelected.isVisible = chatRoomInfo.isSelected
        binding.ivUnselected.isVisible = !chatRoomInfo.isSelected
    }
}
