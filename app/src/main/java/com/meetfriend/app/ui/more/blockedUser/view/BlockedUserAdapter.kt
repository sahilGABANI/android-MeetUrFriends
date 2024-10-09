package com.meetfriend.app.ui.more.blockedUser.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.follow.model.BlockInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BlockedUserAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val userClicksSubject: PublishSubject<BlockInfo> = PublishSubject.create()
    val userClicks: Observable<BlockInfo> = userClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<BlockInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.BlockedUserItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.BlockedUserItemType.ordinal -> {
                BlockedUserAdapterViewHolder(BlockedUserView(context).apply {
                    userClicks.subscribe { userClicksSubject.onNext(it) }
                })
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
            is AdapterItem.BlockedUserItem -> {
                (holder.itemView as BlockedUserView).bind(adapterItem.blockInfo)
            }
        }
    }

    private class BlockedUserAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class BlockedUserItem(var blockInfo: BlockInfo) :
            AdapterItem(ViewType.BlockedUserItemType.ordinal)
    }

    private enum class ViewType {
        BlockedUserItemType
    }
}