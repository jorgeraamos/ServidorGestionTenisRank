package com.tennis.server.engine

import com.tennis.server.model.Jornada
import com.tennis.server.model.Participante
import com.tennis.server.model.Partido
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph

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
            participantes + Participante(id = "SISTEMA_BYE", puntos = -1)
        } else {
            // Si es par, usamos la lista tal cual
            participantes
        }

        // Añadimos los vértices al grafo
        listaNodos.forEach { participante -> grafo.addVertex(participante.id) }

        // A continuación añadimos las conexiones entre los jugadores




        // 2. Calcular pesos: |puntos_A - puntos_B|
        // 3. Ejecutar algoritmo
        // 4. Retornar lista de objetos Partido

        return emptyList() // Implementaremos esto a continuación
    }
}

