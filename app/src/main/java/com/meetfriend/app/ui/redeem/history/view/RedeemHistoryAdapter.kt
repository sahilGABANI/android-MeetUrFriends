package com.meetfriend.app.ui.redeem.history.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.redeem.model.RedeemHistoryInfo
import io.reactivex.subjects.PublishSubject

class RedeemHistoryAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val historyItemClicksSubject: PublishSubject<RedeemHistoryInfo> =
        PublishSubject.create()

    private var listItem = listOf<ListItem>()

    var listOfDataItems: List<RedeemHistoryInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()

        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<ListItem>()

        listOfDataItems?.forEach {
            listItem.add(ListItem.RedeemHistoryItem(it))

        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.RedeemHistoryItemType.ordinal -> {
                RedeemHistoryAdapterViewHolder(RedeemHistoryView(context).apply {
                    historyItemClicks.subscribe { historyItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.RedeemHistoryItem -> {
                (holder.itemView as RedeemHistoryView).bind(listItem.redeemHistoryInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class RedeemHistoryAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class RedeemHistoryItem(var redeemHistoryInfo: RedeemHistoryInfo) :
            ListItem(ViewType.RedeemHistoryItemType.ordinal)
    }

    private enum class ViewType {
        RedeemHistoryItemType
    }
}