package org.worldscanner.core.filter

import org.worldscanner.core.nbt.NbtByte
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtDouble
import org.worldscanner.core.nbt.NbtFloat
import org.worldscanner.core.nbt.NbtInt
import org.worldscanner.core.nbt.NbtLong
import org.worldscanner.core.nbt.NbtShort
import org.worldscanner.core.nbt.NbtTag

/**
 * NBT comparison helpers shared by the filter parser and the matcher.
 *
 * The key idea is that NBT values inside item data are *typed* (int vs short
 * vs byte), but filter authors write plain numbers. [nbtEquals] therefore
 * compares numbers numerically across types instead of requiring exact tag
 * type equality.
 */
internal object FilterNbt {

    /** Reads a whole number out of a byte/short/int/long tag, or null. */
    fun extractInt(tag: NbtTag?): Int? = when (tag) {
        is NbtByte -> tag.value.toInt()
        is NbtShort -> tag.value.toInt()
        is NbtInt -> tag.value
        is NbtLong -> tag.value.toInt()
        else -> null
    }

    /**
     * Partial "contains" match used for compounds: every entry in [required]
     * must exist in [actual] and match recursively. Non-compound values fall
     * back to exact equality.
     */
    fun nbtContains(required: NbtTag, actual: NbtTag): Boolean = when {
        required is NbtCompound && actual is NbtCompound ->
            required.entries.all { (key, value) ->
                actual.entries[key]?.let { nbtContains(value, it) } == true
            }
        else -> nbtEquals(required, actual)
    }

    /** Exact equality with numeric cross-type tolerance for number tags. */
    fun nbtEquals(a: NbtTag, b: NbtTag): Boolean {
        val an = numericValue(a)
        val bn = numericValue(b)
        return if (an != null && bn != null) {
            if (an is Float || an is Double || bn is Float || bn is Double) {
                an.toDouble() == bn.toDouble()
            } else {
                an.toLong() == bn.toLong()
            }
        } else {
            a == b
        }
    }

    private fun numericValue(tag: NbtTag): Number? = when (tag) {
        is NbtByte -> tag.value
        is NbtShort -> tag.value
        is NbtInt -> tag.value
        is NbtLong -> tag.value
        is NbtFloat -> tag.value
        is NbtDouble -> tag.value
        else -> null
    }
}
