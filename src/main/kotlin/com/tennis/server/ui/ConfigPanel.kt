package com.tennis.server.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import com.tennis.server.data.getAllEdiciones
import com.tennis.server.data.getAllRankings
import java.time.Instant
import java.time.ZoneId
import com.tennis.server.model.AppConfig
import com.tennis.server.model.Edicion
import com.tennis.server.viewmodel.AppViewModel

/**
 * Panel de Configuración de la aplicación.
 * Permite definir el nombre del torneo, el intervalo de días entre jornadas,
 * la fecha de inicio temporal y activar/desactivar la auto-generación de jornadas.
 *
 * @param viewModel ViewModel principal con la lógica de negocio.
 * @param modifier Modificador opcional para la vista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPanel(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    // Escucha el estado global de la app (config y jornadas)
    val appData by viewModel.appData.collectAsState()
    val config = appData.config

    val opcionesRankings by viewModel.rankingsDisponibles.collectAsState()

    // Estado para las ediciones que se cargarán dinámicamente
    var opcionesEdicion by remember { mutableStateOf<List<Edicion>>(emptyList()) }

    // Definimos las variables:
    // Clonamos la configuración actual en estados locales para editarlos antes de guardar
    var selectedRanking by remember { mutableStateOf(config.selectedRanking) }
    var selectedEdicion by remember {mutableStateOf(config.selectedEdicion)}
    var intervalo by remember { mutableStateOf(config.intervaloJornadaDias.toFloat()) }
    var fechaInicio by remember { mutableStateOf(config.fechaInicio) }
    var autoGenerar by remember { mutableStateOf(config.autoGenerarJornadas) }
    
    // Estado para controlar cuándo mostrar y ocultar el selector de calendario
    var showDatePicker by remember { mutableStateOf(false) }
    // Estado interno del componente DatePicker (para leer qué día ha seleccionado el usuario)
    val datePickerState = rememberDatePickerState()

    // Cada vez que se seleccione un ranking, actualizaremos las ediciones que se puedan elegir en relación a dicho ranking
    LaunchedEffect(selectedRanking) {
        selectedRanking?.let { ranking ->
            // Cargamos las ediciones de la base de datos usando el ID del ranking
            opcionesEdicion = getAllEdiciones(ranking.id)

            //  Si el ranking cambia, reseteamos la edición seleccionada para que no quede una edición del ranking anterior.
            if (selectedEdicion?.idRanking != ranking.id) {
                selectedEdicion = null
            }
        }
    }

    // Layout principal en columna
    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Text("Configuración del Servidor", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta envolvente para los campos
        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // Añadimos espacio entre los selectores
                ) {
                    // Selector para el Nombre del Ranking
                    Box(modifier = Modifier.weight(1f)) { // Usamos box para repartir el espacio
                        SelectorOpcionesGenerico(
                            label = "Ranking",
                            opciones = opcionesRankings,
                            itemTexto = { it.nombre },
                            seleccionado = selectedRanking,
                            onOptionSelected = { selectedRanking = it }
                        )
                    }

                    // Selector para la Edicion del Ranking
                    Box(modifier = Modifier.weight(1f)) {
                        SelectorOpcionesGenerico(
                            label = "Edición del Ranking",
                            opciones = opcionesEdicion,
                            itemTexto = { it.nombre },
                            seleccionado = selectedEdicion,
                            onOptionSelected = { selectedEdicion = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Campo de Texto para la Fecha de Inicio
                OutlinedTextField(
                    value = fechaInicio,
                    onValueChange = { fechaInicio = it },
                    label = { Text("Fecha de Inicio (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    // Icono anclado al final del campo de texto
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                        }
                    }
                )
                
                // --- DIÁLOGO DEL CALENDARIO (Solo se muesta si showDatePicker es true) ---
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false }, // Si clicas fuera, se oculta
                        confirmButton = {
                            Button(onClick = {
                                // Convertimos los milisegundos seleccionados a una fecha legible YYYY-MM-DD
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val localDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.of("UTC"))
                                        .toLocalDate()
                                    fechaInicio = localDate.toString() // Guardamos la fecha convertida
                                }
                                showDatePicker = false // Cerramos el calendario
                            }) {
                                Text("Aceptar")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDatePicker = false }) {
                                Text("Cancelar")
                            }
                        }
                    ) {
                        // El calendario dibujado en pantalla (Material 3)
                        DatePicker(state = datePickerState)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector deslizante (Slider) para los días de intervalo
                Text("Intervalo entre jornadas: ${intervalo.toInt()} días")
                Slider(
                    value = intervalo,
                    onValueChange = { intervalo = it },
                    valueRange = 1f..30f,    // Entre 1 y 30 días
                    steps = 29               // Cantidad de paradas del slider
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interruptor para Generación Automática
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = autoGenerar, onCheckedChange = { autoGenerar = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generar jornadas automáticamente")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Final de Guardado
                Button(
                    onClick = {
                        // Le mandamos al ViewModel el nuevo objeto de configuración
                        // para que se sobreescriba y se guarde de forma permanente.
                        viewModel.updateConfig(
                            AppConfig(
                                selectedRanking = selectedRanking,
                                intervaloJornadaDias = intervalo.toInt(),
                                fechaInicio = fechaInicio,
                                autoGenerarJornadas = autoGenerar
                            )
                        )
                    },
                    modifier = Modifier.align(Alignment.End) // Alineado a la derecha
                ) {
                    Text("Guardar Configuración")
                }
            }
        }
    }
}
