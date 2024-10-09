package com.meetfriend.app.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.meetfriend.app.R
import com.meetfriend.app.databinding.ActivityVideoPlayBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.videoplayer.JzvdStdOutgoer


class VideoPlayActivity : BasicActivity() {

    lateinit var binding: ActivityVideoPlayBinding
    var path = ""

    companion object {
        val PATH = "path"

        fun getIntent(context: Context, url: String): Intent {
            val intent = Intent(context, VideoPlayActivity::class.java)
            intent.putExtra(PATH, url)
            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVideoPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        path = intent.extras!!.getString(PATH, "")
        autoPlayVideo()

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
          onBackPressedDispatcher.onBackPressed()
        }.autoDispose()
    }

    private fun autoPlayVideo() {

        val player: JzvdStdOutgoer = findViewById(R.id.exoPlayer)
        player.videoUrl = path
        player.apply {
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_NORMAL,
            )
            startVideoAfterPreloading()
        }
    }

    override fun onResume() {
        super.onResume()
        Jzvd.goOnPlayOnResume()

    }

    override fun onPause() {
        super.onPause()
        Jzvd.goOnPlayOnPause()

    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }
}