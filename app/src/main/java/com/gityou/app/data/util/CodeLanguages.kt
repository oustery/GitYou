package com.gityou.app.data.util

/**
 * Maps file names to Prism4j language ids for the syntax-highlighted file
 * viewer, and builds a markdown fence around raw code so the existing
 * markdown pipeline can render it.
 */
object CodeLanguages {

    private val BY_EXTENSION: Map<String, String> = mapOf(
        "kt" to "kotlin", "kts" to "kotlin",
        "java" to "java",
        "py" to "python",
        "js" to "javascript", "mjs" to "javascript", "cjs" to "javascript",
        "ts" to "typescript", "tsx" to "typescript",
        "json" to "json",
        "xml" to "markup", "html" to "markup", "svg" to "markup", "xaml" to "markup",
        "yml" to "yaml", "yaml" to "yaml",
        "sh" to "bash", "bash" to "bash", "zsh" to "bash",
        "c" to "c", "h" to "c",
        "cpp" to "cpp", "cc" to "cpp", "hpp" to "cpp",
        "cs" to "csharp",
        "go" to "go",
        "rs" to "rust",
        "rb" to "ruby",
        "php" to "php",
        "sql" to "sql",
        "swift" to "swift",
        "dart" to "dart",
        "gradle" to "groovy", "groovy" to "groovy", "jenkinsfile" to "groovy",
        "css" to "css", "scss" to "css", "less" to "css",
        "properties" to "properties", "ini" to "properties",
        "toml" to "toml",
        "md" to "markdown",
    )

    /** Prism4j language id for [fileName], or null when highlighting is unknown. */
    fun languageOf(fileName: String): String? {
        val name = fileName.substringAfterLast('/')
        // Build files without an extension are still groovy.
        if (!name.contains('.')) {
            return if (name.equals("Jenkinsfile", ignoreCase = true) ||
                name.equals("gradlew", ignoreCase = true)
            ) {
                "groovy"
            } else {
                null
            }
        }
        return BY_EXTENSION[name.substringAfterLast('.').lowercase()]
    }

    fun isMarkdown(fileName: String): Boolean =
        fileName.substringAfterLast('/').substringAfterLast('.', "").lowercase() == "md"

    private const val FENCE = "````"

    /**
     * Wraps [code] in a four-backtick fence tagged with [language]. A 4-fence
     * is safe against every 3-backtick block inside the source; runs of 4+
     * backticks (never seen in real code) get a zero-width space inserted.
     */
    fun wrapInCodeFence(language: String?, code: String): String {
        val safe = code.replace("````", "```\u200b`")
        return "$FENCE${language.orEmpty()}\n$safe\n$FENCE"
    }
}
