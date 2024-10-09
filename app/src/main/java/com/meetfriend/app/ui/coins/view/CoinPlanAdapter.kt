package com.meetfriend.app.ui.coins.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.gift.model.CoinPlanInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CoinPlanAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val coinPlanItemClicksSubject: PublishSubject<CoinPlanInfo> = PublishSubject.create()
    val coinPlanItemClicks: Observable<CoinPlanInfo> = coinPlanItemClicksSubject.hide()

    private var listItem = listOf<ListItem>()

    var listOfDataItems: List<CoinPlanInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()

        }

    var selectedPlanInfo: CoinPlanInfo? = null
        set(selectedPlanInfo) {
            field = selectedPlanInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<ListItem>()

        listOfDataItems?.forEach {
            listItem.add(ListItem.CoinPlanItem(it, it == selectedPlanInfo))

        }

        this.listItem = listItem
        notifyDataSetChanged()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CoinPlanItemType.ordinal -> {
                CoinPlanAdapterViewHolder(CoinPlanView(context).apply {
                    planItemClicks.subscribe { coinPlanItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.CoinPlanItem -> {
                (holder.itemView as CoinPlanView).bind(listItem.coinPlanInfo, listItem.isSelected)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class CoinPlanAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class CoinPlanItem(var coinPlanInfo: CoinPlanInfo, var isSelected: Boolean) :
            ListItem(ViewType.CoinPlanItemType.ordinal)
    }

    private enum class ViewType {
        CoinPlanItemType
    }
}