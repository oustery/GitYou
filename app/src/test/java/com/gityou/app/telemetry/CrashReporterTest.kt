package com.gityou.app.telemetry

import com.gityou.app.data.telemetry.CrashReporter
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Local crash capture (G-017): records an exception, chains to the previous
 * handler, keeps only the newest [CrashReporter.KEEP] files and can clear them.
 */
class CrashReporterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun reporter(dir: File, clock: () -> Long): CrashReporter =
        CrashReporter(dir = dir, version = "1.2.3", clock = clock)

    @Test
    fun `record writes a parsable json report`() {
        val dir = tmp.newFolder("crash")
        reporter(dir, clock = { 1_500L }).record(Thread.currentThread(), IllegalStateException("boom at the seam"))

        val file = reporter(dir, clock = { 0L }).latest()
        assertNotNull(file)
        val json = Json.parseToJsonElement(file!!.readText()).jsonObject
        assertEquals("java.lang.IllegalStateException", json["exception"]!!.jsonPrimitive.content)
        assertEquals("boom at the seam", json["message"]!!.jsonPrimitive.content)
        assertEquals("1.2.3", json["version"]!!.jsonPrimitive.content)
        assertEquals(1_500L, json["ts"]!!.jsonPrimitive.content.toLong())
    }

    @Test
    fun `install chains to the previous uncaught handler`() {
        val dir = tmp.newFolder("chain")
        val delegated = AtomicInteger(0)
        val original = Thread.getDefaultUncaughtExceptionHandler()
        try {
            Thread.setDefaultUncaughtExceptionHandler { _, _ -> delegated.incrementAndGet() }
            val rep = reporter(dir, clock = { 0L })
            rep.install()
            Thread.getDefaultUncaughtExceptionHandler()!!
                .uncaughtException(Thread.currentThread(), IllegalStateException("boom"))

            assertEquals(1, delegated.get())
            assertEquals(1, rep.count())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original)
        }
    }

    @Test
    fun `only the newest KEEP reports survive`() {
        val dir = tmp.newFolder("prune")
        var now = 0L
        val rep = reporter(dir, clock = { now })
        repeat(CrashReporter.KEEP + 3) {
            now += 1_000
            rep.record(Thread.currentThread(), IllegalStateException("boom #$now"))
        }

        assertEquals(CrashReporter.KEEP, rep.count())
        val newestName = rep.latest()!!.name
        assertEquals("crash-$now.json", newestName)
        // The oldest ones are gone.
        val remaining = dir.listFiles { f -> f.name.startsWith(CrashReporter.PREFIX) }!!.map { it.name }
        assertFalse(remaining.contains("crash-1000.json"))
    }

    @Test
    fun `clear removes everything`() {
        val dir = tmp.newFolder("clear")
        val rep = reporter(dir, clock = { 0L })
        rep.record(Thread.currentThread(), IllegalStateException("boom"))
        rep.record(Thread.currentThread(), IllegalStateException("boom again"))
        rep.clear()
        assertEquals(0, rep.count())
        assertNull(rep.latest())
    }
}
