package com.meetfriend.app.ui.follow.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowClickStates
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FollowersListAdapter(private val context: Context, private var userId: Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val followersItemClicksSubject: PublishSubject<FollowClickStates> =
        PublishSubject.create()
    val followersItemClicks: Observable<FollowClickStates> = followersItemClicksSubject.hide()

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
            listItem.add(ListItem.FollowersListItem(it))

        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.FollowersListItemType.ordinal -> {
                FollowersListAdapterViewHolder(FollowersListView(context).apply {
                    followersItemClicks.subscribe { followersItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.FollowersListItem -> {
                (holder.itemView as FollowersListView).bind(listItem.followingList, userId)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class FollowersListAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class FollowersListItem(var followingList: MeetFriendUser) :
            ListItem(ViewType.FollowersListItemType.ordinal)
    }

    private enum class ViewType {
        FollowersListItemType
    }

}