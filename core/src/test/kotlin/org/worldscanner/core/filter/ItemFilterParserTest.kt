package org.worldscanner.core.filter

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.worldscanner.core.nbt.NbtByte
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtDouble
import org.worldscanner.core.nbt.NbtInt
import org.worldscanner.core.nbt.NbtString
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemFilterParserTest {

    private fun parse(raw: String): ItemFilter = ItemFilterParser.parse(raw).getOrThrow()

    // ------------------------------------------------------------------ ids

    @Test
    fun `parses the required example - diamond sword with mending`() {
        val filter = parse("diamond_sword[enchantments={mending:1}]")

        assertEquals("diamond_sword", filter.normalizedId)
        assertEquals(1, filter.conditions.size)
        val enchantments = assertIs<ComponentCondition.Enchantments>(filter.conditions[0])
        assertEquals(1, enchantments.levels["mending"])
    }

    @Test
    fun `parses item id without namespace`() {
        assertEquals("diamond_sword", parse("diamond_sword").normalizedId)
    }

    @Test
    fun `normalizes namespaced item id`() {
        assertEquals("diamond_sword", parse("minecraft:diamond_sword").normalizedId)
    }

    @Test
    fun `bare id without components has no conditions`() {
        val filter = parse("minecraft:stick")
        assertEquals("stick", filter.normalizedId)
        assertTrue(filter.conditions.isEmpty())
    }

    // ---------------------------------------------------------- components

    @Test
    fun `parses multiple enchantments with levels`() {
        val filter = parse("diamond_sword[enchantments={mending:1,sharpness:5}]")
        val enchantments = assertIs<ComponentCondition.Enchantments>(filter.conditions.single())
        assertEquals(mapOf("mending" to 1, "sharpness" to 5), enchantments.levels)
    }

    @Test
    fun `parses quoted namespaced enchantment key`() {
        val filter = parse("diamond_sword[enchantments={\"minecraft:mending\":1}]")
        val enchantments = assertIs<ComponentCondition.Enchantments>(filter.conditions.single())
        assertEquals(1, enchantments.levels["minecraft:mending"])
    }

    @Test
    fun `parses damage`() {
        val filter = parse("diamond_sword[damage=150]")
        assertEquals(ComponentCondition.Damage(150), filter.conditions.single())
    }

    @Test
    fun `parses namespaced component key`() {
        val filter = parse("diamond_sword[minecraft:damage=5]")
        assertEquals(ComponentCondition.Damage(5), filter.conditions.single())
    }

    @Test
    fun `parses multiple components`() {
        val filter = parse("diamond_sword[enchantments={mending:1,sharpness:5},damage=150]")
        assertEquals(2, filter.conditions.size)
        assertIs<ComponentCondition.Enchantments>(filter.conditions[0])
        assertEquals(ComponentCondition.Damage(150), filter.conditions[1])
    }

    @Test
    fun `parses custom name`() {
        val filter = parse("diamond_sword[custom_name=\"My Sword\"]")
        assertEquals(ComponentCondition.CustomName("My Sword"), filter.conditions.single())
    }

    @Test
    fun `parses custom name with single quotes`() {
        val filter = parse("diamond_sword[custom_name='My Sword']")
        assertEquals(ComponentCondition.CustomName("My Sword"), filter.conditions.single())
    }

    @Test
    fun `parses custom name as text component`() {
        val filter = parse("diamond_sword[custom_name={\"text\":\"My Sword\"}]")
        assertEquals(ComponentCondition.CustomName("My Sword"), filter.conditions.single())
    }

    @Test
    fun `parses custom data compound`() {
        val filter = parse("diamond[custom_data={owner:\"koca\",level:3,rare:true}]")
        val customData = assertIs<ComponentCondition.CustomData>(filter.conditions.single())
        assertEquals("koca", (customData.required["owner"] as NbtString).value)
        assertEquals(3, (customData.required["level"] as NbtInt).value)
        assertEquals(1, (customData.required["rare"] as NbtByte).value)
    }

    @Test
    fun `parses raw component with list value`() {
        val filter = parse("diamond_sword[attribute_modifiers=[{id:\"boost\",amount:1.5}]]")
        val raw = assertIs<ComponentCondition.Raw>(filter.conditions.single())
        assertEquals("attribute_modifiers", raw.componentKey)
        val list = assertIs<org.worldscanner.core.nbt.NbtList>(raw.required)
        val entry = assertIs<NbtCompound>(list[0])
        assertEquals(1.5, (entry["amount"] as NbtDouble).value)
    }

    @Test
    fun `parses generic numeric and bool primitives`() {
        val filter = parse("item[foo=42,bar=-3.5,baz=true]")
        val raw = assertIs<ComponentCondition.Raw>(filter.conditions[0])
        assertEquals(42, (raw.required as NbtInt).value)
        val floatCondition = assertIs<ComponentCondition.Raw>(filter.conditions[1])
        assertEquals(-3.5, (floatCondition.required as NbtDouble).value)
    }

    @Test
    fun `parses component-only filter with brackets`() {
        val filter = parse("[damage=150]")
        assertNull(filter.normalizedId)
        assertEquals(ComponentCondition.Damage(150), filter.conditions.single())
    }

    @Test
    fun `parses component-only filter without brackets`() {
        val filter = parse("damage=150")
        assertNull(filter.normalizedId)
        assertEquals(ComponentCondition.Damage(150), filter.conditions.single())
    }

    @Test
    fun `parses whitespace tolerant input`() {
        val filter = parse("  diamond_sword [ enchantments = { mending : 1 } ] ")
        assertEquals("diamond_sword", filter.normalizedId)
        assertEquals(mapOf("mending" to 1), (filter.conditions.single() as ComponentCondition.Enchantments).levels)
    }

    @Test
    fun `parses value containing resource location`() {
        val filter = parse("diamond[custom_data={id:minecraft:diamond}]")
        val customData = assertIs<ComponentCondition.CustomData>(filter.conditions.single())
        assertEquals("minecraft:diamond", (customData.required["id"] as NbtString).value)
    }

    @Test
    fun `parses escaped quotes in strings`() {
        val filter = parse("sword[custom_name=\"a \\\"quoted\\\" name\"]")
        assertEquals(ComponentCondition.CustomName("a \"quoted\" name"), filter.conditions.single())
    }

    // ------------------------------------------------------------- failures

    private fun assertFails(raw: String, position: Int? = null) {
        val result = ItemFilterParser.parse(raw)
        assertTrue(result.isFailure, "expected parse failure for: $raw")
        if (position != null) {
            val error = assertThrows<FilterSyntaxException> { throw result.exceptionOrNull()!! }
            assertEquals(position, error.position)
        }
    }

    @Test
    fun `rejects empty input`() {
        assertFails("")
        assertFails("   ")
    }

    @Test
    fun `rejects unterminated component list`() {
        assertFails("diamond_sword[")
    }

    @Test
    fun `rejects unterminated compound`() {
        assertFails("diamond_sword[enchantments={mending:1]")
    }

    @Test
    fun `rejects enchantments with non-compound value`() {
        assertFails("diamond_sword[enchantments=5]")
    }

    @Test
    fun `rejects missing equals separator`() {
        assertFails("diamond_sword[damage150]")
    }

    @Test
    fun `rejects trailing garbage`() {
        assertFails("diamond_sword[damage=150]xyz")
    }

    @Test
    fun `rejects number expected for damage`() {
        assertFails("diamond_sword[damage=abc]")
    }

    @Test
    fun `custom name string comparison is exact`() {
        val name = parse("sword[custom_name=\"My Sword\"]").conditions.single()
        assertTrue(name is ComponentCondition.CustomName && name.text == "My Sword")
        assertFalse(name is ComponentCondition.CustomName && name.text == "my sword")
    }
}
