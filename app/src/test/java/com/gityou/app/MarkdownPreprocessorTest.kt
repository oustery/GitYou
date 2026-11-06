package com.gityou.app

import com.gityou.app.data.util.MarkdownPreprocessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownPreprocessorTest {

    @Test
    fun `plain markdown passes through untouched`() {
        val input = "# Title\n\nSome **bold** text with [link](https://example.com)"
        assertEquals(input, MarkdownPreprocessor.prepare(input))
    }

    @Test
    fun `details tags are removed but content stays`() {
        val input = "<details>\n<summary>Release notes</summary>\n\n- fix one\n</details>"
        val output = MarkdownPreprocessor.prepare(input)
        assertFalse(output.contains("<details"))
        assertFalse(output.contains("</details>"))
        assert(output.contains("- fix one"))
    }

    @Test
    fun `summary becomes bold line`() {
        val input = "<details>\n<summary>Release notes</summary>\nContent here\n</details>"
        val output = MarkdownPreprocessor.prepare(input)
        assert(output.contains("**Release notes**")) { output }
        assertFalse(output.contains("<summary"))
    }

    @Test
    fun `summary with html attributes and inner markup is handled`() {
        val input = "<details open><summary><em>Notes</em> v2</summary>body</details>"
        val output = MarkdownPreprocessor.prepare(input)
        assert(output.contains("**<em>Notes</em> v2**")) { output }
    }

    @Test
    fun `dependabot style body renders without raw tags`() {
        val input = """
            Bumps `hilt` from 2.57.2 to 2.60.1.
            <details>
            <summary>Release notes</summary>
            <p><em>Sourced from <a href="https://github.com/google/dagger/releases">releases</a>.</em></p>
            <blockquote>
            <h2>Dagger 2.60.1</h2>
            <ul><li>Fixes <a href="https://example.com">#5203</a></li></ul>
            </blockquote>
            </details>
        """.trimIndent()
        val output = MarkdownPreprocessor.prepare(input)
        listOf("<details>", "</details>", "<summary>", "</summary>").forEach { tag ->
            assertFalse("leaked tag: $tag", output.contains(tag))
        }
        assert(output.contains("**Release notes**")) { output }
        assert(output.contains("Bumps `hilt`")) { output }
    }

    @Test
    fun `br tags become newlines`() {
        assertEquals("a\nb", MarkdownPreprocessor.prepare("a<br>b"))
        assertEquals("a\nb", MarkdownPreprocessor.prepare("a<br/>b"))
        assertEquals("a\nb", MarkdownPreprocessor.prepare("a<BR />b"))
    }

    @Test
    fun `runs of blank lines collapse`() {
        val output = MarkdownPreprocessor.prepare("a\n\n\n\n\nb")
        assertEquals("a\n\nb", output)
    }

    @Test
    fun `multiline summary collapses to one line`() {
        val input = "<summary>Line one\nLine two\n\nLine three</summary>"
        val output = MarkdownPreprocessor.prepare(input)
        assert(output.contains("**Line one Line two Line three**")) { output }
    }

    @Test
    fun `text without angle brackets stays intact`() {
        val input = "Just plain **markdown** with no html"
        assertEquals(input, MarkdownPreprocessor.prepare(input))
    }

    @Test
    fun `real dependabot pr body keeps badge and command list`() {
        // Shape of the actual PR #9 body on oustery/GitYou: intro paragraph,
        // an image-only link (SVG badge), markdown comments, hr, then a
        // <details> block with a <br /> and the command bullet list.
        val input = """
            Bumps androidx.compose:compose-bom from 2025.06.01 to 2026.09.00.


            [![Dependabot compatibility score](https://dependabot-badges.githubapp.com/badges/compatibility_score?dependency-name=compose-bom&package-manager=gradle)](https://docs.github.com/en/github/managing-security-vulnerabilities/about-dependabot-security-updates#about-compatibility-scores)

            Dependabot will resolve any conflicts with this PR as long as you don't alter it yourself. You can also trigger a rebase manually by commenting `@dependabot rebase`.

            [//]: # (dependabot-automerge-start)
            [//]: # (dependabot-automerge-end)

            ---

            <details>
            <summary>Dependabot commands and options</summary>
            <br />

            You can trigger Dependabot actions by commenting on this PR:
            - `@dependabot rebase` will rebase this PR
            - `@dependabot recreate` will recreate this PR


            </details>
        """.trimIndent()
        val output = MarkdownPreprocessor.prepare(input)
        // The badge image must survive so the renderer can show it.
        assert(output.contains("![Dependabot compatibility score]")) { output }
        // Details/summary unwrapped, content kept.
        listOf("<details>", "</details>", "<summary>", "</summary>").forEach { tag ->
            assertFalse("leaked tag: $tag", output.contains(tag))
        }
        assert(output.contains("**Dependabot commands and options**")) { output }
        // The bullet list of commands survives.
        assert(output.contains("- `@dependabot rebase` will rebase this PR")) { output }
        // No triple blank runs left behind.
        assertFalse("no triple blank runs", output.contains("\n\n\n"))
    }
}
