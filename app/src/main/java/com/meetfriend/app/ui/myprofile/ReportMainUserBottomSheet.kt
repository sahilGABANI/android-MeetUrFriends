package com.meetfriend.app.ui.myprofile

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetReportUserBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.myprofile.report.viewmodel.ReportMainUserViewModel
import javax.inject.Inject

class ReportMainUserBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetReportUserBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val INTENT_USER_ID = "userId"
        private const val INTENT_IS_FROM_CHATROOM_PROFILE = "isFromChatRoomProfile"


        fun newInstance(userId: Int, isFromChatRoomProfile: Boolean): ReportMainUserBottomSheet {
            val args = Bundle()
            userId.let { args.putInt(INTENT_USER_ID, it) }
            isFromChatRoomProfile.let { args.putBoolean(INTENT_IS_FROM_CHATROOM_PROFILE, it) }
            val fragment = ReportMainUserBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReportMainUserViewModel>
    lateinit var reportUserViewModel: ReportMainUserViewModel

    private var userId: Int = 0
    lateinit var selectReason: String
    var isFromProfile = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
        MeetFriendApplication.component.inject(this)
        reportUserViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReportUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
        loadDataFromIntent()
        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        reportUserViewModel.reportUserState.subscribeAndObserveOnMainThread {
            when (it) {
                is com.meetfriend.app.ui.myprofile.report.viewmodel.ReportUserViewState.ReportUserSuccess -> {
                    dismissBottomSheet()
                }
                is com.meetfriend.app.ui.myprofile.report.viewmodel.ReportUserViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun loadDataFromIntent() {
        userId = arguments?.getInt(INTENT_USER_ID, 0) ?: 0
        isFromProfile = arguments?.getBoolean(INTENT_IS_FROM_CHATROOM_PROFILE) ?: false

    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private fun listenToViewEvents() {
        binding.tvSpam.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvSpam.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvBullying.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvBullying.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvNudity.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvNudity.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvDoNotLike.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvDoNotLike.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvHateSpeech.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvHateSpeech.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvViolence.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvViolence.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvFalseInfo.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvFalseInfo.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvScam.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvScam.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvSuicide.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvSuicide.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvSaleIllegal.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvSaleIllegal.text.toString()
            apiCall()
        }.autoDispose()
        binding.tvIntellectualProperty.throttleClicks().subscribeAndObserveOnMainThread {
            selectReason = binding.tvIntellectualProperty.text.toString()
            apiCall()
        }.autoDispose()
    }

    private fun apiCall() {
        if (isFromProfile) {
            showToast(getString(R.string.labek_profile_photo_updated_successfully))
            dismissBottomSheet()
        } else {
            if (userId != 0) {
                reportUserViewModel.reportUser(userId, selectReason)
            }
        }
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

}