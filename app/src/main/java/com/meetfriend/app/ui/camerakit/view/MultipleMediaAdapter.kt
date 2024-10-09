package com.meetfriend.app.ui.camerakit.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.post.model.MultipleImageDetails
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MultipleMediaAdapter(private val context: Context, private val preview: Boolean) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val chatRoomItemClickSubject: PublishSubject<MultipleImageDetails> =
        PublishSubject.create()
    val chatRoomItemClick: Observable<MultipleImageDetails> = chatRoomItemClickSubject.hide()

    private val addMediaClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val addMediaClick: Observable<Unit> = addMediaClickSubject.hide()

    private val closeClickSubject: PublishSubject<MultipleImageDetails> =
        PublishSubject.create()
    val closeClick: Observable<MultipleImageDetails> = closeClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<MultipleImageDetails>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.MultipleMediaItem(it))
        }
        if (preview) {
            adapterItems.add(AdapterItem.AddMediaItem) // Add the "+" button item
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.MultipleMediaItemType.ordinal -> {
                MultipleMediaAdapterViewHolder(
                    MultipleMediaView(context).apply {
                        chatRoomItemClick.subscribe { chatRoomItemClickSubject.onNext(it) }
                        closeClick.subscribe { closeClickSubject.onNext(it) }
                    }
                )
            }
            ViewType.AddMediaItemType.ordinal -> {
                AddMediaAdapterViewHolder(
                    AddMeadiaViewHolder(context).apply {
                        addMediaClick.subscribe { addMediaClickSubject.onNext(it) }
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
        var adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.MultipleMediaItem -> {
                (holder.itemView as MultipleMediaView).bind(adapterItem.multipleImageDetails)
            }

            is AdapterItem.AddMediaItem -> {
                (holder.itemView as AddMeadiaViewHolder).bind()
            }
        }
    }
    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class MultipleMediaAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)
    inner class AddMediaAdapterViewHolder(private val view: AddMeadiaViewHolder) : RecyclerView.ViewHolder(view) {
        fun bind() {
            view.bind()
            view.addMediaClick.subscribe {
                addMediaClickSubject.onNext(Unit)
            }
        }
    }

    sealed class AdapterItem(val type: Int) {
        data class MultipleMediaItem(var multipleImageDetails: MultipleImageDetails) :
            AdapterItem(ViewType.MultipleMediaItemType.ordinal)

        object AddMediaItem : AdapterItem(ViewType.AddMediaItemType.ordinal)
    }

    private enum class ViewType {
        MultipleMediaItemType,
        AddMediaItemType
    }
}
