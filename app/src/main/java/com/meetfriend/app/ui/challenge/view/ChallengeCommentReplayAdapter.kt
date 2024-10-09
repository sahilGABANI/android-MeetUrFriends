package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.challenge.model.ChildCommentItem
import com.meetfriend.app.api.post.model.ChallengeCommentReplyState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ChallengeCommentReplayAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val commentItemClickSubject: PublishSubject<ChallengeCommentReplyState> =
        PublishSubject.create()
    val commentItemClick: Observable<ChallengeCommentReplyState> = commentItemClickSubject.hide()


    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ChildCommentItem>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.ChallengeCommentItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ChallengeCommentItemType.ordinal -> {
                ChallengeCommentViewHolder(ChallengeCommentReplayView(context).apply {
                    commentItemClick.subscribe { commentItemClickSubject.onNext(it) }
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
            is AdapterItem.ChallengeCommentItem -> {
                (holder.itemView as ChallengeCommentReplayView).bind(adapterItem.ChatRoomInfo)
            }
        }
    }

    private class ChallengeCommentViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ChallengeCommentItem(var ChatRoomInfo: ChildCommentItem) :
            AdapterItem(ViewType.ChallengeCommentItemType.ordinal)
    }

    private enum class ViewType {
        ChallengeCommentItemType
    }
}