package com.meetfriend.app.ui.chatRoom.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.chat.model.TempChatRoomInfo
import com.meetfriend.app.api.chat.model.TempMessageInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ChatRoomAdminAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val chatRoomAdminItemClickSubject: PublishSubject<TempChatRoomInfo> =
        PublishSubject.create()

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
            adapterItems.add(AdapterItem.ChatRoomAdminItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ChatRoomAdminItemType.ordinal -> {
                ChatRoomAdminAdapterViewHolder(ChatRoomAdminView(context).apply {
                    chatRoomAdminItemClick.subscribe { chatRoomAdminItemClickSubject.onNext(it) }
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
            is AdapterItem.ChatRoomAdminItem -> {
                (holder.itemView as ChatRoomAdminView).bind(adapterItem.tempMessageInfo)
            }
        }
    }

    private class ChatRoomAdminAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ChatRoomAdminItem(var tempMessageInfo: TempMessageInfo) :
            AdapterItem(ViewType.ChatRoomAdminItemType.ordinal)
    }

    private enum class ViewType {
        ChatRoomAdminItemType
    }
}