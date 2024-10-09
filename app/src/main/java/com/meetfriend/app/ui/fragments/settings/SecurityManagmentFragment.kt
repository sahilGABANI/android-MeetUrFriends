package com.meetfriend.app.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.BaseFragment
import com.meetfriend.app.databinding.FragmentSecurityMangmentBinding
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.responseclasses.settings.SecurityManagementResult
import com.meetfriend.app.responseclasses.settings.data
import com.meetfriend.app.ui.adapters.SecurityManagmentAdapter
import com.meetfriend.app.ui.bottomsheet.SecurityManagmentBottomSheet
import com.meetfriend.app.viewmodal.SettingViewModal
import com.meetfriend.app.viewmodal.SettingViewState
import contractorssmart.app.utilsclasses.CommonMethods
import javax.inject.Inject

class SecurityManagmentFragment :
    BaseFragment(),
    SecurityManagmentAdapter.AdapterCallback,
    SecurityManagmentBottomSheet.AdapterCallback {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SettingViewModal>
    private lateinit var settingViewModal: SettingViewModal

    private var _binding: FragmentSecurityMangmentBinding? = null
    private val binding get() = _binding!!
    companion object {
        const val PER_PAGE = 1000
    }

    override fun provideYourFragmentView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityMangmentBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MeetFriendApplication.component.inject(this)
        settingViewModal = getViewModelFromFactory(viewModelFactory)
        binding.headerLoginBackButton.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        fetchSecurityManagment()
        initializeObservers()
    }

    private fun fetchSecurityManagment() {
        val mHashMap = HashMap<String, Any>()
        mHashMap["page"] = 1
        mHashMap["per_page"] = PER_PAGE
        settingViewModal.fetchSecurity(mHashMap)
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
                is SettingViewState.SecurityData -> {
                    if (viewState.securityData.status) {
                        viewState.securityData.result?.let {
                            setAdapter(it)
                        }
                    } else {
                        CommonMethods.showToastMessageAtTop(
                            requireActivity(),
                            viewState.securityData.result.toString()
                        )
                    }
                }
                is SettingViewState.DeleteSecurityData -> {
                    viewState.deleteResponse.let {
                        if (it.status) {
                            CommonMethods.showToastMessageAtTop(requireActivity(), it.message.toString())
                        } else {
                            CommonMethods.showToastMessageAtTop(requireActivity(), it.message.toString())
                        }
                    }
                }
                is SettingViewState.ErrorMessage -> {
                    CommonMethods.showToastMessageAtTop(
                        requireActivity(),
                        viewState.errorMessage
                    )
                }
                else -> {}
            }
        }
    }

    private fun setAdapter(securityManagementResult: SecurityManagementResult) {
        if (securityManagementResult.data.isNullOrEmpty()) {
            binding.securityRV.visibility = View.GONE
            binding.tvNoSecurity.visibility = View.VISIBLE
        } else {
            binding.tvNoSecurity.visibility = View.GONE
            binding.securityRV.visibility = View.VISIBLE
            binding.securityRV.layoutManager = LinearLayoutManager(requireContext())
            val adapter = SecurityManagmentAdapter(this, requireContext())
            binding.securityRV.adapter = adapter
            adapter.submitList(securityManagementResult.data)
        }
    }

    override fun itemClick(result: data) {
        val bottomSheet = result.let { SecurityManagmentBottomSheet(it, this) }
        activity?.supportFragmentManager?.let { bottomSheet!!.show(it, "Tag") }
    }

    override fun refreshSecurity() {
        fetchSecurityManagment()
    }
}
