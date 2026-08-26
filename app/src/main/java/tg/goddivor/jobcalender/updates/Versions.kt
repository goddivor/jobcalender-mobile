package tg.goddivor.jobcalender.updates

/**
 * Compares versions by segment, never as strings: "1.2.10" is newer than "1.2.9", which a string
 * comparison gets backwards. A missing segment counts as zero, so "1.2" and "1.2.0" are equal.
 */
fun compareVersions(left: String, right: String): Int {
    val a = left.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
    val b = right.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(a.size, b.size)) {
        val first = a.getOrElse(index) { 0 }
        val second = b.getOrElse(index) { 0 }
        if (first != second) return if (first > second) 1 else -1
    }
    return 0
}

/**
 * GitHub release bodies are Markdown. Strip the syntax rather than render it: the dialog is three
 * lines tall and a stray "###" reads worse than plain prose.
 */
fun markdownToText(markdown: String): String = markdown
    .lineSequence()
    .map { line ->
        line.trim()
            .replace(Regex("^#{1,6}\\s*"), "")
            .replace(Regex("^[-*+]\\s+"), "• ")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("\\*(.+?)\\*"), "$1")
            .replace(Regex("`(.+?)`"), "$1")
            .replace(Regex("\\[(.+?)]\\((.+?)\\)"), "$1")
    }
    .fold(mutableListOf<String>()) { lines, line ->
        // A body wrapped at some column arrives with hard newlines inside its sentences. Rejoin a
        // continuation with the line it belongs to, so a phone reflows it at its own width. A blank
        // line still ends the paragraph, and a bullet always starts one.
        val previous = lines.lastOrNull()
        when {
            line.isBlank() -> if (previous != null && previous.isNotBlank()) lines.add("")
            line.startsWith("• ") || previous.isNullOrBlank() -> lines.add(line)
            else -> lines[lines.lastIndex] = "$previous $line"
        }
        lines
    }
    .filter { it.isNotBlank() }
    .joinToString("\n")
    .trim()
