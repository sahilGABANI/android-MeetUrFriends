package com.meetfriend.app.ui.livestreaming

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.livestreaming.model.LiveSummaryInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetLiveSummaryBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.FileUtils.formatTo
import com.meetfriend.app.utils.FileUtils.toDate
import javax.inject.Inject

class LiveSummaryBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {

        private const val INTENT_LIVE_SUMMARY_INFO = "INTENT_LIVE_SUMMARY_INFO"

        fun newInstance(liveSummaryInfo: LiveSummaryInfo): LiveSummaryBottomSheet {
            val args = Bundle()
            liveSummaryInfo.let { args.putParcelable(INTENT_LIVE_SUMMARY_INFO, it) }

            val fragment = LiveSummaryBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: BottomSheetLiveSummaryBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var liveSummaryInfo: LiveSummaryInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
        MeetFriendApplication.component.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLiveSummaryBinding.inflate(inflater, container, false)
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
        liveSummaryInfo = arguments?.getParcelable(INTENT_LIVE_SUMMARY_INFO) ?: null
        listenToViewEvent()
        setData()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                return
            }
        }

    private fun listenToViewEvent() {
        binding.closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }

    private fun setData() {
        val totalCents =
            (liveSummaryInfo?.centsValue?.toFloat() ?: 0F) * (
                liveSummaryInfo?.totalCoins?.toFloat()
                    ?: 0F
                )
        binding.usernameAppCompatTextView.text =
            loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName

        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        Glide.with(this)
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.profilePicRoundedImageView)

        binding.followersCountAppCompatTextView.text =
            loggedInUserCache.getLoggedInUser()?.loggedInUser?.noOfFollowers?.prettyCount()
                .toString().plus(" ").plus(resources.getString(R.string.followers))
        binding.followingCountAppCompatTextView.text =
            loggedInUserCache.getLoggedInUser()?.loggedInUser?.noOfFollowings?.prettyCount()
                .toString().plus(" ").plus(resources.getString(R.string.following))

        if (!liveSummaryInfo?.startTime.isNullOrEmpty() && liveSummaryInfo?.startTime != "") {
            binding.startTimeTextView.text =
                liveSummaryInfo?.startTime?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    ?.formatTo("hh:mm:ss a")
        }
        if (!liveSummaryInfo?.endTime.isNullOrEmpty() && liveSummaryInfo?.endTime != "") {
            binding.endTimeTextView.text =
                liveSummaryInfo?.endTime?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    ?.formatTo("hh:mm:ss a")
        }
        binding.totalTimeAppCompatTextView.text = liveSummaryInfo?.totalTime
        binding.totalLikeAppCompatTextView.text =
            liveSummaryInfo?.totalLikes?.prettyCount().toString()
        binding.totalCommentsTextView.text =
            liveSummaryInfo?.totalComments?.prettyCount().toString()
        binding.totalViewersTextView.text = liveSummaryInfo?.totalViewers?.prettyCount().toString()
        binding.totalGiftsAppCompatTextView.text =
            liveSummaryInfo?.totalGifts?.toInt()?.prettyCount().toString()
        binding.totalCoinsAppCompatTextView.text =
            liveSummaryInfo?.totalCoins?.toInt()?.prettyCount().toString()
        binding.totalCentsAppCompatTextView.text = totalCents.toString()
    }
}
