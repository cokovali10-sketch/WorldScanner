package org.worldscanner.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "WorldScanner",
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        val viewModel = remember { ScanViewModel() }
        ScanApp(viewModel = viewModel)
    }
}
