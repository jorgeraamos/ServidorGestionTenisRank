package com.tennis.server.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Set(
    val id: String = UUID.randomUUID().toString(),
    val idPartido: String,                // uuid FK -> partidos
    val numeroSet: Int,                   // 1, 2, 3...
    val juegosJugador1: Int = 0,
    val juegosJugador2: Int = 0
)
