package com.meetfriend.app.ui.camerakit.videoplayer.event

import com.meetfriend.app.ui.camerakit.videoplayer.view.RangeSeekBarView


interface OnRangeSeekBarEvent {
    fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float)
    fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float)
    fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float)
    fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float)
}