package org.worldscanner.core

import kotlinx.coroutines.flow.Flow
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
 * val result = runBlocking { SearchEngine.find(worldRoot, ScanQuery(itemTargets = setOf("minecraft:diamond"))) }
 * val stats = runBlocking { SearchEngine.analyze(worldRoot) }
 * ```
 *
 * Both [find] and [analyze] are `suspend` and run the parallel scan on
 * [kotlinx.coroutines.Dispatchers.Default]. Use [findFlow] to consume results as
 * a live [Flow].
 */
object SearchEngine {

    private const val DEFAULT_PARALLELISM = 0 // 0 == auto (availableProcessors)

    private fun scanner(parallelism: Int = DEFAULT_PARALLELISM): WorldScanner =
        WorldScanner(if (parallelism <= 0) Runtime.getRuntime().availableProcessors() else parallelism)

    /** Streams every matching item as it is found across parallel workers. */
    fun findFlow(
        worldRoot: Path,
        query: ScanQuery,
        parallelism: Int = DEFAULT_PARALLELISM,
        onProgress: ScanProgress? = null,
    ): Flow<SearchResult> = scanner(parallelism).scanFlow(worldRoot, query, onProgress)

    /** Searches a world for the given items. */
    suspend fun find(
        worldRoot: Path,
        query: ScanQuery,
        parallelism: Int = DEFAULT_PARALLELISM,
        onProgress: ScanProgress? = null,
    ): ScanReport = scanner(parallelism).scan(worldRoot, query, onProgress)

    /** Gathers aggregate statistics about a world. */
    suspend fun analyze(
        worldRoot: Path,
        parallelism: Int = DEFAULT_PARALLELISM,
        onProgress: ScanProgress? = null,
        dimension: DimensionType? = null,
        regionX: Int? = null,
        regionZ: Int? = null,
    ): WorldAnalysisResult = scanner(parallelism).analyze(worldRoot, onProgress, dimension, regionX, regionZ)

    /** Lightweight world metadata (regions, chunks, bytes, dimensions). */
    fun describe(worldRoot: Path): WorldSummary = scanner(1).describe(worldRoot)
}
