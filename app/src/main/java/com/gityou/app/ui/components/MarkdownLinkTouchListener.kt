package com.gityou.app.ui.components

import android.text.Layout
import android.text.Spannable
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

/**
 * Tap handler for links inside a rendered markdown [TextView] (G-007).
 *
 * `LinkMovementMethod` always opens the browser, yet most links in GitHub
 * content point back into GitHub itself (issues, PRs, repositories, users).
 * This listener resolves the span under the finger and gives the host screen a
 * chance to navigate in-app; when it declines, the touch falls through to the
 * platform movement method, so external links keep working exactly as before.
 *
 * Returning false for everything that is not a handled link is what keeps
 * text selection, scrolling and long-press intact.
 */
internal class MarkdownLinkTouchListener(
    private val onLink: (String) -> Boolean,
) : View.OnTouchListener {

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return false
        val textView = view as? TextView ?: return false
        val text = textView.text as? Spannable ?: return false
        val layout: Layout = textView.layout ?: return false

        val x = (event.x - textView.totalPaddingLeft + textView.scrollX).toInt()
        val y = (event.y - textView.totalPaddingTop + textView.scrollY).toInt()
        if (y < 0 || y > layout.height) return false
        if (x < 0 || x > layout.width) return false

        val line = layout.getLineForVertical(y)
        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
        val span = text.getSpans(offset, offset, URLSpan::class.java).firstOrNull() ?: return false

        // `getOffsetForHorizontal` snaps to the closest character, so a tap in
        // the empty space after a short line would still hit the last link;
        // require the tap to be inside the span's own horizontal bounds.
        val start = text.getSpanStart(span)
        val end = text.getSpanEnd(span)
        val left = layout.getPrimaryHorizontal(start)
        val right = layout.getPrimaryHorizontal(end)
        if (x < minOf(left, right) || x > maxOf(left, right)) return false

        return onLink(span.url)
    }
}
