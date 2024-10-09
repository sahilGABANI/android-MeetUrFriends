package com.meetfriend.app.ui.challenge.bottomsheet

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentChallengeBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.challenge.AddNewChallengeActivity
import com.meetfriend.app.ui.challenge.CreateChallengeActivity
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewModel
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewState
import com.meetfriend.app.utilclasses.CallProgressWheel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ChallengeBottomSheetFragment : BaseBottomSheetDialogFragment() {

    private val optionClickSubject: PublishSubject<String> = PublishSubject.create()
    val optionClick: Observable<String> = optionClickSubject.hide()

    private var _binding: FragmentChallengeBottomSheetBinding? = null
    private val binding get() = _binding!!
    private var mediaPath: String? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChallengeViewModel>
    private lateinit var challengeViewModel: ChallengeViewModel

    var challengeId: Int = 0

    companion object {

        private const val INTENT_CHALLENGE_ID = "ChallengeId"
        private const val INTENT_CHALLENGE_TITLE = "ChallengeTitle"
        fun newInstance(
            challengeId: Int,
            challengeTitle: String
        ): ChallengeBottomSheetFragment {
            val args = Bundle()
            challengeId.let { args.putInt(INTENT_CHALLENGE_ID, it) }
            challengeTitle.let { args.putString(INTENT_CHALLENGE_TITLE, it) }
            val fragment = ChallengeBottomSheetFragment()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
        MeetFriendApplication.component.inject(this)
        challengeViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeBottomSheetBinding.inflate(inflater, container, false)
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
        listenToViewModel()
    }

    private fun listenToViewModel() {
        challengeViewModel.challengeState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChallengeViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ChallengeViewState.LoadingState -> {
                    if (it.isLoading) {
                        CallProgressWheel.showLoadingDialog(requireContext())
                        binding.postAppCompatTextView.visibility = View.GONE
                    } else {
                        CallProgressWheel.dismissLoadingDialog()
                        binding.postAppCompatTextView.visibility = View.VISIBLE
                    }
                }
                is ChallengeViewState.SuccessCreateChallengeMessage -> {
                    showToast(it.successMessage)
                    optionClickSubject.onNext(challengeId.toString())
                    dismiss()
                }
                else -> {

                }
            }
        }.autoDispose()
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
        challengeId = arguments?.getInt(INTENT_CHALLENGE_ID) ?: 0
        val challengeTitle = arguments?.getString(INTENT_CHALLENGE_TITLE) ?: 0
        binding.titleAppCompatTextView.text = "$challengeTitle"

        binding.close.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()

        binding.llUpload.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResult(
                AddNewChallengeActivity.launchActivity(
                    requireActivity(),
                    AddNewChallengeActivity.MEDIA_TYPE_IMAGE
                ), CreateChallengeActivity.REQUEST_FOR_CHOOSE_PHOTO
            )
        }

        binding.postAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.edtDescription.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_please_enter_challenge_description))
            } else if (mediaPath.isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_please_select_media_file))
            } else {

                val builder = MultipartBody.Builder()
                builder.setType(MultipartBody.FORM)
                builder.addFormDataPart("challenge_id", challengeId.toString())
                builder.addFormDataPart("description", binding.edtDescription.text.toString())

                // Map is used to multipart the file using okhttp3.RequestBody
                // Multiple Images
                // Map is used to multipart the file using okhttp3.RequestBody
                // Multiple Images

                val file = File(mediaPath)
                builder.addFormDataPart(
                    "media",
                    file.name,
                    file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                )

                val requestBody = builder.build()
                challengeViewModel.uploadChallengeAcceptFile(requestBody, challengeId)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == CreateChallengeActivity.REQUEST_FOR_CHOOSE_PHOTO) {
            var filePath = data?.getStringExtra("FILE_PATH")
            mediaPath = filePath

            binding.mediaLinearLayout.visibility = View.GONE
            binding.photoAppCompatImageView.visibility = View.VISIBLE

            Glide.with(requireActivity())
                .load(filePath)
                .centerCrop()
                .into(binding.photoAppCompatImageView)
        }
    }
}