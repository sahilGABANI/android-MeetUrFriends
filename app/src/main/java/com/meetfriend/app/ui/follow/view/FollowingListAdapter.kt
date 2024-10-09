package com.meetfriend.app.ui.follow.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowClickStates
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


class FollowingListAdapter(private val context: Context, val userId: Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val followingItemClicksSubject: PublishSubject<FollowClickStates> =
        PublishSubject.create()
    val followingItemClicks: Observable<FollowClickStates> = followingItemClicksSubject.hide()

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
            listItem.add(ListItem.FollowingItem(it))
        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.FollowingItemType.ordinal -> {
                FollowingAdapterViewHolder(FollowingListView(context).apply {
                    followingItemClicks.subscribe { followingItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.FollowingItem -> {
                (holder.itemView as FollowingListView).bind(listItem.followingList, userId)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class FollowingAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class FollowingItem(var followingList: MeetFriendUser) :
            ListItem(ViewType.FollowingItemType.ordinal)
    }

    private enum class ViewType {
        FollowingItemType
    }

}