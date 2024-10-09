package com.meetfriend.app.ui.chatRoom.profile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.profile.model.ProfileItemInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PreviousProfileImageAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val previousProfileImageViewClicksSubject: PublishSubject<ProfileItemInfo> =
        PublishSubject.create()
    val previousProfileImageViewClicks: Observable<ProfileItemInfo> =
        previousProfileImageViewClicksSubject.hide()

    private val deleteProfileImageViewClicksSubject: PublishSubject<ProfileItemInfo> =
        PublishSubject.create()
    val deleteProfileImageViewClicks: Observable<ProfileItemInfo> =
        deleteProfileImageViewClicksSubject.hide()

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
            adapterItems.add(AdapterItem.PreviousProfileImageItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PreviousProfileImageItemType.ordinal -> {
                PreviousProfileImageAdapterViewHolder(
                    PreviousProfileImageView(context).apply {
                        previousProfileImageViewClicks.subscribe {
                            previousProfileImageViewClicksSubject.onNext(
                                it
                            )
                        }
                        deleteProfileImageViewClicks.subscribe {
                            deleteProfileImageViewClicksSubject.onNext(
                                it
                            )
                        }
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
            is AdapterItem.PreviousProfileImageItem -> {
                (holder.itemView as PreviousProfileImageView).bind(adapterItem.profileItemInfo)
            }
        }
    }

    private class PreviousProfileImageAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PreviousProfileImageItem(var profileItemInfo: ProfileItemInfo) :
            AdapterItem(ViewType.PreviousProfileImageItemType.ordinal)
    }

    private enum class ViewType {
        PreviousProfileImageItemType
    }
}
