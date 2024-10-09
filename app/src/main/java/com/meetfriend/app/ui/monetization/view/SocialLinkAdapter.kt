package com.meetfriend.app.ui.monetization.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.monetization.model.SocialLinkInfo
import com.meetfriend.app.api.monetization.model.TargetInfo
import com.meetfriend.app.ui.giftsGallery.view.GiftItemAdapter
import com.meetfriend.app.ui.giftsGallery.view.GiftItemView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SocialLinkAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val socialLinkClicksSubject: PublishSubject<SocialLinkInfo> =
        PublishSubject.create()
    val socialLinkClicks: Observable<SocialLinkInfo> = socialLinkClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: MutableList<SocialLinkInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.SocialLinkViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SocialLinkViewType.ordinal -> {
                SocialLinkViewHolder(SocialLinkView(context).apply {
                    socialLinkClicks.subscribe { socialLinkClicksSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
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
            is AdapterItem.SocialLinkViewItem -> {
                (holder.itemView as SocialLinkView).bind(adapterItem.data)
            }
        }
    }

    class SocialLinkViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SocialLinkViewItem(val data: SocialLinkInfo) : AdapterItem(ViewType.SocialLinkViewType.ordinal)
    }

    private enum class ViewType {
        SocialLinkViewType
    }
}
