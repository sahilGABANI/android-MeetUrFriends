package com.meetfriend.app.ui.home.shorts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.meetfriend.app.databinding.ActivityPostCameraBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant.FiXED_1000_MILLISECOND
import com.meetfriend.app.utils.Constant.FiXED_3600_INT
import com.meetfriend.app.utils.Constant.FiXED_60_INT
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import java.io.File
import java.util.*

class PostCameraActivity : BasicActivity() {

    companion object {
        const val RC_CAPTURE_PICTURE = 10001
        const val INTENT_EXTRA_IS_CAPTURE_PHOTO = "INTENT_EXTRA_IS_CAPTURE_PHOTO"
        const val INTENT_EXTRA_FILE_PATH = "INTENT_EXTRA_FILE_PATH"
        fun launchActivity(context: Context, isCapturePhoto: Boolean): Intent {
            val intent = Intent(context, PostCameraActivity::class.java)
            intent.putExtra(INTENT_EXTRA_IS_CAPTURE_PHOTO, isCapturePhoto)
            return intent
        }
    }

    private lateinit var binding: ActivityPostCameraBinding
    private var isCapturePhoto: Boolean = true
    private var seconds = 0
    private var handler: Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        isCapturePhoto = intent?.getBooleanExtra(INTENT_EXTRA_IS_CAPTURE_PHOTO, true) ?: true

        binding.cameraView.setLifecycleOwner(this)
        binding.cameraView.useDeviceOrientation = true
        binding.cameraView.addCameraListener(cameraListener)

        if (isCapturePhoto) {
            binding.cameraView.mode = Mode.PICTURE
            binding.ivCapturePhoto.visibility = View.VISIBLE
            binding.ivStartVideoRecord.visibility = View.GONE
            binding.ivStopVideoRecord.visibility = View.GONE
            binding.secondsAppCompatTextView.visibility = View.GONE
        } else {
            binding.cameraView.mode = Mode.VIDEO
            binding.ivCapturePhoto.visibility = View.GONE
            binding.ivStartVideoRecord.visibility = View.VISIBLE
            binding.ivStopVideoRecord.visibility = View.GONE
            binding.secondsAppCompatTextView.visibility = View.VISIBLE
        }

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.ivFlipCamera.throttleClicks().subscribeAndObserveOnMainThread {
            when (binding.cameraView.facing) {
                Facing.BACK -> binding.cameraView.facing = Facing.FRONT
                Facing.FRONT -> binding.cameraView.facing = Facing.BACK
            }
        }.autoDispose()

        binding.ivCapturePhoto.throttleClicks().subscribeAndObserveOnMainThread {
            binding.cameraView.takePictureSnapshot()
        }.autoDispose()

        binding.ivStartVideoRecord.throttleClicks().subscribeAndObserveOnMainThread {
            val videoFile = File(cacheDir, System.currentTimeMillis().toString().plus(".mp4"))
            videoFile.let {
                handleTimerEvent()
                binding.cameraView.takeVideoSnapshot(it)
            }
        }.autoDispose()

        binding.ivStopVideoRecord.throttleClicks().subscribeAndObserveOnMainThread {
            binding.cameraView.stopVideo()
        }.autoDispose()
    }

    private fun handleTimerEvent() {
        handler = Handler(Looper.getMainLooper())
        handler?.post(object : Runnable {
            override fun run() {
                val hours: Int = seconds / FiXED_3600_INT
                val minutes: Int = seconds % FiXED_3600_INT / FiXED_60_INT
                val secs: Int = seconds % FiXED_60_INT
                val time: String = java.lang.String.format(
                    Locale.getDefault(),
                    "%d:%02d:%02d",
                    hours,
                    minutes,
                    secs
                )
                binding.secondsAppCompatTextView.text = time
                seconds++
                handler?.postDelayed(this, FiXED_1000_MILLISECOND)
            }
        })
    }

    private val cameraListener: CameraListener = object : CameraListener() {

        override fun onVideoRecordingStart() {
            super.onVideoRecordingStart()
            binding.ivClose.visibility = View.INVISIBLE

            binding.ivFlipCamera.visibility = View.INVISIBLE
            binding.ivStartVideoRecord.visibility = View.GONE
            binding.ivStopVideoRecord.visibility = View.VISIBLE
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)

            binding.ivClose.visibility = View.VISIBLE

            binding.ivFlipCamera.visibility = View.VISIBLE
            binding.ivStartVideoRecord.visibility = View.VISIBLE
            binding.ivStopVideoRecord.visibility = View.GONE

            binding.secondsAppCompatTextView.text = "00:00"
            seconds = 0
            handler = null

            val videoPath = result.file.absolutePath
            if (!videoPath.isNullOrEmpty()) {
                returnResult(videoPath)
            }
        }

        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            result.toFile(
                File(
                    cacheDir,
                    System.currentTimeMillis().toString().plus(".jpg")
                )
            ) { photoFile ->
                if (photoFile != null) {
                    returnResult(photoFile.absolutePath)
                }
            }
        }
    }

    private fun returnResult(absolutePath: String) {
        val intent = Intent()
        intent.putExtra(INTENT_EXTRA_IS_CAPTURE_PHOTO, isCapturePhoto)
        intent.putExtra(INTENT_EXTRA_FILE_PATH, absolutePath)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler = null
        binding.cameraView.removeCameraListener(cameraListener)
    }
}
