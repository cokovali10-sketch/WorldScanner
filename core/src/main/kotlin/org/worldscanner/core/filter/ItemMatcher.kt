package org.worldscanner.core.filter

import org.worldscanner.core.model.ItemStack
import org.worldscanner.core.nbt.NbtCompound

/**
 * Decides whether a given item matches a search filter.
 *
 * The primary entry point works on a raw item [NbtCompound] (as stored in a
 * chunk), which keeps this interface usable directly by the region scanner.
 * A convenience overload for an already-decoded [ItemStack] is provided too.
 *
 * ```kotlin
 * val matcher: ItemMatcher = ComponentItemMatcher(ItemFilterParser.parse(
 *     "diamond_sword[enchantments={mending:1}]",
 * ).getOrThrow())
 *
 * if (matcher.matches(itemCompound)) { /* found */ }
 * ```
 */
fun interface ItemMatcher {

    /** Returns true when [nbtCompound] (an item stack) satisfies the filter. */
    fun matches(nbtCompound: NbtCompound): Boolean

    /** Matches an already-decoded [ItemStack] by re-serializing it to NBT. */
    fun matches(stack: ItemStack): Boolean = matches(ItemStack.toNbt(stack))
}
