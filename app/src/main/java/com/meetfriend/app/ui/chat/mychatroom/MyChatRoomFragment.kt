package com.meetfriend.app.ui.chat.mychatroom

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomListItemActionState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentMyChatRoomBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.chat.mychatroom.view.MyChatRoomListAdapter
import com.meetfriend.app.ui.chat.mychatroom.viewmodel.MyChatRoomViewModel
import com.meetfriend.app.ui.chat.mychatroom.viewmodel.MyChatRoomViewState
import com.meetfriend.app.ui.chatRoom.create.UpdateSubscriptionBottomSheet
import com.meetfriend.app.ui.chatRoom.roomview.ViewChatRoomActivity
import com.meetfriend.app.utils.FileUtils
import javax.inject.Inject

class MyChatRoomFragment : BasicFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = MyChatRoomFragment()
    }

    private var _binding: FragmentMyChatRoomBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MyChatRoomViewModel>
    private lateinit var myChatRoomViewModel: MyChatRoomViewModel

    private lateinit var myChatRoomAdapter: MyChatRoomListAdapter

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyChatRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        myChatRoomViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        listenToViewEvent()
        myChatRoomViewModel.resetPaginationForMyChatRoom()
    }

    private fun listenToViewEvent() {
        myChatRoomAdapter = MyChatRoomListAdapter(requireContext()).apply {
            chatRoomItemClick.subscribeAndObserveOnMainThread {
                when (it) {
                    is ChatRoomListItemActionState.ContainerClick -> {
                        if (it.chatRoomInfo.roomType == 0 || it.chatRoomInfo.roomType == 2) {
                            startActivity(
                                ViewChatRoomActivity.getIntent(
                                    requireContext(),
                                    it.chatRoomInfo
                                )
                            )
                        } else {
                            if (it.chatRoomInfo.isJoin == true) {
                                if (it.chatRoomInfo.is_expired == true) {
                                    val subscriptionBottomSheet =
                                        UpdateSubscriptionBottomSheet.newInstance(it.chatRoomInfo)
                                    subscriptionBottomSheet.show(
                                        childFragmentManager,
                                        UpdateSubscriptionBottomSheet::class.java.name
                                    )
                                } else {
                                    if (it.chatRoomInfo.isKickout == false) {
                                        startActivity(
                                            ViewChatRoomActivity.getIntent(
                                                requireContext(),
                                                it.chatRoomInfo
                                            )
                                        )
                                    } else {
                                        showToast("You have kicked out from this room")
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        binding.rvChatRoomList.apply {
            adapter = myChatRoomAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                myChatRoomViewModel.loadMoreMyChatRoom()
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            myChatRoomViewModel.resetPaginationForMyChatRoom()
        }.autoDispose()
        RxBus.listen(RxEvent.CreateChatRoom::class.java).subscribeAndObserveOnMainThread {
            myChatRoomViewModel.resetPaginationForMyChatRoom()
        }.autoDispose()
        RxBus.listen(RxEvent.SubscriptionSuccessFull::class.java).subscribeAndObserveOnMainThread {
            myChatRoomViewModel.resetPaginationForMyChatRoom()
        }.autoDispose()
        RxBus.listen(RxEvent.DeleteChatRoom::class.java).subscribeAndObserveOnMainThread {
            myChatRoomViewModel.resetPaginationForMyChatRoom()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        myChatRoomViewModel.myChatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is MyChatRoomViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is MyChatRoomViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is MyChatRoomViewState.SuccessMessage -> {
                }
                is MyChatRoomViewState.ListOfMyChatRoom -> {
                    hideLoading()
                    if (it.listOfChatRoom.isEmpty()) {
                        binding.llEmptyState.visibility = View.VISIBLE
                        binding.rvChatRoomList.visibility = View.GONE
                    } else {
                        binding.rvChatRoomList.visibility = View.VISIBLE
                        binding.llEmptyState.visibility = View.GONE
                        myChatRoomAdapter.listOfDataItems = it.listOfChatRoom
                    }
                }
            }
        }.autoDispose()
    }

    private fun buttonVisibility(loading: Boolean) {
        if (myChatRoomAdapter.listOfDataItems?.size ?: 0 == 0) {
            if (loading) showLoading() else hideLoading()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == FileUtils.PICK_IMAGE) && (resultCode == Activity.RESULT_OK)) {
            data?.data?.also {
                for (fragment in childFragmentManager.fragments) {
                    fragment.onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }
}
