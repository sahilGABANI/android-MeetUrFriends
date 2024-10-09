package com.meetfriend.app.ui.music

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.session.PlaybackStateCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.REPEAT_MODE_ONE
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding3.widget.textChanges
import com.meetfriend.app.api.music.model.MusicInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityAddMusicBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.camerakit.SnapEditorActivity
import com.meetfriend.app.ui.camerakit.utils.MIME_TYPE_IMAGE_JPEG
import com.meetfriend.app.ui.music.view.AddMusicAdapter
import com.meetfriend.app.ui.music.viewmodel.MusicInfoViewState
import com.meetfriend.app.ui.music.viewmodel.MusicViewModel
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddMusicActivity : BasicActivity() {

    companion object {
        private const val INTENT_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"
        private const val INTENT_EXTRA_MEDIA_TYPE = "INTENT_EXTRA_MEDIA_TYPE"
        fun getIntent(
            context: Context, videoPath: String, mediaType: String
        ): Intent {
            val intent = Intent(context, AddMusicActivity::class.java)
            intent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
            intent.putExtra(INTENT_EXTRA_MEDIA_TYPE, mediaType)
            return intent
        }
    }

    private var mediaType: String = MIME_TYPE_IMAGE_JPEG
    private var videoPath: String? = null
    private lateinit var binding: ActivityAddMusicBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MusicViewModel>
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var addMusicAdapter: AddMusicAdapter
    private var search = ""
    private var durationOfMedia: Int = 0
    var mHandler: Handler? = null
    var runnable: Runnable? = null
    private lateinit var exoPlayer  :ExoPlayer
    private var musicCategory = ""
    private var currentMusicInfo : MusicInfo? =null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)
        binding = ActivityAddMusicBinding.inflate(layoutInflater)
        videoPath = intent?.getStringExtra(INTENT_EXTRA_VIDEO_PATH) ?: ""
        mediaType = intent?.getStringExtra(INTENT_EXTRA_MEDIA_TYPE) ?: MIME_TYPE_IMAGE_JPEG
        musicViewModel = getViewModelFromFactory(viewModelFactory)
        setContentView(binding.root)
        mHandler = Handler()
        initUI()
        listenToViewModel()
        musicViewModel.resetPagination("Latest")
    }

    private fun listenToViewModel() {
        musicViewModel.musicState.subscribeAndObserveOnMainThread {
            when (it) {
                is MusicInfoViewState.GetMusicList -> {
                    addMusicAdapter.listOfMusicInfo = it.listOfMusic
                }
                is MusicInfoViewState.LoadingState -> {
                    showLoading(it.isLoading)
                }
                is MusicInfoViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
            }
        }.autoDispose()
    }

    private fun initUI() {
        initAdapter()
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.tabLayout.invalidate()
                when (tab.position) {
                    0 -> {
                        musicCategory = "latest"
                    }
                    1 -> {
                        musicCategory = "Bollywood+2017"
                    }
                    2 -> {
                        musicCategory = "Hollywood"
                    }
                    3 -> {
                        musicCategory = "90s"
                    }
                    4 -> {
                        musicCategory = "2000s"
                    }
                    5 -> {
                        musicCategory = "Punjabi"
                    }
                    6 -> {
                        musicCategory = "Love+songs"
                    }
                    7 -> {
                        musicCategory = "Indian+classical"
                    }
                    8 -> {
                        musicCategory = "Hip+Hop"
                    }
                    9 -> {
                        musicCategory = "Lo-fi"
                    }

                }
                musicViewModel.resetPagination(musicCategory)
                if (currentMusicInfo != null){
                    currentMusicInfo?.let {
                        RxBus.publish(RxEvent.UpdateProgressBar(it,0))
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })


        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
           onBackPressedDispatcher.onBackPressed()
        }.autoDispose()
        binding.searchAppCompatEditText.textChanges().skipInitialValue().doOnNext {

        }.debounce(300, TimeUnit.MILLISECONDS, Schedulers.io()).subscribeOnIoAndObserveOnMainThread({
            if (it.length > 2) {
                search = it.toString()
                musicViewModel.resetPagination(it.toString())
            } else {
                musicViewModel.resetPagination(musicCategory)
            }
        }, {
        }).autoDispose()
    }

    private fun initAdapter() {
        addMusicAdapter = AddMusicAdapter(this).apply {
            musicClick.subscribeAndObserveOnMainThread {
                if (it.isPlaying) {
                    currentMusicInfo = it
                    if (this@AddMusicActivity::exoPlayer.isInitialized) {
                        exoPlayer.release()
                    }
                    val listofmusic = addMusicAdapter.listOfMusicInfo
                    listofmusic?.forEach { music ->
                        if (music.isPlaying && it.id != music.id) music.isPlaying = false
                    }
                    addMusicAdapter.listOfMusicInfo = listofmusic
                    val downloadUrl = it.downloadUrl?.lastOrNull()?.url ?: ""
                    runnable = Runnable { updateSeekBar() }
                    startPlayAudio(downloadUrl)

                } else {
                    pausePlayAudio()
                }
            }.autoDispose()

            selectMusicClick.subscribeAndObserveOnMainThread {
                if (this@AddMusicActivity::exoPlayer.isInitialized) {
                    exoPlayer.pause()
                    exoPlayer.stop()
                }
                videoPath?.let { it1 ->
                    startActivityForResult(
                        TrimMusicActivity.getIntent(this@AddMusicActivity, mediaType, it1, it), SnapEditorActivity.ADD_MUSIC_REQUEST_CODE
                    )
                }
            }.autoDispose()
        }
        binding.musicRecyclerView.apply {
            adapter = addMusicAdapter
            layoutManager = LinearLayoutManager(this@AddMusicActivity, RecyclerView.VERTICAL, false)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager ?: return
                        var lastVisibleItemPosition = 0
                        if (layoutManager is LinearLayoutManager) {
                            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        }
                        val adjAdapterItemCount = layoutManager.itemCount
                        if (layoutManager.childCount > 0 && lastVisibleItemPosition >= adjAdapterItemCount - 2 && adjAdapterItemCount >= layoutManager.childCount) {
                            if (search.isNullOrEmpty()) musicViewModel.loadMore() else musicViewModel.loadMore(search)
                        }
                    }
                }
            })
        }
    }


    fun startPlayAudio(audioUrl: String) {
        exoPlayer = ExoPlayer.Builder(this@AddMusicActivity).build()
        val mediaItem = MediaItem.fromUri(audioUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        exoPlayer.play()
        exoPlayer.repeatMode = REPEAT_MODE_ONE
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                    //do something
                    durationOfMedia = exoPlayer.duration.toInt()
                    updateSeekBar()
                } else if (playbackState == PlaybackStateCompat.STATE_PAUSED){
                    runnable = null
                }
            }
        })
    }

    private fun updateSeekBar() {
        if (this::exoPlayer.isInitialized) {
            val progress: Double = exoPlayer.currentPosition.toDouble().div(durationOfMedia)
            if (currentMusicInfo != null){
                currentMusicInfo?.let {
                    RxBus.publish(RxEvent.UpdateProgressBar(it,(progress * 100).toInt()))
                }
            }
            runnable?.let { mHandler?.postDelayed(it, 100) }
        }

    }

    private fun pausePlayAudio() {
        if (this@AddMusicActivity::exoPlayer.isInitialized) {
            runnable = null
            exoPlayer.pause()
            exoPlayer.stop()
        }
    }

    private fun killMediaPlayer() {
        if (this@AddMusicActivity::exoPlayer.isInitialized) {
            exoPlayer.pause()
            exoPlayer.pause()
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    override fun onStop() {
        super.onStop()
        killMediaPlayer()
    }

    override fun onPause() {
        super.onPause()
        runnable = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == SnapEditorActivity.ADD_MUSIC_REQUEST_CODE) {
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}