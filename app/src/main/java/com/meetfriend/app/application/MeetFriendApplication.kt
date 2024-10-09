package com.meetfriend.app.application

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import androidx.multidex.MultiDex
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.meetfriend.app.BuildConfig
import com.meetfriend.app.di.BaseAppComponent
import com.meetfriend.app.di.BaseUiApp
import com.meetfriend.app.socket.SocketDataManager
import timber.log.Timber
import javax.inject.Inject


@SuppressLint("Registered")
open class MeetFriendApplication : BaseUiApp() {

    @Inject
    lateinit var socketDataManager: SocketDataManager

    companion object {
        var assetManager: AssetManager? = null
        lateinit var component: BaseAppComponent

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        setupLog()
        initInstallTime()
    }

    private fun setupLog() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    /**
     * start install-time delivery mode
     */
    private fun initInstallTime() {
        try {
            val context = createPackageContext("com.meetfriend.app", 0)
            assetManager = context.assets
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e("AssetManager $e")
        }
    }

    override fun getAppComponent(): BaseAppComponent {
        return component
    }

    override fun setAppComponent(baseAppComponent: BaseAppComponent) {
        component = baseAppComponent
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}