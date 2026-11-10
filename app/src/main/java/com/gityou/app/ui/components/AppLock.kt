package com.gityou.app.ui.components

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gityou.app.R
import com.gityou.app.ui.theme.Spacing

/**
 * Lock screen shown while the app is locked (G-016).
 *
 * The device credential (PIN, pattern, password or biometric, whichever the
 * system accepts) is verified by the platform — the app never stores or checks
 * a secret of its own. `KeyguardManager.createConfirmDeviceCredentialIntent`
 * is the neutral API that works on every supported version and does not tie
 * the client to a biometric library.
 *
 * When the device has no credential enrolled the lock cannot exist at all, so
 * the screen says so and lets the user in: locking someone out of their own
 * client permanently is worse than the risk this guards against.
 */
@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyguard = remember(context) {
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }
    val hasCredential = remember(context) { keyguard.isDeviceSecure }
    var failed by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onUnlocked()
        } else {
            // Cancelled or wrong credential: stay locked, offer another try.
            failed = true
        }
    }

    fun prompt() {
        val intent: Intent? = keyguard.createConfirmDeviceCredentialIntent(
            context.getString(R.string.lock_title),
            context.getString(R.string.lock_subtitle),
        )
        if (intent == null) {
            onUnlocked()
        } else {
            launcher.launch(intent)
        }
    }

    // Ask immediately: unlocking should be one gesture, not "unlock, then tap".
    LaunchedEffect(hasCredential) {
        if (hasCredential) prompt()
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )
            Text(
                stringResource(R.string.lock_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = Spacing.lg),
            )
            Text(
                stringResource(
                    if (hasCredential) R.string.lock_subtitle else R.string.lock_no_credential,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            Button(
                onClick = { if (hasCredential) prompt() else onUnlocked() },
                modifier = Modifier.padding(top = Spacing.xl),
            ) {
                Text(
                    stringResource(
                        when {
                            !hasCredential -> R.string.lock_continue
                            failed -> R.string.lock_retry
                            else -> R.string.lock_unlock
                        },
                    ),
                )
            }
        }
    }
}
