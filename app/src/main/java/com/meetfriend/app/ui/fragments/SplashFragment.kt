package com.meetfriend.app.ui.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.meetfriend.app.BuildConfig
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.ui.main.MainHomeActivity
import com.vansuita.library.CheckNewAppVersion
import javax.inject.Inject


class SplashFragment : Fragment() {

    companion object {
        const val SPLASH_DELAY: Long = 2000
    }

    lateinit var mContext: Context

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mContext = requireContext()
    }

    override fun onResume() {
        super.onResume()
        if (::mContext.isInitialized) {
            CheckNewAppVersion(context).setOnTaskCompleteListener { result ->
                if (result.hasNewVersion()) {
                    val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(
                        ContextThemeWrapper(
                            requireContext(),
                            R.style.DialogAnimation
                        )
                    )

                    val message = resources.getString(
                        R.string.label_new_version_available_message,
                        resources.getString(R.string.application_name),
                        BuildConfig.VERSION_NAME
                    )
                    alertDialogBuilder.setTitle(mContext.resources.getString(R.string.label_new_version_available))
                    alertDialogBuilder.setMessage(message)
                    alertDialogBuilder.setCancelable(false)
                    alertDialogBuilder.setPositiveButton(
                        R.string.label_update_now
                    ) { dialog, _ ->
                        result.openUpdateLink()
                        dialog.cancel()
                    }
                    alertDialogBuilder.setNegativeButton(
                        R.string.label_cancel
                    ) { dialog, _ ->
                        dialog.cancel()
                        callUIMethods()
                    }
                    alertDialogBuilder.show()
                } else {
                    callUIMethods()
                }
            }.execute()
        }
    }


    private fun callUIMethods() {
        Handler(Looper.getMainLooper()).postDelayed(
            {
                if (loggedInUserCache.getLoginUserToken() == null) {
                    try {
                        lifecycleScope.launchWhenResumed {
                            startActivity(MainHomeActivity.getIntent(mContext,false))
                            (mContext as Activity).finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        lifecycleScope.launchWhenResumed {
                            startActivity(MainHomeActivity.getIntent(mContext))
                            (mContext as Activity).finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, SPLASH_DELAY
        )
    }
}

