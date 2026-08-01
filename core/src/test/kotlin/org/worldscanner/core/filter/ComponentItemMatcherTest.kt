package org.worldscanner.core.filter

import org.junit.jupiter.api.Test
import org.worldscanner.core.model.ItemStack
import org.worldscanner.core.nbt.NbtByte
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtInt
import org.worldscanner.core.nbt.NbtList
import org.worldscanner.core.nbt.NbtString
import org.worldscanner.core.nbt.NbtTag
import org.worldscanner.core.nbt.NbtType
import org.worldscanner.core.nbt.compoundOf
import org.worldscanner.core.scan.ChunkScanner
import org.worldscanner.core.scan.ResultSink
import org.worldscanner.core.scan.ScanQuery
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.model.SearchResult
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ComponentItemMatcherTest {

    private fun matcher(filter: String): ItemMatcher =
        ComponentItemMatcher(ItemFilterParser.parse(filter).getOrThrow())

    // ------------------------------------------------------- NBT builders

    private fun modernSword(
        mending: Int? = 1,
        sharpness: Int? = 5,
        damage: Int? = 150,
        customName: NbtTag? = null,
        customData: NbtCompound? = null,
    ): NbtCompound {
        val enchantments = NbtCompound(
            mapOf(
                "levels" to NbtCompound(
                    buildMap {
                        if (mending != null) put("minecraft:mending", NbtInt(mending))
                        if (sharpness != null) put("minecraft:sharpness", NbtInt(sharpness))
                    },
                ),
                "show_in_tooltip" to NbtByte(1),
            ),
        )
        return compoundOf(
            "id" to NbtString("minecraft:diamond_sword"),
            "count" to NbtInt(1),
            "components" to NbtCompound(
                buildMap {
                    put("minecraft:enchantments", enchantments)
                    if (damage != null) put("minecraft:damage", NbtInt(damage))
                    if (customName != null) put("minecraft:custom_name", customName)
                    if (customData != null) put("minecraft:custom_data", customData)
                },
            ),
        )
    }

    private fun legacySword(
        mending: Int? = 1,
        sharpness: Int? = 5,
        damage: Int? = 150,
        customName: String? = null,
        customData: Map<String, NbtTag> = emptyMap(),
    ): NbtCompound {
        val enchantments = NbtList(
            NbtType.COMPOUND,
            buildList {
                if (mending != null) add(compoundOf("id" to NbtString("minecraft:mending"), "lvl" to NbtInt(mending)))
                if (sharpness != null) add(compoundOf("id" to NbtString("minecraft:sharpness"), "lvl" to NbtInt(sharpness)))
            },
        )
        return compoundOf(
            "id" to NbtString("minecraft:diamond_sword"),
            "Count" to NbtInt(1),
            "tag" to NbtCompound(
                buildMap {
                    put("Enchantments", enchantments)
                    if (damage != null) put("Damage", NbtInt(damage))
                    if (customName != null) put("display", compoundOf("Name" to NbtString(customName)))
                    customData.forEach { (k, v) -> put(k, v) }
                },
            ),
        )
    }

    // ------------------------------------------------------ required example

    @Test
    fun `matches the required example - diamond sword with mending`() {
        val m = matcher("diamond_sword[enchantments={mending:1}]")
        assertTrue(m.matches(modernSword()))
    }

    // ------------------------------------------------------------ id checks

    @Test
    fun `matches namespaced and plain ids equally`() {
        val m = matcher("minecraft:diamond_sword[enchantments={mending:1}]")
        assertTrue(m.matches(modernSword()))
        assertTrue(m.matches(modernSword().let {
            // strip namespace from stored id
            val entries = it.entries.toMutableMap()
            entries["id"] = NbtString("diamond_sword")
            NbtCompound(entries)
        }))
    }

    @Test
    fun `rejects wrong item id`() {
        val m = matcher("netherite_sword[enchantments={mending:1}]")
        assertFalse(m.matches(modernSword()))
    }

    @Test
    fun `component-only filter matches any item id`() {
        val m = matcher("[enchantments={mending:1}]")
        assertTrue(m.matches(modernSword()))
    }

    // -------------------------------------------------------- enchantments

    @Test
    fun `matches multiple enchantments`() {
        val m = matcher("diamond_sword[enchantments={mending:1,sharpness:5}]")
        assertTrue(m.matches(modernSword(mending = 1, sharpness = 5)))
    }

    @Test
    fun `higher level still matches`() {
        val m = matcher("diamond_sword[enchantments={sharpness:5}]")
        assertTrue(m.matches(modernSword(sharpness = 7)))
    }

    @Test
    fun `lower level does not match`() {
        val m = matcher("diamond_sword[enchantments={sharpness:5}]")
        assertFalse(m.matches(modernSword(sharpness = 4)))
    }

    @Test
    fun `missing enchantment does not match`() {
        val m = matcher("diamond_sword[enchantments={silk_touch:1}]")
        assertFalse(m.matches(modernSword()))
    }

    @Test
    fun `matches legacy enchantments via tag`() {
        val m = matcher("diamond_sword[enchantments={mending:1,sharpness:5}]")
        assertTrue(m.matches(legacySword()))
    }

    @Test
    fun `legacy lower level does not match`() {
        val m = matcher("diamond_sword[enchantments={sharpness:5}]")
        assertFalse(m.matches(legacySword(sharpness = 3)))
    }

    // ------------------------------------------------------------- damage

    @Test
    fun `matches modern damage`() {
        val m = matcher("diamond_sword[damage=150]")
        assertTrue(m.matches(modernSword(damage = 150)))
        assertFalse(m.matches(modernSword(damage = 149)))
    }

    @Test
    fun `matches legacy damage`() {
        val m = matcher("diamond_sword[damage=150]")
        assertTrue(m.matches(legacySword(damage = 150)))
        assertFalse(m.matches(legacySword(damage = 151)))
    }

    // -------------------------------------------------------- custom name

    @Test
    fun `matches modern custom name from text component`() {
        val m = matcher("diamond_sword[custom_name=\"My Sword\"]")
        assertTrue(m.matches(modernSword(customName = compoundOf("text" to NbtString("My Sword"), "italic" to NbtByte(0)))))
    }

    @Test
    fun `matches modern custom name stored as plain string`() {
        val m = matcher("diamond_sword[custom_name=\"My Sword\"]")
        assertTrue(m.matches(modernSword(customName = NbtString("My Sword"))))
    }

    @Test
    fun `matches legacy custom name as json text component`() {
        val m = matcher("diamond_sword[custom_name=\"My Sword\"]")
        assertTrue(m.matches(legacySword(customName = """{"text":"My Sword","italic":false}""")))
    }

    @Test
    fun `custom name mismatch`() {
        val m = matcher("diamond_sword[custom_name=\"Other\"]")
        assertFalse(m.matches(modernSword(customName = compoundOf("text" to NbtString("My Sword")))))
    }

    // -------------------------------------------------------- custom data

    @Test
    fun `matches modern custom data partially`() {
        val m = matcher("diamond_sword[custom_data={owner:\"koca\"}]")
        assertTrue(
            m.matches(
                modernSword(
                    customData = compoundOf(
                        "owner" to NbtString("koca"),
                        "color" to NbtString("red"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `nested custom data matches recursively`() {
        val m = matcher("diamond_sword[custom_data={stats:{kills:10}}]")
        assertTrue(
            m.matches(
                modernSword(
                    customData = compoundOf(
                        "stats" to compoundOf("kills" to NbtInt(10), "deaths" to NbtInt(2)),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `custom data mismatch`() {
        val m = matcher("diamond_sword[custom_data={owner:\"alice\"}]")
        assertFalse(m.matches(modernSword(customData = compoundOf("owner" to NbtString("bob")))))
    }

    @Test
    fun `matches legacy custom data stored in tag`() {
        val m = matcher("diamond_sword[custom_data={owner:\"koca\"}]")
        assertTrue(m.matches(legacySword(customData = mapOf("owner" to NbtString("koca")))))
    }

    // --------------------------------------------------- ItemStack overload

    @Test
    fun `matches an already-decoded ItemStack`() {
        val m = matcher("diamond_sword[enchantments={mending:1}]")
        val stack = ItemStack.from(modernSword())!!
        assertTrue(m.matches(stack))
    }

    @Test
    fun `ItemStack mismatch`() {
        val m = matcher("netherite_sword[enchantments={mending:1}]")
        val stack = ItemStack.from(modernSword())!!
        assertFalse(m.matches(stack))
    }

    // ------------------------------------------------- scanner integration

    @Test
    fun `scanner returns coordinates and container type for matched item`() {
        val m = matcher("diamond_sword[enchantments={mending:1},damage=150]")
        val query = ScanQuery(matcher = m)

        val chest = compoundOf(
            "id" to NbtString("minecraft:chest"),
            "x" to NbtInt(5),
            "y" to NbtInt(70),
            "z" to NbtInt(8),
            "container" to compoundOf(
                "Items" to NbtList(
                    NbtType.COMPOUND,
                    listOf(modernSword()),
                ),
            ),
        )
        val chunk = compoundOf(
            "sections" to NbtList(
                NbtType.COMPOUND,
                listOf(
                    compoundOf(
                        "Y" to NbtInt(0),
                        "block_entities" to NbtList(NbtType.COMPOUND, listOf(chest)),
                    ),
                ),
            ),
        )

        val scanner = ChunkScanner(query)
        val results = mutableListOf<SearchResult>()
        scanner.scanChunk(chunk, DimensionType.OVERWORLD, 3, 4, 33, 66, ResultSink { result ->
            results += result
            true
        })

        assertEquals(1, results.size)
        val result = results[0]
        assertEquals("diamond_sword", result.item.normalizedId)
        assertEquals(3, result.regionX)
        assertEquals(4, result.regionZ)
        assertEquals(33, result.chunkX)
        assertEquals(66, result.chunkZ)
        val source = assertIs<org.worldscanner.core.model.ItemSource.BlockEntity>(result.source)
        assertEquals("minecraft:chest", source.id)
        assertEquals(5, source.pos.x)
        assertEquals(70, source.pos.y)
        assertEquals(8, source.pos.z)
    }
}
