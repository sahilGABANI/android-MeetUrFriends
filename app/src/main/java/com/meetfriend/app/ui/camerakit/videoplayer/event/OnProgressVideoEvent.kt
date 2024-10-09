package com.meetfriend.app.ui.camerakit.videoplayer.event

interface OnProgressVideoEvent {

    fun updateProgress(time: Float, max: Long, scale: Long)
}