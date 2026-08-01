package org.worldscanner.core.scan

import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.model.ItemStack
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.int
import org.worldscanner.core.nbt.string
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe accumulator shared by parallel [ChunkAnalyzer] workers.
 */
class AnalysisAccumulator internal constructor() {
    internal val itemCounts = ConcurrentHashMap<String, Long>()
    internal val blockEntityCounts = ConcurrentHashMap<String, Int>()
    internal val entityCounts = ConcurrentHashMap<String, Int>()
    internal val chunksByDimension = ConcurrentHashMap<DimensionType, AtomicInteger>()
    internal val minDataVersion = AtomicInteger(Int.MAX_VALUE)
    internal val maxDataVersion = AtomicInteger(Int.MIN_VALUE)
    internal val chunksWithDataVersion = AtomicInteger()

    internal fun trackDataVersion(root: NbtCompound) {
        val version = root.int("DataVersion") ?: return
        chunksWithDataVersion.incrementAndGet()
        minDataVersion.updateAndGet { minOf(it, version) }
        maxDataVersion.updateAndGet { maxOf(it, version) }
    }

    internal fun toResult(regionsByDimension: Map<DimensionType, Int>, scanStats: ScanStats): WorldAnalysisResult =
        WorldAnalysisResult(
            regionsByDimension = regionsByDimension,
            chunksByDimension = chunksByDimension.mapValues { it.value.get() },
            blockEntitiesByType = blockEntityCounts.toSortedMap(),
            entitiesByType = entityCounts.toSortedMap(),
            itemsByType = itemCounts.entries.asSequence()
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .map { it.key to it.value }
                .toList(),
            dataVersionRange = if (chunksWithDataVersion.get() == 0) null
            else minDataVersion.get() to maxDataVersion.get(),
            scanStats = scanStats,
        )
}

/**
 * Counts items (including nested container contents), block entities and
 * entities of one parsed chunk. Stateless; all state lives in [AnalysisAccumulator].
 */
object ChunkAnalyzer {

    private const val MAX_NESTING_DEPTH = 8

    fun analyze(root: NbtCompound, dimension: DimensionType, acc: AnalysisAccumulator) {
        acc.chunksByDimension.computeIfAbsent(dimension) { AtomicInteger() }.incrementAndGet()
        acc.trackDataVersion(root)

        for (blockEntity in ChunkStructure.blockEntities(root)) {
            blockEntity.string("id")?.let { acc.blockEntityCounts.merge(it, 1, Int::plus) }
            for (stack in ItemStackExtractor.extractDirect(blockEntity)) countItems(stack, acc)
        }
        for (entity in ChunkStructure.entities(root)) {
            entity.string("id")?.let { acc.entityCounts.merge(it, 1, Int::plus) }
            for (stack in ItemStackExtractor.extractDirect(entity)) countItems(stack, acc)
        }
    }

    private fun countItems(stack: ItemStack, acc: AnalysisAccumulator, depth: Int = 0) {
        if (depth > MAX_NESTING_DEPTH) return
        acc.itemCounts.merge(stack.normalizedId, stack.count.toLong(), Long::plus)
        ItemStackExtractor.extractNested(stack) { nested ->
            countItems(nested, acc, depth + 1)
        }
    }
}

/**
 * Immutable result of a world analysis run.
 */
data class WorldAnalysisResult(
    val regionsByDimension: Map<DimensionType, Int>,
    val chunksByDimension: Map<DimensionType, Int>,
    val blockEntitiesByType: Map<String, Int>,
    val entitiesByType: Map<String, Int>,
    /** Normalized item id -> total count, sorted descending by count. */
    val itemsByType: List<Pair<String, Long>>,
    /** Min..max chunk `DataVersion` across the world, or null when unknown. */
    val dataVersionRange: Pair<Int, Int>?,
    val scanStats: ScanStats,
) {
    companion object {
        fun empty(): WorldAnalysisResult = WorldAnalysisResult(
            regionsByDimension = emptyMap(),
            chunksByDimension = emptyMap(),
            blockEntitiesByType = emptyMap(),
            entitiesByType = emptyMap(),
            itemsByType = emptyList(),
            dataVersionRange = null,
            ScanStats(),
        )
    }
}
