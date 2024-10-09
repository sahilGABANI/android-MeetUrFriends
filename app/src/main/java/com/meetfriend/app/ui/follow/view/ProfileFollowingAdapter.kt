package com.meetfriend.app.ui.follow.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.follow.FollowersFragment
import com.meetfriend.app.ui.follow.FollowingFragment
import com.meetfriend.app.ui.follow.SuggestedFragment

class ProfileFollowingAdapter(fragmentActivity: FragmentActivity, private val userId: Int) :
    FragmentStateAdapter(fragmentActivity) {


    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                FollowingFragment.newInstance(userId)
            }
            1 -> {
                FollowersFragment.newInstance(userId)
            }
            2 -> {
                SuggestedFragment()
            }
            else -> {
                FollowingFragment.newInstance(userId)
            }
        }
    }
}