package com.gityou.app

import com.gityou.app.data.util.MarkdownPreprocessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
