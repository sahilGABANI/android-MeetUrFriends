package com.meetfriend.app.ui.home.shorts.comment.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.post.model.ShortsCommentState
import com.meetfriend.app.responseclasses.video.Child_comments
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ShortsCommentReplyAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val replyCommentClicksSubject: PublishSubject<ShortsCommentState> =
        PublishSubject.create()
    val replyCommentClicks: Observable<ShortsCommentState> = replyCommentClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<Child_comments>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.CommentReplyItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CommentReplyItemType.ordinal -> {
                ShortsCommentReplyViewHolder(
                    ShortsCommentReplyView(context).apply {
                        replyCommentClicks.subscribe { replyCommentClicksSubject.onNext(it) }
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
            is AdapterItem.CommentReplyItem -> {
                (holder.itemView as ShortsCommentReplyView).bind(adapterItem.postComments)
            }
        }
    }

    private class ShortsCommentReplyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class CommentReplyItem(var postComments: Child_comments) :
            AdapterItem(ViewType.CommentReplyItemType.ordinal)
    }

    private enum class ViewType {
        CommentReplyItemType
    }
}
