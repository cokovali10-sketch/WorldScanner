package org.worldscanner.core.filter

import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtString
import org.worldscanner.core.nbt.NbtTag
import org.worldscanner.core.nbt.compound
import org.worldscanner.core.nbt.compounds
import org.worldscanner.core.nbt.list
import org.worldscanner.core.nbt.normalizeResourceLocation
import org.worldscanner.core.nbt.string

/**
 * [ItemMatcher] implementation backed by a parsed [ItemFilter].
 *
 * Reads the modern (1.20.5+) item layout where components live in the
 * `components` compound keyed by namespaced names such as
 * `minecraft:enchantments`, `minecraft:damage`, `minecraft:custom_name` and
 * `minecraft:custom_data`; it also understands the legacy (pre-1.20.5) `tag`
 * layout so the same filter works across all supported versions.
 *
 * Matching semantics per condition:
 * - **Enchantments**: every requested enchantment must be present with a level
 *   >= the requested one (modern `components...levels` map or legacy
 *   `tag.Enchantments` list).
 * - **Damage**: exact integer equality (modern `components` int or legacy
 *   `tag.Damage`).
 * - **Custom name**: exact text comparison (modern text component or legacy
 *   `tag.display.Name`, with a tiny JSON `{"text":...}` extractor).
 * - **Custom data**: partial compound match (modern
 *   `components["minecraft:custom_data"]` or the whole legacy `tag`).
 * - **Raw**: structural "contains" match against a modern component value.
 */
class ComponentItemMatcher(private val filter: ItemFilter) : ItemMatcher {

    override fun matches(nbtCompound: NbtCompound): Boolean {
        val id = nbtCompound.string("id") ?: return false
        val wanted = filter.normalizedId
        if (wanted != null && id.normalizeResourceLocation() != wanted) return false

        val components = nbtCompound.compound("components")
        val legacyTag = nbtCompound.compound("tag")
        return filter.conditions.all { condition -> condition.matches(components, legacyTag) }
    }
}

private fun ComponentCondition.matches(components: NbtCompound?, legacyTag: NbtCompound?): Boolean =
    when (this) {
        is ComponentCondition.Enchantments -> matchesEnchantments(components, legacyTag)
        is ComponentCondition.Damage -> matchesDamage(components, legacyTag)
        is ComponentCondition.CustomName -> matchesCustomName(components, legacyTag)
        is ComponentCondition.CustomData -> matchesCustomData(components, legacyTag)
        is ComponentCondition.Raw -> matchesRaw(components)
    }

private fun ComponentCondition.Enchantments.matchesEnchantments(
    components: NbtCompound?,
    legacyTag: NbtCompound?,
): Boolean {
    val modernLevels: Map<String, Int> = components
        ?.compound("minecraft:enchantments")
        ?.compound("levels")
        ?.entries
        ?.mapNotNull { (key, value) -> FilterNbt.extractInt(value)?.let { key to it } }
        ?.toMap()
        .orEmpty()

    val legacyEnchantments = legacyTag?.list("Enchantments")?.compounds().orEmpty()

    return levels.all { (enchantment, minLevel) ->
        val wanted = enchantment.normalizeResourceLocation()
        val modernLevel = modernLevels.entries
            .firstOrNull { it.key.normalizeResourceLocation() == wanted }
            ?.value
        if (modernLevel != null) {
            modernLevel >= minLevel
        } else {
            val legacyLevel = legacyEnchantments
                .firstOrNull { it.string("id")?.normalizeResourceLocation() == wanted }
                ?.let { FilterNbt.extractInt(it["lvl"]) }
            legacyLevel != null && legacyLevel >= minLevel
        }
    }
}

private fun ComponentCondition.Damage.matchesDamage(
    components: NbtCompound?,
    legacyTag: NbtCompound?,
): Boolean {
    val modern = components?.let {
        FilterNbt.extractInt(it["minecraft:damage"])
            ?: it.compound("minecraft:damage")?.let { damage -> FilterNbt.extractInt(damage["damage"]) }
    }
    val legacy = legacyTag?.let { FilterNbt.extractInt(it["Damage"]) }
    val actual = modern ?: legacy
    return actual != null && actual == value
}

private fun ComponentCondition.CustomName.matchesCustomName(
    components: NbtCompound?,
    legacyTag: NbtCompound?,
): Boolean {
    val modern = components?.get("minecraft:custom_name")?.let(::extractNameText)
    if (modern != null) return modern == text
    val legacy = legacyTag?.compound("display")?.string("Name")?.let(::extractNameText)
    return legacy == text
}

private fun ComponentCondition.CustomData.matchesCustomData(
    components: NbtCompound?,
    legacyTag: NbtCompound?,
): Boolean {
    val modern = components?.compound("minecraft:custom_data")
    if (modern != null) return FilterNbt.nbtContains(required, modern)
    // Legacy items merged /give custom data directly into `tag`.
    return legacyTag != null && FilterNbt.nbtContains(required, legacyTag)
}

private fun ComponentCondition.Raw.matchesRaw(components: NbtCompound?): Boolean {
    if (components == null) return false
    val direct = components[componentKey]
    val namespaced = if (componentKey.contains(':')) null else components["minecraft:$componentKey"]
    val actual = direct ?: namespaced ?: return false
    return FilterNbt.nbtContains(required, actual)
}

/** Extracts the visible text from an NBT text component (string or `{text:...}`). */
internal fun extractNameText(tag: NbtTag?): String? = when (tag) {
    is NbtString -> extractNameText(tag.value)
    is NbtCompound -> (tag["text"] as? NbtString)?.value
    else -> null
}

/** Extracts `{"text":"..."}` text from a raw string; falls back to the raw string. */
internal fun extractNameText(raw: String): String {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{")) return raw
    return extractJsonTextField(trimmed) ?: raw
}

private const val JSON_TEXT_KEY = "\"text\""

private fun extractJsonTextField(json: String): String? {
    val keyIndex = json.indexOf(JSON_TEXT_KEY)
    if (keyIndex < 0) return null
    var i = keyIndex + JSON_TEXT_KEY.length
    while (i < json.length && json[i].isWhitespace()) i++
    if (i >= json.length || json[i] != ':') return null
    i++
    while (i < json.length && json[i].isWhitespace()) i++
    if (i >= json.length || json[i] != '"') return null
    i++
    val out = StringBuilder()
    while (i < json.length) {
        when (val c = json[i]) {
            '\\' -> {
                if (i + 1 >= json.length) return null
                out.append(
                    when (json[i + 1]) {
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        '"' -> '"'
                        '\\' -> '\\'
                        else -> json[i + 1]
                    },
                )
                i += 2
            }
            '"' -> return out.toString()
            else -> {
                out.append(c)
                i++
            }
        }
    }
    return null
}
