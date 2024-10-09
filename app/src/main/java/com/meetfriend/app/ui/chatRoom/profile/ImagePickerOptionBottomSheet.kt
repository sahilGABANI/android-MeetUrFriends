package com.meetfriend.app.ui.chatRoom.profile

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.profile.model.SelectedOption
import com.meetfriend.app.databinding.BottomSheetImagePickerOptionBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ImagePickerOptionBottomSheet : BaseBottomSheetDialogFragment() {

    private val imageSelectionOptionClickSubject: PublishSubject<String> = PublishSubject.create()
    val imageSelectionOptionClicks: Observable<String> = imageSelectionOptionClickSubject.hide()

    private var _binding: BottomSheetImagePickerOptionBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetImagePickerOptionBinding.inflate(inflater, container, false)
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
        binding.tvCameraOption.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
            imageSelectionOptionClickSubject.onNext(SelectedOption.Camera.name)
        }.autoDispose()

        binding.tvGalleryOption.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
            imageSelectionOptionClickSubject.onNext(SelectedOption.Gallery.name)
        }.autoDispose()

        binding.tvVideoCameraOption.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
            imageSelectionOptionClickSubject.onNext(SelectedOption.VideoCamera.name)
        }.autoDispose()

        binding.tvVideoGalleryOption.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
            imageSelectionOptionClickSubject.onNext(SelectedOption.VideoGallery.name)
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }
}