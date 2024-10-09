package com.meetfriend.app.ui.activities

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
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.messaging.FirebaseMessaging
import com.meetfriend.app.R
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityWelcomeBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.viewmodal.ForgotPasswordViewState
import com.meetfriend.app.viewmodal.RegisterViewModal
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class WelcomeActivity : BaseBottomSheetDialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = WelcomeActivity()
    }

    private var _binding: ActivityWelcomeBinding? = null
    private val binding
        get() = _binding ?: error("LoginBottomSheet should not be accessed when it's null")

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<RegisterViewModal>
    private lateinit var registerViewModal: RegisterViewModal
    private var deviceToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSViewDialogTheme)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityWelcomeBinding.inflate(inflater, container, false)
        return binding.root
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        MeetFriendApplication.component.inject(this)
        registerViewModal = getViewModelFromFactory(viewModelFactory)
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
        registerViewModal = ViewModelProvider(this)[RegisterViewModal::class.java]
        sendDeviceToken()
        listenToViewEvents()
        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                // Get new FCM registration token
                deviceToken = task.result
                try {
                    @SuppressLint("HardwareIds")
                    val deviceId = Settings.Secure.getString(
                        requireContext().contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                    val mHashMap = HashMap<String, Any>()
                    mHashMap["device_id"] = deviceId
                    mHashMap["device_token"] = task.result
                    mHashMap["device_type"] = "Android"
                    registerViewModal.deviceToken(mHashMap)
                    Timber.tag("TAG").e("onViewCreated: $deviceId \n ${task.result}")
                } catch (e: IOException) {
                    Timber.e(e, "An error occurred $e")
                }
            }
        )
    }

    private fun sendDeviceToken() {
        registerViewModal.forgotPasswordState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is ForgotPasswordViewState.DeviceToken -> {
                    if (state.deviceDataData.status) {
                        Timber.tag("TAG").e("sendDeviceToken: Save")
                    } else {
                        Timber.tag("TAG").e("sendDeviceToken: ${state.deviceDataData}")
                    }
                }

                else -> {}
            }
        }.autoDispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            try {
                RxBus.publish(RxEvent.GoogleSignInResult(result))
            } catch (e: IOException) {
                Timber.e(e, "An error occurred $e")
            }
        }
    }

    private fun listenToViewEvents() {
        binding.tvLoginBtn.throttleClicks().subscribeAndObserveOnMainThread {
            openLoginBottomSheet()
            Constant.CLICK_COUNT++
        }.autoDispose()

        binding.tvRegisterBtn.throttleClicks().subscribeAndObserveOnMainThread {
            openRegisterBottomSheet()
            Constant.CLICK_COUNT++
        }.autoDispose()
    }

    override fun onResume() {
        super.onResume()
        RxBus.publish(RxEvent.PauseForYouShorts)
    }

    override fun onPause() {
        super.onPause()
        RxBus.publish(RxEvent.PlayForYouShorts)
    }
}
