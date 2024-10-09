package com.meetfriend.app.ui.mygifts.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.gift.model.GiftTransaction
import io.reactivex.subjects.PublishSubject

class GiftTransactionAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val historyItemClicksSubject: PublishSubject<GiftTransaction> =
        PublishSubject.create()

    private var listItem = listOf<ListItem>()

    var listOfDataItems: List<GiftTransaction>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()

        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<ListItem>()

        listOfDataItems?.forEach {
            listItem.add(ListItem.GiftHistoryItem(it))

        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.GiftHistoryItemType.ordinal -> {
                GiftHistoryAdapterViewHolder(GiftTransactionView(context).apply {
                    historyItemClicks.subscribe { historyItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.GiftHistoryItem -> {
                (holder.itemView as GiftTransactionView).bind(listItem.giftTransactionInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class GiftHistoryAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class GiftHistoryItem(var giftTransactionInfo: GiftTransaction) :
            ListItem(ViewType.GiftHistoryItemType.ordinal)
    }

    private enum class ViewType {
        GiftHistoryItemType
    }
}