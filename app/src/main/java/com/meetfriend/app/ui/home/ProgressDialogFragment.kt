package com.meetfriend.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.meetfriend.app.R
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.DialogProgressOfUploadBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showLongToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewModel
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewState
import com.meetfriend.app.ui.main.viewmodel.HomeViewModel
import com.meetfriend.app.ui.main.viewmodel.HomeViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ProgressDialogFragment : BaseDialogFragment() {

    private val progressStateSubject: PublishSubject<String> = PublishSubject.create()
    val progressState: Observable<String> = progressStateSubject.hide()

    private val progressCancelStateSubject: PublishSubject<Boolean> = PublishSubject.create()
    val progressCancelState: Observable<Boolean> = progressCancelStateSubject.hide()

    companion object {
        const val POST_TYPE = "POST_TYPE"
        fun newInstance(postType: String): ProgressDialogFragment {
            val args = Bundle()
            postType.let { args.putString(POST_TYPE, it) }
            val fragment = ProgressDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: DialogProgressOfUploadBinding? = null
    private val binding get() = _binding!!
    private var postType = ""

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddNewPostViewModel>
    private lateinit var addNewPostViewModel: AddNewPostViewModel

    @Inject
    internal lateinit var homeViewModelFactory: ViewModelFactory<HomeViewModel>
    private lateinit var homeViewModel: HomeViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogProgressOfUploadBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)
        addNewPostViewModel = getViewModelFromFactory(viewModelFactory)
        homeViewModel = getViewModelFromFactory(homeViewModelFactory)
        postType = arguments?.getString(POST_TYPE) ?: ""
        loadDataFromIntent()
        listenToViewEvent()
        listenToViewModel()
    }

    private fun loadDataFromIntent() {
        if (postType == "story") {
            homeViewModel.getLiveDataInfo()
        } else {
            addNewPostViewModel.getLiveDataInfo()
        }
    }

    private fun listenToViewModel() {
        addNewPostViewModel.addNewPostState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddNewPostViewState.ProgressDisplay -> {
                    binding.progress.progress = it.progressInfo.toInt()
                    if (it.progressInfo.equals(100.0))
                        progressStateSubject.onNext("done")
                }

                is AddNewPostViewState.UploadImageCloudFlareSuccess -> {
                    progressStateSubject.onNext("done")
                    dismiss()
                }

                is AddNewPostViewState.UploadVideoCloudFlareSuccess -> {
                    progressStateSubject.onNext("done")
                    dismiss()
                }

                is AddNewPostViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }

                else -> {}
            }
        }.autoDispose()

        homeViewModel.homeState.subscribeAndObserveOnMainThread {
            when (it) {
                is HomeViewState.ProgressDisplay -> {
                    binding.progress.progress = it.progressInfo.toInt()
                    if (it.progressInfo.equals(100.0))
                        progressStateSubject.onNext("done")
                }
                is HomeViewState.UploadVideoCloudFlareSuccess -> {
                    progressStateSubject.onNext("done")
                    dismiss()
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        binding.joinchatAppCompatTextView.text = when (postType) {
            AddNewPostInfoActivity.POST_TYPE_IMAGE,
            AddNewPostInfoActivity.POST_TYPE_POST_VIDEO -> {
                resources.getString(R.string.label_please_wait_until_we_upload_your_post)
            }
            "story" -> {
                resources.getString(R.string.label_please_wait_until_we_upload_your_story)
            }
            else -> {
                resources.getString(R.string.label_please_wait_until_we_upload_your_shorts)
            }
        }

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            if (postType != AddNewPostInfoActivity.POST_TYPE_IMAGE) {
                progressCancelStateSubject.onNext(false)
            } else {
                progressCancelStateSubject.onNext(false)
            }
        }.autoDispose()
    }
}

