package org.worldscanner.core.anvil

import org.worldscanner.core.model.DimensionType
import java.nio.file.Files
import java.nio.file.Path

/**
 * Discovers `.mca` region files under a world folder and classifies them by
 * dimension:
 *
 *  - `<world>/region`                          -> OVERWORLD
 *  - `<world>/DIM-1/region`                    -> NETHER
 *  - `<world>/DIM1/region`                     -> END
 *
 * A path pointing directly at a `region` folder is treated as the overworld.
 */
object RegionDiscovery {

    data class RegionEntry(val path: Path, val dimension: DimensionType, val regionX: Int, val regionZ: Int)

    fun discover(worldRoot: Path): List<RegionEntry> {
        if (!Files.exists(worldRoot)) return emptyList()

        val candidates = mutableListOf<Pair<Path, DimensionType>>()
        val isRegionFolder = worldRoot.fileName?.toString() == "region"
        if (isRegionFolder) {
            candidates += worldRoot to DimensionType.OVERWORLD
        } else {
            candidates += worldRoot.resolve("region") to DimensionType.OVERWORLD
            candidates += worldRoot.resolve("DIM-1/region") to DimensionType.NETHER
            candidates += worldRoot.resolve("DIM1/region") to DimensionType.END
        }

        return candidates
            .flatMap { (dir, dim) -> listRegionFiles(dir, dim) }
            .sortedBy { it.regionX * 1_000_000 + it.regionZ }
    }

    private fun listRegionFiles(dir: Path, dimension: DimensionType): List<RegionEntry> {
        if (!Files.isDirectory(dir)) return emptyList()
        val entries = mutableListOf<RegionEntry>()
        Files.newDirectoryStream(dir, "*.mca").use { stream ->
            for (file in stream) {
                val (rx, rz) = parseRegionName(file.fileName.toString())
                entries += RegionEntry(file, dimension, rx, rz)
            }
        }
        return entries
    }

    fun parseRegionName(fileName: String): Pair<Int, Int> {
        val name = fileName.removeSuffix(".mca")
        val parts = name.split(".")
        if (parts.size != 3 || parts[0] != "r") return 0 to 0
        return (parts[1].toIntOrNull() ?: 0) to (parts[2].toIntOrNull() ?: 0)
    }
}
