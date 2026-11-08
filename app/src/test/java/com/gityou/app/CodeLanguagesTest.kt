package com.gityou.app

import com.gityou.app.data.util.CodeLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeLanguagesTest {

    @Test
    fun `common extensions map to prism languages`() {
        assertEquals("kotlin", CodeLanguages.languageOf("Main.kt"))
        assertEquals("kotlin", CodeLanguages.languageOf("build.gradle.kts"))
        assertEquals("java", CodeLanguages.languageOf("src/Foo.java"))
        assertEquals("python", CodeLanguages.languageOf("script.py"))
        assertEquals("markup", CodeLanguages.languageOf("AndroidManifest.xml"))
        assertEquals("yaml", CodeLanguages.languageOf("ci.yml"))
    }

    @Test
    fun `unknown extension yields null`() {
        assertNull(CodeLanguages.languageOf("data.unknownext"))
        assertNull(CodeLanguages.languageOf("Makefile"))
    }

    @Test
    fun `extensionless build files are groovy`() {
        assertEquals("groovy", CodeLanguages.languageOf("Jenkinsfile"))
        assertNull(CodeLanguages.languageOf("LICENSE"))
    }

    @Test
    fun `markdown detection is case insensitive and name based`() {
        assertTrue(CodeLanguages.isMarkdown("README.md"))
        assertTrue(CodeLanguages.isMarkdown("docs/Guide.MD"))
        assertFalse(CodeLanguages.isMarkdown("Main.kt"))
    }

    @Test
    fun `fence wrap keeps code intact and escapes fence runs`() {
        val code = "fun main() {\n    println(\"hi\")\n}"
        val wrapped = CodeLanguages.wrapInCodeFence("kotlin", code)

        assertTrue(wrapped.startsWith("````kotlin\n"))
        assertTrue(wrapped.endsWith("\n````"))
        assertTrue(wrapped.contains(code))
    }

    @Test
    fun `code containing triple backticks does not break the fence`() {
        val code = "```kotlin\nval x = 1\n```"
        val wrapped = CodeLanguages.wrapInCodeFence(null, code)

        // The 4-backtick outer fence survives a 3-backtick block inside.
        assertTrue(wrapped.startsWith("````\n"))
        assertTrue(wrapped.contains("```kotlin"))
    }

    @Test
    fun `code containing four backticks gets neutralized`() {
        val wrapped = CodeLanguages.wrapInCodeFence(null, "a ```` b")

        // Exactly two unbroken 4-backtick runs remain: the outer fence pair.
        assertEquals(2, Regex("````").findAll(wrapped).count())
        assertTrue(wrapped.contains("```\u200b`"))
    }
}
