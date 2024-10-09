package com.meetfriend.app.ui.camerakit.attachment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.post.model.LinkAttachmentState
import com.meetfriend.app.api.post.model.ResultGoogleSearchData
import com.meetfriend.app.api.post.model.ResultWebData
import com.meetfriend.app.databinding.BottomsheetVideoTimerBinding
import com.meetfriend.app.databinding.FragmentLinkAttachmentBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.extension.hideKeyboard
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LinkAttachmentBottomSheetFragment : BaseBottomSheetDialogFragment() {

    private var _binding: FragmentLinkAttachmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var webLogoUrl: String
    private lateinit var webTitle: String
    private lateinit var webUrl: String
    private lateinit var googleSearchLogoUrl: String
    private lateinit var googleSearchTitle: String
    private lateinit var googleSearchUrl: String
    private var isWebOpen = false
    private var isAddButtonClickable = false


    private val linkAttachmentStateSubject: PublishSubject<LinkAttachmentState> = PublishSubject.create()
    val linkAttachmentState: Observable<LinkAttachmentState> = linkAttachmentStateSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLinkAttachmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
        listenToViewEvents()

    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    private fun listenToViewEvents() {
        googleSearchLogoUrl = "gujjubet.com"

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.addJavascriptInterface(this, "Android")

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Check if the loaded URL is from Google search results
                if (url?.startsWith("https://www.google.com/search?q=") == true) {
                    // Extract JSON data from the Google search results page

                    binding.progressBar.visibility = View.GONE
                    isWebOpen = false
                    isAddButtonClickable = true
                } else {
                    // Extract data from any other website
                    binding.progressBar.visibility = View.GONE
                    isWebOpen = true
                    isAddButtonClickable = true
                    extractDataFromWebView()
                }
            }
        }
        binding.searchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Handle the actionDone event
                binding.progressBar.visibility = View.VISIBLE
                val query = binding.searchQuery.text.toString()
                googleSearchTitle = "$query - Google Search"
                googleSearchUrl = "https://www.google.com/search?q=$query"
                searchGoogle(query)
                requireActivity().hideKeyboard(binding.searchQuery)
                binding.searchQuery.clearFocus()
                true
            } else {
                false
            }
        }

        binding.addAttachment.setOnClickListener {
            if (isAddButtonClickable) {
                if (isWebOpen) {
                    val resultWebData = ResultWebData(webLogoUrl, webTitle, webUrl)
                    linkAttachmentStateSubject.onNext(LinkAttachmentState.WebAddClick(resultWebData))
                    dismiss()
                } else {
                    val resultGoogleSearchData = ResultGoogleSearchData(googleSearchTitle, googleSearchUrl)
                    linkAttachmentStateSubject.onNext(LinkAttachmentState.GoogleAddClick(resultGoogleSearchData))
                    dismiss()
                }
            }
        }

        binding.exitButton.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }


    private fun searchGoogle(query: String) {
        binding.webView.loadUrl("https://www.google.com/search?q=$query")
        requireActivity().hideKeyboard()
    }

    private fun Activity.hideKeyboard() {
        val view: View? = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun extractDataFromWebView() {
        binding.webView.evaluateJavascript(
            "(function() { " + "   var title = document.title;" + "   var url = window.location.href;" + "   var favicon = document.querySelector('link[rel=\"shortcut icon\"]') ? document.querySelector('link[rel=\"shortcut icon\"]').href : '';" + "   if (!favicon) favicon = document.querySelector('link[rel=\"icon\"]') ? document.querySelector('link[rel=\"icon\"]').href : '';" + "   if (!favicon) favicon = 'https://www.google.com/s2/favicons?domain=' + url.split('/')[2];" + "   return JSON.stringify({title: title, url: url, favicon: favicon});" + "})();"
        ) { result ->
            val newResult = result.replace("{", "").replace("}", "").replace("\\", "").split(",")
            for (item in newResult.indices) {
                val tempStr = newResult[item].replace("\"", "")
                when (item) {
                    0 -> {
                        webTitle = tempStr.removePrefix("title:")
                    }
                    1 -> {
                        val str = tempStr.removePrefix("url:")
                        webUrl = str
                    }
                    2 -> {
                        val str = tempStr.removePrefix("favicon:")
                        webLogoUrl = str
                    }
                }
            }
        }
    }

}