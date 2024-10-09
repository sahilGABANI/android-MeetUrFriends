package com.meetfriend.app.ui.home.create.croppostimages.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewCropPostImagesBinding

class CropPostImagesView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: ViewCropPostImagesBinding

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_crop_post_images, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding = ViewCropPostImagesBinding.bind(view)
    }

    fun bind(filePath: String) {
        binding.apply {
            Glide.with(context)
                .load(filePath)
                .placeholder(R.color.colorPrimary)
                .into(binding.ivPostImage)
        }
    }
}