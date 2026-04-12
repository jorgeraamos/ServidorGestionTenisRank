package com.tennis.server.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tennis.server.model.Jugador
import com.tennis.server.model.Participante
import com.tennis.server.viewmodel.AppViewModel

@Composable
fun JugadoresPanel(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val appData by viewModel.appData.collectAsState()
    val participantes = appData.participantes

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Text("Jugadores Registrados", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        if (participantes.isEmpty()) {
            Text("No hay jugadores registrados.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn {
                itemsIndexed(participantes) { index, participante ->
                    JugadorCard(participante, index + 1, onDelete = { viewModel.removeJugador(participante.id) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

}

@Composable
fun JugadorCard(participante: Participante, index: Int, onDelete: () -> Unit) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("#$index - ${participante.jugador.nombreCompleto}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Puntos: ${participante.puntos} ", style = MaterialTheme.typography.body2)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colors.error)
            }
        }
    }
}


