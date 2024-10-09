package com.meetfriend.app.ui.myprofile

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.model.HomePagePostInfoState
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.api.post.model.PostInformation
import com.meetfriend.app.api.post.model.PostLikeUnlikeRequest
import com.meetfriend.app.api.post.model.PostLikesInformation
import com.meetfriend.app.api.post.model.PostShareRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentPostBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.activities.FullScreenActivity
import com.meetfriend.app.ui.activities.VideoPlayActivity
import com.meetfriend.app.ui.giftsGallery.GiftGalleryBottomSheet
import com.meetfriend.app.ui.hashtag.HashTagListActivity
import com.meetfriend.app.ui.home.SharePostBottomSheet
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.shorts.ShortsCommentBottomSheet
import com.meetfriend.app.ui.main.post.BottomSheetPostMoreItem
import com.meetfriend.app.ui.main.post.PostLikesDialog
import com.meetfriend.app.ui.main.view.MainHomeAdapter
import com.meetfriend.app.ui.myprofile.viewmodel.UserProfileViewModel
import com.meetfriend.app.ui.myprofile.viewmodel.UserProfileViewState
import com.meetfriend.app.ui.tag.TaggedUserActivity
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FiXED_1000_MILLISECOND
import com.meetfriend.app.utils.Constant.FiXED_50_INT
import com.meetfriend.app.utils.ShareHelper
import com.meetfriend.app.videoplayer.JZMediaExoKotlin
import com.meetfriend.app.videoplayer.JzvdStdOutgoer
import com.meetfriend.app.viewmodal.HomeViewModal
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PostFragment : BasicFragment() {

    private lateinit var deletedItem: PostInformation
    private var homeViewModal: HomeViewModal? = null

    private lateinit var mainHomeAdapter: MainHomeAdapter

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserProfileViewModel>
    private lateinit var userProfileViewModel: UserProfileViewModel
    private var userId: Int = 0
    private var hashTagId: Int = 0
    private var hashTagName: String? = null

    private var autoplayFirst = true
    private var currentItemIsVideo = false

    private var mCurrentPosition = -1

    private var onShareIntent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModal = ViewModelProvider(this).get(HomeViewModal::class.java)
        userProfileViewModel = getViewModelFromFactory(viewModelFactory)
        arguments?.let {
            if (it.containsKey(USER_ID)) {
                userId = it.getInt(USER_ID)

                binding.useThisHashtagAppCompatTextView.visibility = View.GONE
            }
        }
        if (userId != 0) {
            userProfileViewModel.getPost(userId)
        }

        initUI()
        listenToViewModel()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listenToViewModel() {
        userProfileViewModel.userProfileState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is UserProfileViewState.GetPostResponse -> handleGetPostResponse(state)
                is UserProfileViewState.PostDeleteSuccessFully -> handlePostDeleteSuccess()
                is UserProfileViewState.PostShareResponse -> handlePostShareResponse()
                is UserProfileViewState.PostLoadingState -> handlePostLoadingState(state.isLoading)
                else -> { /* No action needed */ }
            }
        }.autoDispose()
    }

    private fun handleGetPostResponse(state: UserProfileViewState.GetPostResponse) {
        hideLoading()
        binding.noPostsLinearLayout.visibility = if (state.userPhotosResponse.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.postsRecyclerView.apply {
            setItemViewCacheSize(state.userPhotosResponse.size)
            recycledViewPool.clear()
            setItemViewCacheSize(state.userPhotosResponse.size)
        }

        mainHomeAdapter.listOfPosts = state.userPhotosResponse

        if (!mainHomeAdapter.listOfPosts.isNullOrEmpty()) {
            autoPlayFirstPostIfNeeded()
        }

        handleDelayedAutoPlay()
    }

    private fun autoPlayFirstPostIfNeeded() {
        mainHomeAdapter.listOfPosts?.firstOrNull()?.let { postInfo ->
            val data = if (postInfo.isShared == 1) {
                postInfo.sharedPost?.postMedia
            } else {
                postInfo.postMedia
            }

            if (!data.isNullOrEmpty() && data.first().extension == ".m3u8" && autoplayFirst) {
                Observable.timer(FiXED_1000_MILLISECOND, TimeUnit.MILLISECONDS)
                    .subscribeAndObserveOnMainThread {
                        autoplayFirst = false
                        RxBus.publish(RxEvent.StartVideo(false))
                    }.autoDispose()
            }
        }
    }

    private fun handleDelayedAutoPlay() {
        Handler(Looper.getMainLooper()).postDelayed({
            val layoutManager = binding.postsRecyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

            if (firstVisibleItemPosition > -1) {
                val itemType = mainHomeAdapter.getItemViewType(firstVisibleItemPosition)
                if (itemType != 1) {
                    autoPlayVideo()
                } else {
                    Jzvd.goOnPlayOnPause()
                }
            }
        }, FiXED_50_INT.toLong())
    }

    private fun handlePostDeleteSuccess() {
        val list = mainHomeAdapter.listOfPosts as ArrayList
        list.remove(deletedItem)
        mainHomeAdapter.listOfPosts = list
        mainHomeAdapter.notifyDataSetChanged()
    }

    private fun handlePostShareResponse() {
        userProfileViewModel.resetPaginationForUserPost(userId)
    }

    private fun handlePostLoadingState(isLoading: Boolean) {
        if (mainHomeAdapter.listOfPosts.isNullOrEmpty()) {
            showLoading(isLoading)
        }
    }

    private fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.android.chrome")
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            intent.setPackage(null)
            startActivity(intent)
        }
    }

    private fun initUI() {
        setupMainHomeAdapter()
        setupRecyclerView()
        subscribeToRxBusEvents()
    }

    private fun subscribeToRxBusEvents() {
        RxBus.listen(RxEvent.RefreshHomePagePostPlayVideo::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.isVisible) {
                if (mCurrentPosition > -1) {
                    val itemType = mainHomeAdapter.getItemViewType(mCurrentPosition)
                    if (itemType == 1) {
                        Jzvd.goOnPlayOnPause()
                    } else {
                        if (onShareIntent) {
                            onShareIntent = false
                            if (isVisible) {
                                autoPlayVideo()
                            }
                        } else {
                            Observable.timer(FiXED_1000_MILLISECOND, TimeUnit.MILLISECONDS)
                                .subscribeAndObserveOnMainThread {
                                    RxBus.publish(RxEvent.StartVideo(false))
                                }.autoDispose()
                        }
                    }
                }
            }
        }, {
        }).autoDispose()

        RxBus.listen(RxEvent.PlayHashtagVideo::class.java).subscribeAndObserveOnMainThread {
            if (isResumed) {
                if (!it.isPlay) {
                    Jzvd.goOnPlayOnPause()
                }
            }
        }.autoDispose()
    }

    private fun setupMainHomeAdapter() {
        mainHomeAdapter = MainHomeAdapter(requireContext()).apply {
            postViewClicks.subscribeAndObserveOnMainThread { state ->
                handlePostViewClick(state)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.postsRecyclerView.apply {
            adapter = mainHomeAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        checkItemVisibility()
                        handleScroll(recyclerView)
                    }
                }
            })
        }
    }

    private fun handleScroll(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager ?: return
        val lastVisibleItemPosition = (layoutManager as? LinearLayoutManager)?.findLastVisibleItemPosition() ?: 0
        val adjAdapterItemCount = layoutManager.itemCount

        if (layoutManager.childCount > 0 &&
            lastVisibleItemPosition >= adjAdapterItemCount - 2 &&
            adjAdapterItemCount >= layoutManager.childCount
        ) {
            if (userId != 0) {
                userProfileViewModel.loadMorePost(userId)
            } else if (hashTagId != 0) {
                userProfileViewModel.loadMorePostH(hashTagId)
            }
        }
    }

    private fun handlePostViewClick(state: HomePagePostInfoState) {
        when (state) {
            is HomePagePostInfoState.UserProfileClick -> handleUserProfile(state)
            is HomePagePostInfoState.OptionsClick -> handleOptionClick(state)
            is HomePagePostInfoState.PostMediaClick -> handlePostMediaClick(state)
            is HomePagePostInfoState.PostLikeClick -> handlePostLikeClick(state)
            is HomePagePostInfoState.OpenLinkAttachmentMultiple -> openUrlInBrowser(state.uri)
            is HomePagePostInfoState.AddPostLikeClick -> handleAddPostLike(state)
            is HomePagePostInfoState.RemovePostLikeClick -> handleRemovePostLike(state)
            is HomePagePostInfoState.PostLikeCountClick -> handlePostLikeCountClick(state)
            is HomePagePostInfoState.CommentClick -> handleCommentClick(state)
            is HomePagePostInfoState.ShareClick -> handleShareClick(state)
            is HomePagePostInfoState.RepostClick -> handleRepostClick(state)
            is HomePagePostInfoState.TagPeopleClick -> handleTagPeopleClick(state)
            is HomePagePostInfoState.TagPeopleFirstUserClick -> handleTagPeopleFirstUserClick(state)
            is HomePagePostInfoState.OtherUserProfileClick -> handleOtherUserProfileClick(state)
            is HomePagePostInfoState.TagRepostPeopleClick -> handleTagRepostPeopleClick(state)
            is HomePagePostInfoState.TagRepostPeopleFirstUserClick -> handleTagRepostPeopleFirstUserClick(state)
            is HomePagePostInfoState.PostMediaVideoClick -> handlePostMediasVideoClick(state)
            is HomePagePostInfoState.MentionUserNavigationClick -> handleMentionUserNavigationClick(state)
            is HomePagePostInfoState.ShowUserNotFoundToast -> if (state.isShow) showToast("User not found")
            is HomePagePostInfoState.MentionHashTagNavigationClick -> handleMentionHashTagNavigationClick(state)
            is HomePagePostInfoState.ShowHashtagNotFoundToast -> if (state.isShow) showToast("Hashtag not found")
            is HomePagePostInfoState.GiftClick -> handleGiftClick(state)
            else -> {}
        }
    }

    private fun handleMentionHashTagNavigationClick(state: HomePagePostInfoState.MentionHashTagNavigationClick) {
        startActivity(
            HashTagListActivity.getIntent(
                requireContext(),
                state.hashTagId,
                state.hashTagName
            )
        )
    }

    private fun handleMentionUserNavigationClick(state: HomePagePostInfoState.MentionUserNavigationClick) {
        startActivity(
            MyProfileActivity.getIntentWithData(
                requireContext(),
                state.userId
            )
        )
    }

    private fun handleAddPostLike(state: HomePagePostInfoState.AddPostLikeClick) {
        val postLikeUnlikeRequest = PostLikeUnlikeRequest(state.postInfo.id, Constant.LIKE)
        userProfileViewModel.postLikeDisLike(postLikeUnlikeRequest)
    }

    private fun handleRemovePostLike(state: HomePagePostInfoState.RemovePostLikeClick) {
        val postLikeUnlikeRequest = PostLikeUnlikeRequest(state.postInfo.id, Constant.UNLIKE)
        userProfileViewModel.postLikeDisLike(postLikeUnlikeRequest)
    }

    private fun handlePostLikeCountClick(state: HomePagePostInfoState.PostLikeCountClick) {
        val postLikesDialog = PostLikesDialog.newInstance(
            state.postInfo.postLikes as ArrayList<PostLikesInformation>
        )
        postLikesDialog.show(childFragmentManager, PostLikesDialog::class.java.name)
    }

    private fun handleCommentClick(state: HomePagePostInfoState.CommentClick) {
        val bottomSheet = ShortsCommentBottomSheet.newInstance(state.postInfo.id.toString())
        bottomSheet.show(childFragmentManager, ShortsCommentBottomSheet::class.java.name)
    }

    private fun handleShareClick(state: HomePagePostInfoState.ShareClick) {
        ShareHelper.shareDeepLink(requireContext(), 0, state.postInfo.id, "", true) {
            ShareHelper.shareText(requireContext(), it)
        }
    }

    private fun handleTagPeopleClick(state: HomePagePostInfoState.TagPeopleClick) {
        startActivity(
            TaggedUserActivity.getIntent(
                requireContext(),
                state.postInfo.taggedUsersList as ArrayList
            )
        )
    }

    private fun handleTagPeopleFirstUserClick(state: HomePagePostInfoState.TagPeopleFirstUserClick) {
        state.postInfo.taggedUsersList?.firstOrNull()?.user?.id?.let {
            startActivity(
                MyProfileActivity.getIntentWithData(
                    requireContext(),
                    it
                )
            )
        }
    }

    private fun handleOtherUserProfileClick(state: HomePagePostInfoState.OtherUserProfileClick) {
        if (currentItemIsVideo && Jzvd.CURRENT_JZVD.state == Jzvd.STATE_PAUSE) Jzvd.goOnPlayOnResume()

        startActivity(
            MyProfileActivity.getIntentWithData(
                requireContext(),
                state.postInfo.sharedPost?.userId ?: 0
            )
        )
    }

    private fun handleTagRepostPeopleClick(state: HomePagePostInfoState.TagRepostPeopleClick) {
        startActivity(
            TaggedUserActivity.getIntent(
                requireContext(),
                state.postInfo?.taggedUsersList as ArrayList
            )
        )
    }

    private fun handleTagRepostPeopleFirstUserClick(state: HomePagePostInfoState.TagRepostPeopleFirstUserClick) {
        state.postInfo?.taggedUsersList?.firstOrNull()?.user?.id?.let {
            startActivity(
                MyProfileActivity.getIntentWithData(
                    requireContext(),
                    it
                )
            )
        }
    }

    private fun handleUserProfile(state: HomePagePostInfoState.UserProfileClick) {
        if (currentItemIsVideo) {
            if (Jzvd.CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.state == Jzvd.STATE_PAUSE) {
                Jzvd.goOnPlayOnResume()
            }
        }
        startActivity(
            MyProfileActivity.getIntentWithData(
                requireContext(),
                state.postInfo.userId ?: 0
            )
        )
    }

    private fun handlePostMediaClick(state: HomePagePostInfoState.PostMediaClick) {
        val url = if (state.postInfo.isShared == 1) {
            state.postInfo.sharedPost?.postMedia?.get(
                state.position
            )?.filePath
        } else {
            state.postInfo.postMedia?.get(state.position)?.filePath
        }
        val intent = Intent(requireActivity(), FullScreenActivity::class.java)
        intent.putExtra(
            "url",
            url
        )
        requireActivity().startActivity(intent)
    }

    private fun handlePostLikeClick(state: HomePagePostInfoState.PostLikeClick) {
        val postLikedDisliked: String?
        if (state.postInfo.isLikedCount == 0) {
            postLikedDisliked = Constant.UNLIKE
            state.postInfo.isLikedCount = 0
            state.postInfo.postLikesCount = state.postInfo.postLikesCount?.minus(1)
        } else {
            postLikedDisliked = Constant.LIKE
            state.postInfo.isLikedCount = 1
            state.postInfo.postLikesCount = state.postInfo.postLikesCount?.plus(1)
        }
        val postLikeUnlikeRequest = PostLikeUnlikeRequest(state.postInfo.id, postLikedDisliked)
        userProfileViewModel.postLikeDisLike(postLikeUnlikeRequest)
    }

    private fun handleRepostClick(state: HomePagePostInfoState.RepostClick) {
        val bottomSheet = SharePostBottomSheet()
        bottomSheet.shareClicks.subscribeAndObserveOnMainThread {
            val postShareId = if (state.postInfo.isShared == 1) {
                state.postInfo.sharedPost?.id
            } else {
                state.postInfo.id
            }
            if (postShareId != null) {
                val postShareRequest =
                    PostShareRequest(
                        postShareId,
                        it.privacy.toString(),
                        it.about.toString(),
                        it.mentionUserId
                    )
                userProfileViewModel.postShare(postShareRequest)
            }
        }.autoDispose()
        bottomSheet.show(
            childFragmentManager,
            SharePostBottomSheet::class.java.name
        )
    }

    private fun handlePostMediasVideoClick(state: HomePagePostInfoState.PostMediaVideoClick) {
        val intent = Intent(requireActivity(), VideoPlayActivity::class.java)
        val url = if (state.postInfo.isShared == 1) {
            state.postInfo.sharedPost?.postMedia?.firstOrNull()?.filePath
        } else {
            state.postInfo.postMedia?.firstOrNull()?.filePath
        }
        if (url != null) {
            intent.putExtra(
                "path",
                url
            )
            requireActivity().startActivity(intent)
        }
    }

    private fun handleGiftClick(state: HomePagePostInfoState.GiftClick) {
        val bottomSheet = GiftGalleryBottomSheet.newInstance(
            state.postInfo.user.id.toString(),
            state.postInfo.id.toString(),
            true,
            isFromWhere = GiftGalleryBottomSheet.INTENT_IS_FROM_POST
        )
        bottomSheet.giftResponseState.subscribeAndObserveOnMainThread {
            when (it) {
                is GiftItemClickStates.SendGiftInChatClick -> {
                    state.postInfo.totalGiftCount = state.postInfo.totalGiftCount?.plus(1)
                    mainHomeAdapter.notifyDataSetChanged()
                }

                else -> {
                }
            }
        }.autoDispose()
        bottomSheet.show(
            childFragmentManager,
            GiftGalleryBottomSheet::class.java.name
        )
    }

    private fun handleOptionClick(state: HomePagePostInfoState.OptionsClick) {
        val bottomSheetPostMoreItem = BottomSheetPostMoreItem.newInstanceWithoutData(state.postInfo.userId ?: 0)

        bottomSheetPostMoreItem.moreOptionClicks.subscribeAndObserveOnMainThread { option ->
            when (option) {
                resources.getString(R.string.label_copy) -> handleCopyOption(state)
                resources.getString(R.string.label_share) -> handleShareOption(state)
                resources.getString(R.string.label_delete) -> handleDeleteOption(state)
                resources.getString(R.string.labek_repost) -> handleRepostOption(state)
            }
        }.autoDispose()

        bottomSheetPostMoreItem.show(childFragmentManager, "TAGS")
    }

    private fun handleCopyOption(state: HomePagePostInfoState.OptionsClick) {
        ShareHelper.shareDeepLink(requireContext(), 0, state.postInfo.id, "", true) { link ->
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied!", link)
            clipboard.setPrimaryClip(clip)
            showToast("Link copied!!")
        }
    }

    private fun handleShareOption(state: HomePagePostInfoState.OptionsClick) {
        ShareHelper.shareDeepLink(requireContext(), 0, state.postInfo.id, "", true) { link ->
            ShareHelper.shareText(requireContext(), link)
        }
    }

    private fun handleDeleteOption(state: HomePagePostInfoState.OptionsClick) {
        if (currentItemIsVideo && Jzvd.CURRENT_JZVD.state == Jzvd.STATE_PAUSE) {
            Jzvd.goOnPlayOnResume()
        }
        deletePost(state.postInfo)
    }

    private fun handleRepostOption(state: HomePagePostInfoState.OptionsClick) {
        val bottomSheet = SharePostBottomSheet()
        bottomSheet.shareClicks.subscribeAndObserveOnMainThread { shareData ->
            val postShareId = if (state.postInfo.isShared == 1) {
                state.postInfo.sharedPost?.id ?: 0
            } else {
                state.postInfo.id
            }
            val postShareRequest = PostShareRequest(
                postShareId,
                shareData.privacy.toString(),
                shareData.about.toString(),
                shareData.mentionUserId
            )
            userProfileViewModel.postShare(postShareRequest)
        }.autoDispose()

        bottomSheet.show(childFragmentManager, SharePostBottomSheet::class.java.name)
    }

    private fun deletePost(postInfo: PostInformation) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setMessage("Are you sure, you want to delete this post?")
        builder.setPositiveButton(
            Html.fromHtml(
                "<font color='#B22827'>Delete</font>"
            )
        ) { dialog, _ ->
            dialog.dismiss()
            deletedItem = postInfo
            userProfileViewModel.deletePost(postInfo.id)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    companion object {
        var USER_ID = "USER_ID"
        var HASHTAG_ID = "HASHTAG_ID"
        var HASHTAG_NAME = "HASHTAG_NAME"
        var FROM_WHERE = "FROM_WHERE"
        var INTENT_SEARCH = "INTENT_SEARCH"

        @JvmStatic
        fun newInstance() = PostFragment()

        @JvmStatic
        fun getInstance(userId: Int): PostFragment {
            val postFragment = PostFragment()

            val bundle: Bundle = Bundle()
            bundle.putInt(USER_ID, userId)

            postFragment.arguments = bundle

            return postFragment
        }

        @JvmStatic
        fun getInstanceForHashTag(
            hashTagId: Int,
            hashTagName: String,
            fromWhere: Int,
            search: String? = null
        ): PostFragment {
            val postFragment = PostFragment()

            val bundle: Bundle = Bundle()
            bundle.putInt(HASHTAG_ID, hashTagId)
            bundle.putString(HASHTAG_NAME, hashTagName)
            bundle.putString(INTENT_SEARCH, search)
            bundle.putInt(FROM_WHERE, fromWhere)

            postFragment.arguments = bundle

            return postFragment
        }
    }

    override fun onResume() {
        super.onResume()

        if (isResumed) {
            autoplayFirst = true
            RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(isResumed))
        }

        arguments?.let {
            if (it.containsKey(HASHTAG_ID)) {
                hashTagId = it.getInt(HASHTAG_ID)
                hashTagName = it.getString(HASHTAG_NAME) ?: ""

                if (it.getInt(FROM_WHERE) == 0) {
                    binding.useThisHashtagAppCompatTextView.visibility = View.VISIBLE

                    binding.useThisHashtagAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
                        XXPermissions.with(requireContext())
                            .permission(Permission.CAMERA)
                            .permission(Permission.RECORD_AUDIO)
                            .permission(Permission.READ_MEDIA_IMAGES)
                            .permission(Permission.READ_MEDIA_VIDEO)
                            .request(object : OnPermissionCallback {

                                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                                    if (all) {
                                        startActivity(
                                            AddNewPostInfoActivity.launchActivity(
                                               LaunchActivityData(requireContext(),
                                                postType = AddNewPostInfoActivity.CREATE_POST,
                                                imagePathList = arrayListOf(),
                                                tagName = hashTagName
                                               )
                                            )
                                        )
                                    }
                                }

                                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                                    super.onDenied(permissions, never)
                                    if (never) {
                                        showToast(
                                            "Authorization is permanently denied, please manually grant permissions"
                                        )
                                        XXPermissions.startPermissionActivity(
                                            requireContext(),
                                            permissions
                                        )
                                    } else {
                                        showToast(getString(R.string.msg_permission_denied))
                                    }
                                }
                            })
                    }

                    userProfileViewModel.resetPaginationForHashTagPost(hashTagId)
                } else {
                    binding.useThisHashtagAppCompatTextView.visibility = View.GONE
//
                    userProfileViewModel.resetPaginationForHashTagPost(hashTagId, "")

                    RxBus.listen(RxEvent.SearchTextPost::class.java).subscribeAndObserveOnMainThread {
                        userProfileViewModel.resetPaginationForHashTagPost(hashTagId, it.search.toString())
                    }.autoDispose()
                }
            }
        }
    }

    private fun checkItemVisibility() {
        val layoutManager = binding.postsRecyclerView.layoutManager as LinearLayoutManager
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        if (mCurrentPosition != firstVisibleItemPosition) {
            mCurrentPosition = firstVisibleItemPosition

            val itemType = mainHomeAdapter.getItemViewType(firstVisibleItemPosition)
            if (itemType != 1) {
                autoPlayVideo()
                currentItemIsVideo = true
            } else {
                currentItemIsVideo = false
                Jzvd.goOnPlayOnPause()
            }
        } else {
            val itemType = mainHomeAdapter.getItemViewType(firstVisibleItemPosition)
            if (itemType != 1) {
                currentItemIsVideo = true
                autoPlayVideo()
            } else {
                currentItemIsVideo = false
                Jzvd.goOnPlayOnPause()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(false))
        Jzvd.goOnPlayOnPause()
    }

    private fun autoPlayVideo() {
        if (isResumed && isVisible && isAdded) {
            if (binding.postsRecyclerView.getChildAt(0) == null) {
                return
            }

            if (binding.postsRecyclerView.getChildAt(0) != null && binding.postsRecyclerView.getChildAt(0)
                    .findViewById<ViewPager2>(R.id.sliderImageSlider) != null && binding.postsRecyclerView.getChildAt(0)
                    .findViewById<ViewPager2>(R.id.sliderImageSlider).getChildAt(0)
                    .findViewById<JzvdStdOutgoer>(R.id.outgoerVideoPlayer) != null
            ) {
                val player: JzvdStdOutgoer? = binding.postsRecyclerView.getChildAt(
                    0
                ).findViewById<ViewPager2>(R.id.sliderImageSlider).getChildAt(0)
                    .findViewById<JzvdStdOutgoer>(R.id.outgoerVideoPlayer)
                player?.apply {
                    player.posterImageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    val jzDataSource = JZDataSource(this.videoUrl)
                    jzDataSource.looping = true
                    this.setUp(
                        jzDataSource,
                        Jzvd.SCREEN_NORMAL,
                        JZMediaExoKotlin::class.java
                    )
                    Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
                    startVideoAfterPreloading()
                }
            }
        }
    }
}
