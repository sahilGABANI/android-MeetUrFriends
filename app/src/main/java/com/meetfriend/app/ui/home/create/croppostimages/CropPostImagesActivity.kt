package com.meetfriend.app.ui.home.create.croppostimages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.meetfriend.app.api.post.model.LaunchActivityData
import com.meetfriend.app.databinding.ActivityCropPostImagesBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.startActivityForResultWithDefaultAnimation
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.create.croppostimages.view.CropPostImagesAdapter
import com.meetfriend.app.ui.home.cropimage.ImageCropActivity

class CropPostImagesActivity : BasicActivity() {

    companion object {
        private const val INTENT_EXTRA_IMAGE_PATH_LIST = "INTENT_EXTRA_IMAGE_PATH_LIST"
        private const val REQUEST_CODE_CROP_POST_IMAGES = 10001
        fun getIntent(context: Context, imagePathList: ArrayList<String>): Intent {
            val intent = Intent(context, CropPostImagesActivity::class.java)
            intent.putStringArrayListExtra(INTENT_EXTRA_IMAGE_PATH_LIST, imagePathList)
            return intent
        }
    }

    private lateinit var binding: ActivityCropPostImagesBinding
    private lateinit var cropPostImagesAdapter: CropPostImagesAdapter

    private var imagePathList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCropPostImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            val imagePathList = it.getStringArrayListExtra(INTENT_EXTRA_IMAGE_PATH_LIST)
            if (!imagePathList.isNullOrEmpty()) {
                this.imagePathList = imagePathList
                listenToViewEvents()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun listenToViewEvents() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.ivCrop.throttleClicks().subscribeAndObserveOnMainThread {
            val intent = ImageCropActivity.getIntent(
                this,
                imagePathList[binding.viewPager2.currentItem]
            )
            startActivityForResultWithDefaultAnimation(intent, REQUEST_CODE_CROP_POST_IMAGES)
        }.autoDispose()

        binding.ivDone.throttleClicks().subscribeAndObserveOnMainThread {
            val intent = AddNewPostInfoActivity.launchActivity(
                LaunchActivityData(
                    this,
                    postType = AddNewPostInfoActivity.POST_TYPE_IMAGE,
                    imagePathList = imagePathList
                )
            )
            startActivity(intent)
        }.autoDispose()

        cropPostImagesAdapter = CropPostImagesAdapter(this)
        binding.viewPager2.adapter = cropPostImagesAdapter
        cropPostImagesAdapter.listOfDataItems = imagePathList
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CROP_POST_IMAGES) {
                data?.let {
                    if (it.hasExtra(ImageCropActivity.INTENT_EXTRA_FILE_PATH)) {
                        val croppedImageFilePath =
                            it.getStringExtra(ImageCropActivity.INTENT_EXTRA_FILE_PATH)
                        if (!croppedImageFilePath.isNullOrEmpty()) {
                            val currentItem = binding.viewPager2.currentItem
                            imagePathList[currentItem] = croppedImageFilePath
                            cropPostImagesAdapter.listOfDataItems = imagePathList
                            binding.viewPager2.currentItem = currentItem
                        }
                    }
                }
            }
        }
    }
}
