package com.tennis.server.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Partido(
    val id: String = UUID.randomUUID().toString(),
    val creadoAt: String = "",            // ISO timestamp
    val idJugador1: String,               // uuid FK -> jugadores
    val idJugador2: String,               // uuid FK -> jugadores
    val estado: String = "pendiente",     // "pendiente", "en_curso", "finalizado"
    val idGanador: String? = null,        // uuid FK -> jugadores (nullable)
    val puntosIntercambiados: Double = 0.0, // float8
    val idJornada: Int = 0                // int4 FK -> jornadas
)
