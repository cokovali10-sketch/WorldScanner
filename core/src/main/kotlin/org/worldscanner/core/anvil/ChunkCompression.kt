package org.worldscanner.core.anvil

import net.jpountz.lz4.LZ4Factory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater

/**
 * Decompresses chunk payloads by their Anvil compression type:
 *
 *  1 = gzip, 2 = zlib, 3 = uncompressed, 4 = LZ4 block.
 *
 * All buffers are allocated per call; callers reuse one [ChunkCompression]
 * instance per worker thread to keep pressure on the GC low.
 */
class ChunkCompression {

    private val inflater = Inflater()

    /** Decompresses [data] of [compressionType] into a fresh byte array. */
    fun decompress(compressionType: Int, data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(maxOf(1024, data.size * 3))
        when (compressionType) {
            1 -> gunzip(data, out)
            2 -> zlib(data, out)
            3 -> return data
            4 -> return lz4(data)
            else -> throw IllegalArgumentException("Unsupported chunk compression type: $compressionType")
        }
        return out.toByteArray()
    }

    private fun zlib(data: ByteArray, out: ByteArrayOutputStream) {
        inflater.reset()
        inflater.setInput(data)
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        throw DataFormatException("zlib stream truncated")
                    }
                    if (inflater.finished()) break
                }
                checkOutputSize(out, count)
                out.write(buffer, 0, count)
            }
        } catch (e: DataFormatException) {
            throw ChunkDecompressionException("Invalid zlib chunk data", e)
        }
    }

    private fun gunzip(data: ByteArray, out: ByteArrayOutputStream) {
        try {
            GZIPInputStream(ByteArrayInputStream(data)).use { gzip ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val count = gzip.read(buffer)
                    if (count < 0) break
                    checkOutputSize(out, count)
                    out.write(buffer, 0, count)
                }
            }
        } catch (e: Exception) {
            throw ChunkDecompressionException("Invalid gzip chunk data", e)
        }
    }

    private fun lz4(data: ByteArray): ByteArray {
        val decompressor = LZ4Factory.fastestInstance().fastDecompressor()
        val dest = ByteArray(MAX_DECOMPRESSED_SIZE)
        val decompressed = try {
            decompressor.decompress(data, dest)
        } catch (e: Exception) {
            throw ChunkDecompressionException("Invalid LZ4 chunk data", e)
        }
        return dest.copyOf(decompressed)
    }

    private fun checkOutputSize(out: ByteArrayOutputStream, extra: Int) {
        if (out.size().toLong() + extra > MAX_DECOMPRESSED_SIZE) {
            throw ChunkDecompressionException(
                "Decompressed chunk exceeds safety limit of ${MAX_DECOMPRESSED_SIZE / (1024 * 1024)} MiB",
            )
        }
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAX_DECOMPRESSED_SIZE = 64 * 1024 * 1024
    }
}

class ChunkDecompressionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
