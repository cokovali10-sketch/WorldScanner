package org.worldscanner.core.nbt

import java.io.ByteArrayInputStream
import java.io.DataInput
import java.io.DataInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Streaming, big-endian binary NBT decoder built on [DataInput].
 *
 * Parses the standard Minecraft NBT format into an immutable [NbtCompound] tree.
 * Primitive reads go straight through [DataInputStream] over a
 * [ByteArrayInputStream], so no intermediate byte copies are made. Structurally
 * invalid or oversized data raises [NbtFormatException]; truncated streams
 * surface as [java.io.EOFException].
 */
class NbtReader private constructor(private val input: DataInput) {

    /** Reads the root named compound, e.g. the root of a chunk NBT. */
    fun readRoot(): NbtCompound {
        val rootType = NbtType.fromId(input.readByte())
        require(rootType == NbtType.COMPOUND) {
            "Root NBT tag must be TAG_Compound, found $rootType"
        }
        readString() // root name (usually empty)
        return readCompoundPayload()
    }

    private fun readCompoundPayload(): NbtCompound {
        val entries = LinkedHashMap<String, NbtTag>()
        while (true) {
            val type = NbtType.fromId(input.readByte())
            if (type == NbtType.END) break
            val name = readString()
            entries[name] = readPayload(type)
        }
        return NbtCompound(entries)
    }

    private fun readPayload(type: NbtType): NbtTag = when (type) {
        NbtType.END -> throw NbtFormatException("Unexpected TAG_End inside payload")

        NbtType.BYTE -> NbtByte(input.readByte())
        NbtType.SHORT -> NbtShort(input.readShort())
        NbtType.INT -> NbtInt(input.readInt())
        NbtType.LONG -> NbtLong(input.readLong())
        NbtType.FLOAT -> NbtFloat(input.readFloat())
        NbtType.DOUBLE -> NbtDouble(input.readDouble())

        NbtType.STRING -> NbtString(readString())

        NbtType.BYTE_ARRAY -> {
            val length = checkLength(input.readInt(), MAX_ARRAY_BYTES, "byte array")
            val bytes = ByteArray(length)
            input.readFully(bytes)
            NbtByteArray(bytes)
        }

        NbtType.INT_ARRAY -> {
            val length = checkLength(input.readInt(), MAX_ARRAY_BYTES / INT_BYTES, "int array")
            val values = IntArray(length)
            for (i in 0 until length) values[i] = input.readInt()
            NbtIntArray(values)
        }

        NbtType.LONG_ARRAY -> {
            val length = checkLength(input.readInt(), MAX_ARRAY_BYTES / LONG_BYTES, "long array")
            val values = LongArray(length)
            for (i in 0 until length) values[i] = input.readLong()
            NbtLongArray(values)
        }

        NbtType.LIST -> {
            val elementType = NbtType.fromId(input.readByte())
            val size = checkLength(input.readInt(), MAX_LIST_ITEMS, "list")
            val items = ArrayList<NbtTag>(size)
            for (i in 0 until size) items.add(readPayload(elementType))
            NbtList(elementType, items)
        }

        NbtType.COMPOUND -> readCompoundPayload()
    }

    private fun readString(): String {
        val length = input.readUnsignedShort()
        if (length == 0) return ""
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun checkLength(length: Int, max: Int, what: String): Int {
        if (length < 0 || length > max) {
            throw NbtFormatException("Invalid $what length: $length (max $max)")
        }
        return length
    }

    companion object {
        private const val INT_BYTES = 4
        private const val LONG_BYTES = 8

        /** Guard against decompression bombs: arrays and lists are bounded. */
        private const val MAX_ARRAY_BYTES = 64 * 1024 * 1024
        private const val MAX_LIST_ITEMS = 5_000_000

        /** Parses a root compound from raw NBT bytes. */
        fun read(bytes: ByteArray): NbtCompound =
            NbtReader(DataInputStream(ByteArrayInputStream(bytes))).readRoot()

        /** Parses a root compound from a stream positioned at the NBT start. */
        fun read(input: InputStream): NbtCompound =
            NbtReader(DataInputStream(input)).readRoot()
    }
}

class NbtFormatException(message: String) : IllegalArgumentException(message)
