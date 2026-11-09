package com.gityou.app.di

import com.gityou.app.data.store.TokenCipher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Storage-layer wiring: the token cipher is a plain class over Android Keystore. */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideTokenCipher(): TokenCipher = TokenCipher()
}
