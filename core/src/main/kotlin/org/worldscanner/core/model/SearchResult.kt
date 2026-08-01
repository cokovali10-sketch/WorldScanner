package org.worldscanner.core.model

/**
 * Where a matching item was found inside a chunk.
 */
sealed interface ItemSource {

    /** A tile/block entity such as a chest, barrel, furnace, hopper or shulker box. */
    data class BlockEntity(
        val id: String,
        val pos: BlockPos,
    ) : ItemSource

    /** A world entity such as a dropped item, armor stand, minecart or villager. */
    data class Entity(
        val id: String,
        val pos: BlockPos?,
        val uuid: String?,
    ) : ItemSource
}

/**
 * A single found item occurrence with its full container ancestry.
 */
data class FoundItem(
    val stack: ItemStack,
    /** Container path from the outermost to the innermost holder, e.g.
     * `[minecraft:chest, minecraft:shulker_box]`. Empty for a top-level match. */
    val containerPath: List<String>,
    val source: ItemSource,
)

/**
 * A user-facing search result combining a found item with its world location.
 */
data class SearchResult(
    val item: ItemStack,
    val dimension: DimensionType,
    val regionX: Int,
    val regionZ: Int,
    val chunkX: Int,
    val chunkZ: Int,
    val foundItem: FoundItem,
) {
    val source: ItemSource get() = foundItem.source
    val containerPath: List<String> get() = foundItem.containerPath
}
