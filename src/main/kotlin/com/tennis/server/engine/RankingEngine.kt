package com.tennis.server.engine

import com.tennis.server.data.actualizarHistorialRivales
import com.tennis.server.data.getAllSets
import com.tennis.server.data.updatePuntuaciones
import com.tennis.server.model.Participante
import com.tennis.server.model.Partido
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.pow
import kotlin.math.roundToInt

object RankingEngine {

    suspend fun actualizarPuntuaciones(
        idEdicion: Int,
        partidos: List<Partido>,
        participantes: List<Participante>
    ) =
        coroutineScope {
            // Creamos una lista de tareas para ejecutar de manera asíncrona operaciones sobre supabase
            val tareas = mutableListOf<Deferred<Unit>>()

            val idsPartidos = partidos.map { it.id }
            // Cargamos todos los sets a la vez
            val sets = getAllSets(idsPartidos)

            for (partido in partidos) {
                val jugador1 = participantes.find { it.id == partido.idJugador1 } ?: continue
                val jugador2 = participantes.find { it.id == partido.idJugador2 } ?: continue

                // Comprobamos que no estemos ante un partido de descanso
                if (jugador1?.id != "SISTEMA_BYE" && jugador2?.id != "SISTEMA_BYE") {
                    var juegos_jugador1 = 0
                    var juegos_jugador2 = 0
                    var sets_jugador1 = 0
                    var sets_jugador2 = 0
                    val setsDelPartido = sets.filter { it.idPartido == partido.id }
                    for (set in setsDelPartido) {
                        val juegos_set_jugador1 = set.juegosJugador1
                        val juegos_set_jugador2 = set.juegosJugador2
                        juegos_jugador1 += juegos_set_jugador1
                        juegos_jugador2 += juegos_set_jugador2
                        if (juegos_set_jugador1 > juegos_set_jugador2)
                            sets_jugador1 += 1
                        else if (juegos_set_jugador1 < juegos_set_jugador2)
                            sets_jugador2 += 1
                    }
                    if(sets_jugador1 == sets_jugador2){
                        continue
                    }
                    val puntos_jugador1 = jugador1.puntos
                    val puntos_jugador2 = jugador2.puntos

                    val expectativa_AB =
                        1.0 / (1.0 + 10.0.pow((puntos_jugador2 - puntos_jugador1) / 400.0))

                    val k = when {
                        jugador1.partidosJugados <= 5 || jugador2.partidosJugados <= 5 -> 40
                        jugador1.partidosJugados <= 10 || jugador2.partidosJugados <= 10 -> 30
                        else -> 20
                    }
                    var nueva_puntuacion_jugador1 = 0
                    var nueva_puntuacion_jugador2 = 0
                    var idGanador = ""

                    if (sets_jugador1 > sets_jugador2) {
                        val peso_welo =
                            juegos_jugador1.toDouble() / (juegos_jugador1.toDouble() + juegos_jugador2.toDouble())
                        val puntos_intercambiados = (k * (1 - expectativa_AB) * peso_welo).roundToInt()
                        nueva_puntuacion_jugador1 =
                            (puntos_jugador1 + puntos_intercambiados)
                        nueva_puntuacion_jugador2 =
                            (puntos_jugador2 - puntos_intercambiados)
                        idGanador = jugador1.id
                    } else {
                        val peso_welo =
                            juegos_jugador2.toDouble() / (juegos_jugador1.toDouble() + juegos_jugador2.toDouble())
                        val puntos_intercambiados = (k * (1 - (1 - expectativa_AB)) * peso_welo).roundToInt()
                        nueva_puntuacion_jugador1 =
                            (puntos_jugador1 - puntos_intercambiados)
                        nueva_puntuacion_jugador2 =
                            (puntos_jugador2 + puntos_intercambiados)
                        idGanador = jugador2.id
                    }

                    // Actualizamos las puntuaciones en supabase de manera asíncrona
                    val tarea = async {
                        updatePuntuaciones(
                            idEdicion,
                            partido.id,
                            jugador1.id,
                            jugador2.id,
                            idGanador,
                            nueva_puntuacion_jugador1,
                            nueva_puntuacion_jugador2,
                            jugador1.partidosJugados,
                            jugador2.partidosJugados
                        )
                        // actualizamos también el campo historial_rivales
                        actualizarHistorialRivales(idEdicion, jugador1.id, jugador2.id)
                        actualizarHistorialRivales(idEdicion, jugador2.id, jugador1.id)
                    }
                    tareas.add(tarea)
                }else {  // Si es un partido de descanso:
                    // Actualizamos tb el campo historial_rivales para el jugador que ha descansado
                    if (jugador1.id == "SISTEMA_BYE") {
                        val tarea_actualizar = async {
                            actualizarHistorialRivales(idEdicion, jugador2.id, jugador2.id)
                        }
                        tareas.add(tarea_actualizar)
                    } else if (jugador2.id == "SISTEMA_BYE") {
                        val tarea_actualizar = async {
                            actualizarHistorialRivales(idEdicion, jugador1.id, jugador2.id)
                        }
                        tareas.add(tarea_actualizar)
                    }
                }
            }
            tareas.awaitAll()
        }
}