package com.tennis.server.model

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val intervaloJornadaDias: Int = 7,         // cada cuántos días se juega una jornada
    val fechaInicio: String = "",               // fecha de inicio del ranking
    val selectedRanking: Ranking? = null,
    val selectedEdicion: Edicion? = null,
    val ultimaJornada: Jornada? = null
)

@Serializable
data class AppData(
    val config: AppConfig = AppConfig(),
    val participantes: List<Participante> = emptyList(),
    val jornadas: List<Jornada> = emptyList(),
    val partidos: List<Partido> = emptyList(),
    val sets: List<Set> = emptyList()
)
