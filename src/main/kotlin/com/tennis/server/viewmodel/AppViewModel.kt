package com.tennis.server.viewmodel

import com.tennis.server.data.DataRepository
import com.tennis.server.data.getAllRankings
import com.tennis.server.engine.MatchEngine
import com.tennis.server.model.*
import com.tennis.server.scheduler.JornadaScheduler
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

    // schedulerRunning indica si el motor de autogeneración está encendido.
    private val _schedulerRunning = MutableStateFlow(false)
    val schedulerRunning: StateFlow<Boolean> = _schedulerRunning.asStateFlow()

    // --- TEMPORIZADOR DE JORNADAS AUTOMÁTICAS ---
    private val scheduler = JornadaScheduler(CoroutineScope(Dispatchers.Default)) {
        // [Callback]: Esto es lo que el scheduler ejecuta cuando toca generar la siguiente ronda.
        scope.launch {
            log("Scheduler: Generando jornada automática...")
            generarJornada() // Llama a la lógica principal
        }
    }

    /**
     * Bloque INIT: Se ejecuta en cuanto la aplicación arranca.
     */
    init {
        log("Iniciando Servidor de Tenis...")
        scope.launch {
            // Espera a que la base de datos se cargue ("await")
            val data = repository.load()
            _appData.value = data // Actualiza la UI
            log("Datos cargados correctamente")

            // Cargamos los nombres de los rankings para el selector
            cargarRankings()
            
            // Si estaba activada la generación automática, encendemos el temporizador.
            if(data.config.autoGenerarJornadas) {
                iniciarScheduler()
            }
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

        // Reevalúa el hilo automático según vengan los nuevos ajustes
        if (config.autoGenerarJornadas) {
            iniciarScheduler()
        } else {
            detenerScheduler()
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

    // --- ACCIONES SOBRE JUGADORES ---
    fun addJugador(jugador: Jugador) {
        val current = _appData.value
        _appData.value = current.copy(jugadores = current.jugadores + jugador)
        saveData()
        log("Jugador añadido: ${jugador.nombreCompleto}")
    }
    
    fun removeJugador(id: String) {
        val current = _appData.value
        val name = current.jugadores.find { it.id == id }?.nombreCompleto ?: id
        // Filtra para dejar todos excepto el que queremos borrar
        _appData.value = current.copy(jugadores = current.jugadores.filter { it.id != id })
        saveData()
        log("Jugador eliminado: $name")
    }

    // --- ACCIONES DE JORNADAS Y PARTIDOS ---
    /**
     * Fuerza o genera automáticamente la próxima lista de partidos según la fecha.
     */
    fun generarJornada() {
        val current = _appData.value
        val number = current.jornadas.size + 1 // Determina si es la Jornada 1, 2, 3...
        
        // Define el objeto de la nueva jornada
        val newJornada = Jornada(
            id = number,
            nombre = "Jornada $number",
            fechaInicio = LocalDate.now().toString(),
            // La fecha final es "hoy + N días de intervalo"
            fechaFin = LocalDate.now().plusDays(current.config.intervaloJornadaDias.toLong()).toString(),
            estado = "en_curso"
        )
        
        log("Generando emparejamientos para ${newJornada.nombre} (Modo Demo)")
        
        // --- LLAMADA AL ALGORITMO DE EMPAREJAMIENTO ---
        val partidos = MatchEngine.generarEmparejamientos(current.jugadores, newJornada)
        
        if (partidos.isEmpty() && current.jugadores.isNotEmpty()) {
            log("AVISO DEMO: MatchEngine devolvió lista vacía de partidos. Falta implementar algoritmo.")
        }

        // Incorpora la jornada y sus nuevos partidos al estado global combinado
        _appData.value = current.copy(
            jornadas = current.jornadas + newJornada,
            partidos = current.partidos + partidos
        )
        saveData() // Se persiste el cambio completo (jornada + partidos) en el JSON
        log("Jornada $number creada exitosamente")
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    // --- CONTROL INTERNO DEL SCHEDULER ---
    private fun iniciarScheduler() {
        val config = _appData.value.config
        val lastRound = _appData.value.jornadas.lastOrNull()?.fechaInicio
        scheduler.start(config, lastRound)
        _schedulerRunning.value = scheduler.isRunning
        if(scheduler.isRunning) log("Scheduler automático INICIADO")
    }

    private fun detenerScheduler() {
        scheduler.stop()
        _schedulerRunning.value = scheduler.isRunning
        log("Scheduler automático DETENIDO")
    }

    // Función auxiliar para enviar datos a DataRepository en segundo plano
    private fun saveData() {
        scope.launch {
            repository.save(_appData.value)
        }
    }
}
