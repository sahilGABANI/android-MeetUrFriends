package com.meetfriend.app.ui.mygifts.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.mygifts.SentGiftsFragment
import com.meetfriend.app.ui.mygifts.ReceivedGiftFragment
import com.meetfriend.app.ui.mygifts.GiftWeeklySummaryFragment

class GiftTransactionTabAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                ReceivedGiftFragment.newInstance()
            }
            1 -> {
                SentGiftsFragment.newInstance()
            }
            2 -> {
                GiftWeeklySummaryFragment.newInstance()
            }
            else -> {
                ReceivedGiftFragment.newInstance()
            }
        }
    }
}