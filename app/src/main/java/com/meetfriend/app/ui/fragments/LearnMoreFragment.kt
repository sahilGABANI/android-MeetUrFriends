package com.meetfriend.app.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.meetfriend.app.base.BaseFragment
import com.meetfriend.app.databinding.FragmentLearnMoreBinding
import com.meetfriend.app.utils.Constant.LEARN_MORE

class LearnMoreFragment : BaseFragment() {

    lateinit var binding: FragmentLearnMoreBinding

    override fun provideYourFragmentView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLearnMoreBinding.inflate(inflater, parent, false)
        binding.textHeader.text = "Learn More"

        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
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
        binding.webview.loadUrl(LEARN_MORE)
    }
}
