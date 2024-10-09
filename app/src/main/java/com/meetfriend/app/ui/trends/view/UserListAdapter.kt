package com.meetfriend.app.ui.trends.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UserListAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val hashtagItemClicksSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val hashtagItemClicks: Observable<MeetFriendUser> = hashtagItemClicksSubject.hide()

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
                FollowingAdapterViewHolder(UserListView(context).apply {
                    hashtagItemClicks.subscribe { hashtagItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.FollowingItem -> {
                (holder.itemView as UserListView).bind(listItem.followingList)
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