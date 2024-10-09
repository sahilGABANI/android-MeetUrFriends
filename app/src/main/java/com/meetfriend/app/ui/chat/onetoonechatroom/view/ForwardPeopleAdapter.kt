package com.meetfriend.app.ui.chat.onetoonechatroom.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ForwardPeopleAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val forwardPeopleClickSubject: PublishSubject<ChatRoomInfo> = PublishSubject.create()
    val forwardPeopleClick: Observable<ChatRoomInfo> = forwardPeopleClickSubject.hide()

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
                    ForwardPeopleListView(context).apply {
                        forwardPeopleClick.subscribe { forwardPeopleClickSubject.onNext(it) }
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
                (holder.itemView as ForwardPeopleListView).bind(adapterItem.chatRoomInfo)
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
