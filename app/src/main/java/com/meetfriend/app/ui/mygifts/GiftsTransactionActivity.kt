package com.meetfriend.app.ui.mygifts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.gift.model.TransactionType
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityGiftsTransactionBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.monetization.earnings.bottomsheet.DateFilterBottomSheet
import com.meetfriend.app.ui.monetization.earnings.bottomsheet.DatesFilter
import com.meetfriend.app.ui.mygifts.view.GiftTransactionTabAdapter
import com.meetfriend.app.ui.redeem.history.RedeemHistoryActivity
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FiXED_3_INT
import com.meetfriend.app.utils.FileUtils
import contractorssmart.app.utilsclasses.PreferenceHandler
import java.util.*
import javax.inject.Inject

class GiftsTransactionActivity : BasicActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, GiftsTransactionActivity::class.java)
        }
    }

    lateinit var binding: ActivityGiftsTransactionBinding

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var giftTransactionTabAdapter: GiftTransactionTabAdapter

    private var datesFilter: DatesFilter? = null
    private var symbol = "$"
    private var transactionType: TransactionType = TransactionType.Received

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGiftsTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        listenToViewEvent()

        val currencyCode: String
        var data = PreferenceHandler.readString(this, "countryCode", "")
        if (data == "") {
            data = Locale.getDefault().toString()
            currencyCode = android.icu.util.Currency.getInstance(Locale(data)).currencyCode ?: ""
        } else {
            currencyCode = android.icu.util.Currency.getInstance(Locale("", data)).currencyCode ?: ""
        }
        symbol = Currency.getInstance(currencyCode).symbol ?: ""
        Constant.CURRENCY_SYMBOL = symbol
        intent?.let {
            if (it.getBooleanExtra(MainHomeActivity.INTENT_IS_SWITCH_ACCOUNT, false)) {
                intent?.getIntExtra(MainHomeActivity.USER_ID, 0)?.let { userId ->
                    RxBus.publish(RxEvent.switchUserAccount(userId))
                }
            }
        }
    }

    private fun listenToViewEvent() {
        binding.ivHistory.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(RedeemHistoryActivity.getIntent(this, 0))
        }.autoDispose()

        binding.tvUserName.text = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName

        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        Glide.with(this@GiftsTransactionActivity)
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.profileImage)

        val (currentSunday, currentSaturday) = FileUtils.getWeekDates()
        val dateString =
            resources.getString(R.string.earnings_date_between, currentSunday, currentSaturday)
        binding.totalEarningWithin.text = dateString

        datesFilter = DatesFilter(
            binding.dateFilterButton.text.toString(),
            currentSunday,
            currentSaturday
        )
        initTabAdapter()

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.dateFilterButton.throttleClicks().subscribeAndObserveOnMainThread {
            val dateFilterBottomSheet =
                DateFilterBottomSheet.newInstance(binding.dateFilterButton.text.toString())
            dateFilterBottomSheet.selectState.subscribeAndObserveOnMainThread {
                binding.dateFilterButton.text = it.selectedOption
                manageSelectedDateOption(
                    it.selectedOption,
                    it.sunday.toString(),
                    it.saturday.toString()
                )
                datesFilter = it
            }
            dateFilterBottomSheet.show(
                supportFragmentManager,
                DateFilterBottomSheet.Companion::class.java.name
            )
        }.autoDispose()

        RxBus.listen(RxEvent.TotalEarning::class.java).subscribeAndObserveOnMainThread {
            binding.totalEarning.text =
                symbol.plus(" ").plus(
                    String.format(
                        Locale.US,
                        "%.2f",
                        it.amount.toFloat()
                    )
                )
        }.autoDispose()
    }

    private fun initTabAdapter() {
        giftTransactionTabAdapter = GiftTransactionTabAdapter(
            this
        )
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = FiXED_3_INT.toInt()
        binding.viewPager.adapter = giftTransactionTabAdapter
        TabLayoutMediator(
            binding.historyTabLayout,
            binding.viewPager,
            false,
            false
        ) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = resources.getString(R.string.label_received_history)
                }

                1 -> {
                    tab.text = resources.getString(R.string.label_sent_history)
                }

                2 -> {
                    tab.text = resources.getString(R.string.label_weekly_summary)
                }
            }
        }.attach()
        binding.historyTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.text) {
                    resources.getString(R.string.label_received_history) -> {
                        transactionType = TransactionType.Received
                    }

                    resources.getString(R.string.label_sent_history) -> {
                        transactionType = TransactionType.Sent
                    }

                    resources.getString(R.string.label_weekly_summary) -> {
                        transactionType = TransactionType.Weekly
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                return
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                return
            }
        })
    }

    private fun manageSelectedDateOption(
        selectedOption: String?,
        sunday: String,
        saturday: String
    ) {
        when (selectedOption) {
            resources.getString(R.string.bsv_this_week) -> {
                val (thisSunday, thisSaturday) = FileUtils.getWeekDates()
                binding.totalEarningWithin.text = resources.getString(
                    R.string.earnings_date_between,
                    thisSunday,
                    thisSaturday
                )
                RxBus.publish(
                    RxEvent.RefreshGiftSummaryFragment(
                        thisSunday,
                        thisSaturday
                    )
                )
            }

            resources.getString(R.string.bsv_last_week) -> {
                val (lastSunday, lastSaturday) = FileUtils.getPreviousWeekDates()
                binding.totalEarningWithin.text = resources.getString(
                    R.string.earnings_date_between,
                    lastSunday,
                    lastSaturday
                )
                RxBus.publish(
                    RxEvent.RefreshGiftSummaryFragment(
                        lastSunday,
                        lastSaturday
                    )
                )
            }

            else -> {
                binding.totalEarningWithin.text = resources.getString(
                    R.string.earnings_date_between,
                    sunday,
                    saturday
                )
                RxBus.publish(RxEvent.RefreshGiftSummaryFragment(sunday, saturday))
            }
        }
    }
}
