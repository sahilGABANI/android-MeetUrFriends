package com.meetfriend.app.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.squareup.picasso.Picasso
import contractorssmart.app.utilsclasses.PreferenceHandler

open class BaseFragmentNew : Fragment() {

    fun getUserId(): String {
        return PreferenceHandler.readString(requireActivity(), PreferenceHandler.USER_ID, "")
    }

    fun showLoading(show: Boolean?) {
        if (show!!) showLoading() else hideLoading()
    }

    fun showToast(msg: CharSequence) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    fun hideKeyboard(activity: Activity) {
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Check if no view has focus
        val currentFocusedView = activity.currentFocus
        currentFocusedView?.let {
            inputMethodManager.hideSoftInputFromWindow(
                currentFocusedView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    fun hideKeyboard() {
        requireActivity().window
            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

    }

    protected fun showLoading() {
        CallProgressWheel.showLoadingDialog(requireActivity())
        // spotsDialog!!.show()
    }

    protected fun hideLoading() {
        CallProgressWheel.dismissLoadingDialog()
//        if (spotsDialog != null) {
//            spotsDialog!!.dismiss()
//        }
    }

}