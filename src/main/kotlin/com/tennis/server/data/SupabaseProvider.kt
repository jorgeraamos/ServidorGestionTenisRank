package com.tennis.server.data
import com.tennis.server.data.SupabaseClient.client
import com.tennis.server.model.Edicion
import com.tennis.server.model.Participante
import com.tennis.server.model.Partido
import com.tennis.server.model.Ranking
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

import io.github.jan.supabase.storage.Storage
import java.util.Collections.emptyList
import java.util.Locale.filter
import javax.management.Query.eq
import kotlin.collections.map


// Conexión con Supabase
object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = ConfigLoader.supabaseUrl,
        supabaseKey = ConfigLoader.supabaseKey
    ) {
        install(Auth)
        install(Postgrest)
        install(io.github.jan.supabase.storage.Storage)
    }
}

suspend fun getAllRankings(): List<Ranking> {
    return try {
        val response = client.postgrest["ranking"]
            .select {}
        response.decodeList<Ranking>()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        emptyList()
    }
}

suspend fun getAllEdiciones(rankingId: Int): List<Edicion> {
    return try {
        val response = client.postgrest["edicion"]
            .select {
                filter {
                    // Filtramos por el ID del ranking y por el estado de las ediciones
                    eq("id_ranking", rankingId)
                    eq("estado", "activo")
                }
            }
        response.decodeList<Edicion>()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        emptyList()
    }
}

// Una vez se ha seleccionado el ranking y su edicion, procedemos a coger todos los participantes.
suspend fun getAllParticipantes(edicionId: Int): List<Participante> {
    return try {
        val response = client.postgrest["participa"]
            .select(Columns.raw("id_jugador,puntos,historial_rivales")) {
                filter {
                    eq("id_edicion", edicionId)
                }
            }
        response.decodeList<Participante>()
    } catch (e: Exception) {
        println("Error cargando participantes: ${e.message}")
        emptyList()
    }
}

// Función para actualizar el historial de rivales de un jugador
suspend fun actualizarHistorialRivales(
    edicionId: Int,
    jugadorId: String,
    rivalId: String,
) {
    try {
        // Buscamos el historial que tiene el jugador actualmente en la DB
        val participante = client.postgrest["participa"]
            .select {
                filter {
                    eq("id_edicion", edicionId)
                    eq("id_jugador", jugadorId)
                }
            }
            .decodeSingle<Participante>() // Necesitas tener la data class Participante con @Serializable

        val historialActual = participante.historialRivales

        // Mantenemos solo los últimos 3 rivales para no saturar la penalización
        val nuevoHistorial = (listOf(rivalId) + historialActual).take(3)

        client.postgrest["participa"].update(
            {
                set("historial_rivales", nuevoHistorial)
            }
        ) {
            filter {
                eq("id_edicion", edicionId)
                eq("id_jugador", jugadorId)
            }
        }

        //("SUPABASE", "Historial de rivales actualizado para $jugadorId")

    } catch (e: Exception) {
        println("SUPABASE_ERROR" + "Error al actualizar historial: ${e.message}")
    }
}

suspend fun insertPartidos(partidosGenerados: List<Partido>, edicionId: Int) {
    // No guardamos como partido el partido de descanso.
    val partidosReales = partidosGenerados.filter {
        it.idJugador1 != "SISTEMA_BYE" && it.idJugador2 != "SISTEMA_BYE"
    }

    // Insertamos en supabase los partidos
    client.postgrest["partido"].insert(partidosReales)

    // Actualizamos el historial de los rivales para cada jugador
    partidosGenerados.forEach { partido ->
        // Si el jugador1 es real, le añadimos al jugador2 (sea real o BYE)
        if (partido.idJugador1 != "SISTEMA_BYE") {
            actualizarHistorialRivales(edicionId, partido.idJugador1, partido.idJugador2)
        }

        // Lo mismo para el jugador2
        if (partido.idJugador2 != "SISTEMA_BYE") {
            actualizarHistorialRivales(edicionId, partido.idJugador2, partido.idJugador1)
        }
    }
}