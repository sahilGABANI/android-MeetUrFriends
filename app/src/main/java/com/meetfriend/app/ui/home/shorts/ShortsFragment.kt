package com.meetfriend.app.ui.home.shorts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentShortsBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.ui.activities.WelcomeActivity
import com.meetfriend.app.ui.home.shorts.view.ShortsFragmentTabAdapter
import com.meetfriend.app.utils.Constant
import org.json.JSONObject
import javax.inject.Inject
import kotlin.properties.Delegates

class ShortsFragment : BasicFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = ShortsFragment()
        private const val SHORTS_OFFSCREEN = 2
    }

    private var _binding: FragmentShortsBinding? = null
    private val binding get() = _binding!!

    private var loggedInUserId by Delegates.notNull<Int>()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var shortsFragmentTabAdapter: ShortsFragmentTabAdapter
    private var count = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShortsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)

        loggedInUserId = loggedInUserCache.getLoggedInUserId()
        listenToViewEvents()
    }

    private fun openLoginPopup() {
        val loginBottomSheet = WelcomeActivity.newInstance()
        loginBottomSheet.show(childFragmentManager, WelcomeActivity::class.java.name)
    }
    private fun listenToViewEvents() {
        shortsFragmentTabAdapter = ShortsFragmentTabAdapter(
            requireActivity()
        )
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = SHORTS_OFFSCREEN.toInt()
        binding.viewPager.adapter = shortsFragmentTabAdapter
        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.tabLayout.invalidate()
                val isLogin = loggedInUserCache.getLoginUserToken() != null

                if (tab.text == resources.getString(R.string.label_for_you)) {
                    RxBus.publish(RxEvent.RefreshFoYouPlayFragment)
                    binding.viewPager.setCurrentItem(0, false)
                } else if (tab.text == resources.getString(R.string.label_following)) {
                    if (isLogin) {
                        RxBus.publish(RxEvent.RefreshFollowingPlayFragment)
                        binding.viewPager.setCurrentItem(1, false)
                    } else {
                        openLoginPopup()
                        binding.tabLayout.getTabAt(0)?.select()
                        binding.viewPager.setCurrentItem(0, false)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                return
            }
            override fun onTabReselected(tab: TabLayout.Tab) {
                return
            }
        })
    }

    override fun onResume() {
        super.onResume()

        count += 1

        if (isResumed) {
            RxBus.publish(RxEvent.PlayVideo(true, count))
            mp?.timeEvent(Constant.SCREEN_TIME)
        }
        RxBus.publish(RxEvent.RefreshPlayerView)
    }

    override fun onPause() {
        super.onPause()
        RxBus.publish(RxEvent.PlayVideo(false, count))
    }

    override fun onStop() {
        super.onStop()
        val props = JSONObject()
        props.put(Constant.SCREEN_TYPE, "shorts")

        mp?.track(Constant.SCREEN_TIME, props)
    }
}
