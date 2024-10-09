package com.meetfriend.app.ui.camerakit.videoplayer.event

import android.net.Uri

interface OnVideoEditedEvent {
    fun getResult(uri: Uri)

    fun onError(message: String)
    fun onProgress(percentage: Int)
}