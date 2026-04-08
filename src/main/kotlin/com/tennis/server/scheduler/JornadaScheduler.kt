package com.tennis.server.scheduler

import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.tennis.server.model.AppConfig

/**
 * Programador (Scheduler) que se ejecuta en segundo plano.
 * Su objetivo es comprobar periódicamente si ya ha pasado el tiempo estipulado
 * en la configuración y es necesario generar una nueva jornada de partidos automáticamente.
 *
 * @param scope CoroutineScope en el que se lanzará el bucle infinito del temporizador.
 * @param onGenerateRound Callback (acción) que se ejecutará cuando el tiempo lo requiera.
 */
class JornadaScheduler(
    private val scope: CoroutineScope,
    private val onGenerateRound: () -> Unit
) {
    // Variable para almacenar el trabajo en segundo plano activo (la corrutina).
    private var job: Job? = null
    
    // Indica si el temporizador está activado o no.
    var isRunning: Boolean = false
        private set

    /**
     * Inicia el comprobador automático en segundo plano.
     */
    fun start(config: AppConfig, lastRoundDate: String?) {
        // Evitar múltiples ejecuciones en paralelo si ya está corriendo
        if (isRunning) return
        
        // No hacer nada si en la configuración global no está permitida la auto-generación
        if (!config.autoGenerarJornadas) return

        isRunning = true
        
        // Iniciar un demonio (bucle infinito) en un hilo apto para tareas (Dispatchers.Default)
        job = scope.launch(Dispatchers.Default) {
            while (isActive) { // Mientras el trabajo (Job) no haya sido cancelado...
                checkAndGenerate(config, lastRoundDate)
                delay(60_000) // Pausar durante 1 minuto (60.000 ms)
            }
        }
    }

    /**
     * Detiene por completo el hilo automático.
     */
    fun stop() {
        job?.cancel()    // Cancela la corrutina que tenga el bucle infinito
        isRunning = false
    }

    /**
     * Revisa la fecha actual en la máquina real y la compara con
     * la fecha en que empezó el torneo o en la que se generó la última jornada.
     * Toma la decisión de si debe disparar el método `onGenerateRound()`.
     */
    private fun checkAndGenerate(config: AppConfig, lastRoundDateStr: String?) {
        val today = LocalDate.now()
        
        // Decide si deberíamos generar comparando fechas
        val shouldGenerate = if (lastRoundDateStr == null || lastRoundDateStr.isBlank()) {
           // Aún NO hay jornadas en toda la historia de la aplicación.
           // Revisamos si la 'fechaInicio' del torneo ya llegó o ya es pasada.
           if(config.fechaInicio.isNotBlank()){
               try {
                   // Parsear a "fecha pura" (sin horas). Ej: 2026-10-31
                   val startDate = LocalDate.parse(config.fechaInicio)
                   // Si hoy NO es antes de la fechaInicio, significa que ya ha empezado.
                   !today.isBefore(startDate)
               } catch(e: Exception) { false } // En caso de texto con formato de fecha malo
           } else {
               false
           }
        } else {
            // SÍ hay jornadas previas.
            // Entonces, medimos cuánto se distancia 'hoy' de la fecha de la última vez.
            try {
                val lastDate = LocalDate.parse(lastRoundDateStr)
                // Calcular diferencia de días naturales. Ej: Hoy=10, Última=1 -> Hay 9 días
                val daysSinceLast = ChronoUnit.DAYS.between(lastDate, today)
                // Comprobar si superamos el intervalo de días fijado en Configuración
                daysSinceLast >= config.intervaloJornadaDias
            } catch (e: Exception) {
                false
            }
        }

        // Si la comprobación de fechas da verde, ejecutamos el código del generador de emparejamientos
        if (shouldGenerate) {
            onGenerateRound()
            // (Nota de concepto: En una app real, tras ejecutar esto deberíamos guardar
            // de inmediato en disco para evitar que al minuto siguiente vuelva a dispararlo).
        }
    }
}
