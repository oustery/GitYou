package com.gityou.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gityou.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Persisted app preferences and the GitHub access token. */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    data class AppSettings(
        val themeMode: String = "system",
        val dynamicColor: Boolean = false,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dataStore = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(context.filesDir, "gityou.preferences_pb") },
    )

    private object Keys {
        val TOKEN = stringPreferencesKey("github_token")
        val CLIENT_ID = stringPreferencesKey("oauth_client_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    @Volatile
    private var cachedToken: String? = null

    init {
        scope.launch {
            dataStore.data
                .catch { /* corrupted preferences: start clean */ }
                .collect { prefs -> cachedToken = prefs[Keys.TOKEN] }
        }
    }

    val tokenFlow: Flow<String?> = dataStore.data
        .catch { }
        .map { it[Keys.TOKEN] }

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.THEME_MODE] ?: "system",
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            )
        }

    fun cachedToken(): String? = cachedToken

    /** In-app override, falling back to the compile-time BuildConfig value. */
    suspend fun clientId(): String {
        val stored = dataStore.data.first()[Keys.CLIENT_ID]?.takeIf { it.isNotBlank() }
        return (stored ?: BuildConfig.GITYOU_CLIENT_ID).trim()
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { it[Keys.TOKEN] = token.trim() }
    }

    suspend fun clearToken() {
        dataStore.edit { it.remove(Keys.TOKEN) }
    }

    suspend fun saveClientId(clientId: String) {
        dataStore.edit { it[Keys.CLIENT_ID] = clientId.trim() }
    }

    suspend fun saveThemeMode(mode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun saveDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }
}
