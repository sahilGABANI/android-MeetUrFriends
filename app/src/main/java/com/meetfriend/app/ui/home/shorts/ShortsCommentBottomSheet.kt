package com.meetfriend.app.ui.home.shorts

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.post.model.PostCommentResponse
import com.meetfriend.app.api.post.model.ShortsCommentState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomSheetShortsCommentBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.hideKeyboard
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.video.Child_comments
import com.meetfriend.app.responseclasses.video.Post
import com.meetfriend.app.responseclasses.video.Post_comments
import com.meetfriend.app.responseclasses.video.User
import com.meetfriend.app.ui.chatRoom.roomview.view.MentionUserAdapter
import com.meetfriend.app.ui.home.shorts.comment.view.ShortsCommentAdapter
import com.meetfriend.app.ui.home.shorts.comment.viewmodel.ViewPostViewModel
import com.meetfriend.app.ui.home.shorts.comment.viewmodel.ViewPostViewState
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FIX_100_MILLISECOND
import com.meetfriend.app.utils.Constant.FIX_400_MILLISECOND
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ShortsCommentBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {

        const val INTENT_POST_ID = "INTENT_POST_ID"
        const val INTENT_IS_POST = "INTENT_IS_POST"

        fun newInstance(
            postId: String,
            isPost: Boolean = false,
        ): ShortsCommentBottomSheet {
            val args = Bundle()

            postId.let { args.putString(INTENT_POST_ID, it) }
            isPost.let { args.putBoolean(INTENT_IS_POST, it) }
            val fragment = ShortsCommentBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: BottomSheetShortsCommentBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ViewPostViewModel>
    private lateinit var viewPostViewModel: ViewPostViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var shortsCommentAdapter: ShortsCommentAdapter
    private lateinit var mentionUserAdapter: MentionUserAdapter
    private var isFromPost: Boolean = false
    private var postId = ""
    private var dataPost: Post? = null
    private var editCommentData: Post_comments? = null
    private var replyCommentData: Post_comments? = null
    private var editReplyCommentData: Child_comments? = null
    private var deleteCommentReplyData: Post_comments? = null
    private var listOfComment: MutableList<Post_comments>? = null
    private var deleteCommentPosition: Int? = null
    private var deleteCommentReplyPosition: Int? = null
    private var editCommentPosition: Int? = null
    private var replyCommentPosition: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeR)
        MeetFriendApplication.component.inject(this)
        viewPostViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_shorts_comment, container, false)
        _binding = BottomSheetShortsCommentBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheetView = bottomSheetDialog
                .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheetView != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            }
        }

        arguments?.let {
            postId = it.getString(INTENT_POST_ID) as String
            isFromPost = it.getBoolean(INTENT_IS_POST, false)
        }

        Glide.with(this).load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivUserProfile)
        viewPostViewModel.viewPost(postId)
        listenToViewModel()
        listenToViewEvent()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                return
            }
        }

    private fun listenToViewEvent() {
        setupBackButton()
        setupSendMessageButton()
        setupShortsCommentAdapter()
        setupMentionUserAdapter()
        setupCommentEditText()
        setupRecyclerView()
    }

    private fun setupBackButton() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }

    private fun setupSendMessageButton() {
        binding.ivSendMessage.throttleClicks().subscribeAndObserveOnMainThread {
            when {
                editCommentData != null -> updateComment(editCommentData!!)
                editReplyCommentData != null -> updateCommentChild(editReplyCommentData!!)
                else -> addCommentApi()
            }
        }.autoDispose()
    }

    private fun updateCommentChild(editReplyCommentData: Child_comments) {
        viewPostViewModel.updateComment(editReplyCommentData.id, binding.etComment.text.toString())
    }

    private fun updateComment(commentData: Post_comments) {
        viewPostViewModel.updateComment(commentData.id, binding.etComment.text.toString())
    }

    private fun setupShortsCommentAdapter() {
        shortsCommentAdapter = ShortsCommentAdapter(requireContext()).apply {
            commentClicks.subscribeAndObserveOnMainThread { handleCommentClick(it) }.autoDispose()
            replyCommentClicks.subscribeAndObserveOnMainThread { handleReplyClick(it) }.autoDispose()
        }
    }

    private fun handleCommentClick(state: ShortsCommentState) {
        when (state) {
            is ShortsCommentState.DeleteClick -> deleteComment(state.postComments)
            is ShortsCommentState.EditClick -> editComment(state.postComments)
            is ShortsCommentState.ReplyClick -> replyComment(state.postComments)
            is ShortsCommentState.MentionUserClick -> handleMentionUserClick(state)
            else -> {}
        }
    }

    private fun handleMentionUserClick(state: ShortsCommentState.MentionUserClick) {
        val tagsList = state.postComments.mention_comments
        if (tagsList?.any { it.user.userName == state.mentionText } == true) {
            startActivity(
                MyProfileActivity.getIntentWithData(
                    requireContext(),
                    tagsList.first { it.user.userName == state.mentionText }.user.id
                )
            )
        } else {
            showToast("User not found")
        }
    }

    private fun handleReplyClick(state: ShortsCommentState) {
        when (state) {
            is ShortsCommentState.ReplyDeleteClick -> handleReplyDeleteClick(state)
            is ShortsCommentState.ReplyEditClick -> editCommentReply(state.childComment)
            is ShortsCommentState.ReplyReplyClick -> replyCommentReply(state.childComment)
            is ShortsCommentState.ReplyMentionUserClick -> handleReplyMentionUserClick(state)
            else -> {}
        }
    }

    private fun handleReplyDeleteClick(state: ShortsCommentState.ReplyDeleteClick) {
        val parentId = state.childComment.parent_id
        deleteCommentReplyData = dataPost?.post_comments?.find { it.id == parentId }
        deleteCommentReplyPosition = deleteCommentReplyData?.child_comments?.indexOf(state.childComment)
        onDeleteReplyClick(state.childComment)
    }

    private fun handleReplyMentionUserClick(state: ShortsCommentState.ReplyMentionUserClick) {
        val tagsList = state.childComment.mention_comments
        if (tagsList?.any { it.user.userName == state.mentionText } == true) {
            startActivity(
                MyProfileActivity.getIntentWithData(
                    requireContext(),
                    tagsList.first {
                        it.user.userName == state.mentionText
                    }.user.id
                )
            )
        } else {
            showToast("User not found")
        }
    }

    private fun setupMentionUserAdapter() {
        mentionUserAdapter = MentionUserAdapter(requireContext()).apply {
            userClicks.subscribeAndObserveOnMainThread { mentionUser ->
                handleMentionUserClick(mentionUser)
            }.autoDispose()
        }

        binding.rvMentionUserList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMentionUserList.adapter = mentionUserAdapter
    }

    private fun handleMentionUserClick(mentionUser: MeetFriendUser) {
        val cursorPosition: Int = binding.etComment.selectionStart
        val descriptionString = binding.etComment.text.toString()
        val subString = descriptionString.subSequence(0, cursorPosition).toString()
        viewPostViewModel.searchUserClicked(binding.etComment.text.toString(), subString, mentionUser)
    }

    private fun setupCommentEditText() {
        binding.etComment.textChanges()
            .debounce(FIX_400_MILLISECOND, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread { handleCommentTextChange(it) }
            .autoDispose()
    }

    private fun handleCommentTextChange(text: CharSequence) {
        if (text.isEmpty()) {
            binding.llMentionUserListContainer.visibility = View.GONE
        } else {
            val lastChar = text.last().toString()
            if (!lastChar.contains("@")) {
                val lastWord = text.split(" ").last()
                if (lastWord.contains("@")) {
                    val search: String = lastWord.substringAfterLast("@")
                    viewPostViewModel.getUserForMention(search)
                } else {
                    binding.llMentionUserListContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvComments.apply {
            adapter = shortsCommentAdapter
            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom < oldBottom) {
                    binding.rvComments.postDelayed({
                        listOfComment?.lastIndex?.let { lastIndex ->
                            binding.rvComments.smoothScrollToPosition(lastIndex)
                        }
                    }, FIX_100_MILLISECOND)
                }
            }
        }
    }

    private fun listenToViewModel() {
        viewPostViewModel.viewPostState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is ViewPostViewState.ErrorMessage -> handleErrorMessage(state)
                is ViewPostViewState.LoadingState -> handleLoadingState(state)
                is ViewPostViewState.PostData -> handlePostData(state)
                is ViewPostViewState.CommentReplyResponse -> handleCommentReplyResponse(state)
                is ViewPostViewState.CommentResponse -> handleCommentResponse(state)
                is ViewPostViewState.DeleteCommentMessage -> handleDeleteCommentMessage()
                is ViewPostViewState.UpdateCommentResponse -> handleUpdateCommentResponse()
                is ViewPostViewState.UserListForMention -> handleUserListForMention(state)
                is ViewPostViewState.UpdateDescriptionText -> handleUpdateDescriptionText(state)
                else -> {}
            }
        }.autoDispose()
    }

    private fun handleErrorMessage(state: ViewPostViewState.ErrorMessage) {
        showToast(state.errorMessage)
    }

    private fun handleLoadingState(state: ViewPostViewState.LoadingState) {
        manageLoadingState(state.isLoading)
    }

    private fun handlePostData(state: ViewPostViewState.PostData) {
        dataPost = state.postData
        listOfComment = state.postData.post_comments
        shortsCommentAdapter.listOfDataItems = listOfComment
        manageEmptyState()
    }

    private fun handleCommentReplyResponse(state: ViewPostViewState.CommentReplyResponse) {
        val replyUser = User(
            id = state.commentResponse.user.id,
            firstName = state.commentResponse.user.firstName.toString(),
            lastName = state.commentResponse.user.lastName.toString(),
            profile_photo = state.commentResponse.user.profilePhoto.toString(),
            cover_photo = state.commentResponse.user.profilePhoto,
            gender = "",
            userName = state.commentResponse.user.userName
        )

        val addedReply = Child_comments(
            id = state.commentResponse.id,
            post_id = state.commentResponse.postId,
            user_id = state.commentResponse.userId,
            parent_id = state.commentResponse.parentId,
            content = state.commentResponse.content.toString(),
            created_at = state.commentResponse.createdAt.toString(),
            user = replyUser,
            replied_to_user_id = "",
            replied_to_user = "",
            mention_comments = state.commentResponse.mentionComments
        )

        replyCommentPosition?.let { position ->
            listOfComment?.get(position)?.child_comments?.add(addedReply)
        }
        binding.etComment.setText("")
        shortsCommentAdapter.listOfDataItems = listOfComment
        replyCommentData = null
        replyCommentPosition = null
        hideKeyboard()
    }

    private fun handleCommentResponse(state: ViewPostViewState.CommentResponse) {
        addComment(state.commentResponse)
    }

    private fun handleDeleteCommentMessage() {
        when {
            deleteCommentPosition != null -> {
                deleteCommentPosition?.let { position ->
                    listOfComment?.removeAt(position)
                }
            }
            deleteCommentReplyData != null -> {
                deleteCommentReplyData?.child_comments?.removeAt(deleteCommentReplyPosition ?: return)
            }
        }
        shortsCommentAdapter.listOfDataItems = listOfComment
        manageEmptyState()
        hideKeyboard()
    }

    private fun handleUpdateCommentResponse() {
        hideKeyboard()
        val newContent = binding.etComment.text.toString()
        editReplyCommentData?.let {
            it.content = newContent
        } ?: editCommentPosition?.let { position ->
            listOfComment?.elementAt(position)?.content = newContent
        }

        binding.etComment.setText("")
        resetCommentEditState()
    }

    private fun resetCommentEditState() {
        editCommentPosition = null
        editCommentData = null
        editReplyCommentData = null
        replyCommentData = null
    }

    private fun handleUserListForMention(state: ViewPostViewState.UserListForMention) {
        mentionUserViewVisibility(!state.listOfUserForMention.isNullOrEmpty())
        mentionUserAdapter.listOfDataItems = state.listOfUserForMention
    }

    private fun handleUpdateDescriptionText(state: ViewPostViewState.UpdateDescriptionText) {
        mentionUserViewVisibility(false)
        binding.etComment.setText(state.descriptionString)
        binding.etComment.setSelection(binding.etComment.text.toString().length)
    }

    private fun mentionUserViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.llMentionUserListContainer.visibility == View.GONE) {
            binding.llMentionUserListContainer.visibility = View.VISIBLE
        } else if (!isVisibility && binding.llMentionUserListContainer.visibility == View.VISIBLE) {
            binding.llMentionUserListContainer.visibility = View.GONE
        }
    }

    private fun addComment(commentResponse: PostCommentResponse) {
        binding.etComment.setText("")

        val user = User(
            id = commentResponse.user.id,
            firstName = commentResponse.user.firstName.toString(),
            lastName = commentResponse.user.lastName.toString(),
            profile_photo = commentResponse.user.profilePhoto.toString(),
            cover_photo = commentResponse.user.profilePhoto,
            gender = "",
            userName = commentResponse.user.userName,
        )

        val addedComment = Post_comments(
            id = commentResponse.id,
            post_id = commentResponse.postId,
            user_id = commentResponse.userId,
            parent_id = commentResponse.parentId.toString(),
            content = commentResponse.content,
            created_at = commentResponse.createdAt,
            user = user,
            replied_to_user_id = "",
            mention_comments = commentResponse.mentionComments
        )
        listOfComment?.add(0, addedComment)
        shortsCommentAdapter.listOfDataItems = listOfComment
        binding.rvComments.scrollToPosition(0)
        manageEmptyState()
        hideKeyboard()
    }

    private fun manageLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rlCommentContainer.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.rlCommentContainer.visibility = View.VISIBLE
        }
    }

    private fun manageEmptyState() {
        if (listOfComment.isNullOrEmpty()) {
            binding.llEmptyState.visibility = View.VISIBLE
            binding.rvComments.visibility = View.GONE
        } else {
            binding.llEmptyState.visibility = View.GONE
            binding.rvComments.visibility = View.VISIBLE
        }
    }

    private fun addCommentApi() {
        if (replyCommentData != null) {
            replyCommentData?.let {
                val content = binding.etComment.text.toString()
                if (content.isNotEmpty()) {
                    viewPostViewModel.commentReply(
                        dataPost!!.id,
                        binding.etComment.text.toString(),
                        it.id
                    )
                }
            }
        } else {
            val content = binding.etComment.text.toString()
            if (content.isNotEmpty()) {
                dataPost?.id?.let {
                    viewPostViewModel.commentPost(
                        it,
                        binding.etComment.text.toString()
                    )
                }
            }
        }
    }

    private fun onDeleteClick(data: Post_comments) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
        builder.setMessage("Are you sure, you want to delete this comment?")
        builder.setPositiveButton(
            "Yes"
        ) { dialog, _ ->
            dialog.dismiss()
            viewPostViewModel.deleteComment(data.id)
        }
        builder.setNegativeButton(
            "No"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun onDeleteReplyClick(data: Child_comments) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
        builder.setMessage("Are you sure, you want to delete this comment?")
        builder.setPositiveButton(
            "Yes"
        ) { dialog, _ ->
            dialog.dismiss()
            viewPostViewModel.deleteComment(data.id)
        }
        builder.setNegativeButton(
            "No"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun onEditCommentClicked(data: Post_comments) {
        editCommentData = data
        binding.etComment.setText(data.content)
        binding.etComment.setSelection(binding.etComment.length())
        binding.etComment.requestFocus()
        showKeyboard()
    }

    private fun onEditCommentReplyClicked(data: Child_comments) {
        editReplyCommentData = data
        binding.etComment.setText(data.content)
        binding.etComment.setSelection(binding.etComment.length())
        binding.etComment.requestFocus()
        showKeyboard()
    }

    private fun onReplyClicked(data: Post_comments) {
        replyCommentData = data
        binding.etComment.requestFocus()
        showKeyboard()
    }

    private fun deleteComment(postComment: Post_comments) {
        editCommentData = null
        editReplyCommentData = null
        replyCommentData = null
        deleteCommentPosition = listOfComment?.indexOf(postComment)
        onDeleteClick(postComment)
    }

    private fun editComment(postComment: Post_comments) {
        editReplyCommentData = null
        replyCommentData = null
        editCommentData = postComment
        editCommentPosition = listOfComment?.indexOf(postComment)
        onEditCommentClicked(postComment)
    }

    private fun replyComment(postComment: Post_comments) {
        editCommentData = null
        editReplyCommentData = null
        replyCommentPosition = listOfComment?.indexOf(postComment)
        onReplyClicked(postComment)
    }

    private fun editCommentReply(childComment: Child_comments) {
        editCommentData = null
        replyCommentData = null
        editReplyCommentData = childComment
        onEditCommentReplyClicked(childComment)
    }

    private fun replyCommentReply(childComment: Child_comments) {
        editCommentData = null
        editReplyCommentData = null
        val parentId = childComment.parent_id
        val parentComment = dataPost?.post_comments?.find {
            it.id == parentId
        }
        replyCommentPosition = listOfComment?.indexOf(parentComment)
        parentComment?.let {
            onReplyClicked(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataPost?.type.equals(Constant.TYPE_SHORTS)) {
            RxBus.publish(RxEvent.CommentUpdate(dataPost))
        } else {
            RxBus.publish(RxEvent.PostCommentUpdate(dataPost))
        }
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
    }
}
