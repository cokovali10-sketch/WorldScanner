package org.worldscanner.cli

import org.worldscanner.core.model.SearchResult
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object JsonExporter {

    fun export(results: List<SearchResult>, path: Path) {
        Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { writer ->
            writer.write("[\n")
            for ((index, result) in results.withIndex()) {
                writer.write("  ${resultToJson(result)}")
                if (index < results.lastIndex) writer.write(",")
                writer.write("\n")
            }
            writer.write("]\n")
        }
    }

    private fun resultToJson(result: SearchResult): String {
        val source = result.source
        return buildString {
            append("{")
            append("\"item\":").append(jsonString(result.item.normalizedId)).append(",")
            append("\"count\":").append(result.item.count).append(",")
            append("\"dimension\":").append(jsonString(result.dimension.name.lowercase())).append(",")
            append("\"region\":[").append(result.regionX).append(",").append(result.regionZ).append("],")
            append("\"chunk\":[").append(result.chunkX).append(",").append(result.chunkZ).append("],")
            append("\"position\":")
            when (source) {
                is org.worldscanner.core.model.ItemSource.BlockEntity -> {
                    append("[")
                    append(source.pos.x).append(",")
                    append(source.pos.y).append(",")
                    append(source.pos.z)
                    append("]")
                    append(",\"source\":\"block_entity\",\"block\":").append(jsonString(source.id))
                }
                is org.worldscanner.core.model.ItemSource.Entity -> {
                    append("[")
                    append(source.pos?.x ?: 0).append(",")
                    append(source.pos?.y ?: 0).append(",")
                    append(source.pos?.z ?: 0)
                    append("]")
                    append(",\"source\":\"entity\",\"entity\":").append(jsonString(source.id))
                }
            }
            append(",")
            append("\"containerPath\":[")
            for ((i, container) in result.containerPath.withIndex()) {
                append(jsonString(container))
                if (i < result.containerPath.lastIndex) append(",")
            }
            append("]")
            append("}")
        }
    }

    private fun jsonString(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (ch < ' ') sb.append("\\u").append(ch.code.toString(16).padStart(4, '0')) else sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
