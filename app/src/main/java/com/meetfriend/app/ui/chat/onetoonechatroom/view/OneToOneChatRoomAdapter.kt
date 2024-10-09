package com.meetfriend.app.ui.chat.onetoonechatroom.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomListItemActionState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OneToOneChatRoomAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val chatRoomItemClickSubject: PublishSubject<ChatRoomListItemActionState> =
        PublishSubject.create()
    val chatRoomItemClick: Observable<ChatRoomListItemActionState> = chatRoomItemClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ChatRoomInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.OneToOneChatRoomItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OneToOneChatRoomItemType.ordinal -> {
                OneToOneChatRoomAdapterViewHolder(
                    OneToOneChatRoomView(context).apply {
                        chatRoomItemClick.subscribe { chatRoomItemClickSubject.onNext(it) }
                    }
                )
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
            is AdapterItem.OneToOneChatRoomItem -> {
                (holder.itemView as OneToOneChatRoomView).bind(adapterItem.chatRoomInfo)
            }
        }
    }

    private class OneToOneChatRoomAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class OneToOneChatRoomItem(var chatRoomInfo: ChatRoomInfo) :
            AdapterItem(ViewType.OneToOneChatRoomItemType.ordinal)
    }

    private enum class ViewType {
        OneToOneChatRoomItemType
    }
}
