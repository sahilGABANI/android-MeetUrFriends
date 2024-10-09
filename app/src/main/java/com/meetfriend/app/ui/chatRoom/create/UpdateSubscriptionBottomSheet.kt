package com.meetfriend.app.ui.chatRoom.create

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.subscription.model.SubscriptionOption
import com.meetfriend.app.api.subscription.model.TempPlanInfo
import com.meetfriend.app.databinding.BottomSheetUpdateSubscriptionBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.create.view.PlanItemAdapter
import com.meetfriend.app.ui.chatRoom.subscription.SubscriptionActivity

class UpdateSubscriptionBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {

        private const val INTENT_CHAT_ROOM_INFO = "chatRoomInfo"

        fun newInstance(chatRoomInfo: ChatRoomInfo): UpdateSubscriptionBottomSheet {
            val args = Bundle()
            chatRoomInfo.let { args.putParcelable(INTENT_CHAT_ROOM_INFO, it) }
            val fragment = UpdateSubscriptionBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: BottomSheetUpdateSubscriptionBinding? = null
    private val binding get() = _binding!!

    private lateinit var planItemAdapter: PlanItemAdapter
    private var listOfPlans: MutableList<TempPlanInfo> = mutableListOf()
    lateinit var selectedPlanInfo: TempPlanInfo
    lateinit var chatRoomInfo: ChatRoomInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUpdateSubscriptionBinding.inflate(inflater, container, false)
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

        loadPlanData()
        loadDataFromIntent()
        listenToViewEvents()
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
        planItemAdapter = PlanItemAdapter(requireContext()).apply {
            planItemClicks.subscribeAndObserveOnMainThread {
                selectedPlanInfo = it
                planItemAdapter.selectedRoomType = it.roomType
            }.autoDispose()
        }
        binding.rvPlans.adapter = planItemAdapter

        planItemAdapter.listOfDataItems = listOfPlans

        binding.tvPay.throttleClicks().subscribeAndObserveOnMainThread {
            val option = if (chatRoomInfo.roomType == 0) {
                SubscriptionOption.Admin.name
            } else {
                SubscriptionOption.ChatRoom.name
            }
            startActivity(
                SubscriptionActivity.getIntent(
                    requireContext(),
                    selectedPlanInfo,
                    chatRoomInfo.id,
                    option
                )
            )
            dismissBottomSheet()
        }.autoDispose()
    }

    @Throws(IllegalStateException::class)
    private fun loadDataFromIntent() {
        chatRoomInfo = arguments?.getParcelable(INTENT_CHAT_ROOM_INFO)
            ?: throw IllegalStateException("No args provided")

        binding.tvHeaderWithRoomName.text =
            getString(R.string.label_update_subscription, chatRoomInfo.roomName)
    }

    private fun loadPlanData() {
        listOfPlans.add(
            TempPlanInfo(
                coinImage = null,
                roomType = getString(R.string.label_primary_room),
                amount = getString(R.string.price_7_99),
                duration = getString(R.string.label_1_month),
                isSelected = false,
                month = 1
            )
        )

        listOfPlans.add(
            TempPlanInfo(
                coinImage = null,
                roomType = getString(R.string.label_basic_room),
                amount = getString(R.string.price_23_99),
                duration = getString(R.string.label_3_month),
                isSelected = false,
                month = 3

            )
        )

        listOfPlans.add(
            TempPlanInfo(
                coinImage = null,
                roomType = getString(R.string.label_standard_room),
                amount = getString(R.string.price_47_99),
                duration = getString(R.string.label_6_month),
                isSelected = false,
                month = 6
            )
        )

        listOfPlans.add(
            TempPlanInfo(
                coinImage = null,
                roomType = getString(R.string.label_premium_room),
                amount = getString(R.string.price_95_99),
                duration = getString(R.string.label_12_month),
                isSelected = false,
                month = 12
            )
        )
        selectedPlanInfo = listOfPlans.first()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }
}
