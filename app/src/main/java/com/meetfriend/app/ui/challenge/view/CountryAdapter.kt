package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.challenge.model.CountryModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CountryAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val countryItemClickSubject: PublishSubject<CountryModel> = PublishSubject.create()
    val countryItemClick: Observable<CountryModel> = countryItemClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<CountryModel>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.CountryItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CountryItemType.ordinal -> {
                CountryAdapterViewHolder(
                    ViewCountryItem(context).apply {
                        countryItemClick.subscribe { countryItemClickSubject.onNext(it) }
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
            is AdapterItem.CountryItem -> {
                (holder.itemView as ViewCountryItem).bind(adapterItem.chatRoomInfo)
            }
        }
    }

    private class CountryAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class CountryItem(var chatRoomInfo: CountryModel) :
            AdapterItem(ViewType.CountryItemType.ordinal)
    }

    private enum class ViewType {
        CountryItemType
    }
}
