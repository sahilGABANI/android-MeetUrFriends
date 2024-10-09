package com.meetfriend.app.ui.music.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AddMusicAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val musicClickSubject: PublishSubject<MusicInfo> = PublishSubject.create()
    val musicClick: Observable<MusicInfo> = musicClickSubject.hide()

    private val selectMusicClickSubject: PublishSubject<MusicInfo> = PublishSubject.create()
    val selectMusicClick: Observable<MusicInfo> = selectMusicClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfMusicInfo: List<MusicInfo>? = null
        set(listOfMusicInfo) {
            field = listOfMusicInfo
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfMusicInfo?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.MusicTypeViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.MusicViewItemType.ordinal -> {
                MusicInfoViewHolder(AddMusicView(context).apply {
                    musicClick.subscribeAndObserveOnMainThread {
                        musicClickSubject.onNext(it)
                    }

                    selectMusicClick.subscribeAndObserveOnMainThread {
                        selectMusicClickSubject.onNext(it)
                    }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.MusicTypeViewItem -> {
                (holder.itemView as AddMusicView).bind(adapterItem.musicResponse)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class MusicInfoViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class MusicTypeViewItem(val musicResponse: MusicInfo) :
            AdapterItem(ViewType.MusicViewItemType.ordinal)
    }

    private enum class ViewType {
        MusicViewItemType
    }
}