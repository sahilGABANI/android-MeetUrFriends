package com.meetfriend.app.ui.livestreaming.inviteuser.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.giftsGallery.SendGiftCoinBottomsheet
import com.meetfriend.app.ui.livestreaming.LiveStreamingMoreOptionBottomSheet
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class InviteUserInLiveStreamingAdapter(
    private val context: Context,
    private val isOpenCreate: Boolean,
    private val isAllowPlayGame: Int,
    private val isOpenFrom: String,
    private val totalCoins: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inviteUpdatedSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val inviteUpdated: Observable<MeetFriendUser> = inviteUpdatedSubject.hide()

    private val createChatViewClicksSubject: PublishSubject<MeetFriendUser> =
        PublishSubject.create()
    val createChatViewClicks: Observable<MeetFriendUser> = createChatViewClicksSubject.hide()

    private val userClicksSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val userClicks: Observable<MeetFriendUser> = userClicksSubject.hide()

    private val handleNavSubject: PublishSubject<Unit> = PublishSubject.create()
    val onBackViewClick: Observable<Unit> = handleNavSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<MeetFriendUser>? = null
        set(listOfFollowResponse) {
            field = listOfFollowResponse
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.InviteUserItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.InviteUserItemType.ordinal -> {
                ChatRoomAdapterViewHolder(
                    InviteUserInLiveStreamingView(
                        context,
                        isOpenCreate,
                        isAllowPlayGame,
                        isOpenFrom
                    ).apply {
                        inviteButtonViewClicks.subscribe {
                            inviteUpdatedSubject.onNext(it)
                        }
                        invitedButtonViewClicks.subscribe {
                        }
                        createChatViewClicks.subscribe {
                            if (isOpenFrom == "sendCoin") {
                                val bottomsheet = SendGiftCoinBottomsheet(totalCoins, it.id.toString())
                                bottomsheet.onClick.subscribeAndObserveOnMainThread {
                                    handleNavSubject.onNext(Unit)
                                }
                                bottomsheet.show(
                                    (context as AppCompatActivity).supportFragmentManager,
                                    LiveStreamingMoreOptionBottomSheet::class.java.name
                                )
                            } else if (isOpenFrom != "WhoCanWatch"){
                                createChatViewClicksSubject.onNext(it)
                            }
                        }

                        userClicks.subscribe {
                            userClicksSubject.onNext(it)
                        }
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
            is AdapterItem.InviteUserItem -> {
                (holder.itemView as InviteUserInLiveStreamingView).bind(
                    adapterItem.mentionUserInfo
                )
            }
        }
    }

    private class ChatRoomAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class InviteUserItem(var mentionUserInfo: MeetFriendUser) :
            AdapterItem(ViewType.InviteUserItemType.ordinal)
    }

    private enum class ViewType {
        InviteUserItemType
    }

}
