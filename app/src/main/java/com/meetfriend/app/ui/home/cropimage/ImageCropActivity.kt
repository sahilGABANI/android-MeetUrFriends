package com.meetfriend.app.ui.home.cropimage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.meetfriend.app.R
import com.meetfriend.app.databinding.ActivityCropImageBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.newbase.view.cropper.CropImageView
import timber.log.Timber
import java.io.File

class ImageCropActivity : BasicActivity(), CropImageView.OnCropImageCompleteListener {

    companion object {
        const val INTENT_EXTRA_FILE_PATH = "INTENT_EXTRA_FILE_PATH"
        fun getIntent(context: Context, filePath: String): Intent {
            val intent = Intent(context, ImageCropActivity::class.java)
            intent.putExtra(INTENT_EXTRA_FILE_PATH, filePath)
            return intent
        }
    }

    private lateinit var binding: ActivityCropImageBinding
    private var selectedFilePath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCropImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        loadDataFromIntent()
    }

    private fun initViews() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.ivDone.throttleClicks().subscribeAndObserveOnMainThread {
            val uri = binding.cropImageView.imageUri
            if (uri != null) {
                if (uri.toString().isNotEmpty()) {
                    val file = File(
                        filesDir,
                        System.currentTimeMillis().toString().plus(File(selectedFilePath).extension)
                    )
                    binding.cropImageView.saveCroppedImageAsync(Uri.fromFile(file))
                } else {
                    showToast(getString(R.string.msg_can_not_create_destination_file))
                }
            } else {
                showToast(getString(R.string.msg_can_not_create_destination_file))
            }
        }.autoDispose()

        binding.cropImageView.setOnCropImageCompleteListener(this)
    }

    private fun loadDataFromIntent() {
        intent?.let {
            val filePath = it.getStringExtra(INTENT_EXTRA_FILE_PATH)
            if (!filePath.isNullOrEmpty()) {
                selectedFilePath = filePath
                loadPhoto()
            } else {
              onBackPressedDispatcher.onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun loadPhoto() {
        binding.apply {
            cropImageView.clearAspectRatio()
            cropImageView.resetCropRect()
            cropImageView.setAspectRatio(1, 1)
            val file = File(selectedFilePath)
            cropImageView.setImageUriAsync(Uri.fromFile(file))
        }
    }

    override fun onCropImageComplete(view: CropImageView?, result: CropImageView.CropResult?) {
        result?.uri?.path?.let {
            val filePath = File(it).path
            Timber.tag("<><>").e(filePath)
            val intent = Intent()
            intent.putExtra(INTENT_EXTRA_FILE_PATH, filePath)
            setResult(Activity.RESULT_OK, intent)
            finish()
        } ?: showToast(getString(R.string.msg_can_not_create_destination_file))
    }
}