package com.meetfriend.app.ui.myprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentShortBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.home.shortdetails.ShortDetailsActivity
import com.meetfriend.app.ui.myprofile.view.MediaAdapter
import com.meetfriend.app.ui.myprofile.viewmodel.UserProfileViewModel
import com.meetfriend.app.ui.myprofile.viewmodel.UserProfileViewState
import javax.inject.Inject


class ShortFragment : BasicFragment() {


    private var userId: Int = 0
    private var _binding: FragmentShortBinding? = null
    private val binding get() = _binding!!

    private lateinit var mediaAdapter: MediaAdapter

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserProfileViewModel>
    private lateinit var userProfileViewModel: UserProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentShortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        userProfileViewModel = getViewModelFromFactory(viewModelFactory)

        initUI()
        getPhotosData()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        userProfileViewModel.userProfileState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserProfileViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserProfileViewState.LoadingState -> {
                }
                is UserProfileViewState.UserVideos -> {
                    if (!it.updatePhotoResponse.result.data.isNullOrEmpty()) {
                        mediaAdapter.listOfPosts = it.updatePhotoResponse.result.data
                        binding.noVideoLinearLayout.visibility = View.GONE
                    } else {
                        binding.noVideoLinearLayout.visibility = View.VISIBLE
                    }
                }
                is UserProfileViewState.GetShortsInformation -> {
                    if (!it.userShortsResponse.isNullOrEmpty()) {
                        mediaAdapter.listOfPosts = it.userShortsResponse
                        binding.noVideoLinearLayout.visibility = View.GONE
                        binding.shortsRecyclerView.isVisible = true

                    } else {
                        binding.noVideoLinearLayout.visibility = View.VISIBLE
                        binding.shortsRecyclerView.isVisible = false

                    }
                }
                is UserProfileViewState.PostLoadingState -> {
                    if (mediaAdapter.listOfPosts.isNullOrEmpty()) {
                        showLoading(it.isLoading)
                    }
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun initUI() {
        mediaAdapter = MediaAdapter(requireContext()).apply {
            postViewClicks.subscribeAndObserveOnMainThread {
                startActivity(ShortDetailsActivity.getIntent(requireContext(), it.posts_id))
            }
        }

        val llm = GridLayoutManager(requireContext(), 3)

        binding.shortsRecyclerView.apply {
            adapter = mediaAdapter
            layoutManager = llm
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager ?: return
                        var lastVisibleItemPosition = 0
                        if (layoutManager is GridLayoutManager) {
                            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        }
                        val adjAdapterItemCount = layoutManager.itemCount
                        if (
                            layoutManager.childCount > 0
                            && lastVisibleItemPosition >= adjAdapterItemCount - 2
                            && adjAdapterItemCount >= layoutManager.childCount
                        ) {
                            userProfileViewModel.loadMoreUserShort(userId)
                        }
                    }
                }
            })
        }

        RxBus.listen(RxEvent.UserProfileShortDelete::class.java).subscribeAndObserveOnMainThread { deleteId ->
            val list = mediaAdapter.listOfPosts as ArrayList
            list.removeAll{ it.posts_id == deleteId.deleteShortId}
            mediaAdapter.listOfPosts = list
        }.autoDispose()
    }

    private fun getPhotosData() {
        userId = arguments?.getInt(USER_ID) ?: 0
        if (userId != 0) {
            userProfileViewModel.resetPaginationForUserShorts(userId)
        }
    }


    companion object {
        var USER_ID = "USER_ID"

        @JvmStatic
        fun newInstance() = ShortFragment()

        @JvmStatic
        fun getInstance(userId: Int): ShortFragment {
            val shortFragment = ShortFragment()

            val bundle = Bundle()
            bundle.putInt(USER_ID, userId)

            shortFragment.arguments = bundle

            return shortFragment
        }
    }
}