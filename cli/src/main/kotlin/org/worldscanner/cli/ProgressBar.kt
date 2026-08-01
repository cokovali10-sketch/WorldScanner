package org.worldscanner.cli

/**
 * Draws a single-line `\r`-overwritten progress bar. No-ops when the output is
 * not a terminal (progress would just spam logs).
 */
class ProgressBar(
    private val label: String,
    private val width: Int = 30,
) {
    private var lastLine = ""

    /** Returns true when the bar was actually drawn (i.e. on a terminal). */
    fun draw(done: Long, total: Long): Boolean {
        if (total <= 0 || !Ansi.enabled()) return false
        val percent = ((done.toDouble() / total) * 100).coerceIn(0.0, 100.0)
        val filled = ((percent / 100.0) * width).toInt()
        val bar = (ansiGreen("#".repeat(filled)) + ansiDim("-".repeat(width - filled)))
        val line = "\r${ansiCyan(label)} ${bar} ${String.format("%5.1f%%", percent)} ${done}/${total}"
        System.out.print(line)
        lastLine = line
        return true
    }

    /** Clears the bar, leaving no residue. */
    fun clear() {
        if (lastLine.isEmpty()) return
        System.out.print("\r" + " ".repeat(lastLine.length) + "\r")
        lastLine = ""
    }
}
