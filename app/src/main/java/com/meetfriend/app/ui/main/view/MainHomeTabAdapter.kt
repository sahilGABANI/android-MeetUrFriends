package com.meetfriend.app.ui.main.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.challenge.ChallengeFragment
import com.meetfriend.app.ui.fragments.MoreFragment
import com.meetfriend.app.ui.home.shorts.ShortsFragment
import com.meetfriend.app.ui.main.MainHomeFragment
import com.meetfriend.app.ui.messages.MessageListActivity
import com.meetfriend.app.ui.trends.TrendsFlowActivity

class MainHomeTabAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    companion object {
        private const val HOME_POSITION = 0
        private const val SHORTS_POSITION = 1
        private const val TRENDS_POSITION = 2
        private const val CHALLENGE_POSITION = 3
        private const val MESSAGE_POSITION = 4
        private const val MORE_POSITION = 5
    }
    override fun getItemCount() = 6

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            HOME_POSITION -> {
                MainHomeFragment.newInstance()
            }
            SHORTS_POSITION -> {
                ShortsFragment.newInstance()
            }
            TRENDS_POSITION -> {
                TrendsFlowActivity.newInstance()
            }
            CHALLENGE_POSITION -> {
                ChallengeFragment.newInstance()
            }
            MESSAGE_POSITION -> {
                MessageListActivity.newInstance()
            }
            MORE_POSITION -> {
                MoreFragment()
            }
            else -> {
                MainHomeFragment.newInstance()
            }
        }
    }
}
