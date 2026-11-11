package com.gityou.app.data.telemetry

import android.util.Log
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Local crash capture (G-017). When the app is about to die from an uncaught
 * exception, a small JSON record is written to the diagnostics directory and
 * the original handler is invoked, so the system dialog and the crash logcat
 * behave exactly as before.
 *
 * Reports stay on the device. The Settings screen shows how many are stored
 * and offers to share the newest one manually — no network is involved.
 */
class CrashReporter(
    private val dir: File,
    private val version: String,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    @Serializable
    internal data class Record(
        val ts: Long,
        val version: String,
        val thread: String,
        val exception: String,
        val message: String? = null,
        val stack: String,
    )

    private val json = Json

    /**
     * Installs the handler. Crash capture is always on (it is the only way to
     * ever learn that the app crashes); it contains no personal data beyond
     * the stack trace itself, and the user can inspect or delete the files.
     */
    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(thread, throwable) }
                .onFailure { Log.w(TAG, "crash capture failed", it) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Writes one crash file and prunes the directory to [KEEP] newest. */
    internal fun record(thread: Thread, throwable: Throwable) {
        dir.mkdirs()
        val record = Record(
            ts = clock(),
            version = version,
            thread = thread.name,
            exception = throwable.javaClass.name,
            message = throwable.message?.takeIf { it.isNotBlank() },
            stack = throwable.stackTraceToString().take(MAX_STACK_CHARS),
        )
        File(dir, "$PREFIX${clock()}.json").writeText(json.encodeToString(Record.serializer(), record))
        prune()
    }

    /** Newest stored crash report, or null when none exist. */
    fun latest(): File? = files().lastOrNull()

    /** Number of stored crash reports. */
    fun count(): Int = files().size

    /** Deletes every stored crash report. */
    fun clear() {
        files().forEach { runCatching { it.delete() } }
    }

    private fun prune() {
        val all = files()
        if (all.size <= KEEP) return
        all.take(all.size - KEEP).forEach { runCatching { it.delete() } }
    }

    private fun files(): List<File> = dir
        .listFiles { f -> f.isFile && f.name.startsWith(PREFIX) && f.name.endsWith(".json") }
        ?.sortedBy { it.name }
        .orEmpty()

    companion object {
        private const val TAG = "CrashReporter"
        const val DIR_NAME = "diagnostics"
        const val PREFIX = "crash-"

        /** Newest [KEEP] crash files survive; older ones are pruned on write. */
        const val KEEP = 5
        private const val MAX_STACK_CHARS = 16_000
    }
}
