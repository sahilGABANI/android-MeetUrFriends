package com.meetfriend.app.ui.main.story

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meetfriend.app.ui.storywork.models.ResultListResult
import java.util.ArrayList

class StoryListAdapter(
    private val onClick: (Int) -> Unit,
    private val onBackPress: (Int) -> Unit,
    private val size: Int,
    private val list: ArrayList<ResultListResult>,
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = size

    override fun createFragment(position: Int): StoryListFragment = StoryListFragment.newInstance({
        onClick(it)
    }, {
        onBackPress(it)
    }, position, list[position])
}
