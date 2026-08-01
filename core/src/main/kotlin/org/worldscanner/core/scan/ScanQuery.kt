package org.worldscanner.core.scan

import org.worldscanner.core.filter.ItemMatcher
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.nbt.normalizeResourceLocation

/**
 * Search configuration for a [WorldScanner] run.
 */
class ScanQuery(
    itemTargets: Set<String> = emptySet(),
    /**
     * Optional component matcher (see [ItemMatcher]). When set it drives the
     * search instead of (or in addition to) [itemTargets]; it can match on
     * enchantments, damage, custom names, custom data and arbitrary components.
     */
    val matcher: ItemMatcher? = null,
    val dimension: DimensionType? = null,
    val regionX: Int? = null,
    val regionZ: Int? = null,
    /** Global result cap; scanning stops early once reached. */
    val limit: Int = Int.MAX_VALUE,
    /** Descend into nested containers such as shulker boxes inside chests. */
    val includeNested: Boolean = true,
    /** Recursion guard for nested containers. */
    val maxNestingDepth: Int = 8,
) {
    /** Targets normalized (without the `minecraft:` prefix). */
    val itemTargets: Set<String> = itemTargets.map { it.normalizeResourceLocation() }.toSet()

    /** True when nothing to look for; scan can short-circuit immediately. */
    val isEmpty: Boolean get() = itemTargets.isEmpty() && matcher == null
}
