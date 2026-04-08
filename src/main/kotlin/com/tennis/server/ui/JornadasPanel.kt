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
import com.tennis.server.model.Partido
import com.tennis.server.viewmodel.AppViewModel

@Composable
fun JornadasPanel(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val appData by viewModel.appData.collectAsState()
    val jornadas = appData.jornadas
    val partidos = appData.partidos
    val jugadores = appData.jugadores

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Jornadas", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.generarJornada() }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Generar")
                Spacer(Modifier.width(4.dp))
                Text("Generar Emparejamientos (Demo)")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (jornadas.isEmpty()) {
            Text("No hay jornadas creadas.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(jornadas.reversed()) { jornada ->
                    val partidosJornada = partidos.filter { it.idJornada == jornada.id }
                    JornadaExpandableCard(jornada, partidosJornada, jugadores)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun JornadaExpandableCard(jornada: Jornada, partidos: List<Partido>, jugadores: List<com.tennis.server.model.Jugador>) {
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
                    Text(jornada.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.subtitle1)
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
                        Text("No hay partidos en esta jornada (Modo Demo: Motor devolvió lista vacía)", color = MaterialTheme.colors.secondary)
                    } else {
                        partidos.forEach { partido ->
                            val p1 = jugadores.find { it.id == partido.idJugador1 }?.nombreCompleto ?: "Jugador Extraño"
                            val p2 = jugadores.find { it.id == partido.idJugador2 }?.nombreCompleto ?: "Jugador Extraño"
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$p1 vs $p2")
                                Text("Estado: ${partido.estado}")
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
