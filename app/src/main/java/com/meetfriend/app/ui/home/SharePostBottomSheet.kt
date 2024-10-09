package com.meetfriend.app.ui.home

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.post.model.ShareData
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.SharePostDialogBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getSelectedTagUserIds
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.roomview.view.MentionUserAdapter
import com.meetfriend.app.ui.home.viewmodel.SharePostViewModel
import com.meetfriend.app.ui.home.viewmodel.SharePostViewState
import com.meetfriend.app.utils.Constant.FIX_400_MILLISECOND
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SharePostBottomSheet : BaseBottomSheetDialogFragment() {

    private val shareClicksSubject: PublishSubject<ShareData> = PublishSubject.create()
    val shareClicks: Observable<ShareData> = shareClicksSubject.hide()

    private var _binding: SharePostDialogBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SharePostViewModel>
    private lateinit var sharePostViewModel: SharePostViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    var privacy = "1"

    private lateinit var mentionUserAdapter: MentionUserAdapter
    private var selectedTagUserInfo: MutableList<MeetFriendUser> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

        MeetFriendApplication.component.inject(this)
        sharePostViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SharePostDialogBinding.inflate(inflater, container, false)
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
        loadData()
        listenToViewModel()
        listenToViewEvents()
    }

    private fun loadData() {
        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName == "null" &&
            !loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName.isNullOrEmpty()
        ) {
            binding.tvUsername.text =
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName.plus(" ")
                    .plus(loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName)
        } else {
            binding.tvUsername.text = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName
        }

        Glide.with(this)
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserPicture)

        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }
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

    private fun listenToViewEvents() {
        setupShareButtonClick()
        setupCloseButtonClick()
        setupPrivacySpinner()
        setupMentionUserAdapter()
        setupStoryTextWatcher()
    }

    // Share button click handler
    private fun setupShareButtonClick() {
        binding.tvShareButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                shareClicksSubject.onNext(
                    ShareData(
                        privacy,
                        binding.etStoryText.text.toString(),
                        getSelectedTagUserIds(selectedTagUserInfo, binding.etStoryText.text.toString())
                    )
                )
                dismiss()
            }
        }.autoDispose()
    }

    // Close button click handler
    private fun setupCloseButtonClick() {
        binding.close.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }

    // Privacy spinner setup
    private fun setupPrivacySpinner() {
        binding.postPrivacySpinners.throttleClicks().subscribeAndObserveOnMainThread {
            val arrayList = arrayListOf("Public", "Following")
            val arrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                arrayList
            )
            binding.postPrivacySpinners.setAdapter(arrayAdapter)
            binding.postPrivacySpinners.showDropDown()
            binding.postPrivacySpinners.isSelected = false
        }

        binding.postPrivacySpinners.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                binding.postPrivacySpinners.isSelected = true
                val selectedOptionName = parent.getItemAtPosition(position).toString()
                val drawableRight = ContextCompat.getDrawable(requireContext(), R.drawable.ic_new_drop_down)
                when (selectedOptionName) {
                    resources.getString(R.string.label_public) -> {
                        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_public)
                        binding.postPrivacySpinners.setCompoundDrawablesWithIntrinsicBounds(
                            drawable,
                            null,
                            drawableRight,
                            null
                        )
                        privacy = "1"
                    }
                    resources.getString(R.string.following) -> {
                        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_friend_icon)
                        binding.postPrivacySpinners.setCompoundDrawablesWithIntrinsicBounds(
                            drawable,
                            null,
                            drawableRight,
                            null
                        )
                        privacy = "2"
                    }
                }
            }
    }

    // Mention adapter setup
    private fun setupMentionUserAdapter() {
        mentionUserAdapter = MentionUserAdapter(requireContext()).apply {
            userClicks.subscribeAndObserveOnMainThread { mentionUser ->
                val cursorPosition: Int = binding.etStoryText.selectionStart
                val descriptionString = binding.etStoryText.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                sharePostViewModel.searchUserClicked(binding.etStoryText.text.toString(), subString, mentionUser)
                if (mentionUser !in selectedTagUserInfo) {
                    selectedTagUserInfo.add(mentionUser)
                }
            }.autoDispose()
        }

        binding.rvMentionUserList.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = mentionUserAdapter
        }
    }

    // Text watcher for story text
    private fun setupStoryTextWatcher() {
        binding.etStoryText.textChanges()
            .debounce(FIX_400_MILLISECOND, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isEmpty()) {
                    binding.llMentionUserListContainer.visibility = View.GONE
                } else {
                    handleStoryTextChange(it)
                }
            }.autoDispose()
    }

    // Handle story text change
    private fun handleStoryTextChange(text: CharSequence) {
        val lastChar = text.last().toString()
        if (!lastChar.contains("@")) {
            val wordList = text.split(" ")
            val lastWord = wordList.last()
            val search: String = lastWord.substringAfterLast("@")

            if (lastWord.contains("@")) {
                sharePostViewModel.getUserForMention(search)
            } else {
                binding.llMentionUserListContainer.visibility = View.GONE
            }
        }
    }

    private fun listenToViewModel() {
        sharePostViewModel.sharePostState.subscribeAndObserveOnMainThread {
            when (it) {
                is SharePostViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is SharePostViewState.LoadingState -> {
                }
                is SharePostViewState.UserListForMention -> {
                    mentionUserViewVisibility(!it.listOfUserForMention.isNullOrEmpty())
                    mentionUserAdapter.listOfDataItems = it.listOfUserForMention
                }
                is SharePostViewState.UpdateDescriptionText -> {
                    mentionUserViewVisibility(false)
                    binding.etStoryText.setText(it.descriptionString)
                    binding.etStoryText.setSelection(binding.etStoryText.text.toString().length)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun isValidate(): Boolean {
        return if (binding.etStoryText.text.isNullOrEmpty()) {
            showToast("Please say something about this")
            false
        } else {
            true
        }
    }

    private fun mentionUserViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.llMentionUserListContainer.visibility == View.GONE) {
            binding.llMentionUserListContainer.visibility = View.VISIBLE
        } else if (!isVisibility && binding.llMentionUserListContainer.visibility == View.VISIBLE) {
            binding.llMentionUserListContainer.visibility = View.GONE
        }
    }
}
