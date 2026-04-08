package com.tennis.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Participante(
    @SerialName("id_jugador") val id: String,
    @SerialName("puntos") val puntos: Int
)