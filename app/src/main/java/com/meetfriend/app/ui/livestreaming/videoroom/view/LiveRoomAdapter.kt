package com.meetfriend.app.ui.livestreaming.videoroom.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveRoomAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val roomClickSubject: PublishSubject<LiveEventInfo> = PublishSubject.create()
    val roomClicks: Observable<LiveEventInfo> = roomClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<LiveEventInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.LiveRoomItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LiveRoomItemType.ordinal -> {
                ChatRoomAdapterViewHolder(
                    LiveRoomView(context).apply {
                        roomClicks.subscribe { roomClickSubject.onNext(it) }
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
            is AdapterItem.LiveRoomItem -> {
                (holder.itemView as LiveRoomView).bind(adapterItem.liveEventInfo)
            }
        }
    }

    private class ChatRoomAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class LiveRoomItem(var liveEventInfo: LiveEventInfo) :
            AdapterItem(ViewType.LiveRoomItemType.ordinal)
    }

    private enum class ViewType {
        LiveRoomItemType
    }
}
