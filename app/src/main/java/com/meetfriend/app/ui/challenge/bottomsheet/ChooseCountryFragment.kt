package com.meetfriend.app.ui.challenge.bottomsheet

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.challenge.model.CountryModel
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentChooseCountryBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.challenge.view.CountryAdapter
import com.meetfriend.app.ui.challenge.viewmodel.AddNewChallengeViewState
import com.meetfriend.app.ui.challenge.viewmodel.CreateChallengeViewModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ChooseCountryFragment : BaseBottomSheetDialogFragment() {

    private var _binding: FragmentChooseCountryBinding? = null
    private val binding get() = _binding!!
    private lateinit var countryAdapter: CountryAdapter

    private val countryItemClickSubject: PublishSubject<List<CountryModel>> =
        PublishSubject.create()
    val countryItemClick: Observable<List<CountryModel>> = countryItemClickSubject.hide()

    companion object {
        const val TIMEOUT = 400L
        fun newInstance(): ChooseCountryFragment {
            return ChooseCountryFragment()
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateChallengeViewModel>
    private lateinit var createChallengeViewModel: CreateChallengeViewModel

    private var listOfCountries: ArrayList<CountryModel> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
        MeetFriendApplication.component.inject(this)
        createChallengeViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChooseCountryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        createChallengeViewModel.getAllCountryList()
        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvents()
        listenToViewModel()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        }

    private fun listenToViewEvents() {
        initAdapter()

        binding.searchAppCompatEditText.textChanges()
            .debounce(TIMEOUT, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isEmpty()) {
                    countryAdapter.listOfDataItems = listOfCountries
                } else {
                    var filterList = listOfCountries.filter {
                        it.name?.lowercase()?.contains(
                            binding.searchAppCompatEditText.text.toString()
                        ) == true
                    }
                    countryAdapter.listOfDataItems = filterList
                }
            }.autoDispose()

        binding.close.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()

        binding.tvDone.throttleClicks().subscribeAndObserveOnMainThread {
            val list = arrayListOf<CountryModel>()
            listOfCountries.forEach {
                if (it.isSelected) {
                    list.add(it)
                }
            }

            countryItemClickSubject.onNext(list)
        }.autoDispose()
    }

    private fun listenToViewModel() {
        createChallengeViewModel.addNewChallengeState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddNewChallengeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is AddNewChallengeViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is AddNewChallengeViewState.AllCountryListResponse -> {
                    listOfCountries = it.countryList
                    countryAdapter.listOfDataItems = it.countryList
                }
                else -> {}
            }
        }
    }

    private fun initAdapter() {
        countryAdapter = CountryAdapter(requireContext()).apply {
            countryItemClick.subscribeAndObserveOnMainThread {
                if (it.sortName.equals("All")) {
                    var selection = !it.isSelected
                    listOfCountries.forEach { country ->
                        country.isSelected = selection
                    }
                    countryAdapter.listOfDataItems = listOfCountries
                } else {
                    var list = countryAdapter.listOfDataItems ?: arrayListOf()
                    var index = list.indexOf(it)
                    list.get(index).isSelected = !list.get(index).isSelected
                    countryAdapter.listOfDataItems = list
                }
            }.autoDispose()
        }
        binding.rlCountry.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = countryAdapter
        }
    }
}
