package com.meetfriend.app.ui.mygifts

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.R
import com.meetfriend.app.api.monetization.model.EarningListRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentGiftWeeklySummaryBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.mygifts.view.GiftSummaryAdapter
import com.meetfriend.app.ui.mygifts.viewmodel.GiftWeeklySummaryViewModel
import com.meetfriend.app.ui.mygifts.viewmodel.GiftWeeklySummaryViewState
import com.meetfriend.app.ui.redeem.RedeemAmountBottomSheet
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FileUtils
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject


class GiftWeeklySummaryFragment : BasicFragment() {
    companion object {

        @JvmStatic
        fun newInstance(): GiftWeeklySummaryFragment {
            val args = Bundle()
            val fragment = GiftWeeklySummaryFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentGiftWeeklySummaryBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GiftWeeklySummaryViewModel>
    private lateinit var giftWeeklySummaryViewModel: GiftWeeklySummaryViewModel

    private lateinit var giftSummaryAdapter: GiftSummaryAdapter
    private var timeZone: String = ""
    private var totalEarning = "0.0"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGiftWeeklySummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MeetFriendApplication.component.inject(this)

        giftWeeklySummaryViewModel = getViewModelFromFactory(viewModelFactory)
        listenToViewModel()
        listenToViewEvents()

        timeZone = if (Build.VERSION.SDK_INT >= 26) {
            ZonedDateTime.now().zone.toString()
        } else {
            TimeZone.getDefault().id
        }
        val (currentSunday, currentSaturday) = FileUtils.getWeekDates()

        giftWeeklySummaryViewModel.giftWeeklySummary(
            EarningListRequest(
                FileUtils.convertDateFormat(currentSunday),
                FileUtils.convertDateFormat(currentSaturday),
                timeZone
            )
        )

    }

    private fun listenToViewEvents() {
        FileUtils.manageRedeem(requireContext(), binding.tvDisableRedeem, binding.tvRedeem)
        var startDate = FileUtils.getWeekDates().first
        var endDate = FileUtils.getWeekDates().second

        giftSummaryAdapter = GiftSummaryAdapter(requireContext())

        binding.giftsHistoryRecyclerView.adapter = giftSummaryAdapter
        binding.giftsHistoryRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        RxBus.listen(RxEvent.RefreshGiftSummaryFragment::class.java)
            .subscribeAndObserveOnMainThread {
                startDate = it.startDate
                endDate = it.endDate
                giftWeeklySummaryViewModel.giftWeeklySummary(
                    EarningListRequest(
                        FileUtils.convertDateFormat(it.startDate),
                        FileUtils.convertDateFormat(it.endDate),
                        timeZone
                    )
                )

            }

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
                        "%.2f",
                        totalEarning.toFloat()
                    ), 0
                )
                bottomSheet.show(childFragmentManager, RedeemAmountBottomSheet::class.java.name)
            }

        }.autoDispose()

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            giftWeeklySummaryViewModel.giftWeeklySummary(
                EarningListRequest(
                    FileUtils.convertDateFormat(startDate),
                    FileUtils.convertDateFormat(endDate),
                    timeZone
                )
            )
        }.autoDispose()
    }

    private fun listenToViewModel() {
        giftWeeklySummaryViewModel.giftSummaryState.subscribeAndObserveOnMainThread {

            when (it) {
                GiftWeeklySummaryViewState.EmptyState -> {
                    binding.giftsHistoryRecyclerView.isVisible = false
                    binding.mNoGift.isVisible = true
                }
                is GiftWeeklySummaryViewState.ErrorMessage -> {
                    hideLoading()
                    showToast(it.errorMessage)
                }
                is GiftWeeklySummaryViewState.GiftTransactionData -> {
                    binding.giftsHistoryRecyclerView.isVisible = true
                    binding.mNoGift.isVisible = false
                    hideLoading()
                    giftSummaryAdapter.listOfDataItems = it.data

                }
                is GiftWeeklySummaryViewState.LoadingState -> {
                    showLoading(it.isLoading)
                }

                is GiftWeeklySummaryViewState.SuccessMessage -> {
                }
                is GiftWeeklySummaryViewState.TotalEarningAmount -> {
                    var amount = (it.amount?.toFloat() ?: 0F) * Constant.EXCHANGE_RATE.toFloat()
                    if (amount < 0F) {
                        amount = 0F
                    }
                    totalEarning = amount.toString()
                    binding.tvTotalEarning.text =
                        Constant.CURRENCY_SYMBOL.plus(String.format("%.2f", amount))
                    RxBus.publish(RxEvent.TotalEarning(amount.toString()))
                }
            }
        }.autoDispose()
    }

}