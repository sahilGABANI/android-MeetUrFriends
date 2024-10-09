package com.meetfriend.app.ui.follow.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowClickStates
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SuggestedListAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val suggestedItemClicksSubject: PublishSubject<FollowClickStates> =
        PublishSubject.create()
    val suggestedItemClicks: Observable<FollowClickStates> = suggestedItemClicksSubject.hide()

    private var listItem = listOf<ListItem>()

    var listOfDataItems: List<MeetFriendUser>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()

        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<ListItem>()

        listOfDataItems?.forEach {
            listItem.add(ListItem.SuggestedListItem(it))

        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SuggestedListItemType.ordinal -> {
                SuggestedListAdapterViewHolder(SuggestedListView(context).apply {
                    suggestedItemClicks.subscribe { suggestedItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.SuggestedListItem -> {
                (holder.itemView as SuggestedListView).bind(listItem.userInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class SuggestedListAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class SuggestedListItem(var userInfo: MeetFriendUser) :
            ListItem(ViewType.SuggestedListItemType.ordinal)
    }

    private enum class ViewType {
        SuggestedListItemType
    }

}