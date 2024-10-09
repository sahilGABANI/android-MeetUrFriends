package com.meetfriend.app.ui.main.post

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.R
import com.meetfriend.app.api.post.model.PostLikesInformation
import com.meetfriend.app.databinding.PostLikesBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.main.post.view.PostLikesAdapter
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class PostLikesDialog : BaseBottomSheetDialogFragment() {

    companion object {
        private const val INTENT_POST_LIKES = "listOfPostLikes"
        private const val SEARCHING_REFRESH_TIME = 400

        fun newInstance(listOfPostLikes: ArrayList<PostLikesInformation>): PostLikesDialog {
            val args = Bundle()
            listOfPostLikes.let { args.putParcelableArrayList(INTENT_POST_LIKES, it) }
            val fragment = PostLikesDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: PostLikesBinding? = null
    private val binding get() = _binding!!

    private var listOfPostLikes: ArrayList<PostLikesInformation>? = null
    private var allUsers: ArrayList<PostLikesInformation>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = PostLikesBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
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
        listOfPostLikes =
            arguments?.getParcelableArrayList(INTENT_POST_LIKES)
                ?: throw IllegalStateException("No args provided")

        listenToViewEvent()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                return
            }
        }

    private fun listenToViewEvent() {

        val rvAdapter = PostLikesAdapter(requireContext())
        binding.tagPeopleRV.adapter = rvAdapter
        allUsers = listOfPostLikes // Initialize the allUsers list with the received data
        binding.closeAppCompatImageView.setOnClickListener {
            dismiss()
        }

        binding.tagPeopleRV.setHasFixedSize(true)
        binding.tagPeopleRV.layoutManager = LinearLayoutManager(context)
        binding.tagPeopleRV.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.tagPeopleRV.setEmptyView(binding.tvNoFriends)
        binding.closesearchAppCompatImageView.setOnClickListener {
            binding.searchAppCompatEditText.text = null
        }
        binding.searchAppCompatEditText.textChanges()
            .debounce(SEARCHING_REFRESH_TIME.toLong(), TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isNotEmpty()) {
                    val filteredUsers = allUsers?.filter { userModel ->
                        userModel.user?.userName?.contains(it, ignoreCase = true) == true
                    }
                    rvAdapter.listOfDataItems = filteredUsers
                    binding.closesearchAppCompatImageView.visibility = View.VISIBLE
                } else {
                    rvAdapter.listOfDataItems = allUsers
                    binding.closesearchAppCompatImageView.visibility = View.GONE
                }
                rvAdapter.notifyDataSetChanged()
            }.autoDispose()
        rvAdapter.listOfDataItems = listOfPostLikes
    }
}

