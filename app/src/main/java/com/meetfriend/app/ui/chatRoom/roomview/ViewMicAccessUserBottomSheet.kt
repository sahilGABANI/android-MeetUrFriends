package com.meetfriend.app.ui.chatRoom.roomview

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.GetMiceAccessRequest
import com.meetfriend.app.api.chat.model.MiceAccessInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetViewMiceAccessUserBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.roomview.view.MiceAccessAdapter
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomInfoViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomInfoViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ViewMicAccessUserBottomSheet : BaseBottomSheetDialogFragment() {

    private val removeClickSubject: PublishSubject<MiceAccessInfo> = PublishSubject.create()
    val removeClicks: Observable<MiceAccessInfo> = removeClickSubject.hide()
    private var _binding: BottomSheetViewMiceAccessUserBinding? = null
    private val binding get() = _binding!!

    companion object {

        private const val INTENT_CHAT_ROOM_INFO = "chatRoomInfo"

        fun newInstance(chatRoomInfo: ChatRoomInfo?): ViewMicAccessUserBottomSheet {
            val args = Bundle()
            chatRoomInfo.let { args.putParcelable(INTENT_CHAT_ROOM_INFO, it) }
            val fragment = ViewMicAccessUserBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatRoomInfoViewModel>
    private lateinit var chatRoomInfoViewModel: ChatRoomInfoViewModel

    private lateinit var miceAccessAdapter: MiceAccessAdapter

    lateinit var chatRoomInfo: ChatRoomInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

        MeetFriendApplication.component.inject(this)
        chatRoomInfoViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetViewMiceAccessUserBinding.inflate(inflater, container, false)
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
        chatRoomInfo = arguments?.getParcelable(INTENT_CHAT_ROOM_INFO)
            ?: throw IllegalStateException("No args provided")

        listenToViewEvents()
        listenToViewModel()

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
        miceAccessAdapter = MiceAccessAdapter(requireContext()).apply {
            removeMiceAccessClicks.subscribeAndObserveOnMainThread {
                removeClickSubject.onNext(it)
            }.autoDispose()
        }
        binding.rvMiceAccess.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = miceAccessAdapter
        }

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        chatRoomInfoViewModel.chatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatRoomInfoViewState.MiceAccess -> {
                    miceAccessAdapter.listOfDataItems = it.chatRoomUserInfo
                    if (miceAccessAdapter.listOfDataItems.isNullOrEmpty()) {
                        binding.rvMiceAccess.visibility = View.GONE
                        binding.llEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.rvMiceAccess.visibility = View.VISIBLE
                        binding.llEmptyState.visibility = View.GONE
                    }
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

    override fun onResume() {
        super.onResume()
        chatRoomInfoViewModel.getMiceAccessInfo(GetMiceAccessRequest(chatRoomInfo.id))
    }
}