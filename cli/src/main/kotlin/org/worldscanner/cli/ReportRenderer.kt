package org.worldscanner.cli

import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.model.ItemSource
import org.worldscanner.core.model.SearchResult

/**
 * Plain-text rendering of scan results. Uses ANSI colors when the terminal
 * supports them (see [Ansi]).
 */
object ReportRenderer {

    fun render(results: List<SearchResult>): List<String> {
        if (results.isEmpty()) return emptyList()

        val rawRows = results.mapIndexed { index, result -> rowOf(index + 1, result) }
        val widths = IntArray(9)
        for (row in rawRows) {
            for (i in row.indices) widths[i] = maxOf(widths[i], row[i].length)
        }
        val header = listOf("$", "item", "count", "dimension", "region", "chunk", "pos", "in", "holder")
        return buildList {
            add(ansiBold(formatRow(header, widths)))
            add(ansiDim(formatRow(header.map { "-".repeat(it.length) }, widths)))
            for (row in rawRows) add(formatRow(row.toList(), widths))
        }
    }

    private fun rowOf(index: Int, result: SearchResult): Array<String> {
        val source = result.source
        val position = when (source) {
            is ItemSource.BlockEntity -> source.pos.toString()
            is ItemSource.Entity -> source.pos?.toString() ?: "-"
        }
        val holder = when (source) {
            is ItemSource.BlockEntity -> source.id
            is ItemSource.Entity -> source.id
        }
        return arrayOf(
            index.toString(),
            result.item.normalizedId,
            result.item.count.toString(),
            result.dimension.name.lowercase(),
            "${result.regionX},${result.regionZ}",
            "${result.chunkX},${result.chunkZ}",
            position,
            if (result.containerPath.isEmpty()) "-" else result.containerPath.joinToString(">") { it.removePrefix("minecraft:") },
            holder.removePrefix("minecraft:"),
        )
    }

    private fun formatRow(row: List<String>, widths: IntArray): String {
        val painted = row.mapIndexed { i, cell -> paintCell(i, cell.padEnd(widths[i])) }
        return painted.joinToString("  ").trimEnd()
    }

    private fun paintCell(column: Int, text: String): String = when (column) {
        1 -> ansiCyan(text)
        3 -> colorDimension(text)
        4 -> ansiDim(text)
        5 -> ansiDim(text)
        6 -> ansiDim(text)
        7 -> if (text == "-") text else ansiYellow(text)
        8 -> ansiGreen(text)
        else -> text
    }

    private fun colorDimension(raw: String): String = when (raw) {
        "overworld" -> ansiGreen(raw)
        "nether" -> ansiRed(raw)
        "the_end" -> ansiYellow(raw)
        else -> ansiDim(raw)
    }
}
