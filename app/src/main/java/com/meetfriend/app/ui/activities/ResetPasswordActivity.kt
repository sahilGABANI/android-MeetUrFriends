package com.meetfriend.app.ui.activities

import android.os.Bundle
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityResetPasswordBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.viewmodal.ForgotPasswordViewState
import com.meetfriend.app.viewmodal.RegisterViewModal
import contractorssmart.app.utilsclasses.CommonMethods
import java.util.regex.Pattern
import javax.inject.Inject

class ResetPasswordActivity : BasicActivity() {

    lateinit var binding: ActivityResetPasswordBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<RegisterViewModal>
    private lateinit var registerViewModal: RegisterViewModal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MeetFriendApplication.component.inject(this)
        registerViewModal = getViewModelFromFactory(viewModelFactory)
        initializeObservers()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.apply {
            tvReset.setOnClickListener {
                if (etOldPassword.text.toString().trim().equals("")) {
                    textFieldOldPassword.error = "Old password is empty"
                    return@setOnClickListener
                }
                textFieldOldPassword.error = null
                if (etNewPassword.text.toString().trim().equals("")) {
                    textFieldNewPassword.error = "New password is empty"
                    return@setOnClickListener
                }
                textFieldNewPassword.error = null
                if (!isValidPasswordFormat(etNewPassword.text.toString().trim())) {
                    textFieldNewPassword.error =
                        "Password must be atleast 8 characters long," +
                        " containing lower, upper case, number and special characters"
                    return@setOnClickListener
                }
                textFieldNewPassword.error = null

                if (etConfirmPassword.text.toString().trim().equals("")) {
                    textFieldCPassword.error = "Confirm password is empty"
                    return@setOnClickListener
                }
                textFieldCPassword.error = null
                resetPasswordApi()
                openInterstitialAds()
                Constant.CLICK_COUNT++
            }
            ivBackIcon.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    fun isValidPasswordFormat(password: String): Boolean {
        val passwordREGEX = Pattern.compile(
            "^" +
                "(?=.*[0-9])" + // at least 1 digit
                /*    "(?=.*[a-z])" + */
                // at least 1 lower case letter
                "(?=.*[A-Z])" + // at least 1 upper case letter
                "(?=.*[a-zA-Z])" + // any letter
                "(?=.*[@#$%^&+=?])" + // at least 1 special character
                /*"(?=\\S+$)" + */
                // no white spaces
                ".{8,}" + // at least 8 characters
                "$"
        )
        return passwordREGEX.matcher(password).matches()
    }

    private fun resetPasswordApi() {
        val mHashMap = HashMap<String, Any>()
        mHashMap["old_password"] = binding.etOldPassword.text.toString().trim()
        mHashMap["new_password"] = binding.etNewPassword.text.toString().trim()
        mHashMap["confirm_password"] = binding.etConfirmPassword.text.toString().trim()
        registerViewModal.resetPassword(mHashMap)
    }

    private fun initializeObservers() {
        registerViewModal.forgotPasswordState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is ForgotPasswordViewState.ResetPassword -> {
                    if (state.resetPassword.status) {
                        CommonMethods.showToastMessageAtTop(this, state.resetPassword.message.toString())
                        onBackPressedDispatcher.onBackPressed()
                    } else {
                        CommonMethods.showToastMessageAtTop(this, state.resetPassword.message.toString())
                    }
                }
                is ForgotPasswordViewState.LoadingState -> {
                    showLoading(state.isLoading)
                }
                is ForgotPasswordViewState.ErrorMessage -> {
                    showToast(state.errorMessage)
                }
                else -> {}
            }
        }.autoDispose()
    }
}
