package com.meetfriend.app.base

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.meetfriend.app.R
import com.meetfriend.app.utilclasses.CallProgressWheel
import com.squareup.picasso.Picasso
import contractorssmart.app.utilsclasses.PreferenceHandler


abstract class BaseActivity : AppCompatActivity() {


    abstract fun initializeLayout(): Int
    abstract fun inflateLayout()

    var baseActivity: BaseActivity? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseActivity = this@BaseActivity
        setContentView(initializeLayout())
        inflateLayout()
    }

    fun showToast(msg: CharSequence) {
        Toast.makeText(baseActivity, msg, Toast.LENGTH_SHORT).show()
    }

    fun showLoading(show: Boolean?) {
        if (show!!) showLoading() else hideLoading()
    }

    protected fun showLoading() {
        CallProgressWheel.showLoadingDialog(this@BaseActivity)
    }

    protected fun hideLoading() {
        CallProgressWheel.dismissLoadingDialog()
    }
    }
