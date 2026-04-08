package com.tennis.server.model

import kotlinx.serialization.Serializable

@Serializable
data class Jornada(
    val id: Int = 0,                     // int4 en Supabase
    val nombre: String = "",
    val fechaInicio: String = "",         // date ISO
    val fechaFin: String = "",            // date ISO
    val estado: String = "pendiente"      // "pendiente", "en_curso", "finalizada"
)
