package com.gityou.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.TimeSource

/**
 * Injectable monotonic time source so time-dependent logic (device-flow
 * deadline, retries) is testable without a real clock (F-015).
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideTimeSource(): TimeSource = TimeSource.Monotonic
}
