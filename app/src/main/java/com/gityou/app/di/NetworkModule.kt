package com.gityou.app.di

import com.gityou.app.BuildConfig
import com.gityou.app.data.remote.AuthApi
import com.gityou.app.data.remote.AuthInterceptor
import com.gityou.app.data.remote.GitHubApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

/**
 * OkHttp client for markdown images (README badges, issue screenshots).
 * Deliberately WITHOUT AuthInterceptor: image URLs point at arbitrary
 * third-party hosts (camo.githubusercontent.com, shields.io, <img src> in
 * issue bodies) and must never receive the GitHub bearer token (F-002).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MarkdownImageClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    /** Unauthenticated client for arbitrary image hosts; no token, no logging. */
    @Provides
    @Singleton
    @MarkdownImageClient
    fun provideMarkdownImageClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @GitHubRetrofit
    fun provideGitHubRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            // Scalars must be registered first: the raw README endpoint returns
            // plain text, which the JSON converter alone cannot deserialize.
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideGitHubApi(@GitHubRetrofit retrofit: Retrofit): GitHubApi =
        retrofit.create(GitHubApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(@AuthRetrofit retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)
}
