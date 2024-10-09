package com.meetfriend.app.ui.challenge.bottomsheet

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.model.*
import com.meetfriend.app.api.post.model.ChallengeCommentState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentChallengeCommentBottmSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.challenge.view.ChallengeCommentAdapter
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeCommentViewModel
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeCommentViewState
import com.meetfriend.app.ui.chatRoom.roomview.view.MentionUserAdapter
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ChallengeCommentBottomSheetFragment : BaseBottomSheetDialogFragment() {
    private val optionClickSubject: PublishSubject<String> = PublishSubject.create()
    val optionClick: Observable<String> = optionClickSubject.hide()

    companion object {

        private const val INTENT_CHALLENGE_ID = "ChallengeId"
        private const val INTENT_CHALLENGE_POST_ID = "ChallengePostId"
        private const val INTENT_CHALLENGE_PROFILE_URL = "INTENT_CHALLENGE_PROFILE_URL"
        fun newInstance(
            challengeId: Int?, challengeAuthorImage: String?,
            challengePostId: Int? = null
        ): ChallengeCommentBottomSheetFragment {
            val args = Bundle()

            challengeId?.let { args.putInt(INTENT_CHALLENGE_ID, it) }
            challengePostId?.let { args.putInt(INTENT_CHALLENGE_POST_ID, it) }
            challengeAuthorImage.let { args.putString(INTENT_CHALLENGE_PROFILE_URL, it) }
            val fragment = ChallengeCommentBottomSheetFragment()
            fragment.arguments = args
            return fragment
        }

    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChallengeCommentViewModel>
    private lateinit var challengeViewModel: ChallengeCommentViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var challengeId: Int? = 0
    private var challengePostId: Int? = 0

    private var commentItem: Int = -1
    private var commentId: Int = -1
    private var parentCommentId: Int = -1

    private var challengeAuthorImage: String? = null
    private var deleteComment: ChallengeComment? = null
    private var replayCommentDetails: ChallengeComment? = null
    private var deleteChildComment: ChildCommentItem? = null
    private var listOfComment: MutableList<ChallengeComment>? = mutableListOf()
    private var replyCommentPosition: Int? = null
    private var deleteReplyCommentPosition: Int? = null

    private var _binding: FragmentChallengeCommentBottmSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var challengeCommentAdapter: ChallengeCommentAdapter
    private lateinit var mentionUserAdapter: MentionUserAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
        challengeViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeR)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeCommentBottmSheetBinding.inflate(inflater, container, false)
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
        arguments?.let {
            challengeId = it.getInt(INTENT_CHALLENGE_ID, 0)
            challengePostId = it.getInt(INTENT_CHALLENGE_POST_ID, 0)
            challengeAuthorImage = it.getString(INTENT_CHALLENGE_PROFILE_URL, null)

            Glide.with(requireActivity())
                .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto)
                .error(resources.getDrawable(R.drawable.ic_user_profile, null))
                .into(binding.ivUserProfile)
        }

        listenToViewModel()
        listenToViewEvents()
        if (challengePostId != 0) {
            challengePostId?.let { challengeViewModel.resetPaginationChallengePostComment(it) }
        } else {
            challengeId?.let { challengeViewModel.resetPaginationChallengeComment(it) }
        }

    }


    private fun listenToViewModel() {
        challengeViewModel.challengeCommentState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChallengeCommentViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ChallengeCommentViewState.UpdateCommentMessage -> {
                    hideKeyboard()
                    binding.etComment.text?.clear()
                    binding.etComment.clearFocus()
                    val list = challengeCommentAdapter.listOfDataItems

                    if (commentItem == 1) {
                        list?.find { it.id == commentId }?.apply {
                            this.content = it.challengeComment.content
                            this.mentionComments = it.challengeComment.mentionComments
                        }
                    } else if (commentItem == 2) {
                        list?.find { it.id == parentCommentId }?.apply {
                            this.childComments?.find { it.id == commentId }?.apply {
                                this.content = it.challengeComment.content
                                this.mentionComments = it.challengeComment.mentionComments
                            }
                        }
                    }

                    commentItem = 0

                    challengeCommentAdapter.listOfDataItems = list
                }
                is ChallengeCommentViewState.ListOfChallengeComment -> {
                    listOfComment = it.listOfChallengeComment.toMutableList()
                    hideKeyboard()
                    if (it.listOfChallengeComment.isEmpty()) {
                        binding.llEmptyState.isVisible = true
                        binding.rvChallengeComment.visibility = View.INVISIBLE
                    } else {
                        binding.llEmptyState.isVisible = false
                        binding.rvChallengeComment.isVisible = true
                    }
                    challengeCommentAdapter.listOfDataItems = it.listOfChallengeComment
                }
                is ChallengeCommentViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is ChallengeCommentViewState.SuccessDeleteMessage -> {
                    if (deleteReplyCommentPosition != null && deleteChildComment != null) {
                        var list =
                            challengeCommentAdapter.listOfDataItems as ArrayList<ChallengeComment>
                        deleteReplyCommentPosition?.let {
                            val childCommentList = (list.get(it).childComments
                                ?: arrayListOf()) as ArrayList<ChildCommentItem>
                            childCommentList.remove(deleteChildComment)
                            list.get(it).childComments = childCommentList
                        }
                        challengeCommentAdapter.listOfDataItems = list
                        deleteReplyCommentPosition = null
                        deleteChildComment = null
                    } else {
                        var list =
                            challengeCommentAdapter.listOfDataItems as ArrayList<ChallengeComment>
                        list.remove(deleteComment)
                        challengeCommentAdapter.listOfDataItems = list
                        if (list.isEmpty()) {
                            binding.llEmptyState.isVisible = true
                            binding.rvChallengeComment.visibility = View.INVISIBLE
                        } else {
                            binding.llEmptyState.visibility = View.INVISIBLE
                            binding.rvChallengeComment.visibility = View.VISIBLE
                        }
                    }
                }

                is ChallengeCommentViewState.SuccessDeletePostMessage -> {
                    if (deleteReplyCommentPosition != null && deleteChildComment != null) {
                        var list =
                            challengeCommentAdapter.listOfDataItems as ArrayList<ChallengeComment>
                        deleteReplyCommentPosition?.let {
                            val childCommentList = (list.get(it).childComments
                                ?: arrayListOf()) as ArrayList<ChildCommentItem>
                            childCommentList.remove(deleteChildComment)
                            list.get(it).childComments = childCommentList
                        }
                        challengeCommentAdapter.listOfDataItems = list
                        deleteReplyCommentPosition = null
                        deleteChildComment = null
                    } else {
                        var list =
                            challengeCommentAdapter.listOfDataItems as ArrayList<ChallengeComment>
                        list.remove(deleteComment)
                        challengeCommentAdapter.listOfDataItems = list
                        if (list.isEmpty()) {
                            binding.llEmptyState.isVisible = true
                            binding.rvChallengeComment.visibility = View.INVISIBLE
                        } else {
                            binding.llEmptyState.isVisible = false
                            binding.rvChallengeComment.visibility = View.GONE
                        }
                    }
                }
                is ChallengeCommentViewState.AddChallengeComment -> {
                    optionClickSubject.onNext("comment")
                    hideKeyboard()
                    binding.etComment.text?.clear()
                    binding.etComment.clearFocus()
                    binding.llEmptyState.isVisible = false
                    binding.rvChallengeComment.isVisible = true

                    if (challengeCommentAdapter.listOfDataItems.isNullOrEmpty()) {
                        val list: ArrayList<ChallengeComment> = arrayListOf()
                        list.add(0, it.listOfChallengeComment)
                        listOfComment = list

                        challengeCommentAdapter.listOfDataItems = list
                    } else {
                        val list =
                            challengeCommentAdapter.listOfDataItems as ArrayList<ChallengeComment>
                        list.add(0, it.listOfChallengeComment)
                        listOfComment = list
                        challengeCommentAdapter.listOfDataItems = list
                    }


                    if (replyCommentPosition != null && replyCommentPosition ?: 0 >= 0) {
                        val list = challengeCommentAdapter.listOfDataItems
                        replyCommentPosition = list?.indexOf(replayCommentDetails)
                    }

                    binding.llEmptyState.visibility = View.GONE
                }
                is ChallengeCommentViewState.AddChallengeCommentReplay -> {
                    hideKeyboard()

                    if (replyCommentPosition != null) {
                        binding.etComment.text?.clear()
                        binding.etComment.clearFocus()
                        val list =
                            challengeCommentAdapter.listOfDataItems as ArrayList<ChallengeComment>
                        var childCommentList: ArrayList<ChildCommentItem> = arrayListOf()
                        replyCommentPosition?.let { it1 ->
                            childCommentList = (list.get(it1).childComments
                                ?: arrayListOf()) as ArrayList<ChildCommentItem>
                        }
                        val childCommentItem = ChildCommentItem(
                            id = it.listOfChallengeComment.id,
                            content = it.listOfChallengeComment.content,
                            challengeId = it.listOfChallengeComment.challengeId,
                            parentId = it.listOfChallengeComment.parentId,
                            user = it.listOfChallengeComment.user,
                            userId = it.listOfChallengeComment.userId,
                            createdAt = it.listOfChallengeComment.createdAt,
                            repliedToUserId = it.listOfChallengeComment.repliedToUserId,
                            isLikedCount = it.listOfChallengeComment.isLikedCount,
                            noOfLikesCount = it.listOfChallengeComment.noOfLikesCount,
                            mentionComments = it.listOfChallengeComment.mentionComments
                        )
                        childCommentList.add(childCommentItem)
                        replyCommentPosition?.let { list[it].childComments = childCommentList }
                        challengeCommentAdapter.listOfDataItems = list
                        replyCommentPosition = null
                        replayCommentDetails = null
                    } else {
                        binding.etComment.text?.clear()
                        binding.etComment.clearFocus()
                        if (challengeCommentAdapter.listOfDataItems.isNullOrEmpty()) {
                            val list: ArrayList<ChallengeComment> = arrayListOf()
                            list.add(0, it.listOfChallengeComment)
                            challengeCommentAdapter.listOfDataItems = list
                        } else {
                            val list =
                                challengeCommentAdapter.listOfDataItems as ArrayList<ChallengeComment>
                            list.add(0, it.listOfChallengeComment)
                            challengeCommentAdapter.listOfDataItems = list
                        }

                        binding.llEmptyState.visibility = View.GONE

                    }
                }
                is ChallengeCommentViewState.UserListForMention -> {
                    mentionUserViewVisibility(!it.listOfUserForMention.isNullOrEmpty())
                    mentionUserAdapter.listOfDataItems = it.listOfUserForMention
                }
                is ChallengeCommentViewState.UpdateDescriptionText -> {
                    mentionUserViewVisibility(false)
                    binding.etComment.setText(it.descriptionString)
                    binding.etComment.setSelection(binding.etComment.text.toString().length)

                }
                else -> {

                }
            }
        }.autoDispose()
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


    private fun mentionUserViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.llMentionUserListContainer.visibility == View.GONE) {
            binding.llMentionUserListContainer.visibility = View.VISIBLE
        } else if (!isVisibility && binding.llMentionUserListContainer.visibility == View.VISIBLE) {
            binding.llMentionUserListContainer.visibility = View.GONE
        }
    }

    private fun listenToViewEvents() {
        initAdapter()

        binding.closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }

        binding.etComment.textChanges().debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isEmpty()) {
                    binding.llMentionUserListContainer.visibility = View.GONE
                } else {
                    val lastChar = it.last().toString()
                    if (lastChar.contains("@")) {
                    } else {
                        val wordList = it.split(" ")
                        val lastWord = wordList.last()
                        val search: String = lastWord.substringAfterLast("@")

                        if (lastWord.contains("@")) {
                            challengeViewModel.getUserForMention(search)
                        } else {
                            binding.llMentionUserListContainer.visibility = View.GONE
                        }
                    }
                }
            }.autoDispose()
        binding.ivSendMessage.throttleClicks().subscribeAndObserveOnMainThread {
            if (challengePostId != 0) {
                if (commentItem == 1) {
                    val content = binding.etComment.text.toString()
                    challengeViewModel.updateChallengePostComment(
                        ChallengeUpdateCommentRequest(
                            commentId,
                            content
                        )
                    )
                } else if (commentItem == 2) {
                    val content = binding.etComment.text.toString()
                    challengeViewModel.updateChallengePostComment(
                        ChallengeUpdateCommentRequest(
                            commentId,
                            content
                        )
                    )
                } else if (replayCommentDetails != null) {
                    val content = binding.etComment.text.toString()
                    if (content.isNotEmpty()) {
                        challengeId?.let {
                            challengeId?.let {
                                challengeViewModel.addChallengePostComment(
                                    AddChallengePostCommentRequest(
                                        challengeId = it,
                                        content = binding.etComment.text.toString(),
                                        parentId = replayCommentDetails?.id,
                                        challengePostId = challengePostId
                                    )
                                )
                            }
                        }
                    }
                } else {
                    val content = binding.etComment.text.toString()
                    if (content.isNotEmpty()) {
                        challengeId?.let {
                            challengeViewModel.addChallengePostComment(
                                AddChallengePostCommentRequest(
                                    challengeId = it,
                                    content = binding.etComment.text.toString(),
                                    challengePostId = challengePostId
                                )
                            )
                        }
                    }
                }
            } else {
                if (commentItem == 1) {
                    val content = binding.etComment.text.toString()
                    challengeViewModel.updateChallengeComment(
                        ChallengeUpdateCommentRequest(
                            commentId,
                            content
                        )
                    )
                } else if (commentItem == 2) {
                    val content = binding.etComment.text.toString()
                    challengeViewModel.updateChallengeComment(
                        ChallengeUpdateCommentRequest(
                            commentId,
                            content
                        )
                    )
                } else if (replayCommentDetails != null) {
                    val content = binding.etComment.text.toString()
                    if (content.isNotEmpty()) {
                        challengeId?.let {
                            challengeId?.let {
                                challengeViewModel.addChallengeComment(
                                    AddChallengeCommentRequest(
                                        challengeId = it,
                                        content = binding.etComment.text.toString(),
                                        parentId = replayCommentDetails?.id
                                    )
                                )
                            }
                        }
                    }
                } else {
                    val content = binding.etComment.text.toString()
                    if (content.isNotEmpty()) {
                        challengeId?.let {
                            challengeId?.let {
                                challengeViewModel.addChallengeComment(
                                    AddChallengeCommentRequest(
                                        challengeId = it,
                                        content = binding.etComment.text.toString(),
                                        parentId = null
                                    )
                                )
                            }
                        }
                    }
                }
            }

        }.autoDispose()
    }

    private fun initAdapter() {
        challengeCommentAdapter = ChallengeCommentAdapter(requireContext()).apply {
            commentItemClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is ChallengeCommentState.AddCommentLikeClick -> {
                        if (challengePostId != 0) {
                            state.dataVideo.id?.let {
                                challengeViewModel.challengePostCommentLikeUnLike(
                                    ChallengePostCommentRequest(
                                        commentId = it, challengePostId = challengePostId
                                    )
                                )
                            }
                        } else {
                            state.dataVideo.id?.let {
                                challengeViewModel.challengeCommentLikeUnLike(
                                    ChallengeCommentRequest(it)
                                )
                            }
                        }

                    }
                    is ChallengeCommentState.DeleteClick -> {
                        val builder = AlertDialog.Builder(requireActivity())
                        builder.setCancelable(false)
                        builder.setMessage(resources.getString(R.string.label_are_you_sure_you_want_to_delete_comment))
                        builder.setPositiveButton(resources.getString(R.string.label_yes)) { dialog, _ ->
                            dialog.dismiss()
                            if (challengePostId != 0) {
                                deleteComment = state.postComments
                                state.postComments.id?.let {
                                    challengeViewModel.deleteChallengePostComment(
                                        ChallengePostDeleteCommentRequest(it)
                                    )
                                }
                            } else {
                                deleteComment = state.postComments
                                state.postComments.id?.let {
                                    challengeViewModel.deleteChallengeComment(
                                        ChallengeCommentRequest(it)
                                    )
                                }
                            }
                        }
                        builder.setNegativeButton(resources.getString(R.string.label_cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        val alert = builder.create()
                        alert.show()
                    }
                    is ChallengeCommentState.EditClick -> {
                        commentItem = 1
                        commentId = state.postComments.id ?: -1

                        binding.etComment.setText(state.postComments.content)
                        binding.etComment.requestFocus()
                        val imm =
                            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
                    }
                    is ChallengeCommentState.RemoveCommentLikeClick -> {
                        if (challengePostId != 0) {
                            state.dataVideo.id?.let {
                                challengeViewModel.challengePostCommentLikeUnLike(
                                    ChallengePostCommentRequest(
                                        commentId = it, challengePostId = challengePostId
                                    )
                                )
                            }
                        } else {
                            state.dataVideo.id?.let {
                                challengeViewModel.challengeCommentLikeUnLike(
                                    ChallengeCommentRequest(it)
                                )
                            }
                        }
                    }
                    is ChallengeCommentState.AddReplyCommentLikeClick -> {
                        if (challengePostId != 0) {
                            state.childComment.id?.let {
                                challengeViewModel.challengePostCommentLikeUnLike(
                                    ChallengePostCommentRequest(
                                        commentId = it, challengePostId = challengePostId
                                    )
                                )
                            }
                        } else {
                            state.childComment.id?.let {
                                challengeViewModel.challengeCommentLikeUnLike(
                                    ChallengeCommentRequest(it)
                                )
                            }
                        }
                    }
                    is ChallengeCommentState.ReplyClick -> {
                        replyComment(state.postComments)
                    }
                    is ChallengeCommentState.ReplyDeleteClick -> {
                        val builder = AlertDialog.Builder(requireActivity())
                        builder.setCancelable(false)
                        builder.setMessage(resources.getString(R.string.label_are_you_sure_you_want_to_delete_reply_comment))
                        builder.setPositiveButton(resources.getString(R.string.label_yes)) { dialog, _ ->
                            dialog.dismiss()
                            deleteReplyComment(state.postComments, state.childCommentItem)

                        }
                        builder.setNegativeButton(resources.getString(R.string.label_cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        val alert = builder.create()
                        alert.show()

                    }
                    is ChallengeCommentState.ReplyEditClick -> {

                        commentItem = 2
                        commentId = state.childComment.id ?: -1
                        parentCommentId = state.childComment.parentId ?: -1
                        binding.etComment.setText(state.childComment.content)
                        binding.etComment.requestFocus()
                        val imm =
                            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
                    }
                    is ChallengeCommentState.ReplyReplyClick -> {}
                    is ChallengeCommentState.UserProfileClick -> {

                        state.userId.let {
                            startActivity(MyProfileActivity.getIntentWithData(requireContext(), it))
                        }
                    }
                }
            }.autoDispose()
        }
        binding.rvChallengeComment.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = challengeCommentAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                if (challengePostId != 0) {
                                    challengePostId?.let {
                                        challengeViewModel.loadMoreChallengePostComment(
                                            it
                                        )
                                    }
                                } else {
                                    challengeId?.let {
                                        challengeViewModel.loadMoreChallengeComment(
                                            it
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }

        mentionUserAdapter = MentionUserAdapter(requireContext()).apply {
            userClicks.subscribeAndObserveOnMainThread { mentionUser ->
                val cursorPosition: Int = binding.etComment.selectionStart
                val descriptionString = binding.etComment.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                challengeViewModel.searchUserClicked(
                    binding.etComment.text.toString(),
                    subString,
                    mentionUser
                )
            }.autoDispose()
        }

        binding.rvMentionUserList.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = mentionUserAdapter
        }
    }

    private fun deleteReplyComment(
        postComments: ChallengeComment,
        childCommentItem: ChildCommentItem
    ) {
        deleteReplyCommentPosition = listOfComment?.indexOf(postComments)
        deleteChildComment = childCommentItem
        if (childCommentItem.id != null) {
            challengeViewModel.deleteChallengeComment(
                ChallengeCommentRequest(
                    commentId = childCommentItem.id,
                    parentId = postComments.id
                )
            )
        }
    }


    private fun replyComment(postComments: ChallengeComment) {
        replayCommentDetails = postComments
        replyCommentPosition = listOfComment?.indexOf(postComments)
        onReplyClicked(postComments)
    }


    private fun onReplyClicked(postComments: ChallengeComment) {
        replayCommentDetails = postComments
        binding.etComment.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
    }

}