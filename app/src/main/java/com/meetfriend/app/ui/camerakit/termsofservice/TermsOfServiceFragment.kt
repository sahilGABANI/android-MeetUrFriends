package com.meetfriend.app.ui.camerakit.termsofservice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.meetfriend.app.R
import com.meetfriend.app.databinding.FragmentTermsOfServiceBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.camerakit.model.TermsOfServiceState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class TermsOfServiceFragment : BaseDialogFragment() {

    private val termsOfServiceDialogSubject: PublishSubject<TermsOfServiceState> =
        PublishSubject.create()
    val termsOfServiceDialogState: Observable<TermsOfServiceState> =
        termsOfServiceDialogSubject.hide()

    companion object {
        fun newInstance(): TermsOfServiceFragment {
            return TermsOfServiceFragment()
        }
    }

    private var _binding: FragmentTermsOfServiceBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermsOfServiceBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.tvAccept.throttleClicks().subscribeAndObserveOnMainThread {
            termsOfServiceDialogSubject.onNext(TermsOfServiceState.AcceptClick)
        }.autoDispose()
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            termsOfServiceDialogSubject.onNext(TermsOfServiceState.CloseDialogClick)
        }.autoDispose()
    }
}
