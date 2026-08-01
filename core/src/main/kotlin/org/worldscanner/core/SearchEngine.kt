package org.worldscanner.core

import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.model.SearchResult
import org.worldscanner.core.scan.ScanProgress
import org.worldscanner.core.scan.ScanQuery
import org.worldscanner.core.scan.ScanReport
import org.worldscanner.core.scan.WorldAnalysisResult
import org.worldscanner.core.scan.WorldScanner
import org.worldscanner.core.scan.WorldSummary
import java.nio.file.Path

/**
 * High-level facade over the WorldScanner library.
 *
 * ```kotlin
 * val result = SearchEngine.find(worldRoot, ScanQuery(itemTargets = setOf("minecraft:diamond")))
 * val stats = SearchEngine.analyze(worldRoot)
 * ```
 */
object SearchEngine {

    private const val DEFAULT_PARALLELISM = 0 // 0 == auto (availableProcessors)

    private fun scanner(parallelism: Int = DEFAULT_PARALLELISM): WorldScanner =
        WorldScanner(if (parallelism <= 0) Runtime.getRuntime().availableProcessors() else parallelism)

    /** Searches a world for the given items. */
    fun find(
        worldRoot: Path,
        query: ScanQuery,
        parallelism: Int = DEFAULT_PARALLELISM,
        onProgress: ScanProgress? = null,
    ): ScanReport = scanner(parallelism).use { it.scan(worldRoot, query, onProgress) }

    /** Gathers aggregate statistics about a world. */
    fun analyze(
        worldRoot: Path,
        parallelism: Int = DEFAULT_PARALLELISM,
        onProgress: ScanProgress? = null,
        dimension: DimensionType? = null,
        regionX: Int? = null,
        regionZ: Int? = null,
    ): WorldAnalysisResult = scanner(parallelism).use {
        it.analyze(worldRoot, onProgress, dimension, regionX, regionZ)
    }

    /** Lightweight world metadata (regions, chunks, bytes, dimensions). */
    fun describe(worldRoot: Path): WorldSummary = scanner(1).use { it.describe(worldRoot) }
}
