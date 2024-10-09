package com.meetfriend.app.ui.livestreaming

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.api.livestreaming.model.LivePopupType
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.DialogLiveStreamAccpetRejectBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.livestreaming.viewmodel.UpdateInviteCoHostStatusViewModel
import com.meetfriend.app.utils.Constant.FiXED_100_INT
import com.meetfriend.app.utils.Constant.FiXED_90_INT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class LiveStreamAcceptRejectDialog(
    private val liveEventInfo: LiveEventInfo,
    private val isFrom: LivePopupType
) : BaseDialogFragment() {

    private var _binding: DialogLiveStreamAccpetRejectBinding? = null
    private val binding get() = _binding!!

    private val inviteCoHostStatusSubject: PublishSubject<Boolean> = PublishSubject.create()
    val inviteCoHostStatus: Observable<Boolean> = inviteCoHostStatusSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UpdateInviteCoHostStatusViewModel>
    private lateinit var updateInviteCoHostStatusViewModel: UpdateInviteCoHostStatusViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
        MeetFriendApplication.component.inject(this)
        updateInviteCoHostStatusViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogLiveStreamAccpetRejectBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewModel()
        listenToViewEvent()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setWidthHeightPercent(FiXED_90_INT)
    }

    private fun DialogFragment.setWidthHeightPercent(percentageWidthInt: Int) {
        val percentageWidth = percentageWidthInt.toFloat() / FiXED_100_INT
        val dm = resources.displayMetrics
        val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
        val percentWidth = rect.width() * percentageWidth
        dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun listenToViewModel() {
        binding.ivUserProfile.visibility =
            if (liveEventInfo.profilePhoto.isNullOrEmpty()) View.GONE else View.VISIBLE
        Glide.with(requireContext())
            .load(liveEventInfo.profilePhoto ?: "")
            .placeholder(R.drawable.image_placeholder)
            .into(binding.ivUserProfile)

        binding.tvUsername.text = liveEventInfo.userName ?: ""

        if (liveEventInfo.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        binding.tvAccept.throttleClicks().subscribeAndObserveOnMainThread {
            inviteCoHostStatusSubject.onNext(true)
        }.autoDispose()

        binding.tvReject.throttleClicks().subscribeAndObserveOnMainThread {
            updateInviteCoHostStatusViewModel.rejectAsCoHost(liveEventInfo.id.toString())
        }.autoDispose()

        when (isFrom) {
            LivePopupType.JoinLive -> {
                binding.tvEndLive.isVisible = true
                binding.tvAccept.text = getString(R.string.label_join_live)
                binding.tvReject.text = resources.getString(R.string.label_not_now)
            }
            LivePopupType.JoinVideoCall -> {
            }
            LivePopupType.JoinAsCoHost -> {
            }
        }
    }

    private fun listenToViewEvent() {
        updateInviteCoHostStatusViewModel.updateInviteCoHostStatusStates.subscribeAndObserveOnMainThread {
            when (it) {
                is UpdateInviteCoHostStatusViewModel.UpdateInviteCoHostStatus.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is UpdateInviteCoHostStatusViewModel.UpdateInviteCoHostStatus.LoadingSettingState -> {
                    rejectButtonVisibility(it.isLoading)
                }
                UpdateInviteCoHostStatusViewModel.UpdateInviteCoHostStatus.RejectedCoHostRequest -> {
                    inviteCoHostStatusSubject.onNext(false)
                }
                else -> {
                }
            }
        }.autoDispose()
    }

    private fun rejectButtonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.llAcceptRejectButtons.visibility = View.INVISIBLE
        } else {
            binding.llAcceptRejectButtons.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        }
    }

    fun dismissDialog() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
