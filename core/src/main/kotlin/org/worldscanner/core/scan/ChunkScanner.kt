package org.worldscanner.core.scan

import org.worldscanner.core.model.BlockPos
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.model.FoundItem
import org.worldscanner.core.model.ItemSource
import org.worldscanner.core.model.ItemStack
import org.worldscanner.core.model.SearchResult
import org.worldscanner.core.model.toBlockPos
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtDouble
import org.worldscanner.core.nbt.intOr
import org.worldscanner.core.nbt.list
import org.worldscanner.core.nbt.string

/** Receives search results; returns false to abort the scan early. */
fun interface ResultSink {
    fun offer(result: SearchResult): Boolean
}

/**
 * Parses one decompressed chunk NBT and locates matching items inside block
 * entities, entities and, recursively, inside nested containers.
 */
class ChunkScanner(val query: ScanQuery) {

    fun scanChunk(
        root: NbtCompound,
        dimension: DimensionType,
        regionX: Int,
        regionZ: Int,
        chunkX: Int,
        chunkZ: Int,
        sink: ResultSink,
    ): Boolean {
        if (query.isEmpty) return true

        for (blockEntity in ChunkStructure.blockEntities(root)) {
            val id = blockEntity.string("id") ?: continue
            val pos = BlockPos(
                blockEntity.intOr("x", 0),
                blockEntity.intOr("y", 0),
                blockEntity.intOr("z", 0),
            )
            val source = ItemSource.BlockEntity(id, pos)
            if (!scanHolder(blockEntity, source, dimension, regionX, regionZ, chunkX, chunkZ, sink)) {
                return false
            }
        }

        for (entity in ChunkStructure.entities(root)) {
            val id = entity.string("id") ?: continue
            val pos = entity.list("Pos")?.items
                ?.mapNotNull { (it as? NbtDouble)?.value }
                ?.toDoubleArray()
                ?.let { toBlockPos(*it) }
            val uuid = entity.string("UUID")
            val source = ItemSource.Entity(id, pos, uuid)
            if (!scanHolder(entity, source, dimension, regionX, regionZ, chunkX, chunkZ, sink)) {
                return false
            }
        }
        return true
    }

    private fun scanHolder(
        holder: NbtCompound,
        source: ItemSource,
        dimension: DimensionType,
        regionX: Int,
        regionZ: Int,
        chunkX: Int,
        chunkZ: Int,
        sink: ResultSink,
    ): Boolean {
        for (stack in ItemStackExtractor.extractDirect(holder)) {
            if (matchesTarget(stack)) {
                if (!emit(stack, emptyList(), source, dimension, regionX, regionZ, chunkX, chunkZ, sink)) {
                    return false
                }
            }
            if (query.includeNested && !walkNested(stack, emptyList(), 1, source, dimension, regionX, regionZ, chunkX, chunkZ, sink)) {
                return false
            }
        }
        return true
    }

    private fun walkNested(
        stack: ItemStack,
        containerPath: List<String>,
        depth: Int,
        source: ItemSource,
        dimension: DimensionType,
        regionX: Int,
        regionZ: Int,
        chunkX: Int,
        chunkZ: Int,
        sink: ResultSink,
    ): Boolean {
        if (depth > query.maxNestingDepth) return true

        var continueScanning = true
        ItemStackExtractor.extractNested(stack) { nested ->
            if (!continueScanning) return@extractNested
            val nestedPath = containerPath + stack.normalizedId
            if (matchesTarget(nested)) {
                continueScanning = emit(nested, nestedPath, source, dimension, regionX, regionZ, chunkX, chunkZ, sink)
            }
            if (continueScanning) {
                continueScanning = walkNested(nested, nestedPath, depth + 1, source, dimension, regionX, regionZ, chunkX, chunkZ, sink)
            }
        }
        return continueScanning
    }

    private fun emit(
        stack: ItemStack,
        containerPath: List<String>,
        source: ItemSource,
        dimension: DimensionType,
        regionX: Int,
        regionZ: Int,
        chunkX: Int,
        chunkZ: Int,
        sink: ResultSink,
    ): Boolean {
        val found = FoundItem(stack, containerPath, source)
        return sink.offer(SearchResult(stack, dimension, regionX, regionZ, chunkX, chunkZ, found))
    }

    private fun matchesTarget(stack: ItemStack): Boolean {
        val matcher = query.matcher
        if (matcher != null) return matcher.matches(stack)
        return stack.normalizedId in query.itemTargets
    }
}
