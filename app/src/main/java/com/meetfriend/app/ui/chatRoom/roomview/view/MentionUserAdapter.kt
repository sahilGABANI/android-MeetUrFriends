package com.meetfriend.app.ui.chatRoom.roomview.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.post.model.HashTagsResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MentionUserAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val userClicksSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val userClicks: Observable<MeetFriendUser> = userClicksSubject.hide()

    private val hashTagClicksSubject: PublishSubject<HashTagsResponse> = PublishSubject.create()
    val hashTagClicks: Observable<HashTagsResponse> = hashTagClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<MeetFriendUser>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    var listOfHashTags: List<HashTagsResponse>? = null
        set(listOfHashTags) {
            field = listOfHashTags
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.MentionUserItem(it))
        }

        listOfHashTags?.forEach {
            adapterItems.add(AdapterItem.HashTagDataItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.MentionUserItemType.ordinal -> {
                ChatRoomAdapterViewHolder(
                    MentionUserView(context).apply {
                        userClicks.subscribe { userClicksSubject.onNext(it) }
                        hashTagClicks.subscribe { hashTagClicksSubject.onNext(it) }
                    }
                )
            }
            ViewType.HashTagDataItemType.ordinal -> {
                ChatRoomAdapterViewHolder(
                    MentionUserView(context).apply {
                        userClicks.subscribe { userClicksSubject.onNext(it) }
                        hashTagClicks.subscribe {
                            hashTagClicksSubject.onNext(it)
                        }
                    }
                )
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
            is AdapterItem.MentionUserItem -> {
                (holder.itemView as MentionUserView).bind(adapterItem.mentionUserInfo)
            }
            is AdapterItem.HashTagDataItem -> {
                (holder.itemView as MentionUserView).bindHashTag(adapterItem.mentionUserInfo)
            }
        }
    }

    private class ChatRoomAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class MentionUserItem(var mentionUserInfo: MeetFriendUser) : AdapterItem(
            ViewType.MentionUserItemType.ordinal
        )
        data class HashTagDataItem(var mentionUserInfo: HashTagsResponse) : AdapterItem(
            ViewType.HashTagDataItemType.ordinal
        )
    }

    private enum class ViewType {
        MentionUserItemType,
        HashTagDataItemType
    }
}
