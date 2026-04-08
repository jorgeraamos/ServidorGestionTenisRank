package com.tennis.server.engine

import com.tennis.server.model.Jornada
import com.tennis.server.model.Jugador
import com.tennis.server.model.Partido

object MatchEngine {
    /**
     * Genera los emparejamientos (partidos) para una jornada.
     * DEMO: Método vacío que retorna lista vacía.
     * TODO: Implementar algoritmo real de emparejamiento.
     *
     * @param jugadores Lista de jugadores activos
     * @param jornada   La jornada para la que se generan partidos
     * @return Lista de Partido generados (vacía en demo)
     */
    fun generarEmparejamientos(jugadores: List<Jugador>, jornada: Jornada): List<Partido> {
        // Stub: retorna lista vacía para la demo
        return emptyList()
    }
}
