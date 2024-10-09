package com.meetfriend.app.ui.camerakit.termsofservice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.meetfriend.app.R
import com.meetfriend.app.databinding.FragmentStandardPromptDialogBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.camerakit.model.StandardPromptDialogState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class StandardPromptDialog : BaseDialogFragment() {

    private val standardPromptDialogSubject: PublishSubject<StandardPromptDialogState> =
        PublishSubject.create()
    val standardPromptDialogState: Observable<StandardPromptDialogState> =
        standardPromptDialogSubject.hide()

    companion object {
        const val PRIVACY_START = 48
        const val PRIVACY_END = 62
        const val TERMS_START = 83
        const val TERMS_END = 99
        const val LEARN_START = 171
        const val LEARN_END = 181
        fun newInstance(): StandardPromptDialog {
            return StandardPromptDialog()
        }
    }
    private var _binding: FragmentStandardPromptDialogBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStandardPromptDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val spannableText = SpannableString(
            "By using Lenses, you acknowledge reading Snap's Privacy Policy and agree to Snap's Terms of Service. " +
                "Some Lenses use information about your face," +
                " hands and voice to work. Learn More, and if you want to " +
                "agree and continue, tap below."
        )
        val privacyPolicySpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://values.snap.com/privacy/privacy-policy"))
                startActivity(intent)
            }
        }
        val termsOfServiceSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://snap.com/terms"))
                startActivity(intent)
            }
        }
        val learnMoreSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://help.snapchat.com/hc/en-us/articles/7012366118676")
                )
                startActivity(intent)
            }
        }

        spannableText.setSpan(privacyPolicySpan, PRIVACY_START, PRIVACY_END, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableText.setSpan(termsOfServiceSpan, TERMS_START, TERMS_END, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableText.setSpan(learnMoreSpan, LEARN_START, LEARN_END, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvTerms.text = spannableText
        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()

        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            standardPromptDialogSubject.onNext(StandardPromptDialogState.CloseDialogClick)
        }.autoDispose()
        binding.tvAccept.throttleClicks().subscribeAndObserveOnMainThread {
            standardPromptDialogSubject.onNext(StandardPromptDialogState.AcceptClick)
        }.autoDispose()
    }
}
