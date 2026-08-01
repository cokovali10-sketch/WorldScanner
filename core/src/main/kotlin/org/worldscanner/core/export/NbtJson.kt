package org.worldscanner.core.export

import org.worldscanner.core.nbt.NbtByte
import org.worldscanner.core.nbt.NbtByteArray
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtDouble
import org.worldscanner.core.nbt.NbtEnd
import org.worldscanner.core.nbt.NbtFloat
import org.worldscanner.core.nbt.NbtInt
import org.worldscanner.core.nbt.NbtIntArray
import org.worldscanner.core.nbt.NbtList
import org.worldscanner.core.nbt.NbtLong
import org.worldscanner.core.nbt.NbtLongArray
import org.worldscanner.core.nbt.NbtShort
import org.worldscanner.core.nbt.NbtString
import org.worldscanner.core.nbt.NbtTag

/** Serializes the in-memory NBT model to a JSON string. */
object NbtJson {

    fun toJson(tag: NbtTag): String = buildString { appendTag(tag) }

    fun toJson(compound: NbtCompound): String = toJson(compound as NbtTag)

    private fun StringBuilder.appendTag(tag: NbtTag) {
        when (tag) {
            is NbtByte -> append(tag.value.toInt())
            is NbtShort -> append(tag.value.toInt())
            is NbtInt -> append(tag.value)
            is NbtLong -> append(tag.value)
            is NbtFloat -> append(tag.value)
            is NbtDouble -> append(tag.value)
            is NbtString -> append(quote(tag.value))
            is NbtByteArray -> appendArray(tag.value.map { it.toString() })
            is NbtIntArray -> appendArray(tag.value.map { it.toString() })
            is NbtLongArray -> appendArray(tag.value.map { it.toString() })
            is NbtList -> {
                append('[')
                tag.items.forEachIndexed { index, item ->
                    if (index > 0) append(',')
                    appendTag(item)
                }
                append(']')
            }
            is NbtCompound -> {
                append('{')
                tag.entries.entries.forEachIndexed { index, (key, value) ->
                    if (index > 0) append(',')
                    append(quote(key))
                    append(':')
                    appendTag(value)
                }
                append('}')
            }
            NbtEnd -> append("null")
        }
    }

    private fun StringBuilder.appendArray(parts: List<String>) {
        append('[')
        parts.forEachIndexed { index, part ->
            if (index > 0) append(',')
            append(part)
        }
        append(']')
    }

    private fun quote(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (ch < ' ') sb.append("\\u").append(ch.code.toString(16).padStart(4, '0')) else sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
