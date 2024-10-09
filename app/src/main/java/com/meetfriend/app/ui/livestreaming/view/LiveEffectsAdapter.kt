package com.meetfriend.app.ui.livestreaming.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.effect.model.EffectResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveEffectsAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    private val effectClicksSubject: PublishSubject<EffectResponse> = PublishSubject.create()
    val effectClicks: Observable<EffectResponse> = effectClicksSubject.hide()

    var listOfEffects: ArrayList<EffectResponse>? = null
        set(listOfEffects) {
            field = listOfEffects
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfEffects?.forEach {
            adapterItem.add(AdapterItem.EffectsDataItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.EffectsDataItemType.ordinal -> {
                EffectsAdapterViewHolder(
                    LiveEffectsView(context).apply {
                        effectClicks.subscribe { effectClicksSubject.onNext(it) }
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
            is AdapterItem.EffectsDataItem -> {
                (holder.itemView as LiveEffectsView).bind(adapterItem.effectResponse)
            }
        }
    }

    private class EffectsAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class EffectsDataItem(var effectResponse: EffectResponse) :
            AdapterItem(ViewType.EffectsDataItemType.ordinal)
    }

    private enum class ViewType {
        EffectsDataItemType
    }
}
