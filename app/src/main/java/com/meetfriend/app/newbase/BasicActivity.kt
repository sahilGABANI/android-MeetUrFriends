package com.meetfriend.app.newbase

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.SendJoinChatRoomRequestRequest
import com.meetfriend.app.api.notification.model.AcceptRejectRequestRequest
import com.meetfriend.app.api.notification.model.NotificationActionState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.chatRoom.JoinChatRoomRequestDialogFragment
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewState
import com.meetfriend.app.ui.redeem.RedeemSuccessDialog
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utilclasses.ads.AdsManager
import com.meetfriend.app.utils.Constant
import com.mixpanel.android.mpmetrics.MixpanelAPI
import contractorssmart.app.utilsclasses.PreferenceHandler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*
import javax.inject.Inject

abstract class BasicActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()

    var isTransactionSafe = false
    var isTransactionPending = false
    var mp: MixpanelAPI? = null

    @Inject
    internal lateinit var viewModelFactoryB: ViewModelFactory<ChatRoomViewModel>
    lateinit var chatRoomViewModelB: ChatRoomViewModel

    @Inject
    lateinit var loggedInUserCacheBase: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mp = MixpanelAPI.getInstance(this, "9baf1cfb430c0de219529759f0b22395", true)

        MeetFriendApplication.component.inject(this)
        chatRoomViewModelB = getViewModelFromFactory(viewModelFactoryB)

        chatRoomViewModelB.observeJoinRoomRequest()
        listenToViewModelB()

        var currencyCode = ""
        var data = PreferenceHandler.readString(this, "countryCode", "")

        if (data.isEmpty()) {
            data = Locale.getDefault().toString()
            val locale = Locale(data)
            currencyCode = android.icu.util.Currency.getInstance(locale)?.currencyCode ?: ""
        } else {
            val locale = Locale("", data)
            currencyCode = android.icu.util.Currency.getInstance(locale)?.currencyCode ?: ""
        }

        Constant.CURRENCY_SYMBOL = Currency.getInstance(currencyCode).symbol ?: ""

        RxBus.listen(RxEvent.ShowRedeemInfo::class.java).subscribeAndObserveOnMainThread {
            openRedeemSuccessDialog(it.status == "1", it.amount)
        }.autoDispose()
    }

    private fun openRedeemSuccessDialog(isSuccess: Boolean, amount: String) {
        if (isTransactionSafe) {
            val redeemSuccessDialog = RedeemSuccessDialog.newInstance(isSuccess, amount)
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.add(redeemSuccessDialog, "requestDialog")
            fragmentTransaction.commit()
        } else {
            isTransactionPending = true
        }
    }

    fun showLoading(show: Boolean?) {
        if (show!!) showLoading() else hideLoading()
    }

    protected fun showLoading() {
        CallProgressWheel.showLoadingDialog(this@BasicActivity)
    }

    protected fun hideLoading() {
        CallProgressWheel.dismissLoadingDialog()
    }

    override fun onPostResume() {
        super.onPostResume()
        isTransactionSafe = true
    }

    override fun onPause() {
        super.onPause()
        isTransactionSafe = false
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    fun Disposable.autoDispose() {
        compositeDisposable.add(this)
    }

    private fun listenToViewModelB() {
        chatRoomViewModelB.chatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatRoomViewState.ErrorMessage -> {}
                is ChatRoomViewState.LoadingState -> {}
                is ChatRoomViewState.ReceivedRequestData -> {
                    openReceivedRequestDialog(it.requestData)
                }
                is ChatRoomViewState.SuccessMessage -> {}
                is ChatRoomViewState.AcceptRejectSuccess -> {
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun openReceivedRequestDialog(requestData: SendJoinChatRoomRequestRequest) {
        if (isTransactionSafe) {
            val joinChatRoomDialogFragment = JoinChatRoomRequestDialogFragment.newInstance(requestData)
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.add(joinChatRoomDialogFragment, "requestDialog")
            fragmentTransaction.commit()
            isTransactionPending = false

            joinChatRoomDialogFragment.notificationState.subscribeAndObserveOnMainThread {
                when (it) {
                    is NotificationActionState.AcceptFromPopup -> {
                        chatRoomViewModelB.acceptRejectRequest(
                            AcceptRejectRequestRequest(
                                conversationId = it.receivedRequestData.conversationId,
                                status = 1,
                                fromUid = it.receivedRequestData.senderId
                            )
                        )
                        joinChatRoomDialogFragment.dismiss()
                    }
                    is NotificationActionState.RejectFromPopup -> {
                        chatRoomViewModelB.acceptRejectRequest(
                            AcceptRejectRequestRequest(
                                conversationId = it.receivedRequestData.conversationId,
                                status = 2,
                                fromUid = it.receivedRequestData.senderId
                            )
                        )
                        joinChatRoomDialogFragment.dismiss()
                    }
                    else -> {}
                }
            }
        } else {
            isTransactionPending = true
        }
    }
    fun getProfilePic(): String {
        return PreferenceHandler.readString(this, PreferenceHandler.PROFILE_PHOTO, "")
    }

    fun openInterstitialAds() {
        if (Constant.CLICK_COUNT % loggedInUserCacheBase.getClickCountAd().toInt() == 0) {
            AdsManager.getAdsManager().openInterstitialAds(
                this,
                loggedInUserCacheBase.getInterstitialAdId(),
                object : AdsManager.InterstitialAdsCallback {
                    override fun InterstitialResponse(nativeAd: InterstitialAd?) {
                        nativeAd?.show(this@BasicActivity)
                    }

                    override fun adsOnLoaded() {
                    }
                }
            )
        }
    }
}
