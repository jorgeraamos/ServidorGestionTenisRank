package com.tennis.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Partido(
    @SerialName( "id" ) val id: String = UUID.randomUUID().toString(),
    @SerialName("creado_at") val creadoAt: String = "",            // ISO timestamp
    @SerialName("id_jugador1") val idJugador1: String,               // uuid FK -> jugadores
    @SerialName("id_jugador2") val idJugador2: String,               // uuid FK -> jugadores
    @SerialName("estado") val estado: String = "pendiente",     // "pendiente", "en_curso", "finalizado"
    @SerialName("id_ganador") val idGanador: String? = null,        // uuid FK -> jugadores (nullable)
    @SerialName("puntos_intercambiados") val puntosIntercambiados: Double? = 0.0, // float8
    @SerialName("id_jornada") val idJornada: Int                 // int4 FK -> jornadas
)
