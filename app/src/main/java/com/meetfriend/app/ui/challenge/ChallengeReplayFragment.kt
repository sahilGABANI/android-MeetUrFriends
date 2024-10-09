package com.meetfriend.app.ui.challenge

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.OrientationHelper
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.*
import com.meetfriend.app.api.post.model.ChallengePostPageState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentChallengeReplayBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.challenge.bottomsheet.ChallengeCommentBottomSheetFragment
import com.meetfriend.app.ui.challenge.view.PlayChallengeReplayAdapter
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewModel
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewState
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.ShareHelper
import com.meetfriend.app.videoplayer.JZMediaExoKotlin
import com.meetfriend.app.videoplayer.JzvdStdOutgoer
import com.meetfriend.app.videoplayer.OnViewPagerListener
import com.meetfriend.app.videoplayer.ViewPagerLayoutManager
import org.json.JSONObject

import javax.inject.Inject
import kotlin.properties.Delegates

class ChallengeReplayFragment : BasicFragment() {

    companion object {

        private const val INTENT_CHALLENGE_ID = "challengeId"

        fun newInstance(challengeId: Int?): ChallengeReplayFragment {
            val args = Bundle()
            challengeId?.let { args.putInt(INTENT_CHALLENGE_ID, it) }
            val fragment = ChallengeReplayFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentChallengeReplayBinding? = null
    private val binding get() = _binding!!
    private var challengeId: Int = 0
    private var loggedInUserId by Delegates.notNull<Int>()

    private lateinit var playShortsAdapter: PlayChallengeReplayAdapter
    private lateinit var videoLayoutManager: ViewPagerLayoutManager
    private lateinit var challengeItem: ChallengeItem
    private var isVideoInitCompleted = false
    private var mCurrentPosition = -1

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChallengeViewModel>
    private lateinit var challengeViewModel: ChallengeViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeReplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MeetFriendApplication.component.inject(this)
        challengeViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()
        listenToViewEvent()
        listenToViewModel()
        challengeId = arguments?.getInt(INTENT_CHALLENGE_ID, 0) ?: 0
        challengeViewModel.challengeDetail(ChallengeCountRequest(challengeId))
    }

    private fun listenToViewModel() {
        challengeViewModel.challengeState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChallengeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ChallengeViewState.ChallengeLikeSuccess -> {
                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "challengePost")
                    props.put(Constant.CONTENT_ID, it.challengeId)

                    mp?.track(Constant.SHARE_CONTENT, props)
                }
                is ChallengeViewState.ChallengeDetails -> {
                    challengeItem = it.challengeItem
                    setShortsInfo(it.challengeItem.challengePost)
                }

                is ChallengeViewState.SuccessDeleteChallengeMessage -> {
                    if (playShortsAdapter.listOfDataItems?.size ?: 0 > 1) {
                        challengeViewModel.challengeDetail(ChallengeCountRequest(challengeId))
                    } else {
                        parentFragmentManager.popBackStack()
                    }
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun setShortsInfo(shortsList: ArrayList<ChallengePostInfo>?) {
        showProgress(false)
        if (shortsList?.isNotEmpty() == true) {
            playShortsAdapter.listOfDataItems = shortsList
        }
    }

    private fun listenToViewEvent() {
        videoLayoutManager = ViewPagerLayoutManager(requireContext(), OrientationHelper.VERTICAL)
        playShortsAdapter = PlayChallengeReplayAdapter(requireContext()).apply {

            if (loggedInUserId != 0) {
                playShortsViewClicks.subscribeAndObserveOnMainThread { state ->
                    when (state) {
                        is ChallengePostPageState.UserProfileClick -> {
                            if (state.dataVideo.user?.id != 0) {
                                startActivity(
                                    MyProfileActivity.getIntentWithData(
                                        requireContext(),
                                        state.dataVideo.user?.id ?: 0
                                    )
                                )
                            }
                        }

                        is ChallengePostPageState.AddReelLikeClick -> {
                            likePost(state.dataVideo)
                        }
                        is ChallengePostPageState.RemoveReelLikeClick -> {
                            likePost(state.dataVideo)
                        }
                        is ChallengePostPageState.CommentClick -> {
                            if (state.dataVideo.id != null && challengeItem.id != null) {
                                val challengeCommentBottomSheetFragment =
                                    ChallengeCommentBottomSheetFragment.newInstance(
                                        challengeItem.id,
                                        state.dataVideo.user?.profilePhoto,
                                        state.dataVideo.id
                                    )
                                challengeCommentBottomSheetFragment.show(
                                    parentFragmentManager,
                                    ChallengeCommentBottomSheetFragment::class.java.name
                                )
                            }
                        }

                        is ChallengePostPageState.ShareClick -> {
                            ShareHelper.shareDeepLink(
                                requireContext(),
                                6,
                                state.dataVideo.id ?: 0,
                                "",true
                            ) {

                                ShareHelper.shareText(requireContext(), it)
                                showToast(resources.getString(R.string.msg_copied_video_link))


                                val props = JSONObject()
                                props.put(Constant.CONTENT_TYPE, "challengePost")
                                props.put(Constant.CONTENT_ID, state.dataVideo.id)

                                mp?.track(Constant.SHARE_CONTENT, props)
                            }
                        }
                        is ChallengePostPageState.UserChallengeReplyProfileClick -> {

                        }
                        is ChallengePostPageState.MoreClick -> {

                            var isRejectLiveJoin = if ((loggedInUserCache.getLoggedInUserId()) == state.dataVideo.user?.id ?: 0
                            ) {
                                false
                            } else {
                                false
                            }


                            openMoreOptionBottomSheet(
                                state.dataVideo.id ?: 0,
                                state.dataVideo.user?.id ?: 0,
                                isRejectLiveJoin
                            )
                        }
                        is ChallengePostPageState.JoinChallengeClick -> {

                        }
                        else -> {}
                    }
                }

            }


        }
        binding.rvChallengePost.apply {
            layoutManager = videoLayoutManager
            adapter = playShortsAdapter
        }

        videoLayoutManager.setOnViewPagerListener(object : OnViewPagerListener {
            override fun onInitComplete() {
                if (isVideoInitCompleted) {
                    return
                }
                isVideoInitCompleted = true
            }

            override fun onPageRelease(isNext: Boolean, position: Int) {
                if (mCurrentPosition == position) {
                }
            }

            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
                if (mCurrentPosition == position) {
                    return
                }

                val postId = challengeItem.challengePost?.get(position)?.id
                if (postId != null)
                    challengeViewModel.challengePostViewByUser(
                        ChallengePostViewByUserRequest(
                            challengeId = challengeId,
                            challengePostId = postId
                        )
                    )

                if (isResumed)
                    autoPlayVideo()
                mCurrentPosition = position
                if (isBottom) {
                    if(loggedInUserCache.getLoginUserToken() != null) challengeViewModel.loadMoreLiveChallenge()
                }
            }
        })

        binding.back.throttleClicks().subscribeAndObserveOnMainThread {
            parentFragmentManager.popBackStack()
        }.autoDispose()
    }


    private fun autoPlayVideo() {

        if (binding.rvChallengePost.getChildAt(0) == null) {
            return
        }

        val player: JzvdStdOutgoer? =
            binding.rvChallengePost.getChildAt(0).findViewById(R.id.jzvdStdOutgoer)
        player?.apply {
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_NORMAL,
                JZMediaExoKotlin::class.java
            )
            startVideoAfterPreloading()
        }
    }

    private fun likePost(dataVideo: ChallengePostInfo) {
        try {
            if (dataVideo.id != null) {
                challengeViewModel.challengePostLikeUnLike(
                    ChallengePostLikeRequest(
                        challengeId = challengeId,
                        challengePostId = dataVideo.id,
                        status = 0
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showProgress(isVisible: Boolean) {
        if (isVisible) {
            CallProgressWheel.showLoadingDialog(requireActivity())
        } else {
            CallProgressWheel.dismissLoadingDialog()
        }
    }

    private fun openMoreOptionBottomSheet(
        challengePostId: Int,
        challengeUserId: Int,
        isRejectLiveJoin: Boolean
    ) {
        val bottomSheet: MoreOptionBottomSheet = MoreOptionBottomSheet.newInstance(
            challengeId = challengePostId,
            challengeUserId,
            isRejectLiveJoin
        )

        bottomSheet.optionClick.subscribeAndObserveOnMainThread {
            when (it) {
                resources.getString(R.string.report) -> {
                    val reportDialog =
                        ReportOptionBottomSheet.newInstance(challengeId, challengePostId)
                    reportDialog.optionClick.subscribeAndObserveOnMainThread {
                        val props = JSONObject()
                        props.put(Constant.CONTENT_TYPE, "challengePost")
                        props.put(Constant.CONTENT_ID, challengeId)

                        mp?.track(Constant.REPORT_CONTENT, props)
                    }
                    reportDialog.show(
                        childFragmentManager,
                        ReportOptionBottomSheet::class.java.name
                    )
                }
                resources.getString(R.string.label_copy_link) -> {
                    ShareHelper.shareDeepLink(requireContext(), 6, challengePostId, "",true) {
                        val clipboard: ClipboardManager =
                            requireActivity().getSystemService(
                                AppCompatActivity.CLIPBOARD_SERVICE
                            ) as ClipboardManager
                        val clip = ClipData.newPlainText("Url", it)
                        clipboard.setPrimaryClip(clip)
                        showToast(resources.getString(R.string.msg_copied_video_link))
                    }
                }

                resources.getString(R.string.label_delete) -> {

                    challengeViewModel.deleteChallengePost(
                        DeleteChallengePostRequest(
                            challengePostId
                        )
                    )
                }

            }
        }.autoDispose()
        bottomSheet.show(childFragmentManager, MoreOptionBottomSheet::class.java.name)
    }
}