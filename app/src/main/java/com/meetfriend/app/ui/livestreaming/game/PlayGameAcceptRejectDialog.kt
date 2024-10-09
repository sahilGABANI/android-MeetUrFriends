package com.meetfriend.app.ui.livestreaming.game

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.databinding.DialogPlayGameAcceptRejectBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant.FiXED_100_INT
import com.meetfriend.app.utils.Constant.FiXED_90_INT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PlayGameAcceptRejectDialog(
    private val chatUserName: String?,
    private val profilePhoto: String?,
    private val isVerified: Int?
) : BaseDialogFragment() {

    private var _binding: DialogPlayGameAcceptRejectBinding? = null
    private val binding get() = _binding!!

    private val gameRequestStatesSubject: PublishSubject<Boolean> = PublishSubject.create()
    val gameRequestStates: Observable<Boolean> = gameRequestStatesSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogPlayGameAcceptRejectBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewEvent()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setWidthHeightPercent(FiXED_90_INT)
    }

    private fun DialogFragment.setWidthHeightPercent(percentageWidthInt: Int) {
        val percentageWidth = percentageWidthInt.toFloat() / FiXED_100_INT
        val dm = resources.displayMetrics
        val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
        val percentWidth = rect.width() * percentageWidth
        dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun listenToViewEvent() {
        binding.ivUserProfile.visibility =
            if (profilePhoto.isNullOrEmpty()) View.GONE else View.VISIBLE
        Glide.with(requireContext())
            .load(profilePhoto ?: "")
            .placeholder(R.drawable.image_placeholder)
            .into(binding.ivUserProfile)
        binding.tvUsername.text = chatUserName ?: ""
        if (isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }
        binding.tvAccept.throttleClicks().subscribeAndObserveOnMainThread {
            gameRequestStatesSubject.onNext(true)
        }.autoDispose()

        binding.tvReject.throttleClicks().subscribeAndObserveOnMainThread {
            dismissDialog()
        }.autoDispose()
    }

    fun dismissDialog() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
