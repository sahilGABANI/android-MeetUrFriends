package com.meetfriend.app.ui.dual

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.meetfriend.app.R
import com.meetfriend.app.api.cloudflare.model.CloudFlareConfig
import com.meetfriend.app.api.cloudflare.model.StoryUploadData
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.api.story.model.AddStoryRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.application.MeetFriendApplication.Companion.context
import com.meetfriend.app.databinding.ActivityDualCameraBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.*
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewModel
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewState
import com.meetfriend.app.ui.main.MainHomeActivity
import com.otaliastudios.cameraview.*
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*
import javax.inject.Inject

class DualCameraActivity : BasicActivity() {
    companion object {
        var IS_SHORTS = "IS_SHORTS"
        var IS_CHALLENGE = "IS_CHALLENGE"
        var IS_STORY = "IS_STORY"

        fun getIntent(
            context: Context,
            isShorts: Boolean = false,
            isChallenge: Boolean = false,
            isStory: Boolean = false,
        ): Intent {
            val intent = Intent(context, DualCameraActivity::class.java)
            intent.putExtra(IS_SHORTS, isShorts)
            intent.putExtra(IS_CHALLENGE, isChallenge)
            intent.putExtra(IS_STORY, isStory)
            return intent
        }
    }

    private lateinit var binding: ActivityDualCameraBinding
    private var countDownTimer: CountDownTimer? = null
    private var hiddenCountDownTimer: CountDownTimer? = null
    private var hiddenRecordingTimeSeconds: Int = 0
    private var recordingTimeSeconds: Int = 0
    private var cameraIconTint: ColorStateList? = null
    private var backFile: File? = null
    private var frontFile: File? = null
    private var isAudioMute: Boolean = false
    private var isChallenge: Boolean = false
    private var cloudFlareConfig: CloudFlareConfig? = null
    private var isShorts: Boolean = false
    private var isStory: Boolean = false
    private var pausedTimes: MutableList<Int> = mutableListOf()
    private var resumedTimes: MutableList<Int> = mutableListOf()
    private var duration: Long = 0L

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddNewPostViewModel>
    private lateinit var deepArViewModel: AddNewPostViewModel
    private var CAMERA_MODE = 2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDualCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)


        MeetFriendApplication.component.inject(this)
        deepArViewModel = getViewModelFromFactory(viewModelFactory)
        isShorts = intent?.getBooleanExtra(IS_SHORTS, false) ?: false
        isChallenge = intent?.getBooleanExtra(IS_CHALLENGE, false) ?: false
        isStory = intent?.getBooleanExtra(IS_STORY, false) ?: false
        if (!checkPermissions()) {
            requestPermissions()
        }
        listenToViewModel()
        if (isOnline(this) && isStory) {
            deepArViewModel.getCloudFlareConfig(false)
        } else if (isStory) {
            showToast("No internet connection")
        }

        binding.apply {
            backCameraView.setLifecycleOwner(this@DualCameraActivity)
            backCameraView.useDeviceOrientation = true
            backCameraView.addCameraListener(backCameraListener)
            backCameraView.mode = Mode.VIDEO
            backCameraView.facing = Facing.BACK

            frontCameraView.setLifecycleOwner(this@DualCameraActivity)
            frontCameraView.useDeviceOrientation = true
            frontCameraView.addCameraListener(frontCameraListener)
            frontCameraView.mode = Mode.VIDEO
            frontCameraView.facing = Facing.FRONT

            frontCameraView.addCameraListener(object : CameraListener() {
                override fun onCameraError(exception: CameraException) {
                    CAMERA_MODE = 1
                    frontCameraView.close()
                    frontCameraView.removeCameraListener(frontCameraListener)
                    showToast("dual camera is not supported in your device.")
                    finish()
                }

                override fun onCameraOpened(options: CameraOptions) {
                }

                override fun onCameraClosed() {
                    frontCameraView.close()
                    frontCameraView.removeCameraListener(frontCameraListener)
                }
            })

            Handler(Looper.getMainLooper()).postDelayed({
                updateCameraUI()
            }, 2000)


            captureBtn.throttleClicks().subscribeAndObserveOnMainThread {
                if (backCameraView.isTakingVideo) {
                    cameraIconTint =
                        ContextCompat.getColorStateList(
                            this@DualCameraActivity,
                            android.R.color.white
                        )
                    // Stop video recording if already recording
                    backCameraView.stopVideo()
                    if (CAMERA_MODE == 2) frontCameraView.stopVideo()
                    stopTimer()
                    hiddenCountDownTimer?.cancel()
                    captureBtn.imageTintList = cameraIconTint
                } else {
                    cameraIconTint = ContextCompat.getColorStateList(
                        this@DualCameraActivity,
                        android.R.color.holo_red_light
                    )
                    val backFile = File(cacheDir, "${System.currentTimeMillis()}_${generateRandomString(2)}" + ".mp4")
                    val frontFile = File(cacheDir, "${System.currentTimeMillis()}_${generateRandomString(2)}" + "_" + ".mp4")
                    backCameraView.takeVideo(backFile)
                    if (CAMERA_MODE == 2) frontCameraView.takeVideo(frontFile)
                    Handler(Looper.getMainLooper()).postDelayed({
                        hiddenStartTimer()
                        startTimer()
                    }, 2000)
                    runOnUiThread {
                        captureBtn.imageTintList = cameraIconTint
                    }
                }
            }.autoDispose()

            backCamera.setOnClickListener {
                CAMERA_MODE = 1
                updateCameraUI()
            }

        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            CameraView.PERMISSION_REQUEST_CODE
        )
    }


    private fun listenToViewModel() {
        deepArViewModel.addNewPostState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddNewPostViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }

                is AddNewPostViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressedDispatcher.onBackPressed()
                }

                is AddNewPostViewState.UploadImageCloudFlareSuccess -> {
                    val addStoryRequest = AddStoryRequest(
                        type = "image",
                        image = it.imageUrl
                    )
                    deepArViewModel.addStory(addStoryRequest)
                }

                is AddNewPostViewState.StoryUploadSuccess -> {
                    hideLoading()
                    showLongToast(it.successMessage)
                    onBackPressedDispatcher.onBackPressed()
                }

                else -> {}
            }
        }
    }


    private val backCameraListener: CameraListener = object : CameraListener() {
        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            backFile = result.file

            if (CAMERA_MODE == 2) {
                mergeVideoFiles()
            } else if (pausedTimes.isNotEmpty() && CAMERA_MODE != 2) {
                binding.progressBar.visibility = View.VISIBLE
                binding.captureBtn.isClickable = false
                backFile?.let { backFile -> trimAndConcatenateVideos(backFile) }
            } else {
                displayCapturedVideo(result.file)
            }
        }

        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
            // Handle back camera errors
            // Restart the camera engine in response to the error
        }
    }

    private val frontCameraListener: CameraListener = object : CameraListener() {
        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            frontFile = result.file
            if (CAMERA_MODE == 2) {
                mergeVideoFiles()
            } else {
                displayCapturedVideo(result.file)
            }
        }

        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
            // Handle front camera errors
            // Restart the camera engine in response to the error
        }
    }

    private fun trimAndConcatenateVideos(editFile: File) {
        val outputFile =
            File(cacheDir, "final_${System.currentTimeMillis()}_${generateRandomString(2)}.mp4")
        val videoSegments = mutableListOf<File>()
        val originalVideoFile = editFile ?: return
        var startTime = 0
        for (i in 0 until minOf(pausedTimes.size, resumedTimes.size)) {
            val pauseTime = pausedTimes[i]
            val resumeTime = resumedTimes[i]
            val segmentFile = File(cacheDir, "segment_${i}_${generateRandomString(2)}.mp4")
            trimVideoSegment(originalVideoFile, segmentFile, startTime, pauseTime)
            videoSegments.add(segmentFile)
            startTime = resumeTime
        }
        val finalSegmentFile = File(cacheDir, "final_segment_${generateRandomString(2)}.mp4")
        trimVideoSegment(originalVideoFile, finalSegmentFile, startTime, Int.MAX_VALUE)
        videoSegments.add(finalSegmentFile)
        concatenateVideos(outputFile, *videoSegments.toTypedArray())
        displayCapturedVideo(outputFile)
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                recordingTimeSeconds++
            }

            override fun onFinish() {
                // Not required for recording timer
            }
        }.start()
    }


    private fun stopTimer() {
        countDownTimer?.cancel()
    }

    private fun concatenateVideos(outputFile: File, vararg inputFiles: File) {
        val listFile = File.createTempFile("list", ".txt")
        listFile.deleteOnExit()
        val writer = BufferedWriter(FileWriter(listFile))
        inputFiles.forEach { writer.write("file '${it.absolutePath}'\n") }
        writer.close()
        val ffmpegCommand = arrayOf(
            "-y",
            "-f", "concat",
            "-safe", "0",
            "-i", listFile.absolutePath,
            "-c", "copy",
            outputFile.absolutePath
        )
        val result = FFmpeg.execute(ffmpegCommand)
        if (result != Config.RETURN_CODE_SUCCESS) {
            Toast.makeText(this@DualCameraActivity, resources.getString(R.string.network_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCameraUI() {
        with(binding) {
            when (CAMERA_MODE) {
                1 -> {

                    backCameraView.close()
                    backCameraView.removeCameraListener(backCameraListener)
                    frontCameraView.close()
                    frontCameraView.removeCameraListener(frontCameraListener)
                    frontCameraView.visibility = View.GONE

                    backCameraView.setLifecycleOwner(this@DualCameraActivity)
                    backCameraView.useDeviceOrientation = true
                    backCameraView.addCameraListener(backCameraListener)
                    backCameraView.mode = Mode.VIDEO
                    backCameraView.facing = Facing.BACK

                    backCameraView.addCameraListener(object : CameraListener() {
                        override fun onCameraError(exception: CameraException) {
                        }

                        override fun onCameraOpened(options: CameraOptions) {
                        }

                        override fun onCameraClosed() {
                        }
                    })
                }

                2 -> {
                    backCameraView.close()
                    backCameraView.removeCameraListener(backCameraListener)
                    frontCameraView.close()
                    frontCameraView.removeCameraListener(frontCameraListener)

                    frontCameraView.setLifecycleOwner(this@DualCameraActivity)
                    frontCameraView.useDeviceOrientation = true
                    frontCameraView.addCameraListener(frontCameraListener)
                    frontCameraView.mode = Mode.VIDEO

                    frontCameraView.facing = Facing.FRONT
                    frontCameraView.visibility = View.VISIBLE

                    backCameraView.setLifecycleOwner(this@DualCameraActivity)
                    backCameraView.useDeviceOrientation = true
                    backCameraView.addCameraListener(backCameraListener)
                    backCameraView.mode = Mode.VIDEO
                    backCameraView.facing = Facing.BACK

                    backCameraView.addCameraListener(object : CameraListener() {
                        override fun onCameraError(exception: CameraException) {
                        }

                        override fun onCameraOpened(options: CameraOptions) {
                        }

                        override fun onCameraClosed() {
                        }
                    })

                    frontCameraView.addCameraListener(object : CameraListener() {
                        override fun onCameraError(exception: CameraException) {
                        }

                        override fun onCameraOpened(options: CameraOptions) {
                        }

                        override fun onCameraClosed() {
                        }
                    })
                }

                else -> {
                    backCameraView.close()
                    backCameraView.removeCameraListener(backCameraListener)
                    frontCameraView.close()
                    frontCameraView.removeCameraListener(frontCameraListener)
                    frontCameraView.visibility = View.GONE

                    backCameraView.setLifecycleOwner(this@DualCameraActivity)
                    backCameraView.useDeviceOrientation = true
                    backCameraView.addCameraListener(backCameraListener)
                    backCameraView.mode = Mode.VIDEO
                    backCameraView.facing = Facing.BACK

                    backCameraView.addCameraListener(object : CameraListener() {
                        override fun onCameraError(exception: CameraException) {
                        }

                        override fun onCameraOpened(options: CameraOptions) {
                        }

                        override fun onCameraClosed() {
                        }
                    })
                }
            }
        }
    }


    private fun mergeVideoFiles() {
        binding.progressBar.visibility = View.VISIBLE
        binding.captureBtn.isClickable = false
        if (backFile != null && frontFile != null) {
            val cacheDir = context.cacheDir // Get the cache directory
            val dirPathFile = File(cacheDir, "temp_video") // Create or access the "temp_video" subdirectory

            if (!dirPathFile.exists()) {
                dirPathFile.mkdirs() // Create the subdirectory if it doesn't exist
            }

// Create the video file with a unique name using timestamp
            val mergedVideoFile = File(dirPathFile, "merged_video_${System.currentTimeMillis()}.mp4")

            val ffmpegCmd = if (isAudioMute) {
                arrayOf(
                    "-i", backFile?.absolutePath,
                    "-i", frontFile?.absolutePath,
                    "-filter_complex",
                    "[1]scale=320:240 [ovrl]; [0][ovrl] overlay=x=0:y=0",
                    "-map", "[out]",
                    "-an", // Disable all audio streams
                    mergedVideoFile.absolutePath
                )
            } else {
                arrayOf(
                    "-i", backFile?.absolutePath,
                    "-i", frontFile?.absolutePath,
                    "-filter_complex",
                    "[1]scale=320:240 [ovrl]; [0][ovrl] overlay=x=0:y=0",
                    "-map", "[out]",
                    "-map", "0:a", // Map the audio stream from the original video file
                    "-c:a", "copy", // Copy the original audio stream
                    mergedVideoFile.absolutePath
                )
            }

            Toast.makeText(this@DualCameraActivity, resources.getString(R.string.please_wait), Toast.LENGTH_LONG).show()
            Thread {
                val result: Int = FFmpeg.execute(ffmpegCmd)
                if (result == 0) {
                    runOnUiThread {
                        if (pausedTimes.isNotEmpty()) {
                            trimAndConcatenateVideos(mergedVideoFile.absoluteFile)
                        } else {
                            displayCapturedVideo(mergedVideoFile.absoluteFile)
                        }
                    }
                } else if (result == 255) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        onBackPressedDispatcher.onBackPressed()
                    }
                } else {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }.start()
        } else {
            if (backFile == null) {
                binding.progressBar.visibility = View.GONE
                onBackPressedDispatcher.onBackPressed()
                Toast.makeText(this@DualCameraActivity, resources.getString(R.string.network_error), Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun displayCapturedVideo(videoFile: File) {
        Toast.makeText(this@DualCameraActivity, "VideoFile, displayCapturedVideo() -> videoFile: $videoFile", Toast.LENGTH_LONG).show()
        runOnUiThread {
            binding.captureBtn.isClickable = true
            binding.progressBar.visibility = View.GONE

            // Create the directory Dabadu in the Movies directory
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

            val savedFile = File(dirPathFile, "${videoFile.name.substringBefore(".mp4")}_${generateRandomString(2)}.mp4")
            // Save the video file in the Dabadu directory
            savedFile.exists()
            savedFile.mkdir()
            if (!savedFile.exists()) {
                if (!savedFile.mkdir()) {
                }
            }
            MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(savedFile.absolutePath),
                arrayOf("video/mp4"),
            ) { path: String, uri: Uri? ->
                if (uri == null) {
                    Toast.makeText(this@DualCameraActivity, "media scan failed...", Toast.LENGTH_LONG).show()
                } else {
                    openEditor(path)
                }
            }
        }
    }

    private fun openEditor(inputImage: String) {
        if (isChallenge) {
            val intent = Intent()
            intent.putExtra("FILE_PATH", inputImage)
            setResult(RESULT_OK, intent)
            finish()
        } else if (inputImage.toString().contains("png")) {
            if (isStory) {
                showLoading(true)
                Timber.tag("StoryOpenEditor").d("cloudFlareConfig: $cloudFlareConfig")
                cloudFlareConfig?.let {
                    deepArViewModel.uploadImageToCloudFlare(this, it, File(inputImage))
                } ?: deepArViewModel.getCloudFlareConfig(false)
            } else {
                Timber.tag("openEditor").d("inputImage: $inputImage & filePath: $inputImage")
                RxBus.publish(RxEvent.CreatePost(AddNewPostInfoActivity.POST_TYPE_IMAGE, inputImage, Uri.EMPTY))
                finish()
            }
        } else if (inputImage.toString().lowercase().contains("mp4")
            || inputImage.toString().lowercase().contains("mov")
            || inputImage.toString().lowercase().contains("MKV")
        ) {
            showLoading(true)
            compressVideo(inputImage)
        } else {
            val intent = AddNewPostInfoActivity.launchActivity(
                LaunchActivityData(this@DualCameraActivity,
                postType = AddNewPostInfoActivity.POST_TYPE_IMAGE,
                imagePathList = arrayListOf(inputImage),
                tagName = "",
                videoUri = null
                )
            )
            startActivity(intent)
        }
    }


    private fun compressVideo(videoPath: String) {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            this.resources.getString(R.string.application_name)
        )
        val videoUris = listOf(Uri.fromFile(File(videoPath)))
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        duration =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                ?: 1000L
        lifecycleScope.launch {
            VideoCompressor.start(
                context = applicationContext,
                videoUris,
                isStreamable = false,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies,
                    subFolderName = resources.getString(R.string.application_name)
                ),
                configureWith = com.abedelazizshe.lightcompressorlibrary.config.Configuration(
                    quality = VideoQuality.HIGH,
                    videoNames = videoUris.map { uri -> uri.pathSegments.last() },
                    isMinBitrateCheckEnabled = false,
                ),
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {

                    }

                    override fun onStart(index: Int) {
                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        hideLoading()
                        if (isShorts) {
                            val intent = AddNewPostInfoActivity.launchActivity(
                                LaunchActivityData(this@DualCameraActivity,
                                postType = if (isShorts) AddNewPostInfoActivity.POST_TYPE_VIDEO else AddNewPostInfoActivity.POST_TYPE_POST_VIDEO,
                                videoPath = videoPath,
                                tagName = "",
                                videoUri = null
                                )
                            )
                            startActivity(intent)
                        } else if (isStory) {
                            if (isOnline(this@DualCameraActivity)) {
                                if (path.toString().isNotEmpty()) {
                                    cloudFlareConfig?.let {
                                        startActivity(
                                            MainHomeActivity.getIntentFromStoryUpload(
                                                this@DualCameraActivity,
                                                StoryUploadData(
                                                    cloudFlareConfig ?: CloudFlareConfig(),
                                                    path.toString(),
                                                    (duration / 1000).toInt().toString(),
                                                    linkAttachmentDetails = null,
                                                    null,
                                                    null,
                                                    null
                                                )
                                            )
                                        )
                                    } ?: deepArViewModel.getCloudFlareConfig(false)
                                }
                            }
                        } else {
                            RxBus.publish(RxEvent.CreatePost(AddNewPostInfoActivity.POST_TYPE_POST_VIDEO, videoPath, Uri.EMPTY))
                            finish()

                        }
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        Timber.wtf(failureMessage)
                        hideLoading()
                    }

                    override fun onCancelled(index: Int) {
                        Timber.wtf("compression has been cancelled")
                        hideLoading()
                    }
                },
            )
        }
    }


    private fun trimVideoSegment(inputFile: File, outputFile: File, startTime: Int, endTime: Int) {
        val ffmpegCommand = arrayOf(
            "-y",
            "-i", inputFile.absolutePath,
            "-ss", startTime.toString(),
            "-to", endTime.toString(),
            "-c", "copy",
            outputFile.absolutePath
        )
        val result = FFmpeg.execute(ffmpegCommand)
        if (result != Config.RETURN_CODE_SUCCESS) {
            Toast.makeText(this@DualCameraActivity, resources.getString(R.string.network_error), Toast.LENGTH_SHORT).show()
        }
    }

    fun generateRandomString(length: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { _ -> Random().nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun hiddenStartTimer() {
        hiddenCountDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                hiddenRecordingTimeSeconds++
            }

            override fun onFinish() {
                // Not required for recording timer
            }
        }.start()
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return true
                }
            }
        }
        return false
    }
}