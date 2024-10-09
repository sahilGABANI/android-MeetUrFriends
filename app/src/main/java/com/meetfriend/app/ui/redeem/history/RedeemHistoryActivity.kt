package com.meetfriend.app.ui.redeem.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.R
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityRedeemHistoryBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.redeem.RedeemAmountBottomSheet
import com.meetfriend.app.ui.redeem.history.view.RedeemHistoryAdapter
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FileUtils
import javax.inject.Inject

class RedeemHistoryActivity : BasicActivity() {

    companion object {
        private const val INTENT_FROM_MONETIZATION = "INTENT_FROM_MONETIZATION"
        fun getIntent(context: Context, isMonetization: Int): Intent {
            val intent = Intent(context, RedeemHistoryActivity::class.java)
            intent.putExtra(INTENT_FROM_MONETIZATION, isMonetization)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<RedeemHistoryViewModel>
    private lateinit var redeemHistoryViewModel: RedeemHistoryViewModel

    lateinit var binding: ActivityRedeemHistoryBinding
    lateinit var redeemHistoryAdapter: RedeemHistoryAdapter
    private var totalEarning = "0.0"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRedeemHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this@RedeemHistoryActivity)
        redeemHistoryViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        val isFromMonetization = intent?.let { it.getIntExtra(INTENT_FROM_MONETIZATION, 0) }
        manageAPICall(isFromMonetization ?: 0)

        FileUtils.manageRedeem(this,binding.tvDisableRedeem,binding.tvRedeem)

        binding.ivBackIcon.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.tvRedeem.throttleClicks().subscribeAndObserveOnMainThread {
            val minAmount = 50.0 * Constant.EXCHANGE_RATE.toFloat()
            if (totalEarning.toFloat() < minAmount) {
                showToast(
                    getString(R.string.label_amount_must_be_at_least).plus(" ").plus(Constant.CURRENCY_SYMBOL).plus(
                        String.format("%.2f", minAmount)
                    )
                )
            } else {
                val bottomSheet = RedeemAmountBottomSheet.newInstance(
                    String.format(
                        "%.2f", totalEarning.toFloat()
                    ), isFromMonetization ?: 0
                )
                bottomSheet.show(supportFragmentManager, RedeemAmountBottomSheet::class.java.name)
            }
        }.autoDispose()

        redeemHistoryAdapter = RedeemHistoryAdapter(this)

        binding.rvHistory.apply {
            adapter = redeemHistoryAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                if(isFromMonetization == 1) {
                                    redeemHistoryViewModel.loadMoreMHistory()
                                } else {
                                    redeemHistoryViewModel.loadMoreHistory()
                                }
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            manageAPICall(isFromMonetization ?: 0)
            binding.rvHistory.smoothScrollToPosition(0)
        }.autoDispose()
    }

    private fun manageAPICall(isFrom: Int) {
        if (isFrom == 1) {
            redeemHistoryViewModel.resetPaginationForMHistory()
        } else {
            redeemHistoryViewModel.resetPaginationForHistory()
        }
    }

    private fun listenToViewModel() {
        redeemHistoryViewModel.redeemHistoryState.subscribeAndObserveOnMainThread {

            when (it) {
                is RedeemHistoryViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is RedeemHistoryViewState.LoadingState -> {
                    if (redeemHistoryAdapter.listOfDataItems.isNullOrEmpty()) {
                        if (it.isLoading) showLoading() else hideLoading()
                    }
                }
                is RedeemHistoryViewState.HistoryData -> {
                    binding.tvEmptyState.isVisible = it.data.isNullOrEmpty()
                    redeemHistoryAdapter.listOfDataItems = it.data
                }
                is RedeemHistoryViewState.VerifyOTPSuccess -> {

                }
                is RedeemHistoryViewState.EarningAmount -> {
                    var amount = (it.amount?.toFloat() ?: 0F) * Constant.EXCHANGE_RATE.toFloat()
                    if (amount < 0F) {
                        amount = 0F
                    }
                    totalEarning = amount.toString()
                    binding.tvTotalEarning.text =
                        Constant.CURRENCY_SYMBOL.plus(String.format("%.2f", amount))
                }
            }
        }.autoDispose()
    }

}