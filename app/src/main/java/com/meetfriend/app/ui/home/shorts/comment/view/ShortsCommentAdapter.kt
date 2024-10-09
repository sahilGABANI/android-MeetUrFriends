package com.meetfriend.app.ui.home.shorts.comment.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.post.model.ShortsCommentState
import com.meetfriend.app.responseclasses.video.Post_comments
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ShortsCommentAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val commentClicksSubject: PublishSubject<ShortsCommentState> = PublishSubject.create()
    val commentClicks: Observable<ShortsCommentState> = commentClicksSubject.hide()

    private val replyCommentClicksSubject: PublishSubject<ShortsCommentState> =
        PublishSubject.create()
    val replyCommentClicks: Observable<ShortsCommentState> = replyCommentClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<Post_comments>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.CommentItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CommentItemType.ordinal -> {
                ShortsCommentViewHolder(
                    ShortsCommentView(context).apply {
                        commentClicks.subscribe { commentClicksSubject.onNext(it) }
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
            is AdapterItem.CommentItem -> {
                (holder.itemView as ShortsCommentView).bind(adapterItem.postComments)
            }
        }
    }

    private class ShortsCommentViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class CommentItem(var postComments: Post_comments) :
            AdapterItem(ViewType.CommentItemType.ordinal)
    }

    private enum class ViewType {
        CommentItemType
    }
}
