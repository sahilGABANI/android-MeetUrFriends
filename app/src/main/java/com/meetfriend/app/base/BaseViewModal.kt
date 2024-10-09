package com.meetfriend.app.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModal:ViewModel() {
    var apiError = MutableLiveData<String>()
    var onFailure = MutableLiveData<Throwable>()
    var isLoading = MutableLiveData<Boolean>()
}