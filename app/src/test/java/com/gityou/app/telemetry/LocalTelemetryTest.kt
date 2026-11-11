package com.gityou.app.telemetry

import com.gityou.app.data.telemetry.LocalTelemetry
import com.gityou.app.data.telemetry.TelemetryEvent
import java.io.File
import java.util.concurrent.Executor
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Behaviour of the opt-in local journal (G-017): gated by the enabled flag,
 * JSONL on disk, capped in size, wiped when diagnostics are turned off.
 * Runs on the JVM — the journal is plain file I/O, no Android framework.
 */
class LocalTelemetryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val directExecutor = Executor { it.run() }

    private fun telemetry(dir: File, enabled: Boolean = true, maxBytes: Int = 64 * 1024): LocalTelemetry =
        LocalTelemetry(
            dir = dir,
            enabled = { enabled },
            executor = directExecutor,
            clock = { 1_700_000_000_000 },
            maxBytes = maxBytes,
        )

    private fun logFile(dir: File) = File(dir, LocalTelemetry.LOG_FILE)

    @Test
    fun `disabled gate drops events without creating the file`() {
        val dir = tmp.newFolder("off")
        LocalTelemetry(dir, { false }, directExecutor).event("repo_opened")
        assertFalse(logFile(dir).exists())
    }

    @Test
    fun `enabled gate writes one jsonl line per event`() {
        val dir = tmp.newFolder("on")
        val telemetry = telemetry(dir)
        telemetry.event("repo_opened")
        telemetry.event("search_performed", mapOf("type" to "repos"))

        val lines = logFile(dir).readLines().filter { it.isNotBlank() }
        assertEquals(2, lines.size)
        val first = Json.decodeFromString(TelemetryEvent.serializer(), lines[0])
        assertEquals("repo_opened", first.name)
        val second = Json.decodeFromString(TelemetryEvent.serializer(), lines[1])
        assertEquals(mapOf("type" to "repos"), second.params)
    }

    @Test
    fun `blank names are dropped and blank params are stripped`() {
        val dir = tmp.newFolder("blank")
        val telemetry = telemetry(dir)
        telemetry.event("  ")
        telemetry.event("search_performed", mapOf("type" to " "))

        val lines = logFile(dir).readLines().filter { it.isNotBlank() }
        // The blank-name event is gone; the valid one keeps its (now empty)
        // parameter map so the journal never stores whitespace noise.
        assertEquals(1, lines.size)
        val only = Json.decodeFromString(TelemetryEvent.serializer(), lines[0])
        assertEquals("search_performed", only.name)
        assertTrue(only.params.isEmpty())
    }

    @Test
    fun `journal trims the oldest lines when it outgrows maxBytes`() {
        val dir = tmp.newFolder("cap")
        val telemetry = LocalTelemetry(
            dir = dir,
            enabled = { true },
            executor = directExecutor,
            clock = { 1_700_000_000_000 },
            maxBytes = 200,
        )
        repeat(50) { telemetry.event("event_number_$it") }

        val content = logFile(dir).readText()
        assertTrue(content.length <= 200 + 1)
        // Newest events must survive the trim.
        assertTrue(content.contains("event_number_49"))
        // Oldest ones must be gone.
        assertFalse(content.contains("event_number_0"))
    }

    @Test
    fun `wipe removes the journal even when the gate is already off`() {
        val dir = tmp.newFolder("wipe")
        val telemetry = LocalTelemetry(dir, { false }, directExecutor)
        logFile(dir).writeText("{\"ts\":1,\"name\":\"stale\"}\n")
        telemetry.wipe()
        assertFalse(logFile(dir).exists())
    }
}
