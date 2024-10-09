package com.meetfriend.app.ui.music.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.MusicItemViewBinding
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AddMusicView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val musicClickSubject: PublishSubject<MusicInfo> = PublishSubject.create()
    val musicClick: Observable<MusicInfo> = musicClickSubject.hide()

    private val selectMusicClickSubject: PublishSubject<MusicInfo> = PublishSubject.create()
    val selectMusicClick: Observable<MusicInfo> = selectMusicClickSubject.hide()

    private lateinit var binding: MusicItemViewBinding
    private lateinit var musicResponse: MusicInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.music_item_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = MusicItemViewBinding.bind(view)

        binding.apply {
            rlMain.throttleClicks().subscribeAndObserveOnMainThread {
                selectMusicClickSubject.onNext(musicResponse)
            }.autoDispose()

            playAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                musicResponse.isPlaying = !musicResponse.isPlaying
                if(musicResponse.isPlaying) {
                    loadMusic.isVisible = true
                    playAppCompatImageView.setPadding(resources.getDimensionPixelOffset(R.dimen._10sdp),resources.getDimensionPixelOffset(R.dimen._10sdp),resources.getDimensionPixelOffset(R.dimen._10sdp),resources.getDimensionPixelOffset(R.dimen._10sdp))
                    playAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.music_pause_button, null))
                } else {
                    playAppCompatImageView.setPadding(0,0,0,0)
                    playAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.music_play_button, null))
                }
                musicClickSubject.onNext(musicResponse)
            }.autoDispose()

            RxBus.listen(RxEvent.UpdateProgressBar::class.java).subscribeAndObserveOnMainThread {
                if (this@AddMusicView::musicResponse.isInitialized) {
                    if (musicResponse.id == it.musicInfo.id) {
                        loadMusic.isVisible = false
                        progressProgressBar.setProgress(it.process,true)
                    }
                }
            }.autoDispose()
        }



    }

    fun bind(musicRes: MusicInfo) {
        this.musicResponse = musicRes
        binding.apply {
            progressProgressBar.setProgress(0,true)
            if(musicResponse.isPlaying) {
                playAppCompatImageView.setPadding(resources.getDimensionPixelOffset(R.dimen._10sdp),resources.getDimensionPixelOffset(R.dimen._10sdp),resources.getDimensionPixelOffset(R.dimen._10sdp),resources.getDimensionPixelOffset(R.dimen._10sdp))
                playAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.music_pause_button, null))
            } else {
                playAppCompatImageView.setPadding(0,0,0,0)
                playAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.music_play_button, null))
            }

            Glide.with(context)
                .load(musicResponse.image?.lastOrNull()?.url)
                .placeholder(R.drawable.dummy_profile_pic)
                .centerCrop()
                .into(ivProfile)


            musicTitleAppCompatTextView.text = musicResponse.name
            singerNameAppCompatTextView.text = getArtistsNames()

            singerNameAppCompatTextView.visibility = if(getArtistsNames().isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }


    fun getArtistsNames(): String? {
        return musicResponse.artists?.all?.joinToString(separator = ", ") { it?.name ?: "" }
    }
}