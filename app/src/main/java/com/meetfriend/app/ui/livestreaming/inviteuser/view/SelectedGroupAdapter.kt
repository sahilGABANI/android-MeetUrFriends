package com.meetfriend.app.ui.livestreaming.inviteuser.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SelectedGroupAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val removeItemClickStateSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val removeItemClick: Observable<MeetFriendUser> = removeItemClickStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfUsers: List<MeetFriendUser>? = arrayListOf()
        set(listOfUsers) {
            field = listOfUsers
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfUsers?.forEach { user ->
            adapterItem.add(AdapterItem.SelectedGroupUserViewItem(user))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SelectedGroupUserViewItemType.ordinal -> {
                SelectedGroupViewHolder(
                    SelectedGroupView(context).apply {
                        removeItemClick.subscribe { removeItemClickStateSubject.onNext(it) }
                    }
                )
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SelectedGroupUserViewItem -> {
                (holder.itemView as SelectedGroupView).bind(adapterItem.followUser)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SelectedGroupViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SelectedGroupUserViewItem(
            val followUser: MeetFriendUser
        ) : AdapterItem(ViewType.SelectedGroupUserViewItemType.ordinal)
    }

    private enum class ViewType {
        SelectedGroupUserViewItemType
    }
}
