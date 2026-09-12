package com.antigravity.go

import android.app.Application
import com.antigravity.go.util.AppLogger

class AntigravityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.i("App", "AntigravityApp created")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AppLogger.w("App", "onLowMemory warning triggered")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        AppLogger.d("App", "onTrimMemory level: $level")
    }
}
