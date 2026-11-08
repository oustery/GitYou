package com.gityou.app.data.util

/**
 * Parses the unified diff `patch` field that GitHub embeds into commit files
 * into renderable lines. Pure Kotlin so the whole grammar is unit-tested.
 */
object DiffParser {

    enum class LineType { ADDED, REMOVED, CONTEXT, HUNK }

    /** One rendered diff row: its kind, raw text and (for context lines) both numbers. */
    data class DiffLine(
        val type: LineType,
        val text: String,
        val oldNumber: Int? = null,
        val newNumber: Int? = null,
    )

    /**
     * Parses a single file's patch. Unknown line shapes fall back to CONTEXT so
     * a malformed patch never crashes the diff viewer.
     */
    fun parse(patch: String): List<DiffLine> {
        val result = mutableListOf<DiffLine>()
        var oldNo = 0
        var newNo = 0

        for (raw in patch.lines()) {
            when {
                raw.startsWith("@@") -> {
                    val header = parseHunkHeader(raw)
                    oldNo = header.first
                    newNo = header.second
                    result += DiffLine(LineType.HUNK, raw)
                }
                raw.startsWith("+") -> {
                    result += DiffLine(LineType.ADDED, raw, newNumber = newNo)
                    newNo++
                }
                raw.startsWith("-") -> {
                    result += DiffLine(LineType.REMOVED, raw, oldNumber = oldNo)
                    oldNo++
                }
                raw.startsWith(" ") -> {
                    result += DiffLine(LineType.CONTEXT, raw, oldNumber = oldNo, newNumber = newNo)
                    oldNo++
                    newNo++
                }
                raw.startsWith("\\") -> {
                    // "\ No newline at end of file" — render as-is, no numbering.
                    result += DiffLine(LineType.CONTEXT, raw)
                }
                raw.isEmpty() && result.isNotEmpty() -> {
                    // Trailing blank line inside a patch: treat as empty context.
                    result += DiffLine(LineType.CONTEXT, "", oldNumber = oldNo, newNumber = newNo)
                    oldNo++
                    newNo++
                }
            }
        }
        return result
    }

    /** Extracts (oldStart, newStart) from "@@ -a,b +c,d @@ ..."; defaults keep parsing alive. */
    fun parseHunkHeader(header: String): Pair<Int, Int> {
        val old = Regex("-\\d+").find(header)?.value?.drop(1)?.toIntOrNull() ?: 1
        val new = Regex("\\+\\d+").find(header)?.value?.drop(1)?.toIntOrNull() ?: 1
        return old to new
    }
}
