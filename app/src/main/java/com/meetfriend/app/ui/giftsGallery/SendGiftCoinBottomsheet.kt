package com.meetfriend.app.ui.giftsGallery

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.meetfriend.app.R
import com.meetfriend.app.api.monetization.model.SendCoinRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomsheetSendCoinBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.mygifts.viewmodel.GiftTransactionViewState
import com.meetfriend.app.ui.mygifts.viewmodel.GiftsTransactionViewModel
import com.meetfriend.app.utilclasses.CallProgressWheel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class SendGiftCoinBottomsheet(var totalCoins : String,var user_id : String) : BaseBottomSheetDialogFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GiftsTransactionViewModel>
    private lateinit var giftsTransactionViewModel: GiftsTransactionViewModel

    private var _binding: BottomsheetSendCoinBinding? = null
    private val binding get() = _binding!!

    private val sendGiftCoinSubject: PublishSubject<Unit> = PublishSubject.create()
    val onClick: Observable<Unit> = sendGiftCoinSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        MeetFriendApplication.component.inject(this)
        giftsTransactionViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSendCoinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setOnKeyListener { _, p1, _ -> p1 == KeyEvent.KEYCODE_BACK }
        }
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        giftsTransactionViewModel.giftTransactionState.subscribeAndObserveOnMainThread {
            when(it){
                is GiftTransactionViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is GiftTransactionViewState.LoadingState -> {
                    showLoading(it.isLoading)
                }
                is GiftTransactionViewState.SuccessMessage -> {
                    dismissBottomSheet()
                    sendGiftCoinSubject.onNext(Unit)
                    showToast(it.successMessage)
                }
                else -> {}
            }


        }.autoDispose()
    }

    private fun listenToViewEvent() {
        binding.availableCoinsAppCompatTextView.text = totalCoins

        binding.tvSend.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.etEnterCoins.text.toString().isNotEmpty()) {
                if (binding.etEnterCoins.text.toString().toInt() == 0) {
                    showToast("Coins amount must be greater than 0.")
                } else {
                    if (totalCoins.toDouble() >= binding.etEnterCoins.text.toString().toDouble()) {
                        giftsTransactionViewModel.sendCoins(
                            SendCoinRequest(
                                binding.etEnterCoins.text.toString(),
                                user_id
                            )
                        )
                    } else {
                        showToast("Coins must be less than available coins.")
                    }
                }
            } else {
                showToast("Please enter coins")
            }
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    fun showLoading(show: Boolean?) {
        if (show!!) showLoading() else hideLoading()
    }

    fun showLoading() {
        CallProgressWheel.showLoadingDialog(requireActivity())
    }

    fun hideLoading() {
        CallProgressWheel.dismissLoadingDialog()
    }

}