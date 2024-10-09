package com.meetfriend.app.ui.chat.onetoonechatroom

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetForwardPeopleListBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chat.onetoonechatroom.view.ForwardPeopleAdapter
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.ForwardMsgViewModel
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.ForwardMsgViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ForwardUserListBottomSheet : BaseBottomSheetDialogFragment() {

    private val selectedPeopleSubject: PublishSubject<ArrayList<Int>> = PublishSubject.create()
    val selectedPeople: Observable<ArrayList<Int>> = selectedPeopleSubject.hide()

    private var _binding: BottomSheetForwardPeopleListBinding? = null
    private val binding get() = _binding!!

    companion object {

        private const val INTENT_MESSAGE_INFO = "messageInfo"
        private const val INTENT_ROOM_TYPE = "roomType"

        fun newInstanceWithData(
            messageInfo: MessageInfo,
            isNormalRoom: Boolean
        ): ForwardUserListBottomSheet {
            val args = Bundle()
            messageInfo.let { args.putParcelable(INTENT_MESSAGE_INFO, it) }
            args.putBoolean(INTENT_ROOM_TYPE, isNormalRoom)

            val fragment = ForwardUserListBottomSheet()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(messageInfo: MessageInfo): ForwardUserListBottomSheet {
            val args = Bundle()
            messageInfo.let { args.putParcelable(INTENT_MESSAGE_INFO, it) }
            val fragment = ForwardUserListBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ForwardMsgViewModel>
    private lateinit var forwardMsgViewModel: ForwardMsgViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    lateinit var messageInfo: MessageInfo
    var isNormalRoom: Boolean = false
    private lateinit var forwardPeopleAdapter: ForwardPeopleAdapter
    private var forwardPeopleList: ArrayList<Int> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetForwardPeopleListBinding.inflate(inflater, container, false)
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

        MeetFriendApplication.component.inject(this)
        forwardMsgViewModel = getViewModelFromFactory(viewModelFactory)

        messageInfo = arguments?.getParcelable(INTENT_MESSAGE_INFO)
            ?: throw IllegalStateException("No args provided")
        listenToViewModel()
        listenToViewEvents()

        if (isNormalRoom) {
            forwardMsgViewModel.resetPaginationForOneToOneNormalChatRoom()
        } else {
            forwardMsgViewModel.resetPaginationForOneToOneChatRoom()
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

    private fun listenToViewEvents() {
        requireArguments().let {
            isNormalRoom = it.getBoolean(INTENT_ROOM_TYPE)
        }

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()

        binding.ivDone.throttleClicks().subscribeAndObserveOnMainThread {
            if (!forwardPeopleList.isNullOrEmpty()) {
                selectedPeopleSubject.onNext(forwardPeopleList)
            }
            dismissBottomSheet()
        }.autoDispose()

        forwardPeopleAdapter = ForwardPeopleAdapter(requireContext()).apply {
            forwardPeopleClick.subscribeAndObserveOnMainThread {
                if (it.isSelected) {
                    it.isSelected = false
                    forwardPeopleList.remove(it.id)
                    listOfDataItems?.indexOf(it)?.let { it1 -> notifyItemChanged(it1) }
                } else {
                    it.isSelected = true
                    forwardPeopleList.add(it.id)
                    listOfDataItems?.indexOf(it)?.let { it1 -> notifyItemChanged(it1) }
                }
            }.autoDispose()
        }

        binding.rvPeopleList.apply {
            adapter = forwardPeopleAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                if (isNormalRoom) {
                                    forwardMsgViewModel.loadNormalMoreOneToOneChatRoom()
                                } else {
                                    forwardMsgViewModel.loadMoreOneToOneChatRoom()
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    private fun listenToViewModel() {
        forwardMsgViewModel.forwardMsgState.subscribeAndObserveOnMainThread {
            when (it) {
                is ForwardMsgViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is ForwardMsgViewState.LoadingState -> {
                }
                is ForwardMsgViewState.SuccessMessage -> {
                }
                is ForwardMsgViewState.ListOfOneToOneChatRoom -> {
                    forwardPeopleAdapter.listOfDataItems = it.listOfChatRoom
                }
            }
        }.autoDispose()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }
}
