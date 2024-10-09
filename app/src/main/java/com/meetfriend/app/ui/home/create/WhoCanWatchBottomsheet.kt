package com.meetfriend.app.ui.home.create

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatRadioButton
import com.meetfriend.app.R
import com.meetfriend.app.databinding.BottomsheetWhoCanWatchBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.messages.create.CreateMessageActivity
import com.meetfriend.app.ui.mygifts.viewmodel.GiftsTransactionViewModel
import com.meetfriend.app.utilclasses.CallProgressWheel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class WhoCanWatchBottomsheet(
    var isPrivate: Int?,
    var listData: HashMap<Int, Pair<String?, String?>>? = null,
    var optionIsSelect: String?,
) : BaseBottomSheetDialogFragment() {

    companion object {
        private const val REQUEST_CODE_WHO_CAN_WATCH = 10004
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GiftsTransactionViewModel>

    private var _binding: BottomsheetWhoCanWatchBinding? = null
    private val binding get() = _binding!!
    private var whoCanWatchPeopleHashMap = HashMap<Int, Pair<String?, String?>>()
    private var selectedOption: String = ""

    private val selectStateSubject: PublishSubject<DatesFilter> = PublishSubject.create()
    val selectState: Observable<DatesFilter> = selectStateSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomsheetWhoCanWatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setOnKeyListener { _, p1, _ -> p1 == KeyEvent.KEYCODE_BACK }
        }

        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        setVizibility()
        when (optionIsSelect) {
            "Followers" -> {
                selectedOption = "Followers"
                isRadioButtonClicked(binding.rbClickCloseFriends, binding.rbClickEveryone)
            }
            "List" -> {
                selectedOption = "List"
                isRadioButtonClicked(binding.rbClickEveryone, binding.rbClickCloseFriends)
            }
            else -> {
                isRadioButtonClicked(binding.rbClickCloseFriends, binding.rbClickEveryone)
            }
        }
        if (listData?.size == null) {
            binding.tvTotalSelectedFollower.text = "0 Followers"
        } else {
            binding.tvTotalSelectedFollower.text = "${listData?.size} Followers"
        }
        binding.rlEveryOne.throttleClicks().subscribeAndObserveOnMainThread {
            isRadioButtonClicked(binding.rbClickCloseFriends, binding.rbClickEveryone)
            selectedOption = "Followers"
        }.autoDispose()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()
        binding.rlSelectedUSer.throttleClicks().subscribeAndObserveOnMainThread {
            isRadioButtonClicked(binding.rbClickEveryone, binding.rbClickCloseFriends)
            selectedOption = "List"
        }.autoDispose()
        binding.llSelectFollower.throttleClicks().subscribeAndObserveOnMainThread {
            isRadioButtonClicked(binding.rbClickEveryone, binding.rbClickCloseFriends)
            selectedOption = "List"
            startActivityForResultWithDefaultAnimation(
                CreateMessageActivity.getIntentWithData(
                    requireContext(),
                    "WhoCanWatch",
                    "",
                    listData ?: whoCanWatchPeopleHashMap
                ),
                REQUEST_CODE_WHO_CAN_WATCH
            )
        }.autoDispose()
        binding.tvDone.throttleClicks().subscribeAndObserveOnMainThread {
            if (whoCanWatchPeopleHashMap.isNotEmpty()) {
                selectStateSubject.onNext(DatesFilter(selectedOption, whoCanWatchPeopleHashMap))
            } else {
                selectStateSubject.onNext(DatesFilter(selectedOption, listData))
            }
            dismissBottomSheet()
        }.autoDispose()
    }

    private fun setVizibility() {
        if (isPrivate == 1) {
            binding.tvEveryOne.text = resources.getText(R.string.everyone)
            binding.tvTotalFollower.visibility = View.VISIBLE
            binding.tvTotalFollower.text =
                loggedInUserCacheBase.getLoggedInUser()?.loggedInUser?.noOfFollowers.toString() + " Followers"
        } else {
            binding.tvTotalFollower.visibility = View.GONE
            binding.tvEveryOne.text = resources.getText(R.string.everyone)
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    fun showLoading(show: Boolean?) {
        if (show!!) showLoading() else hideLoading()
    }

    fun showLoading() {
        CallProgressWheel.showLoadingDialog(requireActivity())
        // spotsDialog!!.show()
    }

    fun hideLoading() {
        CallProgressWheel.dismissLoadingDialog()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_WHO_CAN_WATCH) {
                data?.let { intentData ->
                    handleWhoCanWatchData(intentData)
                }
            }
        }
    }
    private fun handleWhoCanWatchData(data: Intent) {
        if (data.hasExtra(CreateMessageActivity.INTENT_EXTRA_WHO_CAN_WATCH_HASHMAP)) {
            val taggedPeopleHashMap =
                data.getSerializableExtra(CreateMessageActivity.INTENT_EXTRA_WHO_CAN_WATCH_HASHMAP)
            if (taggedPeopleHashMap != null) {
                this.whoCanWatchPeopleHashMap =
                    taggedPeopleHashMap as java.util.HashMap<Int, Pair<String?, String?>>
                val taggedPeopleList =
                    taggedPeopleHashMap.values.map { it.first?.length }
                binding.tvTotalSelectedFollower.text =
                    "${taggedPeopleList.size} Followers"
            }
        }
    }
    private fun isRadioButtonClicked(
        radioBtnOne: AppCompatRadioButton,
        mainRadioBtn: AppCompatRadioButton,
    ) {
        radioBtnOne.isChecked = false
        mainRadioBtn.isChecked = true
    }

    data class DatesFilter(
        val selectedOption: String? = null,
        val list: HashMap<Int, Pair<String?, String?>>? = null,
    )
}
