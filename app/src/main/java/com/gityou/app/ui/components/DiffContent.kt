package com.gityou.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gityou.app.data.remote.dto.CommitFile
import com.gityou.app.data.util.DiffParser
import com.gityou.app.ui.theme.Spacing

private val AddedGreen = Color(0xFF1E7B34)
private val RemovedRed = Color(0xFFB3261E)
private val AddedGreenContainer = Color(0xFFDDF3D8)
private val RemovedRedContainer = Color(0xFFF9DEDC)

/**
 * Renders the changed files of a commit or pull request with per-line
 * coloring. Files whose patch is missing (binary, too large) show a stat
 * header only.
 */
@Composable
fun DiffContent(
    files: List<CommitFile>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        files.forEach { file -> DiffFileCard(file) }
    }
}

@Composable
private fun DiffFileCard(file: CommitFile) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    file.filename,
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "+${file.additions}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AddedGreen,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    "−${file.deletions}",
                    style = MaterialTheme.typography.labelMedium,
                    color = RemovedRed,
                )
            }
            val patch = file.patch
            if (patch == null) {
                Text(
                    file.status?.replaceFirstChar { it.uppercase() } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.md),
                )
            } else {
                val lines = remember(file.filename, patch) { DiffParser.parse(patch) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    lines.forEach { line -> DiffRow(line) }
                }
            }
        }
    }
}

@Composable
private fun DiffRow(line: DiffParser.DiffLine) {
    val (fg, bg) = when (line.type) {
        DiffParser.LineType.ADDED -> AddedGreen to AddedGreenContainer
        DiffParser.LineType.REMOVED -> RemovedRed to RemovedRedContainer
        DiffParser.LineType.HUNK -> MaterialTheme.colorScheme.primary to Color.Transparent
        DiffParser.LineType.CONTEXT -> MaterialTheme.colorScheme.onSurface to Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg),
        verticalAlignment = Alignment.Top,
    ) {
        val oldNo = line.oldNumber?.toString().orEmpty().padStart(4)
        val newNo = line.newNumber?.toString().orEmpty().padStart(4)
        Text(
            "$oldNo  $newNo",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.sm),
        )
        Text(
            line.text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = fg,
            modifier = Modifier.padding(start = Spacing.sm, end = Spacing.sm),
        )
    }
}
