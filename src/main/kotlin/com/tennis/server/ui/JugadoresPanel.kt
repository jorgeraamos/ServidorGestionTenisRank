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
import com.tennis.server.viewmodel.AppViewModel

@Composable
fun JugadoresPanel(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val appData by viewModel.appData.collectAsState()
    val jugadores = appData.jugadores
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Jugadores Registrados", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
                Spacer(Modifier.width(4.dp))
                Text("Añadir Jugador")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (jugadores.isEmpty()) {
            Text("No hay jugadores registrados.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn {
                itemsIndexed(jugadores) { index, jugador ->
                    JugadorCard(jugador, index + 1, onDelete = { viewModel.removeJugador(jugador.id) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddJugadorModal(
            onDismiss = { showAddDialog = false },
            onSave = { jugador ->
                viewModel.addJugador(jugador)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun JugadorCard(jugador: Jugador, index: Int, onDelete: () -> Unit) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("#$index - ${jugador.nombreCompleto}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Puntos: ${jugador.puntosActuales} | Nacionalidad: ${jugador.nacionalidad.ifEmpty { "N/A" }} | Mano: ${jugador.manoDominante.ifEmpty { "N/A" }}", style = MaterialTheme.typography.body2)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colors.error)
            }
        }
    }
}

@Composable
fun AddJugadorModal(onDismiss: () -> Unit, onSave: (Jugador) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var nacionalidad by remember { mutableStateOf("") }
    var manoDominante by remember { mutableStateOf("") }
    var estilo by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(400.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Nuevo Jugador", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nacionalidad, onValueChange = { nacionalidad = it }, label = { Text("Nacionalidad") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = manoDominante, onValueChange = { manoDominante = it }, label = { Text("Mano Dominante (Diestro/Zurdo)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = estilo, onValueChange = { estilo = it }, label = { Text("Estilo de Juego") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (nombre.isNotBlank()) {
                            onSave(Jugador(
                                nombreCompleto = nombre,
                                nacionalidad = nacionalidad,
                                manoDominante = manoDominante,
                                estiloJuego = estilo
                            ))
                        }
                    }) { Text("Guardar") }
                }
            }
        }
    }
}
