package org.worldscanner.cli

import java.io.PrintStream

/**
 * Minimal ANSI helpers. Color is disabled automatically when the output is not
 * a terminal, or explicitly via [--no-color] / the `NO_COLOR` environment
 * variable (https://no-color.org).
 */
object Ansi {

    enum class Style {
        RESET, BOLD, DIM, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN
    }

    private val codes = mapOf(
        Style.RESET to "\u001B[0m",
        Style.BOLD to "\u001B[1m",
        Style.DIM to "\u001B[2m",
        Style.RED to "\u001B[31m",
        Style.GREEN to "\u001B[32m",
        Style.YELLOW to "\u001B[33m",
        Style.BLUE to "\u001B[34m",
        Style.MAGENTA to "\u001B[35m",
        Style.CYAN to "\u001B[36m",
    )

    private val force = java.lang.System.getenv("WS_FORCE_COLOR")?.equals("1", ignoreCase = true) == true

    private val autoEnabled: Boolean =
        force ||
            (java.lang.System.console() != null &&
                java.lang.System.getenv("NO_COLOR").isNullOrEmpty())

    @Volatile
    private var override: Boolean? = null

    /** Lets callers override detection, e.g. from `--color` / `--no-color`. */
    fun configure(forced: Boolean?) {
        override = forced
    }

    /** Resolved color mode for this process. */
    fun enabled(): Boolean = override ?: autoEnabled

    /** Applies the style sequence; falls back to plain text when disabled. */
    fun paint(style: Style, text: String): String =
        if (enabled()) "${codes[style]}$text${codes[Style.RESET]}" else text
}

/** Convenience accessors so callers read `Ansi.bold(...)`. */
internal fun ansiBold(text: String) = Ansi.paint(Ansi.Style.BOLD, text)
internal fun ansiDim(text: String) = Ansi.paint(Ansi.Style.DIM, text)
internal fun ansiRed(text: String) = Ansi.paint(Ansi.Style.RED, text)
internal fun ansiGreen(text: String) = Ansi.paint(Ansi.Style.GREEN, text)
internal fun ansiYellow(text: String) = Ansi.paint(Ansi.Style.YELLOW, text)
internal fun ansiCyan(text: String) = Ansi.paint(Ansi.Style.CYAN, text)
