package org.worldscanner.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.worldscanner.core.SearchEngine
import org.worldscanner.core.export.CsvExporter
import org.worldscanner.core.export.JsonExporter
import org.worldscanner.core.filter.ComponentItemMatcher
import org.worldscanner.core.filter.FilterSyntaxException
import org.worldscanner.core.filter.ItemFilterParser
import org.worldscanner.core.model.DimensionType
import org.worldscanner.core.model.ItemSource
import org.worldscanner.core.model.SearchResult
import org.worldscanner.core.scan.ScanProgressSnapshot
import org.worldscanner.core.scan.ScanQuery
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.text.NumberFormat
import java.util.Locale as JavaLocale

/** A single row rendered in the results table. */
data class ResultRow(
    val dimension: String,
    val region: String,
    val coords: String,
    val chunk: String,
    val container: String,
    val item: String,
    val sourceBlockId: String? = null,
    val hasInventory: Boolean = false,
)

/** Snapshot of everything the scan UI renders. */
data class UiState(
    val path: String = "",
    val filter: String = "",
    val filterError: String? = null,
    val pathError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val progressFraction: Float = 0f,
    val progressLabel: String? = null,
    val results: List<SearchResult> = emptyList(),
    val resultCountLabel: String = "",
    val selectedIndex: Int? = null,
    val lastSummary: String? = null,
    val exportStatus: String? = null,
) {
    val filterValid: Boolean get() = filter.isNotBlank() && filterError == null
    val pathValid: Boolean get() = path.isNotBlank() && pathError == null
    val canStart: Boolean get() = filterValid && pathValid && !isLoading
}

/**
 * MVVM view-model for the scan screen. Holds the entire UI state in a
 * [StateFlow]. Scanning runs on [Dispatchers.Default] (via the core flow) and
 * results stream in live through [SearchEngine.findFlow]; state mutations
 * happen on [Dispatchers.Main] so Compose updates stay on the UI thread.
 */
class ScanViewModel(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    /** Latest progress snapshot, written by the scanner worker threads. */
    @Volatile
    private var latestProgress: ScanProgressSnapshot? = null

    /** Releases the scope when the owning window is closed. */
    fun dispose() {
        scope.cancel()
        _state.value = UiState()
    }

    fun setPath(value: String) {
        _state.value = _state.value.copy(
            path = value,
            pathError = validatePath(value),
            error = null,
        )
    }

    fun setFilter(value: String) {
        _state.value = _state.value.copy(
            filter = value,
            filterError = validateFilter(value),
            error = null,
        )
    }

    fun startScan() {
        val current = _state.value
        if (current.isLoading || !current.canStart) return

        val worldRoot: Path
        try {
            worldRoot = Paths.get(current.path)
        } catch (e: InvalidPathException) {
            _state.value = _state.value.copy(error = "Invalid path: ${e.message}")
            return
        }
        if (!Files.isDirectory(worldRoot)) {
            _state.value = _state.value.copy(pathError = "Not a directory", error = null)
            return
        }

        val matcher = try {
            ComponentItemMatcher(ItemFilterParser.parse(current.filter.trim()).getOrThrow())
        } catch (e: FilterSyntaxException) {
            _state.value = _state.value.copy(filterError = e.message, error = null)
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            error = null,
            pathError = null,
            filterError = null,
            results = emptyList(),
            resultCountLabel = "",
            progressFraction = 0f,
            progressLabel = "Preparing scan...",
            selectedIndex = null,
            lastSummary = null,
            exportStatus = null,
        )
        latestProgress = null

        scanJob?.cancel()
        scanJob = scope.launch {
            val found = ArrayList<SearchResult>()
            var lastFlushMs = 0L
            try {
                SearchEngine.findFlow(
                    worldRoot = worldRoot,
                    query = ScanQuery(matcher = matcher),
                    onProgress = ::onProgress,
                ).collect { result ->
                    found += result
                    val now = System.currentTimeMillis()
                    if (now - lastFlushMs >= 500) {
                        lastFlushMs = now
                        publishResults(found)
                    }
                }
                publishResults(found)
                _state.value = _state.value.copy(
                    isLoading = false,
                    progressFraction = 1f,
                    progressLabel = null,
                    resultCountLabel = resultCountLabel(found.size),
                    lastSummary = buildSummary(found.size),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Scan failed: ${e.message}",
                    progressLabel = null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}",
                    progressLabel = null,
                )
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _state.value = _state.value.copy(
            isLoading = false,
            progressLabel = null,
            error = "Scan cancelled.",
        )
    }

    fun selectRow(index: Int) {
        _state.value = _state.value.copy(selectedIndex = index)
    }

    fun closeInspector() {
        _state.value = _state.value.copy(selectedIndex = null)
    }

    /** Builds a copyable `/tp` command for the result at [index], or null. */
    fun tpCommandFor(index: Int): String? {
        val result = _state.value.results.getOrNull(index) ?: return null
        val pos = (result.source as? ItemSource.BlockEntity)?.pos
            ?: (result.source as? ItemSource.Entity)?.pos ?: return null
        val teleport = "/tp @s ${pos.x} ${pos.y} ${pos.z}"
        val dimension = result.dimension.executionNamespace() ?: return teleport
        return "/execute in $dimension run $teleport"
    }

    fun exportCsv(path: Path): Boolean = export { CsvExporter.export(it, path) }

    fun exportJson(path: Path): Boolean = export { JsonExporter.export(it, path) }

    private fun export(write: (List<SearchResult>) -> Unit): Boolean {
        val results = _state.value.results
        if (results.isEmpty()) {
            _state.value = _state.value.copy(exportStatus = "Nothing to export yet.")
            return false
        }
        return try {
            write(results)
            _state.value = _state.value.copy(exportStatus = "Exported ${results.size} results.")
            true
        } catch (e: IOException) {
            _state.value = _state.value.copy(exportStatus = "Export failed: ${e.message}")
            false
        } catch (e: Exception) {
            _state.value = _state.value.copy(exportStatus = "Export failed: ${e.message}")
            false
        }
    }

    private fun onProgress(snapshot: ScanProgressSnapshot) {
        latestProgress = snapshot
    }

    private fun publishResults(found: List<SearchResult>) {
        val snapshot = latestProgress
        _state.value = _state.value.copy(
            results = found,
            resultCountLabel = resultCountLabel(found.size),
            progressFraction = snapshot?.fraction ?: _state.value.progressFraction,
            progressLabel = snapshot?.let(::formatProgress),
        )
    }

    private fun formatProgress(snapshot: ScanProgressSnapshot): String {
        val nf = NumberFormat.getIntegerInstance(JavaLocale.US)
        val speed = if (snapshot.chunksPerSecond >= 10.0) {
            nf.format(snapshot.chunksPerSecond.toLong())
        } else {
            "%.1f".format(snapshot.chunksPerSecond)
        }
        val eta = snapshot.etaMillis?.let { formatEta(it) } ?: "…"
        return "Scanned ${nf.format(snapshot.chunksDone)} / ${nf.format(snapshot.totalChunks)} " +
            "chunks (${snapshot.percent}%) · ${snapshot.filesDone}/${snapshot.totalFiles} regions · " +
            "$speed ch/s · ETA $eta"
    }

    private fun formatEta(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    private fun validateFilter(value: String): String? {
        if (value.isBlank()) return null
        return ItemFilterParser.parse(value.trim()).exceptionOrNull()?.message
    }

    private fun validatePath(value: String): String? {
        if (value.isBlank()) return null
        return try {
            val p = Paths.get(value)
            if (!Files.exists(p)) "Path does not exist" else null
        } catch (e: InvalidPathException) {
            "Invalid path"
        }
    }

    private fun resultCountLabel(count: Int): String {
        val nf = NumberFormat.getIntegerInstance(JavaLocale.US)
        return when (count) {
            0 -> "No matching items found"
            1 -> "1 result"
            else -> "${nf.format(count)} results"
        }
    }

    private fun buildSummary(count: Int): String =
        "Scan finished. ${resultCountLabel(count)}."
}

private fun DimensionType.executionNamespace(): String? = when (this) {
    DimensionType.OVERWORLD -> "minecraft:overworld"
    DimensionType.NETHER -> "minecraft:the_nether"
    DimensionType.END -> "minecraft:the_end"
    DimensionType.UNKNOWN -> null
}

internal fun org.worldscanner.core.model.DimensionType.displayName(): String = when (this) {
    org.worldscanner.core.model.DimensionType.OVERWORLD -> "Overworld"
    org.worldscanner.core.model.DimensionType.NETHER -> "Nether"
    org.worldscanner.core.model.DimensionType.END -> "End"
    org.worldscanner.core.model.DimensionType.UNKNOWN -> "Unknown"
}

internal fun SearchResult.toRow(): ResultRow {
    val pos = (source as? ItemSource.BlockEntity)?.pos
        ?: (source as? ItemSource.Entity)?.pos
    return ResultRow(
        dimension = dimension.displayName(),
        region = "$regionX.$regionZ",
        coords = pos?.let { "(${it.x}, ${it.y}, ${it.z})" } ?: "—",
        chunk = "$chunkX, $chunkZ",
        container = containerPath.joinToString(" → ").ifEmpty { "surface" },
        item = item.id,
        sourceBlockId = (source as? ItemSource.BlockEntity)?.id,
        hasInventory = containerContents.isNotEmpty(),
    )
}
