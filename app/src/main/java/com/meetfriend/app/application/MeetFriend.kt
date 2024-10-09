package com.meetfriend.app.application

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.*
import com.meetfriend.app.cache.ExoCacheManager
import com.meetfriend.app.di.DaggerMeetFriendAppComponent
import com.meetfriend.app.di.MeetFriendAppComponent
import com.meetfriend.app.di.MeetFriendAppModule
import com.meetfriend.app.newbase.ActivityManager
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import timber.log.Timber
import java.io.File
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.gms.ads.MobileAds


class MeetFriend : MeetFriendApplication() {
    companion object {
        private const val CACHE_SIZE = 50 * 1024 * 1024L
        private var cacheInstance: Cache? = null
        private const val PRE_CACHE_SIZE = 1024 * 1024L
        private const val TAG = "MeetFriend"
        operator fun get(app: Application): MeetFriend {
            return app as MeetFriend
        }

        operator fun get(activity: Activity): MeetFriend {
            return activity.application as MeetFriend
        }

        lateinit var component: MeetFriendAppComponent
            private set

        lateinit var cache: Cache
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var upstreamDataSourceFactory: DefaultDataSourceFactory
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var cacheDataSourceFactory: CacheDataSource.Factory
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var exoCacheManager: ExoCacheManager
            private set
    }

    private var lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                Timber.tag("AppLifecycleListener").e("App moved to background")
                socketDataManager.disconnect()
            }

            Lifecycle.Event.ON_START -> {
                Timber.tag("AppLifecycleListener").e("App moved to foreground")
                socketDataManager.connect()
            }

            Lifecycle.Event.ON_DESTROY -> {
                Timber.tag("AppLifecycleListener").e("App killed")
                socketDataManager.disconnect()
            }

            else -> {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            component = DaggerMeetFriendAppComponent.builder()
                .meetFriendAppModule(MeetFriendAppModule(this))
                .build()
            component.inject(this)
            super.setAppComponent(component)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        MobileAds.initialize(this) {}
        observeSocket()
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        ActivityManager.getInstance().init(this)
        cache = _cache
        exoCacheManager = _exoCacheManager
        upstreamDataSourceFactory = _upstreamDataSourceFactory
        cacheDataSourceFactory = _cacheDataSourceFactory
    }

    private fun observeSocket() {
        socketDataManager.connectionEmitter().subscribeOnIoAndObserveOnMainThread({

        }, {
            Timber.e(it)
        })
        socketDataManager.connectionError().subscribeOnIoAndObserveOnMainThread({

        }, {
            Timber.e(it)
        })
        socketDataManager.disconnectEmitter().subscribeOnIoAndObserveOnMainThread({

        }, {
            Timber.e(it)
        })
    }

    private val _cache by lazy {
        return@lazy cacheInstance ?: run {
            val exoCacheDir = File("${this.cacheDir.absolutePath}/exo")
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            SimpleCache(exoCacheDir, evictor, StandaloneDatabaseProvider(this)).also {
                cacheInstance = it
            }
        }
    }

    private val _exoCacheManager by lazy {
        ExoCacheManager()
    }

    private val _upstreamDataSourceFactory by lazy { DefaultDataSourceFactory(this) }

    private val _cacheDataSourceFactory by lazy {
        val cacheSink = CacheDataSink.Factory()
            .setCache(_cache)

        CacheDataSource.Factory()
            .setCache(_cache)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setUpstreamDataSourceFactory(_upstreamDataSourceFactory)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setEventListener(object : CacheDataSource.EventListener {
                override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {

                }

                override fun onCacheIgnored(reason: Int) {
                    Timber.tag(TAG).e("onCacheIgnored. reason:$reason")
                }
            })
    }


}