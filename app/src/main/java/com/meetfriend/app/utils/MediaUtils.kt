package com.meetfriend.app.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.Looper
import com.arthenica.mobileffmpeg.FFmpeg
import timber.log.Timber
import java.io.File

class MediaUtils {
    object Utils {
        fun getConvertedFile(directoryPath: String, fileName: String): File {
            val directory = File(directoryPath)
            if (!directory.exists()) {
                directory.mkdirs()
            }


            return File(directory, fileName)
        }
    }

    companion object {
        fun mergeVideoAndAudio(context: Context, videoPath: String, audioPath: String): String? {
            var newVideoPath: String? = null
            val dire: File = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MOVIES)
            val outputFileName = "merge_video_file.mp4"
            val outgoerDir = File(dire, "Meeturfriends")
            Timber.tag("VideoTrim").d("Movies/Meeturfriends: %s", outgoerDir.exists())
            Timber.tag("VideoTrim").d("Movies/Meeturfriends: %s", outgoerDir.path)
            if (outgoerDir.exists()) {
                val make = outgoerDir.mkdirs()
                Timber.tag("VideoTrim").d("Dire Created: %s", make)
                MediaScannerConnection.scanFile(
                    context, arrayOf<String>(outgoerDir.path), null
                ) { path, uri ->
                    Timber.tag("VideoTrim").i("Scanned $path:")
                    Timber.tag("VideoTrim").i( "-> uri=$uri")
//                        videoPath  = it.thumbnail
                }
                val filePath = outgoerDir.let {
                    Utils.getConvertedFile(
                        it.absolutePath, outputFileName
                    )
                }
                Timber.tag("VideoTrim").d("filePath :$filePath")
            }

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

            Timber.tag("VideoTrim").d("DIRECTORY_DOWNLOADS: %s", dir?.exists())
            if (dir?.exists() == false) {
                val make = dir.mkdir()
                Timber.tag("VideoTrim").d("Dire Created: %s", make)
            }
            val mp4UriAfterTrim = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                Utils.getConvertedFile(
                    it.absolutePath, outputFileName
                )
            }

//        Timber.tag("VideoTrim").d("result result: %s", result)
            Timber.tag("VideoTrim").d("result: %s", mp4UriAfterTrim?.path)
            Timber.tag("VideoTrim").d("result: %s", mp4UriAfterTrim?.path?.let { File(it).exists() })

            Handler(Looper.getMainLooper()).postDelayed({
                val cmd = arrayOf(
                    "-y", "-i", videoPath, "-i", audioPath, "-c:v", "copy", "-c:a", "aac", "-map", "0:v:0", "-map", "1:a:0", "-shortest", mp4UriAfterTrim?.path
                )
                Thread {
                    val result: Int = FFmpeg.execute(cmd)
                    Timber.tag("VideoTrim").d("result: %s", result)
                    if (result == 0) {
                        Timber.tag("VideoTrim").i("result: Success")
                        newVideoPath = mp4UriAfterTrim?.path.toString()
                        Timber.tag("VideoTrim").i("videoPath: $newVideoPath")
                        Timber.tag("VideoTrim").i("videoPath: ${mp4UriAfterTrim?.path}")

                    } else if (result == 255) {
                        Timber.tag("VideoTrim").d("result: Canceled")
                    } else {
                        Timber.tag("VideoTrim").e("result: Failed")
                    }
                }.start()
            }, 10)
            return newVideoPath
        }
    }

}