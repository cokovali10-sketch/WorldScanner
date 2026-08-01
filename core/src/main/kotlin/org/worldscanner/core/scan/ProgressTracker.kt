package org.worldscanner.core.scan

import org.worldscanner.core.anvil.RegionDiscovery
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * One region file split into a contiguous chunk-range [WorkUnit] for parallel
 * processing. A single region may contribute several units.
 */
data class WorkUnit(
    val region: RegionDiscovery.RegionEntry,
    val chunkIndices: IntArray,
)

/** Precomputed parallel work plan for a scan run. */
internal class WorkPlan(val units: List<WorkUnit>, val totalChunks: Long)

/**
 * Thread-safe scan progress bookkeeping shared by parallel workers.
 *
 * Counters are incremented from many worker coroutines; reporting is throttled
 * to roughly one callback per second and computes a sliding-window throughput
 * (chunks/s and files/s) plus an ETA derived from the chunk rate.
 */
internal class ProgressTracker(
    plan: WorkPlan,
    private val listener: ScanProgress?,
) {
    private val totalFiles: Int = plan.units.map { it.region.path }.toSet().size
    private val totalChunks: Long = plan.totalChunks
    private val chunksDone = AtomicLong()
    private val filesDone = AtomicInteger()

    /** Remaining units per region path; a region counts as "file done" when zero. */
    private val regionRemaining = ConcurrentHashMap<Path, AtomicInteger>().apply {
        for ((path, units) in plan.units.groupBy { it.region.path }) {
            put(path, AtomicInteger(units.size))
        }
    }

    private data class Sample(val timeNanos: Long, val chunks: Long, val files: Int)

    private val samples = ArrayDeque<Sample>()
    private var lastEmitNanos = 0L
    private val lock = Any()

    fun onChunkDone() {
        chunksDone.incrementAndGet()
    }

    fun onUnitDone(regionPath: Path) {
        val remaining = regionRemaining[regionPath]?.decrementAndGet() ?: return
        if (remaining == 0) filesDone.incrementAndGet()
    }

    /** Emits a snapshot, but no more often than once per second unless [force]. */
    fun maybeReport(force: Boolean = false) {
        val listener = listener ?: return
        val now = System.nanoTime()
        synchronized(lock) {
            if (!force && now - lastEmitNanos < REPORT_INTERVAL_NANOS) return
            lastEmitNanos = now

            val chunks = chunksDone.get()
            val files = filesDone.get()
            samples.addLast(Sample(now, chunks, files))
            while (samples.size > 1 && now - samples.first().timeNanos > WINDOW_NANOS) {
                samples.removeFirst()
            }

            val snapshot = if (samples.size >= 2) {
                val first = samples.first()
                val last = samples.last()
                val seconds = (last.timeNanos - first.timeNanos) / 1_000_000_000.0
                val chunksPerSecond = if (seconds > 0) (last.chunks - first.chunks) / seconds else 0.0
                val filesPerSecond = if (seconds > 0) (last.files - first.files) / seconds else 0.0
                val remaining = (totalChunks - chunks).coerceAtLeast(0)
                val eta = if (chunksPerSecond > 0) (remaining / chunksPerSecond).toLong() else null
                ScanProgressSnapshot(
                    chunksDone = chunks,
                    totalChunks = totalChunks,
                    filesDone = files,
                    totalFiles = totalFiles,
                    chunksPerSecond = chunksPerSecond,
                    filesPerSecond = filesPerSecond,
                    etaMillis = eta,
                )
            } else {
                ScanProgressSnapshot(chunks, totalChunks, files, totalFiles, 0.0, 0.0, null)
            }
            listener.onProgress(snapshot)
        }
    }

    /** Emits a final snapshot unconditionally (e.g. when the scan finishes). */
    fun reportFinal() = maybeReport(force = true)

    private companion object {
        const val REPORT_INTERVAL_NANOS = 1_000_000_000L
        const val WINDOW_NANOS = 5_000_000_000L
    }
}
