package com.meetfriend.app.ui.chat.onetoonechatroom.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.MessageType
import com.meetfriend.app.api.message.model.MessageAction
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewGiftMessageBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant.TIME_FORMAT
import com.meetfriend.app.utils.FileUtils.formatTo
import com.meetfriend.app.utils.FileUtils.toDate
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class GiftMessageView(context: Context, canSendMessage: Int) :
    ConstraintLayoutWithLifecycle(context) {

    private var isLoginUser: Boolean = false
    private val messageItemClicksSubject: PublishSubject<MessageAction> = PublishSubject.create()
    val messageItemItemClicks: Observable<MessageAction> = messageItemClicksSubject.hide()

    private lateinit var binding: ViewGiftMessageBinding
    private lateinit var messageInfo: MessageInfo
    private var canSendMessage: Int = 0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
        this.canSendMessage = canSendMessage
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_gift_message, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewGiftMessageBinding.bind(view)

        MeetFriendApplication.component.inject(this)

        binding.apply {
            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.ViewProfile(messageInfo))
            }.autoDispose()

            tvUserName.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.ViewProfile(messageInfo))
            }.autoDispose()

            acceptAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.AcceptGiftRequest(messageInfo))
            }.autoDispose()

            rejectAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                messageItemClicksSubject.onNext(MessageAction.RejectGiftRequest(messageInfo))
            }.autoDispose()
        }
    }

    fun bind(messageInfo: MessageInfo, isLastMessageFromSender: Boolean) {
        this.messageInfo = messageInfo

        binding.ivUserProfileImage.visibility = if (isLastMessageFromSender) View.VISIBLE else View.INVISIBLE

        Glide.with(context)
            .load(messageInfo.senderProfile)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        binding.tvUserName.text = messageInfo.senderName

        val time = messageInfo.createdAt?.toDate()?.formatTo(TIME_FORMAT)
        binding.tvTime.text = time

        Glide.with(context)
            .load(messageInfo.fileUrl)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.giftAppCompatImageView)

        binding.totalCoinsAppCompatTextView.text = messageInfo.requestCoins.toString()
        binding.giftNameAppCompatTextView.text = messageInfo.message

        isLoginUser = messageInfo.senderId == loggedInUserCache.getLoggedInUserId()

        when (messageInfo.messageType) {
            MessageType.RequestCoins -> {
                handleRequestCoins(messageInfo)
            }
            MessageType.SendGift -> {
                if (isLoginUser) {
                    binding.acceptRejectLinearLayout.isVisible = false
                    binding.sentReceivedAppCompatTextView.isVisible = true
                    binding.sentReceivedAppCompatTextView.text = resources.getString(R.string.label_sent)
                    binding.giftDataRelativeLayout.background =
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.bg_chat_gift
                        )
                    binding.sentReceivedAppCompatTextView.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.color_tab_purple)
                } else {
                    binding.acceptRejectLinearLayout.isVisible = false
                    binding.sentReceivedAppCompatTextView.isVisible = true
                    binding.sentReceivedAppCompatTextView.text = resources.getString(R.string.label_received)
                    binding.giftDataRelativeLayout.background =
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.bg_chat_gift
                        )
                    binding.sentReceivedAppCompatTextView.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.color_tab_purple)
                }
            }
            else -> {
            }
        }
    }

    private fun handleRequestCoins(messageInfo: MessageInfo) {
        when (messageInfo.requestStatus) {
            0 -> {
                if (isLoginUser) {
                    binding.acceptRejectLinearLayout.isVisible = false
                    binding.sentReceivedAppCompatTextView.isVisible = true
                    binding.sentReceivedAppCompatTextView.text = resources.getString(R.string.label_requested)
                    binding.giftDataRelativeLayout.background =
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.bg_gift_requested
                        )
                    binding.sentReceivedAppCompatTextView.backgroundTintList =
                        ContextCompat.getColorStateList(
                            context,
                            R.color.color_tab_purple_70
                        )
                } else {
                    binding.acceptRejectLinearLayout.isVisible = true
                    binding.sentReceivedAppCompatTextView.isVisible = false
                    binding.giftDataRelativeLayout.background =
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.bg_chat_gift
                        )
                }
            }
            1 -> {
                binding.acceptRejectLinearLayout.isVisible = false
                binding.sentReceivedAppCompatTextView.isVisible = true
                binding.sentReceivedAppCompatTextView.text = resources.getString(R.string.label_accepted)
                binding.giftDataRelativeLayout.background =
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.bg_gift_accepted
                    )
                binding.sentReceivedAppCompatTextView.backgroundTintList =
                    ContextCompat.getColorStateList(
                        context,
                        R.color.color_accept_gift_request_70
                    )
            }
            2 -> {
                binding.acceptRejectLinearLayout.isVisible = false
                binding.sentReceivedAppCompatTextView.isVisible = true
                binding.sentReceivedAppCompatTextView.text = resources.getString(R.string.label_rejected)
                binding.giftDataRelativeLayout.background =
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.bg_gift_rejected
                    )
                binding.sentReceivedAppCompatTextView.backgroundTintList =
                    ContextCompat.getColorStateList(
                        context,
                        R.color.color_reject_gift_request_70
                    )
            }
        }
    }
}
