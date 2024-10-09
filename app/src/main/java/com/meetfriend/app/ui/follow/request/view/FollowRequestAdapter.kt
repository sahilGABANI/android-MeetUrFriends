package com.meetfriend.app.ui.follow.request.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowRequestClickStates
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FollowRequestAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val userClicksSubject: PublishSubject<FollowRequestClickStates> =
        PublishSubject.create()
    val userClicks: Observable<FollowRequestClickStates> = userClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<MeetFriendUser>? = null
        set(listOfRequestInfo) {
            field = listOfRequestInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.FollowRequestItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.FollowRequestItemType.ordinal -> {
                FollowRequestViewHolder(FollowRequestView(context).apply {
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
            is AdapterItem.FollowRequestItem -> {
                (holder.itemView as FollowRequestView).bind(adapterItem.userInfo)
            }
        }
    }

    private class FollowRequestViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class FollowRequestItem(var userInfo: MeetFriendUser) :
            AdapterItem(ViewType.FollowRequestItemType.ordinal)
    }

    private enum class ViewType {
        FollowRequestItemType
    }
}