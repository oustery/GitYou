package com.gityou.app.data.util

/**
 * Prepares GitHub-flavored markdown before it reaches Markwon.
 *
 * Markown's HTML plugin renders the common tags (h1-h6, a, em, strong,
 * blockquote, ul/ol, sub, sup) but has no collapsible-section support, so
 * `<details>/<summary>` markup — which dependabot bodies and many READMEs
 * are full of — would otherwise leak as raw text. This preprocessor
 * unwraps those constructs into native markdown:
 *
 *  - `<details>` / `</details>` are removed (content stays, rendered inline);
 *  - `<summary>X</summary>` becomes a bold `**X**` line;
 *  - `<br>` becomes a hard line break;
 *  - runs of 3+ blank lines collapse.
 */
object MarkdownPreprocessor {

    private val detailsOpen = Regex("<details(\\s[^>]*)?>", RegexOption.IGNORE_CASE)
    private val detailsClose = Regex("</details>", RegexOption.IGNORE_CASE)
    private val summary = Regex(
        "<summary(\\s[^>]*)?>(.*?)</summary>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val lineBreak = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val blankLines = Regex("\n{3,}")

    fun prepare(raw: String): String {
        val withoutDetails = raw
            .replace(detailsOpen, "")
            .replace(detailsClose, "")

        val withSummaries = withoutDetails.replace(summary) { match ->
            val text = match.groupValues[2]
                .replace(lineBreak, " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.isEmpty()) "" else "**$text**"
        }

        return withSummaries
            .replace(lineBreak, "\n")
            .replace(blankLines, "\n\n")
            .trim('\n')
    }
}
