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
import com.meetfriend.app.databinding.FragmentReceivedGiftBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.mygifts.view.GiftTransactionAdapter
import com.meetfriend.app.ui.mygifts.viewmodel.GiftTransactionViewState
import com.meetfriend.app.ui.mygifts.viewmodel.GiftsTransactionViewModel
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FileUtils
import java.time.ZonedDateTime
import javax.inject.Inject

class ReceivedGiftFragment : BasicFragment() {

    companion object {

        @JvmStatic
        fun newInstance(): ReceivedGiftFragment {
            val args = Bundle()
            val fragment = ReceivedGiftFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentReceivedGiftBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GiftsTransactionViewModel>
    private lateinit var giftsTransactionViewModel: GiftsTransactionViewModel

    private lateinit var giftsHistoryAdapter: GiftTransactionAdapter

    private var centValue = "0.0032"
    private var timeZone: String = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceivedGiftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MeetFriendApplication.component.inject(this)

        giftsTransactionViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvents()
        timeZone = if (Build.VERSION.SDK_INT >= 26) {
            ZonedDateTime.now().zone.toString()
        } else {
            java.util.TimeZone.getDefault().id
        }

        giftsTransactionViewModel.getCoinInCents()
        giftsTransactionViewModel.getMyEarningInfo()

        val (currentSunday, currentSaturday) = FileUtils.getWeekDates()
        giftsTransactionViewModel.getReceivedGiftTransaction(
            EarningListRequest(
                FileUtils.convertDateFormat(
                    currentSunday
                ), FileUtils.convertDateFormat(currentSaturday),
                timeZone
            )
        )
    }

    private fun listenToViewEvents() {

        var startDate = FileUtils.getWeekDates().first
        var endDate = FileUtils.getWeekDates().second
        binding.sentReceivedAppCompatTextView.text =
            resources.getString(R.string.label_total_received_coins)

        giftsHistoryAdapter = GiftTransactionAdapter(requireContext(),)

        binding.giftsHistoryRecyclerView.adapter = giftsHistoryAdapter
        binding.giftsHistoryRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        RxBus.listen(RxEvent.RefreshGiftSummaryFragment::class.java)
            .subscribeAndObserveOnMainThread {
                startDate = it.startDate
                endDate = it.endDate
                manageAPICall(it.startDate, it.endDate, timeZone)
            }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            manageAPICall(startDate,endDate,timeZone)
        }.autoDispose()

    }

    private fun listenToViewModel() {

        giftsTransactionViewModel.giftTransactionState.subscribeAndObserveOnMainThread {
            when (it) {
                GiftTransactionViewState.EmptyState -> {
                    binding.giftsHistoryRecyclerView.isVisible = false
                    binding.tvNoGifts.text = getString(R.string.label_no_gift_received)
                    binding.mNoGift.isVisible = true
                }
                is GiftTransactionViewState.ErrorMessage -> {
                    hideLoading()
                    showToast(it.errorMessage)
                }
                is GiftTransactionViewState.GiftTransactionData -> {
                    binding.giftsHistoryRecyclerView.isVisible = true
                    binding.mNoGift.isVisible = false
                    hideLoading()
                    giftsHistoryAdapter.listOfDataItems = it.data

                }

                is GiftTransactionViewState.LoadingState -> {
                    showLoading(it.isLoading)

                }
                is GiftTransactionViewState.MyEarningData -> {

                }
                is GiftTransactionViewState.SuccessMessage -> {

                }
                is GiftTransactionViewState.CoinCentsData -> {
                    centValue = it.coinCentsInfo?.value.toString()
                    Constant.CENT_VALUE = centValue
                }
                is GiftTransactionViewState.CoinsInfo -> {
                    binding.totalCoinsAppCompatTextView.text = String.format("%.2f", it.coin.toDouble())
                }
            }
        }.autoDispose()

    }


    private fun manageAPICall(
        sunday: String,
        saturday: String,
        timeZone: String
    ) {

        giftsTransactionViewModel.getReceivedGiftTransaction(
            EarningListRequest(
                FileUtils.convertDateFormat(
                    sunday
                ), FileUtils.convertDateFormat(saturday),
                timeZone
            )
        )

    }

}