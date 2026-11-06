package com.gityou.app.ui.components

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.network.OkHttpNetworkSchemeHandler
import io.noties.markwon.linkify.LinkifyPlugin
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * Builds a fully configured [Markwon] instance bound to the current theme.
 *
 * One instance per color scheme is enough: schemes change rarely, and the
 * builder is cheap. The factory is provided through Hilt so markdown images
 * reuse the app's OkHttp client (connection pool, timeouts, logging off).
 */
@Singleton
class MarkdownFactory @Inject constructor(
    private val imageClient: OkHttpClient,
) {
    fun create(context: Context, colors: androidx.compose.material3.ColorScheme): Markwon {
        val density = context.resources.displayMetrics.density
        val cellPaddingPx = (8 * density).toInt()

        return Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create { plugin ->
                plugin.addSchemeHandler(OkHttpNetworkSchemeHandler.create(imageClient))
            })
            .usePlugin(TablePlugin.create { builder ->
                builder
                    .tableBorderColor(colors.outlineVariant.toArgb())
                    .tableCellPadding(cellPaddingPx)
                    .tableBorderWidth((1 * density).toInt())
                    .tableHeaderRowBackgroundColor(colors.surfaceContainerHigh.toArgb())
                    .tableEvenRowBackgroundColor(colors.surfaceContainerLow.toArgb())
                    .tableOddRowBackgroundColor(android.graphics.Color.TRANSPARENT)
            })
            .usePlugin(TaskListPlugin.create(
                colors.primary.toArgb(),
                colors.outline.toArgb(),
                colors.primary.toArgb(),
            ))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .linkColor(colors.primary.toArgb())
                        .codeBlockTextColor(colors.onSurface.toArgb())
                        .codeBlockBackgroundColor(colors.surfaceContainerHigh.toArgb())
                        .codeTextColor(colors.onSurface.toArgb())
                        .codeBackgroundColor(colors.surfaceContainerHigh.toArgb())
                }
            })
            .build()
    }
}

/** Access point for [MarkdownFactory] inside composables. */
val LocalMarkdownFactory = staticCompositionLocalOf<MarkdownFactory> {
    error("MarkdownFactory is not provided")
}

/**
 * Theme-aware markdown renderer for issue bodies, comments and READMEs.
 * HTML leaks (`<details>`, `<summary>`) are normalized by
 * [com.gityou.app.data.util.MarkdownPreprocessor] before parsing.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val factory = LocalMarkdownFactory.current
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val markwon = remember(factory, colors, context) { factory.create(context, colors) }
    val prepared = remember(markdown) {
        com.gityou.app.data.util.MarkdownPreprocessor.prepare(markdown)
    }
    val parsed = remember(markwon, prepared) { markwon.toMarkdown(prepared) }
    // Density carries both the system and the in-app text scale settings.
    val bodySizePx = with(density) { textStyle.fontSize.toPx() }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = LinkMovementMethod.getInstance()
                highlightColor = android.graphics.Color.TRANSPARENT
            }
        },
        update = { textView ->
            markwon.setParsedMarkdown(textView, parsed)
            textView.setTextColor(colors.onSurface.toArgb())
            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, bodySizePx)
            textView.setLineSpacing(0f, 1.25f)
        },
        modifier = modifier.fillMaxWidth(),
    )
}
