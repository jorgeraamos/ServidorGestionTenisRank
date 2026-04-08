package com.tennis.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ranking(
    @SerialName("id") val id: Int,
    @SerialName("nombre") val nombre: String,
    @SerialName("ciudad") val ciudad: String,
    @SerialName("municipio") val municipio: String
) {

}