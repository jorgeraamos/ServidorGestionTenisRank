package com.tennis.server.viewmodel

import com.tennis.server.data.DataRepository
import com.tennis.server.data.actualizarHistorialRivales
import com.tennis.server.data.deleteJornada
import com.tennis.server.data.getAllJornadas
import com.tennis.server.data.getAllParticipantes
import com.tennis.server.data.getAllRankings
import com.tennis.server.data.getUltimaJornada
import com.tennis.server.data.insertJornada
import com.tennis.server.data.insertPartidos
import com.tennis.server.data.insertSets
import com.tennis.server.data.updateJornada
import com.tennis.server.engine.MatchEngine
import com.tennis.server.engine.MatchEngine.generarEmparejamientos
import com.tennis.server.engine.RankingEngine.actualizarPuntuaciones
import com.tennis.server.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * AppViewModel funciona como el "Cerebro" de la aplicación.
 * Conecta la interfaz gráfica (UI) con la base de datos (DataRepository) y la lógica (MatchEngine).
 * Almacena los estados de forma reactiva (StateFlow) para que la pantalla se actualice sola si cambian.
 */
class AppViewModel {
    // Definimos el entorno en el que corren los procesos (hilo principal de UI)
    private val scope = CoroutineScope(Dispatchers.Main)
    
    // Instancia del repositorio para leer/guardar JSON
    private val repository = DataRepository()
    
    // --- ESTADOS REACTIVOS (UI State) ---
    // appData contiene la lista de jugadores, jornadas, partidos y la configuración.
    private val _appData = MutableStateFlow(AppData())
    val appData: StateFlow<AppData> = _appData.asStateFlow()

    // logs almacena las líneas de texto para la pestaña "Consola de Logs"
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()


    /**
     * Bloque INIT: Se ejecuta en cuanto la aplicación arranca.
     */
    init {
        log("Iniciando Servidor de Tenis...")
        scope.launch {
            // Espera a que la base de datos se cargue ("await")
//            val data = repository.load()
//            _appData.value = data // Actualiza la UI
//            log("Datos cargados correctamente")

            _appData.value = AppData() // Carga los valores por defecto
            log("Aplicación iniciada correctamente")

            // Cargamos los nombres de los rankings para el selector
            cargarRankings()
        }
    }

    /**
     * Escribe un nuevo mensaje en la consola de Logs
     */
    fun log(message: String) {
        val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        _logs.value = _logs.value + "[$timestamp] $message"
    }

    // --- ACCIONES DE CONFIGURACIÓN ---
    fun updateConfig(config: AppConfig) {
        val current = _appData.value // Copia inmutable del estado actual
        _appData.value = current.copy(config = config) // Reemplaza solo "config"
        saveData() // Invoca la persistencia en disco
        log("Configuración actualizada: ${config.selectedRanking}")

    }

    // Cada vez que se selecciona una edición se actualizan los datos:
    fun setEdicionActiva(edicion: Edicion?) {
        scope.launch {
            if (edicion == null) {
                // Si no hay edición, limpiamos los datos dependientes
                _appData.value = _appData.value.copy(
                    config = _appData.value.config.copy(selectedEdicion = null, ultimaJornada = null),
                    jornadas = emptyList(),
                    partidos = emptyList(),
                    participantes = emptyList() // O los participantes de esa edición
                )
                return@launch
            }

            try {
                log("Cargando datos de la edición: ${edicion.nombre}")

                // Llamamos a las funciones de supabase:
                val (jornadas, partidos) = getAllJornadas(edicion.id)
                val participantes = getAllParticipantes(edicion.id)
                val ultima = jornadas.maxByOrNull { it.numero }

                //Actualizamos los datos del AppData
                _appData.value = _appData.value.copy(
                    config = _appData.value.config.copy(
                        selectedEdicion = edicion,
                        ultimaJornada = ultima
                    ),
                    jornadas = jornadas,
                    partidos = partidos,
                    participantes = participantes
                )
                log("Datos de ${edicion.nombre} listos.")
                if(participantes.isEmpty()){
                    log("No hay participantes")
                }
            } catch (e: Exception) {
                log("Error al cambiar de edición: ${e.message}")
            }
        }
    }

    private val _rankingsDisponibles = MutableStateFlow<List<Ranking>>(emptyList())
    val rankingsDisponibles: StateFlow<List<Ranking>> = _rankingsDisponibles

    // Función para cargar los datos
    fun cargarRankings() {
        scope.launch {
            try {
                val rankings = getAllRankings()
                // Extraemos solo el nombre de cada objeto Ranking
                _rankingsDisponibles.value = rankings
                log("Rankings cargados: ${rankings.size} encontrados")
            } catch (e: Exception) {
                log("Error al cargar rankings: ${e.message}")
            }
        }
    }


    fun generarYGuardarNuevaJornada() {
        val currentData = _appData.value
        val config = currentData.config

        // Validamos primero que la edicion esté seleccionada
        val edicion = config.selectedEdicion ?: run {
            log("ERROR: No hay edición seleccionada")
            return
        }

        // Validamos también que la fecha esté seleccionada
        // (no hace falta por otro lado validar el intervalo de la jornada ya que nunca será null)
        if (config.fechaInicio.isBlank()) {
            log("ERROR: La fecha de inicio no puede estar vacía")
            return
        }

        scope.launch {
            try {
                log("Iniciando proceso para la Edición: ${edicion.nombre}")

                // Obtenenemos los participantes de la base de datos para la edicion seleccionada
                val participantes = currentData.participantes

                // SE PODRÍA PONER UNA RESTRICCIÓN DEL NÚMERO DE PARTICIPANTES!!!
                // Comprobamos que haya participantes en dicha edición
                if (participantes.isEmpty()) {
                    log("AVISO: No hay participantes inscritos en esta edición.")
                    return@launch
                }

                log("Participantes cargados")

                // Creamos el objeto Jornada usando los datos de Config
                val numeroJornada = (config.ultimaJornada?.numero ?: 0) + 1
                val fechaInicioParsed = LocalDate.parse(config.fechaInicio)
                val fechaFinParsed = fechaInicioParsed.plusDays(config.intervaloJornadaDias.toLong())

                val nuevaJornada = Jornada(
                    id = 0,
                    idEdicion = edicion.id,
                    numero = numeroJornada,
                    fechaInicio = fechaInicioParsed.toString(),
                    fechaFin = fechaFinParsed.toString(),
                    estado = "pendiente"
                )

                // Insertamos la nueva jornada en Supabase
                // Recogemos la jornadaInsertada que contiene el id real
                val jornadaInsertada = insertJornada(nuevaJornada) { mensaje -> log(mensaje)}

                log("Jornada introducida correctamente ")

                // Ejecutamos el algoritmo de emparejamiento
                log("Calculando emparejamientos óptimos...")
                val partidos = generarEmparejamientos(participantes, jornadaInsertada)

                // Insertamos los partidos en Supabase

                insertPartidos(partidos, edicion.id) {mensaje -> log(mensaje)}


                insertSets(partidos) {mensaje -> log(mensaje)}

                // Actualizamos el estado local para que la UI se refresque sola
                val nuevaConfig = config.copy(ultimaJornada = jornadaInsertada)

                _appData.value = currentData.copy(
                    config = nuevaConfig,
                    jornadas = currentData.jornadas + jornadaInsertada,
                    partidos = currentData.partidos + partidos
                )

                log("ÉXITO: Jornada $numeroJornada generada con ${partidos.size} partidos.")


            } catch (e: Exception) {
                log("Error crítico al generar la nueva Jornada: ${e.message}")
                println("Stacktrace: ${e.printStackTrace()}")
            }
        }
    }

    //Función para cancelar la última jornada
    fun cancelarJornada(jornada: Jornada) {
        scope.launch {
            try {
                // Llamamos a la función deleteJornada de supabase
                deleteJornada(jornada.id) { mensaje -> log(mensaje) }

                // Actualizamos el estado local (AppData) para tener la UI actualizada
                val currentData = _appData.value

                // Filtramos la lista de jornadas para quitar la que acabamos de borrar
                val jornadasActualizadas = currentData.jornadas.filter { it.id != jornada.id }

                // Buscamos cuál es ahora la "nueva" última jornada de esa edición
                val nuevaUltima = jornadasActualizadas
                    .filter { it.idEdicion == jornada.idEdicion }
                    .maxByOrNull { it.numero }

                // Emitimos el nuevo estado
                _appData.value = currentData.copy(
                    jornadas = jornadasActualizadas,
                    config = currentData.config.copy(ultimaJornada = nuevaUltima)
                )

                log("Jornada ${jornada.numero} cancelada con éxito.")

            } catch (e: Exception) {
                log("Error al procesar la cancelación de la jornada: ${e.message}")
            }
        }
    }

    // Función para finalizar la jornada
    fun finalizarJornada(jornada : Jornada){
        scope.launch {
            try {
                val currentData = _appData.value

                //Obtenemos los partidos que pertenecen a la jornada
                val partidosDeLaJornada = currentData.partidos.filter { it.idJornada == jornada.id }

                val edicion = currentData.config.selectedEdicion ?: return@launch log("Error: No hay edición seleccionada")

                // Obtenemos los participantes
                val participantes = currentData.participantes
                // Actualizamos las puntuaciones de la jornada:
                actualizarPuntuaciones(edicion!!.id, partidosDeLaJornada, participantes)

                log("Se han actualizado las puntuaciones correctamente")

                // Llamamos a la función updateJornada de supabase
                updateJornada(jornada) { mensaje -> log(mensaje) }

                // Buscamos cuál es ahora la "nueva" última jornada de esa edición
                val nuevaUltima = getUltimaJornada(currentData.config.selectedEdicion!!.id)

                // Cogemos de nuevo los participantes al haber actualizado sus puntuaciones
                val participantesActualizados = getAllParticipantes(edicion.id)

                // Emitimos el nuevo estado
                _appData.value = currentData.copy(
                    config = currentData.config.copy(ultimaJornada = nuevaUltima),
                    participantes = participantesActualizados
                )

                log("Jornada ${jornada.numero} finalizada con éxito.")

            } catch (e: Exception) {
                log("Error al finalizar la jornada: ${e.message}")
            }
        }
    }

    // --- ACCIONES SOBRE JUGADORES ---
    
    fun removeJugador(id: String) {
        val current = _appData.value
        val name = current.participantes.find { it.id == id }?.jugador?.nombreCompleto ?: id
        // Filtra para dejar todos excepto el que queremos borrar
        _appData.value = current.copy(participantes = current.participantes.filter { it.id != id })
        saveData()
        log("Jugador eliminado: $name")
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    // Función auxiliar para enviar datos a DataRepository en segundo plano
    private fun saveData() {
        scope.launch {
            repository.save(_appData.value)
        }
    }
}
