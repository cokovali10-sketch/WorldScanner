package org.worldscanner.core.filter

import org.worldscanner.core.nbt.NbtByte
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtDouble
import org.worldscanner.core.nbt.NbtFloat
import org.worldscanner.core.nbt.NbtInt
import org.worldscanner.core.nbt.NbtList
import org.worldscanner.core.nbt.NbtLong
import org.worldscanner.core.nbt.NbtShort
import org.worldscanner.core.nbt.NbtString
import org.worldscanner.core.nbt.NbtTag
import org.worldscanner.core.nbt.NbtType
import java.util.Locale

/**
 * Thrown when an item filter string cannot be parsed.
 *
 * @param position character offset in the original input where parsing failed.
 */
class FilterSyntaxException(
    message: String,
    val position: Int,
) : IllegalArgumentException("$message at offset $position")

/**
 * Parses `/give`-style item filters into an [ItemFilter].
 *
 * Supported grammar (a pragmatic SNBT subset):
 * ```text
 * filter       := item-id component-list?
 * item-id      := unquoted id, e.g. "minecraft:diamond_sword" or "diamond_sword"
 * component-list := '[' component (',' component)* ']'
 * component    := key '=' value          // key: enchantments | damage | custom_name | custom_data | <any>
 * value        := compound | list | quoted-string | number | bool | bare-string
 * compound     := '{' entry (',' entry)* '}'   // entry: key ':' value
 * list         := '[' value (',' value)* ']'
 * ```
 *
 * Notes on the syntax:
 * - Inside a compound, an unquoted key ends at the first `:` (the key/value
 *   separator), matching Minecraft's own SNBT reader. A namespaced key such as
 *   `minecraft:mending` must therefore be quoted: `{"minecraft:mending":1}`.
 * - Unquoted *values* may contain `:` (resource locations), so
 *   `{id:minecraft:stone}` parses to key `id` -> string `minecraft:stone`.
 * - The item id is optional: `[damage=150]` and `damage=150` both work.
 *
 * @return [Result.success] with the parsed filter, or [Result.failure] carrying
 *         a [FilterSyntaxException].
 */
object ItemFilterParser {

    fun parse(input: String): Result<ItemFilter> = runCatching { Parser(input).parseFilter() }

    private class Parser(private val text: String) {
        private var pos = 0
        private val length = text.length

        fun parseFilter(): ItemFilter {
            skipWhitespace()
            if (pos >= length) fail("Empty filter")

            // Component-only filter that starts with the bracket: "[damage=150]".
            if (peek() == '[') {
                val conditions = parseComponentList()
                requireEnd()
                return ItemFilter(conditions = conditions)
            }

            // Try to read an item id first.
            val idStart = pos
            var id: String? = parseBareIdentifier()
            skipWhitespace()
            val conditions = when (peek()) {
                '[' -> parseComponentList()
                // Component-only filter written without brackets: "damage=150".
                '=' -> {
                    pos = idStart
                    id = null
                    parseComponentsUntilEnd()
                }
                null -> emptyList()
                else -> fail("Expected '[' after item id")
            }
            requireEnd()
            return ItemFilter(id = id?.ifEmpty { null }, conditions = conditions)
        }

        private fun parseComponentList(): List<ComponentCondition> {
            expect('[')
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return emptyList()
            }
            val out = ArrayList<ComponentCondition>(4)
            while (true) {
                out += parseComponent()
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        pos++
                        skipWhitespace()
                    }
                    ']' -> {
                        pos++
                        return out
                    }
                    null -> fail("Unterminated component list, expected ']'")
                    else -> fail("Expected ',' or ']' in component list")
                }
            }
        }

        private fun parseComponentsUntilEnd(): List<ComponentCondition> {
            val out = ArrayList<ComponentCondition>(4)
            while (true) {
                out += parseComponent()
                skipWhitespace()
                if (peek() == ',') {
                    pos++
                    skipWhitespace()
                } else {
                    return out
                }
            }
        }

        private fun parseComponent(): ComponentCondition {
            skipWhitespace()
            val key = parseKeyUntil('=').trim()
            if (key.isEmpty()) fail("Expected a component key")
            expect('=')
            skipWhitespace()
            val value = parseValue()
            return buildCondition(key, value)
        }

        private fun buildCondition(key: String, value: NbtTag): ComponentCondition =
            when (key.normalizeComponentKey()) {
                "enchantments" -> {
                    val compound = value as? NbtCompound
                        ?: fail("Component '$key' expects a compound, e.g. {mending:1}")
                    ComponentCondition.Enchantments(extractEnchantmentLevels(compound))
                }
                "damage" -> {
                    val damage = FilterNbt.extractInt(value)
                        ?: fail("Component '$key' expects a number, e.g. damage=150")
                    ComponentCondition.Damage(damage)
                }
                "custom_name" -> {
                    val name = when (value) {
                        is NbtString -> value.value
                        is NbtCompound -> (value["text"] as? NbtString)?.value
                            ?: fail("Component '$key' expects a string or a {text:...} component")
                        else -> fail("Component '$key' expects a string or a {text:...} component")
                    }
                    ComponentCondition.CustomName(name)
                }
                "custom_data" -> {
                    val compound = value as? NbtCompound
                        ?: fail("Component '$key' expects a compound, e.g. {owner:\"koca\"}")
                    ComponentCondition.CustomData(compound)
                }
                else -> ComponentCondition.Raw(key, value)
            }

        private fun extractEnchantmentLevels(compound: NbtCompound): Map<String, Int> {
            // Accept both {mending:1} and {levels:{mending:1},show_in_tooltip:1b}.
            val levels = compound["levels"] as? NbtCompound ?: compound
            return levels.entries.mapNotNull { (key, value) ->
                FilterNbt.extractInt(value)?.let { key to it }
            }.toMap()
        }

        // ---- SNBT value parsing ------------------------------------------------

        private fun parseValue(): NbtTag {
            skipWhitespace()
            return when (peek()) {
                '{' -> parseCompound()
                '[' -> parseList()
                '"', '\'' -> NbtString(parseQuotedString())
                null -> fail("Expected a value")
                else -> parsePrimitive()
            }
        }

        private fun parseCompound(): NbtCompound {
            expect('{')
            val entries = LinkedHashMap<String, NbtTag>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return NbtCompound(entries)
            }
            while (true) {
                skipWhitespace()
                val key = parseKeyUntil(':').trim()
                if (key.isEmpty()) fail("Expected a key in compound")
                expect(':')
                skipWhitespace()
                entries[key] = parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        pos++
                        skipWhitespace()
                        if (peek() == '}') {
                            pos++ // trailing comma allowed
                            return NbtCompound(entries)
                        }
                    }
                    '}' -> {
                        pos++
                        return NbtCompound(entries)
                    }
                    null -> fail("Unterminated compound, expected '}'")
                    else -> fail("Expected ',' or '}' in compound")
                }
            }
        }

        private fun parseList(): NbtList {
            expect('[')
            val items = ArrayList<NbtTag>(4)
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return NbtList(NbtType.END, items)
            }
            while (true) {
                skipWhitespace()
                items += parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        pos++
                        skipWhitespace()
                        if (peek() == ']') {
                            pos++
                            break
                        }
                    }
                    ']' -> {
                        pos++
                        break
                    }
                    null -> fail("Unterminated list, expected ']'")
                    else -> fail("Expected ',' or ']' in list")
                }
            }
            val elementType = items.firstOrNull()?.type ?: NbtType.END
            return NbtList(elementType, items)
        }

        private fun parsePrimitive(): NbtTag {
            val start = pos
            while (pos < length && !isPrimitiveDelimiter(text[pos])) pos++
            if (pos == start) fail("Expected a value")
            return parsePrimitiveToken(text.substring(start, pos))
        }

        private fun parsePrimitiveToken(token: String): NbtTag = when (token.lowercase(Locale.ROOT)) {
            "true" -> NbtByte(1)
            "false" -> NbtByte(0)
            else -> parseNumberOrString(token)
        }

        private fun parseNumberOrString(token: String): NbtTag {
            val suffix = when (token.last()) {
                'b', 'B', 's', 'S', 'l', 'L', 'f', 'F', 'd', 'D' -> if (token.length > 1) token.last() else null
                else -> null
            }
            val body = if (suffix != null) token.dropLast(1) else token
            if (!NUMBER_PATTERN.matches(body)) return NbtString(token)
            return if ('.' in body || 'e' in body || 'E' in body) {
                val double = body.toDouble()
                when (suffix) {
                    'f', 'F' -> NbtFloat(double.toFloat())
                    else -> NbtDouble(double)
                }
            } else {
                val long = body.toLong()
                when (suffix) {
                    'b', 'B' -> NbtByte(long.toByte())
                    's', 'S' -> NbtShort(long.toShort())
                    'l', 'L' -> NbtLong(long)
                    else -> NbtInt(long.toInt())
                }
            }
        }

        private fun parseKeyUntil(terminator: Char): String {
            if (peek() == '"' || peek() == '\'') return parseQuotedString()
            val start = pos
            while (pos < length && text[pos] != terminator) pos++
            return text.substring(start, pos)
        }

        private fun parseQuotedString(): String {
            val quote = text[pos]
            pos++
            val out = StringBuilder()
            while (pos < length) {
                when (val c = text[pos]) {
                    '\\' -> {
                        if (pos + 1 >= length) fail("Unterminated string")
                        pos++
                        out.append(unescape(text[pos]))
                        pos++
                    }
                    quote -> {
                        pos++
                        return out.toString()
                    }
                    else -> {
                        out.append(c)
                        pos++
                    }
                }
            }
            fail("Unterminated string")
        }

        private fun unescape(c: Char): Char = when (c) {
            'n' -> '\n'
            't' -> '\t'
            'r' -> '\r'
            'b' -> '\b'
            'f' -> '\u000C'
            else -> c
        }

        private fun parseBareIdentifier(): String {
            val start = pos
            while (pos < length) {
                val c = text[pos]
                if (c == '[' || c == '=' || c.isWhitespace()) break
                pos++
            }
            return text.substring(start, pos)
        }

        // ---- cursor helpers -----------------------------------------------------

        private fun peek(): Char? = if (pos < length) text[pos] else null

        private fun expect(c: Char) {
            if (peek() != c) fail("Expected '$c'")
            pos++
        }

        private fun requireEnd() {
            skipWhitespace()
            if (pos != length) fail("Unexpected trailing characters")
        }

        private fun skipWhitespace() {
            while (pos < length && text[pos].isWhitespace()) pos++
        }

        private fun isPrimitiveDelimiter(c: Char): Boolean =
            c.isWhitespace() || c == ',' || c == '}' || c == ']'

        private fun fail(message: String): Nothing = throw FilterSyntaxException(message, pos)

        private fun String.normalizeComponentKey(): String {
            val lower = lowercase(Locale.ROOT)
            return if (lower.startsWith("minecraft:")) lower.removePrefix("minecraft:") else lower
        }

        companion object {
            private val NUMBER_PATTERN = Regex("[+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?")
        }
    }
}
