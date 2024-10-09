package com.meetfriend.app.utilclasses.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.arthenica.mobileffmpeg.FFmpeg
import com.meetfriend.app.R
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.utils.FileUtils
import java.io.File


class DownloadService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: NotificationCompat.Builder

    var videoUrl: String? = null
    var videoId: String? = null
    val videoFileName: String
        get() {
            val rnds = (0..10000).random()

            return "video_${videoId}_${rnds}.mp4"
        }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        videoUrl = intent.getStringExtra("videoUrl")
        videoId = intent.getIntExtra("videoId", 0).toString()
        val url = videoUrl

        val downloadFilePath = getFilePath(videoFileName)
        if (url != null && downloadFilePath != null) {
            startVideoDownload(url, downloadFilePath)
        }
        return START_STICKY

    }

    private fun getFilePath(videoFileName: String): String? {

        val dirPathFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/Meeturfriends/Video"
        )
        dirPathFile.exists()
        dirPathFile.mkdirs()
        // Create the storage directory if it does not exist
        if (!dirPathFile.exists()) {
            if (!dirPathFile.mkdirs()) {
            }
        }
        dirPath = dirPathFile.absolutePath
        return File(dirPathFile, videoFileName).absolutePath

    }

    override fun onCreate() {
        super.onCreate()
        notification = NotificationCompat.Builder(this, download_channel_id)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloading...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                download_channel_id,
                "Download channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notification.build())
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        var dirPath: String? = null
        private const val download_channel_id = "Download Channel"
    }


    private fun startVideoDownload(videoDownloadUrl: String, downloadFilePath: String) {
        val cmd = java.lang.String.format(
            "-i %s -acodec %s -bsf:a aac_adtstoasc -vcodec %s %s",
            videoDownloadUrl,
            "copy",
            "copy",
            downloadFilePath
        )
        val command = cmd.split(" ".toRegex()).toTypedArray()
        execFFmpegBinaryForDownloadVideo(command, downloadFilePath)
    }

    private fun prepareVideoFile(videoFilePath: String) {
        val rnds = (0..10000).random()
        val outPutFilePath = getFilePath("${rnds}_${videoFileName}") ?: return
        val logoFilePath = FileUtils.getHinoteLogoFile(applicationContext).absolutePath

        val complexCommand: Array<String> = arrayOf(
            "-y",
            "-i",
            videoFilePath,
            "-i",
            logoFilePath,
            "-filter_complex",
            "[1][0]scale2ref=w=oh*mdar:h=ih*0.05[logo][video];[video][logo]overlay=(W-w -15):(H-h-15)",
            "-c:a",
            "copy",
            outPutFilePath
        )
        execFFmpegBinaryForVideo(complexCommand, videoFilePath, outPutFilePath)
    }

    private fun execFFmpegBinaryForVideo(
        command: Array<String>,
        videoFilePath: String,
        outPutFilePath: String
    ) {
        try {
            val handler = Handler(Looper.getMainLooper())

            Thread {
                when (FFmpeg.execute(command)) {
                    0 -> {
                        cancel()
                        val deleteFile = File(videoFilePath)
                        deleteFile.delete()
                        MediaScannerConnection.scanFile(
                            applicationContext,
                            arrayOf(outPutFilePath),
                            arrayOf("video/mp4"),
                        ) { _: String, uri: Uri? ->
                            if (uri == null) {
                                handler.post {
                                    Toast.makeText(
                                        applicationContext,
                                        "media scan failed...",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    RxBus.publish(RxEvent.ShowProgress(false))
                                }
                            } else {
                                handler.post {
                                    Toast.makeText(
                                        applicationContext,
                                        "Downloaded successfully",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    RxBus.publish(RxEvent.ShowProgress(false))
                                }
                            }
                        }
                    }
                    255 -> {
                        handler.post {
                            Toast.makeText(
                                applicationContext,
                                "Please try again!!",
                                Toast.LENGTH_LONG
                            ).show()
                            RxBus.publish(RxEvent.ShowProgress(false))
                        }
                    }
                    else -> {
                        // Failed case:
                        // line 489 command fails on some devices in
                        // that case retrying with accurateCmt as alternative command
                        handler.post {
                            Toast.makeText(
                                applicationContext,
                                "Please try again!!",
                                Toast.LENGTH_LONG
                            ).show()
                            RxBus.publish(RxEvent.ShowProgress(false))
                        }
                    }
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cancel()
        }
    }

    private fun execFFmpegBinaryForDownloadVideo(
        command: Array<String>,
        downloadFilePath: String
    ) {
        try {
            val handler = Handler(Looper.getMainLooper())
            Thread {
                when (FFmpeg.execute(command)) {
                    0 -> {
                        MediaScannerConnection.scanFile(
                            applicationContext,
                            arrayOf(downloadFilePath),
                            arrayOf("video/mp4"),
                        ) { _: String, uri: Uri? ->
                            prepareVideoFile(downloadFilePath)
                            if (uri == null) {
                                handler.post {
                                    Toast.makeText(
                                        applicationContext,
                                        "media scan failed...",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                            }
                        }
                    }
                    255 -> {
                        handler.post {
                            Toast.makeText(
                                applicationContext,
                                "Something went wrong",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        RxBus.publish(RxEvent.ShowProgress(false))
                    }
                    else -> {
                        // Failed case:
                        // line 489 command fails on some devices in
                        // that case retrying with accurateCmt as alternative command
                        handler.post {
                            Toast.makeText(
                                applicationContext,
                                "Something went wrong",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        RxBus.publish(RxEvent.ShowProgress(false))
                    }
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cancel()
        }
    }

    private fun cancel() {
        notificationManager.cancel(0)
        stopSelf()
    }
}