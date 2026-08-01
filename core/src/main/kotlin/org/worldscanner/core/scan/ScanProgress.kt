package org.worldscanner.core.scan

/**
 * A snapshot of scan progress including throughput metrics.
 *
 * @param chunksDone chunks actually parsed so far
 * @param totalChunks total chunk slots across all selected region files
 * @param filesDone region files fully processed so far
 * @param totalFiles total region files in the scan
 * @param chunksPerSecond measured chunk processing rate (sliding 5s window)
 * @param filesPerSecond measured region-file completion rate
 * @param etaMillis estimated remaining time derived from the chunk rate, or null
 */
data class ScanProgressSnapshot(
    val chunksDone: Long,
    val totalChunks: Long,
    val filesDone: Int,
    val totalFiles: Int,
    val chunksPerSecond: Double,
    val filesPerSecond: Double,
    val etaMillis: Long?,
) {
    /** Progress as 0..100. */
    val percent: Int get() = if (totalChunks > 0) ((chunksDone * 100) / totalChunks).toInt().coerceIn(0, 100) else 0

    /** Progress as 0f..1f, for linear progress indicators. */
    val fraction: Float get() = if (totalChunks > 0) (chunksDone.toFloat() / totalChunks) else 0f

    val isComplete: Boolean get() = chunksDone >= totalChunks
}

/**
 * Receives throttled progress updates during a scan. The snapshot carries chunk,
 * file and speed/ETA metrics so consumers can render both a progress bar and a
 * live throughput readout.
 */
fun interface ScanProgress {
    fun onProgress(snapshot: ScanProgressSnapshot)
}
