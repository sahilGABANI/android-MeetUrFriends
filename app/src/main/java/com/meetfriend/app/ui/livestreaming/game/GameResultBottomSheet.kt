package com.meetfriend.app.ui.livestreaming.game

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.livestreaming.model.GameResult
import com.meetfriend.app.databinding.BottomSheetGameResultBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant

class GameResultBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {

        private const val INTENT_GAME_RESULT_INFO = "INTENT_GAME_RESULT_INFO"

        fun newInstance(endGameInfo: GameResult): GameResultBottomSheet {
            val args = Bundle()
            endGameInfo.let { args.putParcelable(INTENT_GAME_RESULT_INFO, it) }

            val fragment = GameResultBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: BottomSheetGameResultBinding? = null
    private val binding get() = _binding!!

    private var gameResultInfo: GameResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetGameResultBinding.inflate(inflater, container, false)
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

        gameResultInfo = arguments?.getParcelable(INTENT_GAME_RESULT_INFO) ?: null
        setData()
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

    private fun setData() {
        val totalCents =
            (gameResultInfo?.centsValue?.toFloat() ?: 0F) * (
                gameResultInfo?.totalCoins?.toFloat()
                    ?: 0F
                )
        binding.totalTimeAppCompatTextView.text = gameResultInfo?.totalGameDuration
        binding.totalLikeAppCompatTextView.text =
            gameResultInfo?.likes?.prettyCount().toString().plus(" ").plus(" ")
                .plus(resources.getString(R.string.label_liked_users))
        binding.totalGiftsAppCompatTextView.text =
            gameResultInfo?.totalGifts.toString().plus(" ").plus(" ")
                .plus(resources.getString(R.string.label_gifts))
        binding.totalCoinsAppCompatTextView.text =
            gameResultInfo?.totalCoins.toString().plus(" ").plus(" ")
                .plus(resources.getString(R.string.label_coins))
        binding.totalCentsAppCompatTextView.text = totalCents.toString().plus(" ").plus(" ")
            .plus(resources.getString(R.string.label_cents))

        binding.closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()

        when (gameResultInfo?.result) {
            Constant.GAME_RESULT_WIN -> {
                binding.resultAppCompatImageView.setImageResource(R.drawable.ic_game_win_emoji)
                binding.winLossStatusAppCompatTextView.text =
                    resources.getString(R.string.label_you_win_this_game)
            }
            Constant.GAME_RESULT_LOSS -> {
                binding.resultAppCompatImageView.setImageResource(R.drawable.ic_game_loss_emoji)
                binding.winLossStatusAppCompatTextView.text =
                    resources.getString(R.string.label_you_lose_this_game)
            }
            Constant.GAME_RESULT_DRAW -> {
                binding.resultAppCompatImageView.setImageResource(R.drawable.ic_game_draw_emoji)
                binding.winLossStatusAppCompatTextView.text =
                    resources.getString(R.string.label_it_is_draw)
            }
        }
    }
}
