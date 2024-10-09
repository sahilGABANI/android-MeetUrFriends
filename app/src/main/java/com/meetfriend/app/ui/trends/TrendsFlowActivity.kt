package com.meetfriend.app.ui.trends

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.hbb20.CountryCodePicker
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.databinding.ActivityTrendsFlowBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.trends.view.TrendFlowAdapter
import com.meetfriend.app.utils.Constant.FIX_400_MILLISECOND
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TrendsFlowActivity : BasicFragment() {

    private var _binding: ActivityTrendsFlowBinding? = null
    val binding get() = _binding!!
    private lateinit var trendFlowAdapter: TrendFlowAdapter
    private var count = 0

    companion object {
        private const val COUNTRY_POSITION = 0
        private const val USER_POSITION = 1
        private const val HASHTAG_POSITION = 2
        private const val POST_POSITION = 3
        private const val SHORTS_POSITION = 4
        private const val CHALLENGE_POSITION = 5
        private const val SEARCH_POSITION = 6
        private const val OFF_SCREEN_PAGE_LIMIT = 7

        fun getIntent(context: Context): Intent {
            return Intent(context, TrendsFlowActivity::class.java)
        }

        @JvmStatic
        fun newInstance() = TrendsFlowActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityTrendsFlowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initUI() {
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(COUNTRY_POSITION))
        binding.etSearchTag.textChanges().debounce(FIX_400_MILLISECOND, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (isResumed) {
                    when (binding.tabLayout.selectedTabPosition) {
                        USER_POSITION -> {
                            RxBus.publish(RxEvent.SearchTextUser(it.toString()))
                        }

                        HASHTAG_POSITION -> {
                            RxBus.publish(RxEvent.SearchTextHashtags(it.toString()))
                        }

                        POST_POSITION -> {
                            RxBus.publish(RxEvent.SearchTextPost(it.toString()))
                        }

                        SHORTS_POSITION -> {
                            RxBus.publish(RxEvent.SearchTextShorts(it.toString()))
                        }

                        CHALLENGE_POSITION -> {
                            RxBus.publish(RxEvent.SearchTextChallenges(it.toString()))
                        }

                        SEARCH_POSITION -> {
                            RxBus.publish(RxEvent.SearchTextHashtags(it.toString()))
                        }
                    }
                }
            }.autoDispose()
        val search = binding.etSearchTag.text.toString().ifEmpty { "" }
        trendFlowAdapter = TrendFlowAdapter(
            requireActivity(), search
        )
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = OFF_SCREEN_PAGE_LIMIT.toInt()
        binding.viewPager.adapter = trendFlowAdapter
        setupTabUI()
        handleTabSelectListner()
    }

    private fun setupTabUI() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager, false, false) { tab, position ->
            when (position) {
                COUNTRY_POSITION -> {
                    tab.text = resources.getString(R.string.label_country)
                    val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab_layout, null)
                    tab.customView = customView
                    val ccp: CountryCodePicker = customView.findViewById(R.id.ccp)
                    tab.text = resources.getString(R.string.label_country).plus(" ")
                        .plus("(")
                        .plus(ccp.selectedCountryNameCode)
                        .plus(")")
                    ccp.setOnCountryChangeListener {
                        tab.text = resources.getString(R.string.label_country).plus(" ")
                            .plus("(")
                            .plus(ccp.selectedCountryNameCode)
                            .plus(")")
                        RxBus.publish(RxEvent.SearchCountryHashtags(ccp.selectedCountryNameCode))
                    }
                }

                USER_POSITION -> {
                    tab.text = resources.getString(R.string.users)
                }

                HASHTAG_POSITION -> {
                    tab.text = resources.getString(R.string.hashtags)
                }

                POST_POSITION -> {
                    tab.text = resources.getString(R.string.post)
                }

                SHORTS_POSITION -> {
                    tab.text = resources.getString(R.string.shorts)
                }

                CHALLENGE_POSITION -> {
                    tab.text = resources.getString(R.string.challenges)
                }

                SEARCH_POSITION -> {
                    tab.text = ""
                    tab.icon = resources.getDrawable(R.drawable.quantum_ic_search_grey600_24, null)
                }
            }
        }.attach()
    }

    private fun handleTabSelectListner() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    SHORTS_POSITION -> {
                        RxBus.publish(RxEvent.RefreshHashtagVideoFragment)
                        binding.toolbarRelativeLayout.isVisible = false
                        hideKeyBoard(imm)
                    }

                    SEARCH_POSITION -> {
                        binding.toolbarRelativeLayout.isVisible = true
                        binding.etSearchTag.requestFocus()
                        imm.showSoftInput(binding.etSearchTag, InputMethodManager.SHOW_IMPLICIT)
                    }

                    else -> {
                        binding.toolbarRelativeLayout.isVisible = false
                        hideKeyBoard(imm)
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
            RxBus.publish(RxEvent.PlayHashtagVideo(true, count))
        }
        RxBus.publish(RxEvent.RefreshPlayerView)
    }

    override fun onPause() {
        super.onPause()
        RxBus.publish(RxEvent.PlayHashtagVideo(false, count))
    }

    private fun hideKeyBoard(imm: InputMethodManager) {
        try {
            imm.hideSoftInputFromWindow(view?.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
