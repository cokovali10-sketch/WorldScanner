package org.worldscanner.core.scan

import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.compound
import org.worldscanner.core.nbt.compounds
import org.worldscanner.core.nbt.list

/**
 * Locates block entities and entities inside a parsed chunk root.
 *
 * Handles both the modern layout (`sections[].block_entities`, root `entities`)
 * and the legacy pre-1.18 layout (`Level.TileEntities` / `Level.Entities`).
 */
object ChunkStructure {

    fun blockEntities(root: NbtCompound): List<NbtCompound> {
        val out = ArrayList<NbtCompound>(16)
        val level = root.compound("Level")

        root.list("sections")?.let { sections ->
            for (section in sections.compounds()) {
                section.list("block_entities")?.let { out += it.compounds() }
            }
        }
        root.list("block_entities")?.let { out += it.compounds() }
        level?.list("block_entities")?.let { out += it.compounds() }
        level?.list("TileEntities")?.let { out += it.compounds() }
        return out
    }

    fun entities(root: NbtCompound): List<NbtCompound> {
        val out = ArrayList<NbtCompound>(16)
        val level = root.compound("Level")

        root.list("entities")?.let { out += it.compounds() }
        level?.list("Entities")?.let { out += it.compounds() }
        return out
    }
}
