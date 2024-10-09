package com.meetfriend.app.cache

import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.offline.HlsDownloader
import com.meetfriend.app.application.MeetFriend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Executors

class ExoCacheManager {
    companion object {
        private const val PRE_CACHE_SIZE = 1024 * 1024L
        private const val TAG = "ExoCacheManager"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val executor = Executors.newSingleThreadExecutor()

    fun prepareCacheVideos(videoUrls: List<String>) {
        videoUrls.forEach { videoUrl ->
            coroutineScope.launch {
                preCacheVideo(Uri.parse(videoUrl))
            }
        }
    }

    fun prepareCacheVideo(videoUrl: String) {
        coroutineScope.launch {
            preCacheVideo(Uri.parse(videoUrl))
        }
    }

    private suspend fun preCacheVideo(uri: Uri) = withContext(Dispatchers.IO) {
        runCatching {
            // do nothing if already cache enough
            if (MeetFriend.cache.isCached(uri.toString(), 0, PRE_CACHE_SIZE)) {
                Timber.tag(TAG).i("video has been cached, return")
                return@runCatching
            }
            val downloader = HlsDownloader(
                MediaItem.Builder().setUri(uri)
                    .build(),
                MeetFriend.cacheDataSourceFactory,
                executor
            )
            Timber.tag(TAG).i("start pre-caching for position: $uri")
            downloader.download { _, bytesDownloaded, percentDownloaded ->
                if (bytesDownloaded >= PRE_CACHE_SIZE) downloader.cancel()
                Timber.tag(TAG)
                    .i("PRE_CACHE_SIZE: $PRE_CACHE_SIZE, bytesDownloaded: $bytesDownloaded, percentDownloaded: $percentDownloaded")
            }
        }.onFailure {
            if (it is InterruptedException) return@onFailure
            Timber.tag(TAG).i("Cache fail for position: $uri with exception: $it}")
            it.printStackTrace()
        }.onSuccess {
            Timber.tag(TAG).i("Cache success for position: $uri")
        }
        Unit
    }
}