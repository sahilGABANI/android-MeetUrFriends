package com.meetfriend.app.ui.main.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.post.model.PostMediaInformation
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PostMediaAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val playVideoSubject: PublishSubject<Unit> = PublishSubject.create()

    private val mediaPhotoViewClickSubject: PublishSubject<String> = PublishSubject.create()
    val mediaPhotoViewClick: Observable<String> = mediaPhotoViewClickSubject.hide()

    private val mediaVideoViewClickSubject: PublishSubject<String> = PublishSubject.create()
    val mediaVideoViewClick: Observable<String> = mediaVideoViewClickSubject.hide()

    var deviceHeight: Int? = null

    private var adapterItems = listOf<AdapterItem>()

    var postMediaType: Int = 1
    var listOfDataItems: List<PostMediaInformation>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.HomePagePostMediaViewItem(postMediaType, it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.HomePagePostMediaItemViewType.ordinal -> {
                HomePagePostMediaViewHolder(
                    HomePagePostMediaView(context).apply {
                        playVideo.subscribe { playVideoSubject.onNext(it) }
                        mediaPhotoViewClick.subscribe { mediaPhotoViewClickSubject.onNext(it) }
                        mediaVideoViewClick.subscribe { mediaVideoViewClickSubject.onNext(it) }
                    }
                )
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.HomePagePostMediaViewItem -> {
                (holder.itemView as HomePagePostMediaView)
                    .bind(postMediaType, adapterItem.data, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class HomePagePostMediaViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class HomePagePostMediaViewItem(val postMediaType: Int, val data: PostMediaInformation) :
            AdapterItem(ViewType.HomePagePostMediaItemViewType.ordinal)
    }

    private enum class ViewType {
        HomePagePostMediaItemViewType
    }
}
