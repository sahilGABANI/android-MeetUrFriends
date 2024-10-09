package com.meetfriend.app.ui.chatRoom.profile

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.profile.model.ProfileItemInfo
import com.meetfriend.app.api.profile.model.ProfileMoreActionState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.OtherUserMoreBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class OtherUserMoreBottomSheet : BaseBottomSheetDialogFragment() {

    private val profileMoreOptionStateSubject: PublishSubject<ProfileMoreActionState> =
        PublishSubject.create()
    val profileMoreOptionState: Observable<ProfileMoreActionState> =
        profileMoreOptionStateSubject.hide()

    companion object {

        private const val INTENT_PROFILE_ITEM_INFO = "userProfileInfo"

        fun newInstance(profileItemInfo: ProfileItemInfo?): OtherUserMoreBottomSheet {
            val args = Bundle()
            profileItemInfo.let { args.putParcelable(INTENT_PROFILE_ITEM_INFO, it) }
            val fragment = OtherUserMoreBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: OtherUserMoreBottomSheetBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    lateinit var profileItemInfo: ProfileItemInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OtherUserMoreBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        MeetFriendApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()


        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        profileItemInfo = arguments?.getParcelable(INTENT_PROFILE_ITEM_INFO)
            ?: throw IllegalStateException("No args provided")

        listenToViewEvents()

    }

    private fun listenToViewEvents() {

        binding.llDetetePost.throttleClicks().subscribeAndObserveOnMainThread {
            profileMoreOptionStateSubject.onNext(ProfileMoreActionState.DeleteClick)
            dismissBottomSheet()
        }

        binding.llSharePost.throttleClicks().subscribeAndObserveOnMainThread {
            profileMoreOptionStateSubject.onNext(ProfileMoreActionState.ShareClick)
            dismissBottomSheet()
        }

        binding.llReport.throttleClicks().subscribeAndObserveOnMainThread {
            profileMoreOptionStateSubject.onNext(ProfileMoreActionState.ReportClick)
            dismissBottomSheet()

        }

        if (profileItemInfo.userId == loggedInUserId) {
            binding.llDetetePost.visibility = View.VISIBLE
            binding.llReport.visibility = View.GONE
        } else {
            binding.llDetetePost.visibility = View.GONE
            binding.llSharePost.visibility = View.GONE
            binding.llReport.visibility = View.VISIBLE
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


    private fun dismissBottomSheet() {
        dismiss()
    }
}