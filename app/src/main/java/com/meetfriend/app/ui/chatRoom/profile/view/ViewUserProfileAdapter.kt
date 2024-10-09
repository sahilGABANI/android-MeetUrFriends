package com.meetfriend.app.ui.chatRoom.profile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.profile.model.ProfileItemInfo

class ViewUserProfileAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ProfileItemInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.ProfileImageItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ProfileImageItemType.ordinal -> {
                ProfileImageAdapterViewHolder(ViewUserprofileView(context).apply {
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
            is AdapterItem.ProfileImageItem -> {
                (holder.itemView as ViewUserprofileView).bind(adapterItem.profileItemInfo)
            }
        }
    }

    private class ProfileImageAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ProfileImageItem(var profileItemInfo: ProfileItemInfo) :
            AdapterItem(ViewType.ProfileImageItemType.ordinal)
    }

    private enum class ViewType {
        ProfileImageItemType
    }
}