package com.meetfriend.app.ui.giftsGallery.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.gift.model.GiftsItemInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GiftItemAdapter(private val context: Context, private val isFromLive: Boolean) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val giftsItemClicksSubject: PublishSubject<GiftItemClickStates> =
        PublishSubject.create()
    val giftsItemClicks: Observable<GiftItemClickStates> = giftsItemClicksSubject.hide()

    private var listItem = listOf<ListItem>()

    var listOfDataItems: List<GiftsItemInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()

        }

    var selectedGiftInfo: GiftsItemInfo? = null
        set(selectedGiftInfo) {
            field = selectedGiftInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<ListItem>()

        listOfDataItems?.forEach {
            listItem.add(ListItem.GiftsItem(it, it == selectedGiftInfo))

        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.GiftsItemType.ordinal -> {
                GiftsAdapterViewHolder(GiftItemView(context, isFromLive).apply {
                    giftsItemClicks.subscribe { giftsItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.GiftsItem -> {
                (holder.itemView as GiftItemView).bind(listItem.giftsItemInfo, listItem.isSelected)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class GiftsAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class GiftsItem(var giftsItemInfo: GiftsItemInfo, var isSelected: Boolean) :
            ListItem(ViewType.GiftsItemType.ordinal)
    }

    private enum class ViewType {
        GiftsItemType
    }
}