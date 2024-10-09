package com.meetfriend.app.ui.home.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.challenge.ChallengeFragment
import com.meetfriend.app.ui.fragments.MoreFragment
import com.meetfriend.app.ui.home.shorts.ShortsFragment

class HomeNavigationAdapter(
    fragmentActivity: FragmentActivity,
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                ShortsFragment.newInstance()

            }
            1 -> {
                ShortsFragment.newInstance()
            }

            3 -> {
                ChallengeFragment()
            }
            4 -> {
                MoreFragment()
            }

            else -> {
                ShortsFragment.newInstance()

            }
        }
    }
}