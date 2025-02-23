package com.meetfriend.app.ffmpeg

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.LogMessage
import com.arthenica.mobileffmpeg.Statistics
import java.util.concurrent.CyclicBarrier

class FFmpegUtils {

    fun callFFmpegQuery(query: Array<String>, fFmpegCallBack: FFmpegCallBack) {
        val gate = CyclicBarrier(2)
        object : Thread() {
            override fun run() {
                gate.await()
                process(query, fFmpegCallBack)
            }
        }.start()
        gate.await()
    }

    private fun process(query: Array<String>, ffmpegCallBack: FFmpegCallBack) {
        val processHandler = Handler(Looper.getMainLooper())
        Config.enableLogCallback { logMessage ->
            val logs = LogMessage(logMessage.executionId, logMessage.level, logMessage.text)
            processHandler.post {
                ffmpegCallBack.process(logs)
            }
        }
        Config.enableStatisticsCallback { statistics ->
            val statisticsLog =
                Statistics(
                    statistics.executionId,
                    statistics.videoFrameNumber,
                    statistics.videoFps,
                    statistics.videoQuality,
                    statistics.size,
                    statistics.time,
                    statistics.bitrate,
                    statistics.speed
                )
            processHandler.post {
                ffmpegCallBack.statisticsProcess(statisticsLog)
            }
        }
        when (FFmpeg.execute(query)) {
            Config.RETURN_CODE_SUCCESS -> {
                processHandler.post {
                    ffmpegCallBack.success()
                }
            }
            Config.RETURN_CODE_CANCEL -> {
                processHandler.post {
                    ffmpegCallBack.cancel()
                    FFmpeg.cancel()
                }
            }
            else -> {
                processHandler.post {
                    ffmpegCallBack.failed()
                    Config.printLastCommandOutput(Log.INFO)
                }
            }
        }
    }
}