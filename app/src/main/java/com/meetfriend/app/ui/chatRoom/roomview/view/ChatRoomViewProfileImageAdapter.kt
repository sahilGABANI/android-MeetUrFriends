package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.chat.model.TempMessageInfo

class ChatRoomViewProfileImageAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<TempMessageInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.ChatRoomViewProfileImageItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ChatRoomViewProfileImageItemType.ordinal -> {
                ChatRoomViewProfileImageAdapterViewHolder(ChatRoomViewProfileImageView(context).apply {
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
            is AdapterItem.ChatRoomViewProfileImageItem -> {
                (holder.itemView as ChatRoomViewProfileImageView).bind(adapterItem.chatRoomInfo)
            }
        }
    }

    private class ChatRoomViewProfileImageAdapterViewHolder(view: View) :
        RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ChatRoomViewProfileImageItem(var chatRoomInfo: TempMessageInfo) :
            AdapterItem(ViewType.ChatRoomViewProfileImageItemType.ordinal)
    }

    private enum class ViewType {
        ChatRoomViewProfileImageItemType
    }
}