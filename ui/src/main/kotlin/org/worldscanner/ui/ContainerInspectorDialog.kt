package org.worldscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.worldscanner.core.model.ItemSource
import org.worldscanner.core.model.SearchResult
import org.worldscanner.ui.theme.MinecraftColors
import org.worldscanner.ui.theme.MonoFontFamily
import androidx.compose.ui.window.Dialog

/**
 * Modal that lists every item inside the container that produced a match,
 * including nested container contents (shulker boxes, bundles, ...). Aggregated
 * by item id so a 27-slot chest becomes a compact inventory readout.
 */
@Composable
fun ContainerInspectorDialog(result: SearchResult?, onDismiss: () -> Unit) {
    if (result == null) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MinecraftColors.SurfaceDark,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MinecraftColors.EnchantCyan,
                    )
                    Column {
                        Text(
                            "Container contents",
                            style = MaterialTheme.typography.titleMedium,
                            color = MinecraftColors.TextPrimary,
                        )
                        Text(
                            result.inspectorLocation(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MinecraftColors.TextSecondary,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                if (result.containerContents.isEmpty()) {
                    Text(
                        "This source has no decodable inventory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MinecraftColors.TextSecondary,
                    )
                } else {
                    val aggregated = result.containerContents
                        .groupBy { it.normalizedId }
                        .map { (id, stacks) -> id to stacks.sumOf { it.count } }
                        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MinecraftColors.SurfaceVariant),
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(aggregated, key = { it.first }) { (id, count) ->
                                Row(
                                    Modifier.fillMaxWidth().background(MinecraftColors.SurfaceVariant),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        id,
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = MonoFontFamily,
                                        color = MinecraftColors.TextPrimary,
                                    )
                                    Text(
                                        "× $count",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        color = MinecraftColors.ExperienceGold,
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "${result.containerContents.size} stack(s) total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MinecraftColors.TextSecondary,
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

private fun SearchResult.inspectorLocation(): String {
    val source = this.source
    val pos = (source as? ItemSource.BlockEntity)?.pos
        ?: (source as? ItemSource.Entity)?.pos
    val holder = when (source) {
        is ItemSource.BlockEntity -> source.id
        is ItemSource.Entity -> source.id
    }
    val coords = pos?.let { "(${it.x}, ${it.y}, ${it.z})" } ?: "unknown position"
    return "$holder at $coords · ${dimension.displayName()}"
}
