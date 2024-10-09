@file:JvmName("ActivityExtension")

package com.meetfriend.app.newbase.extension

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.meetfriend.app.R

fun Activity.startActivityWithDefaultAnimation(intent: Intent) {
    startActivity(intent)
    overridePendingTransition(R.anim.activity_move_in_from_right, R.anim.activity_move_out_to_left)
}

fun Activity.startActivityForResultWithDefaultAnimation(intent: Intent, requestCode: Int) {
    startActivityForResult(intent, requestCode)
    overridePendingTransition(R.anim.activity_move_in_from_right, R.anim.activity_move_out_to_left)
}

fun Activity.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Activity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Activity.hideKeyboard(): Boolean {
    val view = window.currentFocus
    return hideKeyboard(window, view)
}

fun Activity.hideKeyboard(view: View): Boolean {
    return hideKeyboard(window, view)
}

private fun hideKeyboard(window: Window, view: View?): Boolean {
    if (view == null) {
        return false
    }
    val inputMethodManager =
        window.context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as? InputMethodManager
    return inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0) ?: false
}

inline fun <reified T : ViewModel> AppCompatActivity.getViewModelFromFactory(vmFactory: ViewModelProvider.Factory): T {
    return ViewModelProvider(this, vmFactory)[T::class.java]
}
