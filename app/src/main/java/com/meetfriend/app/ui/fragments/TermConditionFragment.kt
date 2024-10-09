package com.meetfriend.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.meetfriend.app.base.BaseFragment
import com.meetfriend.app.databinding.FragmentPolicyBinding
import com.meetfriend.app.utils.Constant.TERMS_N_CONDITIONS

class TermConditionFragment : BaseFragment() {
    lateinit var binding: FragmentPolicyBinding

    override fun provideYourFragmentView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPolicyBinding.inflate(inflater, parent, false)
        binding.textHeader.text = "Term & Conditions"

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.webview.settings.javaScriptEnabled = true

        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                view.loadUrl(url)
                return true // Indicates the host application handles the URL
            }
        }
        binding.webview.loadUrl(TERMS_N_CONDITIONS)
    }
}
