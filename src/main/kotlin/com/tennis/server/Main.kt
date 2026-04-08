package com.tennis.server

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.tennis.server.ui.MainScreen
import com.tennis.server.ui.theme.TennisServerTheme
import com.tennis.server.viewmodel.AppViewModel

fun main() = application {
    val viewModel = AppViewModel()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Servidor Gestión de Tenis v1.0",
        state = WindowState(size = DpSize(1200.dp, 800.dp))
    ) {
        TennisServerTheme {
            MainScreen(viewModel)
        }
    }
}
