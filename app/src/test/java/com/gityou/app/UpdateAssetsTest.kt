package com.gityou.app

import com.gityou.app.data.remote.dto.ReleaseAsset
import com.gityou.app.data.update.UpdateAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Picks the release APK matching the device's primary ABI. */
class UpdateAssetsTest {

    private fun asset(name: String) = ReleaseAsset(id = name.hashCode().toLong(), name = name, downloadUrl = "https://example.com/$name")

    private val ciAssets = listOf(
        asset("app-armeabi-v7a-release.apk"),
        asset("app-arm64-v8a-release.apk"),
        asset("app-x86-release.apk"),
        asset("app-x86_64-release.apk"),
    )

    @Test
    fun `primary device ABI wins`() {
        val picked = UpdateAssets.pickApkAsset(ciAssets, listOf("x86_64", "x86"))
        assertEquals("app-x86_64-release.apk", picked?.name)
    }

    @Test
    fun `arm64 device picks arm64`() {
        val picked = UpdateAssets.pickApkAsset(ciAssets, listOf("arm64-v8a", "armeabi-v7a"))
        assertEquals("app-arm64-v8a-release.apk", picked?.name)
    }

    @Test
    fun `x86 must not match inside x86_64`() {
        val picked = UpdateAssets.pickApkAsset(ciAssets, listOf("x86"))
        assertEquals("app-x86-release.apk", picked?.name)
    }

    @Test
    fun `unknown ABI falls back to universal then to the first apk`() {
        val withUniversal = ciAssets + asset("app-universal-release.apk")
        assertEquals("app-universal-release.apk", UpdateAssets.pickApkAsset(withUniversal, listOf("mips"))?.name)
        assertEquals("app-armeabi-v7a-release.apk", UpdateAssets.pickApkAsset(ciAssets, listOf("mips"))?.name)
    }

    @Test
    fun `non-apk assets and missing urls are ignored`() {
        val messy = listOf(
            asset("checksums.txt"),
            ReleaseAsset(id = 1, name = "app-arm64-v8a-release.apk", downloadUrl = null),
            asset("GitYou-arm64-v8a.apk"),
        )
        assertEquals("GitYou-arm64-v8a.apk", UpdateAssets.pickApkAsset(messy, listOf("arm64-v8a"))?.name)
    }

    @Test
    fun `no apk at all returns null`() {
        assertNull(UpdateAssets.pickApkAsset(emptyList(), listOf("arm64-v8a")))
        assertNull(UpdateAssets.pickApkAsset(listOf(asset("notes.md")), listOf("arm64-v8a")))
    }
}
