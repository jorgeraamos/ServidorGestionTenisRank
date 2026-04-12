package com.tennis.server.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tennis.server.model.Jornada
import com.tennis.server.model.Participante
import com.tennis.server.model.Partido
import com.tennis.server.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun JornadasPanel(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val appData by viewModel.appData.collectAsState()
    val jornadas = appData.jornadas
    val partidos = appData.partidos
    val participantes = appData.participantes
    val config = appData.config

    val scope = rememberCoroutineScope() // Necesario para lanzar el Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Buscamos la jornada que se puede finalizar:
    // Es la última jornada de la edición actual que NO esté ya finalizada.
    val jornadaAFinalizar = jornadas
        .filter { it.idEdicion == config.selectedEdicion?.id }
        .maxByOrNull { it.numero }
        .takeIf { it?.estado != "finalizada" }

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Jornadas", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    if(jornadaAFinalizar != null) {
                        jornadaAFinalizar?.let { viewModel.finalizarJornada(it) }
                    }else{
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Error: No hay ninguna jornada para finalizar",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
            }
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Generar")
                Spacer(Modifier.width(4.dp))
                    Text("Finalizar Jornada ${jornadaAFinalizar?.numero ?: ""}")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Se mostrarán todas las jornadas que se han creado de dicha edición y sus partidos:
        if (jornadas.isEmpty()) {
            Text("No hay jornadas creadas.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(jornadas.reversed()) { jornada ->
                    val partidosJornada = partidos.filter { it.idJornada == jornada.id }
                    JornadaExpandableCard(jornada, partidosJornada, participantes)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        // Zona en la que saldrá el mensaje en caso de error:
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@Composable
fun JornadaExpandableCard(jornada: Jornada, partidos: List<Partido>, participantes: List<Participante>) {
    var expanded by remember { mutableStateOf(false) }

    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Jornada ${jornada.numero}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.subtitle1)
                    Text("${jornada.fechaInicio} - ${jornada.fechaFin} | Estado: ${jornada.estado}", style = MaterialTheme.typography.body2, color = Color.Gray)
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand"
                )
            }

            if (expanded) {
                Divider()
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    if (partidos.isEmpty()) {
                        Text("No hay partidos en esta jornada ", color = MaterialTheme.colors.secondary)
                    } else {
                        partidos.forEach { partido ->

                            val p1 = participantes.find { it.id == partido.idJugador1 }?.jugador?.nombreCompleto ?: "DESCANSO"
                            val p2 = participantes.find { it.id == partido.idJugador2 }?.jugador?.nombreCompleto ?: "DESCANSO"
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if(p1 == "DESCANSO"){
                                    Text("Descanso para el jugador $p2")
                                }else if(p2 == "DESCANSO"){
                                    Text("Descanso para el jugador $p1")
                                }else {
                                    Text("$p1 vs $p2")
                                    Text("Estado: ${partido.estado}")
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
