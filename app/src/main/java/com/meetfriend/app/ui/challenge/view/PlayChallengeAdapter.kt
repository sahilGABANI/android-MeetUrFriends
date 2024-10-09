package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.challenge.model.ChallengeItem
import com.meetfriend.app.api.challenge.model.ChallengeType
import com.meetfriend.app.api.post.model.ChallengePageState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PlayChallengeAdapter(private val context: Context, private val challengeType: ChallengeType) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val playShortsViewClicksSubject: PublishSubject<ChallengePageState> =
        PublishSubject.create()
    val playShortsViewClicks: Observable<ChallengePageState> = playShortsViewClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ChallengeItem>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.PlayShortsViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PlayShortsViewItemType.ordinal -> {
                PlayReelAdapterViewHolder(
                    PlayChallengeView(context).apply {
                        playShortsViewClicks.subscribe { playShortsViewClicksSubject.onNext(it) }
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
            is AdapterItem.PlayShortsViewItem -> {
                (holder.itemView as PlayChallengeView).bind(adapterItem.dataVideo, challengeType)
            }
        }
    }

    private class PlayReelAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PlayShortsViewItem(var dataVideo: ChallengeItem?) :
            AdapterItem(ViewType.PlayShortsViewItemType.ordinal)
    }

    private enum class ViewType {
        PlayShortsViewItemType
    }
}
