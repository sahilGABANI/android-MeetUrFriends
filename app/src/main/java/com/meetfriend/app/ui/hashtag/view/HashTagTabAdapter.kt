package com.meetfriend.app.ui.hashtag.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.challenge.AllChallengeFragment
import com.meetfriend.app.ui.home.shorts.ForYouShortsFragment
import com.meetfriend.app.ui.myprofile.PostFragment

class HashTagTabAdapter(fragmentActivity: FragmentActivity, private val hashTagId: Int, private val hashTagName: String) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                PostFragment.getInstanceForHashTag(hashTagId, hashTagName, 0)
            }
            1 -> {
                ForYouShortsFragment.getInstanceForHashTag(hashTagId, hashTagName, 0)
            }
            2 -> {
                AllChallengeFragment.getInstanceForHashTag(hashTagId, hashTagName, 0)
            }
            else -> {
                PostFragment.getInstanceForHashTag(hashTagId, hashTagName, 0)
            }
        }
    }
}