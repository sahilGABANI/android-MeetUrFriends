package com.meetfriend.app.ui.myprofile.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.myprofile.LikesFragment
import com.meetfriend.app.ui.myprofile.PostFragment
import com.meetfriend.app.ui.myprofile.ShortFragment

class MyProfileTabAdapter(fragmentActivity: FragmentActivity, val userId: Int) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                PostFragment.getInstance(userId = userId)
            }
            1 -> {
                ShortFragment.getInstance(userId = userId)
            }
            2 -> {
                LikesFragment.getInstance(userId)
            }
            else -> {
                PostFragment.getInstance(userId = userId)
            }
        }
    }
}