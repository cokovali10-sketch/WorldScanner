package org.worldscanner.core.anvil

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Random-access reader for a single Minecraft Anvil `.mca` region file.
 *
 * All reads are positional ([FileChannel.read] with an absolute position), which
 * makes a single [RegionFile] instance safe to share across worker threads while
 * a parallel chunk scan is running.
 */
class RegionFile private constructor(
    val path: Path,
    private val channel: FileChannel,
) : AutoCloseable {

    /** Sector offset and sector count for a chunk slot. Offset is in 4 KiB sectors. */
    data class ChunkLocation(val sectorOffset: Int, val sectorCount: Int) {
        val exists: Boolean get() = sectorOffset != 0 && sectorCount != 0
    }

    private val locations = arrayOfNulls<ChunkLocation>(CHUNK_COUNT)
    private val timestamps = IntArray(CHUNK_COUNT)

    init {
        val header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
        readFully(header, 0L)
        header.rewind()
        for (index in 0 until CHUNK_COUNT) {
            val offset = header.int
            val sectorOffset = offset ushr 8
            val sectorCount = offset and 0xFF
            locations[index] = ChunkLocation(sectorOffset, sectorCount)
        }
        for (index in 0 until CHUNK_COUNT) {
            timestamps[index] = header.int
        }
    }

    fun chunkLocation(index: Int): ChunkLocation? =
        if (index in 0 until CHUNK_COUNT) locations[index] else null

    fun timestamp(index: Int): Int =
        if (index in 0 until CHUNK_COUNT) timestamps[index] else 0

    fun hasChunk(index: Int): Boolean {
        val location = chunkLocation(index) ?: return false
        return location.exists
    }

    fun chunkCount(): Int = locations.count { it != null && it.exists }

    /** Reads a single compressed chunk payload. Returns null for empty slots. */
    fun readChunk(index: Int): ChunkPayload? {
        val location = chunkLocation(index) ?: return null
        if (!location.exists) return null

        val dataStart = location.sectorOffset.toLong() * SECTOR_BYTES
        val head = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.BIG_ENDIAN)
        readFully(head, dataStart)
        head.rewind()
        val chunkLength = head.int
        val compressionType = head.get().toInt() and 0xFF

        if (chunkLength < 1) {
            throw IOException("Invalid chunk length $chunkLength in ${path.fileName}")
        }
        val payloadLength = chunkLength - 1
        val payload = ByteArray(payloadLength)
        val payloadBuffer = ByteBuffer.wrap(payload)
        readFully(payloadBuffer, dataStart + HEADER_BYTES)
        return ChunkPayload(index, compressionType, payload)
    }

    private fun readFully(target: ByteBuffer, position: Long) {
        var cursor = position
        while (target.hasRemaining()) {
            val read = channel.read(target, cursor)
            if (read < 0) {
                throw IOException("Unexpected end of region file ${path.fileName}")
            }
            cursor += read
        }
    }

    override fun close() {
        channel.close()
    }

    companion object {
        const val SECTOR_BYTES = 4096
        const val HEADER_SIZE = SECTOR_BYTES * 2
        private const val HEADER_BYTES = 5
        const val CHUNK_COUNT = 1024

        fun open(path: Path): RegionFile {
            val channel = FileChannel.open(path, StandardOpenOption.READ)
            return RegionFile(path, channel)
        }
    }
}

/** A compressed chunk slice read from a region file. */
data class ChunkPayload(
    val chunkIndex: Int,
    val compressionType: Int,
    val data: ByteArray,
)
