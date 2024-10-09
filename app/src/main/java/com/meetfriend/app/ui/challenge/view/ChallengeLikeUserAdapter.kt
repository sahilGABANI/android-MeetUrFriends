package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.challenge.model.ChallengeUserModel

class ChallengeLikeUserAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var adapterItems = listOf<AdapterItem>()

    var listOfLikedUser: List<ChallengeUserModel>? = null
        set(listOfLikedUser) {
            field = listOfLikedUser
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfLikedUser?.forEach {
            adapterItems.add(AdapterItem.LikeUserItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LikeUserItemType.ordinal -> {
                LikedUsersViewHolder(ChallengeLikeUserView(context).apply {
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
            is AdapterItem.LikeUserItem -> {
                (holder.itemView as ChallengeLikeUserView).bind(adapterItem.challengeUser)
            }
        }
    }

    private class LikedUsersViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class LikeUserItem(var challengeUser: ChallengeUserModel) :
            AdapterItem(ViewType.LikeUserItemType.ordinal)
    }

    private enum class ViewType {
        LikeUserItemType
    }
}