package com.tennis.server.data
import com.tennis.server.data.SupabaseClient.client
import com.tennis.server.model.Edicion
import com.tennis.server.model.Participante
import com.tennis.server.model.Ranking
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

import io.github.jan.supabase.storage.Storage
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
suspend fun actualizarHistorial(
    edicionId: Int,
    jugadorId: String,
    nuevoRivalId: String,
    historialActual: List<String>
) {
    try {
        // Mantenemos solo los últimos 3 rivales para no saturar la penalización
        val nuevoHistorial = (listOf(nuevoRivalId) + historialActual).take(3)

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