package com.meetfriend.app.ui.home.shorts.report

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.post.model.PostReportRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ReportChallengeDialogBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.challenge.view.ReportAdapter
import com.meetfriend.app.ui.home.shorts.report.viewmodel.ReportDialogViewModel
import com.meetfriend.app.ui.home.shorts.report.viewmodel.ReportDialogViewState
import com.meetfriend.app.utils.Constant.DISMISS
import com.meetfriend.app.utils.Constant.STORY
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ReportDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val INTENT_POST_ID = "postId"
        private const val INTENT_REPORT_LIVE = "reportLive"
        private const val REPORT_FOR = "REPORT_FOR"

        fun newInstance(
            postId: Int,
            isFromLive: Boolean,
            reportFor: String
        ): ReportDialogFragment {
            val args = Bundle()
            postId.let { args.putInt(INTENT_POST_ID, it) }
            isFromLive.let { args.putBoolean(INTENT_REPORT_LIVE, it) }
            reportFor.let { args.putString(REPORT_FOR, it) }
            val fragment = ReportDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: ReportChallengeDialogBinding? = null
    private val binding get() = _binding!!
    private var reportFor = ""

    private val optionClickSubject: PublishSubject<String> = PublishSubject.create()
    val optionClick: Observable<String> = optionClickSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReportDialogViewModel>
    lateinit var reportDialogViewModel: ReportDialogViewModel

    private var postId: Int = 0
    private var isFromLive: Boolean = false
    private var listOfReports: ArrayList<String> = arrayListOf()
    private lateinit var reportAdapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
        MeetFriendApplication.component.inject(this)
        reportDialogViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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

        postId =
            arguments?.getInt(INTENT_POST_ID) ?: throw IllegalStateException("No args provided")
        isFromLive = arguments?.getBoolean(INTENT_REPORT_LIVE)
            ?: throw IllegalStateException("No args provided")
        reportFor =
            arguments?.getString(REPORT_FOR) ?: throw IllegalStateException("No args provided")

        listenToViewModel()
        listenToViewEvent()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (reportFor == STORY) {
                        optionClickSubject.onNext(DISMISS)
                    }
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                return
            }
        }

    private fun listenToViewModel() {
        reportDialogViewModel.reportDialogState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReportDialogViewState.ErrorMessage -> {
                    if (reportFor == STORY) {
                        optionClickSubject.onNext(DISMISS)
                    }
                    dismiss()
                    showToast(it.errorMessage)
                }

                is ReportDialogViewState.LoadingState -> {
                }

                is ReportDialogViewState.SuccessMessage -> {
                    if (reportFor == STORY) {
                        optionClickSubject.onNext(DISMISS)
                    } else {
                        optionClickSubject.onNext(postId.toString())
                    }
                    dismiss()
                    showToast(it.successMessage)
                }
            }
        }.autoDispose()
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
                if (isFromLive) {
                    reportLive(postId, it)
                } else if (reportFor == STORY) {
                    reportDialogViewModel.reportStory(postId.toString(), it)
                } else {
                    reportPostApi(postId, it)
                }
            }
        }

        binding.reportReasonRecyclerView.apply {
            adapter = reportAdapter
        }

        reportAdapter.listOfReports = listOfReports
    }

    private fun reportLive(liveId: Int, reason: String) {
        reportDialogViewModel.reportLiveStreaming(liveId, reason)
    }

    private fun reportPostApi(postId: Int, reason: String) {
        val postReportRequest = PostReportRequest(postId, reason, "report")
        reportDialogViewModel.reportOrHidePost(postReportRequest)
    }
}
