package com.meetfriend.app.ui.location.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.places.model.ResultResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LocationAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val locationItemClickSubject: PublishSubject<ResultResponse> = PublishSubject.create()
    val locationItemClick: Observable<ResultResponse> = locationItemClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ResultResponse>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.LocationItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LocationItemType.ordinal -> {
                LocationAdapterViewHolder(
                    LocationView(context).apply {
                        locationItemClick.subscribe { locationItemClickSubject.onNext(it) }
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
            is AdapterItem.LocationItem -> {
                (holder.itemView as LocationView).bind(adapterItem.resultResponse)
            }
        }
    }

    private class LocationAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class LocationItem(var resultResponse: ResultResponse) :
            AdapterItem(ViewType.LocationItemType.ordinal)
    }

    private enum class ViewType {
        LocationItemType
    }
}
