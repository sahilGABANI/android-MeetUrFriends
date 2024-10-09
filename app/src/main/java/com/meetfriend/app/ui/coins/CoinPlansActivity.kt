package com.meetfriend.app.ui.coins

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.meetfriend.app.R
import com.meetfriend.app.api.gift.model.CoinPlanInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityCoinPlansBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.coins.view.CoinPlanAdapter
import com.meetfriend.app.ui.coins.viewmodel.CoinPlansViewModel
import com.meetfriend.app.ui.coins.viewmodel.CoinPlansViewState
import com.meetfriend.app.utilclasses.PurchaseHelper
import com.meetfriend.app.utilclasses.PurchaseHelper.PurchaseHelperListener
import javax.inject.Inject

class CoinPlansActivity : BasicActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, CoinPlansActivity::class.java)
        }
    }

    lateinit var binding: ActivityCoinPlansBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CoinPlansViewModel>
    private lateinit var coinPlansViewModel: CoinPlansViewModel

    private lateinit var coinPlanAdapter: CoinPlanAdapter
    private var listOfCoinPlans: List<CoinPlanInfo> = listOf()
    private var purchaseHelper: PurchaseHelper? = null
    private var isPurchaseQueryPending = false
    private var index = 0
    private var selectedRechargePlan: CoinPlanInfo? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCoinPlansBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        coinPlansViewModel = getViewModelFromFactory(viewModelFactory)


        purchaseHelper = PurchaseHelper(this, getPurchaseHelperListener())


        listenToViewModel()
        listenToViewEvent()
        coinPlansViewModel.getCoinPlans()
        coinPlansViewModel.getMyEarningInfo()
        isPurchaseQueryPending = true

    }

    private fun getPurchaseHelperListener(): PurchaseHelperListener? {
        return object : PurchaseHelperListener {
            override fun onServiceConnected(resultCode: Int) {
                if (isPurchaseQueryPending) {
                    purchaseHelper?.getPurchasedItems()
                    isPurchaseQueryPending = false
                }
            }

            override fun onSkuQueryResponse(skuDetails: List<SkuDetails>) {}
            override fun onPurchasehistoryResponse(purchasedItems: List<Purchase>) {}
            override fun onPurchasesUpdated(responseCode: Int, purchases: List<Purchase>?) {

                if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    val coins = ((listOfCoinPlans[index].coins!!.toDouble() + listOfCoinPlans[index]
                        .discountCoins!!.toDouble()).toString() + "")
                    coinPlansViewModel.coinPurchase(
                        coins, purchases[0].orderId.toString(), listOfCoinPlans[index].id.toString(),
                        listOfCoinPlans[index].amount.toString()
                    )

                }
            }
        }
    }


    private fun listenToViewEvent() {
        coinPlanAdapter = CoinPlanAdapter(this).apply {
            coinPlanItemClicks.subscribeAndObserveOnMainThread {
                selectedRechargePlan = it
                coinPlanAdapter.selectedPlanInfo = it

            }.autoDispose()

        }
        binding.planRecyclerView.apply {
            adapter = coinPlanAdapter
            layoutManager =
                GridLayoutManager(this@CoinPlansActivity, 2, GridLayoutManager.VERTICAL, false)


        }
        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.rechargeAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            if (selectedRechargePlan != null) {
                var planName: String = when (selectedRechargePlan?.id) {
                    1 -> "gift_"
                    2 -> "basic_02"
                    3 -> "silver_03"
                    4 -> "gold_04"
                    5 -> "platinum_05"
                    6 -> "best_value_06"
                    7 -> "best_offer_07"
                    8 -> getString(R.string.in_app_purchase_id_gift_0_99)
                    9 -> getString(R.string.in_app_purchase_id_gift_4_99)
                    10 -> getString(R.string.in_app_purchase_id_gift_9_96)
                    11 -> getString(R.string.in_app_purchase_id_gift_14_99)
                    12 -> getString(R.string.in_app_purchase_id_gift_19_99)
                    13 -> getString(R.string.in_app_purchase_id_gift_24_99)
                    14 -> getString(R.string.in_app_purchase_id_gift_29_99)
                    15 -> getString(R.string.in_app_purchase_id_gift_49_99)
                    16 -> getString(R.string.in_app_purchase_id_gift_74_99)
                    17 -> getString(R.string.in_app_purchase_id_gift_99_99)
                    18 -> getString(R.string.in_app_purchase_id_gift_149_99)
                    19 -> getString(R.string.in_app_purchase_id_gift_199_99)
                    20 -> getString(R.string.in_app_purchase_id_gift_249_99)
                    21 -> getString(R.string.in_app_purchase_id_gift_299_99)
                    else -> {
                        ""
                    }
                }

                index = listOfCoinPlans.indexOf(selectedRechargePlan)
                purchaseHelper?.launchBillingFLow(
                    BillingClient.SkuType.INAPP,
                    planName
                )
            } else {
                showToast("please select plan for recharge")
            }

        }.autoDispose()
    }

    private fun listenToViewModel() {

        coinPlansViewModel.coinPlansState.subscribeAndObserveOnMainThread {
            when (it) {
                is CoinPlansViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is CoinPlansViewState.CoinPlanData -> {
                    listOfCoinPlans = it.coinPlanData
                    coinPlanAdapter.listOfDataItems = it.coinPlanData
                    coinPlanAdapter.selectedPlanInfo = listOfCoinPlans.firstOrNull()
                    selectedRechargePlan = listOfCoinPlans.firstOrNull()
                    hideShowNoData(listOfCoinPlans)
                   hideLoading()

                }
                is CoinPlansViewState.LoadingState -> {
                    if (coinPlanAdapter.listOfDataItems.isNullOrEmpty()) {
                       if(it.isLoading) showLoading() else hideLoading()
                    }
                }
                is CoinPlansViewState.PurchaseSuccessMessage -> {
                    showToast(it.successMessage)
                    finish()
                }
                CoinPlansViewState.EmptyState -> {
                    hideShowNoData(listOfCoinPlans)
                    hideLoading()
                }
                is CoinPlansViewState.MyEarningData -> {
                    binding.availableCoinsAppCompatTextView.text =
                        String.format("%.2f", it.myEarningInfo?.totalCurrentCoins)


                }
                else -> {}
            }
        }.autoDispose()


    }

    private fun hideShowNoData(listOfCoinPlans: List<CoinPlanInfo>) {
        if (listOfCoinPlans.isNotEmpty()) {
            binding.tvEmptyState.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.VISIBLE
        }
    }

}