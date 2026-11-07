package com.gityou.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gityou.app.R
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.remote.dto.PullRequest
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.util.Formatters
import com.gityou.app.data.util.relativeOrAbsolute
import com.gityou.app.ui.theme.Spacing

/** Compact repository summary used in Home, Search and Profile lists. */
@Composable
fun RepoCard(
    repo: Repo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAvatar: Boolean = true,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // In "my repositories" the same avatar would repeat on every
                // row; callers hide it there for a quieter, cleaner list.
                if (showAvatar) {
                    AsyncImage(
                        model = repo.owner?.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                }
                // titleMedium already carries SemiBold in Type.kt; no override.
                Text(
                    repo.fullName ?: repo.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            repo.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageDot(LanguageColors.colorFor(repo.language))
                    repo.language?.let {
                        Spacer(Modifier.width(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(Formatters.count(repo.stargazersCount), style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CallSplit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(Formatters.count(repo.forksCount), style = MaterialTheme.typography.bodySmall)
                }
                Formatters.date(repo.updatedAt)?.let {
                    Spacer(Modifier.weight(1f))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * One row for an issue or pull request list entry. The state chip uses the
 * container role of the state color (primary/tertiary/surface), so contrast
 * is guaranteed by the tonal palette instead of an arbitrary alpha.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IssuePullRow(
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    title: String,
    meta: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialShapes.Pill.toShape())
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun IssueRow(
    issue: Issue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpen = issue.state == "open"
    val (container, content) = if (isOpen) {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    IssuePullRow(
        icon = if (isOpen) Icons.Filled.BugReport else Icons.Filled.CheckCircleOutline,
        containerColor = container,
        iconTint = content,
        title = issue.title,
        meta = stringResource(
            R.string.issue_row_meta,
            issue.number,
            relativeTimeString(Formatters.relativeOrAbsolute(issue.createdAt)),
            issue.user?.login ?: "",
        ),
        trailing = issue.commentCount.takeIf { it > 0 }?.let { count ->
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ModeComment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    // A real gap, not a space character glued to the number (F-029).
                    Spacer(Modifier.width(2.dp))
                    Text(count.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun PullRow(
    pull: PullRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // GitHub semantics: open = brand container, merged = tertiary, closed = quiet.
    val (icon, container, content) = when {
        pull.merged -> Triple(
            Icons.Filled.MergeType,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        pull.state == "open" -> Triple(
            Icons.Filled.CallSplit,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        else -> Triple(
            Icons.Filled.CallSplit,
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    IssuePullRow(
        icon = icon,
        containerColor = container,
        iconTint = content,
        title = if (pull.draft) stringResource(R.string.pull_draft_prefix, pull.title) else pull.title,
        meta = stringResource(
            R.string.issue_row_meta,
            pull.number,
            relativeTimeString(Formatters.relativeOrAbsolute(pull.createdAt)),
            pull.user?.login ?: "",
        ),
        trailing = pull.commentCount.takeIf { it > 0 }?.let { count ->
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ModeComment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(count.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        onClick = onClick,
        modifier = modifier,
    )
}

/** Stat block for the repository overview (stars / forks / issues). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatBlock(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = Spacing.lg)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialShapes.Pill.toShape())
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
