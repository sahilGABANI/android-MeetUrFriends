package com.meetfriend.app.ui.activities

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.meetfriend.app.R
import com.meetfriend.app.databinding.ActivityFullScreenBinding
import com.meetfriend.app.utils.blurview.FKBlurView
import contractorssmart.app.utilsclasses.CommonMethods

class FullScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val url = intent.extras!!.getString("url", "")
        CommonMethods.setImage(binding.photoView, url)

        Glide.with(this@FullScreenActivity)
            .load(url)
            .placeholder(R.drawable.image_placeholder)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    val blurView = findViewById<FKBlurView>(R.id.fkBlurView)
                    blurView.setBlur(this@FullScreenActivity, blurView, 80)
                    return false
                }
            })
            .into(binding.ivBackProfile)
        binding.closeActivity.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
