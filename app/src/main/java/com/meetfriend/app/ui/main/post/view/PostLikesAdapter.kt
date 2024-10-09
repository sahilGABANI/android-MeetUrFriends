package com.meetfriend.app.ui.main.post.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.post.model.PostLikesInformation
import io.reactivex.subjects.PublishSubject

class PostLikesAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val giftsItemClicksSubject: PublishSubject<GiftItemClickStates> =
        PublishSubject.create()

    private var listItem = listOf<ListItem>()

    var listOfDataItems: List<PostLikesInformation>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<ListItem>()

        listOfDataItems?.forEach {
            listItem.add(ListItem.PostLikesItem(it))
        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PostLikesItemType.ordinal -> {
                PostLikesAdapterViewHolder(
                    PostLikesView(context).apply {
                        giftsItemClicks.subscribe { giftsItemClicksSubject.onNext(it) }
                    }
                )
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.PostLikesItem -> {
                (holder.itemView as PostLikesView).bind(listItem.postLikesInformation)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class PostLikesAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class PostLikesItem(var postLikesInformation: PostLikesInformation) :
            ListItem(ViewType.PostLikesItemType.ordinal)
    }

    private enum class ViewType {
        PostLikesItemType
    }
}
