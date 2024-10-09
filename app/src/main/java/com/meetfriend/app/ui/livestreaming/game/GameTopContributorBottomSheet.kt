package com.meetfriend.app.ui.livestreaming.game

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.livestreaming.model.TopGifter
import com.meetfriend.app.databinding.GameTopContributorBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.livestreaming.game.view.GameTopContributorAdapter

class GameTopContributorBottomSheet : BaseBottomSheetDialogFragment() {
    companion object {

        private const val INTENT_TOP_GIFTER_LIST = "INTENT_TOP_GIFTER_LIST"

        fun newInstance(topGifterList: ArrayList<TopGifter>): GameTopContributorBottomSheet {
            val args = Bundle()
            topGifterList.let { args.putParcelableArrayList(INTENT_TOP_GIFTER_LIST, it) }
            val fragment = GameTopContributorBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: GameTopContributorBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var gameTopContributorAdapter: GameTopContributorAdapter

    private var listOfTopGifter: ArrayList<TopGifter>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GameTopContributorBottomSheetBinding.inflate(inflater, container, false)
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

        listOfTopGifter = arguments?.getParcelableArrayList(INTENT_TOP_GIFTER_LIST) ?: null

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
        binding.close.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }

        gameTopContributorAdapter = GameTopContributorAdapter(requireContext())

        binding.rvTopContributor.adapter = gameTopContributorAdapter
        gameTopContributorAdapter.listOfDataItems = listOfTopGifter
    }
}
