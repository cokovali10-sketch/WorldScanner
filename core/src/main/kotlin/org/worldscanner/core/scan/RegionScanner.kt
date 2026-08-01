package org.worldscanner.core.scan

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.worldscanner.core.anvil.ChunkCompression
import org.worldscanner.core.anvil.RegionFile
import org.worldscanner.core.model.SearchResult
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtReader
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Scans a single region file (or a subset of its chunks) and exposes every
 * match as a cold [Flow].
 *
 * Each call to [scanUnit] opens its own [RegionFile] handle, so at most
 * `workerCount` handles are open at once. Decompression buffers are thread-local
 * to keep GC pressure low. The flow is cooperative with cancellation: the chunk
 * loop checks [ensureActive] so a cancelled scan stops promptly, and it aborts
 * early once the shared [stop] flag is raised (e.g. the result limit was hit).
 */
internal class RegionScanner(
    private val query: ScanQuery,
    private val stats: ScanStats,
    private val stop: AtomicBoolean,
    private val tracker: ProgressTracker,
) {
    private val compression = ThreadLocal.withInitial { ChunkCompression() }
    private val chunkScanner = ChunkScanner(query)

    /** Streams the search results of one [unit]. Emits per-chunk batches. */
    fun scanUnit(unit: WorkUnit): Flow<SearchResult> = flow {
        if (stop.get()) return@flow
        stats.regionsScanned.incrementAndGet()

        try {
            RegionFile.open(unit.region.path).use { file ->
                for (index in unit.chunkIndices) {
                    currentCoroutineContext().ensureActive()
                    if (stop.get()) return@flow

                    val root = readChunkRoot(file, index) ?: continue

                    val chunkX = unit.region.regionX * 32 + (index % 32)
                    val chunkZ = unit.region.regionZ * 32 + (index / 32)
                    val batch = ArrayList<SearchResult>(4)
                    val ok = chunkScanner.scanChunk(
                        root,
                        unit.region.dimension,
                        unit.region.regionX,
                        unit.region.regionZ,
                        chunkX,
                        chunkZ,
                        ResultSink { result ->
                            batch += result
                            true
                        },
                    )
                    if (!ok) {
                        stop.set(true)
                        return@flow
                    }

                    stats.chunksScanned.incrementAndGet()
                    tracker.onChunkDone()
                    tracker.maybeReport()

                    for (result in batch) emit(result)
                }
            }
        } catch (e: IOException) {
            stats.openErrors.incrementAndGet()
        }

        tracker.onUnitDone(unit.region.path)
    }

    private fun readChunkRoot(file: RegionFile, index: Int): NbtCompound? {
        val payload = try {
            file.readChunk(index)
        } catch (e: IOException) {
            stats.readErrors.incrementAndGet()
            null
        } ?: return null

        val decompressed = try {
            compression.get().decompress(payload.compressionType, payload.data)
        } catch (e: Exception) {
            stats.decompressionErrors.incrementAndGet()
            null
        } ?: return null

        return try {
            NbtReader.read(decompressed)
        } catch (e: Exception) {
            stats.nbtErrors.incrementAndGet()
            null
        }
    }
}
