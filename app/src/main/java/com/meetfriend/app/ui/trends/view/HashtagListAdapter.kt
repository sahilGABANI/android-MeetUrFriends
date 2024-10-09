package com.meetfriend.app.ui.trends.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.hashtag.model.HashtagResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HashtagListAdapter(private val context: Context, val userId: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val hashtagItemClicksSubject: PublishSubject<HashtagResponse> = PublishSubject.create()
    val hashtagItemClicks: Observable<HashtagResponse> = hashtagItemClicksSubject.hide()

    private var listItem = listOf<HashtagListItem>()

    var listOfDataItems: List<HashtagResponse>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<HashtagListItem>()

        listOfDataItems?.forEach {
            listItem.add(HashtagListItem.HashtagItem(it))
        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.HashtagItemType.ordinal -> {
                HashtagAdapterViewHolder(HashtagListView(context).apply {
                    hashtagItemClicks.subscribe { hashtagItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is HashtagListItem.HashtagItem -> {
                (holder.itemView as HashtagListView).bind(listItem.hashtagResponse)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class HashtagAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class HashtagListItem(val type: Int) {
        data class HashtagItem(var hashtagResponse: HashtagResponse) : HashtagListItem(ViewType.HashtagItemType.ordinal)
    }

    private enum class ViewType {
        HashtagItemType
    }

}