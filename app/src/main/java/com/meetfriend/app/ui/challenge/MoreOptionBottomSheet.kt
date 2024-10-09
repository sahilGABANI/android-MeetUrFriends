package com.meetfriend.app.ui.challenge

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetChallengeMoreOptionBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class MoreOptionBottomSheet : BaseBottomSheetDialogFragment() {

    private val optionClickSubject: PublishSubject<String> = PublishSubject.create()
    val optionClick: Observable<String> = optionClickSubject.hide()

    private var _binding: BottomSheetChallengeMoreOptionBinding? = null
    private val binding get() = _binding!!

    private var challengeId: Int = 0
    private var challengeUserId: Int = 0
    private var isRejectLiveJoin: Boolean = false

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {

        private const val INTENT_CHALLENGE_ID = "challengeId"
        private const val INTENT_CHALLENGE_USER_ID = "challengeUserId"
        private const val INTENT_CHALLENGE_REJECT_OPTION = "isRejectLiveJoin"

        fun newInstance(
            challengeId: Int,
            challengeUserId: Int,
            isRejectLiveJoin: Boolean
        ): MoreOptionBottomSheet {
            val args = Bundle()
            challengeId.let { args.putInt(INTENT_CHALLENGE_ID, it) }
            challengeUserId.let { args.putInt(INTENT_CHALLENGE_USER_ID, it) }
            isRejectLiveJoin.let { args.putBoolean(INTENT_CHALLENGE_REJECT_OPTION, it) }
            val fragment = MoreOptionBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

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
        _binding = BottomSheetChallengeMoreOptionBinding.inflate(inflater, container, false)
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

        challengeId = arguments?.getInt(INTENT_CHALLENGE_ID, 0) ?: 0
        isRejectLiveJoin = arguments?.getBoolean(INTENT_CHALLENGE_REJECT_OPTION, false) ?: false
        challengeUserId = arguments?.getInt(INTENT_CHALLENGE_USER_ID, 0) ?: 0

        binding.llReject.visibility =
            if (isRejectLiveJoin) View.VISIBLE else View.GONE
        binding.llDelete.visibility =
            if (challengeUserId == loggedInUserCache.getLoggedInUserId()) View.VISIBLE else View.GONE
        binding.llReport.visibility =
            if (challengeUserId == loggedInUserCache.getLoggedInUserId()) View.GONE else View.VISIBLE
        listenToViewEvent()
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

    private fun listenToViewEvent() {
        binding.llReject.throttleClicks().subscribeAndObserveOnMainThread {
            optionClickSubject.onNext(binding.rejectAppCompatTextView.text.toString())
            dismissBottomSheet()
        }

        binding.llReport.throttleClicks().subscribeAndObserveOnMainThread {
            optionClickSubject.onNext(binding.reportAppCompatTextView.text.toString())
            dismissBottomSheet()
        }.autoDispose()

        binding.llCopyLink.throttleClicks().subscribeAndObserveOnMainThread {
            optionClickSubject.onNext(binding.copyLinkAppCompatTextView.text.toString())
            dismissBottomSheet()
        }.autoDispose()

        binding.llDelete.throttleClicks().subscribeAndObserveOnMainThread {
            optionClickSubject.onNext(binding.deleteAppCompatTextView.text.toString())
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
}