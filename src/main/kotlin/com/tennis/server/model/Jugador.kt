package com.tennis.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Jugador(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("nombre_completo") val nombreCompleto: String,
    val email: String = "",
    val puntosActuales: Int = 0,
    val posicionRanking: Int = 0,
    val avatarUrl: String = "",
    val createdAt: String = "",          // ISO timestamp
    val nacionalidad: String = "",
    val fechaNacimiento: String = "",     // date ISO
    val manoDominante: String = "",       // "Diestro", "Zurdo"
    val estiloJuego: String = "",         // "Ofensivo", "Defensivo", etc.
    val mejorGolpe: String = "",          // "Drive", "Revés", "Saque", etc.
    val superficieFavorita: String = ""   // "Tierra batida", "Dura", "Hierba"
)
