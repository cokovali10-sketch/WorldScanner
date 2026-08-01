package org.worldscanner.core.export

import org.worldscanner.core.model.ItemSource
import org.worldscanner.core.model.SearchResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/** Exports scan results as a flat CSV table (one row per result). */
object CsvExporter {

    fun export(results: List<SearchResult>, path: Path) {
        Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { writer ->
            writer.write(
                "item,count,dimension,region_x,region_z,chunk_x,chunk_z,pos_x,pos_y,pos_z,source,holder,container_path,container_items,container_item_count\n",
            )
            for (result in results) {
                val source = result.source
                val (posX, posY, posZ) = when (source) {
                    is ItemSource.BlockEntity -> Triple(source.pos.x, source.pos.y, source.pos.z)
                    is ItemSource.Entity -> Triple(source.pos?.x ?: 0, source.pos?.y ?: 0, source.pos?.z ?: 0)
                }
                val holder = when (source) {
                    is ItemSource.BlockEntity -> source.id
                    is ItemSource.Entity -> source.id
                }
                val sourceType = if (source is ItemSource.BlockEntity) "block_entity" else "entity"
                val containerItems = result.containerContents.joinToString(";") {
                    "${it.normalizedId} x${it.count}"
                }
                val cells = listOf(
                    result.item.normalizedId,
                    result.item.count.toString(),
                    result.dimension.name.lowercase(),
                    result.regionX.toString(),
                    result.regionZ.toString(),
                    result.chunkX.toString(),
                    result.chunkZ.toString(),
                    posX.toString(),
                    posY.toString(),
                    posZ.toString(),
                    sourceType,
                    holder,
                    result.containerPath.joinToString(";"),
                    containerItems,
                    result.containerContents.size.toString(),
                )
                writer.write(cells.joinToString(",") { escapeCsv(it) })
                writer.write("\n")
            }
        }
    }

    private fun escapeCsv(value: String): String {
        if (',' in value || '"' in value || '\n' in value || '\r' in value) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}
