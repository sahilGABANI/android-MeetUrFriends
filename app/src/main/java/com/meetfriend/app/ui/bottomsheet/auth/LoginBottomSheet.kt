package com.meetfriend.app.ui.bottomsheet.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.messaging.FirebaseMessaging
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.LoginRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentLoginBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.hideKeyboard
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.Result
import com.meetfriend.app.ui.bottomsheet.auth.model.LoginData
import com.meetfriend.app.ui.login.viewmodel.LoginViewModel
import com.meetfriend.app.ui.login.viewmodel.LoginViewState
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utils.FileUtils
import com.mixpanel.android.mpmetrics.MixpanelAPI
import contractorssmart.app.utilsclasses.CommonMethods
import contractorssmart.app.utilsclasses.PreferenceHandler
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.*
import javax.inject.Inject

class LoginBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = LoginBottomSheet()
    }

    private var _binding: FragmentLoginBottomSheetBinding? = null
    private val binding
        get() = _binding ?: error("LoginBottomSheet should not be accessed when it's null")

    private var deviceToken: String? = ""
    var googleSignInClient: GoogleSignInClient? = null
    val rcSignIn: Int = 1
    private var isRember: Boolean = false

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var loginViewModel: LoginViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var googleSignInHelper: GoogleSignInHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSViewDialogTheme)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        googleSignInHelper = GoogleSignInHelper()
        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheetView =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheetView != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.skipCollapsed = true
                behavior.isDraggable = false
                behavior.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }
        }

        MeetFriendApplication.component.inject(this)
        loginViewModel = getViewModelFromFactory(viewModelFactory)
        mp = MixpanelAPI.getInstance(requireContext(), "9baf1cfb430c0de219529759f0b22395", false)

        listenToViewModel()
        listenToViewEvents()

        if (loggedInUserCache.getLoggedUserRememberMe()) {
            binding.editEmailPhone.setText(loggedInUserCache.getLoggedInUserEmailOrPhone())
            binding.editPassword.setText(loggedInUserCache.getLoggedUserPassword())
            binding.checkBoxRememberMe.isChecked = true
        }
        // new code for token
        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                deviceToken = task.result
            }
        )

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private val bottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        }

    @Throws(JSONException::class)
    private fun sendToMixpanel(type: String) {
        val props = JSONObject()
        props.put("login_type", type)
        mp?.track("Login", props)
    }

    private fun listenToViewEvents() {
        with(binding) {
            dismissBtn.throttleClicks().subscribeAndObserveOnMainThread {
                dismiss()
            }.autoDispose()

            tvForgotPass.throttleClicks().subscribeAndObserveOnMainThread {
                openForgotPasswordBottomSheet()
                dismiss()
            }.autoDispose()

            binding.googleView.throttleClicks().subscribeAndObserveOnMainThread {
                sendToMixpanel("google")
                val signInIntent: Intent? = googleSignInClient?.signInIntent
                if (signInIntent != null) {
                    startActivityForResult(signInIntent, rcSignIn)
                }
            }.autoDispose()

            binding.tvLoginBtn.throttleClicks().subscribeAndObserveOnMainThread {
                hideKeyboard()
                sendToMixpanel("email_or_phone")

                if (binding.editEmailPhone.text.toString().trim() == "") {
                    showToast("Email is empty")
                } else if (binding.editPassword.text.toString().trim() == "") {
                    showToast("Password is empty")
                } else {
                    googleSignInHelper.prepareData(
                        LoginData(
                            email = binding.editEmailPhone.text.toString().trim(),
                            password = binding.editPassword.text.toString().trim(),
                            loginType = "normal",
                            googleId = "",
                            firstName = "",
                            lastName = ""
                        )
                    )
                }
            }.autoDispose()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == rcSignIn) {
            data?.let {
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                try {
//                    handleSignInResult(result)
                    googleSignInHelper.handleSignInResult(result)
                } catch (e: IOException) {
                    Timber.e(e, "An error occurred $e")
                }
            }
        }
    }
    private inner class GoogleSignInHelper {
        fun handleSignInResult(result: GoogleSignInResult?) {
            if (result != null && result.isSuccess) {
                // Signed in successfully, show authenticated UI.
                val acct: GoogleSignInAccount? = result.signInAccount
                if (acct != null) {
                    val personName = acct.displayName ?: ""
                    if (acct != null) {
                        val names = personName.split(" ")
                        val fname = if (names.isNotEmpty()) names[0] else "" // First name (if available)
                        val lname = if (names.size > 1) names[1] else "" // Last name (if available)
                        val email = acct.email ?: ""
                        val id = acct.id ?: ""

                        prepareData(LoginData(email, "", "google", id, fname, lname))
                    } else {
                        val email = acct.email ?: ""
                        val id = acct.id ?: ""
                        prepareData(LoginData(email, "", "google", id, personName, ""))
                    }
                }
            }
        }
        fun prepareData(loginData: LoginData) {
            val email = loginData.email
            val password = loginData.password
            val loginType = loginData.loginType
            val googleId = loginData.googleId
            val etFirstName = loginData.firstName
            val etLastName = loginData.lastName
            if (binding.checkBoxRememberMe.isChecked) {
                rememberMeIsChecked(true)
            } else {
                loggedInUserCache.setLoggedInUserRememberMe(false)
            }

            if (deviceToken.equals("")) {
                CommonMethods.showToastMessageAtTop(
                    requireContext(),
                    "Unable to get device token. Please try again"
                )
                return
            }
            if (CommonMethods.getDeviceName().equals("")) {
                CommonMethods.getDeviceName()
                CommonMethods.showToastMessageAtTop(
                    requireContext(),
                    "Unable to get device name. Please try again"
                )
                return
            }
            @SuppressLint("HardwareIds")
            val deviceId = Settings.Secure.getString(
                requireActivity().contentResolver,
                Settings.Secure.ANDROID_ID
            )

            var check: String = "0"
            if (isRember) {
                check = "1"
            }

            val username =
                if (loginType == "google") {
                    etFirstName.trim().plus("_").plus(etLastName.trim())
                } else {
                    ""
                }

            val request = LoginRequest(
                emailOrPhone = email,
                deviceType = "Android",
                deviceToken = deviceToken.toString(),
                deviceModel = CommonMethods.getDeviceName(),
                password = password,
                remember = check,
                deviceLocation = "",
                deviceId = deviceId,
                googleId = googleId,
                loginType = loginType,
                firstName = etFirstName.trim(),
                lastName = etLastName.trim(),
                userName = username
            )

            loginViewModel.login(request)
        }

        fun rememberMeIsChecked(isChecked: Boolean) {
            val loggedUserUserEmailOrPhone = binding.editEmailPhone.text.toString()
            val loggedInUserPassword = binding.editPassword.text.toString()
            if (isChecked) {
                loggedInUserCache.setLoggedInUserEmailOrPhone(loggedUserUserEmailOrPhone)
                loggedInUserCache.setLoggedInUserPassword(loggedInUserPassword)
                loggedInUserCache.setLoggedInUserRememberMe(true)
            }
        }
    }

    private fun listenToViewModel() {
        loginViewModel.loginState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is LoginViewState.LoadingState -> {
                    if (state.isLoading) {
                        CallProgressWheel.showLoadingDialog(requireContext())
                    } else {
                        CallProgressWheel.dismissLoadingDialog()
                    }
                }
                is LoginViewState.ErrorMessage -> {
                    showLongToast(state.errorMessage)
                    CallProgressWheel.dismissLoadingDialog()
                }
                is LoginViewState.SuccessMessage -> {
                }
                is LoginViewState.LoginData -> {
                    val jsonObject = JSONObject().apply {
                        put("userId", state.loginData.result?.id)
                        put("email", state.loginData.result?.emailOrPhone)
                        put("name", state.loginData.result?.userName)
                    }
                    mp?.identify((state.loginData.result?.id ?: 0).toString())
                    mp?.people?.set(jsonObject)
                    saveUserData(state)
                    startActivity(MainHomeActivity.getIntent(requireContext()))
                    requireActivity().finish()
                    if (state.loginData.profileUpdatedStatus == true) {
                        try {
                            PreferenceHandler.writeString(
                                requireActivity(),
                                PreferenceHandler.SHOW_SUGGESTION,
                                "no"
                            )
                        } catch (e: IOException) {
                            Timber.e(e, "An error occurred $e")
                        }
                    } else {
                        try {
                            PreferenceHandler.writeString(
                                requireActivity(),
                                PreferenceHandler.SHOW_SUGGESTION,
                                "yes"
                            )
                        } catch (e: IOException) {
                            Timber.e(e, "An error occurred $e")
                        }
                    }
                }
            }
        }.autoDispose()
    }

    private fun saveUserData(state: LoginViewState.LoginData) {
        val result = state.loginData.result ?: return

        val editor = FileUtils.loginUserTokenSharedPreference.edit()
        editor.clear()
        editor.putString("loggedInUserToken", state.loginData.accessToken)
        editor.apply()

        loggedInUserCache.getLoggedInUser()?.loggedInUser?.apply {
            firstName = result.firstName.toString()
            id = result.id ?: 0
            lastName = result.lastName.toString()
            userName = result.userName.toString()
            city = result.city.toString()
            work = result.work.toString()
            education = result.education.toString()
            gender = result.gender.toString()
            dob = result.dob.toString()
            relationship = result.relationship.toString()
            hobbies = result.hobbies.toString()
            bio = result.bio.toString()
            emailOrPhone = result.emailOrPhone.toString()
            profilePhoto = result.profilePhoto.toString()
            coverPhoto = result.coverPhoto.toString()
            googleId = result.googleId.toString()
        }?.let { loggedInUserCache.setLoggedInUser(it) }

        CommonMethods.saveUserData(
            Result(
                result.city.toString(),
                result.coverPhoto.toString(),
                result.dob.toString(),
                result.education.toString(),
                result.emailOrPhone.toString(),
                result.firstName.toString(),
                result.gender.toString(),
                result.googleId.toString(),
                result.hobbies.toString(),
                result.id ?: 0,
                result.userCustomerCount ?: 0,
                result.lastName.toString(),
                result.profilePhoto.toString(),
                result.relationship.toString(),
                result.work.toString(),
                result.bio.toString(),
                result.userName.toString()
            ),
            requireActivity(),
            state.loginData.mediaUrl.toString()
        )

        savePreferences(state)
    }

    private fun savePreferences(state: LoginViewState.LoginData) {
        PreferenceHandler.writeString(
            requireActivity(),
            PreferenceHandler.AUTHORIZATION_TOKEN,
            state.loginData.accessToken.toString()
        )
        PreferenceHandler.writeBoolean(requireActivity(), PreferenceHandler.IS_USER_FIRST_TIME_LOGIN, true)
        PreferenceHandler.writeString(
            requireActivity(),
            PreferenceHandler.PROFILE_UPDATED_STATUS,
            state.loginData.profileUpdatedStatus.toString()
        )

        val suggestionValue = if (state.loginData.profileUpdatedStatus == true) "no" else "yes"
        try {
            PreferenceHandler.writeString(requireActivity(), PreferenceHandler.SHOW_SUGGESTION, suggestionValue)
        } catch (e: IOException) {
            Timber.e(e, "An error occurred $e")
        }
    }
    override fun onResume() {
        super.onResume()
        RxBus.listen(RxEvent.GoogleSignInResult::class.java).subscribeAndObserveOnMainThread {
//            handleSignInResult(it.result)
            googleSignInHelper.handleSignInResult(it.result)
        }.autoDispose()
    }
}
