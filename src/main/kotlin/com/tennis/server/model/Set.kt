package com.tennis.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Set(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("id_partido")  val idPartido: String,                // uuid FK -> partidos
    @SerialName("numero_set") val numeroSet: Int,                   // 1, 2, 3...
    @SerialName("juegos_jugador1" ) val juegosJugador1: Int ,
    @SerialName("juegos_jugador2") val juegosJugador2: Int
)
