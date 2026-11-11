package com.gityou.app.data.telemetry

import android.util.Log
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Product analytics, as minimal as the audit demands (G-017): named events
 * with short string parameters, written by [LocalTelemetry].
 *
 * Deliberately NOT a remote SDK: there is no backend to receive data, so the
 * honest implementation is a local, opt-in journal. A future provider (Sentry,
 * Firebase) can implement this interface without touching call sites.
 */
interface Telemetry {

    /**
     * Records that [name] happened, with optional [params]. Implementations
     * must never throw and must never block the caller.
     */
    fun event(name: String, params: Map<String, String> = emptyMap())

    /**
     * Erases everything this instance retained. Called when the user turns
     * diagnostics off: "off" means "no data retained", not "paused".
     */
    fun wipe()
}

/** A [Telemetry] that does nothing: used while diagnostics are disabled. */
object NoopTelemetry : Telemetry {
    override fun event(name: String, params: Map<String, String>) = Unit
    override fun wipe() = Unit
}

/** One journaled event; the on-disk format is JSON Lines. */
@Serializable
internal data class TelemetryEvent(
    val ts: Long,
    val name: String,
    val params: Map<String, String> = emptyMap(),
)

/**
 * Opt-in, device-local event journal (G-017).
 *
 * Design rules that keep this privacy-safe:
 *  - disabled by default; [enabled] is consulted for EVERY event, so flipping
 *    the switch in Settings takes effect immediately, without a restart;
 *  - the journal file lives in the app's private storage and is only ever
 *    read back through the "clear" action — nothing uploads it;
 *  - turning diagnostics off wipes the file instead of just pausing writes,
 *    so "off" really means "no data retained";
 *  - failures are logged and swallowed: telemetry must never break a feature.
 */
class LocalTelemetry(
    private val dir: File,
    private val enabled: () -> Boolean,
    private val executor: Executor = defaultExecutor(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxBytes: Int = MAX_LOG_BYTES,
) : Telemetry {

    private val json = Json
    private val logFile: File get() = File(dir, LOG_FILE)

    override fun event(name: String, params: Map<String, String>) {
        if (name.isBlank()) return
        if (!enabled()) return
        val record = TelemetryEvent(
            ts = clock(),
            name = name.trim(),
            params = params.filterKeys { it.isNotBlank() }.filterValues { it.isNotBlank() },
        )
        executor.execute {
            runCatching { append(record) }
                .onFailure { Log.w(TAG, "telemetry write failed", it) }
        }
    }

    /** Deletes the journal (called when the user turns diagnostics off). */
    override fun wipe() {
        executor.execute {
            runCatching { logFile.delete() }
                .onFailure { Log.w(TAG, "telemetry wipe failed", it) }
        }
    }

    private fun append(record: TelemetryEvent) {
        dir.mkdirs()
        val lines = if (logFile.exists()) logFile.readLines().filter { it.isNotBlank() } else emptyList()
        val updated = lines + json.encodeToString(TelemetryEvent.serializer(), record)
        // Bound the file: drop the oldest lines when it outgrows [maxBytes].
        var kept = updated
        while (kept.size > 1 && kept.joinToString("\n").length > maxBytes) {
            kept = kept.drop(1)
        }
        logFile.writeText(kept.joinToString("\n") + "\n")
    }

    companion object {
        private const val TAG = "LocalTelemetry"
        const val LOG_FILE = "events.log"

        /** Keeps roughly a week of typical usage; small enough to share freely. */
        const val MAX_LOG_BYTES = 64 * 1024

        private fun defaultExecutor(): Executor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "gityou-telemetry").apply { isDaemon = true }
        }
    }
}
