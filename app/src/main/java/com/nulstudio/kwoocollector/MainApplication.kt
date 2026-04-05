package com.nulstudio.kwoocollector

import android.app.Application
import com.nulstudio.kwoocollector.push.JPushService
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        JPushService.initialize(this)
        startKoin {
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }
}
