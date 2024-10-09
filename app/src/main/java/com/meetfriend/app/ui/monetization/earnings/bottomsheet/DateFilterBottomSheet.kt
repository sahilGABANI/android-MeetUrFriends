package com.meetfriend.app.ui.monetization.earnings.bottomsheet

import android.graphics.Color
import android.graphics.PorterDuff
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.util.Pair
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.meetfriend.app.R
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.DateFilterBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.monetization.earnings.DateValidatorFiftyYearRange
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.*


class DateFilterBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        const val TAG = "DateFilterBottomSheet"
        const val SELECTED_RADIO_BTN = "selectedRadioBtn"
        const val ACCEPTED_DATE = "acceptedDate"
        fun newInstance(selectedRadioBtn: String, acceptDate: String ? = null): DateFilterBottomSheet {
            val bundle = Bundle()
            bundle.putString(SELECTED_RADIO_BTN, selectedRadioBtn)
            bundle.putString(ACCEPTED_DATE, acceptDate)
            val dateFilterBottomSheet = DateFilterBottomSheet()
            dateFilterBottomSheet.arguments = bundle
            return dateFilterBottomSheet
        }
    }

    private val selectStateSubject: PublishSubject<DatesFilter> = PublishSubject.create()
    val selectState: Observable<DatesFilter> = selectStateSubject.hide()

    private var _binding: DateFilterBottomSheetBinding? = null
    private val binding get() = _binding!!
    private var selectedOption: String = ""
    private var acceptedDate: String = ""
    private var startDate: String = ""
    private var endDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
        MeetFriendApplication.component.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DateFilterBottomSheetBinding.inflate(inflater, container, false)
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

        arguments?.let {
            selectedOption = it.getString(SELECTED_RADIO_BTN) ?: ""
            acceptedDate = it.getString(ACCEPTED_DATE) ?: ""
        }

        listenToViewEvents()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private fun listenToViewEvents() {
        binding.apply {

            when (selectedOption) {
                resources.getString(R.string.bsv_this_week) -> {
                    isRadioButtonClicked(lastWeekRadioBtn, customRadioBtn, thisWeekRadioBtn)
                }

                resources.getString(R.string.bsv_last_week) -> {
                    isRadioButtonClicked(thisWeekRadioBtn, customRadioBtn, lastWeekRadioBtn)
                }

                else -> {
                    isRadioButtonClicked(thisWeekRadioBtn, lastWeekRadioBtn, customRadioBtn)
                }
            }

            ivClose.throttleClicks().subscribeAndObserveOnMainThread {
                dismiss()
            }.autoDispose()

            tvDone.throttleClicks().subscribeAndObserveOnMainThread {
                dismiss()
                selectedOption =
                    selectedOption.ifEmpty { resources.getString(R.string.bsv_this_week) }
                selectStateSubject.onNext(
                    DatesFilter(
                        selectedOption = selectedOption,
                        sunday = startDate,
                        saturday = endDate
                    )
                )
            }.autoDispose()

            rlFirstWeekContainer.throttleClicks().subscribeAndObserveOnMainThread {
                selectedOption = resources.getString(R.string.bsv_this_week)
                isRadioButtonClicked(lastWeekRadioBtn, customRadioBtn, thisWeekRadioBtn)
            }.autoDispose()

            rlLastWeekContainer.throttleClicks().subscribeAndObserveOnMainThread {
                selectedOption = resources.getString(R.string.bsv_last_week)
                isRadioButtonClicked(thisWeekRadioBtn, customRadioBtn, lastWeekRadioBtn)
            }.autoDispose()

            rlCustomContainer.throttleClicks().subscribeAndObserveOnMainThread {
                manageCustomClick()
            }.autoDispose()

            thisWeekRadioBtn.throttleClicks().subscribeAndObserveOnMainThread {
                selectedOption = resources.getString(R.string.bsv_this_week)
                isRadioButtonClicked(lastWeekRadioBtn, customRadioBtn, thisWeekRadioBtn)
            }.autoDispose()

            lastWeekRadioBtn.throttleClicks().subscribeAndObserveOnMainThread {
                selectedOption = resources.getString(R.string.bsv_last_week)
                isRadioButtonClicked(thisWeekRadioBtn, customRadioBtn, lastWeekRadioBtn)
            }.autoDispose()

            customRadioBtn.throttleClicks().subscribeAndObserveOnMainThread {
                manageCustomClick()
            }.autoDispose()
        }
    }

    private fun manageCustomClick() {
        selectedOption = resources.getString(R.string.bsv_custom)
        isRadioButtonClicked(binding.thisWeekRadioBtn, binding.lastWeekRadioBtn, binding.customRadioBtn)
        dismiss()
        showDateRangePicker()
    }

    private fun showDateRangePicker() {
        val currentDate = MaterialDatePicker.todayInUtcMilliseconds()
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        var milliseconds:Long? = null
        if(acceptedDate != "") {
            val date = sdf.parse(acceptedDate)
            milliseconds = date?.time
        }

        val validator : CalendarConstraints.DateValidator = if(acceptedDate == "") {
            DateValidatorPointBackward.now()} else {DateValidatorFiftyYearRange(
            milliseconds ?: 0L,
            currentDate
        )}
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTheme(R.style.ThemeMaterialCalender)
            .setTitleText("Select Dates")
            .setSelection(
                Pair(currentDate, currentDate),
            )
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(0L)
                    .setEnd(currentDate)
                    .setValidator(validator)
                    .build()
            )
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            startDate = convertTimestampToDate(selection.first ?: 0L)
            endDate = convertTimestampToDate(selection.second ?: 0L)
            selectedOption =
                selectedOption.ifEmpty { resources.getString(R.string.bsv_this_week) }
            selectStateSubject.onNext(
                DatesFilter(
                    selectedOption = selectedOption,
                    sunday = startDate,
                    saturday = endDate
                )
            )
        }

        picker.show(requireActivity().supportFragmentManager, picker.toString())
    }

    private fun convertTimestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = Date(timestamp)
        return sdf.format(date)
    }

    private fun isRadioButtonClicked(
        radioBtnOne: AppCompatRadioButton,
        radioBtnTwo: AppCompatRadioButton,
        mainRadioBtn: AppCompatRadioButton
    ) {
        radioBtnOne.isChecked = false
        radioBtnTwo.isChecked = false
        mainRadioBtn.isChecked = true
    }
}

data class DatesFilter(
    val selectedOption: String? = null,
    val sunday: String? = null,
    val saturday: String? = null
)