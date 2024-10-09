package com.meetfriend.app.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.BottomsheetSecurityManagmentBinding
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.responseclasses.settings.data
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.meetfriend.app.viewmodal.SettingViewModal
import com.meetfriend.app.viewmodal.SettingViewState
import contractorssmart.app.utilsclasses.CommonMethods
import javax.inject.Inject

class SecurityManagmentBottomSheet(var result: data, var callBack: AdapterCallback) :
    BottomSheetDialogFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SettingViewModal>
    private lateinit var settingViewModal: SettingViewModal

    private var _binding: BottomsheetSecurityManagmentBinding? = null
    private val binding
        get() = _binding ?: error("LoginBottomSheet should not be accessed when it's null")

    interface AdapterCallback {
        fun refreshSecurity()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomsheetSecurityManagmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MeetFriendApplication.component.inject(this)
        settingViewModal = getViewModelFromFactory(viewModelFactory)
        initializeObservers()
        binding.tvName.text = result.device_model + " - " + result.device_location
        binding.tvDeviceLastTime.text = result.created_at
        binding.tvDeleteDevice.setOnClickListener {
            deleteDevice(result.id)
        }
    }

    fun deleteDevice(id: String) {
        val mHashMap = HashMap<String, Any>()
        mHashMap["sm_id"] = id
        settingViewModal.deleteSecurity(mHashMap)
    }

    private fun initializeObservers() {
        settingViewModal.settingState.subscribeAndObserveOnMainThread { viewState ->
            when (viewState) {
                is SettingViewState.LoadingState -> {
                    if (viewState.isLoading) {
                        showLoading(true)
                    } else {
                        hideLoading()
                    }
                }
                is SettingViewState.DeleteSecurityData -> {
                    CommonMethods.showToastMessageAtTop(requireActivity(), viewState.deleteResponse.message.toString())
                    if (viewState.deleteResponse.status) {
                        dismiss()
                        callBack.refreshSecurity()
                    }
                }
                is SettingViewState.ErrorMessage -> {
                    // Show error message
                    CommonMethods.showToastMessageAtTop(
                        requireActivity(),
                        viewState.errorMessage
                    )
                }
                else -> {}
            }
        }
    }

    fun showLoading(show: Boolean?) {
        if (show!!) showLoading() else hideLoading()
    }

    fun showLoading() {
        CallProgressWheel.showLoadingDialog(requireActivity())
    }

    fun hideLoading() {
        CallProgressWheel.dismissLoadingDialog()
    }
}
