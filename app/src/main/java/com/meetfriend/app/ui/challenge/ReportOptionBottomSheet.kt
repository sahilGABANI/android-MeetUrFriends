package com.meetfriend.app.ui.challenge

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.challenge.model.ReportChallengePostRequest
import com.meetfriend.app.api.challenge.model.ReportChallengeRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ReportChallengeDialogBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.challenge.view.ReportAdapter
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewModel
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ReportOptionBottomSheet : BaseBottomSheetDialogFragment() {

    private val optionClickSubject: PublishSubject<String> = PublishSubject.create()
    val optionClick: Observable<String> = optionClickSubject.hide()

    private var _binding: ReportChallengeDialogBinding? = null
    private val binding get() = _binding!!

    private var challengeId: Int = 0
    private var challengePostId: Int = 0
    private var listOfReports: ArrayList<String> = arrayListOf()

    private lateinit var reportAdapter: ReportAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChallengeViewModel>
    private lateinit var challengeViewModel: ChallengeViewModel

    companion object {

        private const val INTENT_CHALLENGE_ID = "challengeId"
        private const val INTENT_CHALLENGE_POST_ID = "challengePostId"

        fun newInstance(challengeId: Int, challengePostId: Int?): ReportOptionBottomSheet {
            val args = Bundle()
            challengeId.let { args.putInt(INTENT_CHALLENGE_ID, it) }
            challengePostId?.let { args.putInt(INTENT_CHALLENGE_POST_ID, it) }
            val fragment = ReportOptionBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
        MeetFriendApplication.component.inject(this)
        challengeViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ReportChallengeDialogBinding.inflate(inflater, container, false)
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
        challengePostId = arguments?.getInt(INTENT_CHALLENGE_POST_ID, 0) ?: 0

        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        challengeViewModel.challengeState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChallengeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ChallengeViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                    dismissBottomSheet()
                }
                else -> {}
            }
        }.autoDispose()
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
        listOfReports.add("It's spam")
        listOfReports.add("Nudity or sexual activity")
        listOfReports.add("just don't like it")
        listOfReports.add("Hate speech or symbols")
        listOfReports.add("Violence or dangerous organisations")
        listOfReports.add("Bullying or harassment")
        listOfReports.add("False information")
        listOfReports.add("Sam or fraud")
        listOfReports.add("Suicide or self-injury")
        listOfReports.add("Sale of illegal or regulated goods")
        listOfReports.add("Intellectual property violation")

        reportAdapter = ReportAdapter(requireContext()).apply {
            reportItemClick.subscribeAndObserveOnMainThread {
                showToast(it)
                if (challengePostId == 0) {
                    challengeViewModel.reportChallenge(ReportChallengeRequest(challengeId, it))
                } else {
                    challengeViewModel.reportChallengePost(
                        ReportChallengePostRequest(
                            challengeId,
                            it,
                            challengePostId
                        )
                    )
                }


            }
        }

        binding.reportReasonRecyclerView.apply {
            adapter = reportAdapter
        }

        reportAdapter.listOfReports = listOfReports
    }

    fun dismissBottomSheet() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}