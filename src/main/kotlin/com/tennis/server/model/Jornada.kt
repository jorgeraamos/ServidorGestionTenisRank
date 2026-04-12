package com.tennis.server.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Jornada(
    @SerialName("id") val id: Int = 0,                     // int4 en Supabase
    @SerialName("id_edicion") val idEdicion: Int,
    @SerialName("numero") val numero: Int,
    @SerialName("fecha_inicio") val fechaInicio: String = "",         // date ISO
    @SerialName("fecha_fin") val fechaFin: String = "",            // date ISO
    @SerialName("estado") val estado: String = "pendiente"      // "pendiente", "en_curso", "finalizada"
)
