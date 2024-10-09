package com.meetfriend.app.ui.myprofile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.chat.model.CreateOneToOneChatRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityMyProfileBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chat.onetoonechatroom.ViewOneToOneChatRoomActivity
import com.meetfriend.app.ui.follow.ProfileFollowingActivity
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.myprofile.view.MyProfileTabAdapter
import com.meetfriend.app.ui.myprofile.viewmodel.UserProfileViewModel
import com.meetfriend.app.ui.myprofile.viewmodel.UserProfileViewState
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FiXED_3_INT
import com.meetfriend.app.utils.ShareHelper
import org.json.JSONObject
import javax.inject.Inject

class MyProfileActivity : BasicActivity() {
    companion object {
        const val USER_ID = "USER_ID"
        const val GET_INFO_FROM_PREVIOUS = 101
        fun getIntent(context: Context): Intent {
            return Intent(context, MyProfileActivity::class.java)
        }

        fun getIntentWithData(context: Context, userId: Int): Intent {
            val intent = Intent(context, MyProfileActivity::class.java)
            intent.putExtra(USER_ID, userId)
            return intent
        }
    }

    private lateinit var binding: ActivityMyProfileBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserProfileViewModel>
    private lateinit var userProfileViewModel: UserProfileViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var myProfileTabAdapter: MyProfileTabAdapter
    private var userId = -1
    private var userProfileInfo: MeetFriendUser? = null
    private var isFollowingCountClickable: Boolean = false
    private var isBlock = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        userProfileViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewModel()
        initUI()
        intent?.let {
            if (it.getBooleanExtra(MainHomeActivity.INTENT_IS_SWITCH_ACCOUNT, false)) {
                intent?.getIntExtra(MainHomeActivity.USER_ID, 0)?.let { userId ->
                    RxBus.publish(RxEvent.switchUserAccount(userId))
                }
            }
        }
    }

    private fun listenToViewModel() {
        userProfileViewModel.userProfileState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserProfileViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is UserProfileViewState.LoadingState -> {
                    showLoading(it.isLoading)
                }

                is UserProfileViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }

                is UserProfileViewState.UserProfileData -> {
                    loadData(it.userProfile)
                    userProfileInfo = it.userProfile

                    if (userId == loggedInUserCache.getLoggedInUserId()) {
                        loggedInUserCache.setLoggedInUser(it.userProfile)
                    }
                    isFollowingCountClickable = isFollowingCountClickable(userProfileInfo)
                }
                is UserProfileViewState.OneToOneChatData -> {
                    val canSendMsg =
                        userProfileInfo?.isFollow == "1" && userProfileInfo?.followBack == 1
                    startActivity(
                        ViewOneToOneChatRoomActivity.getIntentWithData(
                            this,
                            it.chatRoomInfo,
                            true,
                            canSendMsg
                        )
                    )
                }

                is UserProfileViewState.ReportUserSuccess -> {
                    val props = JSONObject()
                    props.put(Constant.CONTENT_TYPE, "profile")
                    props.put(Constant.CONTENT_ID, userId)

                    mp?.track(Constant.REPORT_CONTENT, props)
                    showToast(it.successMessage)
                }

                is UserProfileViewState.BlockSuccessMessage -> {
                    showToast(it.successMessage)
                }

                else -> {
                }
            }
        }.autoDispose()
    }

    private fun isFollowingCountClickable(userProfileInfo: MeetFriendUser?): Boolean {
        return when {
            isPublicProfile(userProfileInfo) -> true
            isPrivateProfileFollowed(userProfileInfo) -> true
            isCurrentUserProfile(userProfileInfo) -> true
            else -> false
        }
    }

    private fun isPublicProfile(userProfileInfo: MeetFriendUser?): Boolean {
        return userProfileInfo?.isPrivate == 0
    }

    private fun isPrivateProfileFollowed(userProfileInfo: MeetFriendUser?): Boolean {
        return userProfileInfo?.isPrivate == 1 && userProfileInfo.isFollow == "1"
    }

    private fun isCurrentUserProfile(userProfileInfo: MeetFriendUser?): Boolean {
        return userProfileInfo?.id == loggedInUserCache.getLoggedInUserId()
    }

    private fun loadData(userProfile: MeetFriendUser) {
        loadProfileImage(userProfile.profilePhoto)
        setUserName(userProfile)
        setBio(userProfile)
        updateCountTexts(userProfile)
        setProfileTitle()
        manageFollowButtonsVisibility(userProfile)
        manageMediaLayoutVisibility(userProfile)
    }

    private fun loadProfileImage(imageUrl: String?) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(resources.getDrawable(R.drawable.image_placeholder, null))
            .into(binding.profilePicRoundedImageView)
    }

    private fun setUserName(userProfile: MeetFriendUser) {
        binding.nameAppCompatTextView.text =
            if (!userProfile.userName.isNullOrEmpty() && userProfile.userName != "null") {
                userProfile.userName
            } else {
                userProfile.firstName.plus(userProfile.lastName)
            }
        binding.usernameAppCompatTextView.text =
            resources.getString(R.string.sign_at_the_rate).plus(userProfile.userName)
    }

    private fun setBio(userProfile: MeetFriendUser) {
        binding.bioAppCompatTextView.text =
            if (userProfile.bio.isNullOrEmpty() || userProfile.bio == "null") "" else userProfile.bio!!
    }

    private fun updateCountTexts(userProfile: MeetFriendUser) {
        binding.tvFollowingCount.text = userProfile.noOfFollowings.toString()
        binding.tvFollowersCount.text = userProfile.noOfFollowers.toString()
        binding.tvLikeCount.text = userProfile.postLike.toString()
    }

    private fun setProfileTitle() {
        binding.titleAppCompatTextView.text =
            if (userId != loggedInUserCache.getLoggedInUserId()) {
                binding.nameAppCompatTextView.text
            } else {
                resources.getString(R.string.my_profile)
            }
    }

    private fun manageFollowButtonsVisibility(userProfile: MeetFriendUser) {
        binding.ivAccountVerified.visibility = if (userProfile.isVerified == 1) View.VISIBLE else View.GONE

        if (userProfile.isPrivate == 1) {
            handlePrivateAccountFollowButtons(userProfile)
        } else {
            handlePublicAccountFollowButtons(userProfile)
        }
    }

    private fun handlePrivateAccountFollowButtons(userProfile: MeetFriendUser) {
        binding.tvRequested.isVisible = userProfile.isRequested == 1
        when {
            userProfile.isFollow == "1" && (userProfile.followBack == 1 || userProfile.followBack == 0) -> {
                binding.tvFollowing.isVisible = true
                binding.tvFollowBack.isVisible = false
                binding.tvFollow.isVisible = false
            }
            userProfile.isFollow == "0" && userProfile.followBack == 1 -> {
                binding.tvFollowing.isVisible = false
                binding.tvFollowBack.visibility = View.VISIBLE
                binding.tvFollow.isVisible = false
            }
            else -> {
                binding.tvFollow.isVisible = true
                binding.tvFollowBack.isVisible = false
                binding.tvFollowing.isVisible = false
            }
        }
    }

    private fun handlePublicAccountFollowButtons(userProfile: MeetFriendUser) {
        when {
            userProfile.isFollow == "0" && userProfile.followBack == 1 -> {
                binding.tvFollowBack.isVisible = true
                binding.tvFollowing.isVisible = false
                binding.tvFollow.isVisible = false
            }
            userProfile.isFollow == "1" && (userProfile.followBack == 0 || userProfile.followBack == 1) -> {
                binding.tvFollowBack.isVisible = false
                binding.tvFollowing.isVisible = true
                binding.tvFollow.isVisible = false
            }
            else -> {
                binding.tvFollowBack.isVisible = false
                binding.tvFollowing.isVisible = false
                binding.tvFollow.isVisible = true
            }
        }
    }

    private fun manageMediaLayoutVisibility(userProfile: MeetFriendUser) {
        if (userProfile.myFriend?.blockStatus == "block") {
            hideAllMediaLayoutElements()
        } else {
            if (userProfile.id == loggedInUserCache.getLoggedInUserId() || userProfile.isPrivate == 0) {
                binding.LinerLayoutMedia.visibility = View.VISIBLE
            } else if (userProfile.isPrivate == 1 && userProfile.isFollow == "1") {
                binding.LinerLayoutMedia.visibility = View.VISIBLE
            } else {
                binding.LinerLayoutMedia.visibility = View.GONE
                binding.LinerLayoutPrivateAccount.visibility = View.VISIBLE
                binding.viewPager.visibility = View.GONE
            }
        }
    }

    private fun hideAllMediaLayoutElements() {
        binding.LinerLayoutMedia.visibility = View.GONE
        binding.LinerLayoutPrivateAccount.visibility = View.GONE
        binding.tvMessage.visibility = View.GONE
        binding.viewPager.visibility = View.GONE
        binding.llCountContainer.visibility = View.GONE
        binding.flFollowActions.visibility = View.GONE
    }

    private fun initUI() {
        setupUserId()
        setupBackButton()
        setupMessageButton()
        handleEditProfileVisibility()
        setupEditButton()
        setupFollowButton()
        setupFollowingButton()
        setupFollowBackButton()
        setupTotalFollowersButton()
        setupTotalFollowingButton()
        setupViewPager()
        setupMoreButton()
    }

    private fun setupUserId() {
        intent?.let {
            val userId = intent.getIntExtra(USER_ID, 0)
            this.userId = if (userId == 0) {
                loggedInUserCache.getLoggedInUserId()
            } else {
                userId
            }
            userProfileViewModel.userProfile(this.userId)
        }
    }

    private fun setupBackButton() {
        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()
    }

    private fun setupMessageButton() {
        binding.tvMessage.throttleClicks().subscribeAndObserveOnMainThread {
            userProfileViewModel.createOneToOneChat(
                CreateOneToOneChatRequest(userProfileInfo?.id ?: 0)
            )
        }.autoDispose()
    }

    private fun handleEditProfileVisibility() {
        if (userId == loggedInUserCache.getLoggedInUserId()) {
            binding.otherUserActionLinearLayout.isVisible = false
            binding.editAppCompatImageView.isVisible = true
            binding.tvFollowing.isVisible = false
            binding.tvFollow.isVisible = false
        }
    }

    private fun setupEditButton() {
        binding.editAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResult(
                EditProfileActivity.getIntent(this@MyProfileActivity, false),
                GET_INFO_FROM_PREVIOUS
            )
        }.autoDispose()
    }

    private fun setupFollowButton() {
        binding.tvFollow.throttleClicks().subscribeAndObserveOnMainThread {
            RxBus.publish(RxEvent.UpdateCanChat(false))
            userProfileViewModel.followUnfollow(userId)
            updateFollowButtonUI()
        }.autoDispose()
    }

    private fun updateFollowButtonUI() {
        if (userProfileInfo?.isPrivate == 1) {
            binding.tvFollow.text = resources.getString(R.string.label_requested)
        } else {
            userProfileInfo?.noOfFollowers = userProfileInfo?.noOfFollowers?.plus(1)
            binding.tvFollowersCount.text = userProfileInfo?.noOfFollowers?.prettyCount().toString()
            binding.tvFollowing.visibility = View.VISIBLE
            binding.tvFollow.visibility = View.GONE
            binding.tvRequested.visibility = View.GONE
        }
    }

    private fun setupFollowingButton() {
        binding.tvFollowing.throttleClicks().subscribeAndObserveOnMainThread {
            userProfileViewModel.followUnfollow(userId)
            updateFollowingButtonUI()
        }.autoDispose()
    }

    private fun updateFollowingButtonUI() {
        userProfileInfo?.noOfFollowers = userProfileInfo?.noOfFollowers?.minus(1)
        binding.tvFollowersCount.text = userProfileInfo?.noOfFollowers?.prettyCount().toString()

        if (userProfileInfo?.followBack == 1) {
            binding.tvFollowing.visibility = View.GONE
            binding.tvFollow.visibility = View.GONE
            binding.tvFollowBack.visibility = View.VISIBLE
        } else {
            binding.tvFollowing.visibility = View.GONE
            binding.tvFollow.visibility = View.VISIBLE
            binding.tvRequested.visibility = View.GONE
        }

        if (userProfileInfo?.isPrivate == 1) {
            binding.LinerLayoutMedia.visibility = View.GONE
            binding.LinerLayoutPrivateAccount.visibility = View.VISIBLE
            binding.viewPager.visibility = View.GONE
        }

        RxBus.publish(RxEvent.UpdateCanChat(false))
    }

    private fun setupFollowBackButton() {
        binding.tvFollowBack.throttleClicks().subscribeAndObserveOnMainThread {
            userProfileViewModel.followUnfollow(userId)
            updateFollowBackButtonUI()
        }.autoDispose()
    }

    private fun updateFollowBackButtonUI() {
        if (userProfileInfo?.isPrivate == 1) {
            binding.tvFollowing.visibility = View.GONE
            binding.tvFollow.visibility = View.GONE
            binding.tvRequested.visibility = View.VISIBLE
            binding.tvFollowBack.visibility = View.GONE
            RxBus.publish(RxEvent.UpdateCanChat(false))
        } else {
            binding.tvFollowing.visibility = View.VISIBLE
            binding.tvFollow.visibility = View.GONE
            binding.tvRequested.visibility = View.GONE
            binding.tvFollowBack.visibility = View.GONE
            RxBus.publish(RxEvent.UpdateCanChat(true))

            userProfileInfo?.noOfFollowers = userProfileInfo?.noOfFollowers?.plus(1)
            binding.tvFollowersCount.text = userProfileInfo?.noOfFollowers?.prettyCount().toString()
        }
    }

    private fun setupTotalFollowersButton() {
        binding.llTotalFollowers.throttleClicks().subscribeAndObserveOnMainThread {
            if (isFollowingCountClickable) {
                startActivity(
                    ProfileFollowingActivity.getIntent(
                        this,
                        userId,
                        false,
                        binding.nameAppCompatTextView.text.toString()
                    )
                )
            }
        }.autoDispose()
    }

    private fun setupTotalFollowingButton() {
        binding.llTotalFollowing.throttleClicks().subscribeAndObserveOnMainThread {
            if (isFollowingCountClickable) {
                startActivity(
                    ProfileFollowingActivity.getIntent(
                        this,
                        userId,
                        true,
                        binding.nameAppCompatTextView.text.toString()
                    )
                )
            }
        }.autoDispose()
    }

    private fun setupViewPager() {
        myProfileTabAdapter = MyProfileTabAdapter(this, userId)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = FiXED_3_INT.toInt()
        binding.viewPager.adapter = myProfileTabAdapter
        binding.viewPager.hackMatchParentCheckInViewPager()

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.tab_post)
                1 -> tab.text = getString(R.string.tab_shorts)
                2 -> tab.text = getString(R.string.tab_liked)
            }
        }.attach()
    }

    private fun setupMoreButton() {
        binding.ivMore.throttleClicks().subscribeAndObserveOnMainThread {
            displayPopupMenu()
        }.autoDispose()
    }

    private fun ViewPager2.hackMatchParentCheckInViewPager() {
        (getChildAt(0) as RecyclerView).clearOnChildAttachStateChangeListeners()
    }

    private fun displayPopupMenu() {
        val layoutInflater = layoutInflater
        val popupView: View = layoutInflater.inflate(R.layout.popup_profile_actions, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setupViewVisibility(popupView)

        popupWindow.isOutsideTouchable = true
        setupClickListeners(popupView, popupWindow)

        popupWindow.showAsDropDown(binding.ivMore)
    }

    private fun setupViewVisibility(popupView: View) {
        val report: LinearLayout = popupView.findViewById(R.id.llReport)
        val block: LinearLayout = popupView.findViewById(R.id.llBlockUser)
        val unblock: LinearLayout = popupView.findViewById(R.id.llUnBlockUser)

        if (userId == loggedInUserCache.getLoggedInUserId()) {
            report.isVisible = false
            block.isVisible = false
        } else {
            if (isBlock) {
                block.isVisible = false
                unblock.isVisible = true
            } else {
                block.isVisible = true
                unblock.isVisible = false
            }
        }
    }

    private fun setupClickListeners(popupView: View, popupWindow: PopupWindow) {
        val profileLink: LinearLayout = popupView.findViewById(R.id.llProfileLink)
        val report: LinearLayout = popupView.findViewById(R.id.llReport)
        val share: LinearLayout = popupView.findViewById(R.id.llShare)
        val block: LinearLayout = popupView.findViewById(R.id.llBlockUser)
        val unblock: LinearLayout = popupView.findViewById(R.id.llUnBlockUser)

        block.setOnClickListener {
            handleBlockUser(popupWindow)
        }

        unblock.setOnClickListener {
            handleUnblockUser(popupWindow)
        }

        profileLink.setOnClickListener {
            shareProfileLink(popupWindow)
        }

        report.setOnClickListener {
            showReportUserDialog(popupWindow)
        }

        share.setOnClickListener {
            shareUserProfile(popupWindow)
        }
    }

    private fun handleBlockUser(popupWindow: PopupWindow) {
        userProfileViewModel.blockUnBlockPeople(userId, "block")
        popupWindow.dismiss()
        hideUserProfileViews()
        isBlock = true
    }

    private fun handleUnblockUser(popupWindow: PopupWindow) {
        userProfileViewModel.blockUnBlockPeople(userId, "unblock")
        popupWindow.dismiss()
        showUserProfileViews()
        isBlock = false
    }

    private fun shareProfileLink(popupWindow: PopupWindow) {
        ShareHelper.shareDeepLink(this@MyProfileActivity, 2, userId, "", true) {
            val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Url", it)
            clipboard.setPrimaryClip(clip)
            showToast(resources.getString(R.string.msg_copied_profile_link))
        }
        popupWindow.dismiss()
    }

    private fun showReportUserDialog(popupWindow: PopupWindow) {
        val reportUserBottomSheet = ReportMainUserBottomSheet.newInstance(userId, false)
        reportUserBottomSheet.show(supportFragmentManager, ReportMainUserBottomSheet::class.java.name)
        popupWindow.dismiss()
    }

    private fun shareUserProfile(popupWindow: PopupWindow) {
        ShareHelper.shareDeepLink(this@MyProfileActivity, 2, userId, "", true) {
            ShareHelper.shareText(this@MyProfileActivity, it)
        }
        popupWindow.dismiss()
    }

    private fun hideUserProfileViews() {
        binding.LinerLayoutMedia.visibility = View.GONE
        binding.LinerLayoutPrivateAccount.visibility = View.GONE
        binding.tvMessage.visibility = View.GONE
        binding.viewPager.visibility = View.GONE
        binding.llCountContainer.visibility = View.GONE
        binding.flFollowActions.visibility = View.GONE
    }

    private fun showUserProfileViews() {
        if ((userProfileInfo?.isFollow != "0" && userProfileInfo?.isPrivate != 0) ||
            (userProfileInfo?.isFollow != "0")
        ) {
            binding.LinerLayoutMedia.visibility = View.VISIBLE
            binding.viewPager.visibility = View.VISIBLE
        } else {
            binding.LinerLayoutPrivateAccount.visibility = View.VISIBLE
        }
        binding.tvMessage.visibility = View.VISIBLE
        binding.llCountContainer.visibility = View.VISIBLE
        binding.flFollowActions.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GET_INFO_FROM_PREVIOUS && resultCode == RESULT_OK) {
            val profile = data?.getStringExtra("profilePicture")
            val uName = data?.getStringExtra("name")
            Glide.with(this).load(profile)
                .placeholder(resources.getDrawable(R.drawable.image_placeholder, null))
                .into(binding.profilePicRoundedImageView)

            binding.nameAppCompatTextView.text = uName
        }
    }

    override fun onResume() {
        super.onResume()
        userProfileViewModel.userProfile(userId)
        binding.root.requestLayout()
    }
}
