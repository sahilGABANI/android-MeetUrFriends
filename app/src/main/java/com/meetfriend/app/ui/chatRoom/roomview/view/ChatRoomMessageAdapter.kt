package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.MessageType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ChatRoomMessageAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messageItemClicksSubject: PublishSubject<MessageInfo> = PublishSubject.create()
    val messageItemClicks: Observable<MessageInfo> = messageItemClicksSubject.hide()

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
                MessageType.Initial -> {
                    adapterItems.add(AdapterItem.ChatRoomMessageInitialItem(it))
                }
                MessageType.Kickout -> {
                    adapterItems.add(AdapterItem.ChatRoomMessageInitialItem(it))
                }
                MessageType.Slap -> {
                    adapterItems.add(AdapterItem.ChatRoomMessageInitialItem(it))
                }
                MessageType.Ban -> {
                    adapterItems.add(AdapterItem.ChatRoomMessageInitialItem(it))
                }
                MessageType.JoinF -> {
                    adapterItems.add(AdapterItem.ChatRoomMessageJoinItem(it))
                }
                MessageType.Text -> {
                    adapterItems.add(AdapterItem.ChatRoomMessageItem(it))
                }
                MessageType.Join -> {
                    adapterItems.add(AdapterItem.ChatRoomMessageInitialItem(it))
                }
                MessageType.Left -> {
                    adapterItems.add(AdapterItem.ChatRoomMessageInitialItem(it))
                }
                null -> {
                    adapterItems.add(AdapterItem.ChatRoomMessageItem(it))
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
                ChatRoomMessageAdapterViewHolder(ChatRoomMessageView(context).apply {
                    messageItemItemClicks.subscribe { messageItemClicksSubject.onNext(it) }
                })
            }
            ViewType.ChatRoomMessageInitialItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(InitialMessageView(context).apply {

                })
            }
            ViewType.ChatRoomMessageJoinItemType.ordinal -> {
                ChatRoomMessageAdapterViewHolder(JoinMessageView(context).apply { })
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
                (holder.itemView as ChatRoomMessageView).bind(adapterItem.messageInfo)
            }
            is AdapterItem.ChatRoomMessageInitialItem -> {
                (holder.itemView as InitialMessageView).bind(adapterItem.messageInfo)
            }
            is AdapterItem.ChatRoomMessageJoinItem -> {
                (holder.itemView as JoinMessageView).bind(adapterItem.messageInfo)
            }
        }
    }

    private class ChatRoomMessageAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ChatRoomMessageItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomMessageItemType.ordinal)

        data class ChatRoomMessageInitialItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomMessageInitialItemType.ordinal)

        data class ChatRoomMessageJoinItem(var messageInfo: MessageInfo) :
            AdapterItem(ViewType.ChatRoomMessageJoinItemType.ordinal)

    }

    private enum class ViewType {
        ChatRoomMessageItemType,
        ChatRoomMessageInitialItemType,
        ChatRoomMessageJoinItemType
    }
}