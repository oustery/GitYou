package com.gityou.app.data.update

import com.gityou.app.data.remote.dto.Release
import com.gityou.app.data.remote.dto.ReleaseAsset

/** An installable update: the newest release plus the APK asset to download. */
data class UpdateInfo(
    val release: Release,
    /** Null when the release ships no downloadable APK — only "open on GitHub". */
    val asset: ReleaseAsset?,
) {
    /** Tag without the leading "v", suitable for side-by-side display. */
    val displayVersion: String = release.tagName.removePrefix("v").removePrefix("V")
}

/**
 * Version comparison for release tags such as "1.2.0", "v1.3.0", "1.4.0-rc1".
 * Pure and Android-free so it can be unit-tested directly.
 */
object UpdateVersions {

    /** True when [latest] is strictly newer than the running [current] version. */
    fun isNewer(current: String, latest: String): Boolean = compare(current, latest) < 0

    /**
     * Numeric comparison of dot-separated components (missing components are 0).
     * An optional "-suffix" marks a prerelease, which loses to the plain release
     * of the same core version. Malformed input degrades gracefully: garbage
     * components count as 0, so the app never crashes on a weird tag.
     */
    fun compare(a: String, b: String): Int {
        val (coreA, preA) = split(a)
        val (coreB, preB) = split(b)
        for (i in 0 until maxOf(coreA.size, coreB.size)) {
            val x = coreA.getOrElse(i) { 0 }
            val y = coreB.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return comparePreRelease(preA, preB)
    }

    /** "v1.2.0-rc1" -> ([1, 2, 0], "rc1"). */
    private fun split(version: String): Pair<List<Int>, String> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        val dash = cleaned.indexOf('-')
        val core = if (dash >= 0) cleaned.substring(0, dash) else cleaned
        val pre = if (dash >= 0) cleaned.substring(dash + 1) else ""
        val numbers = core.split('.').map { part -> part.filter(Char::isDigit).toIntOrNull() ?: 0 }
        return numbers to pre
    }

    private fun comparePreRelease(a: String, b: String): Int = when {
        a == b -> 0
        a.isEmpty() -> 1 // a is the plain release, b is a prerelease
        b.isEmpty() -> -1
        else -> naturalCompare(a, b)
    }

    /**
     * Natural (digit-aware) order for prerelease suffixes: digit runs compare
     * numerically, so "rc10" outranks "rc9" the way every human — and semver —
     * expects; plain lexicographic ordering would put "rc10" before "rc9"
     * (N-002) and the user would never be offered the fix.
     */
    private fun naturalCompare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                val digitCompare = compareDigitRuns(a, i, b, j)
                if (digitCompare != 0) return digitCompare
                i = digitRunEnd(a, i)
                j = digitRunEnd(b, j)
            } else {
                if (ca != cb) return ca.compareTo(cb)
                i++
                j++
            }
        }
        return (a.length - i).compareTo(b.length - j)
    }

    /** Compares the digit runs starting at [ai] in [a] and [bj] in [b] numerically. */
    private fun compareDigitRuns(a: String, ai: Int, b: String, bj: Int): Int {
        val runA = a.substring(ai, digitRunEnd(a, ai)).trimStart('0')
        val runB = b.substring(bj, digitRunEnd(b, bj)).trimStart('0')
        // Leading zeros stripped so "007" == "7"; longer means bigger.
        return runA.length.compareTo(runB.length).takeIf { it != 0 }
            ?: runA.compareTo(runB)
    }

    /** Index right after the run of digits starting at [start]. */
    private fun digitRunEnd(s: String, start: Int): Int {
        var end = start
        while (end < s.length && s[end].isDigit()) end++
        return end
    }
}

/**
 * Chooses which release asset to download. The build produces one APK per ABI,
 * so the device's own ABIs are tried in order (a 64-bit device also runs 32-bit
 * builds), then a universal APK as fallback. Pure and Android-free: the caller
 * passes its own ABI list.
 */
object UpdateAssets {

    fun pickApkAsset(assets: List<ReleaseAsset>, deviceAbis: List<String>): ReleaseAsset? {
        val apks = assets.filter { it.downloadUrl != null && it.name.endsWith(".apk", ignoreCase = true) }
        if (apks.isEmpty()) return null
        for (abi in deviceAbis) {
            apks.firstOrNull { matchesAbi(it.name, abi) }?.let { return it }
        }
        return apks.firstOrNull { it.name.contains("universal", ignoreCase = true) } ?: apks.first()
    }

    /**
     * Delimiter-aware match: "x86" must not match inside "x86_64".
     * Gradle splits are named app-<abi>-<buildType>.apk, so the ABI is
     * surrounded by "-" on both sides.
     */
    private fun matchesAbi(name: String, abi: String): Boolean =
        name.contains("-$abi-", ignoreCase = true) ||
            name.contains("-$abi.", ignoreCase = true) ||
            name.startsWith("$abi-", ignoreCase = true)
}
