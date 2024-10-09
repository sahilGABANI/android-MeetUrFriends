package com.meetfriend.app.ui.livestreaming.videoroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.meetfriend.app.R
import com.meetfriend.app.databinding.DialogLiveStreamEnterPasswordBinding
import com.meetfriend.app.newbase.BaseDialogFragment

class LiveStreamingEnterPasswordDialog :
    BaseDialogFragment() {

    private var _binding: DialogLiveStreamEnterPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogLiveStreamEnterPasswordBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }
}
