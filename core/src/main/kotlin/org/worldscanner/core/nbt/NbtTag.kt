package org.worldscanner.core.nbt

import java.util.Locale

/**
 * Immutable in-memory representation of an NBT tag tree.
 *
 * The whole chunk is parsed into this model; no byte-level string matching is
 * ever performed. All searches run against the typed [NbtCompound] structure.
 */
sealed interface NbtTag {
    val type: NbtType
}

data object NbtEnd : NbtTag {
    override val type: NbtType = NbtType.END
}

data class NbtByte(val value: Byte) : NbtTag {
    override val type: NbtType = NbtType.BYTE
}

data class NbtShort(val value: Short) : NbtTag {
    override val type: NbtType = NbtType.SHORT
}

data class NbtInt(val value: Int) : NbtTag {
    override val type: NbtType = NbtType.INT
}

data class NbtLong(val value: Long) : NbtTag {
    override val type: NbtType = NbtType.LONG
}

data class NbtFloat(val value: Float) : NbtTag {
    override val type: NbtType = NbtType.FLOAT
}

data class NbtDouble(val value: Double) : NbtTag {
    override val type: NbtType = NbtType.DOUBLE
}

data class NbtByteArray(val value: ByteArray) : NbtTag {
    override val type: NbtType = NbtType.BYTE_ARRAY

    override fun equals(other: Any?): Boolean =
        other is NbtByteArray && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()
}

data class NbtString(val value: String) : NbtTag {
    override val type: NbtType = NbtType.STRING
}

data class NbtList(val elementType: NbtType, val items: List<NbtTag>) : NbtTag {
    override val type: NbtType = NbtType.LIST

    val size: Int get() = items.size

    operator fun get(index: Int): NbtTag = items[index]
}

fun NbtList.compounds(): List<NbtCompound> = items.filterIsInstance<NbtCompound>()

fun NbtList.strings(): List<String> = items.filterIsInstance<NbtString>().map { it.value }

data class NbtCompound(val entries: Map<String, NbtTag>) : NbtTag {
    override val type: NbtType = NbtType.COMPOUND

    val size: Int get() = entries.size

    operator fun get(name: String): NbtTag? = entries[name]

    fun contains(name: String): Boolean = name in entries

    fun names(): Set<String> = entries.keys
}

data class NbtIntArray(val value: IntArray) : NbtTag {
    override val type: NbtType = NbtType.INT_ARRAY

    override fun equals(other: Any?): Boolean =
        other is NbtIntArray && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()
}

data class NbtLongArray(val value: LongArray) : NbtTag {
    override val type: NbtType = NbtType.LONG_ARRAY

    override fun equals(other: Any?): Boolean =
        other is NbtLongArray && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()
}

/** Typed accessors for [NbtCompound]. Missing or mistyped keys return null. */

fun NbtCompound.getByteOrNull(name: String): Byte? = (entries[name] as? NbtByte)?.value

fun NbtCompound.getShortOrNull(name: String): Short? = (entries[name] as? NbtShort)?.value

fun NbtCompound.getIntOrNull(name: String): Int? = (entries[name] as? NbtInt)?.value

fun NbtCompound.getLongOrNull(name: String): Long? = (entries[name] as? NbtLong)?.value

fun NbtCompound.getFloatOrNull(name: String): Float? = (entries[name] as? NbtFloat)?.value

fun NbtCompound.getDoubleOrNull(name: String): Double? = (entries[name] as? NbtDouble)?.value

fun NbtCompound.getByteArrayOrNull(name: String): ByteArray? = (entries[name] as? NbtByteArray)?.value

fun NbtCompound.getStringOrNull(name: String): String? = (entries[name] as? NbtString)?.value

fun NbtCompound.getListOrNull(name: String): NbtList? = entries[name] as? NbtList

fun NbtCompound.getCompoundOrNull(name: String): NbtCompound? = entries[name] as? NbtCompound

fun NbtCompound.getIntArrayOrNull(name: String): IntArray? = (entries[name] as? NbtIntArray)?.value

fun NbtCompound.getLongArrayOrNull(name: String): LongArray? = (entries[name] as? NbtLongArray)?.value

/** Short aliases kept for call sites across the codebase. */

fun NbtCompound.byte(name: String): Byte? = getByteOrNull(name)

fun NbtCompound.short(name: String): Short? = getShortOrNull(name)

fun NbtCompound.int(name: String): Int? = getIntOrNull(name)

fun NbtCompound.long(name: String): Long? = getLongOrNull(name)

fun NbtCompound.float(name: String): Float? = getFloatOrNull(name)

fun NbtCompound.double(name: String): Double? = getDoubleOrNull(name)

fun NbtCompound.byteArray(name: String): ByteArray? = getByteArrayOrNull(name)

fun NbtCompound.string(name: String): String? = getStringOrNull(name)

fun NbtCompound.list(name: String): NbtList? = getListOrNull(name)

fun NbtCompound.compound(name: String): NbtCompound? = getCompoundOrNull(name)

fun NbtCompound.intArray(name: String): IntArray? = getIntArrayOrNull(name)

fun NbtCompound.longArray(name: String): LongArray? = getLongArrayOrNull(name)

/** Returns [name] as a string, or [default] when absent. */
fun NbtCompound.stringOr(name: String, default: String): String = getStringOrNull(name) ?: default

/** Returns [name] as an int, or [default] when absent. */
fun NbtCompound.intOr(name: String, default: Int): Int = getIntOrNull(name) ?: default

/**
 * Normalizes a Minecraft resource location for comparison.
 * "minecraft:diamond" and "diamond" compare equal.
 */
fun String.normalizeResourceLocation(): String {
    val trimmed = trim().lowercase(Locale.ROOT)
    return if (trimmed.startsWith("minecraft:")) trimmed.removePrefix("minecraft:") else trimmed
}

/** Builds a compound from the given key-value pairs. */
fun compoundOf(vararg entries: Pair<String, NbtTag>): NbtCompound =
    NbtCompound(entries.toMap())
