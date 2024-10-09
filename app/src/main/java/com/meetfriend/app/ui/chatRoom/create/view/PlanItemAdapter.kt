package com.meetfriend.app.ui.chatRoom.create.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.subscription.model.TempPlanInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PlanItemAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val planItemClickSubject: PublishSubject<TempPlanInfo> = PublishSubject.create()
    val planItemClicks: Observable<TempPlanInfo> = planItemClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<TempPlanInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }
    var selectedRoomType: String = "Primary Room"
        set(selectedRoomType) {
            field = selectedRoomType
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.PlanItem(it, it.roomType == selectedRoomType))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PlanItemType.ordinal -> {
                PlanAdapterViewHolder(
                    PlanItemView(context).apply {
                        planItemClicks.subscribe { planItemClickSubject.onNext(it) }
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
            is AdapterItem.PlanItem -> {
                (holder.itemView as PlanItemView).bind(
                    adapterItem.tempPlanInfo,
                    adapterItem.isSelected
                )
            }
        }
    }

    private class PlanAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PlanItem(var tempPlanInfo: TempPlanInfo, var isSelected: Boolean) :
            AdapterItem(ViewType.PlanItemType.ordinal)
    }

    private enum class ViewType {
        PlanItemType
    }
}
