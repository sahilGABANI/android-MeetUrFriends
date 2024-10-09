package com.meetfriend.app.ui.bottomsheet.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
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
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.application.MeetFriend
import com.meetfriend.app.databinding.FragmentRegisterBottomSheetBinding
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
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.register.viewmodel.NewRegisterViewModel
import com.meetfriend.app.ui.register.viewmodel.RegisterViewState
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.FileUtils
import com.meetfriend.app.viewmodal.ForgotPasswordViewState
import com.meetfriend.app.viewmodal.RegisterViewModal
import com.mixpanel.android.mpmetrics.MixpanelAPI
import contractorssmart.app.utilsclasses.CommonMethods
import contractorssmart.app.utilsclasses.PreferenceHandler
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject

class RegisterBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = RegisterBottomSheet()
        const val START_SPAN1 = 15
        const val START_SPAN2 = 37
        const val END = 32
    }

    private var _binding: FragmentRegisterBottomSheetBinding? = null
    private val binding
        get() = _binding ?: error("RegisterBottomSheet should not be accessed when it's null")

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<NewRegisterViewModel>
    private lateinit var newRegisterViewModal: NewRegisterViewModel
    @Inject
    internal lateinit var registerViewModelFactory: ViewModelFactory<RegisterViewModal>
    private lateinit var registerViewModal: RegisterViewModal

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var deviceToken: String = ""
    var locationTXT: String = ""
    private var country = ""
    var googleSignInClient: GoogleSignInClient? = null
    val rcSignIn: Int = 1
    private lateinit var registerHelper: RegisterHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSViewDialogTheme)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register_bottom_sheet, container, false)
        _binding = FragmentRegisterBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        registerHelper = RegisterHelper()

        MeetFriend.component.inject(this)
        newRegisterViewModal = getViewModelFromFactory(viewModelFactory)
        registerViewModal = getViewModelFromFactory(registerViewModelFactory)

//        registerViewModal = ViewModelProvider(this)[RegisterViewModal::class.java]

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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        listenToViewModel()
        registerHelper.listenToViewEvents()

        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                deviceToken = task.result
            }
        )

        iniData()
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
    private fun iniData() {
        binding.checkBox.isVisible = true
        registerHelper.initializeObservers()

        binding.tvRegisterBtn.throttleClicks().subscribeAndObserveOnMainThread {
            registerHelper.validations()
        }.autoDispose()

        binding.googleView.throttleClicks().subscribeAndObserveOnMainThread {
            registerHelper.sendToMixpanel("google")
            registerHelper.signIn()
        }.autoDispose()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        RxBus.listen(RxEvent.GoogleSignInResult::class.java).subscribeAndObserveOnMainThread {
            registerHelper.handleSignInResult(it.result)
        }.autoDispose()
    }
    private fun listenToViewModel() {
        newRegisterViewModal.registerState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is RegisterViewState.LoadingState -> {
                    if (state.isLoading) {
                        CallProgressWheel.showLoadingDialog(requireContext())
                    } else {
                        CallProgressWheel.dismissLoadingDialog()
                    }
                }

                is RegisterViewState.ErrorMessage -> {
                    showLongToast(state.errorMessage)
                    CallProgressWheel.dismissLoadingDialog()
                }

                is RegisterViewState.SuccessMessage -> {
                }

                is RegisterViewState.LoginData -> {
                    var jsonObject = JSONObject()
                    jsonObject.put("userId", state.loginData.result?.id)
                    jsonObject.put("email", state.loginData.result?.emailOrPhone)
                    jsonObject.put("name", state.loginData.result?.userName)

                    mp?.identify((state.loginData.result?.id ?: 0).toString())
                    mp?.people?.set(jsonObject)
                    var editor = FileUtils.loginUserTokenSharedPreference.edit()
                    editor.clear()
                    editor.putString("loggedInUserToken", state.loginData.accessToken)
                    editor.apply()

                    saveUserData(state)
                    savePreferences(state)

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
                else -> {}
            }
        }.autoDispose()
    }
    private fun updateLogInUesrData(data: MeetFriendUser) {
        loggedInUserCache.getLoggedInUser()?.loggedInUser?.apply {
            firstName = data.firstName.toString()
            id = data.id ?: 0
            lastName = data.lastName.orEmpty()
            userName = data.userName.orEmpty()
            city = data.city.orEmpty()
            work = data.work.orEmpty()
            education = data.education.orEmpty()
            gender = data.gender.orEmpty()
            dob = data.dob.orEmpty()
            relationship = data.relationship.orEmpty()
            hobbies = data.hobbies.orEmpty()
            bio = data.bio.orEmpty()
            emailOrPhone = data.emailOrPhone.orEmpty()
            profilePhoto = data.profilePhoto.orEmpty()
            coverPhoto = data.coverPhoto.orEmpty()
            googleId = data.googleId.orEmpty()
        }?.let { loggedInUserCache.setLoggedInUser(it) }
    }
    private fun saveUserData(it: RegisterViewState.LoginData) {
        var data = it.loginData.result
        data?.let { result ->
            updateLogInUesrData(result)
        }
        CommonMethods.saveUserData(
            Result(
                it.loginData.result?.city.toString(),
                it.loginData.result?.coverPhoto.toString(),
                it.loginData.result?.dob.toString(),
                it.loginData.result?.education.toString(),
                it.loginData.result?.emailOrPhone.toString(),
                it.loginData.result?.firstName.toString(),
                it.loginData.result?.gender.toString(),
                it.loginData.result?.googleId.toString(),
                it.loginData.result?.hobbies.toString(),
                it.loginData.result?.id ?: 0,
                it.loginData.result?.userCustomerCount ?: 0,
                it.loginData.result?.lastName.toString(),
                it.loginData.result?.profilePhoto.toString(),
                it.loginData.result?.relationship.toString(),
                it.loginData.result?.work.toString(),
                it.loginData.result?.bio.toString(),
                it.loginData.result?.userName.toString()
            ),
            requireActivity(),
            it.loginData.mediaUrl.toString()
        )
    }

    private fun savePreferences(state: RegisterViewState.LoginData) {
        PreferenceHandler.writeString(
            requireActivity(),
            PreferenceHandler.AUTHORIZATION_TOKEN,
            state.loginData.accessToken.toString()
        )

        PreferenceHandler.writeBoolean(
            requireActivity(),
            PreferenceHandler.IS_USER_FIRST_TIME_LOGIN,
            true
        )
        PreferenceHandler.writeString(
            requireActivity(),
            PreferenceHandler.PROFILE_UPDATED_STATUS,
            state.loginData.profileUpdatedStatus.toString()
        )
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == rcSignIn) {
            data?.let {
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                try {
                    registerHelper.handleSignInResult(result)
                } catch (e: IOException) {
                    Timber.e(e, "An error occurred $e")
                }
            }
        }
    }
    private inner class RegisterHelper {
        fun listenToViewEvents() {
            with(binding) {
                dismissBtn.throttleClicks().subscribeAndObserveOnMainThread {
                    dismiss()
                }.autoDispose()

                val spannableString = SpannableString("I agree to the Terms & Conditions and Policies")
                val clickableSpan1 = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constant.PRIVACY_POLICY))
                        startActivity(intent)
                    }
                }
                spannableString.setSpan(clickableSpan1, START_SPAN1, END, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

                val clickableSpan2 = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constant.TERMS_N_CONDITIONS))
                        startActivity(intent)
                    }
                }
                spannableString.setSpan(
                    clickableSpan2,
                    START_SPAN2,
                    spannableString.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.tvTerms.text = spannableString
                binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
            }
        }
        fun handleSignInResult(result: GoogleSignInResult?) {
            if (result != null && result.isSuccess) {
                // Signed in successfully, show authenticated UI.
                val acct: GoogleSignInAccount? = result.signInAccount
                if (acct != null) {
                    val personName = acct.displayName ?: ""
                    if (personName.contains(" ")) {
                        val fname = personName.split(" ")[0]
                        val lname = personName.split(" ")[1]
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

            var check = "0"

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
                deviceLocation = locationTXT,
                deviceId = deviceId,
                googleId = googleId,
                loginType = loginType,
                firstName = etFirstName.trim(),
                lastName = etLastName.trim(),
                userName = username
            )

            newRegisterViewModal.login(request)
        }
        fun validations() {
            val expression = "[0-9a-zA-Z._]*"
            val pattern: Pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)

            if (binding.editUserName.text.toString().trim().equals("")) {
                showToast("User name is empty")
            } else if (!pattern.matcher(binding.editUserName.text.toString()).matches()) {
                showToast("special character or space is not allowed in username")
            } else if (binding.editEmail.text.toString().trim().equals("")) {
                showToast("Email is empty")
            } else if (!CommonMethods.isValidEmail(binding.editEmail.text.toString().trim())) {
                showToast("Invalid email")
            } else if (binding.editPassword.text.toString().trim().equals("")) {
                showToast("Password is empty")
            } else if (!isValidPasswordFormat(binding.editPassword.text.toString().trim())) {
                showToast("Password must be atleast 8 characters long")
            } else if (binding.editConfirmPass.text.toString().trim().equals("")) {
                showToast("Confirm Password is empty")
            } else if (binding.editPassword.text.toString().trim() != binding.editConfirmPass.text.toString()
                    .trim()
            ) {
                showToast("Password and Confirm Password should be same")
            } else if (!binding.checkBox.isChecked) {
                showToast("Accept terms and conditions to proceed")
                return
            } else {
                hideKeyboard()
                registerAPI()
                if (deviceToken.isEmpty() || CommonMethods.getDeviceName().isEmpty()) {
                    if (CommonMethods.getDeviceName().isEmpty())CommonMethods.getDeviceName()
                    val errorMessage = if (deviceToken.isEmpty()) {
                        "Unable to get device token. Please try again"
                    } else {
                        "Unable to get device name. Please try again"
                    }

                    CommonMethods.showToastMessageAtTop(requireContext(), errorMessage)
                    return
                }
                @SuppressLint("HardwareIds")
                val deviceId = Settings.Secure.getString(
                    requireActivity().contentResolver,
                    Settings.Secure.ANDROID_ID
                )
            }
        }
        fun registerAPI() {
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
            val mHashMap = HashMap<String, Any>()
            mHashMap["userName"] = binding.editUserName.text.toString()
            mHashMap["email_or_phone"] = binding.editEmail.text.toString().trim()
            mHashMap["country_code"] = country
            mHashMap["password"] = binding.editPassword.text.toString().trim()
            mHashMap["confirmPassword"] = binding.editConfirmPass.text.toString().trim()
            mHashMap["device_type"] = "Android"
            mHashMap["device_id"] = deviceId
            mHashMap["device_token"] = deviceToken
            mHashMap["device_model"] = CommonMethods.getDeviceName()
            mHashMap["device_location"] = locationTXT
            registerViewModal.register(mHashMap)
        }
        fun signIn() {
            val signInIntent: Intent? = googleSignInClient?.signInIntent
            if (signInIntent != null) {
                startActivityForResult(signInIntent, rcSignIn)
            }
        }
        fun verificationPopUp() {
            val factory = LayoutInflater.from(requireContext())
            val dialogView: View = factory.inflate(R.layout.layout_regsiter_info, null)
            val dialog: AlertDialog = AlertDialog.Builder(requireContext()).create()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setView(dialogView)
            dialog.setCancelable(false)
            val login: TextView? = dialogView.findViewById(R.id.tvLogin)
            login?.setOnClickListener {
                dismiss()
                openLoginBottomSheet()
                dialog.dismiss()
            }
            dialog.show()
        }

        @Throws(JSONException::class)
        fun sendToMixpanel(type: String) {
            val props = JSONObject()
            props.put("login_type", type)
            mp?.track("Login", props)
        }
        fun initializeObservers() {
            registerViewModal.forgotPasswordState.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is ForgotPasswordViewState.Register -> {
                        state.registerResponse.let {
                            if (it.status) {
                                try {
                                    it.message?.let { it1 -> showToast(it1) }
                                } catch (e: IOException) {
                                    Timber.e(e, "An error occurred $e")
                                }
                                val mp = MixpanelAPI.getInstance(
                                    requireContext(),
                                    "9baf1cfb430c0de219529759f0b22395",
                                    true
                                )

                                var jsonObject = JSONObject()
                                jsonObject.put("userId", state.registerResponse.result?.result?.id)
                                jsonObject.put("email", it.result?.result?.emailOrPhone)
                                jsonObject.put("name", it.result?.result?.userName)

                                mp?.identify(it.result?.result?.id.toString())
                                mp?.people?.set(jsonObject)
                                verificationPopUp()
                            } else {
                                try {
                                    it.message?.let { it1 -> showToast(it1) }
                                } catch (e: IOException) {
                                    Timber.e(e, "An error occurred $e")
                                }
                            }
                        }
                    }
                    is ForgotPasswordViewState.LoadingState -> {
                        state.isLoading.let {
                            if (it) {
                                CallProgressWheel.showLoadingDialog(requireContext())
                            } else {
                                CallProgressWheel.dismissLoadingDialog()
                            }
                        }
                    }
                    is ForgotPasswordViewState.ErrorMessage -> {
                        state.errorMessage.let { showToast(it) }
                    }
                    else -> {}
                }
            }.autoDispose()
        }
        fun isValidPasswordFormat(password: String): Boolean {
            val passwordREGEX = Pattern.compile(
                "^" + "(?=.*[0-9a-zA-Z])" + // any letter
                    /*"(?=\\S+$)" + */
                    // no white spaces
                    ".{8,}" + // at least 8 characters
                    "$"
            )
            return passwordREGEX.matcher(password).matches()
        }
    }
}
