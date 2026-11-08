package com.gityou.app.data.repo

import android.content.Context
import android.os.Build
import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.remote.dto.ReleaseAsset
import com.gityou.app.data.update.UpdateAssets
import com.gityou.app.data.update.UpdateInfo
import com.gityou.app.data.update.UpdateVersions
import com.gityou.app.data.util.safeCall
import com.gityou.app.di.DownloadClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * In-app updates, self-hosted through GitHub Releases of the app's own
 * repository (oustery/GitYou). Checking is authenticated (the REST endpoint
 * lives on api.github.com); downloading is anonymous over the CDN.
 */
@Singleton
class UpdateRepository @Inject constructor(
    private val api: GitHubApi,
    @DownloadClient private val client: OkHttpClient,
    @ApplicationContext private val context: Context,
) {

    /**
     * Returns the newest installable update, or null when the running build is
     * already up to date. The APK asset is pre-picked for the device ABI.
     * A 404 means the repository has no releases yet — treated as "no update".
     */
    suspend fun checkForUpdate(currentVersion: String): Result<UpdateInfo?> = safeCall {
        val latest = try {
            api.latestRelease(REPO_OWNER, REPO_NAME)
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }
        if (latest == null || !UpdateVersions.isNewer(currentVersion, latest.tagName)) {
            null
        } else {
            UpdateInfo(release = latest, asset = UpdateAssets.pickApkAsset(latest.assets, deviceAbis()))
        }
    }

    /**
     * Streams the APK into cacheDir/updates and returns the finished file.
     * [onProgress] receives (downloaded, total) bytes; total is -1 when the
     * server does not send a content length. Cancellation between chunks stops
     * the loop and closes the connection.
     */
    suspend fun downloadApk(
        asset: ReleaseAsset,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): Result<File> = safeCall {
        val url = asset.downloadUrl ?: throw IOException("Asset has no download URL")
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
            val target = File(dir, asset.name)
            val partial = File(dir, asset.name + PART_SUFFIX)
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw httpError(response.code)
                val body = response.body ?: throw IOException("Empty download body")
                body.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_BYTES)
                        var downloaded = 0L
                        val total = body.contentLength()
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                        output.flush()
                    }
                }
            }
            if (!partial.renameTo(target)) {
                partial.delete()
                throw IOException("Could not move the downloaded APK into place")
            }
            target
        }
    }

    /** Deletes leftover APKs (finished or partial) from previous sessions. */
    fun clearDownloads() {
        val dir = File(context.cacheDir, UPDATES_DIR)
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun deviceAbis(): List<String> = Build.SUPPORTED_ABIS.filter { it.isNotBlank() }

    /**
     * Non-2xx on a plain (non-Retrofit) call: wrapped into an HttpException so
     * ApiErrors.keyOf() maps it to the shared "HTTP error" message.
     */
    private fun httpError(code: Int): HttpException =
        HttpException(Response.error<Any>(code, ByteArray(0).toResponseBody(null)))

    companion object {
        const val REPO_OWNER = "oustery"
        const val REPO_NAME = "GitYou"
        private const val UPDATES_DIR = "updates"
        private const val PART_SUFFIX = ".part"
        private const val BUFFER_BYTES = 64 * 1024
    }
}


