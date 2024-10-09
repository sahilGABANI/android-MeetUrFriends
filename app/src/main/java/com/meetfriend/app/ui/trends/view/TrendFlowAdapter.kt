package com.meetfriend.app.ui.trends.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.challenge.AllChallengeFragment
import com.meetfriend.app.ui.myprofile.PostFragment
import com.meetfriend.app.ui.trends.CountryHashTagFragment
import com.meetfriend.app.ui.trends.HashtagListFragment
import com.meetfriend.app.ui.trends.HashtagShortsFragment
import com.meetfriend.app.ui.trends.UserListFragment
import com.meetfriend.app.utils.Constant.FiXED_1887_INT

class TrendFlowAdapter(fragmentActivity: FragmentActivity, private val searchString: String? = null) :
    FragmentStateAdapter(fragmentActivity) {

    companion object {
        private const val COUNTRY_HASH_TAG = 0
        private const val USER_LIST_SCREEN = 1
        private const val HASHTAG_LIST_SCREEN = 2
        private const val POSTS_HASHTAG_SCREEN = 3
        private const val SHORTS_HASHTAG_SCREEN = 4
        private const val SEARCH_TEXT_CHALLENGE = 5
        private const val SEARCH_TEXT_HASHTAGS_WITH_6 = 6
    }

    override fun getItemCount() = 7

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            COUNTRY_HASH_TAG -> {
                CountryHashTagFragment.newInstance(searchString)
            }
            USER_LIST_SCREEN -> {
                UserListFragment.newInstanceWithSearch(FiXED_1887_INT, searchString)
            }
            HASHTAG_LIST_SCREEN -> {
                HashtagListFragment.newInstance(searchString, 0)
            }
            POSTS_HASHTAG_SCREEN -> {
                PostFragment.getInstanceForHashTag(0, "", 1, searchString)
            }
            SHORTS_HASHTAG_SCREEN -> {
                HashtagShortsFragment.getInstanceForHashTag(0, "", 1, searchString)
            }
            SEARCH_TEXT_CHALLENGE -> {
                AllChallengeFragment.getInstanceForHashTag(0, "", 1, searchString)
            }
            SEARCH_TEXT_HASHTAGS_WITH_6 -> {
                HashtagListFragment.newInstance(searchString, 1)
            }
            else -> {
                PostFragment.getInstanceForHashTag(0, "", 1, searchString)
            }
        }
    }
}
