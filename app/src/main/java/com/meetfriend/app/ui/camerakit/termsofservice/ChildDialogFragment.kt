package com.meetfriend.app.ui.camerakit.termsofservice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.meetfriend.app.R
import com.meetfriend.app.databinding.FragmentChildDialogBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.camerakit.model.ChildDialogState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ChildDialogFragment : BaseDialogFragment() {
    private val childDialogSubject: PublishSubject<ChildDialogState> =
        PublishSubject.create()
    val childDialogState: Observable<ChildDialogState> =
        childDialogSubject.hide()
    companion object {
        fun newInstance(): ChildDialogFragment {
            return ChildDialogFragment()
        }
    }

    private var _binding: FragmentChildDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChildDialogBinding.inflate(inflater, container, false)
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
            childDialogSubject.onNext(ChildDialogState.CloseDialogClick)
        }.autoDispose()
        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            childDialogSubject.onNext(ChildDialogState.CloseDialogClick)
        }.autoDispose()
        binding.tvIAmGuardian.throttleClicks().subscribeAndObserveOnMainThread {
            childDialogSubject.onNext(ChildDialogState.IAmGuardianClick)
        }.autoDispose()
    }
}
