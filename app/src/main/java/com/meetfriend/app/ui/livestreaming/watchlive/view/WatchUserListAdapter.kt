package com.meetfriend.app.ui.livestreaming.watchlive.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.livestreaming.model.LiveJoinResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class WatchUserListAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val watchUsersViewClicksSubject: PublishSubject<LiveJoinResponse> =
        PublishSubject.create()
    val watchUsersViewClicks: Observable<LiveJoinResponse> = watchUsersViewClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<LiveJoinResponse>? = null
        set(liveJoinResponse) {
            field = liveJoinResponse
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.WatchUsersViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.WatchUsersViewItemType.ordinal -> {
                WatchUsersViewHolder(
                    WatchUserListView(context).apply {
                        watchUsersViewClicks.subscribe { watchUsersViewClicksSubject.onNext(it) }
                    }
                )
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
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
            is AdapterItem.WatchUsersViewItem -> {
                (holder.itemView as WatchUserListView).bind(adapterItem.liveJoinResponse)
            }
        }
    }

    private class WatchUsersViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class WatchUsersViewItem(val liveJoinResponse: LiveJoinResponse) :
            AdapterItem(ViewType.WatchUsersViewItemType.ordinal)
    }

    private enum class ViewType {
        WatchUsersViewItemType
    }
}
