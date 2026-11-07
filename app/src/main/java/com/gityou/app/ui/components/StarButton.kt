package com.gityou.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.gityou.app.R

/**
 * Star toggle with coordinated shape + color morph (spring-driven),
 * following the expressive "prominent state change" pattern: the shape
 * relaxes into a near-circle when starred, the container warms to coral.
 * Color is never the only signal: icon and state description change too.
 */
@Composable
fun MorphingStarButton(
    isStarred: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val corner by animateDpAsState(
        targetValue = if (isStarred) 48.dp else 20.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "starCorner",
    )
    val container by animateColorAsState(
        targetValue = if (isStarred) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        label = "starContainer",
    )
    val contentColor: Color = if (isStarred) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    val actionLabel = stringResource(if (isStarred) R.string.star_action_remove else R.string.star_action_add)
    val stateLabel = stringResource(if (isStarred) R.string.star_state_starred else R.string.star_state_not_starred)
    val haptics = appHaptics()

    Box(
        modifier = modifier
            .size(60.dp)
            .clip(RoundedCornerShape(corner))
            .background(container)
            .clickable(enabled = !isLoading) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .semantics {
                role = Role.Button
                contentDescription = actionLabel
                stateDescription = stateLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
            )
        } else {
            Icon(
                if (isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/** Small rounded container for secondary repo actions (fork, open in browser). */
@Composable
fun CircleActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
    }
}

/** 10 dp language dot used in cards; null renders nothing. */
@Composable
fun LanguageDot(color: Color?, modifier: Modifier = Modifier) {
    if (color != null) {
        Box(modifier = modifier.size(10.dp).clip(CircleShape).background(color))
    }
}
