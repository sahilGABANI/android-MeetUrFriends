package com.meetfriend.app.newbase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import dagger.Lazy
import javax.inject.Inject

class ViewModelFactory<T : ViewModel> @Inject constructor(
        private val viewModel: Lazy<T>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = viewModel.get() as T
}