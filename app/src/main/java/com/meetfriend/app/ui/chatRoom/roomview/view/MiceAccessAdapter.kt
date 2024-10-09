package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.chat.model.MiceAccessInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MiceAccessAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()
    private val removeMiceAccessClicksSubject: PublishSubject<MiceAccessInfo> =
        PublishSubject.create()
    val removeMiceAccessClicks: Observable<MiceAccessInfo> = removeMiceAccessClicksSubject.hide()

    var listOfDataItems: List<MiceAccessInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.MiceAccessUserItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.MiceAccessUserItemType.ordinal -> {
                MiceAccessAdapterViewHolder(MiceAccessView(context).apply {
                    removeMiceAccessClicks.subscribe { removeMiceAccessClicksSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.MiceAccessUserItem -> {
                (holder.itemView as MiceAccessView).bind(adapterItem.miceAccessInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class MiceAccessAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class MiceAccessUserItem(var miceAccessInfo: MiceAccessInfo) :
            AdapterItem(ViewType.MiceAccessUserItemType.ordinal)
    }

    private enum class ViewType {
        MiceAccessUserItemType
    }
}
