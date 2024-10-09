@file:JvmName("FragmentExtension")

package com.meetfriend.app.newbase.extension

import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.meetfriend.app.R

fun Fragment.startActivityForResultWithDefaultAnimation(intent: Intent, requestCode: Int) {
    startActivityForResult(intent, requestCode)
    activity?.overridePendingTransition(R.anim.activity_move_in_from_right, R.anim.activity_move_out_to_left)
}

fun Fragment.showToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun Fragment.showLongToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
}

inline fun <reified T : ViewModel> Fragment.getViewModelFromFactory(vmFactory: ViewModelProvider.Factory): T {
    return ViewModelProvider(this, vmFactory)[T::class.java]
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}