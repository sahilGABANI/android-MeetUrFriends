package com.meetfriend.app.ui.livestreaming.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.livestreaming.model.LiveEventSendOrReadComment
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveStreamingCommentAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    private val commentClicksSubject: PublishSubject<LiveEventSendOrReadComment> =
        PublishSubject.create()
    val commentClicks: Observable<LiveEventSendOrReadComment> = commentClicksSubject.hide()

    var listOfDataItems: List<LiveEventSendOrReadComment>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItem.add(AdapterItem.LiveStreamingCommentItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LiveStreamingCommentItemType.ordinal -> {
                ChatRoomAdapterViewHolder(
                    LiveStreamingCommentView(context).apply {
                        commentClicks.subscribe { commentClicksSubject.onNext(it) }
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
            is AdapterItem.LiveStreamingCommentItem -> {
                (holder.itemView as LiveStreamingCommentView).bind(adapterItem.liveEventSendOrReadComment)
            }
        }
    }

    private class ChatRoomAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class LiveStreamingCommentItem(var liveEventSendOrReadComment: LiveEventSendOrReadComment) :
            AdapterItem(ViewType.LiveStreamingCommentItemType.ordinal)
    }

    private enum class ViewType {
        LiveStreamingCommentItemType
    }
}
