package com.tennis.server.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tennis.server.model.Jugador
import com.tennis.server.model.Participante
import com.tennis.server.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JugadoresPanel(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val appData by viewModel.appData.collectAsState()
    val participantes = appData.participantes

    // Estado para controlar el BottomSheet
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Dialogo para confirmar la eliminación de un jugador:
    var showConfirmDialog by remember { mutableStateOf(false) }

    var participanteAEliminar by remember { mutableStateOf<Participante?>(null) }

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // Alineación vertical centrada
        ) {
            Text("Jugadores Registrados", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = Color.White)

            // Este Spacer con peso 1f empuja lo que haya después a la derecha
            Spacer(modifier = Modifier.weight(1f))
            // Botón arriba a la derecha
            IconButton(onClick = {
                scope.launch {
                    // Cargamos los jugadores libres antes de mostrar el panel
                    viewModel.cargarJugadoresLibres() // Obtenemos los jugadores libres
                    showSheet = true
                }
            }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Añadir Jugadores",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (participantes.isEmpty()) {
            Text("No hay jugadores registrados.", modifier = Modifier.padding(16.dp), color = Color.White)
        } else {
            LazyColumn {
                itemsIndexed(participantes) { index, participante ->
                    JugadorCard(participante, index + 1, onDelete =
                        { participanteAEliminar = participante
                            showConfirmDialog = true })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Lógica del Panel para añadir nuevos Jugadores (BottomSheet)
    if (showSheet) {
        val jugadoresLibres = viewModel.jugadoresLibres
        ModalBottomSheet(
            // Cuando se pulse fuera del panel se cerrará
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 400.dp) // Limitamos la altura
            ) {
                Text(
                    "Inscribir Jugador",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (jugadoresLibres.isEmpty()) {
                    Text(
                        "Actualmente no hay jugadores disponibles para inscribir.",
                        modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.body1,
                        color = Color.White
                    )
                } else {
                    LazyColumn {
                        items(jugadoresLibres) { jugador ->
                            JugadorLibreRow(
                                jugador = jugador,
                                onAdd = {
                                    viewModel.inscribirJugador(jugador)
                                    showSheet = false // Cerramos al añadir
                                }
                            )
                            Divider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

    }

    // Diálogo de confirmación para finalizar una jornada
    if(showConfirmDialog){
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false; participanteAEliminar = null },
            title = {
                Text(text = "Confirmar eliminación jugador", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Estás seguro de que deseas eliminar al jugador ${participanteAEliminar?.jugador?.nombreCompleto}? " +
                        "Una vez eliminado, no se podrá deshacer la acción.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F)),
                    onClick = {
                        participanteAEliminar?.let {
                            // IMPORTANTE: Asegúrate de que el ViewModel tenga esta función
                            viewModel.removeJugador(it)
                        }
                        showConfirmDialog = false
                        participanteAEliminar = null
                    }
                ) {
                    Text("Confirmar y Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false; participanteAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
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

@Composable
fun JugadorLibreRow(jugador: Jugador, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(jugador.nombreCompleto, fontWeight = FontWeight.Medium, color = Color.White)
            Text(jugador.email, style = MaterialTheme.typography.caption, color = Color.White)
        }
        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4CAF50))
    }
}


