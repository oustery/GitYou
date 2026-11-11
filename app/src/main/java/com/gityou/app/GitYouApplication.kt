package com.gityou.app

import android.app.Application
import com.gityou.app.data.telemetry.CrashReporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GitYouApplication : Application() {

    @Inject
    lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()
        // Local crash capture (G-017): reports stay in the app's private
        // storage and are only ever shared by explicit user action in Settings.
        crashReporter.install()
    }
}
