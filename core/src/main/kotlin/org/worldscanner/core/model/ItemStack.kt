package org.worldscanner.core.model

import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtInt
import org.worldscanner.core.nbt.NbtString
import org.worldscanner.core.nbt.compound
import org.worldscanner.core.nbt.compoundOf
import org.worldscanner.core.nbt.int
import org.worldscanner.core.nbt.normalizeResourceLocation
import org.worldscanner.core.nbt.string

/**
 * A single item stack, decoded from either the modern (1.20.5+) format
 * (`id`/`count`/`components`) or the legacy format (`id`/`Count`/`tag`).
 */
data class ItemStack(
    /** Raw resource location, e.g. "minecraft:diamond". */
    val id: String,
    /** Stack size. */
    val count: Int,
    /** Slot inside the immediate container, when known. */
    val slot: Int?,
    /** Modern item data (1.20.5+): `components` compound. */
    val components: NbtCompound?,
    /** Legacy item data: `tag` compound. */
    val tag: NbtCompound?,
) {
    /** Normalized id for equality checks, e.g. "diamond". */
    val normalizedId: String get() = id.normalizeResourceLocation()

    fun matches(target: String): Boolean = normalizedId == target.normalizeResourceLocation()

    companion object {
        /** Extracts an [ItemStack] from an item NBT compound, or null if it is not an item. */
        fun from(compound: NbtCompound): ItemStack? {
            val id = compound.string("id") ?: return null
            if (id.isBlank()) return null
            val count = compound.int("count") ?: compound.int("Count") ?: 1
            val slot = compound.int("slot") ?: compound.int("Slot")
            val components = compound.compound("components")
            val tag = compound.compound("tag")
            return ItemStack(id, count, slot, components, tag)
        }

        /** Renders the stack back to an NBT compound (used by exporters/tests). */
        fun toNbt(stack: ItemStack): NbtCompound {
            val entries = mutableListOf<Pair<String, org.worldscanner.core.nbt.NbtTag>>(
                "id" to NbtString(stack.id),
            )
            if (stack.count != 1) entries += "count" to NbtInt(stack.count)
            if (stack.slot != null) entries += "slot" to NbtInt(stack.slot)
            stack.components?.let { entries += "components" to it }
            stack.tag?.let { entries += "tag" to it }
            return compoundOf(*entries.toTypedArray())
        }
    }
}
