package com.gityou.app.di

import android.content.Context
import com.gityou.app.BuildConfig
import com.gityou.app.data.store.TokenStore
import com.gityou.app.data.telemetry.CrashReporter
import com.gityou.app.data.telemetry.LocalTelemetry
import com.gityou.app.data.telemetry.Telemetry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Diagnostics wiring (G-017): one shared directory holds the opt-in event
 * journal and the crash reports, so the Settings screen and the share
 * intent have a single place to look at.
 */
@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {

    @Provides
    @Singleton
    fun provideTelemetry(@ApplicationContext context: Context, tokenStore: TokenStore): Telemetry =
        LocalTelemetry(
            dir = File(context.filesDir, CrashReporter.DIR_NAME),
            // Consulted per event: flipping the Settings switch is immediate.
            enabled = tokenStore::telemetryEnabledSnapshot,
        )

    @Provides
    @Singleton
    fun provideCrashReporter(@ApplicationContext context: Context): CrashReporter =
        CrashReporter(
            dir = File(context.filesDir, CrashReporter.DIR_NAME),
            version = BuildConfig.VERSION_NAME,
        )
}
