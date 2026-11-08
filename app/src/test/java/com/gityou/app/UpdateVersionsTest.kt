package com.gityou.app

import com.gityou.app.data.update.UpdateVersions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Semver-style comparison used to decide whether a release tag is newer. */
class UpdateVersionsTest {

    @Test
    fun `newer patch, minor and major versions are detected`() {
        assertTrue(UpdateVersions.isNewer("1.2.0", "1.2.1"))
        assertTrue(UpdateVersions.isNewer("1.2.0", "1.3.0"))
        assertTrue(UpdateVersions.isNewer("1.2.0", "2.0.0"))
    }

    @Test
    fun `equal versions and downgrades are not newer`() {
        assertFalse(UpdateVersions.isNewer("1.2.0", "1.2.0"))
        assertFalse(UpdateVersions.isNewer("1.2.0", "1.1.9"))
        assertFalse(UpdateVersions.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun `leading v is ignored`() {
        assertTrue(UpdateVersions.isNewer("1.2.0", "v1.2.1"))
        assertTrue(UpdateVersions.isNewer("v1.2.0", "1.2.1"))
        assertFalse(UpdateVersions.isNewer("V1.2.0", "v1.2.0"))
    }

    @Test
    fun `missing components count as zero`() {
        assertTrue(UpdateVersions.isNewer("1.2", "1.2.1"))
        assertFalse(UpdateVersions.isNewer("1.2.0", "1.2"))
        assertEquals(0, UpdateVersions.compare("1.0", "1.0.0.0"))
    }

    @Test
    fun `prerelease loses to the plain release of the same version`() {
        assertTrue(UpdateVersions.isNewer("1.3.0-rc1", "1.3.0"))
        assertFalse(UpdateVersions.isNewer("1.3.0", "1.3.0-rc1"))
        // ...but a newer prerelease still outranks an older release.
        assertTrue(UpdateVersions.isNewer("1.3.0", "1.4.0-rc1"))
    }

    @Test
    fun `prerelease suffixes compare in natural order`() {
        assertTrue(UpdateVersions.isNewer("1.3.0-rc1", "1.3.0-rc2"))
        assertFalse(UpdateVersions.isNewer("1.3.0-beta", "1.3.0-alpha"))
    }

    @Test
    fun `digit runs compare numerically, not lexically`() {
        // Lexicographic order would put "rc10" BEFORE "rc9" (N-002) and a user
        // on rc9 would never be offered rc10.
        assertTrue(UpdateVersions.isNewer("1.3.0-rc9", "1.3.0-rc10"))
        assertFalse(UpdateVersions.isNewer("1.3.0-rc10", "1.3.0-rc9"))
        // Dotted semver prerelease identifiers behave the same way.
        assertTrue(UpdateVersions.isNewer("1.3.0-rc.9", "1.3.0-rc.10"))
        // Leading zeros do not confuse the numeric run.
        assertEquals(0, UpdateVersions.compare("1.3.0-rc07", "1.3.0-rc7"))
        // Digit runs inside a word still compare numerically.
        assertTrue(UpdateVersions.isNewer("1.3.0-build2", "1.3.0-build11"))
    }

    @Test
    fun `malformed tags degrade gracefully instead of crashing`() {
        // Garbage components count as 0; the comparison stays total and never throws.
        assertEquals(0, UpdateVersions.compare("", ""))
        assertTrue(UpdateVersions.isNewer("garbage", "2.0.0"))
        assertFalse(UpdateVersions.isNewer("2.0.0", "garbage"))
    }

    @Test
    fun `four-component tags compare numerically`() {
        assertTrue(UpdateVersions.isNewer("1.2.3.0", "1.2.3.1"))
        assertFalse(UpdateVersions.isNewer("1.2.3.1", "1.2.3.0"))
    }
}
