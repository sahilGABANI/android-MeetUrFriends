package com.meetfriend.app.ui.mygifts.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.gift.model.GiftWeeklyInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GiftSummaryAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val giftItemClicksSubject: PublishSubject<GiftWeeklyInfo> =
        PublishSubject.create()

    private var listItem = listOf<ListItem>()

    var listOfDataItems: List<GiftWeeklyInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()

        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<ListItem>()

        listOfDataItems?.forEach {
            listItem.add(ListItem.GiftSummaryItem(it))

        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.GiftSummaryItemType.ordinal -> {
                GiftSummaryAdapterViewHolder(GiftSummaryView(context).apply {
                    giftItemClicks.subscribe { giftItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.GiftSummaryItem -> {
                (holder.itemView as GiftSummaryView).bind(listItem.giftWeeklyInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class GiftSummaryAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class GiftSummaryItem(var giftWeeklyInfo: GiftWeeklyInfo) :
            ListItem(ViewType.GiftSummaryItemType.ordinal)
    }

    private enum class ViewType {
        GiftSummaryItemType
    }
}