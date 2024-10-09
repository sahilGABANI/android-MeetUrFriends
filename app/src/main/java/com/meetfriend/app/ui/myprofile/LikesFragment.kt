package com.meetfriend.app.ui.myprofile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentLikesBinding
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

class LikesFragment : BasicFragment() {

    private var userId: Int = 0
    private var _binding: FragmentLikesBinding? = null
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
        userProfileViewModel = getViewModelFromFactory(viewModelFactory)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLikesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                is UserProfileViewState.GetUserLikeInformation -> {
                    if (it.userShortsResponse.isNotEmpty()) {
                        mediaAdapter.listOfPosts = it.userShortsResponse
                        binding.noLikesLinearLayout.visibility =
                            if (it.userShortsResponse.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        binding.noLikesLinearLayout.visibility = View.VISIBLE
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

    @SuppressLint("LogNotTimber")
    private fun initUI() {
        mediaAdapter = MediaAdapter(requireContext()).apply {
            postViewClicks.subscribeAndObserveOnMainThread {
                startActivity(ShortDetailsActivity.getIntent(requireContext(), it.posts_id))
            }.autoDispose()
        }

        val llm = GridLayoutManager(requireContext(), 3)

        binding.likedRecyclerView.apply {
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
                            userProfileViewModel.loadMoreUserLikePost(userId)
                        }
                    }
                }
            })
        }

        RxBus.listen(RxEvent.UserProfileLikeUnlike::class.java).subscribeAndObserveOnMainThread { postId ->
            if (postId.postStatus == "unlike" && postId.postId != -1 && postId.postStatus.isNotEmpty()) {
                val updatedList = mediaAdapter.listOfPosts?.toMutableList()
                updatedList?.removeAll { it.posts_id == postId.postId }
                mediaAdapter.listOfPosts = updatedList
            }
        }.autoDispose()


    }

    private fun getPhotosData() {
        userId = arguments?.getInt(ShortFragment.USER_ID) ?: 0
        if (userId != 0) {
            userProfileViewModel.resetPaginationForUserLikePost(userId)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = LikesFragment()

        const val USER_ID = "USER_ID"

        fun getInstance(otherUserId: Int): Fragment {
            val fragment = LikesFragment()

            val bundle = Bundle()
            bundle.putInt(USER_ID, otherUserId)

            fragment.arguments = bundle

            return fragment
        }
    }
}