package com.gityou.app

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gityou.app.data.store.TokenStore
import com.gityou.app.ui.GitYouApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenStore: TokenStore

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        observeLanguageSetting()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            GitYouApp(windowSizeClass = windowSizeClass.widthSizeClass)
        }
    }

    /**
     * Applies the in-app language choice through the platform per-app locale
     * API (Android 13+). Empty locale list restores the system languages.
     * Setting locales recreates the activity, which is the expected behavior.
     */
    private fun observeLanguageSetting() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tokenStore.settingsFlow
                    .map { it.appLanguage }
                    .distinctUntilChanged()
                    .collect { language ->
                        val locales = when (language) {
                            "ru" -> LocaleList.forLanguageTags("ru")
                            "en" -> LocaleList.forLanguageTags("en")
                            else -> LocaleList.getEmptyLocaleList()
                        }
                        val manager = getSystemService(LocaleManager::class.java)
                        if (manager != null && manager.applicationLocales != locales) {
                            manager.applicationLocales = locales
                        }
                    }
            }
        }
    }
}
