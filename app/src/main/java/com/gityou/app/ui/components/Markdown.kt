package com.gityou.app.ui.components

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import com.gityou.app.data.util.MarkdownPreprocessor
import com.gityou.app.di.MarkdownImageClient
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.gif.GifMediaDecoder
import io.noties.markwon.image.network.OkHttpNetworkSchemeHandler
import io.noties.markwon.image.svg.SvgMediaDecoder
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * Builds a fully configured [Markwon] instance bound to the current theme.
 *
 * Instances are cached per [ColorScheme] inside this singleton: every
 * [MarkdownText] node (issue body + every comment in the list) used to
 * rebuild the whole 10-plugin pipeline on first composition and on every
 * scroll (F-010). The cache is keyed by theme identity, so dark/light,
 * dynamic color and accent switches still get their own instance.
 *
 * The image pipeline uses a dedicated UNAUTHENTICATED OkHttp client
 * ([MarkdownImageClient]): markdown images live on arbitrary third-party
 * hosts and must never receive the GitHub bearer token (F-002).
 */
@Singleton
class MarkdownFactory @Inject constructor(
    @MarkdownImageClient private val imageClient: OkHttpClient,
) {

    // Application context only: a cached Markwon must never pin an Activity.
    private val cache = object : LinkedHashMap<ColorScheme, Markwon>(4, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ColorScheme, Markwon>): Boolean = size > 4
    }

    fun create(context: Context, colors: ColorScheme): Markwon = synchronized(cache) {
        cache.getOrPut(colors) { build(context.applicationContext, colors) }
    }

    private fun build(context: Context, colors: ColorScheme): Markwon {
        val density = context.resources.displayMetrics.density
        val cellPaddingPx = (8 * density).toInt()

        return Markwon.builder(context)
            .usePlugin(ImagesPlugin.create { plugin ->
                plugin.addSchemeHandler(OkHttpNetworkSchemeHandler.create(imageClient))
                // SVG badges (shields.io, dependabot) and animated GIFs are
                // everywhere in GitHub content; the decoders ship inside the
                // markwon-image artifact, backed by androidsvg and
                // android-gif-drawable on the classpath.
                plugin.addMediaDecoder(SvgMediaDecoder.create())
                plugin.addMediaDecoder(GifMediaDecoder.create())
                plugin.placeholderProvider {
                    ColorDrawable(colors.surfaceContainerHigh.toArgb())
                }
                // A broken image shows a quiet themed block instead of
                // silently collapsing to nothing (F-011).
                plugin.errorHandler { _, _ ->
                    ColorDrawable(colors.errorContainer.toArgb())
                }
            })
            .usePlugin(HtmlPlugin.create())
            .usePlugin(SyntaxHighlightPlugin.create(markdownPrism, codeTheme(colors)))
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
                        // Full GitHub heading scale (2.0x) is enormous on a
                        // phone screen; a capped ladder keeps hierarchy
                        // without the README title shouting over the text.
                        .headingTextSizeMultipliers(
                            floatArrayOf(1.35f, 1.22f, 1.12f, 1.05f, 1.0f, 0.95f),
                        )
                        .headingBreakColor(colors.outlineVariant.toArgb())
                        .blockQuoteColor(colors.primary.toArgb())
                        .blockQuoteWidth((4 * density).toInt())
                        .codeBlockTextColor(colors.onSurface.toArgb())
                        .codeBlockBackgroundColor(colors.surfaceContainerHigh.toArgb())
                        .codeTextColor(colors.onSurface.toArgb())
                        .codeBackgroundColor(colors.surfaceContainerHigh.toArgb())
                }
            })
            .build()
    }

    private companion object {
        /** Darkula for dark surfaces, default light theme otherwise. */
        fun codeTheme(colors: ColorScheme) =
            if (colors.background.luminance() < 0.5f) {
                Prism4jThemeDarkula.create()
            } else {
                Prism4jThemeDefault.create()
            }
    }
}

/** Access point for [MarkdownFactory] inside composables. */
val LocalMarkdownFactory = staticCompositionLocalOf<MarkdownFactory> {
    error("MarkdownFactory is not provided")
}

/** Whether markdown should render formatted; provided from the user setting. */
val LocalRenderMarkdown = staticCompositionLocalOf { true }

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
    // The "Render Markdown" setting falls back to plain text everywhere.
    if (!LocalRenderMarkdown.current) {
        Text(
            markdown,
            style = textStyle,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    val factory = LocalMarkdownFactory.current
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val markwon = remember(factory, colors, context) { factory.create(context, colors) }
    val prepared = remember(markdown) {
        MarkdownPreprocessor.prepare(markdown)
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
            // Re-parse/set only when the markdown actually changed: update{}
            // runs on every recomposition and a full setParsedMarkdown forces
            // a complete layout pass (F-019).
            if (textView.tag !== parsed) {
                textView.tag = parsed
                markwon.setParsedMarkdown(textView, parsed)
            }
            textView.setTextColor(colors.onSurface.toArgb())
            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, bodySizePx)
            textView.setLineSpacing(0f, 1.25f)
        },
        modifier = modifier.fillMaxWidth(),
    )
}
