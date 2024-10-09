package com.meetfriend.app.ui.main.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.meetfriend.app.R
import com.meetfriend.app.api.post.model.PostMediaInformation
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewHomePagePostMediaBinding
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HomePagePostMediaView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val playVideoSubject: PublishSubject<Unit> = PublishSubject.create()
    val playVideo: Observable<Unit> = playVideoSubject.hide()

    private val mediaPhotoViewClickSubject: PublishSubject<String> = PublishSubject.create()
    val mediaPhotoViewClick: Observable<String> = mediaPhotoViewClickSubject.hide()

    private val mediaVideoViewClickSubject: PublishSubject<String> = PublishSubject.create()
    val mediaVideoViewClick: Observable<String> = mediaVideoViewClickSubject.hide()

    private lateinit var postImage: PostMediaInformation
    private var position: Int = 0

    private var binding: ViewHomePagePostMediaBinding? = null
    private val colorDrawable = ColorDrawable(Color.parseColor("#616161"))

    init {
        inflateUi()
    }

    @SuppressLint("ClickableViewAccessibility", "LogNotTimber")
    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_home_page_post_media, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding = ViewHomePagePostMediaBinding.bind(view)

        RxBus.listen(RxEvent.StartVideo::class.java)
            .subscribeOnIoAndObserveOnMainThread({
                startVideoPlaybackIfVisible(it.checkImage)
            }, {
            }).autoDispose()

        binding?.apply {
            outgoerVideoPlayer.posterImageView.throttleClicks().subscribeAndObserveOnMainThread {
                postImage.filePath?.let { it1 -> mediaVideoViewClickSubject.onNext(it1) }
            }.autoDispose()

            outgoerVideoPlayer.throttleClicks().subscribeAndObserveOnMainThread {
                postImage.filePath?.let { it1 -> mediaVideoViewClickSubject.onNext(it1) }
            }.autoDispose()

            photoView.throttleClicks().subscribeAndObserveOnMainThread {
                mediaPhotoViewClickSubject.onNext(postImage.filePath ?: "")
            }.autoDispose()
        }
    }

    fun bind(postMediaType: Int, postImage: PostMediaInformation, position: Int) {
        this.position = position
        this.postImage = postImage
        binding?.apply {
            when (postMediaType) {
                1 -> {
                    if (postImage.extension != ".m3u8") {
                        setImageView()
                    } else {
                        setVideoView()
                    }
                }

                2 -> {
                    setVideoView()
                }
            }
        }
    }

    private fun setImageView() {
        binding?.apply {
            photoView.visibility = View.VISIBLE
            ivMutePlayer.visibility = View.GONE
            outgoerVideoPlayer.visibility = View.GONE
            videoFrameLayout.visibility = View.GONE
            photoView.let {
                Glide.with(context)
                    .load(postImage.filePath ?: "")
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding?.progressImageProgressBar?.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding?.progressImageProgressBar?.visibility = View.GONE
                            return false
                        }
                    }).placeholder(R.drawable.ic_placer_holder_image_new)
                    .into(it)
            }
        }
    }

    fun setVideoView() {
        binding?.outgoerVideoPlayer?.isVideMute = true
        binding?.outgoerVideoPlayer?.mute()
        binding?.photoView?.visibility = View.GONE
        binding?.ivMutePlayer?.visibility = View.VISIBLE
        binding?.videoFrameLayout?.visibility = View.VISIBLE
        binding?.outgoerVideoPlayer?.visibility = View.VISIBLE

        Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
        binding?.outgoerVideoPlayer?.posterImageView?.layoutParams?.height =
            ViewGroup.LayoutParams.MATCH_PARENT
        binding?.outgoerVideoPlayer?.posterImageView?.layoutParams?.width =
            ViewGroup.LayoutParams.MATCH_PARENT

        binding?.outgoerVideoPlayer?.posterImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
        binding?.outgoerVideoPlayer?.posterImageView?.let {
            Glide.with(context)
                .load(postImage.thumbnail)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.progressImageProgressBar?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.progressImageProgressBar?.visibility = View.GONE
                        return false
                    }
                })
                .placeholder(colorDrawable)
                .into(it)
        }

        binding?.outgoerVideoPlayer?.apply {
            this.posterImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            videoUrl = postImage.filePath
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_NORMAL
            )
            Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
        }

        binding?.ivMutePlayer?.throttleClicks()?.subscribeAndObserveOnMainThread {
            binding?.ivMutePlayer?.isSelected = !binding?.ivMutePlayer?.isSelected!!
            if (binding?.ivMutePlayer?.isSelected == true) {
                binding?.outgoerVideoPlayer?.isVideMute = false
                binding?.outgoerVideoPlayer?.unMute()
                binding?.ivMutePlayer?.setImageResource(R.drawable.ic_post_unmute)
            } else {
                binding?.outgoerVideoPlayer?.isVideMute = true
                binding?.outgoerVideoPlayer?.mute()
                binding?.ivMutePlayer?.setImageResource(R.drawable.ic_post_mute)
            }
        }?.autoDispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        playVideoSubject.onNext(Unit)
        binding = null
        Jzvd.releaseAllVideos()
    }

    private fun startVideoPlaybackIfVisible(checkImage: Boolean) {
        if (
            binding?.outgoerVideoPlayer?.isVisible == true &&
            binding?.outgoerVideoPlayer?.state != Jzvd.STATE_PLAYING
        ) {
            binding?.outgoerVideoPlayer?.startVideoAfterPreloading()
        } else {
            if (checkImage) {
                Jzvd.releaseAllVideos()
            }
        }
    }
}
