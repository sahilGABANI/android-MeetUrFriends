package com.meetfriend.app.ui.challenge

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.api.challenge.model.ChallengeType

class ChallengeFragmentTabAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                AllChallengeFragment.newInstance(ChallengeType.AllChallenge)
            }
            1 -> {
                AllChallengeFragment.newInstance(ChallengeType.MyChallenge)
            }
            2 -> {
                AllChallengeFragment.newInstance(ChallengeType.LiveChallenge)
            }
            3 -> {
                AllChallengeFragment.newInstance(ChallengeType.CompletedChallenge)
            }
            else -> {
                AllChallengeFragment.newInstance(ChallengeType.AllChallenge)
            }
        }
    }
}