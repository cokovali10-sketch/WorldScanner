package org.worldscanner.core.filter

import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtTag
import org.worldscanner.core.nbt.normalizeResourceLocation

/**
 * A parsed `/give`-style item filter.
 *
 * ```text
 * diamond_sword[enchantments={mending:1,sharpness:5},damage=150]
 * ```
 *
 * The [id] and [conditions] are both optional: a filter of just `diamond_sword`
 * matches that item id, while `[enchantments={mending:1}]` (or `damage=150`)
 * matches any item satisfying the component constraints.
 */
data class ItemFilter(
    /** Raw item id, may carry the `minecraft:` namespace. `null` matches any item. */
    val id: String? = null,
    /** Component constraints; an item must satisfy all of them to match. */
    val conditions: List<ComponentCondition> = emptyList(),
) {
    /** [id] without the `minecraft:` prefix, for equality comparisons. */
    val normalizedId: String? get() = id?.normalizeResourceLocation()
}

/**
 * A single constraint on one item component.
 *
 * Modern items (1.20.5+) store this data under the `components` branch with
 * namespaced keys such as `minecraft:enchantments`; legacy items keep it in the
 * `tag` branch. The matching code inspects both layouts.
 */
sealed interface ComponentCondition {

    /**
     * `enchantments={mending:1,sharpness:5}`.
     *
     * Each entry maps an enchantment id to a **minimum level** (an item with a
     * higher level still matches, useful for searching for strong gear).
     */
    data class Enchantments(val levels: Map<String, Int>) : ComponentCondition

    /** `damage=150` — exact durability damage applied to the item. */
    data class Damage(val value: Int) : ComponentCondition

    /** `custom_name="My Sword"` — exact display-name text. */
    data class CustomName(val text: String) : ComponentCondition

    /**
     * `custom_data={color:"red",owner:"koca"}`.
     *
     * Matching is *partial*: every entry in [required] must be present in the
     * item's custom data (nested compounds are checked recursively).
     */
    data class CustomData(val required: NbtCompound) : ComponentCondition

    /**
     * Fallback for any component key not covered above, e.g.
     * `attribute_modifiers=[...]`. Matched structurally against the modern
     * `components[key]` value.
     */
    data class Raw(val componentKey: String, val required: NbtTag) : ComponentCondition
}
