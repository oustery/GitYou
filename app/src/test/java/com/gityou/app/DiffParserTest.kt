package com.gityou.app

import com.gityou.app.data.util.DiffParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiffParserTest {

    @Test
    fun `added removed and context lines get sequential numbers`() {
        val patch = """
            @@ -1,3 +1,3 @@ header
             unchanged
            -old line
            +new line
        """.trimIndent()

        val lines = DiffParser.parse(patch)

        assertEquals(4, lines.size)
        val hunk = lines[0]
        assertEquals(DiffParser.LineType.HUNK, hunk.type)
        val context = lines[1]
        assertEquals(DiffParser.LineType.CONTEXT, context.type)
        assertEquals(1, context.oldNumber)
        assertEquals(1, context.newNumber)
        val removed = lines[2]
        assertEquals(DiffParser.LineType.REMOVED, removed.type)
        assertEquals(2, removed.oldNumber)
        assertNull(removed.newNumber)
        val added = lines[3]
        assertEquals(DiffParser.LineType.ADDED, added.type)
        assertEquals(2, added.newNumber)
        assertNull(added.oldNumber)
    }

    @Test
    fun `hunk header start offsets are honored`() {
        val patch = """
            @@ -14,2 +30,2 @@
             a
             b
        """.trimIndent()

        val lines = DiffParser.parse(patch)

        assertEquals(Pair(14, 30), DiffParser.parseHunkHeader(lines[0].text))
        assertEquals(14, lines[1].oldNumber)
        assertEquals(30, lines[1].newNumber)
        assertEquals(15, lines[2].oldNumber)
        assertEquals(31, lines[2].newNumber)
    }

    @Test
    fun `no newline marker keeps parsing without breaking numbering`() {
        val patch = """
            @@ -1,1 +1,2 @@
            -tail without newline
            \ No newline at end of file
            +tail with newline
        """.trimIndent()

        val lines = DiffParser.parse(patch)

        assertEquals(4, lines.size)
        assertEquals(DiffParser.LineType.CONTEXT, lines[2].type)
        assertNull(lines[2].oldNumber)
        val added = lines[3]
        assertEquals(DiffParser.LineType.ADDED, added.type)
        assertEquals(1, added.newNumber)
    }

    @Test
    fun `garbage input produces no crash and no phantom numbers`() {
        val lines = DiffParser.parse("not a diff\r\nrandom text\r\n")

        assertEquals(0, lines.size)
    }
}
