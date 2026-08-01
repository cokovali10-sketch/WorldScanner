package org.worldscanner.core.nbt

import java.io.ByteArrayOutputStream
import java.io.DataOutput
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

/**
 * Binary NBT encoder. Mainly used by tests to build fixtures and available for
 * library consumers; the scanner itself is read-only.
 */
object NbtWriter {

    /** Serializes the root compound to binary NBT. */
    fun write(root: NbtCompound): ByteArray {
        val out = ByteArrayOutputStream(4096)
        val data = DataOutputStream(out)
        writeTag(data, NbtType.COMPOUND, "", root)
        return out.toByteArray()
    }

    private fun writeTag(out: DataOutput, type: NbtType, name: String, tag: NbtTag) {
        out.writeByte(type.id.toInt())
        writeString(out, name)
        writePayload(out, tag)
    }

    private fun writePayload(out: DataOutput, tag: NbtTag) {
        when (tag) {
            is NbtByte -> out.writeByte(tag.value.toInt())
            is NbtShort -> out.writeShort(tag.value.toInt())
            is NbtInt -> out.writeInt(tag.value)
            is NbtLong -> out.writeLong(tag.value)
            is NbtFloat -> out.writeFloat(tag.value)
            is NbtDouble -> out.writeDouble(tag.value)

            is NbtString -> writeString(out, tag.value)

            is NbtByteArray -> {
                out.writeInt(tag.value.size)
                out.write(tag.value)
            }

            is NbtIntArray -> {
                out.writeInt(tag.value.size)
                for (value in tag.value) out.writeInt(value)
            }

            is NbtLongArray -> {
                out.writeInt(tag.value.size)
                for (value in tag.value) out.writeLong(value)
            }

            is NbtList -> {
                val elementType = if (tag.items.isEmpty()) NbtType.END else tag.items.first().type
                out.writeByte(elementType.id.toInt())
                out.writeInt(tag.items.size)
                for (item in tag.items) writePayload(out, item)
            }

            is NbtCompound -> {
                for ((childName, child) in tag.entries) {
                    writeTag(out, child.type, childName, child)
                }
                out.writeByte(NbtType.END.id.toInt())
            }

            NbtEnd -> { /* no payload */ }
        }
    }

    private fun writeString(out: DataOutput, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= 0xFFFF) { "String too long: ${bytes.size} bytes" }
        out.writeShort(bytes.size)
        out.write(bytes)
    }
}
