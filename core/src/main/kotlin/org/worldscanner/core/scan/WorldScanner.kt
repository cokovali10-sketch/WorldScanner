package org.worldscanner.core.scan

import org.worldscanner.core.anvil.ChunkCompression
import org.worldscanner.core.anvil.RegionDiscovery
import org.worldscanner.core.anvil.RegionFile
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.model.SearchResult
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtReader
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Reports scan progress. [chunksDone] counts chunks actually parsed; [totalChunks]
 * is the number of chunk slots present across all region files (known upfront).
 */
fun interface ScanProgress {
    fun onProgress(chunksDone: Long, totalChunks: Long)
}

/**
 * Processes one decompressed and parsed chunk. Return false to abort the scan.
 */
fun interface ChunkVisitor {
    fun visit(root: NbtCompound, region: RegionDiscovery.RegionEntry, chunkX: Int, chunkZ: Int): Boolean
}

/**
 * Parallel scanner over `.mca` region files of a world.
 *
 * Region files are partitioned into chunk-range work units and processed on a
 * fixed thread pool. Each work unit owns its region file handle, so at most
 * [parallelism] handles are open at any moment. Decompression buffers are
 * thread-local to keep GC pressure low.
 */
class WorldScanner(
    private val parallelism: Int = Runtime.getRuntime().availableProcessors(),
) : AutoCloseable {

    private data class WorkUnit(
        val region: RegionDiscovery.RegionEntry,
        val chunkIndices: IntArray,
    )

    private class WorkPlan(val units: List<WorkUnit>, val totalChunks: Long)

    private val executor = Executors.newFixedThreadPool(parallelism)
    private val compression = ThreadLocal.withInitial { ChunkCompression() }

    /** Runs an item search over the world. Results are gathered in scan order. */
    fun scan(worldRoot: Path, query: ScanQuery, onProgress: ScanProgress? = null): ScanReport {
        val stats = ScanStats()
        val regions = discoverAndFilter(worldRoot, query)
        if (regions.isEmpty() || query.itemTargets.isEmpty()) return ScanReport(emptyList(), stats)

        val stop = AtomicBoolean(false)
        val collector = ResultCollector(query.limit, stop)
        val sink = ResultSink { result -> collector.offer(result) }
        val chunkScanner = ChunkScanner(query)
        val visitor = ChunkVisitor { root, region, chunkX, chunkZ ->
            chunkScanner.scanChunk(root, region.dimension, region.regionX, region.regionZ, chunkX, chunkZ, sink)
        }
        runParallel(regions, visitor, stats, stop, onProgress)
        return ScanReport(collector.results(), stats)
    }

    /** Computes world statistics (item / block entity / entity frequency). */
    fun analyze(
        worldRoot: Path,
        onProgress: ScanProgress? = null,
        dimension: DimensionType? = null,
        regionX: Int? = null,
        regionZ: Int? = null,
    ): WorldAnalysisResult {
        val stats = ScanStats()
        val regions = filterRegions(RegionDiscovery.discover(worldRoot), dimension, regionX, regionZ)
        if (regions.isEmpty()) return WorldAnalysisResult.empty()

        val accumulator = AnalysisAccumulator()
        val stop = AtomicBoolean(false)
        val visitor = ChunkVisitor { root, region, _, _ ->
            ChunkAnalyzer.analyze(root, region.dimension, accumulator)
            true
        }
        runParallel(regions, visitor, stats, stop, onProgress)

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

    private fun runParallel(
        regions: List<RegionDiscovery.RegionEntry>,
        visitor: ChunkVisitor,
        stats: ScanStats,
        stop: AtomicBoolean,
        onProgress: ScanProgress?,
    ) {
        val plan = buildWorkUnits(regions)
        if (plan.totalChunks == 0L) return
        val progress = ProgressState(plan.totalChunks, onProgress)

        val futures = plan.units.map { unit ->
            executor.submit { scanWorkUnit(unit, visitor, stats, stop, progress) }
        }
        futures.forEach { future ->
            try {
                future.get()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                stop.set(true)
            } catch (e: Exception) {
                stats.fatalErrors.incrementAndGet()
            }
        }
        progress.report(force = true)
    }

    private fun buildWorkUnits(regions: List<RegionDiscovery.RegionEntry>): WorkPlan {
        val units = ArrayList<WorkUnit>()
        var total = 0L
        for (region in regions) {
            val present = RegionFile.open(region.path).use { file ->
                (0 until RegionFile.CHUNK_COUNT).filter { file.hasChunk(it) }
            }
            total += present.size
            if (present.isEmpty()) continue
            for (partition in partition(present, parallelism)) {
                units += WorkUnit(region, partition.toIntArray())
            }
        }
        return WorkPlan(units, total)
    }

    private fun scanWorkUnit(
        unit: WorkUnit,
        visitor: ChunkVisitor,
        stats: ScanStats,
        stop: AtomicBoolean,
        progress: ProgressState,
    ) {
        if (stop.get()) return
        stats.regionsScanned.incrementAndGet()
        try {
            RegionFile.open(unit.region.path).use { region ->
                for (index in unit.chunkIndices) {
                    if (stop.get()) return
                    val payload = try {
                        region.readChunk(index)
                    } catch (e: IOException) {
                        stats.readErrors.incrementAndGet()
                        null
                    } ?: continue

                    val decompressed = try {
                        compression.get().decompress(payload.compressionType, payload.data)
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
                    stats.chunksScanned.incrementAndGet()

                    val chunkX = unit.region.regionX * 32 + (payload.chunkIndex % 32)
                    val chunkZ = unit.region.regionZ * 32 + (payload.chunkIndex / 32)
                    if (!visitor.visit(root, unit.region, chunkX, chunkZ)) {
                        stop.set(true)
                        return
                    }
                    progress.advance()
                }
            }
        } catch (e: IOException) {
            stats.openErrors.incrementAndGet()
        }
    }

    override fun close() {
        executor.shutdown()
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
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

/** Throttled progress reporting, roughly one callback per 1% of chunks. */
internal class ProgressState(
    private val totalChunks: Long,
    private val listener: ScanProgress?,
) {
    private val done = AtomicLong()
    private val lastBucket = AtomicLong(-1)

    fun advance() {
        val now = done.incrementAndGet()
        val listener = listener ?: return
        val step = maxOf(1L, totalChunks / 100)
        val bucket = now / step
        val previous = lastBucket.get()
        if (bucket != previous && lastBucket.compareAndSet(previous, bucket)) {
            listener.onProgress(now, totalChunks)
        }
    }

    fun report(force: Boolean) {
        if (force) listener?.onProgress(done.get(), totalChunks)
    }
}

/** Splits [items] into at most [partitions] contiguous, balanced groups. */
internal fun <T> partition(items: List<T>, partitions: Int): List<List<T>> {
    if (items.isEmpty() || partitions <= 1) return listOf(items)
    val target = minOf(partitions, items.size)
    val groupSize = (items.size + target - 1) / target
    return items.chunked(groupSize)
}
