package com.meetfriend.app.ui.livestreaming.game.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.follow.model.FollowClickStates
import com.meetfriend.app.api.livestreaming.model.TopGifter
import io.reactivex.subjects.PublishSubject

class GameTopContributorAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val contributorClicksSubject: PublishSubject<FollowClickStates> =
        PublishSubject.create()
    private var listItem = listOf<ListItem>()

    var listOfDataItems: List<TopGifter>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<ListItem>()

        listOfDataItems?.forEach {
            listItem.add(ListItem.ContributorListItem(it))
        }

        this.listItem = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ContributorListItemType.ordinal -> {
                ContributorListAdapterViewHolder(
                    GameTopContributorView(context).apply {
                        contributorClicks.subscribe { contributorClicksSubject.onNext(it) }
                    }
                )
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = listItem.getOrNull(position) ?: return
        when (listItem) {
            is ListItem.ContributorListItem -> {
                (holder.itemView as GameTopContributorView).bind(listItem.topGifterInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    override fun getItemViewType(position: Int): Int {
        return listItem[position].type
    }

    private class ContributorListAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class ListItem(val type: Int) {
        data class ContributorListItem(var topGifterInfo: TopGifter) :
            ListItem(ViewType.ContributorListItemType.ordinal)
    }

    private enum class ViewType {
        ContributorListItemType
    }
}
