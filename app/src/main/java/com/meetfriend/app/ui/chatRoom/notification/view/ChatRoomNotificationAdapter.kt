package com.meetfriend.app.ui.chatRoom.notification.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.notification.model.NotificationActionState
import com.meetfriend.app.api.notification.model.NotificationItemInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ChatRoomNotificationAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val notificationStateSubject: PublishSubject<NotificationActionState> =
        PublishSubject.create()
    val notificationState: Observable<NotificationActionState> = notificationStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<NotificationItemInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.NotificationItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.NotificationItemType.ordinal -> {
                NotificationAdapterViewHolder(
                    ChatRoomNotificationView(context).apply {
                        notificationState.subscribe { notificationStateSubject.onNext(it) }
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
            is AdapterItem.NotificationItem -> {
                (holder.itemView as ChatRoomNotificationView).bind(adapterItem.notificationInfo)
            }
        }
    }

    private class NotificationAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class NotificationItem(var notificationInfo: NotificationItemInfo) :
            AdapterItem(ViewType.NotificationItemType.ordinal)
    }

    private enum class ViewType {
        NotificationItemType
    }
}
