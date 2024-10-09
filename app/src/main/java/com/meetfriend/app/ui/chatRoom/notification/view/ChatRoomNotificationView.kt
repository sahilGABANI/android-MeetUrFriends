package com.meetfriend.app.ui.chatRoom.notification.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.notification.model.NotificationActionState
import com.meetfriend.app.api.notification.model.NotificationItemInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewChatRoomNotificationBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ChatRoomNotificationView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val notificationStateSubject: PublishSubject<NotificationActionState> =
        PublishSubject.create()
    val notificationState: Observable<NotificationActionState> = notificationStateSubject.hide()

    private lateinit var binding: ViewChatRoomNotificationBinding
    private lateinit var notificationInfo: NotificationItemInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_chat_room_notification, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewChatRoomNotificationBinding.bind(view)

        binding.apply {
            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                notificationStateSubject.onNext(
                    NotificationActionState.ProfileImageClick(
                        notificationInfo
                    )
                )
            }.autoDispose()

            tvAccept.throttleClicks().subscribeAndObserveOnMainThread {
                binding.llActionContainer.visibility = View.GONE
                notificationStateSubject.onNext(NotificationActionState.AcceptClick(notificationInfo))
            }.autoDispose()

            tvReject.throttleClicks().subscribeAndObserveOnMainThread {
                binding.llActionContainer.visibility = View.GONE
                notificationStateSubject.onNext(NotificationActionState.RejectClick(notificationInfo))
            }.autoDispose()

            ivDelete.throttleClicks().subscribeAndObserveOnMainThread {
                notificationStateSubject.onNext(NotificationActionState.DeleteClick(notificationInfo))
            }.autoDispose()

            rlDetailContainer.throttleClicks().subscribeAndObserveOnMainThread {
                notificationStateSubject.onNext(
                    NotificationActionState.ContainerClick(
                        notificationInfo
                    )
                )
            }
        }
    }

    fun bind(notificationInfo: NotificationItemInfo) {
        this.notificationInfo = notificationInfo

        binding.tvTime.text = notificationInfo.createdAt
        binding.tvNotificationDetail.text = notificationInfo.message
        Glide.with(this)
            .load(notificationInfo.fromUser?.profilePhoto)
            .error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        if (notificationInfo.type == "chat_request") {
            when (notificationInfo.isRepeat) {
                0 -> {
                    binding.llActionContainer.visibility = View.VISIBLE
                    binding.llAcceptedRejected.visibility = View.GONE
                }
                1 -> {
                    binding.llAcceptedRejected.visibility = View.VISIBLE
                    binding.llActionContainer.visibility = View.GONE

                    binding.tvAcceptedRejected.text = resources.getString(R.string.label_accepted)
                }
                2 -> {
                    binding.llAcceptedRejected.visibility = View.VISIBLE
                    binding.llActionContainer.visibility = View.GONE

                    binding.tvAcceptedRejected.text = resources.getString(R.string.label_rejected)
                }
            }
        } else {
            binding.llActionContainer.visibility = View.GONE
            binding.llAcceptedRejected.visibility = View.GONE
        }
    }
}
