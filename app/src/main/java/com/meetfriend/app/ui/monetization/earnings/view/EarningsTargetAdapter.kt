package com.meetfriend.app.ui.monetization.earnings.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.monetization.model.TargetInfo

class EarningsTargetAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: MutableList<TargetInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.EarningViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.EarningItemViewType.ordinal -> {
                val earningItemView = EarningTargetItemView(context)
                EarningItemViewHolder(earningItemView)
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.EarningViewItem -> {
                (holder.itemView as EarningTargetItemView).bind(adapterItem.data)
            }
        }
    }

    class EarningItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class EarningViewItem(val data: TargetInfo) : AdapterItem(ViewType.EarningItemViewType.ordinal)
    }

    private enum class ViewType {
        EarningItemViewType
    }
}
