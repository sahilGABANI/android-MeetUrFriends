package com.meetfriend.app.ui.myprofile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.responseclasses.photos.Data
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MediaAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val postViewClicksSubject: PublishSubject<Data> = PublishSubject.create()
    val postViewClicks: Observable<Data> = postViewClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfPosts: List<Data>? = null
        set(listOfPosts) {
            field = listOfPosts
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfPosts?.forEach {
            adapterItem.add(AdapterItem.MediaViewItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            ViewType.MediaViewItemType.ordinal -> {
                MediaAdapterViewHolder(MediaView(context).apply {
                    postViewClicks.subscribe {
                        postViewClicksSubject.onNext(it)
                    }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }


    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return

        when (adapterItem) {
            is AdapterItem.MediaViewItem -> {
                (holder.itemView as MediaView).bind(adapterItem.posts)
            }
        }
    }

    private class MediaAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class MediaViewItem(var posts: Data) : AdapterItem(ViewType.MediaViewItemType.ordinal)
    }

    private enum class ViewType {
        MediaViewItemType
    }
}