package com.meetfriend.app.ui.home.shorts.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.home.shorts.FollowingShortsFragment
import com.meetfriend.app.ui.home.shorts.ForYouShortsFragment

class ShortsFragmentTabAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    companion object {
        private const val FOR_YOU_SHORTS_POSITION = 0
        private const val FOLLOWING_SHORTS_POSITION = 1
        private const val ITEM_COUNT = 2
    }

    override fun getItemCount() = ITEM_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            FOR_YOU_SHORTS_POSITION -> {
                ForYouShortsFragment.newInstance()
            }
            FOLLOWING_SHORTS_POSITION -> {
                FollowingShortsFragment.newInstance()
            }
            else -> {
                ForYouShortsFragment.newInstance()
            }
        }
    }
}
