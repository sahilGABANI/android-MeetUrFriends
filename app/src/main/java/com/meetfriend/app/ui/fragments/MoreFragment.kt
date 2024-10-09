package com.meetfriend.app.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessaging
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentMoreBinding
import com.meetfriend.app.newbase.BasicFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.activities.LoginActivity
import com.meetfriend.app.ui.activities.ResetPasswordActivity
import com.meetfriend.app.ui.activities.SettingActivity
import com.meetfriend.app.ui.bottomsheet.auth.AddAccountBottomSheet
import com.meetfriend.app.ui.bottomsheet.auth.switchaccount.SwitchAccountBottomSheet
import com.meetfriend.app.ui.chatRoom.ChatRoomActivity
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewState
import com.meetfriend.app.ui.follow.request.FollowRequestActivity
import com.meetfriend.app.ui.giftsGallery.GiftsGalleryActivity
import com.meetfriend.app.ui.helpnsupport.HelpNSupportActivity
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.monetization.NewMonetizationActivity
import com.meetfriend.app.ui.monetization.earnings.EarningsActivity
import com.meetfriend.app.ui.more.blockedUser.BlockedUserActivity
import com.meetfriend.app.ui.mygifts.GiftsTransactionActivity
import com.meetfriend.app.ui.myprofile.MyProfileActivity
import com.meetfriend.app.utilclasses.UtilsClass
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FileUtils
import com.mixpanel.android.mpmetrics.MixpanelAPI
import contractorssmart.app.utilsclasses.PreferenceHandler
import org.json.JSONException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class MoreFragment : BasicFragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatRoomViewModel>
    private lateinit var moreViewModel: ChatRoomViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var isHubRequestSent: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
        moreViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        listenToViewModel()
        listenToViewEvent()
        mp = MixpanelAPI.getInstance(requireContext(), "9baf1cfb430c0de219529759f0b22395", true)
        if (loggedInUserCache.getLoggedInUserId() != 0) {
            moreViewModel.checkUser(loggedInUserCache.getLoggedInUserId().toString())
        }
    }

    @Throws(JSONException::class)
    private fun sendToMixpanel() {
        mp?.track("Logout")
    }

    private fun listenToViewModel() {
        moreViewModel.chatRoomState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatRoomViewState.ErrorMessage -> showError(it.errorMessage)
                is ChatRoomViewState.GetProfileData -> handleGetProfileData(it.profileData.data?.chatUserName)
                is ChatRoomViewState.DeleteUserSuccessMessage -> handleDeleteUserSuccess()
                is ChatRoomViewState.CheckUserData -> handleCheckUserData(it.data.hubRequest)
                is ChatRoomViewState.LogOutAll -> handleLogOutAll()
                is ChatRoomViewState.LogOut -> handleLogOut()
                is ChatRoomViewState.LogInOtherAccount -> handleLogInOtherAccount()
                else -> {}
            }
        }.autoDispose()
    }

    private fun showError(errorMessage: String) {
        showToast(errorMessage)
    }

    private fun handleGetProfileData(chatUserName: String?) {
        startActivity(
            ChatRoomActivity.getIntent(
                requireContext(),
                chatUserName ?: ""
            )
        )
    }

    private fun handleDeleteUserSuccess() {
        UtilsClass.updateUserStatus(requireActivity(), false)
        PreferenceHandler.writeString(
            requireContext(),
            PreferenceHandler.SHOW_SUGGESTION,
            ""
        )
        clearFirebaseToken()
        loggedInUserCache.clearUserPreferences()
        FileUtils.loginUserTokenSharedPreference.edit().apply {
            clear()
            apply()
        }
        startActivity(Intent(requireActivity(), LoginActivity::class.java))
        requireActivity().finish()
    }

    private fun handleCheckUserData(hubRequest: Boolean?) {
        isHubRequestSent = hubRequest
        loggedInUserCache.setIsHubRequestSent(hubRequest ?: false)
    }

    private fun handleLogOutAll() {
        sendToMixpanel()
        UtilsClass.updateUserStatus(requireActivity(), false)
        PreferenceHandler.writeString(
            requireContext(),
            PreferenceHandler.SHOW_SUGGESTION,
            ""
        )
        clearFirebaseToken()
        loggedInUserCache.clearUserPreferences()
        FileUtils.loginUserTokenSharedPreference.edit().apply {
            clear()
            apply()
        }
        startActivity(MainHomeActivity.getIntent(requireContext(), false))
        requireActivity().finish()
    }

    private fun handleLogOut() {
        sendToMixpanel()
        UtilsClass.updateUserStatus(requireActivity(), false)
        PreferenceHandler.writeString(
            requireContext(),
            PreferenceHandler.SHOW_SUGGESTION,
            ""
        )
        clearFirebaseToken()
        loggedInUserCache.clearUserPreferences()
        FileUtils.loginUserTokenSharedPreference.edit().apply {
            clear()
            apply()
        }
        startActivity(MainHomeActivity.getIntent(requireContext(), false))
        requireActivity().finish()
    }

    private fun handleLogInOtherAccount() {
        startActivity(Intent(requireActivity(), MainHomeActivity::class.java))
    }

    private fun listenToViewEvent() {
        binding.apply {
            setupClickListeners()
            setHeader()
            loggedInUserCache.setChatUser(null)
        }
    }

    private fun setupClickListeners() {
        setupThrottleClickListeners()
        setupProfileClickListeners()
        setupOtherClickListeners()
    }

    private fun setupThrottleClickListeners() {
        binding.apply {
            helpNSupport.throttleClicks().subscribeAndObserveOnMainThread {
                handleHelpNSupportClick()
            }.autoDispose()

            sendFeedback.throttleClicks().subscribeAndObserveOnMainThread {
                handleSendFeedbackClick()
            }.autoDispose()

            logoutLayout.throttleClicks().subscribeAndObserveOnMainThread {
                logout()
            }.autoDispose()

            resetPassword.throttleClicks().subscribeAndObserveOnMainThread {
                handleResetPasswordClick()
            }.autoDispose()

            myProfileLayout.throttleClicks().subscribeAndObserveOnMainThread {
                handleMyProfileClick()
            }.autoDispose()

            settingsLayout.throttleClicks().subscribeAndObserveOnMainThread {
                handleSettingsClick()
            }.autoDispose()

            giftlayout.throttleClicks().subscribeAndObserveOnMainThread {
                handleGiftLayoutClick()
            }.autoDispose()

            mygiftnew.throttleClicks().subscribeAndObserveOnMainThread {
                handleMyGiftClick()
            }.autoDispose()

            followRequestsLayout.throttleClicks().subscribeAndObserveOnMainThread {
                handleFollowRequestsClick()
            }.autoDispose()

            blocKLinerLayout.throttleClicks().subscribeAndObserveOnMainThread {
                handleBlockListClick()
            }.autoDispose()

            cvChatRoom.throttleClicks().subscribeAndObserveOnMainThread {
                moreViewModel.getChatRoomUser()
                handleCommonAdsClick()
            }.autoDispose()

            profileLayout.throttleClicks().subscribeAndObserveOnMainThread {
                openSwitchUserPopup()
            }.autoDispose()

            ivNextForSwitchAccount.throttleClicks().subscribeAndObserveOnMainThread {
                openSwitchUserPopup()
            }.autoDispose()

            addAccountLayout.throttleClicks().subscribeAndObserveOnMainThread {
                val addAccountBottomSheet = AddAccountBottomSheet.newInstance()
                addAccountBottomSheet.show(childFragmentManager, MoreFragment::class.java.name)
            }.autoDispose()

            logoutAllLayout.throttleClicks().subscribeAndObserveOnMainThread {
                logoutAll()
            }.autoDispose()
        }
    }

    private fun setupProfileClickListeners() {
        binding.apply {
            name.throttleClicks().subscribeAndObserveOnMainThread {
                startActivity(
                    MyProfileActivity
                        .getIntentWithData(
                            requireContext(),
                            loggedInUserCache.getLoggedInUserId()
                        )
                )
            }.autoDispose()

            profileimage.throttleClicks().subscribeAndObserveOnMainThread {
                startActivity(
                    MyProfileActivity
                        .getIntentWithData(
                            requireContext(),
                            loggedInUserCache.getLoggedInUserId()
                        )
                )
            }.autoDispose()
        }
    }

    private fun setupOtherClickListeners() {
        binding.moniimage.setOnClickListener {
            if (isHubRequestSent == true || loggedInUserCache.getIsHubRequestSent()) {
                startActivity(EarningsActivity.getIntent(requireContext()))
            } else {
                startActivity(NewMonetizationActivity.getIntent(requireContext()))
            }
            handleCommonAdsClick()
        }
    }

    private fun handleHelpNSupportClick() {
        startActivity(HelpNSupportActivity.getIntent(requireContext()))
        handleCommonAdsClick()
    }

    private fun handleSendFeedbackClick() {
        createPlayStoreIntent()
        handleCommonAdsClick()
    }

    private fun handleResetPasswordClick() {
        val intent = Intent(requireContext(), ResetPasswordActivity::class.java)
        startActivity(intent)
        handleCommonAdsClick()
    }

    private fun handleMyProfileClick() {
        startActivity(MyProfileActivity.getIntent(requireContext()))
        handleCommonAdsClick()
    }

    private fun handleSettingsClick() {
        val intent = Intent(requireContext(), SettingActivity::class.java)
        startActivity(intent)
        handleCommonAdsClick()
    }

    private fun handleGiftLayoutClick() {
        startActivity(GiftsGalleryActivity.getIntent(requireContext(), isFrom = "side", null, null))
        handleCommonAdsClick()
    }

    private fun handleMyGiftClick() {
        startActivity(GiftsTransactionActivity.getIntent(requireContext()))
        handleCommonAdsClick()
    }

    private fun handleFollowRequestsClick() {
        startActivity(FollowRequestActivity.getIntent(requireContext()))
        handleCommonAdsClick()
    }

    private fun handleBlockListClick() {
        startActivity(BlockedUserActivity.getIntent(requireContext()))
        handleCommonAdsClick()
    }

    private fun handleCommonAdsClick() {
        openInterstitialAds()
        Constant.CLICK_COUNT++
    }

    private fun openSwitchUserPopup() {
        val switchAccountBottomSheet = SwitchAccountBottomSheet.newInstance()
        switchAccountBottomSheet.show(childFragmentManager, MoreFragment::class.java.name)
    }

    @SuppressLint("HardwareIds")
    private fun logoutAll() {
        val builder =
            AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setMessage("Are you sure, you want to Logout all accounts?")
        builder.setPositiveButton(
            "Yes"
        ) { dialog, _ ->
            val deviceId = Settings.Secure.getString(
                requireActivity().contentResolver,
                Settings.Secure.ANDROID_ID
            )
            moreViewModel.logOutAll(deviceId)
            dialog.dismiss()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun setHeader() {
        Glide.with(requireContext())
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto)
            .placeholder(R.drawable.new_image_place)
            .into(binding.profileimage)
        val userName = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userName ?: ""
        binding.name.text = if (userName.isNotEmpty() && userName != "null") {
            userName
        } else {
            "${loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName ?: ""} " +
                "${loggedInUserCache.getLoggedInUser()?.loggedInUser?.lastName ?: ""}"
        }
        val locationText = loggedInUserCache.getLoggedInUser()?.loggedInUser?.city

        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }
        if (!locationText.isNullOrEmpty() && locationText != "null") {
            binding.location.text = locationText
        } else {
            binding.location.visibility = View.GONE
        }
    }

    @SuppressLint("HardwareIds")
    private fun logout() {
        val builder =
            AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setMessage("Are you sure, you want to logout?")
        builder.setPositiveButton(
            "Yes"
        ) { dialog, _ ->
            val deviceId = Settings.Secure.getString(
                requireActivity().contentResolver,
                Settings.Secure.ANDROID_ID
            )
            moreViewModel.logOut(deviceId)
            dialog.dismiss()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun clearFirebaseToken() {
        FirebaseMessaging.getInstance().apply {
            deleteToken()
        }
    }

    override fun onResume() {
        super.onResume()
        setHeader()
    }

    private fun createPlayStoreIntent() {
        val uri: Uri = Uri.parse("market://details?id=com.meetfriend.app")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)

        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: IOException) {
            Timber.e(e, "Error:-$e")
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=com.meetfriend.app")
                )
            )
        }
    }
}
