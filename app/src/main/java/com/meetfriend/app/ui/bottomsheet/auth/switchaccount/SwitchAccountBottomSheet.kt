package com.meetfriend.app.ui.bottomsheet.auth.switchaccount

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.authentication.model.SwitchDeviceAccountRequest
import com.meetfriend.app.application.MeetFriend
import com.meetfriend.app.databinding.FragmentSwitchAccountBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.activities.LoginActivity
import com.meetfriend.app.ui.activities.WelcomeActivity
import com.meetfriend.app.ui.bottomsheet.auth.AddAccountBottomSheet
import com.meetfriend.app.ui.bottomsheet.auth.switchaccount.view.SwitchAccountUserAdapter
import com.meetfriend.app.ui.register.viewmodel.NewRegisterViewModel
import com.meetfriend.app.ui.register.viewmodel.RegisterViewState
import com.meetfriend.app.utilclasses.CallProgressWheel
import javax.inject.Inject

class SwitchAccountBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = SwitchAccountBottomSheet()
    }

    private var deviceId: String = ""
    private var _binding: FragmentSwitchAccountBottomSheetBinding? = null
    private val binding
        get() = _binding ?: error("LoginBottomSheet should not be accessed when it's null")

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<NewRegisterViewModel>
    private lateinit var newRegisterViewModal: NewRegisterViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var switchAccountUserAdapter: SwitchAccountUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MeetFriend.component.inject(this)
        newRegisterViewModal = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeR)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwitchAccountBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }

    @SuppressLint("HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheetView = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            if (bottomSheetView != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.skipCollapsed = true
                behavior.isDraggable = false
                behavior.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }
        }
        deviceId = Settings.Secure.getString(
            requireActivity().contentResolver, Settings.Secure.ANDROID_ID
        )
        listenToViewModel()
        listenToViewEvents()
        newRegisterViewModal.getListOfAccount(deviceId)
        initAdapter()
    }

    private fun initAdapter() {
        switchAccountUserAdapter = SwitchAccountUserAdapter(requireContext()).apply {
            switchUserItemClicks.subscribeAndObserveOnMainThread {
                newRegisterViewModal.switchAccount(SwitchDeviceAccountRequest(deviceId, it.id))
            }.autoDispose()
        }
        binding.rvSwitchUser.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = switchAccountUserAdapter
        }
    }

    private fun listenToViewEvents() {
        binding.llAddAccount.throttleClicks().subscribeAndObserveOnMainThread {
            openAddAccountPopUp()
        }.autoDispose()

        binding.closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        newRegisterViewModal.registerState.subscribeAndObserveOnMainThread {
            when (it) {
                is RegisterViewState.LoadingState -> {
                    if (it.isLoading) {
                        CallProgressWheel.showLoadingDialog(requireContext())
                    } else {
                        CallProgressWheel.dismissLoadingDialog()
                    }
                }

                is RegisterViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                    CallProgressWheel.dismissLoadingDialog()
                }

                is RegisterViewState.SuccessMessage -> {
                }
                is RegisterViewState.SwitchAccount -> {
                    CallProgressWheel.dismissLoadingDialog()
                    startActivity(Intent(context, LoginActivity::class.java))
                    requireActivity().finish()
                }
                is RegisterViewState.GetListOfUser -> {
                    CallProgressWheel.dismissLoadingDialog()
                    val listOfUser = arrayListOf<MeetFriendUser>()
                    it.loginData.result?.let { it1 -> listOfUser.addAll(it1) }
                    listOfUser.firstOrNull { it.id == loggedInUserCache.getLoggedInUserId() }?.apply {
                        listOfUser.remove(this)
                        listOfUser.add(0, this)
                    }
                    switchAccountUserAdapter.listOfDataItems = listOfUser
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun openAddAccountPopUp() {
        val addAccountBottomSheet = AddAccountBottomSheet.newInstance()
        addAccountBottomSheet.show(childFragmentManager, WelcomeActivity::class.java.name)
    }
}
