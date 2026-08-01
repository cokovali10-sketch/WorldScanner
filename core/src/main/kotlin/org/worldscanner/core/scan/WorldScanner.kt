package org.worldscanner.core.scan

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.worldscanner.core.anvil.ChunkCompression
import org.worldscanner.core.anvil.RegionDiscovery
import org.worldscanner.core.anvil.RegionFile
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.model.SearchResult
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtReader
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Parallel scanner over `.mca` region files of a world, built on Kotlin
 * Coroutines and [Flow].
 *
 * Region files are partitioned into chunk-range [WorkUnit]s processed by
 * [Dispatchers.Default] workers — at most [parallelism] workers (capped at the
 * number of CPU cores), so only that many region file handles are open at once.
 * A [channelFlow] merges the per-region result streams and exposes them live via
 * [scanFlow]; progress (chunks, files, speed and ETA) is reported through
 * [ScanProgress].
 */
class WorldScanner(
    private val parallelism: Int = Runtime.getRuntime().availableProcessors(),
) : AutoCloseable {

    /**
     * Streams every [SearchResult] as it is found across parallel workers.
     * Collecting the flow is cooperative with cancellation: cancelling the
     * collector stops scanning promptly.
     */
    fun scanFlow(
        worldRoot: Path,
        query: ScanQuery,
        onProgress: ScanProgress? = null,
    ): Flow<SearchResult> = scanFlowInternal(worldRoot, query, onProgress, ScanSession(query))

    /** Runs a full item search and returns the aggregated [ScanReport]. */
    suspend fun scan(
        worldRoot: Path,
        query: ScanQuery,
        onProgress: ScanProgress? = null,
    ): ScanReport {
        val session = ScanSession(query)
        scanFlowInternal(worldRoot, query, onProgress, session).collect { result ->
            session.collector.offer(result)
        }
        return ScanReport(session.collector.results(), session.stats)
    }

    /** Computes world statistics (item / block entity / entity frequency). */
    suspend fun analyze(
        worldRoot: Path,
        onProgress: ScanProgress? = null,
        dimension: DimensionType? = null,
        regionX: Int? = null,
        regionZ: Int? = null,
    ): WorldAnalysisResult {
        val stats = ScanStats()
        val regions = filterRegions(RegionDiscovery.discover(worldRoot), dimension, regionX, regionZ)
        if (regions.isEmpty()) return WorldAnalysisResult.empty()

        val plan = buildWorkUnits(regions)
        if (plan.totalChunks == 0L) return WorldAnalysisResult.empty()

        val accumulator = AnalysisAccumulator()
        val stop = AtomicBoolean(false)
        val tracker = ProgressTracker(plan, onProgress)

        runWorkUnits(plan, workerCount(plan), stats, stop) { unit ->
            analyzeUnit(unit, accumulator, stats, stop, tracker)
        }
        tracker.reportFinal()

        val regionsByDimension = regions.groupBy { it.dimension }.mapValues { it.value.size }
        return accumulator.toResult(regionsByDimension, stats)
    }

    /** Lightweight world inspection without decompressing any chunk. */
    fun describe(worldRoot: Path): WorldSummary {
        val regions = RegionDiscovery.discover(worldRoot)
        var chunks = 0L
        var bytes = 0L
        for (region in regions) {
            RegionFile.open(region.path).use { file ->
                chunks += file.chunkCount()
                bytes += java.nio.file.Files.size(region.path)
            }
        }
        return WorldSummary(regions.size, chunks, bytes, regions.map { it.dimension }.toSet())
    }

    private fun scanFlowInternal(
        worldRoot: Path,
        query: ScanQuery,
        onProgress: ScanProgress?,
        session: ScanSession,
    ): Flow<SearchResult> = channelFlow {
        val regions = discoverAndFilter(worldRoot, query)
        if (regions.isEmpty() || query.isEmpty) return@channelFlow

        val plan = buildWorkUnits(regions)
        if (plan.totalChunks == 0L) return@channelFlow

        val tracker = ProgressTracker(plan, onProgress)
        val scanner = RegionScanner(query, session.stats, session.stop, tracker)

        runWorkUnits(plan, workerCount(plan), session.stats, session.stop) { unit ->
            scanner.scanUnit(unit).collect { result ->
                if (session.stop.get()) return@collect
                send(result)
            }
        }

        tracker.reportFinal()
    }

    private fun analyzeUnit(
        unit: WorkUnit,
        accumulator: AnalysisAccumulator,
        stats: ScanStats,
        stop: AtomicBoolean,
        tracker: ProgressTracker,
    ) {
        if (stop.get()) return
        stats.regionsScanned.incrementAndGet()
        val compression = ChunkCompression()
        try {
            RegionFile.open(unit.region.path).use { file ->
                for (index in unit.chunkIndices) {
                    if (stop.get()) return

                    val payload = try {
                        file.readChunk(index)
                    } catch (e: IOException) {
                        stats.readErrors.incrementAndGet()
                        null
                    } ?: continue

                    val decompressed = try {
                        compression.decompress(payload.compressionType, payload.data)
                    } catch (e: Exception) {
                        stats.decompressionErrors.incrementAndGet()
                        continue
                    }
                    val root = try {
                        NbtReader.read(decompressed)
                    } catch (e: Exception) {
                        stats.nbtErrors.incrementAndGet()
                        continue
                    }

                    ChunkAnalyzer.analyze(root, unit.region.dimension, accumulator)
                    stats.chunksScanned.incrementAndGet()
                    tracker.onChunkDone()
                    tracker.maybeReport()
                }
            }
        } catch (e: IOException) {
            stats.openErrors.incrementAndGet()
        }
        tracker.onUnitDone(unit.region.path)
    }

    /**
     * Runs [block] over all work units with up to [workerCount] parallel
     * coroutines on [Dispatchers.Default]. Units are pulled from a shared atomic
     * counter for balanced load. Cancellation is propagated via [coroutineScope].
     */
    private suspend fun runWorkUnits(
        plan: WorkPlan,
        workerCount: Int,
        stats: ScanStats,
        stop: AtomicBoolean,
        block: suspend (WorkUnit) -> Unit,
    ) {
        val nextUnit = AtomicInteger()
        coroutineScope {
            val workers = (0 until workerCount).map {
                async(Dispatchers.Default) {
                    while (!stop.get()) {
                        val index = nextUnit.getAndIncrement()
                        if (index >= plan.units.size) return@async
                        try {
                            block(plan.units[index])
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            stats.fatalErrors.incrementAndGet()
                        }
                    }
                }
            }
            workers.forEach { worker ->
                try {
                    worker.await()
                } catch (e: CancellationException) {
                    stop.set(true)
                    throw e
                }
            }
        }
    }

    private fun workerCount(plan: WorkPlan): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return minOf(parallelism.coerceAtLeast(1), cores, plan.units.size)
    }

    private fun buildWorkUnits(regions: List<RegionDiscovery.RegionEntry>): WorkPlan {
        val units = ArrayList<WorkUnit>()
        var total = 0L
        val partitionCount = minOf(parallelism.coerceAtLeast(1), Runtime.getRuntime().availableProcessors())
        for (region in regions) {
            val present = RegionFile.open(region.path).use { file ->
                (0 until RegionFile.CHUNK_COUNT).filter { file.hasChunk(it) }
            }
            total += present.size
            if (present.isEmpty()) continue
            for (partition in partition(present, partitionCount)) {
                units += WorkUnit(region, partition.toIntArray())
            }
        }
        return WorkPlan(units, total)
    }

    private fun discoverAndFilter(worldRoot: Path, query: ScanQuery): List<RegionDiscovery.RegionEntry> =
        filterRegions(RegionDiscovery.discover(worldRoot), query.dimension, query.regionX, query.regionZ)

    private fun filterRegions(
        regions: List<RegionDiscovery.RegionEntry>,
        dimension: DimensionType?,
        regionX: Int?,
        regionZ: Int?,
    ): List<RegionDiscovery.RegionEntry> = regions.filter { region ->
        (dimension == null || region.dimension == dimension) &&
            (regionX == null || region.regionX == regionX) &&
            (regionZ == null || region.regionZ == regionZ)
    }

    override fun close() {
        // No native resources are held by this scanner (coroutines manage
        // workers); kept AutoCloseable for API compatibility with `.use {}`.
    }
}

/** Shared mutable state for one scan run. */
internal class ScanSession(query: ScanQuery) {
    val stats = ScanStats()
    val stop = AtomicBoolean(false)
    val collector = ResultCollector(query.limit, stop)
}

/** Aggregated counters for one scan run. */
class ScanStats {
    val regionsScanned = AtomicInteger()
    val chunksScanned = AtomicInteger()
    val readErrors = AtomicInteger()
    val openErrors = AtomicInteger()
    val decompressionErrors = AtomicInteger()
    val nbtErrors = AtomicInteger()
    val fatalErrors = AtomicInteger()
}

/** Result of a scan run. */
data class ScanReport(
    val results: List<SearchResult>,
    val stats: ScanStats,
)

/** Summary produced by [WorldScanner.describe]. */
data class WorldSummary(
    val regionCount: Int,
    val chunkCount: Long,
    val totalBytes: Long,
    val dimensions: Set<DimensionType>,
)

/** Thread-safe bounded result buffer shared by chunk workers. */
internal class ResultCollector(
    private val limit: Int,
    private val stop: AtomicBoolean,
) {
    private val results = ArrayList<SearchResult>()
    private val lock = Any()

    fun offer(result: SearchResult): Boolean {
        if (stop.get()) return false
        synchronized(lock) {
            if (results.size >= limit) {
                stop.set(true)
                return false
            }
            results += result
            return results.size < limit
        }
    }

    fun results(): List<SearchResult> = synchronized(lock) { results.toList() }
}

/** Splits [items] into at most [partitions] contiguous, balanced groups. */
internal fun <T> partition(items: List<T>, partitions: Int): List<List<T>> {
    if (items.isEmpty() || partitions <= 1) return listOf(items)
    val target = minOf(partitions, items.size)
    val groupSize = (items.size + target - 1) / target
    return items.chunked(groupSize)
}
