package com.meetfriend.app.ui.home.create

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.meetfriend.app.R
import com.meetfriend.app.databinding.BottomSheetPostCameraBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PostCameraBottomSheet : BaseBottomSheetDialogFragment() {

    private val postCameraItemClicksSubject: PublishSubject<Boolean> = PublishSubject.create()
    val postCameraItemClicks: Observable<Boolean> = postCameraItemClicksSubject.hide()

    private var _binding: BottomSheetPostCameraBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun create(): PostCameraBottomSheet {
            return PostCameraBottomSheet()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_post_camera, container, false)
        _binding = BottomSheetPostCameraBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvents()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val bottomSheet = (view?.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun listenToViewEvents() {
        binding.tvCapturePhoto.throttleClicks().subscribeAndObserveOnMainThread {
            postCameraItemClicksSubject.onNext(true)
            dismiss()
        }.autoDispose()

        binding.tvRecordVideo.throttleClicks().subscribeAndObserveOnMainThread {
            postCameraItemClicksSubject.onNext(false)
            dismiss()
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}