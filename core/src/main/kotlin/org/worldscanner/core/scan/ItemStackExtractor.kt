package org.worldscanner.core.scan

import org.worldscanner.core.model.ItemStack
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtList
import org.worldscanner.core.nbt.compound
import org.worldscanner.core.nbt.compounds
import org.worldscanner.core.nbt.list

/**
 * Extracts item stacks out of parsed NBT compounds. This is the only place that
 * knows where Minecraft stores inventories; it never searches raw bytes.
 *
 * Handles both the modern (1.20.5+) block-entity layout (`container.Items`) and
 * the legacy layout (`Items`), plus entity inventory tags.
 */
object ItemStackExtractor {

    private val DIRECT_ITEM_COMPOUND_KEYS = listOf("Item", "HeadItem", "BodyItem", "LegsItem", "FeetItem")
    private val DIRECT_ITEM_LIST_KEYS = listOf("Items", "Inventory", "HandItems", "ArmorItems")

    /** Extracts the stacks stored directly on a block entity or entity compound. */
    fun extractDirect(holder: NbtCompound): List<ItemStack> {
        val stacks = ArrayList<ItemStack>(8)
        holder.compound("container")?.list("Items")?.let { appendList(it, stacks) }
        for (key in DIRECT_ITEM_LIST_KEYS) {
            holder.list(key)?.let { appendList(it, stacks) }
        }
        for (key in DIRECT_ITEM_COMPOUND_KEYS) {
            holder.compound(key)?.let { ItemStack.from(it)?.let(stacks::add) }
        }
        return stacks
    }

    private fun appendList(list: NbtList, out: MutableList<ItemStack>) {
        for (compound in list.compounds()) {
            ItemStack.from(compound)?.let(out::add)
        }
    }

    /**
     * Descends into a container item (shulker box, bundle, ...) and feeds every
     * stored stack to [consumer]. Both modern components and legacy `tag` data
     * are inspected.
     */
    fun extractNested(stack: ItemStack, consumer: (ItemStack) -> Unit) {
        val candidates = ArrayList<NbtCompound>(4)
        stack.components?.compound("minecraft:container")?.let(candidates::add)
        stack.components?.compound("minecraft:bundle_contents")?.let(candidates::add)
        stack.components?.compound("minecraft:block_entity_data")?.let(candidates::add)
        stack.tag?.compound("BlockEntityTag")?.let(candidates::add)

        for (candidate in candidates) {
            candidate.list("Items")?.let { for (compound in it.compounds()) ItemStack.from(compound)?.let(consumer) }
            candidate.compound("container")?.list("Items")?.let { for (compound in it.compounds()) ItemStack.from(compound)?.let(consumer) }
        }

        stack.tag?.list("BundleContents")?.let { for (compound in it.compounds()) ItemStack.from(compound)?.let(consumer) }
    }
}
