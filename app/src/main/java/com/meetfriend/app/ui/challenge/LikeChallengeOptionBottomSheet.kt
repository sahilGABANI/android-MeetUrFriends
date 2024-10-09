package com.meetfriend.app.ui.challenge

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.challenge.model.ChallengeUserModel
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.LikeChallengeOptionBottomsheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.challenge.view.ChallengeLikeUserAdapter
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewModel
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewState
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LikeChallengeOptionBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: LikeChallengeOptionBottomsheetBinding? = null
    private val binding get() = _binding!!
    private var challengeId: Int = 0

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChallengeViewModel>
    private lateinit var challengeViewModel: ChallengeViewModel

    private lateinit var challengeLikeUserAdapter: ChallengeLikeUserAdapter
    private  var allUsers: List<ChallengeUserModel> = listOf()
    companion object {

        private const val INTENT_CHALLENGE_ID = "challengeId"

        fun newInstance(challengeId: Int): LikeChallengeOptionBottomSheet {
            val args = Bundle()
            challengeId.let { args.putInt(INTENT_CHALLENGE_ID, it) }
            val fragment = LikeChallengeOptionBottomSheet()
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
        _binding = LikeChallengeOptionBottomsheetBinding.inflate(inflater, container, false)
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

        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        challengeViewModel.challengeState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChallengeViewState.LoadingState -> {}
                is ChallengeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ChallengeViewState.ListOfLikedUserInfo -> {
                    challengeLikeUserAdapter.listOfLikedUser = it.listOfAllChallenge
                    allUsers = it.listOfAllChallenge // Initialize the allUsers list with the received data

                }
                else -> {}
            }
        }
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
        challengeViewModel.resetPaginationLikedUserChallenge(challengeId)
        binding.closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()

        challengeLikeUserAdapter = ChallengeLikeUserAdapter(requireContext()).apply { }
        binding.reelsRecyclerView.apply {
            adapter = challengeLikeUserAdapter
        }

        binding.searchAppCompatEditText.textChanges()
            .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isNotEmpty()) {
                    val filteredUsers = allUsers.filter { userModel ->
                        userModel.user?.userName?.contains(it, ignoreCase = true) == true
                    }
                    challengeLikeUserAdapter.listOfLikedUser = filteredUsers
                } else {
                    challengeLikeUserAdapter.listOfLikedUser = allUsers
                }
                challengeLikeUserAdapter.notifyDataSetChanged()
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