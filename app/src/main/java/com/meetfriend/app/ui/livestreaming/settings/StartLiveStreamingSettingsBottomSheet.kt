package com.meetfriend.app.ui.livestreaming.settings

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding3.widget.checkedChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.livestreaming.model.CreateLiveEventRequest
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetStartLiveStreamingSettingsBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.livestreaming.settings.viewmodel.LiveStreamCreateEventSettingState
import com.meetfriend.app.ui.livestreaming.settings.viewmodel.LiveStreamCreateEventSettingViewModel
import com.meetfriend.app.utils.Constant
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class StartLiveStreamingSettingsBottomSheet : BaseBottomSheetDialogFragment() {

    private val liveNowSuccessSubject: PublishSubject<LiveEventInfo> = PublishSubject.create()
    val liveNowSuccess: Observable<LiveEventInfo> = liveNowSuccessSubject.hide()

    private val closeIconClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val closeIconClick: Observable<Unit> = closeIconClickSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LiveStreamCreateEventSettingViewModel>
    private lateinit var liveStreamCreateEventSettingViewModel: LiveStreamCreateEventSettingViewModel

    private var _binding: BottomSheetStartLiveStreamingSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        MeetFriendApplication.component.inject(this)
        liveStreamCreateEventSettingViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetStartLiveStreamingSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnKeyListener { _, p1, _ -> p1 == KeyEvent.KEYCODE_BACK }
        }

        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        liveStreamCreateEventSettingViewModel.liveStreamCreateEventSettingStates.subscribeAndObserveOnMainThread {
            when (it) {
                is LiveStreamCreateEventSettingState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is LiveStreamCreateEventSettingState.LoadCreateEventInfo -> {
                    liveNowSuccessSubject.onNext(it.liveEventInfo)
                }
                is LiveStreamCreateEventSettingState.LoadingSettingState -> {
                    buttonVisibility(it.isLoading)
                }
                is LiveStreamCreateEventSettingState.SuccessMessage -> {
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        binding.switchMakePrivate.checkedChanges().subscribeAndObserveOnMainThread {
            if (it) {
                binding.etPassword.visibility = View.VISIBLE
            } else {
                binding.etPassword.visibility = View.GONE
            }
        }.autoDispose()

        binding.tvCreate.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                createLiveEvent()
            }
            openInterstitialAds()
            Constant.CLICK_COUNT++
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            closeIconClickSubject.onNext(Unit)
            openInterstitialAds()
            Constant.CLICK_COUNT++
        }.autoDispose()
    }

    private fun createLiveEvent() {
        val isLock = if (binding.switchMakePrivate.isChecked) {
            1
        } else {
            0
        }

        val allowPlayGame = if (binding.switchAllowToPlayGame.isChecked) {
            1
        } else {
            0
        }
        liveStreamCreateEventSettingViewModel.createLiveEvent(
            CreateLiveEventRequest(
                eventName = binding.etEventName.text.toString(),
                isLock = isLock,
                password = binding.etPassword.text.toString(),
                inviteIds = "",
                isAllowPlayGame = allowPlayGame
            )
        )
    }

    private fun isValidate(): Boolean {
        var isValidate = true
        if (binding.etEventName.text.toString().isEmpty()) {
            showToast(resources.getString(R.string.msg_enter_event_name))
            isValidate = false
        } else if (binding.switchMakePrivate.isChecked) {
            if (binding.etPassword.text.toString().isEmpty()) {
                showToast(resources.getString(R.string.empty_password))
                isValidate = false
            }
        }
        return isValidate
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.llBtnCreateCancel.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.llBtnCreateCancel.visibility = View.VISIBLE
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
