package com.meetfriend.app.ui.livestreaming.inviteuser

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetInviteUserInLiveStreamingBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.livestreaming.inviteuser.view.InviteUserInLiveStreamingAdapter
import com.meetfriend.app.ui.livestreaming.inviteuser.viewmodel.InviteUserInLiveState
import com.meetfriend.app.ui.livestreaming.inviteuser.viewmodel.InviteUserInLiveViewModel
import com.meetfriend.app.ui.messages.viewmodel.CreateMessageState
import com.meetfriend.app.ui.messages.viewmodel.CreateMessageViewModel
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FIX_400_MILLISECOND
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class InviteUserInLiveStreamingBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {

        private const val INTENT_IS_ALLOW_PLAY_GAME = "isAllowPlayGame"

        fun newInstance(isAllowPlayGame: Int): InviteUserInLiveStreamingBottomSheet {
            val args = Bundle()
            isAllowPlayGame.let { args.putInt(INTENT_IS_ALLOW_PLAY_GAME, it) }
            val fragment = InviteUserInLiveStreamingBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private val inviteUpdatedSubject: PublishSubject<Map<Int, MeetFriendUser>> =
        PublishSubject.create()
    val inviteUpdated: Observable<Map<Int, MeetFriendUser>> = inviteUpdatedSubject.hide()

    private var listOfFollowResponse = mutableListOf<MeetFriendUser>()

    @Inject
    internal lateinit var messageViewModelFactory: ViewModelFactory<CreateMessageViewModel>
    private lateinit var createMessageViewModel: CreateMessageViewModel

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<InviteUserInLiveViewModel>
    private lateinit var inviteFriendsLiveStreamViewModel: InviteUserInLiveViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: BottomSheetInviteUserInLiveStreamingBinding? = null
    private val binding get() = _binding!!

    private lateinit var inviteFriendsLiveStreamAdapter: InviteUserInLiveStreamingAdapter

    private var listOfFollowResponseWithIdMain: MutableMap<Int, MeetFriendUser> = mutableMapOf()
    var isAllowPlayGame: Int = 0
    val listOfUser: MutableList<MeetFriendUser> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

        MeetFriendApplication.component.inject(this)
        createMessageViewModel = getViewModelFromFactory(messageViewModelFactory)
        inviteFriendsLiveStreamViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetInviteUserInLiveStreamingBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            }
        }

        isAllowPlayGame = arguments?.getInt(INTENT_IS_ALLOW_PLAY_GAME) ?: 0
        listenToViewModel()
        listenToViewEvent()
        inviteFriendsLiveStreamViewModel.resetPaginationForInviteUser()
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

    private fun listenToViewModel() {
        inviteFriendsLiveStreamViewModel.followerStates.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is InviteUserInLiveState.ErrorMessage -> {
                    showToast(state.errorMessage)
                }
                is InviteUserInLiveState.LoadingState -> {
                }
                is InviteUserInLiveState.SuccessMessage -> {
                    showToast(state.successMessage)
                }
                is InviteUserInLiveState.FollowersData -> {
                    state.followingData?.let {
                        listOfFollowResponse = it as MutableList<MeetFriendUser>
                        updateAdapter(it)
                    }
                }
                else -> {}
            }
        }.autoDispose()

        createMessageViewModel.createChatState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is CreateMessageState.ErrorMessage -> {
                    showToast(state.errorMessage)
                }
                is CreateMessageState.SuccessMessage -> {
                    showToast(state.successMessage)
                }
                is CreateMessageState.UserListForNormalChat -> {
                    state.listOfUserForMention?.let {
                        updateAdapter(it)
                    }
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun updateAdapter(listOfFollowResponse: List<MeetFriendUser>) {
        inviteFriendsLiveStreamAdapter =
            InviteUserInLiveStreamingAdapter(requireContext(), false, isAllowPlayGame, "CreateChat", "").apply {
                inviteUpdated.subscribeAndObserveOnMainThread {
                    if (isAllowPlayGame == 1) {
                        val list = inviteFriendsLiveStreamAdapter.listOfDataItems
                        val data = list?.let { it1 -> list.elementAt(it1.indexOf(it)) }
                        if (it.isInvited) {
                            data?.isInvited = false
                            listOfUser.remove(it)
                            inviteFriendsLiveStreamAdapter.listOfDataItems = list
                        } else {
                            if (listOfUser.size < Constant.FiXED_3_INT) {
                                data?.isInvited = true
                                listOfUser.add(it)
                                inviteFriendsLiveStreamAdapter.listOfDataItems = list
                            }
                        }
                    } else {
                        val list = inviteFriendsLiveStreamAdapter.listOfDataItems
                        val data = list?.let { it1 -> list.elementAt(it1.indexOf(it)) }
                        if (it.isInvited) {
                            data?.isInvited = false
                            listOfUser.remove(it)
                            inviteFriendsLiveStreamAdapter.listOfDataItems = list
                        } else {
                            if (listOfUser.size < Constant.FiXED_3_INT) {
                                data?.isInvited = true
                                listOfUser.add(it)
                                inviteFriendsLiveStreamAdapter.listOfDataItems = list
                            }
                        }
                    }
                }.autoDispose()
            }
        val listOfFollowResponseWithId =
            listOfFollowResponse.map { it.id to it }.toMap().toMutableMap()

        inviteFriendsLiveStreamAdapter.listOfDataItems = listOfFollowResponse
        binding.rvInviteUser.apply {
            adapter = inviteFriendsLiveStreamAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                inviteFriendsLiveStreamViewModel.loadMoreInviteUser()
                            }
                        }
                    }
                }
            })
        }

        if (listOfFollowResponseWithId.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }

    private fun listenToViewEvent() {
        binding.ivDone.throttleClicks().subscribeAndObserveOnMainThread {
            val myMap = listOfUser.map { it.id to it }.toMap()
            listOfFollowResponseWithIdMain.putAll(myMap)
            inviteUpdatedSubject.onNext(listOfFollowResponseWithIdMain.toMap())
        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()

        binding.etSearchLiveStreamingInviteUser.textChanges()
            .debounce(FIX_400_MILLISECOND, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isNotEmpty()) {
                    createMessageViewModel.getUserForNormalChat(it.toString())
                } else {
                    if (listOfFollowResponse.isNotEmpty()) updateAdapter(listOfFollowResponse)
                }
            }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}
