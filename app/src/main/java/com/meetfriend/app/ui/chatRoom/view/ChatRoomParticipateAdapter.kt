package com.meetfriend.app.ui.chatRoom.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.chat.model.ChatRoomUser
import com.meetfriend.app.api.chat.model.TempChatRoomInfo
import io.reactivex.subjects.PublishSubject

class ChatRoomParticipateAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val chatRoomParticipateItemClickSubject: PublishSubject<TempChatRoomInfo> =
        PublishSubject.create()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ChatRoomUser>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.ChatRoomParticipateItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ChatRoomParticipateItemType.ordinal -> {
                ChatRoomParticipateAdapterViewHolder(ChatRoomParticipateView(context).apply {
                    chatRoomParticipateItemClick.subscribe {
                        chatRoomParticipateItemClickSubject.onNext(
                            it
                        )
                    }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ChatRoomParticipateItem -> {
                (holder.itemView as ChatRoomParticipateView).bind(adapterItem.tempMessageInfo)
            }
        }
    }

    private class ChatRoomParticipateAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ChatRoomParticipateItem(var tempMessageInfo: ChatRoomUser) :
            AdapterItem(ViewType.ChatRoomParticipateItemType.ordinal)
    }

    private enum class ViewType {
        ChatRoomParticipateItemType
    }
}