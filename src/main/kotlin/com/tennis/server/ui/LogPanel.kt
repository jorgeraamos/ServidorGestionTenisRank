package com.tennis.server.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tennis.server.viewmodel.AppViewModel

@Composable
fun LogPanel(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logs del Servidor", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = Color.White)
            Button(onClick = { viewModel.clearLogs() }) {
                Text("Limpiar Consola")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            if (logs.isEmpty()) {
                Text("No hay logs disponibles.", color = Color.Gray)
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(logs) { logMsg ->
                        val isError = logMsg.contains("Error", ignoreCase = true) || logMsg.contains("AVISO", ignoreCase = true)
                        Text(
                            text = logMsg,
                            color = if (isError) Color(0xFFFF5252) else Color(0xFFE0E0E0),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
