package com.gityou.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gityou.app.R
import com.gityou.app.data.util.RelativeTime

/**
 * Maps a [RelativeTime] bucket onto localized plural strings.
 * Composable so the value re-resolves when the app language changes.
 */
@Composable
@ReadOnlyComposable
fun relativeTimeString(time: RelativeTime): String = when (time) {
    RelativeTime.Now -> stringResource(R.string.time_now)
    is RelativeTime.Minutes -> pluralStringResource(R.plurals.time_minutes_ago, time.value, time.value)
    is RelativeTime.Hours -> pluralStringResource(R.plurals.time_hours_ago, time.value, time.value)
    is RelativeTime.Days -> pluralStringResource(R.plurals.time_days_ago, time.value, time.value)
    is RelativeTime.Absolute -> time.text
}
