package com.meetfriend.app.ui.home.create.croppostimages.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CropPostImagesAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<String>? = null
        set(listOfDataItems) {
            field = listOfDataItems
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.CropPostImagesViewItem(it))
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CropPostImagesViewType.ordinal -> {
                CropPostImagesViewHolder(CropPostImagesView(context))
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.CropPostImagesViewItem -> {
                (holder.itemView as CropPostImagesView).bind(adapterItem.filePath)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class CropPostImagesViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class CropPostImagesViewItem(val filePath: String) :
            AdapterItem(ViewType.CropPostImagesViewType.ordinal)
    }

    private enum class ViewType {
        CropPostImagesViewType
    }
}