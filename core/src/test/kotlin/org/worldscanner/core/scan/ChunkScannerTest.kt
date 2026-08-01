package org.worldscanner.core.scan

import org.junit.jupiter.api.Test
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtDouble
import org.worldscanner.core.nbt.NbtInt
import org.worldscanner.core.nbt.NbtList
import org.worldscanner.core.nbt.NbtString
import org.worldscanner.core.nbt.NbtType
import org.worldscanner.core.nbt.compoundOf
import org.worldscanner.core.nbt.NbtWriter
import org.worldscanner.core.nbt.NbtReader
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkScannerTest {

    private fun item(id: String, count: Int, slot: Int? = null, vararg extra: Pair<String, NbtCompound>): NbtCompound {
        val entries = ArrayList<Pair<String, org.worldscanner.core.nbt.NbtTag>>()
        entries += "id" to NbtString(id)
        entries += "count" to NbtInt(count)
        if (slot != null) entries += "slot" to NbtInt(slot)
        for ((key, value) in extra) entries += key to value
        return compoundOf(*entries.toTypedArray())
    }

    private fun shulkerBoxWithNetherite(): NbtCompound = item(
        "minecraft:shulker_box",
        1,
        1,
        "components" to compoundOf(
            "minecraft:container" to compoundOf(
                "Items" to NbtList(
                    NbtType.COMPOUND,
                    listOf(item("minecraft:netherite_ingot", 2, 0)),
                ),
            ),
        ),
    )

    private fun modernChunk(): NbtCompound {
        val chest = compoundOf(
            "id" to NbtString("minecraft:chest"),
            "x" to NbtInt(10),
            "y" to NbtInt(64),
            "z" to NbtInt(20),
            "container" to compoundOf(
                "Items" to NbtList(
                    NbtType.COMPOUND,
                    listOf(
                        item("minecraft:diamond", 3, 0),
                        shulkerBoxWithNetherite(),
                    ),
                ),
            ),
        )
        val droppedItem = compoundOf(
            "id" to NbtString("minecraft:item"),
            "Pos" to NbtList(NbtType.DOUBLE, listOf(NbtDouble(10.0), NbtDouble(65.0), NbtDouble(20.0))),
            "Item" to item("minecraft:emerald", 1),
        )
        val armorStand = compoundOf(
            "id" to NbtString("minecraft:armor_stand"),
            "Pos" to NbtList(NbtType.DOUBLE, listOf(NbtDouble(11.0), NbtDouble(64.0), NbtDouble(21.0))),
            "ArmorItems" to NbtList(
                NbtType.COMPOUND,
                listOf(
                    item("minecraft:leather_boots", 1),
                    item("minecraft:diamond_helmet", 1),
                ),
            ),
            "HandItems" to NbtList(
                NbtType.COMPOUND,
                listOf(
                    item("minecraft:diamond_sword", 1),
                    item("minecraft:stick", 1),
                ),
            ),
        )
        return compoundOf(
            "DataVersion" to NbtInt(4189),
            "sections" to NbtList(
                NbtType.COMPOUND,
                listOf(
                    compoundOf(
                        "Y" to NbtInt(0),
                        "block_entities" to NbtList(NbtType.COMPOUND, listOf(chest)),
                    ),
                ),
            ),
            "entities" to NbtList(NbtType.COMPOUND, listOf(droppedItem, armorStand)),
        )
    }

    private fun legacyChunk(): NbtCompound {
        val chest = compoundOf(
            "id" to NbtString("minecraft:chest"),
            "x" to NbtInt(0),
            "y" to NbtInt(64),
            "z" to NbtInt(0),
            "Items" to NbtList(
                NbtType.COMPOUND,
                listOf(
                    item("minecraft:shulker_box", 1, 2, "tag" to compoundOf(
                        "BlockEntityTag" to compoundOf(
                            "Items" to NbtList(
                                NbtType.COMPOUND,
                                listOf(item("minecraft:gold_ingot", 5, 0)),
                            ),
                        ),
                    )),
                ),
            ),
        )
        val dropped = compoundOf(
            "id" to NbtString("minecraft:item"),
            "Pos" to NbtList(NbtType.DOUBLE, listOf(NbtDouble(0.0), NbtDouble(64.0), NbtDouble(0.0))),
            "Item" to item("minecraft:diamond", 2),
        )
        return compoundOf(
            "Level" to compoundOf(
                "TileEntities" to NbtList(NbtType.COMPOUND, listOf(chest)),
                "Entities" to NbtList(NbtType.COMPOUND, listOf(dropped)),
            ),
        )
    }

    private fun scan(root: NbtCompound, targets: Set<String>): List<org.worldscanner.core.model.SearchResult> {
        val query = ScanQuery(itemTargets = targets, limit = 1000)
        val scanner = ChunkScanner(query)
        val results = mutableListOf<org.worldscanner.core.model.SearchResult>()
        scanner.scanChunk(root, DimensionType.OVERWORLD, 1, 2, 33, 66, ResultSink { result ->
            results += result
            true
        })
        return results
    }

    @Test
    fun `finds direct item in chest block entity`() {
        val chunk = NbtReader.read(NbtWriter.write(modernChunk()))
        val results = scan(chunk, setOf("diamond"))
        assertEquals(1, results.size)
        assertEquals(3, results[0].item.count)
        val source = results[0].source as org.worldscanner.core.model.ItemSource.BlockEntity
        assertEquals("minecraft:chest", source.id)
        assertEquals(10, source.pos.x)
    }

    @Test
    fun `recurses into shulker box inside chest (modern components)`() {
        val chunk = NbtReader.read(NbtWriter.write(modernChunk()))
        val results = scan(chunk, setOf("netherite_ingot"))
        assertEquals(1, results.size)
        assertEquals(2, results[0].item.count)
        assertEquals(listOf("shulker_box"), results[0].containerPath)
    }

    @Test
    fun `recurses into shulker box via legacy tag BlockEntityTag`() {
        val chunk = NbtReader.read(NbtWriter.write(legacyChunk()))
        val results = scan(chunk, setOf("gold_ingot"))
        assertEquals(1, results.size)
        assertEquals(listOf("shulker_box"), results[0].containerPath)
    }

    @Test
    fun `finds dropped item entity and armor stand equipment`() {
        val chunk = NbtReader.read(NbtWriter.write(modernChunk()))
        val results = scan(chunk, setOf("emerald", "diamond_helmet", "diamond_sword"))
        assertEquals(3, results.size)

        val entities = results.map { it.source }
            .filterIsInstance<org.worldscanner.core.model.ItemSource.Entity>()
        assertEquals(3, entities.size)
        assertTrue(entities.any { it.id == "minecraft:item" && it.pos?.y == 65 })
        assertTrue(entities.any { it.id == "minecraft:armor_stand" })
    }

    @Test
    fun `no results when target absent`() {
        val chunk = NbtReader.read(NbtWriter.write(modernChunk()))
        val results = scan(chunk, setOf("dragon_egg"))
        assertTrue(results.isEmpty())
    }

    @Test
    fun `legacy and modern formats both scanned`() {
        val modern = scan(NbtReader.read(NbtWriter.write(modernChunk())), setOf("diamond"))
        val legacy = scan(NbtReader.read(NbtWriter.write(legacyChunk())), setOf("diamond"))
        assertEquals(1, modern.size)
        assertEquals(1, legacy.size)
    }
}
