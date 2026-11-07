package com.gityou.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import com.gityou.app.BuildConfig
import com.gityou.app.R
import com.gityou.app.data.util.Formatters
import com.gityou.app.ui.components.EyebrowLabel
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.theme.AccentPalette
import com.gityou.app.ui.theme.Spacing
import com.gityou.app.ui.theme.accentPreviewColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLicenses by remember { mutableStateOf(false) }
    val languageSupported = Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU

    // Image cache (Coil): size is shown in the Data section, cleared on demand.
    val imageLoader = remember { SingletonImageLoader.get(context) }
    var cacheBytes by remember { mutableStateOf<Long?>(null) }
    suspend fun refreshCacheSize() {
        cacheBytes = withContext(Dispatchers.IO) { imageLoader.diskCache?.size }
    }
    LaunchedEffect(Unit) { refreshCacheSize() }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(stringResource(R.string.settings_licenses_title)) },
            text = { Text(stringResource(R.string.settings_licenses_body)) },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            // ---- Appearance ----
            EyebrowLabel(stringResource(R.string.settings_appearance_section))
            SettingsCard {
                Text(
                    stringResource(R.string.settings_theme_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md),
                ) {
                    val options = listOf(
                        "system" to stringResource(R.string.settings_theme_system),
                        "light" to stringResource(R.string.settings_theme_light),
                        "dark" to stringResource(R.string.settings_theme_dark),
                    )
                    options.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = settings.themeMode == value,
                            onClick = { viewModel.setThemeMode(value) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }

                SwitchRow(
                    title = stringResource(R.string.settings_dynamic_color_label),
                    body = stringResource(R.string.settings_dynamic_color_body),
                    checked = settings.dynamicColor,
                    enabled = Build.VERSION.SDK_INT >= VERSION_CODES.S,
                    onChecked = viewModel::setDynamicColor,
                )

                // Accent palette picks only apply to the branded fallback scheme.
                if (!settings.dynamicColor) {
                    Text(
                        stringResource(R.string.settings_accent_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        listOf(
                            AccentPalette.INDIGO to stringResource(R.string.settings_accent_indigo),
                            AccentPalette.OCEAN to stringResource(R.string.settings_accent_ocean),
                            AccentPalette.FOREST to stringResource(R.string.settings_accent_forest),
                            AccentPalette.SUNRISE to stringResource(R.string.settings_accent_sunrise),
                        ).forEach { (palette, label) ->
                            AccentSwatch(
                                label = label,
                                color = accentPreviewColor(palette),
                                selected = settings.accent == palette.key,
                                onClick = { viewModel.setAccent(palette.key) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                SwitchRow(
                    title = stringResource(R.string.settings_amoled_label),
                    body = stringResource(R.string.settings_amoled_body),
                    checked = settings.amoled,
                    onChecked = viewModel::setAmoled,
                )

                Text(
                    stringResource(R.string.settings_text_scale_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.md),
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md),
                ) {
                    val scales = listOf(
                        "small" to stringResource(R.string.settings_text_scale_small),
                        "normal" to stringResource(R.string.settings_text_scale_normal),
                        "large" to stringResource(R.string.settings_text_scale_large),
                        "extra_large" to stringResource(R.string.settings_text_scale_extra_large),
                    )
                    scales.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = settings.fontScale == value,
                            onClick = { viewModel.setFontScale(value) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = scales.size),
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }

                SwitchRow(
                    title = stringResource(R.string.settings_motion_label),
                    body = stringResource(R.string.settings_motion_body),
                    checked = settings.expressiveMotion,
                    onChecked = viewModel::setExpressiveMotion,
                )
            }

            // ---- Behavior ----
            EyebrowLabel(
                stringResource(R.string.settings_behavior_section),
                modifier = Modifier.padding(top = Spacing.lg),
            )
            SettingsCard {
                Text(
                    stringResource(R.string.settings_default_tab_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                // Chips flow onto the next line instead of squeezing: long
                // localized labels ("Уведомления") used to overflow the fixed
                // four-segment row and break the layout.
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    val tabs = listOf(
                        "home" to stringResource(R.string.settings_default_tab_home),
                        "search" to stringResource(R.string.settings_default_tab_search),
                        "notifications" to stringResource(R.string.settings_default_tab_notifications),
                        "profile" to stringResource(R.string.settings_default_tab_profile),
                    )
                    tabs.forEach { (value, label) ->
                        FilterChip(
                            selected = settings.defaultTab == value,
                            onClick = { viewModel.setDefaultTab(value) },
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }

                Text(
                    stringResource(R.string.settings_language_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.lg),
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md),
                ) {
                    val languages = listOf(
                        "system" to stringResource(R.string.settings_language_system),
                        "ru" to stringResource(R.string.settings_language_ru),
                        "en" to stringResource(R.string.settings_language_en),
                    )
                    languages.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = settings.appLanguage == value,
                            onClick = { viewModel.setAppLanguage(value) },
                            enabled = value == "system" || languageSupported,
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = languages.size),
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
                if (!languageSupported) {
                    Text(
                        stringResource(R.string.settings_language_unsupported),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }

                SwitchRow(
                    title = stringResource(R.string.settings_mark_read_on_open_label),
                    body = stringResource(R.string.settings_mark_read_on_open_body),
                    checked = settings.markReadOnOpen,
                    onChecked = viewModel::setMarkReadOnOpen,
                )

                SwitchRow(
                    title = stringResource(R.string.settings_haptics_label),
                    body = stringResource(R.string.settings_haptics_body),
                    checked = settings.hapticsEnabled,
                    onChecked = viewModel::setHapticsEnabled,
                )

                SwitchRow(
                    title = stringResource(R.string.settings_markdown_label),
                    body = stringResource(R.string.settings_markdown_body),
                    checked = settings.renderMarkdown,
                    onChecked = viewModel::setRenderMarkdown,
                )
            }

            // ---- Data ----
            EyebrowLabel(
                stringResource(R.string.settings_data_section),
                modifier = Modifier.padding(top = Spacing.lg),
            )
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_cache_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            cacheBytes?.let {
                                stringResource(R.string.settings_cache_size_line, Formatters.bytes(it))
                            } ?: stringResource(R.string.settings_cache_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    imageLoader.memoryCache?.clear()
                                    imageLoader.diskCache?.clear()
                                }
                                refreshCacheSize()
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.settings_cache_cleared),
                                )
                            }
                        },
                        enabled = (cacheBytes ?: 0L) > 0L,
                    ) {
                        Text(stringResource(R.string.settings_cache_clear))
                    }
                }
            }

            // ---- Account ----
            EyebrowLabel(
                stringResource(R.string.settings_account_section),
                modifier = Modifier.padding(top = Spacing.lg),
            )
            StateScreen(state = user, onRetry = viewModel::retryUser) { u ->
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = u.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                        )
                        Spacer(Modifier.width(Spacing.lg))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(u.name ?: u.login, style = MaterialTheme.typography.titleMedium)
                            Text(
                                u.login,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = viewModel::signOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.settings_sign_out))
            }

            // ---- About ----
            EyebrowLabel(
                stringResource(R.string.settings_about_section),
                modifier = Modifier.padding(top = Spacing.lg),
            )
            SettingsCard {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.settings_version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
                Text(
                    stringResource(R.string.settings_design_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/oustery/GitYou")),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(stringResource(R.string.settings_repo_link), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showLicenses = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(R.string.settings_licenses_title),
                            maxLines = 1,
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/oustery/GitYou/releases")),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                ) {
                    Text(stringResource(R.string.settings_releases), maxLines = 1)
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

/** One grouped settings section drawn as a quiet rounded card. */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            content()
        }
    }
}

/** Setting row with title, description and a status-only switch. */
@Composable
private fun SwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.md)
            // The whole row toggles, so the target is comfortably >= 48dp;
            // the Switch itself becomes status-only to avoid double taps.
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChecked,
            )
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Spacing.lg))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

/** Circular brand-color swatch with a label and selection ring. */
@Composable
private fun AccentSwatch(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        modifier = modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 3.dp,
                            color = ringColor,
                            shape = CircleShape,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {}
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}
