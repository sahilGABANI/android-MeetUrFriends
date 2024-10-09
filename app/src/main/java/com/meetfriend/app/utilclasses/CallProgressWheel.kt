package com.meetfriend.app.utilclasses


import android.app.ProgressDialog
import android.content.Context
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.meetfriend.app.R
import com.meetfriend.app.application.MeetFriendApplication
import com.wang.avi.AVLoadingIndicatorView


object CallProgressWheel {

    private var progressDialog: ProgressDialog? = null

    private val isDialogShowing: Boolean
        get() {
            try {
                return progressDialog != null && progressDialog!!.isShowing
            } catch (e: Exception) {
                return false
            }

        }

    /*
      Displays custom loading dialog
     */
    fun showLoadingDialog(context: Context?) {
        try {
            if (isDialogShowing) {
                dismissLoadingDialog()
            }

            if (context is AppCompatActivity) {
                if (context.isFinishing) {
                    return
                }
            }
            val avLoadingIndicatorView = AVLoadingIndicatorView(MeetFriendApplication.context)
            avLoadingIndicatorView.setIndicator("LineSpinFadeLoaderIndicator")
            avLoadingIndicatorView.setIndicatorColor(
                MeetFriendApplication.context.resources.getColor(
                    R.color.color_tab_purple,null
                )
            )
            progressDialog = ProgressDialog(context, android.R.style.Theme_Translucent_NoTitleBar)
            if (progressDialog != null) {
                progressDialog?.show()

                val layoutParams = progressDialog!!.window!!.attributes
                layoutParams.dimAmount = 0.5f
                layoutParams.width = 120
                layoutParams.height = 120

                progressDialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                progressDialog?.setCancelable(false)
                progressDialog?.setContentView(avLoadingIndicatorView)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun dismissLoadingDialog() {
        try {
            if (progressDialog != null) {
                progressDialog?.dismiss()
                progressDialog = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}