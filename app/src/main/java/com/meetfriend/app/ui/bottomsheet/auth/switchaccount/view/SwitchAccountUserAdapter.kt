package com.meetfriend.app.ui.bottomsheet.auth.switchaccount.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SwitchAccountUserAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val switchUserClicksSubject: PublishSubject<MeetFriendUser> =
        PublishSubject.create()
    val switchUserItemClicks: Observable<MeetFriendUser> = switchUserClicksSubject.hide()

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
            listItem.add(ListItem.SwitchUserItem(it))
        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SwitchUserItemType.ordinal -> {
                SwitchUserAdapterViewHolder(
                    SwitchUserView(context).apply {
                        switchUserItemClicks.subscribe { switchUserClicksSubject.onNext(it) }
                    }
                )
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.SwitchUserItem -> {
                (holder.itemView as SwitchUserView).bind(listItem.postLikesInformation)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class SwitchUserAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class SwitchUserItem(var postLikesInformation: MeetFriendUser) :
            ListItem(ViewType.SwitchUserItemType.ordinal)
    }

    private enum class ViewType {
        SwitchUserItemType
    }
}
