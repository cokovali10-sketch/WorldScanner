@file:OptIn(ExperimentalMaterial3Api::class)

package org.worldscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.worldscanner.ui.theme.MinecraftColors
import org.worldscanner.ui.theme.MonoFontFamily
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JFileChooser

/** Root composable for the scan window. */
@Composable
fun ScanApp(viewModel: ScanViewModel = remember { ScanViewModel() }) {
    val state by viewModel.state.collectAsState()

    WorldScannerTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "WorldScanner",
                                tint = MinecraftColors.GrassGreen,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("WorldScanner")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MinecraftColors.SurfaceDark,
                        titleContentColor = MinecraftColors.TextPrimary,
                    ),
                )
            },
            containerColor = MinecraftColors.BackgroundDeep,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WorldPathSection(state, onPathChange = viewModel::setPath, onBrowse = viewModel::setPath)

                FilterInputSection(state, onFilterChange = viewModel::setFilter)

                ScanControls(
                    state = state,
                    onStart = viewModel::startScan,
                    onCancel = viewModel::cancelScan,
                )

                state.error?.let { error ->
                    ErrorBanner(error)
                }

                if (state.isLoading) {
                    LoadingBar(state.progressLabel)
                }

                ResultsTable(
                    state = state,
                    onSelectRow = viewModel::selectRow,
                )
            }
        }
    }
}

@Composable
private fun WorldPathSection(
    state: UiState,
    onPathChange: (String) -> Unit,
    onBrowse: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinecraftColors.SurfaceDark),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "World folder",
                style = MaterialTheme.typography.titleMedium,
                color = MinecraftColors.TextPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.path,
                    onValueChange = onPathChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Path to a Minecraft save (level.dat folder)") },
                    singleLine = true,
                    isError = state.pathError != null,
                    supportingText = {
                        state.pathError?.let { Text(it) }
                    },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                )
                Button(onClick = { browseForDirectory()?.let(onPathChange) }) {
                    Text("Browse…")
                }
            }
        }
    }
}

@Composable
private fun FilterInputSection(state: UiState, onFilterChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinecraftColors.SurfaceDark),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Item filter",
                style = MaterialTheme.typography.titleMedium,
                color = MinecraftColors.TextPrimary,
            )
            OutlinedTextField(
                value = state.filter,
                onValueChange = onFilterChange,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("e.g. diamond_sword[enchantments={mending:1,sharpness:5}]")
                },
                singleLine = true,
                isError = state.filterError != null,
                supportingText = {
                    state.filterError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                },
            )
            Text(
                "SNBT /give-style filter: item id with optional [components].",
                style = MaterialTheme.typography.bodySmall,
                color = MinecraftColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ScanControls(state: UiState, onStart: () -> Unit, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isLoading) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Button(
                onClick = onStart,
                enabled = state.canStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinecraftColors.GrassGreen,
                    contentColor = Color(0xFF0C1407),
                ),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Start scan")
            }
        }

        state.lastSummary?.let { summary ->
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MinecraftColors.GrassGreen,
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun LoadingBar(label: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        label?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MinecraftColors.TextSecondary)
        }
    }
}

@Composable
private fun ResultsTable(state: UiState, onSelectRow: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinecraftColors.SurfaceDark),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Results",
                    style = MaterialTheme.typography.titleMedium,
                    color = MinecraftColors.TextPrimary,
                )
                if (state.resultCountLabel.isNotBlank()) {
                    Text(
                        state.resultCountLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MinecraftColors.TextSecondary,
                    )
                }
            }

            if (state.results.isEmpty() && !state.isLoading) {
                Text(
                    "No results yet — pick a world and run a scan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MinecraftColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                return@Column
            }

            TableHeaderRow()
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
                itemsIndexed(state.results) { index, row ->
                    ResultRowView(
                        row = row,
                        selected = state.selectedIndex == index,
                        onClick = { onSelectRow(index) },
                    )
                }
            }
        }
    }
}

private val ColumnWeights = listOf(1.1f, 0.7f, 1.2f, 1.0f, 1.4f, 1.2f, 0.5f)

@Composable
private fun TableHeaderRow() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Dimension", "Region", "Coordinates", "Chunk", "Container", "Item", "").forEachIndexed { i, title ->
            Text(
                title,
                modifier = Modifier.weight(ColumnWeights[i]),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MinecraftColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ResultRowView(row: ResultRow, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MinecraftColors.SurfaceVariant else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .height(36.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.dimension, Modifier.weight(ColumnWeights[0]), style = MaterialTheme.typography.bodySmall)
        Text(row.region, Modifier.weight(ColumnWeights[1]), style = MaterialTheme.typography.bodySmall)
        Text(
            row.coords,
            Modifier.weight(ColumnWeights[2]),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = MonoFontFamily,
            color = MinecraftColors.EnchantCyan,
        )
        Text(row.chunk, Modifier.weight(ColumnWeights[3]), style = MaterialTheme.typography.bodySmall)
        Text(
            row.container,
            Modifier.weight(ColumnWeights[4]),
            style = MaterialTheme.typography.bodySmall,
            color = MinecraftColors.TextSecondary,
            maxLines = 1,
        )
        Text(row.item, Modifier.weight(ColumnWeights[5]), style = MaterialTheme.typography.bodySmall)
        IconButton(
            onClick = {
                copyToClipboard(row.coords)
                onClick()
            },
            modifier = Modifier.weight(ColumnWeights[6]),
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy coordinates",
                tint = MinecraftColors.GrassGreen,
            )
        }
    }
}

/** Opens a native folder picker. Returns the selected path, or null when cancelled. */
private fun browseForDirectory(): String? {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    chooser.dialogTitle = "Select a Minecraft world folder"
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath
    } else {
        null
    }
}

/** Copies [text] to the system clipboard. */
private fun copyToClipboard(text: String) {
    val selection = StringSelection(text)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
}

@Composable
private fun WorldScannerTheme(content: @Composable () -> Unit) {
    org.worldscanner.ui.theme.WorldScannerTheme(content = content)
}
