package com.meetfriend.app.ui.camerakit.termsofservice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.meetfriend.app.R
import com.meetfriend.app.databinding.FragmentAgeRestrictionBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.camerakit.model.AgeRestrictionDialogState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AgeRestrictionFragment : BaseDialogFragment() {

    private val ageRestrictionDialogSubject: PublishSubject<AgeRestrictionDialogState> =
        PublishSubject.create()
    val ageRestrictionDialogState: Observable<AgeRestrictionDialogState> =
        ageRestrictionDialogSubject.hide()

    companion object {
        fun newInstance(): AgeRestrictionFragment {
            return AgeRestrictionFragment()
        }
    }
    private var _binding: FragmentAgeRestrictionBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAgeRestrictionBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            ageRestrictionDialogSubject.onNext(AgeRestrictionDialogState.CloseDialogClick)
        }.autoDispose()
        binding.tvAdult.throttleClicks().subscribeAndObserveOnMainThread {
            ageRestrictionDialogSubject.onNext(AgeRestrictionDialogState.AdultClick)
        }.autoDispose()
        binding.tvChild.throttleClicks().subscribeAndObserveOnMainThread {
            ageRestrictionDialogSubject.onNext(AgeRestrictionDialogState.ChildClick)
        }.autoDispose()
    }
}
