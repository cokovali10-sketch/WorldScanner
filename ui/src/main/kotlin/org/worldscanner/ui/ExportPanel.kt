package org.worldscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.worldscanner.ui.theme.MinecraftColors
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFileChooser

/** CSV / JSON export controls with a save-file dialog and status readout. */
@Composable
fun ExportPanel(
    status: String?,
    onExportCsv: (Path) -> Unit,
    onExportJson: (Path) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinecraftColors.SurfaceDark),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Export results",
                style = MaterialTheme.typography.titleMedium,
                color = MinecraftColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { saveFile("results.csv", ".csv")?.let(onExportCsv) }) {
                Text("CSV")
            }
            Button(onClick = { saveFile("results.json", ".json")?.let(onExportJson) }) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("JSON")
            }
        }
        status?.let { text ->
            Text(
                text,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MinecraftColors.EnchantCyan,
            )
        }
    }
}

/** Opens a save dialog; returns the chosen path or null when cancelled. */
private fun saveFile(defaultName: String, extension: String): Path? {
    val chooser = JFileChooser()
    chooser.dialogTitle = "Export scan results"
    chooser.selectedFile = java.io.File(defaultName)
    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile ?: return null
        val name = if (file.name.endsWith(extension, ignoreCase = true)) file.name else file.name + extension
        Paths.get(file.parentFile.absolutePath, name)
    } else {
        null
    }
}
