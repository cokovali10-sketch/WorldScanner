package org.worldscanner.core.export

import org.worldscanner.core.model.ItemSource
import org.worldscanner.core.model.ItemStack
import org.worldscanner.core.model.SearchResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Exports scan results to JSON, preserving the full item structure including
 * `components` and legacy `tag` NBT data plus every container's contents.
 */
object JsonExporter {

    fun export(results: List<SearchResult>, path: Path) {
        Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { writer ->
            writer.write("[\n")
            for ((index, result) in results.withIndex()) {
                writer.write("  ")
                writer.write(resultToJson(result))
                if (index < results.lastIndex) writer.write(",")
                writer.write("\n")
            }
            writer.write("]\n")
        }
    }

    fun resultToJson(result: SearchResult): String = buildString {
        append('{')
        append("\"item\":").append(NbtJson.toJson(ItemStack.toNbt(result.item)))
        append(",\"dimension\":").append(quote(result.dimension.name.lowercase()))
        append(",\"region\":[").append(result.regionX).append(',').append(result.regionZ).append(']')
        append(",\"chunk\":[").append(result.chunkX).append(',').append(result.chunkZ).append(']')

        when (val source = result.source) {
            is ItemSource.BlockEntity -> {
                append(",\"position\":[")
                append(source.pos.x).append(',').append(source.pos.y).append(',').append(source.pos.z)
                append("],\"source\":\"block_entity\",\"block\":").append(quote(source.id))
            }
            is ItemSource.Entity -> {
                append(",\"position\":[")
                append(source.pos?.x ?: 0).append(',').append(source.pos?.y ?: 0).append(',').append(source.pos?.z ?: 0)
                append("],\"source\":\"entity\",\"entity\":").append(quote(source.id))
            }
        }

        append(",\"containerPath\":[")
        for ((i, container) in result.containerPath.withIndex()) {
            if (i > 0) append(',')
            append(quote(container))
        }
        append(']')

        append(",\"containerContents\":[")
        for ((i, stack) in result.containerContents.withIndex()) {
            if (i > 0) append(',')
            append(NbtJson.toJson(ItemStack.toNbt(stack)))
        }
        append(']')
        append('}')
    }

    private fun quote(value: String): String = NbtJson.toJson(org.worldscanner.core.nbt.NbtString(value))
}
