package org.worldscanner.core.scan

import org.junit.jupiter.api.Test
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtInt
import org.worldscanner.core.nbt.NbtList
import org.worldscanner.core.nbt.NbtReader
import org.worldscanner.core.nbt.NbtString
import org.worldscanner.core.nbt.NbtType
import org.worldscanner.core.nbt.NbtWriter
import org.worldscanner.core.nbt.compoundOf
import kotlin.test.assertEquals

class WorldAnalysisTest {

    private fun item(id: String, count: Int): NbtCompound = compoundOf(
        "id" to NbtString(id),
        "count" to NbtInt(count),
    )

    private fun sampleChunk(): NbtCompound = compoundOf(
        "DataVersion" to NbtInt(4189),
        "sections" to NbtList(
            NbtType.COMPOUND,
            listOf(
                compoundOf(
                    "Y" to NbtInt(0),
                    "block_entities" to NbtList(
                        NbtType.COMPOUND,
                        listOf(
                            compoundOf(
                                "id" to NbtString("minecraft:chest"),
                                "x" to NbtInt(1),
                                "y" to NbtInt(64),
                                "z" to NbtInt(1),
                                "container" to compoundOf(
                                    "Items" to NbtList(
                                        NbtType.COMPOUND,
                                        listOf(
                                            item("minecraft:diamond", 3),
                                            item("minecraft:shulker_box", 1),
                                            compoundOf(
                                                "id" to NbtString("minecraft:shulker_box"),
                                                "count" to NbtInt(1),
                                                "components" to compoundOf(
                                                    "minecraft:container" to compoundOf(
                                                        "Items" to NbtList(
                                                            NbtType.COMPOUND,
                                                            listOf(item("minecraft:diamond", 2)),
                                                        ),
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        "entities" to NbtList(
            NbtType.COMPOUND,
            listOf(
                compoundOf(
                    "id" to NbtString("minecraft:zombie"),
                    "HandItems" to NbtList(
                        NbtType.COMPOUND,
                        listOf(item("minecraft:diamond_sword", 1), item("minecraft:air", 0)),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `analyzer accumulates items including nested containers`() {
        val chunk = NbtReader.read(NbtWriter.write(sampleChunk()))
        val acc = AnalysisAccumulator()
        ChunkAnalyzer.analyze(chunk, DimensionType.OVERWORLD, acc)

        assertEquals(5L, acc.itemCounts["diamond"]) // 3 direct + 2 nested
        assertEquals(2L, acc.itemCounts["shulker_box"])
        assertEquals(1L, acc.itemCounts["diamond_sword"])

        assertEquals(1, acc.blockEntityCounts["minecraft:chest"])
        assertEquals(1, acc.entityCounts["minecraft:zombie"])
        assertEquals(1, acc.chunksByDimension[DimensionType.OVERWORLD]?.get())
    }

    @Test
    fun `accumulator maps to a sorted result`() {
        val chunk = NbtReader.read(NbtWriter.write(sampleChunk()))
        val acc = AnalysisAccumulator()
        ChunkAnalyzer.analyze(chunk, DimensionType.OVERWORLD, acc)
        ChunkAnalyzer.analyze(chunk, DimensionType.NETHER, acc)

        val result = acc.toResult(
            regionsByDimension = mapOf(DimensionType.OVERWORLD to 1, DimensionType.NETHER to 1),
            scanStats = ScanStats(),
        )

        assertEquals(1, result.chunksByDimension[DimensionType.OVERWORLD])
        assertEquals(1, result.chunksByDimension[DimensionType.NETHER])
        assertEquals(2, result.blockEntitiesByType["minecraft:chest"])
        assertEquals(2, result.entitiesByType["minecraft:zombie"])
        assertEquals(4189 to 4189, result.dataVersionRange)
        // Items sorted by total count descending: diamond 10, shulker 4, sword 2
        assertEquals(listOf("diamond" to 10L, "shulker_box" to 4L, "diamond_sword" to 2L), result.itemsByType)
        assertEquals(1, result.regionsByDimension[DimensionType.OVERWORLD])
    }
}
