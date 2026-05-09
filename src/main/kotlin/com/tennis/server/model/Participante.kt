package com.tennis.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Participante(
    @SerialName("id_edicion") val idEdicion: Int,
    @SerialName("id_jugador") val id: String,
    @SerialName("puntos") val puntos: Int,
    @SerialName("historial_rivales") val historialRivales: List<String> = emptyList(),
    @SerialName("partidos_jugados") val partidosJugados: Int = 0,
    val jugador: Jugador // Se pondrán los datos del join (id, nombre_completo)
)