package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReportAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val reportItemClickSubject: PublishSubject<String> = PublishSubject.create()
    val reportItemClick: Observable<String> = reportItemClickSubject.hide()


    private var adapterItems = listOf<AdapterItem>()

    var listOfReports: List<String>? = null
        set(listOfReports) {
            field = listOfReports
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfReports?.forEach {
            adapterItems.add(AdapterItem.ReportUserItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ReportUserItemType.ordinal -> {
                ReportUserViewHolder(ReportView(context).apply {
                    reportItemClick.subscribe { reportItemClickSubject.onNext(it) }
                })
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
            is AdapterItem.ReportUserItem -> {
                (holder.itemView as ReportView).bind(adapterItem.result)
            }
        }
    }

    private class ReportUserViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ReportUserItem(var result: String) :
            AdapterItem(ViewType.ReportUserItemType.ordinal)
    }

    private enum class ViewType {
        ReportUserItemType
    }
}