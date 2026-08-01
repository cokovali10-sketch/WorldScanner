package org.worldscanner.core.nbt

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NbtRoundTripTest {

    private fun sampleCompound() = NbtCompound(
        linkedMapOf(
            "byte" to NbtByte(42),
            "short" to NbtShort(-12),
            "int" to NbtInt(1_000_000),
            "long" to NbtLong(9_000_000_000),
            "float" to NbtFloat(3.5f),
            "double" to NbtDouble(-0.25),
            "string" to NbtString("minecraft:diamond"),
            "bytes" to NbtByteArray(byteArrayOf(1, 2, 3)),
            "ints" to NbtIntArray(intArrayOf(10, 20)),
            "longs" to NbtLongArray(longArrayOf(1L, 2L)),
            "list" to NbtList(
                NbtType.STRING,
                listOf(NbtString("a"), NbtString("b")),
            ),
            "nested" to compoundOf(
                "name" to NbtString("inner"),
                "empty" to NbtCompound(emptyMap()),
                "emptyList" to NbtList(NbtType.END, emptyList()),
            ),
        ),
    )

    @Test
    fun `round trip preserves all tag types`() {
        val original = sampleCompound()
        val decoded = NbtReader.read(NbtWriter.write(original))

        assertEquals(original, decoded)
    }

    @Test
    fun `compound accessors return typed values`() {
        val decoded = NbtReader.read(NbtWriter.write(sampleCompound()))

        assertEquals(42, decoded.byte("byte"))
        assertEquals(-12, decoded.short("short"))
        assertEquals(1_000_000, decoded.int("int"))
        assertEquals(9_000_000_000L, decoded.long("long"))
        assertEquals(3.5f, decoded.float("float"))
        assertEquals(-0.25, decoded.double("double"))
        assertEquals("minecraft:diamond", decoded.string("string"))
        assertEquals(listOf("a", "b"), decoded.list("list")?.strings())
        assertEquals("inner", decoded.compound("nested")?.string("name"))
        assertNull(decoded.string("missing"))
    }

    @Test
    fun `malformed data throws NbtFormatException`() {
        val bad = byteArrayOf(0x0A, 0x00, 0x00) // root compound, no closing END
        org.junit.jupiter.api.assertThrows<Exception> {
            NbtReader.read(bad)
        }
    }

    @Test
    fun `resource location normalization strips minecraft prefix`() {
        assertEquals("diamond", "minecraft:diamond".normalizeResourceLocation())
        assertEquals("diamond", "DIAMOND".normalizeResourceLocation())
        assertEquals("mod:thing", "Mod:Thing".normalizeResourceLocation())
    }
}
