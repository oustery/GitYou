package com.gityou.app.data.store

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Persisted app preferences and the GitHub access token. */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenCipher: TokenCipher,
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
        /**
         * Ask for a device credential before showing the app (G-016). The
         * stored token grants write access to every repository the user can
         * push to; a lost unlocked phone must not hand that over.
         */
        val biometricLock: Boolean = false,
        /** Minutes in the background before the lock re-arms; 0 = immediately. */
        val lockTimeoutMinutes: Int = 1,
        /**
         * Opt-in local diagnostics journal (G-017). Events are written only
         * to the app's private storage and never leave the device; turning
         * this off wipes the journal. Off is the default.
         */
        val telemetryEnabled: Boolean = false,
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

        /** Must match [TokenCipher]'s prefix; kept here to avoid a hard dependency. */
        const val ENCRYPTED_PREFIX = "enc:v1:"
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
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val LOCK_TIMEOUT = intPreferencesKey("lock_timeout_minutes")
        val TELEMETRY = booleanPreferencesKey("telemetry_enabled")
    }

    @Volatile
    private var cachedToken: String? = null

    /**
     * Synchronous view of [AppSettings.telemetryEnabled] for the telemetry
     * gate, which must decide per event without suspending (G-017).
     */
    @Volatile
    private var cachedTelemetryEnabled = false

    /**
     * Single source of truth for the unread badge (G-004). The badge used to
     * live only in AppViewModel and was recomputed from the first notification
     * page, so "mark all read" left a stale number behind. Every place that
     * learns the true count publishes it here; every badge reads it.
     */
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .catch { cause -> logDataStoreError("token cache", cause) }
                .collect { prefs ->
                    cachedToken = withContext(Dispatchers.IO) {
                        tokenCipher.decrypt(prefs[Keys.TOKEN])
                    }
                    cachedTelemetryEnabled = prefs[Keys.TELEMETRY] ?: false
                }
        }
        scope.launch { migratePlaintextToken() }
    }

    /**
     * G-002 migration: a token written by an older build sits in DataStore as
     * plaintext. Encrypt it in place on first run so the upgrade is invisible
     * to the user and the file stops containing a usable credential.
     */
    private suspend fun migratePlaintextToken() {
        val stored = runCatching { dataStore.data.first()[Keys.TOKEN] }.getOrNull()
        if (stored.isNullOrBlank() || stored.startsWith(ENCRYPTED_PREFIX)) return
        val encrypted = withContext(Dispatchers.IO) { tokenCipher.encrypt(stored) }
        if (encrypted.startsWith(ENCRYPTED_PREFIX)) {
            dataStore.edit { it[Keys.TOKEN] = encrypted }
            cachedToken = stored
        }
    }

    /**
     * Decryption touches the Android Keystore, so the mapping runs on IO
     * instead of on the collector's (usually main) dispatcher.
     */
    val tokenFlow: Flow<String?> = dataStore.data
        .catch { cause -> logDataStoreError("token flow", cause) }
        .map { tokenCipher.decrypt(it[Keys.TOKEN]) }
        .flowOn(Dispatchers.IO)

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
                biometricLock = prefs[Keys.BIOMETRIC_LOCK] ?: false,
                lockTimeoutMinutes = prefs[Keys.LOCK_TIMEOUT] ?: 1,
                telemetryEnabled = prefs[Keys.TELEMETRY] ?: false,
            )
        }

    fun cachedToken(): String? = cachedToken

    /**
     * Publishes the authoritative unread count. [count] must come from a full
     * notification fetch (not from a single page) so the badge never shows a
     * floor of the real number.
     */
    fun setUnreadCount(count: Int) {
        _unreadCount.value = count.coerceAtLeast(0)
    }

    /** Optimistically drops the badge when the server call has succeeded. */
    fun decrementUnread(by: Int = 1) {
        _unreadCount.value = (_unreadCount.value - by).coerceAtLeast(0)
    }

    fun clearUnread() {
        _unreadCount.value = 0
    }

    /** In-app override, falling back to the compile-time BuildConfig value. */
    suspend fun clientId(): String {
        val stored = dataStore.data.first()[Keys.CLIENT_ID]?.takeIf { it.isNotBlank() }
        return (stored ?: BuildConfig.GITYOU_CLIENT_ID).trim()
    }

    suspend fun saveToken(token: String) {
        val trimmed = token.trim()
        // Encrypted at rest with a Keystore-backed key (G-002); encryption is
        // an IO-bound Keystore operation, never run on the caller's thread.
        val stored = withContext(Dispatchers.IO) { tokenCipher.encrypt(trimmed) }
        dataStore.edit { it[Keys.TOKEN] = stored }
        // Update the cache in the same call: the first request after sign-in
        // must already carry the Authorization header (the collector above is
        // asynchronous and would race the network call, F-005).
        cachedToken = trimmed
    }

    suspend fun clearToken() {
        dataStore.edit { it.remove(Keys.TOKEN) }
        clearUnread()
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

    suspend fun saveBiometricLock(enabled: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun saveLockTimeout(minutes: Int) {
        dataStore.edit { it[Keys.LOCK_TIMEOUT] = minutes.coerceAtLeast(0) }
    }

    suspend fun saveTelemetryEnabled(enabled: Boolean) {
        // Update the snapshot first: events fired right after the flip must
        // respect the new value even before the DataStore collector wakes up.
        cachedTelemetryEnabled = enabled
        dataStore.edit { it[Keys.TELEMETRY] = enabled }
    }

    /** Blocking read for the telemetry gate; safe to call from any thread. */
    fun telemetryEnabledSnapshot(): Boolean = cachedTelemetryEnabled

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
