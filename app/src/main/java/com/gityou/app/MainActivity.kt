package com.gityou.app

import android.app.LocaleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gityou.app.data.store.TokenStore
import com.gityou.app.ui.AppViewModel
import com.gityou.app.ui.GitYouApp
import com.gityou.app.ui.components.MarkdownFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenStore: TokenStore

    /** UI-layer factory provided here, not through a ViewModel (F-018). */
    @Inject
    lateinit var markdownFactory: MarkdownFactory

    /**
     * Inbound App Links and shares (G-012) are parsed by the root ViewModel,
     * which survives configuration changes and remembers the request until the
     * user is signed in.
     */
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        observeLanguageSetting()
        appViewModel.onSubmit(intent)
        setContent {
            GitYouApp(
                markdownFactory = markdownFactory,
                appViewModel = appViewModel,
            )
        }
    }

    /**
     * `launchMode` stays standard, so a second App Link starts a new instance
     * only when the task was finished; this covers the warm case.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appViewModel.onSubmit(intent)
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
