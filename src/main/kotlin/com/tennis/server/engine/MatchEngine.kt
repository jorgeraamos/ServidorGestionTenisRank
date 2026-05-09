package com.tennis.server.engine

import com.tennis.server.data.actualizarHistorialRivales
import com.tennis.server.model.Edicion
import com.tennis.server.model.Jornada
import com.tennis.server.model.Jugador
import com.tennis.server.model.Participante
import com.tennis.server.model.Partido
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.jgrapht.alg.matching.blossom.v5.KolmogorovWeightedPerfectMatching

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

    // Función en la que se generan los emparejamientos de cada jornada:
    fun generarEmparejamientos(
        participantes: List<Participante>,
        jornada: Jornada
    ): List<Partido> {
        // Nos creamos el grafo, donde los nodos serán los jugadores y las aristas tendrán un peso que se
        // utilizará como criterio para emparejarlos
        val grafo = SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)

        // Nos creamos una nueva lista ya que los parámetros de la función son inmutables en Kotlin
        val listaNodos = if (participantes.size % 2 != 0) { // En caso de jugadores impar, añadimos un jugador ficticio
            // Si es impar, sumamos el nodo de descanso
            participantes + Participante(
                idEdicion = -1,
                id = "SISTEMA_BYE",
                puntos = -1,
                jugador = Jugador(
                id = "SISTEMA_BYE",
                nombreCompleto = "DESCANSO")
            )
        } else {
            // Si es par, usamos la lista tal cual
            participantes
        }

        // Añadimos los vértices al grafo
        listaNodos.forEach { participante -> grafo.addVertex(participante.id) }

        // Calculamos el valor de la penalización que se sumará al peso de la arista entre dos jugadores que hayan jugado recientemente
        // Dicho valor será la diferencia entre el jugador con mayor puntuación y el que menos
        val puntosMax = listaNodos.maxOf { it.puntos }
        val puntosMin = listaNodos.minOf { it.puntos }
        val penalizacion = (puntosMax - puntosMin).toDouble() + 1.0

        // A continuación añadimos las conexiones entre los jugadores
        for (i in listaNodos.indices) {
            for (j in i + 1 until listaNodos.size) {
                val participante1 = listaNodos[i]
                val participante2 = listaNodos[j]

                // Añadimos la arista
                val edge = grafo.addEdge(participante1.id, participante2.id)

                var peso =
                    // Caso en el que uno de los nodos sea el que representa la jornada de descanso, su peso será igual para todos los nodos
                    if (participante1.id == "SISTEMA_BYE" || participante2.id == "SISTEMA_BYE") {
                        // Añadimos para todas las aristas que conectan con el jugador de "descanso" la mayor diferencia de puntos en el ranking
                        // de esta manera, el último jugador que se ha quedado sin asignar será asignado con la jornada de descanso.
                        penalizacion
                    } else { // En otro caso, el peso de la arista será la diferencia de puntos (nivel) entre ambos jugadores
                        kotlin.math.abs(participante1.puntos - participante2.puntos).toDouble()
                    }
                // Añadimos una penalización si ambos jugadores se han enfrentado recientemente
                // Nótese que esto incluye al jugador que representa la jornada de descanso
                peso += if (participante2.id in participante1.historialRivales || participante1.id in participante2.historialRivales)
                    penalizacion else 0.0
                grafo.setEdgeWeight(edge, peso)
            }
        }

        // Una vez tenemos el grafo definido junto a su matriz de costes, procedemos con la ejecución del algoritmo
        // utilizando la función de Blossom V que viene dada en la librería de jgrapht
        val algoritmo = KolmogorovWeightedPerfectMatching(grafo)
        val matching = algoritmo.matching


        // Una vez tenemos el matching, procedemos a generar los partidos:
        return matching.edges.map {edge ->
            val v1 = grafo.getEdgeSource(edge)
            val v2 = grafo.getEdgeTarget(edge)
                Partido(
                    idJugador1 = v1,
                    idJugador2 = v2,
                    estado = "pendiente",
                    idGanador = null,
                    puntosIntercambiados = 0.0,
                    idJornada = jornada.id,
                )
        }

    }
}

