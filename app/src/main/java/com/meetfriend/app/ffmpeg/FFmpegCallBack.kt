package com.meetfriend.app.ffmpeg

import com.arthenica.mobileffmpeg.LogMessage
import com.arthenica.mobileffmpeg.Statistics

interface FFmpegCallBack {
    fun process(logMessage: LogMessage){}
    fun statisticsProcess(statistics: Statistics) {}
    fun success(){}
    fun cancel(){}
    fun failed(){}
}