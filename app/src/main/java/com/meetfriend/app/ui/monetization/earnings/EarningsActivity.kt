package com.meetfriend.app.ui.monetization.earnings

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.monetization.model.EarningAmountInfo
import com.meetfriend.app.api.monetization.model.EarningListRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityEarningsBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.monetization.earnings.bottomsheet.DateFilterBottomSheet
import com.meetfriend.app.ui.monetization.earnings.view.EarningsTargetAdapter
import com.meetfriend.app.ui.monetization.earnings.viewmodel.EarningsViewModel
import com.meetfriend.app.ui.monetization.earnings.viewmodel.EarningsViewStates
import com.meetfriend.app.ui.redeem.RedeemAmountBottomSheet
import com.meetfriend.app.ui.redeem.history.RedeemHistoryActivity
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FiXED_50_FLOAT
import com.meetfriend.app.utils.FileUtils
import contractorssmart.app.utilsclasses.PreferenceHandler.readString
import java.time.ZonedDateTime
import java.util.*
import java.util.Locale
import javax.inject.Inject

class EarningsActivity : BasicActivity() {

    companion object {
        const val TAG = "EarningsActivity"
        fun getIntent(context: Context): Intent {
            return Intent(context, EarningsActivity::class.java)
        }
    }

    private lateinit var timeZone: String
    private lateinit var binding: ActivityEarningsBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<EarningsViewModel>
    private lateinit var earningsViewModel: EarningsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var earningsTargetAdapter: EarningsTargetAdapter
    private var symbol = ""
    private var acceptedDate: String? = null
    private var totalEarnings = "0.0"

    var startDate: String? = FileUtils.getWeekDates().first
    var endDate: String? = FileUtils.getWeekDates().second

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEarningsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        earningsViewModel = getViewModelFromFactory(viewModelFactory)

        val currencyCode: String
        var data = readString(this, "countryCode", "")
        if (data == "") {
            data = Locale.getDefault().toString()
            currencyCode = android.icu.util.Currency.getInstance(Locale(data)).currencyCode ?: ""
        } else {
            currencyCode = android.icu.util.Currency.getInstance(Locale("", data)).currencyCode ?: ""
        }
        symbol = Currency.getInstance(currencyCode).symbol ?: ""

        listenToViewModel()
        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        FileUtils.manageRedeem(
            this,
            binding.tvDisableRedeem,
            binding.tvRedeem
        )

        timeZone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ZonedDateTime.now().zone.toString()
        } else {
            TimeZone.getDefault().id
        }
        earningsTargetAdapter = EarningsTargetAdapter(this@EarningsActivity)

        binding.apply {
            initAdapter()
            setupClickListeners()
            loadUserProfile()
            val (currentSunday, currentSaturday) = FileUtils.getWeekDates()
            val dateString =
                resources.getString(R.string.earnings_date_between, currentSunday, currentSaturday)
            totalEarningWithin.text = dateString
            manageAPICall(currentSunday, currentSaturday, timeZone)
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            dateFilterButton.throttleClicks().subscribeAndObserveOnMainThread {
                openDateFilterBottomSheet()
            }.autoDispose()

            ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
                onBackPressedDispatcher.onBackPressed()
            }.autoDispose()

            tvRedeem.throttleClicks().subscribeAndObserveOnMainThread {
                handleRedeemClick()
            }.autoDispose()

            ivHistory.throttleClicks().subscribeAndObserveOnMainThread {
                startActivity(RedeemHistoryActivity.getIntent(this@EarningsActivity, 1))
            }.autoDispose()

            swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
                binding.swipeRefreshLayout.isRefreshing = false
                refreshEarningsData()
            }.autoDispose()
        }
    }

    private fun openDateFilterBottomSheet() {
        binding.apply {
            val dateFilterBottomSheet =
                DateFilterBottomSheet.newInstance(dateFilterButton.text.toString(), acceptedDate)
            dateFilterBottomSheet.selectState.subscribeAndObserveOnMainThread {
                dateFilterButton.text = it.selectedOption
                when (it.selectedOption) {
                    resources.getString(R.string.bsv_this_week) -> {
                        val (thisSunday, thisSaturday) = FileUtils.getWeekDates()
                        totalEarningWithin.text = resources.getString(
                            R.string.earnings_date_between,
                            thisSunday,
                            thisSaturday
                        )
                        startDate = thisSunday
                        endDate = thisSaturday
                        manageAPICall(thisSunday, thisSaturday, timeZone)
                    }

                    resources.getString(R.string.bsv_last_week) -> {
                        val (lastSunday, lastSaturday) = FileUtils.getPreviousWeekDates()
                        totalEarningWithin.text = resources.getString(
                            R.string.earnings_date_between,
                            lastSunday,
                            lastSaturday
                        )
                        startDate = lastSunday
                        endDate = lastSaturday
                        manageAPICall(lastSunday, lastSaturday, timeZone)
                    }

                    else -> {
                        totalEarningWithin.text = resources.getString(
                            R.string.earnings_date_between,
                            it.sunday,
                            it.saturday
                        )
                        startDate = it.sunday
                        endDate = it.saturday
                        it.saturday?.let { it1 -> it.sunday?.let { it2 -> manageAPICall(it2, it1, timeZone) } }
                    }
                }
            }
            dateFilterBottomSheet.show(
                supportFragmentManager,
                DateFilterBottomSheet.Companion::class.java.name
            )
        }
    }

    private fun loadUserProfile() {
        binding.apply {
            val loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser
            tvUserName.text = loggedInUser?.userName

            Glide.with(this@EarningsActivity)
                .load(loggedInUser?.profilePhoto)
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .into(profileImage)

            ivAccountVerified.visibility = if (loggedInUser?.isVerified == 1) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun handleRedeemClick() {
        val minAmount = FiXED_50_FLOAT * Constant.EXCHANGE_RATE.toFloat()
        if (totalEarnings.toFloat() < minAmount) {
            showToast(
                getString(R.string.label_amount_must_be_at_least)
                    .plus(" ")
                    .plus(Constant.CURRENCY_SYMBOL)
                    .plus(String.format(Locale.US, "%.2f", minAmount))
            )
        } else {
            val bottomSheet = RedeemAmountBottomSheet.newInstance(
                String.format(Locale.US, "%.2f", totalEarnings.toFloat()),
                1
            )
            bottomSheet.show(supportFragmentManager, RedeemAmountBottomSheet::class.java.name)
        }
    }

    private fun refreshEarningsData() {
        startDate?.let { start ->
            endDate?.let { end ->
                manageAPICall(start, end, timeZone)
            }
        }
    }

    private fun initAdapter() {
        binding.apply {
            rvEarningList.apply {
                layoutManager =
                    LinearLayoutManager(this@EarningsActivity, LinearLayoutManager.VERTICAL, false)
                adapter = earningsTargetAdapter
            }
        }
    }

    private fun manageAPICall(startDate: String, endDate: String, timeZone: String) {
        earningsViewModel.getEarningList(
            EarningListRequest(
                FileUtils.convertDateFormat(startDate),
                FileUtils.convertDateFormat(endDate),
                timeZone
            )
        )
    }

    private fun listenToViewModel() {
        earningsViewModel.earningsState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is EarningsViewStates.ErrorMessage -> {
                }
                is EarningsViewStates.LoadingState -> {
                    if (state.isLoading) showLoading() else hideLoading()
                }
                is EarningsViewStates.EarningData -> {
                    setData(state.data)

                    if (!state.data.totalCategoryCoins.isNullOrEmpty()) {
                        binding.tvEmptyData.isVisible = false
                        binding.rvEarningList.isVisible = true
                    } else {
                        binding.tvEmptyData.isVisible = true
                        binding.rvEarningList.isVisible = false
                    }
                }
                is EarningsViewStates.SuccessMessage -> {
                    showToast(state.successMessage)
                }
                is EarningsViewStates.ClaimedCoin -> {
                    showToast(state.successMessage)
                }
            }
        }.autoDispose()
    }

    private fun setData(data: EarningAmountInfo) {
        acceptedDate = data.acceptDate
        if (data.requsetStatus == "rejected") {
            rejectRequestDialog()
        } else if (data.requsetStatus == "pending") {
            pendingRequestDialog()
        }
        earningsTargetAdapter.listOfDataItems = data.totalCategoryCoins
        val multiply = String.format(
            Locale.US,
            "%.2f",
            (data.totalUsd?.toDouble() ?: 0.00) * (Constant.EXCHANGE_RATE.toDouble())
        )
        binding.totalEarning.text = symbol.plus(" ").plus(multiply)
        val earningMultiply = String.format(
            Locale.US,
            "%.2f",
            (data.totalEarning?.toDouble() ?: 0.00) * (Constant.EXCHANGE_RATE.toDouble())
        )
        binding.tvRedeem.isClickable = (data.totalEarning?.toDouble() ?: 0.00) > 0.00
        if (earningMultiply.toFloat() < 0) {
            binding.tvTotalEarningData.text = symbol.plus(" ").plus(0.00)
        } else {
            binding.tvTotalEarningData.text = symbol.plus(" ").plus(earningMultiply)
        }
        totalEarnings = earningMultiply
    }

    private fun pendingRequestDialog() {
        binding.nestedScrollView.isVisible = false
        val builder = AlertDialog.Builder(this)
        builder.setMessage("we received your request will reply in 24 hours.")
        builder.setPositiveButton(
            resources.getString(R.string.ef_ok)
        ) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun rejectRequestDialog() {
        binding.nestedScrollView.isVisible = false
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your submitted application request is rejected.You can resend request after a week.")
        builder.setPositiveButton(getString(R.string.ef_ok)) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }
}
