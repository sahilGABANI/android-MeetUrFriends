package com.meetfriend.app.ui.main.story

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.storywork.models.HomePageStoryInfoState
import com.meetfriend.app.ui.storywork.models.ResultListResult
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class StoryAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val storyViewClickSubject: PublishSubject<HomePageStoryInfoState> = PublishSubject.create()
    val storyViewClick: Observable<HomePageStoryInfoState> = storyViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfStories: List<ResultListResult>? = null
        set(listOfStories) {
            field = listOfStories
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        adapterItems.add(AdapterItem.AddStoryViewItem("add"))
        listOfStories?.forEach {
            adapterItems.add(AdapterItem.StoryViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.AddStoryViewItemViewType.ordinal -> {
                StoryViewHolder(
                    StoryView(context).apply {
                        storyViewClick.subscribeAndObserveOnMainThread {
                            storyViewClickSubject.onNext(it)
                        }
                    }
                )
            }

            ViewType.StoryViewItemViewType.ordinal -> {
                StoryViewHolder(
                    StoryView(context).apply {
                        storyViewClick.subscribeAndObserveOnMainThread {
                            storyViewClickSubject.onNext(it)
                        }
                    }
                )
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.AddStoryViewItem -> {
                (holder.itemView as StoryView).bindAddInfo()
            }

            is AdapterItem.StoryViewItem -> {
                (holder.itemView as StoryView).bind(adapterItem.storyResponse)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class StoryViewItem(val storyResponse: ResultListResult) :
            AdapterItem(ViewType.StoryViewItemViewType.ordinal)

        data class AddStoryViewItem(val justMessage: String) :
            AdapterItem(ViewType.AddStoryViewItemViewType.ordinal)
    }

    private enum class ViewType {
        AddStoryViewItemViewType,
        StoryViewItemViewType
    }
}
