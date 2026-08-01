package org.worldscanner.cli

import org.worldscanner.core.SearchEngine
import org.worldscanner.core.filter.ComponentItemMatcher
import org.worldscanner.core.filter.ItemFilterParser
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.scan.ScanQuery
import java.nio.file.Files
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isNotEmpty() && (args[0] == "--version" || args[0] == "-V" || args[0] == "version")) {
        println("WorldScanner ${version()}")
        return
    }

    val parsed = CliArgsParser.parse(args)
    if (parsed == null) {
        CliArgsParser.printUsage()
        exitProcess(1)
    }

    Ansi.configure(parsed.color)
    val progress = parsed.progress ?: Ansi.enabled()

    when (parsed.command) {
        CliArgs.Command.INFO -> runInfo(parsed)
        CliArgs.Command.FIND -> runFind(parsed, progress)
        CliArgs.Command.STATS -> runStats(parsed, progress)
    }
}

private fun version(): String =
    Ansi::class.java.`package`.implementationVersion ?: "2.0.0"

private fun requireWorld(parsed: CliArgs): java.nio.file.Path {
    if (!Files.isDirectory(parsed.worldPath)) {
        System.err.println(ansiRed("Error: world path is not a directory: ${parsed.worldPath}"))
        exitProcess(1)
    }
    return parsed.worldPath
}

private fun runInfo(parsed: CliArgs) {
    val world = requireWorld(parsed)
    val summary = SearchEngine.describe(world)

    println(ansiBold("World information"))
    println("  Path       : ${ansiCyan(world.toString())}")
    println("  Region files: ${summary.regionCount}")
    println("  Chunks     : ${summary.chunkCount}")
    println("  Data size  : ${formatBytes(summary.totalBytes)}")
    println("  Dimensions : ${summary.dimensions.joinToString { ansiColorDim(it) }}")
}

private fun runStats(parsed: CliArgs, progress: Boolean) {
    val world = requireWorld(parsed)
    val started = System.nanoTime()
    val bar = ProgressBar("Scanning")
    val result = SearchEngine.analyze(
        worldRoot = world,
        parallelism = parsed.threads ?: 0,
        onProgress = progressBar(bar, progress),
        dimension = parseDimension(parsed.dimension),
        regionX = parsed.regionX,
        regionZ = parsed.regionZ,
    )
    bar.clear()
    val elapsedMs = (System.nanoTime() - started) / 1_000_000

    println()
    println(ansiBold("World statistics"))
    result.dataVersionRange?.let { (min, max) ->
        val label = if (min == max) describeDataVersion(min) else "${describeDataVersion(min)} .. ${describeDataVersion(max)}"
        println("  Version  : ${ansiCyan(label)} (DataVersion $min..$max)")
    }
    println("  Regions  : ${result.regionsByDimension.entries.joinToString(", ") { (dim, count) -> "${ansiColorDim(dim)}: $count" }}")
    println("  Chunks   : ${result.chunksByDimension.entries.joinToString(", ") { (dim, count) -> "${ansiColorDim(dim)}: $count" }}")
    println("  Time     : ${formatMs(elapsedMs)}")

    println(ansiBold("\n  Top items (total count)"))
    if (result.itemsByType.isEmpty()) {
        println("    (none found)")
    } else {
        val maxName = result.itemsByType.maxOf { it.first.removePrefix("minecraft:").length }
        for ((id, count) in result.itemsByType.take(50)) {
            val name = id.removePrefix("minecraft:").padEnd(maxName)
            val bar = bar(count.toDouble(), result.itemsByType.first().second.toDouble(), 24)
            println("    ${ansiCyan(name)} ${count.toString().padStart(10)}  $bar")
        }
        if (result.itemsByType.size > 50) {
            println(ansiDim("    ... and ${result.itemsByType.size - 50} more"))
        }
    }

    println(ansiBold("\n  Block entities"))
    for ((id, count) in result.blockEntitiesByType.toList().sortedByDescending { it.second }.take(20)) {
        println("    ${ansiYellow(id.removePrefix("minecraft:"))}: $count")
    }

    println(ansiBold("\n  Entities"))
    for ((id, count) in result.entitiesByType.toList().sortedByDescending { it.second }.take(20)) {
        println("    ${ansiYellow(id.removePrefix("minecraft:"))}: $count")
    }
    printErrors(result.scanStats)
}

private fun runFind(parsed: CliArgs, progress: Boolean) {
    val world = requireWorld(parsed)

    val matcher = parsed.filter?.let { raw ->
        val result = ItemFilterParser.parse(raw)
        if (result.isFailure) {
            System.err.println(ansiRed("Error: invalid --filter: ${result.exceptionOrNull()?.message}"))
            exitProcess(1)
        }
        ComponentItemMatcher(result.getOrThrow())
    }

    val query = ScanQuery(
        itemTargets = parsed.itemTargets.toSet(),
        matcher = matcher,
        dimension = parseDimension(parsed.dimension),
        regionX = parsed.regionX,
        regionZ = parsed.regionZ,
        limit = parsed.limit ?: Int.MAX_VALUE,
    )

    val scanLabel = if (matcher != null) {
        "'${parsed.filter}'"
    } else {
        parsed.itemTargets.joinToString(", ") { it.removePrefix("minecraft:") }
    }
    println("Scanning $scanLabel in $world ...")

    val started = System.nanoTime()
    val bar = ProgressBar("Scanning")
    val report = SearchEngine.find(
        worldRoot = world,
        query = query,
        parallelism = parsed.threads ?: 0,
        onProgress = progressBar(bar, progress),
    )
    bar.clear()
    val elapsedMs = (System.nanoTime() - started) / 1_000_000

    val results = report.results

    if (parsed.jsonPath != null) {
        JsonExporter.export(results, parsed.jsonPath)
        println("Exported ${results.size} result(s) to ${parsed.jsonPath}")
    }
    if (parsed.csvPath != null) {
        CsvExporter.export(results, parsed.csvPath)
        println("Exported ${results.size} result(s) to ${parsed.csvPath}")
    }

    println()
    if (parsed.summary) {
        println(ansiBold("Found ${results.size} result(s) in ${formatMs(elapsedMs)}"))
        for (result in results.take(20)) {
            val holder = result.source.run {
                when (this) {
                    is org.worldscanner.core.model.ItemSource.BlockEntity -> "${ansiGreen("block")} $id @ $pos"
                    is org.worldscanner.core.model.ItemSource.Entity -> "${ansiGreen("entity")} $id @ ${pos ?: "-"}"
                }
            }
            val nested = if (result.containerPath.isEmpty()) "" else ansiYellow(" in ${result.containerPath.joinToString(">") { it.removePrefix("minecraft:") }}")
            println("  ${ansiCyan(result.item.normalizedId)} x${result.item.count} - $holder$nested")
        }
        if (results.size > 20) println(ansiDim("  ... and ${results.size - 20} more (use --json/--csv to export all)"))
    } else {
        val rows = ReportRenderer.render(results)
        if (rows.isEmpty()) {
            println(ansiYellow("No matching items found."))
        } else {
            rows.forEach(::println)
            println("")
        }
    }
    printErrors(report.stats)
}

private fun printErrors(stats: org.worldscanner.core.scan.ScanStats) {
    val errors = stats.readErrors.get() + stats.openErrors.get() + stats.decompressionErrors.get() + stats.nbtErrors.get()
    if (errors > 0) {
        println(
            ansiYellow(
                "Warnings: $errors chunk(s) skipped (read=${stats.readErrors.get()}, open=${stats.openErrors.get()}, decompress=${stats.decompressionErrors.get()}, nbt=${stats.nbtErrors.get()})",
            ),
        )
    }
}

private fun progressBar(bar: ProgressBar, enabled: Boolean): org.worldscanner.core.scan.ScanProgress {
    if (!enabled) return org.worldscanner.core.scan.ScanProgress { _, _ -> }
    return org.worldscanner.core.scan.ScanProgress { done, total ->
        bar.draw(done, total)
    }
}

private fun bar(value: Double, max: Double, width: Int): String {
    if (max <= 0) return "".padEnd(width, ' ')
    val filled = ((value / max) * width).toInt()
    return (ansiGreen("#".repeat(filled)) + ansiDim("-".repeat(width - filled)))
}

private fun parseDimension(raw: String?): DimensionType? = when (raw) {
    null -> null
    "overworld", "world" -> DimensionType.OVERWORLD
    "nether", "dim-1" -> DimensionType.NETHER
    "end", "the_end", "dim1" -> DimensionType.END
    else -> null
}

/** Maps a chunk `DataVersion` to a human-readable Minecraft version. */
private fun describeDataVersion(version: Int): String {
    val known = listOf(
        3700 to "1.20.2",
        4082 to "1.21.2",
        4189 to "1.21.4",
        4671 to "1.21.11",
        4786 to "26.1",
        4790 to "26.1.2",
        4883 to "26.2 snapshot 1",
        4903 to "26.2",
        5003 to "26.3 snapshot",
    ).sortedBy { it.first }

    known.firstOrNull { it.first == version }?.let { return it.second }
    val floor = known.lastOrNull { it.first < version }
    val ceil = known.firstOrNull { it.first > version }
    return when {
        floor == null && ceil == null -> "unknown (DV $version)"
        floor == null -> "newer than ${ceil!!.second} (DV $version)"
        ceil == null -> "older than ${floor!!.second} (DV $version)"
        else -> "between ${floor.second} and ${ceil.second} (DV $version)"
    }
}

private fun ansiColorDim(dimension: DimensionType): String = when (dimension) {
    DimensionType.OVERWORLD -> ansiGreen("overworld")
    DimensionType.NETHER -> ansiRed("nether")
    DimensionType.END -> ansiYellow("the_end")
    DimensionType.UNKNOWN -> ansiDim("unknown")
}

private fun formatMs(ms: Long): String {
    if (ms < 1000) return "${ms} ms"
    return String.format("%.2f s", ms / 1000.0)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KiB", "MiB", "GiB", "TiB")
    var value = bytes.toDouble()
    var unit = "B"
    for (candidate in units) {
        value /= 1024.0
        unit = candidate
        if (value < 1024) break
    }
    return String.format("%.1f %s", value, unit)
}
