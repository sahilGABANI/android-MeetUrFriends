package com.meetfriend.app.newbase.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager

@SuppressLint("LongLogTag")
fun Context?.hideKeyboard() {
    if (this == null) {
        return
    }
    val inputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    var activity: Activity? = null
    if (this is Activity) {
        activity = this
    } else if (this is ContextWrapper) {
        val parentContext = this.baseContext
        if (parentContext is Activity) {
            activity = parentContext
        }
    }

    if (activity == null) {
        return
    }

    if (activity.currentFocus != null) {
        inputMethodManager.hideSoftInputFromWindow(activity.currentFocus!!.windowToken, 0)
    } else {
    }
}

@SuppressLint("LongLogTag")
fun Context?.showKeyboard() {
    if (this == null) {
        return
    }
    val inputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    var activity: Activity? = null
    if (this is Activity) {
        activity = this
    } else if (this is ContextWrapper) {
        val parentContext = this.baseContext
        if (parentContext is Activity) {
            activity = parentContext
        }
    }

    if (activity == null) {
        return
    }

    val currentFocusView = activity.currentFocus
    currentFocusView?.postDelayed({ inputMethodManager.showSoftInput(currentFocusView, 0) }, 100)
            ?: Log.w("Try to show keyboard but there is no current focus view","")
}

fun View.openKeyboard(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}
