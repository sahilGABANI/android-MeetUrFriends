package com.meetfriend.app.ui.chatRoom.subscription

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.subscription.model.AdminSubscriptionRequest
import com.meetfriend.app.api.subscription.model.SubscriptionOption
import com.meetfriend.app.api.subscription.model.SubscriptionRequest
import com.meetfriend.app.api.subscription.model.TempPlanInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivitySubscriptionBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.roomview.ViewChatRoomActivity
import com.meetfriend.app.ui.chatRoom.subscription.viewmodel.SubscriptionViewModel
import com.meetfriend.app.ui.chatRoom.subscription.viewmodel.SubscriptionViewState
import timber.log.Timber
import java.util.Base64
import javax.inject.Inject
import kotlin.properties.Delegates

class SubscriptionActivity : BasicActivity() {

    companion object {
        private const val INTENT_SELECTED_PLAN = "INTENT_SELECTED_PLAN"
        private const val CHAT_ID = "CHAT_ID"
        private const val INTENT_SUBSCRIPTION_OPTION = "SUBSCRIPTION_OPTION"

        fun getIntent(
            context: Context,
            selectedPlan: TempPlanInfo,
            conversationId: Int,
            subscriptionOption: String
        ): Intent {
            val intent = Intent(context, SubscriptionActivity::class.java)
            intent.putExtra(INTENT_SELECTED_PLAN, selectedPlan)
            intent.putExtra(CHAT_ID, conversationId)
            intent.putExtra(INTENT_SUBSCRIPTION_OPTION, subscriptionOption)
            return intent
        }
    }

    private lateinit var binding: ActivitySubscriptionBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SubscriptionViewModel>
    private lateinit var subscriptionViewModel: SubscriptionViewModel

    private lateinit var billingClient: BillingClient
    lateinit var selectedPlan: TempPlanInfo
    var conversationId by Delegates.notNull<Int>()
    lateinit var subscriptionOption: String

    @Inject
    lateinit var chatRepository: ChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MeetFriendApplication.component.inject(this)
        subscriptionViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDataFromIntent()
        setUpBillingClient()
        listenToViewModel()
        listenToViewEvent()
    }

    private fun loadDataFromIntent() {
        selectedPlan = intent?.getParcelableExtra(INTENT_SELECTED_PLAN) ?: return
        conversationId = intent?.getIntExtra(CHAT_ID, 0) ?: return
        subscriptionOption = intent.getStringExtra(INTENT_SUBSCRIPTION_OPTION) ?: return

        binding.tvRoomType.text = selectedPlan.roomType
        binding.tvAmount.text = selectedPlan.amount
        binding.tvDuration.text = selectedPlan.duration
    }

    private fun setUpBillingClient() {
        val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handleConsumedPurchases(purchase)
                    }
                }
            }

        billingClient = BillingClient.newBuilder(MeetFriendApplication.context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
    }

    private fun listenToViewEvent() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.tvPay.throttleClicks().subscribeAndObserveOnMainThread {
            startConnection()
            showLoading()
            // binding.tvPay.visibility = View.GONE
        }.autoDispose()
    }

    private fun listenToViewModel() {
        subscriptionViewModel.subscriptionState.subscribeAndObserveOnMainThread {
            when (it) {
                is SubscriptionViewState.ErrorMessage -> {}

                is SubscriptionViewState.LoadingState -> {}

                is SubscriptionViewState.SubscriptionSuccess -> {
                }
                is SubscriptionViewState.SubscriptionData -> {
                    finish()
                    RxBus.publish(RxEvent.SubscriptionSuccessFull)
                    startActivity(ViewChatRoomActivity.getIntent(this, it.chatRoomInfo))
                }
                is SubscriptionViewState.AdminSubscriptionData -> {
                    finish()
                    RxBus.publish(RxEvent.UpdatedAdminSubscription)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Billing setup done")

                    queryAvailableProducts(selectedPlan)
                }
            }

            override fun onBillingServiceDisconnected() {
                startConnection()
            }
        })
    }

    private fun queryAvailableProducts(selectedPlan: TempPlanInfo?) {
        val skuList = ArrayList<String>()
        when (selectedPlan?.duration) {
            getString(R.string.label_1_month) -> {
                skuList.add(getString(R.string.in_app_one_month_purchase_id))
            }
            getString(R.string.label_3_month) -> {
                skuList.add(getString(R.string.in_app_three_month_purchase_id))
            }
            getString(R.string.label_6_month) -> {
                skuList.add(getString(R.string.in_app_six_month_purchase_id))
            }
            getString(R.string.label_12_month) -> {
                skuList.add(getString(R.string.in_app_twelve_month_purchase_id))
            }
        }

        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                for (skuDetails in skuDetailsList) {
                    skuDetails?.let {
                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(it)
                            .build()
                        billingClient.launchBillingFlow(this, billingFlowParams).responseCode
                    } ?: ""
                }
            } else {
                runOnUiThread {
                    showToast(billingResult.debugMessage)
                    hideLoading()
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun handleConsumedPurchases(purchase: Purchase) {
        val params =
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.consumeAsync(params) { billingResult, purchaseToken ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val payload =
                        Base64.getMimeEncoder().encodeToString(purchase.toString().toByteArray())

                    when (subscriptionOption) {
                        SubscriptionOption.Admin.name -> {
                            subscriptionViewModel.updateAdminSubscription(
                                conversationId,
                                AdminSubscriptionRequest(
                                    month = selectedPlan.month,
                                    transactionId = purchaseToken,
                                )
                            )
                        }

                        SubscriptionOption.ChatRoom.name -> {
                            subscriptionViewModel.updateSubscription(
                                conversationId,
                                SubscriptionRequest(
                                    month = selectedPlan.month,
                                    transactionId = purchaseToken,
                                    androidPayload = payload
                                )
                            )
                        }
                    }
                }
                else -> {
                    runOnUiThread {
                        showToast(billingResult.debugMessage)
                        binding.tvPay.visibility = View.VISIBLE
                        hideLoading()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClient.endConnection()
    }
}
