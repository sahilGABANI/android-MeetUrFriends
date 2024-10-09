package com.meetfriend.app.ui.chat.onetoonechatroom.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.MessageType
import com.meetfriend.app.api.message.model.MessageAction
import com.meetfriend.app.ui.chatRoom.roomview.view.InitialMessageView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OneToOneChatMessageAdapter(
    private val context: Context,
    private val canSendMessage: Int,
    private val loginUserId: Int,
    private val profileImage: String?
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messageItemClicksSubject: PublishSubject<MessageAction> = PublishSubject.create()
    val messageItemClicks: Observable<MessageAction> = messageItemClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<MessageInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            when (it.messageType) {
                MessageType.Text -> {
                    if (loginUserId == it.senderId) {
                        adapterItems.add(AdapterItem.ChatRoomSenderMessageItem(it))
                    } else {
                        adapterItems.add(AdapterItem.ChatRoomMessageItem(it))
                    }
                }
                MessageType.ReplyStory -> {
                    if (loginUserId == it.senderId) {
                        adapterItems.add(AdapterItem.ChatRoomSenderStoryReplayItem(it))
                    } else {
                        adapterItems.add(AdapterItem.ChatRoomReceiverStoryReplayItem(it))
                    }
                }
                MessageType.Reply -> {
                    if (it.replyId != 0) {
                        if (loginUserId == it.senderId) {
                            adapterItems.add(AdapterItem.ChatRoomSenderMessageReplyItem(it))
                        } else {
                            adapterItems.add(AdapterItem.ChatRoomMessageReplyItem(it))
                        }
                    }
                }
                MessageType.Image -> {
                    if (loginUserId == it.senderId) {
                        adapterItems.add(AdapterItem.ChatRoomSenderImageMessageItem(it))
                    } else {
                        adapterItems.add(AdapterItem.ChatRoomImageMessageItem(it))
                    }
                }
                MessageType.Video -> {
                    if (loginUserId == it.senderId) {
                        adapterItems.add(AdapterItem.ChatRoomSenderImageMessageItem(it))
                    } else {
                        adapterItems.add(AdapterItem.ChatRoomImageMessageItem(it))
                    }
                }
                MessageType.SendGift -> {
                    if (loginUserId == it.senderId) {
                        adapterItems.add(AdapterItem.ChatRoomSenderGiftMessageItem(it))
                    } else {
                        adapterItems.add(AdapterItem.ChatRoomGiftMessageItem(it))
                    }
                }
                MessageType.RequestCoins -> {
                    if (loginUserId == it.senderId) {
                        adapterItems.add(AdapterItem.ChatRoomSenderGiftMessageItem(it))
                    } else {
                        adapterItems.add(AdapterItem.ChatRoomGiftMessageItem(it))
                    }
                }
                MessageType.Typing -> {
                    adapterItems.add(AdapterItem.ChatRoomTypingMessageItem(it))
                }

                MessageType.Date -> {
                    adapterItems.add(AdapterItem.ChatRoomDateMessageItem(it))
                }
                else -> {}
            }
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ChatRoomMessageItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    OneToOneChatMessageView(
                        context,
                        canSendMessage
                    ).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }

            ViewType.ChatRoomReceiverStoryReplayType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    OneToOneChatReceiverStoryReplayView(
                        context,
                        canSendMessage
                    ).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }
            ViewType.ChatRoomMessageReplyItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    OneToOneChatReplyMessageView(
                        context,
                        canSendMessage
                    ).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }
            ViewType.ChatRoomImageMessageItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    OneToOneChatImageMessageView(
                        context,
                        canSendMessage
                    ).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }
            ViewType.ChatRoomGiftMessageItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    GiftMessageView(context, canSendMessage).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }
            ViewType.ChatRoomSenderMessageType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    OneToOneChatSenderMessageView(
                        context,
                        canSendMessage,
                        profileImage
                    ).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }

            ViewType.ChatRoomSenderStoryReplayType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    OneToOneChatSenderReplayStoryView(
                        context,
                        canSendMessage,
                        profileImage
                    ).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }

            ViewType.ChatRoomSenderReplyItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    OneToOneChatReplySenderMessageView(
                        context,
                        canSendMessage,
                        profileImage
                    ).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }
            ViewType.ChatRoomSenderImageMessageItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    OneToOneChatSenderImageMessageView(
                        context,
                        canSendMessage,
                        profileImage
                    ).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }
            ViewType.ChatRoomSenderGiftMessageItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    GiftSenderMessageView(context, canSendMessage, profileImage).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }

            ViewType.ChatRoomTypingMessageItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    OneToOneChatTypingMessageView(context).apply {
                        messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                    }
                )
            }

            ViewType.ChatRoomDateMessageItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(
                    InitialMessageView(context).apply {
                    }
                )
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ChatRoomMessageItem -> {
                val messageInfo = adapterItem.messageInfo
                var isLastMessage = isLastMessageFromSender(position, messageInfo.senderId)
                (holder.itemView as OneToOneChatMessageView).bind(adapterItem.messageInfo, isLastMessage)
            }
            is AdapterItem.ChatRoomReceiverStoryReplayItem -> {
                val messageInfo = adapterItem.messageInfo
                var isLastMessage = isLastMessageFromSender(position, messageInfo.senderId)
                (holder.itemView as OneToOneChatReceiverStoryReplayView).bind(adapterItem.messageInfo, isLastMessage)
            }
            is AdapterItem.ChatRoomMessageReplyItem -> {
                val messageInfo = adapterItem.messageInfo
                var isLastMessage = isLastMessageFromSender(position, messageInfo.senderId)
                (holder.itemView as OneToOneChatReplyMessageView).bind(adapterItem.messageInfo, isLastMessage)
            }
            is AdapterItem.ChatRoomImageMessageItem -> {
                val messageInfo = adapterItem.messageInfo
                var isLastMessage = isLastMessageFromSender(position, messageInfo.senderId)
                (holder.itemView as OneToOneChatImageMessageView).bind(adapterItem.messageInfo, isLastMessage)
            }
            is AdapterItem.ChatRoomGiftMessageItem -> {
                val messageInfo = adapterItem.messageInfo
                var isLastMessage = isLastMessageFromSender(position, messageInfo.senderId)
                (holder.itemView as GiftMessageView).bind(adapterItem.messageInfo, isLastMessage)
            }
            is AdapterItem.ChatRoomSenderMessageItem -> {
                (holder.itemView as OneToOneChatSenderMessageView).bind(adapterItem.messageInfo)
            }
            is AdapterItem.ChatRoomSenderStoryReplayItem -> {
                (holder.itemView as OneToOneChatSenderReplayStoryView).bind(adapterItem.messageInfo)
            }
            is AdapterItem.ChatRoomSenderMessageReplyItem -> {
                (holder.itemView as OneToOneChatReplySenderMessageView).bind(adapterItem.messageInfo)
            }
            is AdapterItem.ChatRoomSenderImageMessageItem -> {
                (holder.itemView as OneToOneChatSenderImageMessageView).bind(adapterItem.messageInfo)
            }
            is AdapterItem.ChatRoomSenderGiftMessageItem -> {
                (holder.itemView as GiftSenderMessageView).bind(adapterItem.messageInfo)
            }
            is AdapterItem.ChatRoomTypingMessageItem -> {
                (holder.itemView as OneToOneChatTypingMessageView).bind(adapterItem.messageInfo)
            }
            is AdapterItem.ChatRoomDateMessageItem -> {
                (holder.itemView as InitialMessageView).bind(adapterItem.messageInfo)
            }
        }
    }

    private class ChatRoomMessageAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ChatRoomMessageItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomMessageItemType.ordinal)
        data class ChatRoomReceiverStoryReplayItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomReceiverStoryReplayType.ordinal)

        data class ChatRoomMessageReplyItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomMessageReplyItemType.ordinal)

        data class ChatRoomImageMessageItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomImageMessageItemType.ordinal)

        data class ChatRoomGiftMessageItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomGiftMessageItemType.ordinal)

        data class ChatRoomSenderMessageItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomSenderMessageType.ordinal)

        data class ChatRoomSenderStoryReplayItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomSenderStoryReplayType.ordinal)

        data class ChatRoomSenderMessageReplyItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomSenderReplyItemType.ordinal)
        data class ChatRoomSenderImageMessageItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomSenderImageMessageItemType.ordinal)

        data class ChatRoomSenderGiftMessageItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomSenderGiftMessageItemType.ordinal)

        data class ChatRoomTypingMessageItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomTypingMessageItemType.ordinal)

        data class ChatRoomDateMessageItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomDateMessageItemType.ordinal)
    }

    private enum class ViewType {
        ChatRoomMessageItemType,
        ChatRoomMessageReplyItemType,
        ChatRoomImageMessageItemType,
        ChatRoomGiftMessageItemType,
        ChatRoomSenderMessageType,
        ChatRoomSenderStoryReplayType,
        ChatRoomReceiverStoryReplayType,
        ChatRoomSenderReplyItemType,
        ChatRoomSenderImageMessageItemType,
        ChatRoomSenderGiftMessageItemType,
        ChatRoomTypingMessageItemType,
        ChatRoomDateMessageItemType
    }

    private fun isLastMessageFromSender(position: Int, senderId: Int?): Boolean {
        if (position < itemCount - 1) {
            if (position - 1 >= 0) {
                val nextMessage = listOfDataItems?.get(position - 1)
                return nextMessage?.senderId != senderId
            }
        }
        return true
    }
}
