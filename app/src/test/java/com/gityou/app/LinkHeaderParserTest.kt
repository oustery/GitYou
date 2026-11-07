package com.gityou.app

import com.gityou.app.data.util.LinkHeaderParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LinkHeaderParserTest {

    @Test
    fun `parses next page from GitHub link header`() {
        val header = "<https://api.github.com/user/repos?page=2&per_page=30>; rel=\"next\", " +
            "<https://api.github.com/user/repos?page=5&per_page=30>; rel=\"last\""

        val links = LinkHeaderParser.parse(header)

        assertEquals("https://api.github.com/user/repos?page=2&per_page=30", links.next)
        assertEquals("https://api.github.com/user/repos?page=5&per_page=30", links.last)
        assertEquals(2, LinkHeaderParser.nextPageNumber(header))
    }

    @Test
    fun `returns null when there is no next link`() {
        val header = "<https://api.github.com/user/repos?page=1>; rel=\"prev\""

        val links = LinkHeaderParser.parse(header)

        assertNull(links.next)
        assertFalse(links.hasNext)
        assertNull(LinkHeaderParser.nextPageNumber(header))
    }

    @Test
    fun `handles empty and null headers`() {
        assertNull(LinkHeaderParser.nextPageNumber(null))
        assertNull(LinkHeaderParser.nextPageNumber(""))
        assertNull(LinkHeaderParser.nextPageNumber("garbage header"))
    }

    @Test
    fun `parses all four relations`() {
        val header = "<https://api.github.com/user/repos?page=1>; rel=\"first\", " +
            "<https://api.github.com/user/repos?page=1>; rel=\"prev\", " +
            "<https://api.github.com/user/repos?page=3>; rel=\"next\", " +
            "<https://api.github.com/user/repos?page=10>; rel=\"last\""

        val links = LinkHeaderParser.parse(header)

        assertEquals(3, LinkHeaderParser.nextPageNumber(header))
        assertEquals("https://api.github.com/user/repos?page=10", links.last)
    }
}
