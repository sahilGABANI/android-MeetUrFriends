package com.meetfriend.app.ui.tag.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.post.model.TaggedUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class TaggedUserAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val userClicksSubject: PublishSubject<TaggedUser> = PublishSubject.create()
    val userClicks: Observable<TaggedUser> = userClicksSubject.hide()

    private var listItem = listOf<ListItem>()

    var listOfDataItems: List<TaggedUser>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()

        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<ListItem>()

        listOfDataItems?.forEach {
            listItem.add(ListItem.TaggedUserItem(it))

        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.TaggedUserItemType.ordinal -> {
                TaggedUserAdapterViewHolder(TaggedUserView(context).apply {
                    userClicks.subscribe { userClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.TaggedUserItem -> {
                (holder.itemView as TaggedUserView).bind(listItem.taggedUser)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class TaggedUserAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class TaggedUserItem(var taggedUser: TaggedUser) :
            ListItem(ViewType.TaggedUserItemType.ordinal)
    }

    private enum class ViewType {
        TaggedUserItemType
    }
}