package com.gityou.app.data.store

import android.content.Context
import android.util.Log
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
        val amoled: Boolean = false,
        val accent: String = "indigo",
        val fontScale: String = "normal",
        val expressiveMotion: Boolean = true,
        val defaultTab: String = "home",
        val appLanguage: String = "system",
        val renderMarkdown: Boolean = true,
        val hapticsEnabled: Boolean = true,
        val markReadOnOpen: Boolean = true,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dataStore = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(context.filesDir, "gityou.preferences_pb") },
    )

    /** Never swallow DataStore failures silently (F-023): corrupted prefs must be diagnosable. */
    private fun logDataStoreError(area: String, cause: Throwable) {
        Log.w(TAG, "DataStore read failed ($area); falling back to defaults", cause)
    }

    private companion object {
        const val TAG = "TokenStore"
    }

    private object Keys {
        val TOKEN = stringPreferencesKey("github_token")
        val CLIENT_ID = stringPreferencesKey("oauth_client_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED = booleanPreferencesKey("amoled")
        val ACCENT = stringPreferencesKey("accent")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val EXPRESSIVE_MOTION = booleanPreferencesKey("expressive_motion")
        val DEFAULT_TAB = stringPreferencesKey("default_tab")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val RENDER_MARKDOWN = booleanPreferencesKey("render_markdown")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val MARK_READ_ON_OPEN = booleanPreferencesKey("mark_read_on_open")
    }

    @Volatile
    private var cachedToken: String? = null

    init {
        scope.launch {
            dataStore.data
                .catch { cause -> logDataStoreError("token cache", cause) }
                .collect { prefs -> cachedToken = prefs[Keys.TOKEN] }
        }
    }

    val tokenFlow: Flow<String?> = dataStore.data
        .catch { cause -> logDataStoreError("token flow", cause) }
        .map { it[Keys.TOKEN] }

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { cause -> logDataStoreError("settings flow", cause) }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.THEME_MODE] ?: "system",
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
                amoled = prefs[Keys.AMOLED] ?: false,
                accent = prefs[Keys.ACCENT] ?: "indigo",
                fontScale = prefs[Keys.FONT_SCALE] ?: "normal",
                expressiveMotion = prefs[Keys.EXPRESSIVE_MOTION] ?: true,
                defaultTab = prefs[Keys.DEFAULT_TAB] ?: "home",
                appLanguage = prefs[Keys.APP_LANGUAGE] ?: "system",
                renderMarkdown = prefs[Keys.RENDER_MARKDOWN] ?: true,
                hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
                markReadOnOpen = prefs[Keys.MARK_READ_ON_OPEN] ?: true,
            )
        }

    fun cachedToken(): String? = cachedToken

    /** In-app override, falling back to the compile-time BuildConfig value. */
    suspend fun clientId(): String {
        val stored = dataStore.data.first()[Keys.CLIENT_ID]?.takeIf { it.isNotBlank() }
        return (stored ?: BuildConfig.GITYOU_CLIENT_ID).trim()
    }

    suspend fun saveToken(token: String) {
        val trimmed = token.trim()
        dataStore.edit { it[Keys.TOKEN] = trimmed }
        // Update the cache in the same call: the first request after sign-in
        // must already carry the Authorization header (the collector above is
        // asynchronous and would race the network call, F-005).
        cachedToken = trimmed
    }

    suspend fun clearToken() {
        dataStore.edit { it.remove(Keys.TOKEN) }
        // Same race as saveToken: a request fired right after sign-out must
        // not go out with the stale token attached.
        cachedToken = null
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

    suspend fun saveAmoled(enabled: Boolean) {
        dataStore.edit { it[Keys.AMOLED] = enabled }
    }

    suspend fun saveAccent(accent: String) {
        dataStore.edit { it[Keys.ACCENT] = accent }
    }

    suspend fun saveFontScale(scale: String) {
        dataStore.edit { it[Keys.FONT_SCALE] = scale }
    }

    suspend fun saveExpressiveMotion(enabled: Boolean) {
        dataStore.edit { it[Keys.EXPRESSIVE_MOTION] = enabled }
    }

    suspend fun saveDefaultTab(tab: String) {
        dataStore.edit { it[Keys.DEFAULT_TAB] = tab }
    }

    suspend fun saveAppLanguage(language: String) {
        dataStore.edit { it[Keys.APP_LANGUAGE] = language }
    }

    suspend fun saveRenderMarkdown(enabled: Boolean) {
        dataStore.edit { it[Keys.RENDER_MARKDOWN] = enabled }
    }

    suspend fun saveHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun saveMarkReadOnOpen(enabled: Boolean) {
        dataStore.edit { it[Keys.MARK_READ_ON_OPEN] = enabled }
    }
}
