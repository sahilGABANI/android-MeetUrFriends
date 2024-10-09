package com.meetfriend.app.ui.chat.joinchatroom

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomListItemActionState
import com.meetfriend.app.api.chat.model.SendJoinChatRoomRequestRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentJoinChatRoomBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.chatRoom.create.UpdateSubscriptionBottomSheet
import com.meetfriend.app.ui.chatRoom.roomview.ViewChatRoomActivity
import com.meetfriend.app.ui.chatRoom.view.ChatRoomAdapter
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewState
import com.meetfriend.app.utilclasses.ads.AdsManager
import com.meetfriend.app.utils.FileUtils
import timber.log.Timber
import javax.inject.Inject

class JoinChatRoomFragment : BasicFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = JoinChatRoomFragment()
    }

    private var _binding: FragmentJoinChatRoomBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatRoomViewModel>
    private lateinit var chatRoomViewModel: ChatRoomViewModel

    private lateinit var chatRoomAdapter: ChatRoomAdapter
    var selectedTab: Int = 0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinChatRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MeetFriendApplication.component.inject(this)
        chatRoomViewModel = getViewModelFromFactory(viewModelFactory)
        listenToViewModel()
        listenToViewEvent()
        callAPIForRefresh()
    }

    private fun listenToViewEvent() {
        chatRoomAdapter = ChatRoomAdapter(requireContext()).apply {
            chatRoomItemClick.subscribeAndObserveOnMainThread { action ->
                when (action) {
                    is ChatRoomListItemActionState.ContainerClick -> handleContainerClick(action)
                    is ChatRoomListItemActionState.JoinClick -> handleJoinClick(action)
                    else -> {}
                }
            }
        }
        binding.rvChatRoomList.apply {
            adapter = chatRoomAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                if (selectedTab == 1) {
                                    chatRoomViewModel.loadMorePrivateChatList()
                                } else {
                                    chatRoomViewModel.loadMorePublicChatList()
                                }
                            }
                        }
                    }
                }
            })
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        tab.text = getString(R.string.label_public)
                        chatRoomViewModel.resetPaginationForPublicChatRoom()
                    }
                    1 -> {
                        tab.text = getString(R.string.label_private)
                        chatRoomViewModel.resetPaginationForPrivateChatRoom()
                    }
                }
                selectedTab = binding.tabLayout.selectedTabPosition
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                return
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                return
            }
        })

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            callAPIForRefresh()
        }.autoDispose()
    }

    private fun handleContainerClick(action: ChatRoomListItemActionState.ContainerClick) {
        when (action.chatRoomInfo.roomType) {
            0 -> handleRoomType0(action)
            1 -> handleRoomType1(action)
            2 -> startActivity(ViewChatRoomActivity.getIntent(requireContext(), action.chatRoomInfo))
        }
    }

    private fun handleRoomType0(action: ChatRoomListItemActionState.ContainerClick) {
        if (action.chatRoomInfo.isKickout == true) {
            showToast(getString(R.string.toast_you_are_kickout))
        } else {
            AdsManager.getAdsManager().openInterstitialAds(
                requireActivity(),
                loggedInUserCache.getInterstitialAdId(),
                getInterstitialCallback(action)
            )
        }
    }

    private fun handleRoomType1(action: ChatRoomListItemActionState.ContainerClick) {
        if (action.chatRoomInfo.isJoin == true) {
            if (action.chatRoomInfo.is_expired == true) {
                showUpdateSubscriptionBottomSheet(action.chatRoomInfo)
            } else if (action.chatRoomInfo.isKickout == false) {
                AdsManager.getAdsManager().openInterstitialAds(
                    requireActivity(),
                    loggedInUserCache.getInterstitialAdId(),
                    getInterstitialCallback(action)
                )
            } else {
                showToast(getString(R.string.toast_you_are_kickout))
            }
        }
    }

    private fun showUpdateSubscriptionBottomSheet(chatRoomInfo: ChatRoomInfo) {
        val subscriptionBottomSheet = UpdateSubscriptionBottomSheet.newInstance(chatRoomInfo)
        subscriptionBottomSheet.show(childFragmentManager, UpdateSubscriptionBottomSheet::class.java.name)
    }

    private fun getInterstitialCallback(it: ChatRoomListItemActionState.ContainerClick) =
        object : AdsManager.InterstitialAdsCallback {
            override fun InterstitialResponse(nativeAd: InterstitialAd?) {
                if (nativeAd != null) {
                    nativeAd.show(requireActivity())
                    nativeAd.fullScreenContentCallback =
                        object :
                            FullScreenContentCallback() {
                            override fun onAdClicked() {
                                Timber.tag("Ads")
                                    .d("Ad was clicked.")
                            }

                            override fun onAdDismissedFullScreenContent() {
                                startActivity(
                                    ViewChatRoomActivity.getIntent(
                                        requireContext(),
                                        it.chatRoomInfo
                                    )
                                )
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: AdError
                            ) {
                                Timber.tag("Ads")
                                    .e("Ad failed to show fullscreen content.")
                                startActivity(
                                    ViewChatRoomActivity.getIntent(
                                        requireContext(),
                                        it.chatRoomInfo
                                    )
                                )
                            }

                            override fun onAdImpression() {
                                Timber.tag("Ads")
                                    .d("Ad recorded an impression.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                Timber.tag("Ads")
                                    .d("Ad showed fullscreen content.")
                            }
                        }
                } else {
                    Timber.tag("Ads")
                        .d("The interstitial ad wasn't ready yet.")
                    startActivity(
                        ViewChatRoomActivity.getIntent(
                            requireContext(),
                            it.chatRoomInfo
                        )
                    )
                }
            }

            override fun adsOnLoaded() {
                startActivity(
                    ViewChatRoomActivity.getIntent(
                        requireContext(),
                        it.chatRoomInfo
                    )
                )
            }
        }

    private fun handleJoinClick(action: ChatRoomListItemActionState.JoinClick) {
        val chatRoomInfo = action.chatRoomInfo
        chatRoomViewModel.sendJoinChatRoomRequest(
            SendJoinChatRoomRequestRequest(
                receiverId = chatRoomInfo.userId.toInt(),
                senderId = loggedInUserCache.getLoggedInUserId(),
                conversationId = chatRoomInfo.id,
                senderName = loggedInUserCache.getChatUser()?.chatUserName,
                conversationName = chatRoomInfo.roomName,
                senderProfile = loggedInUserCache.getChatUser()?.chatUserProfile
            )
        )
    }

    private fun listenToViewModel() {
        chatRoomViewModel.chatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatRoomViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is ChatRoomViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is ChatRoomViewState.SuccessMessage -> {
                }
                is ChatRoomViewState.ListOfChatRoom -> {
                    hideLoading()
                    if (it.listOfChatRoom.isEmpty()) {
                        binding.llEmptyState.visibility = View.VISIBLE
                        binding.rvChatRoomList.visibility = View.GONE
                    } else {
                        binding.rvChatRoomList.visibility = View.VISIBLE
                        binding.llEmptyState.visibility = View.GONE
                        chatRoomAdapter.listOfDataItems = it.listOfChatRoom
                    }
                }
                is ChatRoomViewState.ReceivedRequestData -> {
                }
                is ChatRoomViewState.KickedOutData -> {
                    callAPIForRefresh()
                }

                is ChatRoomViewState.BannedUserData -> {
                    callAPIForRefresh()
                }
                else -> {
                }
            }
        }.autoDispose()
    }

    private fun callAPIForRefresh() {
        when (selectedTab) {
            0 -> {
                chatRoomViewModel.resetPaginationForPublicChatRoom()
            }
            1 -> {
                chatRoomViewModel.resetPaginationForPrivateChatRoom()
            }
        }
    }

    private fun buttonVisibility(loading: Boolean) {
        if ((chatRoomAdapter.listOfDataItems?.size ?: 0) == 0) {
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
