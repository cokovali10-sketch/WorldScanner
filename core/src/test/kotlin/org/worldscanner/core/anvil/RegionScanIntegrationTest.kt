package org.worldscanner.core.anvil

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.nbt.NbtCompound
import org.worldscanner.core.nbt.NbtDouble
import org.worldscanner.core.nbt.NbtInt
import org.worldscanner.core.nbt.NbtList
import org.worldscanner.core.nbt.NbtString
import org.worldscanner.core.nbt.NbtType
import org.worldscanner.core.nbt.NbtWriter
import org.worldscanner.core.nbt.compoundOf
import org.worldscanner.core.scan.ScanQuery
import org.worldscanner.core.scan.WorldScanner
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegionScanIntegrationTest {

    @TempDir
    lateinit var tempDir: Path

    private fun chunkNbt(): NbtCompound {
        val chest = compoundOf(
            "id" to NbtString("minecraft:chest"),
            "x" to NbtInt(10),
            "y" to NbtInt(64),
            "z" to NbtInt(20),
            "container" to compoundOf(
                "Items" to NbtList(
                    NbtType.COMPOUND,
                    listOf(
                        compoundOf(
                            "id" to NbtString("minecraft:shulker_box"),
                            "count" to NbtInt(1),
                            "slot" to NbtInt(0),
                            "components" to compoundOf(
                                "minecraft:container" to compoundOf(
                                    "Items" to NbtList(
                                        NbtType.COMPOUND,
                                        listOf(
                                            compoundOf(
                                                "id" to NbtString("minecraft:netherite_ingot"),
                                                "count" to NbtInt(2),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        return compoundOf(
            "DataVersion" to NbtInt(4189),
            "sections" to NbtList(
                NbtType.COMPOUND,
                listOf(
                    compoundOf(
                        "Y" to NbtInt(0),
                        "block_entities" to NbtList(NbtType.COMPOUND, listOf(chest)),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `end to end scan of zlib compressed region file`() {
        val world = Files.createDirectories(tempDir.resolve("world"))
        val regionDir = Files.createDirectories(world.resolve("region"))
        writeRegionFile(regionDir.resolve("r.0.0.mca"), NbtWriter.write(chunkNbt()), 2)

        WorldScanner(parallelism = 2).use { scanner ->
            val report = scanner.scan(world, ScanQuery(itemTargets = setOf("netherite_ingot")))

            assertEquals(1, report.results.size)
            val result = report.results[0]
            assertEquals("netherite_ingot", result.item.normalizedId)
            assertEquals(2, result.item.count)
            assertEquals(listOf("shulker_box"), result.containerPath)
            assertEquals(DimensionType.OVERWORLD, result.dimension)
            assertEquals(0, result.regionX)
            assertEquals(0, result.regionZ)
            assertEquals(1, report.stats.regionsScanned.get())
            assertEquals(1, report.stats.chunksScanned.get())
        }
    }

    @Test
    fun `end to end scan of gzip compressed region file`() {
        val world = Files.createDirectories(tempDir.resolve("world"))
        val regionDir = Files.createDirectories(world.resolve("region"))
        writeRegionFile(regionDir.resolve("r.0.0.mca"), NbtWriter.write(chunkNbt()), 1)

        WorldScanner(parallelism = 2).use { scanner ->
            val report = scanner.scan(world, ScanQuery(itemTargets = setOf("netherite_ingot")))
            assertEquals(1, report.results.size)
        }
    }

    @Test
    fun `describe reports region and chunk counts`() {
        val world = Files.createDirectories(tempDir.resolve("world"))
        val regionDir = Files.createDirectories(world.resolve("region"))
        writeRegionFile(regionDir.resolve("r.0.0.mca"), NbtWriter.write(chunkNbt()), 2)
        writeRegionFile(regionDir.resolve("r.0.1.mca"), NbtWriter.write(chunkNbt()), 2)

        WorldScanner().use { scanner ->
            val summary = scanner.describe(world)
            assertEquals(2, summary.regionCount)
            assertEquals(2, summary.chunkCount)
            assertTrue(summary.dimensions.contains(DimensionType.OVERWORLD))
        }
    }

    @Test
    fun `empty world yields no results and no errors`() {
        val world = Files.createDirectories(tempDir.resolve("empty"))
        WorldScanner().use { scanner ->
            val report = scanner.scan(world, ScanQuery(itemTargets = setOf("diamond")))
            assertTrue(report.results.isEmpty())
            assertEquals(0, report.stats.regionsScanned.get())
        }
    }

    @Test
    fun `dimension filter restricts scanned regions`() {
        val world = Files.createDirectories(tempDir.resolve("world"))
        Files.createDirectories(world.resolve("region"))
        Files.createDirectories(world.resolve("DIM-1/region"))
        writeRegionFile(world.resolve("region/r.0.0.mca"), NbtWriter.write(chunkNbt()), 2)
        writeRegionFile(world.resolve("DIM-1/region/r.0.0.mca"), NbtWriter.write(chunkNbt()), 2)

        WorldScanner().use { scanner ->
            val overworld = scanner.scan(world, ScanQuery(itemTargets = setOf("netherite_ingot"), dimension = DimensionType.OVERWORLD))
            val nether = scanner.scan(world, ScanQuery(itemTargets = setOf("netherite_ingot"), dimension = DimensionType.NETHER))

            assertEquals(1, overworld.results.size)
            assertEquals(DimensionType.OVERWORLD, overworld.results[0].dimension)
            assertEquals(1, nether.results.size)
            assertEquals(DimensionType.NETHER, nether.results[0].dimension)
            assertEquals(1, overworld.stats.regionsScanned.get())
            assertEquals(1, nether.stats.regionsScanned.get())
        }
    }

    @Test
    fun `analyze honors the dimension filter`() {
        val world = Files.createDirectories(tempDir.resolve("world"))
        Files.createDirectories(world.resolve("region"))
        Files.createDirectories(world.resolve("DIM-1/region"))
        writeRegionFile(world.resolve("region/r.0.0.mca"), NbtWriter.write(chunkNbt()), 2)
        writeRegionFile(world.resolve("DIM-1/region/r.0.0.mca"), NbtWriter.write(chunkNbt()), 2)

        WorldScanner().use { scanner ->
            val overworld = scanner.analyze(world, dimension = DimensionType.OVERWORLD)
            val nether = scanner.analyze(world, dimension = DimensionType.NETHER)

            assertEquals(1, overworld.regionsByDimension[DimensionType.OVERWORLD])
            assertEquals(1, overworld.chunksByDimension[DimensionType.OVERWORLD])
            assertTrue(overworld.chunksByDimension[DimensionType.NETHER] == null)
            assertEquals(1, nether.chunksByDimension[DimensionType.NETHER])
        }
    }

    private fun writeRegionFile(path: Path, chunkBytes: ByteArray, compressionType: Int) {
        val compressed = when (compressionType) {
            1 -> gzip(chunkBytes)
            2 -> zlib(chunkBytes)
            else -> chunkBytes
        }

        val header = ByteBuffer.allocate(RegionFile.HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
        header.put(0)
        header.put(0)
        header.put(2)
        header.put(1) // chunk 0 located at sector 2, occupying 1 sector
        for (i in 1 until RegionFile.CHUNK_COUNT) {
            header.put(0)
            header.put(0)
            header.put(0)
            header.put(0)
        }
        for (i in 0 until RegionFile.CHUNK_COUNT) {
            header.putInt(1000 + i)
        }

        val chunkBuffer = ByteBuffer.allocate(RegionFile.SECTOR_BYTES).order(ByteOrder.BIG_ENDIAN)
        chunkBuffer.putInt(compressed.size + 1)
        chunkBuffer.put(compressionType.toByte())
        chunkBuffer.put(compressed)

        Files.createDirectories(path.parent)
        Files.write(path, header.array())
        val raf = java.io.RandomAccessFile(path.toFile(), "rw")
        raf.seek(2L * RegionFile.SECTOR_BYTES)
        raf.write(chunkBuffer.array())
        raf.close()
    }

    private fun gzip(data: ByteArray): ByteArray = ByteArrayOutputStream().use { out ->
        GZIPOutputStream(out).use { it.write(data) }
        out.toByteArray()
    }

    private fun zlib(data: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            out.write(buffer, 0, count)
        }
        deflater.end()
        return out.toByteArray()
    }
}
