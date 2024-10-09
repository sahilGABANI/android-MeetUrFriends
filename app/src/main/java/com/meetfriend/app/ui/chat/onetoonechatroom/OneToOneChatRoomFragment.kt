package com.meetfriend.app.ui.chat.onetoonechatroom

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomListItemActionState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentOneToOneChatRoomBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.chat.onetoonechatroom.view.OneToOneChatRoomAdapter
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.OneToOneChatRoomViewModel
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.OneToOneChatRoomViewState
import com.meetfriend.app.utils.FileUtils
import javax.inject.Inject

class OneToOneChatRoomFragment : BasicFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = OneToOneChatRoomFragment()
    }

    private var _binding: FragmentOneToOneChatRoomBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OneToOneChatRoomViewModel>
    private lateinit var oneToOneChatRoomViewModel: OneToOneChatRoomViewModel

    private lateinit var oneToOneChatRoomAdapter: OneToOneChatRoomAdapter

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var position = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOneToOneChatRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        oneToOneChatRoomViewModel = getViewModelFromFactory(viewModelFactory)
        listenToViewModel()
        listenToViewEvent()
        oneToOneChatRoomViewModel.resetPaginationForOneToOneChatRoom()
    }

    private fun listenToViewEvent() {
        oneToOneChatRoomAdapter = OneToOneChatRoomAdapter(requireContext()).apply {
            chatRoomItemClick.subscribeAndObserveOnMainThread {
                when (it) {
                    is ChatRoomListItemActionState.ContainerClick -> {
                        startActivity(
                            ViewOneToOneChatRoomActivity.getIntent(
                                requireContext(), it.chatRoomInfo, false
                            )
                        )
                    }
                    is ChatRoomListItemActionState.DeleteClick -> {
                        position = listOfDataItems?.indexOf(it.chatRoomInfo) ?: 0
                        deleteAlertDialog(it.chatRoomInfo)
                    }
                    else -> {}
                }
            }
        }
        binding.rvChatRoomList.apply {
            adapter = oneToOneChatRoomAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                oneToOneChatRoomViewModel.loadMoreOneToOneChatRoom()
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            oneToOneChatRoomViewModel.resetPaginationForOneToOneChatRoom()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        oneToOneChatRoomViewModel.oneToOneChatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is OneToOneChatRoomViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is OneToOneChatRoomViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is OneToOneChatRoomViewState.SuccessMessage -> {
                }
                is OneToOneChatRoomViewState.ListOfOneToOneChatRoom -> {
                    hideLoading()
                    if (it.listOfChatRoom.isEmpty()) {
                        binding.llEmptyState.visibility = View.VISIBLE
                        binding.rvChatRoomList.visibility = View.GONE
                    } else {
                        binding.rvChatRoomList.visibility = View.VISIBLE
                        binding.llEmptyState.visibility = View.GONE
                        oneToOneChatRoomAdapter.listOfDataItems = it.listOfChatRoom
                    }
                }
                is OneToOneChatRoomViewState.DeleteChatRoom -> {
                    val list = oneToOneChatRoomAdapter.listOfDataItems as ArrayList
                    list.removeAt(position)
                    if (isLastItemVisible()) oneToOneChatRoomViewModel.resetPaginationForOneToOneChatRoom()
                    oneToOneChatRoomAdapter.listOfDataItems = list
                }
                is OneToOneChatRoomViewState.DeleteLoadingState -> {
                }
                is OneToOneChatRoomViewState.UnReadMessageState -> {
                    RxBus.publish(RxEvent.UnreadMessageBadge(it.count))
                }
            }
        }.autoDispose()
    }

    private fun isLastItemVisible(): Boolean {
        val layoutManager = binding.rvChatRoomList.layoutManager as LinearLayoutManager
        val position = layoutManager.findLastVisibleItemPosition()
        return position >= oneToOneChatRoomAdapter.itemCount - 1
    }

    private fun buttonVisibility(loading: Boolean) {
        if ((oneToOneChatRoomAdapter.listOfDataItems?.size ?: 0) == 0) {
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

    private fun deleteAlertDialog(pos: ChatRoomInfo) {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Delete")
        alertDialog.setMessage("Are you sure want to delete?")

        alertDialog.setPositiveButton("Yes") { _: DialogInterface, _: Int ->
            oneToOneChatRoomViewModel.deleteChatRoom(pos.id)
        }

        alertDialog.setNegativeButton("No") { _: DialogInterface?, _: Int ->
        }
        val alert: AlertDialog = alertDialog.create()
        alert.show()
    }

    override fun onResume() {
        super.onResume()
        oneToOneChatRoomViewModel.resetPaginationForOneToOneChatRoom()
    }
}
