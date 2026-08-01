package org.worldscanner.core.nbt

/**
 * NBT tag type identifiers as defined by the Minecraft binary NBT format.
 */
enum class NbtType(val id: Byte) {
    END(0),
    BYTE(1),
    SHORT(2),
    INT(3),
    LONG(4),
    FLOAT(5),
    DOUBLE(6),
    BYTE_ARRAY(7),
    STRING(8),
    LIST(9),
    COMPOUND(10),
    INT_ARRAY(11),
    LONG_ARRAY(12);

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: Byte): NbtType =
            byId[id] ?: throw IllegalArgumentException("Unknown NBT tag type: $id")
    }
}
