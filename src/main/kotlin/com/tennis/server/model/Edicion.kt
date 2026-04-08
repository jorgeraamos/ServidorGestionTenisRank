package com.tennis.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Edicion(
    @SerialName("id") val id: Int,
    @SerialName("nombre") val nombre: String,
    @SerialName("fecha_inicio") val fechaInicio : String?,
    @SerialName("fecha_fin") val fechaFin : String?,
    // Esta es la clave ajena que identifica a que ranking pertence
    @SerialName("id_ranking") val idRanking: Int,
    @SerialName("estado") val estado: String?
)