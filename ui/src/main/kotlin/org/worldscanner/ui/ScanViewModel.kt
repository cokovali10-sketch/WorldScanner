package org.worldscanner.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.worldscanner.core.SearchEngine
import org.worldscanner.core.filter.ComponentItemMatcher
import org.worldscanner.core.filter.FilterSyntaxException
import org.worldscanner.core.filter.ItemFilterParser
import org.worldscanner.core.model.ItemSource
import org.worldscanner.core.model.SearchResult
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
)

/** Snapshot of everything the scan UI renders. */
data class UiState(
    val path: String = "",
    val filter: String = "",
    val filterError: String? = null,
    val pathError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val progressLabel: String? = null,
    val results: List<ResultRow> = emptyList(),
    val resultCountLabel: String = "",
    val selectedIndex: Int? = null,
    val lastSummary: String? = null,
) {
    val filterValid: Boolean get() = filter.isNotBlank() && filterError == null
    val pathValid: Boolean get() = path.isNotBlank() && pathError == null
    val canStart: Boolean get() = filterValid && pathValid && !isLoading
}

/**
 * MVVM view-model for the scan screen. Holds the entire UI state in a
 * [StateFlow] and runs scans on [Dispatchers.IO]; [Dispatchers.Main] is used
 * for state mutations so Compose updates happen on the UI thread.
 */
class ScanViewModel(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var lastProgressEmitMs = 0L

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

        val matcher = ComponentItemMatcher(ItemFilterParser.parse(current.filter.trim()).getOrThrow())

        _state.value = _state.value.copy(
            isLoading = true,
            error = null,
            pathError = null,
            filterError = null,
            results = emptyList(),
            resultCountLabel = "",
            progressLabel = "Preparing scan...",
            selectedIndex = null,
            lastSummary = null,
        )

        scanJob?.cancel()
        scanJob = scope.launch {
            try {
                val report = withContext(Dispatchers.IO) {
                    SearchEngine.find(
                        worldRoot = worldRoot,
                        query = ScanQuery(matcher = matcher),
                        onProgress = ::throttledProgress,
                    )
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    results = report.results.map { it.toRow() },
                    resultCountLabel = resultCountLabel(report.results.size),
                    progressLabel = null,
                    lastSummary = buildSummary(report.results.size),
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

    private fun throttledProgress(done: Long, total: Long) {
        val now = System.currentTimeMillis()
        if (now - lastProgressEmitMs < 1000 && done < total) return
        lastProgressEmitMs = now
        val percent = if (total > 0) (done * 100 / total).coerceAtMost(100L) else 0L
        _state.value = _state.value.copy(
            progressLabel = "Scanned $done / $total chunks ($percent%)",
        )
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
        val nf = NumberFormat.getNumberInstance(JavaLocale.US)
        return when (count) {
            0 -> "No matching items found"
            1 -> "1 result"
            else -> "${nf.format(count)} results"
        }
    }

    private fun buildSummary(count: Int): String =
        "Scan finished. ${resultCountLabel(count)}."
}

private fun SearchResult.toRow(): ResultRow {
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
    )
}

private fun org.worldscanner.core.model.DimensionType.displayName(): String = when (this) {
    org.worldscanner.core.model.DimensionType.OVERWORLD -> "Overworld"
    org.worldscanner.core.model.DimensionType.NETHER -> "Nether"
    org.worldscanner.core.model.DimensionType.END -> "End"
    org.worldscanner.core.model.DimensionType.UNKNOWN -> "Unknown"
}
